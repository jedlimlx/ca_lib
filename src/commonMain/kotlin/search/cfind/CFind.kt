package search.cfind

import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.writeString
import LRUCache
import PLATFORM
import PriorityQueue
import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.rendering.TextStyle
import com.github.ajalt.mordant.rendering.TextStyles.bold
import patterns.Pattern
import patterns.Spaceship
import prettyPrintNeighbourhood
import rules.Rule
import rules.RuleFamily
import search.SearchProgram
import simulation.Coordinate
import simulation.Grid
import kotlin.math.pow
import kotlin.time.TimeSource
import kotlin.random.Random
import kotlin.time.measureTime
import fmod

/**
 * Searches for spaceships using a method similar to the method used by gfind, which was described by David Eppstein in
 * his paper, Searching for Spaceships (https://arxiv.org/abs/cs/0004003).
 */
class CFind(
    val rule: Rule,
    val _period: Int,
    val _k: Int,
    val width: Int,
    val symmetry: ShipSymmetry,
    val direction: Coordinate = Coordinate(0, 1),
    val isotropic: Boolean = true,
    val maxQueueSize: Int = 1 shl 20,
    minDeepeningIncrement: Int = -1,
    lookaheadDepth: Int = Int.MAX_VALUE,
    val searchStrategy: SearchStrategy = SearchStrategy.PRIORITY_QUEUE,
    val numShips: Int = Int.MAX_VALUE,
    val partialFrequency: Int = 1000,
    val backupFrequency: Int = 60*15,
    val backupName: String = "dump",
    transpositionTableSize: Int = 1 shl 20,
    val maxTimePerRound: Int = 5*60,
    val numThreads: Int = 8,
    val stdin: Boolean = false,
    val partialFiles: List<String> = listOf(),
    partialFileFrequency: Int = -1,
    val profiling: String? = null,
    verbosity: Int = 0
): SearchProgram(verbosity) {
    override val searchResults: MutableList<Pattern> = mutableListOf()

    val partialFileFrequency = if (partialFileFrequency > 0) partialFileFrequency else partialFrequency

    // TODO Handle alternating stuff properly / don't support alternating neighbourhoods
    var minDeepeningIncrement = if (minDeepeningIncrement == -1) {
        if (searchStrategy == SearchStrategy.HYBRID_BFS) _period
        else _period * 10  // shallow tree with deep leafs :)
    } else minDeepeningIncrement
    val originalMinDeepening = this.minDeepeningIncrement

    // Rotate the direction of the neighbour so the ship will go north
    val basisVectors = Pair(Coordinate(direction.y, -direction.x), direction)
    val neighbourhood: List<List<Coordinate>> = rule.neighbourhood.map {
        it.map {
            basisVectors.first * it.x + basisVectors.second * it.y
        }.toList()
    }.toList()

    // Compute statistics about the periodicity of the integer lattice (for oblique and diagonal searches)
    // TODO Figure out why oblique searches don't work
    val spacing = direction.x * direction.x + direction.y * direction.y
    val k = if (symmetry != ShipSymmetry.GLIDE) _k * spacing else _k
    val period = if (symmetry == ShipSymmetry.GLIDE && direction == Coordinate(1, 1)) _period / 2 else _period
    val backoffPeriod = period * ((k + period) / period)

    // Computing the backOff array to be used when gcd(k, period) > 1
    val backOff = IntArray(period) { -1 }.apply {
        this[0] = k
        if (period == 1) return@apply

        var count = 0
        for (i in 0..<period) {
            var index = 0
            while (this[(count + index + k).mod(period)] != -1) { index++ }

            this[count.mod(period)] = (index + k).mod(backoffPeriod)
            count += this[count.mod(period)]
        }

        this[count.mod(period)] = period * k - count

        val temp = this.toList()
        temp.forEachIndexed { index, it -> this[(index + 1).mod(period)] = it }
    }
    val fwdOff = run {
        val array = IntArray(period) { -1 }
        backOff.forEachIndexed { index, it -> array[(it + index).mod(period)] = it }

        array
    }

    // More stuff for the integer lattice
    private val tempOffsets = IntArray(spacing) {
        for (y in 0..<spacing) {
            if ((it * direction.y - y * direction.x).mod(spacing) == 0) {
                return@IntArray y
            }
        }

        0
    }
    val offsets = IntArray(this.period * this.k *
        (if (symmetry == ShipSymmetry.GLIDE && direction == Coordinate(1, 1)) 2 else 1)
    ) { 0 }.apply {
        if (symmetry != ShipSymmetry.GLIDE || direction != Coordinate(1, 1)) {
            for (i in 0 ..<k) {
                var count = (i*period).mod(k) // TODO fix for gcd(k, p) > 1
                while (count < period * k) {
                    this[count] = tempOffsets[i.mod(tempOffsets.size)]
                    count += backOff[count.mod(period)]
                }
            }
        } else {
            var flipped = false
            for (i in 0 ..<2*period) {
                this[i*k] = if (flipped) 1 else 0
                if (period.mod(2) == 1 || i.mod(period) == 0) flipped = !flipped
                
                for (j in 0..<2*k) {
                    this[(i*k+j*period) % this.size] = (this[i*k] + j) % 2
                }
            }
        }
    }

    // Compute various statistics about the neighbourhood
    // TODO Get neighbourhood coordinate direction conventions right
    val height: Int = neighbourhood.maxOf { it.maxOf { it.y } - it.minOf { it.y } } + 1
    val centralHeight: Int = -neighbourhood.minOf { it.minOf { it.y } }

    val baseCoordinates: List<Coordinate> = neighbourhood[0].filter { it.y == -centralHeight }.sortedBy { it.x }
    val baseCoordinateMap: IntArray = baseCoordinates.map { neighbourhood[0].indexOf(it) }.toIntArray()
    val continuousBaseCoordinates: Boolean = (baseCoordinates.maxOf { it.x } -
            baseCoordinates.minOf { it.x } + 1) == baseCoordinates.size
    val singleBaseCoordinate = baseCoordinates.size == 1

    val reversedBaseCoordinate: List<Coordinate> = run {
        (  // we need to fill in the gaps between the base coordinates
                (baseCoordinates.minOf { it.x } ..< baseCoordinates.maxOf { it.x } step spacing).reversed()
        ).map { Coordinate(it, -centralHeight) }.toList()
    }

    val lastBaseCoordinate = baseCoordinates.last()

    val skippedRows = (neighbourhood[0].minBy { it.y }.y..neighbourhood[0].maxBy { it.y }.y).filter { y ->
        neighbourhood[0].count { it.y == y } == 0 && y != 0
    }.map { it - neighbourhood[0].minBy { it.y }.y }
    val notSkippedRows = (0..height).filter { y -> y !in skippedRows }
    val indexToRowMap = Array(height) { notSkippedRows.indexOf(it) }

    val widthsByHeight: IntArray = IntArray(notSkippedRows.size) { it ->
        val height = notSkippedRows[it]
        val minY = neighbourhood[0].minOf { it.y }
        if (neighbourhood[0].count { it.y == height + minY } > 0) {
            (neighbourhood[0].filter { it.y == height + minY }.maxOf { it.x } -  // ceiling division
                    neighbourhood[0].filter { it.y == height + minY }.minOf { it.x } + spacing) / spacing
        } else -1
    }

    val leftBC: List<Coordinate> = run {
        val minX = neighbourhood[0].minBy { it.x }
        val maxX = baseCoordinates.last()

        // Compute all of the successive minimums in the neighbourhood
        var minY = Int.MAX_VALUE
        val minimums = arrayListOf<Coordinate>()
        for (x in minX.x .. maxX.x) {
            if (neighbourhood[0].count { it.x == x } > 0) {
                val coordinate = neighbourhood[0].filter { it.x == x }.minBy { it.y }
                if (coordinate.y <= minY) {
                    minimums.add(coordinate)
                    minY = coordinate.y
                }
            }
        }

        // Pad everything else out
        var count = 0
        val BCs = arrayListOf<Coordinate>()
        for (x in minX.x .. maxX.x-1) {
            if (minimums.size > count+1 && minimums[count+1].x == x) count++
            BCs.add(Coordinate(x, minimums[count].y))
        }

        BCs.reversed()
    }
    val rightBC: List<Coordinate> = run {
        val minX = baseCoordinates.last()
        val maxX = neighbourhood[0].maxBy { it.x }

        // Compute all of the successive minimums in the neighbourhood
        var minY = Int.MAX_VALUE
        val minimums = arrayListOf<Coordinate>()
        for (x in (minX.x .. maxX.x).reversed()) {
            if (neighbourhood[0].count { it.x == x } > 0) {
                val coordinate = neighbourhood[0].filter { it.x == x }.minBy { it.y }
                if (coordinate.y <= minY) {
                    minimums.add(coordinate)
                    minY = coordinate.y
                }
            }
        }

        // Pad everything else out
        var count = 0
        val BCs = arrayListOf<Coordinate>()
        for (x in (minX.x+1 .. maxX.x).reversed()) {
            if (minimums.size > count+1 && minimums[count+1].x == x) count++
            BCs.add(Coordinate(x, minimums[count].y))
        }

        BCs.reversed()
    }

    val bcDepth: Int = if (rightBC.isNotEmpty()) rightBC.groupBy { it.y }.map { (_, lst) -> lst.size }.max() else -1

    // Pre-computing the respective powers of the different coordinates in the neighbour
    val mainNeighbourhood: List<Pair<Coordinate, Int>> = neighbourhood[0].run {
        var power = 1
        val temp = arrayListOf<Pair<Coordinate, Int>>()
        this.forEach {
            if (it !in baseCoordinates) {
                temp.add(Pair(it, power))
                power *= rule.numStates
            }
        }

        temp
    }
    val neighbourhoodWithoutBg: HashMap<Coordinate, List<Pair<Coordinate, Int>>> = hashMapOf()

    // Initialising the transposition table
    @OptIn(ExperimentalUnsignedTypes::class)
    val equivalentStates: LRUCache<Int, UShortArray> = LRUCache(transpositionTableSize)

    // Computing the rows that should be used in computing the next state
    val indices = Array(period) { phase ->
        var count = 1
        IntArray(height - skippedRows.size) {
            if (count < height) {
                while (count in skippedRows) { count++ }
                (count++) * period
            } else centralHeight * period - backOff[phase]
        }
    }
    val minIndices = indices.map { it.min() }

    private val tempIndices = run {
        val lst = arrayListOf(indices)
        for (i in indices[0].distinct().indices) {
            lst.add(
                Array(period) { phase ->
                    val temp = lst.last()[phase].filter { it > 0 }.min()
                    val pos = lst.last()[phase].indexOf(temp)

                    if (pos < indices[0].size - 1) lst.last()[phase].map { it - temp }.toIntArray()
                    else {
                        val fwdOffset = fwdOff[phase.mod(period)]
                        lst.last()[phase].map {
                            if (it - temp != 0) it - (lst.last()[phase][0] - fwdOffset)
                            else 0
                        }.toIntArray()
                    }
                }
            )
            println(lst.last().map { it.toList() }.toList())
        }

        lst.subList(1, lst.size)
    }

    // TODO make sure its legit to only check 0th element
    val maxLookaheadDepth = tempIndices.map { it[0] }.run {
        val known = hashSetOf<Int>()
        var output = 0
        var breakLoop = false
        this.forEachIndexed { index, lst ->
            if (breakLoop) return@forEachIndexed

            var unknown = 0
            if (lst[0] - period !in known) {
                known.add(lst[0] - period)
                unknown++
            }

            lst.forEach {
                if (it < 0 && it !in known) {
                    known.add(it)
                    unknown++
                }
            }

            if (unknown > 1) {
                output = index
                breakLoop = true
                return@forEachIndexed
            }
        }

        if (breakLoop) output else this.size
    }  // double or even triple lookahead is possible for higher-range rules
    val lookaheadDepth = minOf(lookaheadDepth, maxLookaheadDepth)

    val successorLookaheadDepth = tempIndices.map { it[0].last() }.indexOf(0) + 1
    val successorLookahead = (successorLookaheadDepth == this.lookaheadDepth + 1 || tempIndices[0][1].last() == 0) && lookaheadDepth > 0

    val lookaheadIndices = if (this.lookaheadDepth == 0) listOf() else tempIndices.subList(
        0, if (successorLookahead) successorLookaheadDepth else this.lookaheadDepth
    )

    val rawAdditionalDepth: Int = when (indices[0].indexOf(indices[0].min())) {
        0 -> {
            var count = 1
            while (count in skippedRows) count++
            neighbourhood[0].filter { it.y == baseCoordinates[0].y + count }.maxOf{ it.x } + 1 - baseCoordinates.last().x
        }
        indices[0].size - 1 -> neighbourhood[0].filter { it.y == 0 }.maxOf{ it.x } + 1 - baseCoordinates.last().x
        else -> -1
    }
    val additionalDepthArray = IntArray(spacing) {
        ((rawAdditionalDepth - 1) + (offsets[(it + 1).mod(spacing)] - offsets[it])) / spacing + 1
    }
    val maxWidth: Int = widthsByHeight.slice(0 .. this.lookaheadDepth).maxOf { it }

    // TODO fix approximate lookahead for spacing != 1
    val approximateLookahead = spacing == 1 && lookaheadDepth > 0 && lookaheadIndices[0][0].last() != 0

    // Computing neighbourhoods to be memorised for lookahead
    val memorisedlookaheadNeighbourhood: List<List<Pair<Coordinate, Int>>> = lookaheadIndices.indices.map {
        if (lookaheadIndices[it][0].indexOf(lookaheadIndices[it][0].min()) == 0) {
            var count = 1
            while (count in skippedRows) count++
            mainNeighbourhood.filter { (it, _) -> it.y > -centralHeight + count }
        } else {
            mainNeighbourhood
        }
    }.toList()
    val lookaheadNeighbourhood: List<List<Pair<Coordinate, Int>>> = lookaheadIndices.indices.map {
        if (lookaheadIndices[it][0].indexOf(lookaheadIndices[it][0].min()) == 0) {
            var count = 1
            while (count in skippedRows) count++
            mainNeighbourhood.filter { (it, _) -> it.y == -centralHeight + count }
        } else {
            listOf()
        }
    }.toList()
    val minX = if (lookaheadNeighbourhood.isNotEmpty() && lookaheadNeighbourhood[0].isNotEmpty()) lookaheadNeighbourhood[0].minOf { (it, _) -> it.x } else 0

    // Building lookup tables
    val numEquivalentStates: Int = rule.equivalentStates.distinct().size
    val equivalentStateSets: List<List<Int>> = run {
        val lst = List<ArrayList<Int>>(numEquivalentStates) { arrayListOf() }
        rule.equivalentStates.forEachIndexed { actualState, state -> lst[state].add(actualState) }

        lst
    }

    val cacheWidths: IntArray = IntArray(widthsByHeight.size) {
        pow(numEquivalentStates, widthsByHeight[it] - (if (it == 0) 1 else 0))
    }

    val successorTable: Array<IntArray> = run {
        println("Generating successor lookup table...", verbosity = 0)

        Array(
            pow(rule.numStates, neighbourhood[0].size - baseCoordinates.size + 2)
        ) {
            val lst = IntArray(neighbourhood[0].size) { 0 }

            // Populating the list
            var power = 1
            for (i in lst.indices) {
                if (i !in baseCoordinateMap) {
                    lst[i] = getDigit(it, power, rule.numStates)
                    power *= rule.numStates
                }
            }

            // Getting the current and new states of the cells
            val currentState = getDigit(it, power, rule.numStates)
            power *= rule.numStates

            val newState = getDigit(it, power, rule.numStates)
            power *= rule.numStates

            // Building the inner lookup table
            IntArray(pow(numEquivalentStates, reversedBaseCoordinate.size)) {
                var power = 1
                for (c in reversedBaseCoordinate) {
                    if (c in baseCoordinates)
                        lst[baseCoordinateMap[baseCoordinates.indexOf(c)]] = getDigit(it, power, numEquivalentStates)

                    power *= numEquivalentStates
                }

                // Output will be represented in binary with the ith digit representing if state i can be used
                var output = 0
                for (i in 0 ..< rule.numStates) {
                    lst[baseCoordinateMap.last()] = i
                    if (newState == rule.transitionFunc(lst, currentState, 0, Coordinate(0, 0))) {
                        output += pow(2, i)
                    }
                }

                output
            }
        }
    }

    val combinedSuccessorArray: IntArray = run {
        if (verbosity >= 0 && !stdin) {
            t.cursor.move {
                up(1)
                startOfLine()
                clearScreenAfterCursor()
            }
            t.cursor.hide(showOnExit = true)
        }
        println("Generating approximate lookahead lookup table...", verbosity = 0)

        IntArray(
            pow(rule.numStates, neighbourhood[0].size - baseCoordinates.size + 2)
        ) {
            var num = 0
            for (j in 0..reversedBaseCoordinate.size) {
                var output = false
                for (i in successorTable[it].indices) {
                    if (
                        (
                            j == reversedBaseCoordinate.size || 
                            i and ((2 shl (reversedBaseCoordinate.size - j - 1)) - 1) shl j == 0
                        ) && successorTable[it][i] != 0
                    ) {
                        output = true
                        break
                    }
                }

                if (output) num += 1 shl j
            }

            num
        }
    }

    val memorisedBCs = leftBC + rightBC
    val memorisedBCsMap = memorisedBCs.mapIndexed { index, it -> it to index }.toMap()
    val bcNeighbourhood: ArrayList<List<Coordinate>> = arrayListOf()
    val inverseBcNeighbourhood: ArrayList<List<Coordinate>> = arrayListOf()

    val ignoreBCs: HashSet<Coordinate> = hashSetOf()
    val boundaryConditionTable: Array<Array<BooleanArray?>> = Array(
        leftBC.size + rightBC.size
    ) {
        if (verbosity >= 0 && !stdin) {
            t.cursor.move {
                up(1)
                startOfLine()
                clearScreenAfterCursor()
            }
            t.cursor.hide(showOnExit = true)
        }

        // Compute which coordinates to be removed
        val coordinate = memorisedBCs[it]
        println("Generating boundary condition lookup table for $coordinate...")

        val newNeighbourhood = neighbourhood[0].map { it - coordinate }
        val bcNeighbourhood = newNeighbourhood.filter {
            it.y != 0 && if (coordinate.x < baseCoordinates.last().x) it.x <= 0 else it.x >= 0
        }
        val inverseBcNeighbourhood = newNeighbourhood.filter {
            it.y == 0 && (if (coordinate.x < baseCoordinates.last().x) it.x <= 0 else it.x >= 0)
        }

        this.bcNeighbourhood.add(bcNeighbourhood)
        this.inverseBcNeighbourhood.add(inverseBcNeighbourhood)

        val removedCoordinateIndexes = newNeighbourhood.filter {
            it !in bcNeighbourhood || it in inverseBcNeighbourhood
        }.map { newNeighbourhood.indexOf(it) }.toSet()
        val removedCoordinateIndexes2 = inverseBcNeighbourhood.map { newNeighbourhood.indexOf(it) }.toSet()

        // Check if the central cell will be 0
        val centralCell = (-coordinate.y != 0) && if (
            coordinate.x < baseCoordinates.last().x
        ) -coordinate.x <= 0 else -coordinate.x >= 0

        // Running the computation
        var useful = false
        val output = Array(pow(rule.numStates, bcNeighbourhood.size + if (centralCell) 2 else 0)) {
            val lst = IntArray(neighbourhood[0].size) { 0 }

            // Populating the list
            var power = 1
            for (i in lst.indices) {
                if (i !in removedCoordinateIndexes) {
                    lst[i] = getDigit(it, power, rule.numStates)
                    power *= rule.numStates
                }
            }

            // Getting the current and new states of the cells
            val currentState: Int
            val newState: Int
            if (centralCell) {
                currentState = getDigit(it, power, rule.numStates)
                power *= rule.numStates

                newState = getDigit(it, power, rule.numStates)
            } else {
                currentState = 0
                newState = 0
            }

            // Building the inner lookup table
            var useful2 = false
            val output = BooleanArray(pow(numEquivalentStates, removedCoordinateIndexes2.size)) {
                var power = 1
                for (i in removedCoordinateIndexes2) {
                    lst[i] = getDigit(it, power, numEquivalentStates)
                    power *= numEquivalentStates
                }

                val output = newState == rule.transitionFunc(lst, currentState, 0, Coordinate(0, 0))
                if (!output) {
                    useful = true
                    useful2 = true
                }

                output
            }

            if (useful2) output else null
        }

        // Removing BCs that can be ignored
        if (!useful) ignoreBCs.add(coordinate)
        output
    }

    val approximateLookaheadTable: IntArray? = run {
        if (verbosity >= 0 && !stdin) {
            t.cursor.move {
                up(1)
                startOfLine()
                clearScreenAfterCursor()
            }
            t.cursor.hide(showOnExit = true)
        }
        
        println("Generating approximate lookahead table...")

        if ((rule.numStates + 1.0).pow(neighbourhood[0].size + 1) < 1.0.pow(31)) {
            IntArray(
                pow(rule.numStates + 1, neighbourhood[0].size + 1)
            ) {
                val lst = IntArray(neighbourhood[0].size) { 0 }

                // Populating the list
                var power = 1
                for (i in lst.indices) {
                    val digit = getDigit(it, power, rule.numStates + 1)
                    lst[i] = if (digit == rule.numStates) -1 else digit
                    power *= (rule.numStates + 1)
                }

                // Getting the current and new states of the cells
                val currentState = getDigit(it, power, rule.numStates + 1)

                // Output will be represented in binary with the ith digit representing if state i can be used
                rule.transitionFuncWithUnknowns(lst, currentState, 0, Coordinate(0, 0))
            }
        } else null
    }

    // Filter out the boundary conditions that don't need checking
    val filteredRightBCs = rightBC.filter { it !in ignoreBCs }
    val filteredLeftBCs = leftBC.subList(
        0,
        if (symmetry != ShipSymmetry.ASYMMETRIC && symmetry != ShipSymmetry.GLIDE) lastBaseCoordinate.x
        else leftBC.size
    ).filter { it !in ignoreBCs }

    // Split the boundary conditions by at which depth they apply
    // TODO idk why this doesn't work
//    val splitRightBCs = (0 ..< spacing).map { depth ->
//        filteredRightBCs.filter {
//            val coordinate = -it + lastBaseCoordinate
//            coordinate.x.mod(spacing) == offsets[(depth - coordinate.y * period).mod(offsets.size)]
//        }
//    }
//    val splitLeftBCs = (0 ..< spacing).map { depth ->
//        filteredLeftBCs.filter {
//            val coordinate = -it + Coordinate(width * spacing - 1, 0) + lastBaseCoordinate
//            coordinate.x.mod(spacing) == offsets[(depth - coordinate.y * period).mod(offsets.size)]
//        }
//    }

    // Opening the partial files
    val partialFileStreams = partialFiles.map { SystemFileSystem.sink(Path(it)).buffered() }

    // For hybrid BFS / pure DFS, we will represent the queue / stack as a linked list
    var head: Row? = null
    var tail: Row? = null
    var queueSize = 0
    var numDFSRounds = 0

    // The priority queue for the ikpx2 search algorithm
    val priorityQueue = PriorityQueue<Row>(maxQueueSize)

    // Check if any saved state was loaded into the search
    var loadedState = false

    override fun search() {
        // Clear the lookup table outputs
        if (verbosity >= 0 && !stdin) {
            t.cursor.move {
                up(1)
                startOfLine()
                clearScreenAfterCursor()
            }
            t.cursor.hide(showOnExit = true)
        }

        println(bcNeighbourhood)
        println(inverseBcNeighbourhood)
        println(memorisedBCsMap)

        // TODO Error handling of invalid inputs, e.g. invalid symmetries
        // Print a message that indicates the search is beginning
        println(bold("Beginning search for width ${green("$width")} " +
                "spaceship with ${green("$symmetry")} symmetry moving towards ${green("$direction")} at " +
                "${green("${_k}c/$_period")}${if (rule is RuleFamily) " in ${green(rule.rulestring)}" else ""}..."))

        // Printing out some debugging information
        println(brightRed(bold("\nNeighbourhood\n----------------")), verbosity = 1)
        println((bold("Neighbourhood: ") + "\n${prettyPrintNeighbourhood(neighbourhood[0].toTypedArray())}"), verbosity = 1)
        println((bold("Neighbourhood Height: ") + "$centralHeight / $height"), verbosity = 1)
        println((bold("Boundary Conditions: ") +
                "[${leftBC.map { if (it !in filteredLeftBCs) gray(it.toString()) else it.toString() }.joinToString(", ")}] / " +
                "[${rightBC.map { if (it !in filteredRightBCs) gray(it.toString()) else it.toString() }.joinToString(", ")}]"
        ), verbosity = 1)
        println((bold("Right BC Depth: ") + "$bcDepth"), verbosity = 1)
        println((bold("Base Coordinates: ") + "$baseCoordinates"), verbosity = 1)
        println((bold("Base Coordinate Map: ") + "${baseCoordinateMap.toList()}"), verbosity = 1)
        println((bold("Continuous Base Coordinates: ") + "${reversedBaseCoordinate.toList()}"), verbosity = 1)
        println((bold("Cache Widths: ") + "${cacheWidths.toList()} / ${widthsByHeight.toList()}"), verbosity = 1)
        println((bold("Skipped Rows: ") + "${skippedRows.toList()}"), verbosity = 1)
        println((bold("Index to Row: ") + "${indexToRowMap.toList()}"), verbosity = 1)

        println(brightRed(bold("\nLattice\n----------------")), verbosity = 1)
        println((bold("Basis Vectors: ") + "${basisVectors.first} / ${basisVectors.second}"), verbosity = 1)
        println((bold("Spacing: ") + "$spacing"), verbosity = 1)
        println((bold("Offsets: ") + "${offsets.toList()}"), verbosity = 1)

        println(brightRed(bold("\nMisc\n----------------")), verbosity = 1)
        println((bold("Successor Table Size: ") + "${successorTable.size}"), verbosity = 1)
        println((bold("Backoff Table: ") + "${backOff.toList()}"), verbosity = 1)
        println((bold("Reverse Backoff Table: ") + "${fwdOff.toList()}"), verbosity = 1)
        println((bold("Maximum Lookahead Depth: ") + "$maxLookaheadDepth"), verbosity = 1)
        println((bold("Successor Lookahead: ") + "$successorLookaheadDepth / $successorLookahead"), verbosity = 1)
        println((bold("Approximate Lookahead: ") + "$approximateLookahead"), verbosity = 1)
        println((bold("Additional Depth (for lookahead): ") + "${additionalDepthArray.toList()}"), verbosity = 1)
        println((bold("Lookahead Depth: ") + "$lookaheadDepth"), verbosity = 1)
        println(
            (
                bold("Row Indices: ") +
                "\n${indices.map { it.toList() }.toList()}" +
                (if (this.lookaheadDepth != 0) "\n${tempIndices.subList(0, this.lookaheadDepth).map {
                    it.map { it.toList() }.toList()
                }.joinToString("\n")}" else "") +
                gray(
                    "\n${
                        tempIndices.subList(this.lookaheadDepth, tempIndices.size).map {
                            it.map { it.toList() }.toList()
                        }.joinToString("\n")
                    }"
                )
            ), verbosity = 1
        )

        // Initialising BFS queue with (height - 1) * period empty rows
        var currentRow: Row
        if (!loadedState) {
            currentRow = Row(null, IntArray(width) { 0 }, this)
            for (i in 1 .. period * (height - 1)) {
                val nextRow = Row(currentRow, IntArray(width) { 0 }, this)
                currentRow.next = nextRow
                nextRow.prev = currentRow

                currentRow = nextRow
            }
        } else {
            if (searchStrategy == SearchStrategy.PRIORITY_QUEUE) currentRow = priorityQueue.poll()
            else currentRow = head!!
        }

        // Take note of the starting time
        val timeSource = TimeSource.Monotonic
        val startTime = timeSource.markNow()

        // Information we will be keeping track of
        var shipsFound = 0
        var count = 0
        var clearPartial = false
        var clearLines = 0

        // Some statistics that we will be keeping track of
        var avgSuccessorTime = 0.0
        val queueSizes: ArrayList<Int> = arrayListOf()
        val times: ArrayList<Double> = arrayListOf()
        val numSuccessorList = ArrayList<Map<Int, Int>>()

        val numSuccessors = HashMap<Int, Int>()

        // Some common functions
        fun processSuccessors(successors: List<Row>): List<Row> = if (currentRow.successorSequence != null) {
            // This optimisation is possible because of the nature of depth-first search
            // The successful branch will lie in-between the unknown branches and the deadends
            val sequence = currentRow.successorSequence!!
            val index = sequence[0]
            if (sequence.size > 1) successors[index].successorSequence = sequence.copyOfRange(1, sequence.size)
            successors.subList(0, index + 1)
        } else successors

        fun printPartials(message: Any, numLines: Int = 3, forcePrint: Boolean = false) {
            if ((count++).mod(partialFrequency) == 0 || forcePrint) {
                val grid = currentRow.toGrid(period, symmetry)
                grid.rule = rule

                if (verbosity >= 0 && clearPartial && !stdin) {
                    t.cursor.move {
                        up(numLines + clearLines)
                        startOfLine()
                        clearScreenAfterCursor()
                    }
                    t.cursor.hide(showOnExit = true)
                }

                println(message)
                clearLines = printRLE(grid, write=count.mod(partialFileFrequency) == 0)
                clearPartial = true
            } else if (count.mod(partialFileFrequency) == 0) {
                val grid = currentRow.toGrid(period, symmetry)
                grid.rule = rule

                printRLE(grid, write=true, print=false)
            }
        }

        // Hybrid BFS / Pure DFS
        var backups = 0
        if (searchStrategy == SearchStrategy.HYBRID_BFS || searchStrategy == SearchStrategy.DFS) {
            // We will represent the queue as a linked list
            if (!loadedState) {
                queueSize = 1
                head = if (searchStrategy != SearchStrategy.DFS) currentRow else null
                tail = if (searchStrategy != SearchStrategy.DFS) currentRow else null
            }
            while (shipsFound < numShips) {
                // BFS round runs until the queue size exceeds the maximum queue size
                clearPartial = false
                while (queueSize < maxQueueSize) {
                    if (queueSize == 0) {
                        println(
                            bold(
                                "\nSearch terminated in ${green("${(timeSource.markNow() - startTime).inWholeMilliseconds / 1000.0}s")}. " +
                                        "${green("$shipsFound")} ship${if (shipsFound == 1) "" else "s"} found."
                            )
                        )

                        // Writing to the profiling file
                        if (profiling != null) {
                            val data = StringBuilder().apply {
                                append("queue_size,avg_time,num_successors\n")
                                for (i in 0..<queueSizes.size) {
                                    append("${queueSizes[i]},${times[i]},${(0..<numSuccessorList[i].keys.max()).map {
                                        numSuccessorList[i][it] ?: 0
                                    }.joinToString(",")}\n")
                                }
                            }.toString()

                            SystemFileSystem.sink(Path(profiling)).buffered().writeString(data)
                        }
                        return
                    }

                    // Get the current row that is going to be analysed
                    if (searchStrategy == SearchStrategy.DFS) {
                        currentRow = tail!!
                        tail = currentRow.prev
                    } else {
                        // TODO I thought I fixed this but I didn't
                        currentRow = head!!
                        head = currentRow.next
                    }

                    // Removes the row from the linked list
                    currentRow.pop()
                    queueSize--

                    // Check if the ship is completed
                    if (checkCompletedShip(currentRow)) {
                        clearPartial = false
                        if (++shipsFound == numShips) break
                    }

                    // Check the transposition table for looping components
                    if (checkEquivalentState(currentRow)) continue

                    // Get the rows that will need to be used to find the next row
                    val successors: List<Row>
                    avgSuccessorTime = avgSuccessorTime * 0.999 + measureTime {
                        val (rows, lookaheadRows) = extractRows(currentRow)
                        successors = nextRow(currentRow, rows, lookaheadRows, depth = currentRow.depth + 1).first
                    }.inWholeNanoseconds * 0.001
                    if (successors.size in numSuccessors)
                        numSuccessors[successors.size] = numSuccessors[successors.size]!! + 1
                    else numSuccessors[successors.size] = 1

                    // Adding the new rows to the linked list
                    val temp = processSuccessors(successors)
                    queueSize += temp.size
                    temp.forEach {
                        if (head == null) head = it

                        tail?.next = it
                        it.prev = tail
                        tail = it
                    }

                    if (count.mod(1000) == 0) {
                        queueSizes.add(queueSize)
                        times.add(avgSuccessorTime)
                        numSuccessorList.add(numSuccessors.toMap())

                        numSuccessors.clear()
                    }

                    // Printing out the partials
                    printPartials(bold("\nQueue Size: $queueSize / $maxQueueSize"))
                }

                if (shipsFound == numShips) break

                // Check how much time has past and see if we need to write to a backup
                if ((timeSource.markNow() - startTime).inWholeMilliseconds > (backups+1)*backupFrequency*1000) {
                    backupState("${backupName}_${backups++}.txt", saveState())
                }

                // DFS round runs for a certain deepening increment
                val message = "Beginning depth-first search round, queue size $queueSize"
                println(bold("\n$message"))

                count = 0
                clearPartial = false

                var num = 0
                numDFSRounds++
                if (PLATFORM == "JVM" && numThreads > 1) {
                    val output = multithreadedDfs(this)
                    num = output

                    clearPartial = true
                } else {
                    var row = head
                    var prunedCount = 0
                    val stack = arrayListOf<Row>()
                    while (row != null) {
                        // Placing row within DFS stack
                        stack.clear()
                        stack.add(row)

                        // Computing the depth that needs the row needs to be pruned until
                        val maxDepth = row.prunedDepth + minDeepeningIncrement

                        if (row.prunedDepth > maxDepth) continue

                        num += maxDepth - row.depth

                        do {
                            if (stack.isEmpty()) {
                                if (head!!.id == row.id) head = row.next
                                if (tail!!.id == row.id) tail = row.prev

                                prunedCount++
                                break
                            }

                            // Get the current row that is going to be analysed
                            currentRow = stack.removeLast()
                            if (currentRow.depth == maxDepth) {
                                // Adding the successor sequence to the row
                                val predecessors = currentRow.getAllPredecessors(
                                    maxDepth - row.depth, deepCopy = false
                                ).reversed()
                                row.successorSequence = IntArray(maxDepth - row.depth) { predecessors[it].successorNum }
                                break
                            }

                            // Get the rows that will need to be used to find the next row
                            val (rows, lookaheadRows) = extractRows(currentRow)
                            val successors = nextRow(currentRow, rows, lookaheadRows, depth = currentRow.depth + 1).first
                            val temp = processSuccessors(successors)

                            // Adding the successors to the stack
                            stack.addAll(temp)
                        } while (true)

                        // Printing out the partials
                        printPartials(
                            bold(
                                "\nChecked ${count - 1} / $maxQueueSize rows, " +
                                        "pruned ${(10000 - ((count - prunedCount) * 10000 / (count + 1))) / 100.0}%"
                            )
                        )

                        val temp = row.next
                        if (stack.isEmpty()) {
                            row.pop()
                            queueSize--
                        }
                        row = temp
                    }
                }

                // Clean up the output once the DFS round is done
                if (verbosity >= 0 && clearPartial && !stdin) {
                    t.cursor.move {
                        up(4 + clearLines)
                        startOfLine()
                        clearScreenAfterCursor()
                    }
                    t.cursor.hide(showOnExit = true)
                }

                // Print out some status information
                val averageDeepening = num / maxQueueSize.toDouble()
                println(bold("$message -> $queueSize, average deepening " +
                        "${(100 * averageDeepening).toInt() / 100.0}"))

                // Check how much time has past and see if we need to write to a backup
                if ((timeSource.markNow() - startTime).inWholeMilliseconds > (backups+1)*backupFrequency*1000) {
                    backupState("${backupName}_${backups++}.txt", saveState())
                }
            }
        } else {
            priorityQueue.add(currentRow)

            if (PLATFORM == "JVM" && numThreads > 1) {
                multithreadedPriorityQueue(this)
                shipsFound = searchResults.size
            } else {
                // We just run repeated DFS rounds
                var row: Row
                var pruning = 0.8
                var longestPartialSoFar = currentRow.depth
                val stack = arrayListOf<Row>()
                while (priorityQueue.isNotEmpty()) {
                    row = priorityQueue.poll()

                    stack.clear()
                    stack.add(row)

                    // Decide what depth we should reach
                    val maxDepth = row.prunedDepth + minDeepeningIncrement

                    do {
                        // Check if stack is empty
                        if (stack.isEmpty()) {
                            pruning = 0.99 * pruning + 0.01
                            break
                        }

                        // Get the current row that is going to be analysed
                        currentRow = stack.removeLast()
                        if (currentRow.depth == maxDepth) {
                            pruning *= 0.99

                            // Compute the predecessors
                            val predecessors = currentRow.getAllPredecessors(
                                maxDepth - row.depth, deepCopy = false
                            ).reversed()

                            // Decide how many rows to add to the priority queue
                            var rowsAdded = 0
                            var finalDepth = -1
                            val maxRowsAdded = (maxQueueSize / ((priorityQueue.size + 0.0001) * (1.0 - pruning))).toInt()
                            for (depth in row.depth + 1..maxDepth) {
                                val lst = stack.filter { it.depth == depth }
                                rowsAdded += lst.size

                                if (rowsAdded < maxRowsAdded || depth == row.depth + 1) {
                                    lst.forEach { priorityQueue.add(it) }
                                    finalDepth = depth
                                } else break
                            }

                            if (finalDepth == -1) finalDepth = maxDepth
                            val temp = currentRow.getPredecessor(maxDepth - finalDepth)!!

                            // Adding the successor sequence to the row
                            if (currentRow.depth > temp.depth)
                                temp.successorSequence = IntArray(currentRow.depth - temp.depth) {
                                    predecessors[temp.depth - row.depth + it].successorNum
                                }

                            // Check the transposition table for looping components
                            priorityQueue.add(temp)
                            break
                        }

                        // Check if the ship is completed
                        if (checkCompletedShip(currentRow)) {
                            clearPartial = false
                            if (++shipsFound == numShips) break
                        }

                        // Check the transposition table for looping components
                        if (currentRow.successorSequence == null && checkEquivalentState(currentRow)) continue

                        // Get the rows that will need to be used to find the next row
                        val (rows, lookaheadRows) = extractRows(currentRow)
                        val successors = nextRow(currentRow, rows, lookaheadRows, depth = currentRow.depth + 1).first

                        // Adding the new rows to the stack
                        stack.addAll(processSuccessors(successors))

                        // Printing out the partials
                        if (currentRow.depth > longestPartialSoFar) {
                            longestPartialSoFar = currentRow.depth
                            printPartials(bold("\nDepth: ${currentRow.depth}"), numLines = 4, forcePrint = true)
                            clearPartial = false
                            clearLines--
                        } else {
                            printPartials(
                                bold(
                                    "\nPriority Queue Size: ${priorityQueue.size} / $maxQueueSize" +
                                            "\nStack Size: ${stack.size}, Depth: ${currentRow.depth} / $maxDepth"
                                ), numLines = 4
                            )
                        }
                    } while (true)

                    // Check if sufficiently many ships have been found
                    if (shipsFound == numShips) break

                    // Check how much time has past and see if we need to write to a backup
                    if ((timeSource.markNow() - startTime).inWholeMilliseconds > (backups+1)*backupFrequency*1000) {
                        backupState("${backupName}_${backups++}.txt", saveState())
                    }
                }
            }
        }

        println(
            bold(
                "\nSearch terminated in ${green("${(timeSource.markNow() - startTime).inWholeMilliseconds / 1000.0}s")}. " +
                        "${green("$shipsFound")} ship${if (shipsFound == 1) "" else "s"} found."
            )
        )

        // Writing to the profiling file
        if (profiling != null) {
            val data = StringBuilder().apply {
                append("queue_size,avg_time,num_successors\n")
                for (i in 0..<queueSizes.size) {
                    append("${queueSizes[i]},${times[i]},${(0..<numSuccessorList[i].keys.max()).map {
                        numSuccessorList[i][it] ?: 0
                    }.joinToString(",")}\n")
                }
            }.toString()

            SystemFileSystem.sink(Path(profiling)).buffered().writeString(data)
        }
    }

    override fun stop() {
        TODO("Not yet implemented")
    }

    override fun saveToFile(filename: String) {
        TODO("Not yet implemented")
    }

    override fun saveState(): String = StringBuilder().apply {
        val queueSize = if (searchStrategy == SearchStrategy.PRIORITY_QUEUE) priorityQueue.size else queueSize
        val inQueue = HashSet<Long>(queueSize)
        if (searchStrategy == SearchStrategy.PRIORITY_QUEUE)
            priorityQueue.forEach { inQueue.add(it.id) }
        else {
            var row = head
            while (row != null) {
                inQueue.add(row.id)
                row = row.next
            }
        }

        val added = HashSet<Long>(queueSize)
        val queue = PriorityQueue<Row>(queueSize) { row, other -> row.depth - other.depth }
        if (searchStrategy == SearchStrategy.PRIORITY_QUEUE) {
            for (row in priorityQueue) {
                queue.add(row)
                added.add(row.id)
                row.applyOnPredecessor {
                    if (it.id !in added) {
                        queue.add(it)
                        added.add(it.id)
                    }
                }
            }
        } else {
            var row = head
            while (row != null) {
                queue.add(row)
                added.add(row.id)
                row.applyOnPredecessor {
                    if (it.id !in added) {
                        queue.add(it)
                        added.add(it.id)
                    }
                }

                row = row.next
            }
        }

        append("${queue.size} $width $rule $symmetry $period $k $direction\n")
        while (queue.isNotEmpty()) {
            val row = queue.poll()
            append("${row.id} ${row.predecessor?.id ?: -1} ${row.hashCode()}")

            val output = row.successorSequence?.toList()?.joinToString(",")
            append(if (output == null) " " else " $output ")
            if (row.id in inQueue) append("*")
            append("\n")
        }
    }.toString()

    override fun loadState(string: String) {
        loadedState = true

        val lines = string.split("\n")
        val params = lines[0].split(" ")

        val rows = HashMap<Long, Row>(params[0].toInt())
        queueSize = 0
        for (i in 1 ..< lines.size) {
            val tokens = lines[i].split(" ")
            if (tokens.size < 3) continue

            // Loading in the normal row
            val row = Row(
                rows[tokens[1].toLong()],
                IntArray(width - params[1].toInt()) { 0 } + tokens[2].toInt().toString(rule.numStates).padStart(
                    params[1].toInt(), '0'
                ).map { it.digitToInt() }.reversed().toIntArray(),
                this
            )
            rows[tokens[0].toLong()] = row

            // We can only load the successor sequence if the width is the same
            val sequence = tokens.last().split(",")
            if (tokens.size > 3 && sequence[0].toIntOrNull() != null && width == params[1].toInt())
                row.successorSequence = sequence.map { it.toInt() }.toIntArray()

            // Adding stuff to the queue
            if (tokens.last() == "*") {
                if (searchStrategy == SearchStrategy.PRIORITY_QUEUE) priorityQueue.add(row)
                else {
                    if (head == null) {
                        head = row
                        tail = row
                    } else {
                        tail!!.next = row
                        row.prev = tail
                        tail = row
                    }

                    queueSize++
                }
            }
        }
    }

    fun backupState(file: String, state: String) {
        SystemFileSystem.sink(Path(file)).buffered().writeString(state)
    }

    fun displayPartials(minDepth: Int = 0) {
        val depthMap = hashMapOf<Int, Int>()
        if (searchStrategy == SearchStrategy.PRIORITY_QUEUE) {
            for (row in priorityQueue) {
                if (row.depth >= minDepth) {
                    val grid = row.toGrid(period, symmetry)
                    grid.rule = rule

                    printRLE(grid)
                    println()
                }

                if (row.depth in depthMap) depthMap[row.depth] = depthMap[row.depth]!! + 1
                else depthMap[row.depth] = 1
            }
        } else {
            var row = head
            while (row != null) {
                if (row.depth >= minDepth) {
                    val grid = row.toGrid(period, symmetry)
                    grid.rule = rule

                    printRLE(grid)
                    println()
                }

                if (row.depth in depthMap) depthMap[row.depth] = depthMap[row.depth]!! + 1
                else depthMap[row.depth] = 1

                row = row.next
            }
        }

        // Printing some statistics
        val total = depthMap.map { (_, v) -> v }.sum()
        println(bold(brightRed("Depths of Partials\n----------------")))
        println("${bold("Total Partials:")} $total")
        for (depth in depthMap.keys.sorted()) {
            println("${bold(depth.toString())}: ${depthMap[depth]}, ${(depthMap[depth]!!*10000L/total)/100.0}%")
        }
    }

    /**
     * Checks if the ship is completed.
     */
    fun checkCompletedShip(row: Row): Boolean {
        if (row.completeShip((height - 1) * period) == 1) {
            if (stdin) return true

            val grid = row.toGrid(period, symmetry)
            grid.rule = rule

            println(brightRed(bold("\nShip found!")))
            printRLE(grid, style=brightBlue + bold)

            searchResults.add(Spaceship(0, k, period, grid))
            return true
        }

        return false
    }

    /**
     * Checks if the search has arrived at an equivalent state.
     */
    @OptIn(ExperimentalUnsignedTypes::class)
    fun checkEquivalentState(row: Row): Boolean {
        val rows = row.getAllPredecessors((height - 1) * period)
        val hash = rows.map { it.hashCode() }.hashCode()
        val reverseHash = rows.map { it.reverseHashCode() }.hashCode()
        if (hash in equivalentStates.keys) {
            var equivalent = true
            val state = equivalentStates.get(hash)!!
            for (i in state.indices) {
                if (state[i] != rows[i].hash.toUShort()) {
                    equivalent = false
                    break
                }
            }

            if (!equivalent) return false
        } else if (
            isotropic && 
            (symmetry == ShipSymmetry.GLIDE || symmetry == ShipSymmetry.ASYMMETRIC) &&
            reverseHash in equivalentStates.keys
        ) {
            var equivalent = true
            val state = equivalentStates.get(reverseHash)!!
            for (i in state.indices) {
                if (state[i] != rows[i].reverseHash.toUShort()) {
                    equivalent = false
                    break
                }
            }

            if (!equivalent) return false
        } else {
            val temp = rows.map { it.hash.toUShort() }.toUShortArray()
            equivalentStates.put(hash, temp)
            if (isotropic && (symmetry == ShipSymmetry.GLIDE || symmetry == ShipSymmetry.ASYMMETRIC)) {
                equivalentStates.put(
                    reverseHash, 
                    rows.map { it.reverseHash.toUShort() }.toUShortArray()
                )
            }
            return false
        }

        return true
    }

    /**
     * Extract the relevant rows that will be used in finding the next state given the current latest [row].
     * It will return a pair of rows - one for the regular successor search and the other for lookahead.
     */
    fun extractRows(row: Row): Pair<List<Row>, List<List<Row?>>> = Pair(
        indices[(row.depth + 1).mod(period)].map { row.getPredecessor(it - 1)!! }.toList(),
        lookaheadIndices.map { it[(row.depth + 1).mod(period)].map { row.getPredecessor(it - 1) }.toList() }
    )

    /**
     * Searches for a possible next row given the previous rows provided. Returns null if row cannot be found.
     */
    fun nextRow(
        currentRow: Row,
        rows: List<Row>,
        lookaheadRows: List<List<Row?>>,
        lookaheadDepth: Int = 0,
        lookaheadMemo: IntArray? = null,
        depth: Int = 0
    ): Pair<List<Row>, Int> {
        // Encodes the neighbourhood with the central cell located at coordinate
        fun encodeNeighbourhood(
            coordinate: Coordinate,
            row: IntArray? = null,
            index: Int = -1,
            partialKey: Int = -1,
            bcCoordinate: Coordinate? = null
        ): Int {
            var key = 0

            if (bcCoordinate == null) {
                var power = pow(rule.numStates, mainNeighbourhood.size)

                // Ignore cells that we know are background cells
                if (coordinate !in neighbourhoodWithoutBg) {
                    neighbourhoodWithoutBg[coordinate] = mainNeighbourhood.filter { (it, _) ->
                        if (symmetry == ShipSymmetry.ASYMMETRIC || symmetry == ShipSymmetry.GLIDE)
                            0 <= (it + coordinate).x && (it + coordinate).x < (width * spacing)
                        else (it + coordinate).x >= 0
                    }.map { (it, p) -> Pair(it + coordinate, p) }.toList()
                }

                // Optimisations to compute the neighbourhood for lookahead faster
                if (partialKey == -1) {
                    if (lookaheadDepth != 0) {
                        for ((it, p) in memorisedlookaheadNeighbourhood[lookaheadDepth - 1])
                            key += rows[it + coordinate, 0, row, depth] * p

                        if (approximateLookahead) {
                            for ((it, p) in lookaheadNeighbourhood[lookaheadDepth - 1])
                                key += rows[it + coordinate, 0, row, depth] * p

                            if (index != -1) lookaheadMemo!![index] = key
                        } else {
                            if (index != -1) lookaheadMemo!![index] = key

                            for ((it, p) in lookaheadNeighbourhood[lookaheadDepth - 1])
                                key += rows[it + coordinate, 0, row, depth] * p
                        }
                    } else neighbourhoodWithoutBg[coordinate]!!.forEach { (it, p) ->
                        key += rows[it, 0, row, depth] * p
                    }
                } else {
                    key = partialKey
                    if (!approximateLookahead) {
                        for ((it, p) in lookaheadNeighbourhood[lookaheadDepth - 1])
                            key += rows[it + coordinate, 0, row, depth] * p
                    }
                }

                // Adding current cell state & next cell state
                key += rows[coordinate, 0, row, depth] * power
                power *= rule.numStates

                key += rows[coordinate, 1, row, depth] * power
            } else {
                var power = 0
                bcNeighbourhood[memorisedBCsMap[bcCoordinate]!!].forEachIndexed { index, it ->
                    power = maxOf(power, pow(rule.numStates, index))
                    key += rows[it + bcCoordinate + coordinate, 0, row, depth] * pow(rule.numStates, index)
                }

                // Adding current cell state & next cell state if needed
                val centralCell = (-bcCoordinate.y != 0) && if (
                    bcCoordinate.x < baseCoordinates.last().x
                ) -bcCoordinate.x <= 0 else -bcCoordinate.x >= 0
                if (centralCell) {
                    key += rows[coordinate, 0, row, depth] * power
                    power *= rule.numStates

                    key += rows[coordinate, 1, row, depth, currentRow] * power
                }
            }

            return key
        }

        // Encodes the key used to query the inner lookup table
        fun encodeKey(coordinate: Coordinate, row: IntArray? = null): Int {
            var key = 0
            var power = 1
            for (it in reversedBaseCoordinate) {
                key += rule.equivalentStates[rows[it + coordinate, 0, row, depth]] * power
                power *= numEquivalentStates
            }

            return key
        }

        // Computing the lookup tables for the current row
        val memo = Array<IntArray?>(width + leftBC.size) { null }
        fun lookup(it: Int, row: IntArray? = null): IntArray {
            // Ensures that no effort is wasted if the row could never succeed
            if (memo[it] == null) {
                memo[it] = if (lookaheadMemo != null && lookaheadMemo[it] == -1) {
                    successorTable[
                        encodeNeighbourhood(
                            translate(Coordinate(it, 0), depth) - lastBaseCoordinate, row,
                            index = it,
                            partialKey = lookaheadMemo[it]
                        )
                    ]
                } else {
                    successorTable[
                        encodeNeighbourhood(
                            translate(Coordinate(it, 0), depth) - lastBaseCoordinate, row,
                            index = it,
                            partialKey = lookaheadMemo?.get(it) ?: -1
                        )
                    ]
                }
            }

            return memo[it]!!
        }

        // Running approximate lookahead for the current row
        // TODO fix this optimisation for diagonal ships
        val possibleSuccessorMemo = IntArray(width) { -1 }
        fun possibleSuccessors(it: Int): Int {
            if (possibleSuccessorMemo[it] == -1) {
                if (successorLookahead && lookaheadDepth == 0) {
                    val row = lookaheadRows.last()
                    val invert = symmetry != ShipSymmetry.GLIDE ||
                            (period.mod(2) == 0 && rows.last().phase == 0)
                    if (invert) return 3  // TODO fix this optimisation for glide-symmetric rules

                    val coordinate = translate(
                        Coordinate(if (invert) width - it - 1 else it, centralHeight), depth
                    )

                    if (approximateLookaheadTable != null) {
                        var key = 0
                        var power = 1
                        neighbourhood[0].forEach {
                            val temp = it + coordinate
                            val state = row[temp, 0, null, depth]
                            key += (if (state == -1) rule.numStates else state) * power
                            power *= (rule.numStates + 1)
                        }

                        val cellState = lookaheadRows.last()[coordinate, 0, null, depth]
                        key += cellState * power

                        possibleSuccessorMemo[it] = approximateLookaheadTable[key]
                    } else {
                        val neighbours = IntArray(neighbourhood[0].size) {
                            row[neighbourhood[0][it] + coordinate, 0, null, depth]
                        }
                        val cellState = row[coordinate, 0, null, depth]

                        possibleSuccessorMemo[it] = rule.transitionFuncWithUnknowns(
                            neighbours, cellState, 0, Coordinate()
                        )
                    }
                } else {
                    // Remember the row that evolved into this one
                    val prevRow = currentRow.getPredecessor(fwdOff[depth.mod(period)] - (depth - currentRow.depth))
                    if (prevRow != null) {
                        var output = 0
                        val array = rule.possibleSuccessors[0][prevRow.cells[it]]
                        for (i in array) output += pow(2, i)
                        possibleSuccessorMemo[it] = output
                    } else possibleSuccessorMemo[it] = (1 shl rule.numStates) - 1
                }
            }

            return possibleSuccessorMemo[it]
        }

        // Running another type of approximate lookahead
        val _lookaheadMemo: IntArray?
        val _lookaheadMemo2: IntArray?

        // Only initialise these if they will actually be used
        if (lookaheadDepth < this.lookaheadDepth) {
            _lookaheadMemo = IntArray(width + leftBC.size) { -1 }
            if (approximateLookahead) {
                _lookaheadMemo2 = IntArray(width + leftBC.size) { -1 }
            } else {
                _lookaheadMemo2 = null
            }
        } else {
            _lookaheadMemo = null
            _lookaheadMemo2 = null
        }

        val approximateLookaheadRows: List<Row?>
        val lookaheadDepthDiff = if (lookaheadDepth < this.lookaheadDepth) {
            approximateLookaheadRows = lookaheadRows[0]
            if (lookaheadDepth >= 1)
                lookaheadIndices[lookaheadDepth - 1][depth.mod(period)].filter { it > 0 }.min()
            else minIndices[depth.mod(period)]
        } else {
            approximateLookaheadRows = listOf()
            0
        }
        fun approximateLookahead(index: Int, row: Int): Boolean {
            val index = index - additionalDepthArray[depth.mod(spacing)]
            if (index < 0) return true

            val depth = depth + lookaheadDepthDiff
            val coordinate = translate(Coordinate(index, 0) - lastBaseCoordinate, depth)

            // Computing the lookahead neighbourhood
            var key = 0
            if (_lookaheadMemo!![index] == -1) {
                // TODO change depth to the correct depth
                for ((it, p) in memorisedlookaheadNeighbourhood[lookaheadDepth])
                    key += approximateLookaheadRows[it + coordinate, 0, null, depth] * p

                _lookaheadMemo[index] = key
            } else key = _lookaheadMemo[index]
            
            //println(lookaheadNeighbourhood[0].toList())
            for ((it, p) in lookaheadNeighbourhood[0]) {
                //println("$it ${pow(numEquivalentStates, (it.x + minX) / spacing)}")
                if ((it + coordinate).x >= 0)  // TODO consider different backgrounds
                    key += getDigit(
                        row,
                        pow(numEquivalentStates, (it.x + minX) / spacing), 
                        numEquivalentStates
                    ) * p
            }

            _lookaheadMemo2!![index] = key

            // Adding current cell state & next cell state
            var power = pow(rule.numStates, mainNeighbourhood.size)
            key += approximateLookaheadRows[coordinate, 0, null, depth] * power
            power *= rule.numStates

            key += approximateLookaheadRows[coordinate, 1, null, depth] * power
            return combinedSuccessorArray[key] and (1 shl minOf(index, reversedBaseCoordinate.size)) != 0
        }

        // Checks boundary conditions
        val bcMemo: HashMap<Coordinate, BooleanArray?> = hashMapOf()
        fun checkBoundaryCondition(node: Node, bcList: List<Coordinate>, offset: Coordinate = Coordinate()): Boolean {
            var satisfyBC = true
            val cells = node.completeRow

            bcList.forEach {
                if (!satisfyBC) return@forEach
                val coordinate = -it + offset

                // Do not consider boundary conditions if they do not check valid cells (for diagonal / oblique searches)
                val index: Int
                val tempCoordinate = coordinate + lastBaseCoordinate
                if (spacing != 1) {
                    val temp = coordinate.x - offsets[(depth - coordinate.y * period).mod(offsets.size)]
                    if (temp.mod(spacing) != 0) return@forEach

                    index = tempCoordinate.x / spacing
                } else index = tempCoordinate.x

                // Getting the boundary state
                if (it.y == -centralHeight) {
                    val lookupTable = lookup(index)
                    val boundaryState = rows[tempCoordinate, 0, cells, depth]

                    // Finally checking the boundary condition
                    if (((lookupTable[encodeKey(coordinate, cells)] shr boundaryState) and 0b1) != 1) {
                        satisfyBC = false
                        return@forEach
                    }
                } else {
                    if (it !in bcMemo)
                        bcMemo[it] = boundaryConditionTable[memorisedBCsMap[it]!!][
                            encodeNeighbourhood(coordinate, cells, bcCoordinate = it)
                        ]

                    satisfyBC = bcMemo[it]?.get(
                        inverseBcNeighbourhood[memorisedBCsMap[it]!!].mapIndexed { index, it ->
                            //println("$coordinate ${it + offset} ${offsets.toList()}")
                            rule.equivalentStates[
                                rows[it + offset, 0, cells, depth]
                            ] * pow(numEquivalentStates, index)
                        }.sum()
                    ) ?: true
                    return@forEach
                }
            }

            return satisfyBC
        }

        // Lookup table to prune and combine branches of search
        val table: Array<Array<IntArray>> = Array(this.lookaheadDepth - lookaheadDepth + 1) {
            Array(width + 1) {
                IntArray(pow(numEquivalentStates, maxWidth)) { -1 }
            }
        }

        // Prunes nodes that will reach a deadend
        fun pruneNodes(node: Node, newKey: Int): Boolean {
            var pruned = false
            for (i in 0..if (approximateLookahead) this.lookaheadDepth - lookaheadDepth else 0) {
                if (table[i][node.depth + 1][newKey.mod(cacheWidths[i])] == 0) {
                    pruned = true
                    break
                }
            }

            return pruned
        }

        // Indicates that this node leads to a deadend
        fun deadendNode(node: Node, x: Int) {
            val num = cacheWidths[x]
            node.applyOnPredecessor {
                val temp = it.cells.mod(num)
                if (table[x][it.depth][temp] == -1) {
                    table[x][it.depth][temp] = 0
                    true
                } else true
            }
        }

        // Indicates that this node can reach a completion
        fun completedNode(node: Node) {
            for (i in 0..if (approximateLookahead) this.lookaheadDepth - lookaheadDepth else 0)
                node.applyOnPredecessor {
                    val temp = it.cells.mod(cacheWidths[i])
                    if (table[i][it.depth][temp] != 1) {
                        table[i][it.depth][temp] = 1
                        true
                    } else true
                }
        }

        // Computing the initial key for the inner lookup table
        val key = 0  //encodeKey(-lastBaseCoordinate)

        // Finally running the search
        val completedRows = arrayListOf<Row>()
        val stack = arrayListOf(
            Node(
                null,
                key,
                key.mod(numEquivalentStates),
                0,
                rule.numStates,
                singleBaseCoordinate
            )
        )

        var maxDepth = 0  // Keeping track of maximum depth
        var depthToCheck = Int.MAX_VALUE - 1000  // Ignore all depths beyond this depth
        while (stack.isNotEmpty()) {
            val node = stack.removeLast()
            maxDepth = maxOf(maxDepth, node.depth)

            // If no cells are changed before depthToCheck, the row will be rejected by lookahead again
            if (
                symmetry != ShipSymmetry.GLIDE ||
                (period.mod(2) == 0 && rows.last().phase == 1)
            ) {
                if (depthToCheck + additionalDepthArray[depth.mod(spacing)] < node.depth) continue
                else depthToCheck = Int.MAX_VALUE - 1000

                // Run the approximate lookahead
                if (
                    this.approximateLookahead &&
                    lookaheadDepth < this.lookaheadDepth &&
                    !approximateLookahead(node.depth, node.cells.mod(cacheWidths[1]))
                ) {
                    deadendNode(node, 1)
                    continue
                }
            }

            // Check extra boundary conditions at the start
            if (node.depth == 1 && bcDepth == 1 && filteredRightBCs.isNotEmpty() &&
                !checkBoundaryCondition(node, filteredRightBCs)) continue

            // Stuff that is done at the end of the search
            if (node.depth == width) {
                // Telling algorithm which branches can be pruned and which branches can jump to the end
                completedNode(node.predecessor!!)

                if (
                    bcDepth != 1 && filteredRightBCs.isNotEmpty() &&
                    !checkBoundaryCondition(node, filteredRightBCs)
                ) continue

                if (checkBoundaryCondition(node, filteredLeftBCs, offset=Coordinate(width * spacing - 1, 0))) {
                    // Running the lookahead
                    val row = Row(currentRow, node.completeRow!!, this)
                    if (lookaheadDepth < this.lookaheadDepth) {
                        val newRows = lookaheadRows.mapIndexed { index, rows ->
                            val temp = lookaheadIndices[lookaheadDepth][(depth + 2).mod(period)].min()  // TODO may not be legit
                            val tempIndices = lookaheadIndices[index + lookaheadDepth][(depth + 2).mod(period)]
                            rows.mapIndexed { index, value -> if (tempIndices[index] == temp) row else value }
                        }

                        val (lookaheadOutput, temp) = nextRow(
                            row,
                            newRows.first() as List<Row>,
                            newRows.subList(1, newRows.size),
                            lookaheadDepth + 1,
                            if (approximateLookahead) _lookaheadMemo2 else _lookaheadMemo,
                            depth + lookaheadDepthDiff
                        )

                        if (approximateLookahead) {
                            for (i in width - 1 ..< width + leftBC.size)
                                _lookaheadMemo2!![i] = -1
                        }

                        if (lookaheadOutput.isEmpty()) depthToCheck = temp
                        else completedRows.add(row)
                    } else {
                        completedRows.add(row)
                        if (this.lookaheadDepth != 0) return Pair(completedRows, maxDepth)
                    }
                }

                continue
            }

            // Computing the next possible states
            var deadend = true
            val stateMask = lookup(node.depth)[node.cells.mod(cacheWidths[0])] and
                    possibleSuccessors(node.depth)
            val shifted = (node.cells * numEquivalentStates).mod(pow(numEquivalentStates, maxWidth))

            for (i in 0..<rule.numStates) {
                val newKey = shifted + rule.equivalentStates[i]
                if (
                    ((stateMask shr i) and 0b1) == 1 &&
                    (node.depth + 1 == width || !pruneNodes(node, newKey))
                ) {
                    // Adding the new nodes to the stack
                    deadend = false
                    stack.add(
                        Node(
                            node,
                            newKey, i,
                            node.depth + 1,
                            rule.numStates,
                            singleBaseCoordinate
                        )
                    )
                }
            }

            // Telling the algorithm to prune these branches if they are ever seen again
            if (deadend) deadendNode(node, 0)
        }

        // Add each row's successor num
        completedRows.forEachIndexed { index, it -> it.successorNum = index }
        return Pair(completedRows, maxDepth)
    }

    /**
     * Translates the [coordinate] at [depth] from the internal representation to the actual coordinate on the integer lattice
     */
    private fun translate(coordinate: Coordinate, depth: Int): Coordinate {
        if (spacing == 1) return coordinate
        else return Coordinate(coordinate.x * spacing + offsets[depth.mod(offsets.size)], coordinate.y)
    }

    private operator fun List<Row?>.get(
        coordinate: Coordinate, 
        generation: Int, 
        currentRow: IntArray? = null, 
        depth: Int = 0, 
        mostRecentRow: Row? = null
    ): Int {
        if (coordinate.x < 0) return 0  // TODO allow different backgrounds
        if (coordinate.x >= width * spacing) {
            return when (symmetry) {
                ShipSymmetry.ASYMMETRIC -> 0
                ShipSymmetry.GLIDE -> 0
                ShipSymmetry.EVEN -> this[Coordinate(2 * width * spacing - coordinate.x - 1, coordinate.y), generation, currentRow, depth]
                ShipSymmetry.ODD -> this[Coordinate(2 * width * spacing - coordinate.x - 2, coordinate.y), generation, currentRow, depth]
            }
        }

        if (coordinate.y == 0 && currentRow != null) {
            if (spacing != 1 && coordinate.x.mod(spacing) != offsets[depth.mod(offsets.size)]) {
                println("crap_bc $depth $coordinate ${coordinate.x.mod(spacing)} ${offsets[depth.mod(offsets.size)]}")
                return 0
            }
            return currentRow[coordinate.x / spacing]
        }
        return if (coordinate.y > 0 && coordinate.y < indexToRowMap.size) {
            if (generation == 0) {
                if (indexToRowMap[coordinate.y] != -1) {
                    this[indexToRowMap[coordinate.y] - 1]?.get(coordinate.x) ?: -1
                } else {  // TODO optimise this
                    this[0]?.getPredecessor((coordinate.y - 1) * period)?.get(coordinate.x) ?: -1
                }
            } else if (generation == 1) {
                // TODO optimise this
                val row = if (coordinate.y == centralHeight) this.last()!!
                else mostRecentRow!!.getPredecessor(coordinate.y * period - backOff[depth.mod(period)] - 1)!!

                if (
                    symmetry != ShipSymmetry.GLIDE ||
                    (period.mod(2) == 0 && this.last()!!.phase == 0)
                ) row[coordinate.x] else row[width * spacing - coordinate.x - 1]
            } else -1  // means that the cell state is not known
        } else -1
    }

    override fun println(x: Any, verbosity: Int) {
        if (verbosity <= this.verbosity && !stdin)
            t.println(x)
    }

    fun printRLE(
        grid: Grid, 
        verbosity: Int = 0, 
        style: TextStyle? = null, 
        print: Boolean = true,
        write: Boolean = true
    ): Int {
        var newLines = 1
        val rle = StringBuilder().apply {
            var delay = 0
            val string = grid.toRLE()
            for (i in string.indices) {
                append(string[i])
                if ((i - delay).mod(70) == 0 && i != 0) {
                    if (string[i].isDigit()) delay++
                    else {
                        if (!stdin) append("\n")
                        newLines++
                    }
                }
            }
        }.toString()

        if (rle == "!" && stdin) return 0
        if (verbosity <= this.verbosity && print) {
            if (style != null) t.println(style("x = 0, y = 0, rule = ${rule}\n$rle"))
            else t.println("x = 0, y = 0, rule = ${rule}\n$rle")
        }
        
        if (partialFileStreams.isNotEmpty() && write && rle != "!")
            partialFileStreams[Random.nextInt(partialFileStreams.size)].writeString(
                "x = 0, y = 0, rule = ${rule}\n${rle.replace("\n", "")}\n"
            )

        return newLines
    }
}

expect fun multithreadedDfs(cfind: CFind): Int

expect fun multithreadedPriorityQueue(cfind: CFind)

private fun getDigit(number: Int, power: Int, base: Int) = number.floorDiv(power).mod(base)

private fun pow(base: Int, exponent: Int): Int {
    if (exponent == 0) return 1
    val temp = pow(base, exponent / 2)
    return if (exponent % 2 == 0) temp * temp else base * temp * temp
}
