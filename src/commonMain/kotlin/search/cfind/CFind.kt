package search.cfind

import LRUCache
import PLATFORM
import PriorityQueue
import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.rendering.TextStyle
import com.github.ajalt.mordant.rendering.TextStyles.bold
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.ByteStringBuilder
import kotlinx.io.bytestring.decodeToString
import kotlinx.io.bytestring.encodeToByteString
import kotlinx.serialization.json.Json
import patterns.Pattern
import patterns.Spaceship
import rules.Rule
import rules.RuleFamily
import search.SearchProgram
import simulation.Coordinate
import simulation.Grid
import kotlin.time.TimeSource

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
    val maxQueueSize: Int = 1 shl 20,
    minDeepeningIncrement: Int = -1,
    lookaheadDepth: Int = Int.MAX_VALUE,
    val searchStrategy: SearchStrategy = SearchStrategy.PRIORITY_QUEUE,
    val numShips: Int = Int.MAX_VALUE,
    val partialFrequency: Int = 1000,
    val backupFrequency: Int = 10,
    val numThreads: Int = 8,
    val stdin: Boolean = false,
    verbosity: Int = 0
): SearchProgram(verbosity) {
    override val searchResults: MutableList<Pattern> = mutableListOf()

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

        var count = 0
        for (i in 0..<period) {
            var index = 0
            while (this[(count + index + k).mod(period)] != -1) { index++ }

            this[count.mod(period)] = (index + k).mod(backoffPeriod)
            count += this[count.mod(period)]
        }

        this[count.mod(period)] = (period - count).mod(backoffPeriod)
    }
    val fwdOff = run {
        val array = IntArray(period) { -1 }
        backOff.forEachIndexed { index, it -> array[(it + index).mod(period)] = it }

        array
    }

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
    ) { -1 }.apply {
        if (symmetry != ShipSymmetry.GLIDE || direction != Coordinate(1, 1)) {
            for (i in 0 ..<k) {
                var count = i
                while (count < period * k) {
                    this[count] = tempOffsets[i.mod(tempOffsets.size)]
                    count += backOff[count.mod(period)]
                }
            }
        } else {
            for (i in 0..<period) this[i] = i.mod(2)
            for (i in period ..< period*2) this[i] = (i + 1).mod(2)
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

    val widthsByHeight: IntArray = IntArray(height) { height ->
        val minY = neighbourhood[0].minOf { it.y }
        if (neighbourhood[0].count { it.y == height + minY } > 0) {
            (neighbourhood[0].filter { it.y == height + minY }.maxOf { it.x } -  // ceiling division
                    neighbourhood[0].filter { it.y == height + minY }.minOf { it.x } + spacing) / spacing
        } else -1
    }

    private val temp: Map<Int, Coordinate> = neighbourhood[0].map { it.x }.toSet().sorted().map { index ->
        Pair(index, neighbourhood[0].filter { it.x == index }.minBy { it.y })
    }.toMap()
    val combinedBC: List<Coordinate> = (temp.keys.min() .. temp.keys.max() step spacing).map {
        if (it in temp) {
            if (it > temp.keys.min() && it < temp.keys.max()) {
                if (it < baseCoordinates.last().x) {
                    var i = 0
                    while((it - ++i) !in temp) continue
                    if (temp[it - i]!!.y < temp[it]!!.y) return@map Coordinate(it, temp[it - i]!!.y)
                    else return@map temp[it]!!
                } else {
                    var i = 0
                    while((it + ++i) !in temp) continue
                    if (temp[it + i]!!.y < temp[it]!!.y) return@map Coordinate(it, temp[it + i]!!.y)
                    else return@map temp[it]!!
                }
            }
        } else {  // TODO properly handle cases where temp[it + 1] or temp[it - 1] doesn't exist
            if (it < baseCoordinates.last().x) return@map Coordinate(it, temp[it - 1]!!.y)
            else return@map Coordinate(it, temp[it + 1]!!.y)
        }

        temp[it]!!
    }
    val leftBC: List<Coordinate> = combinedBC.filter { it.x < baseCoordinates.last().x }.reversed()
    val rightBC: List<Coordinate> = combinedBC.filter { it.x > baseCoordinates.last().x }.reversed()

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
    val equivalentStates: LRUCache<Int, IntArray> = LRUCache(1 shl 25)

    // Computing the rows that should be used in computing the next state
    val indices = Array(period) { phase ->
        IntArray(height) {
            if (it < height - 1) (it + 1) * period
            else centralHeight * period - backOff[phase]
        }
    }

    private val tempIndices = run {
        val lst = arrayListOf(indices)
        for (i in indices[0].indices) {
            lst.add(
                Array(period) { phase ->
                    val temp = lst.last()[phase].filter { it > 0 }.min()
                    val pos = lst.last()[phase].indexOf(temp)

                    if (pos < indices[0].size - 1) lst.last()[phase].map { it - temp }.toIntArray()
                    else {
                        val fwdOffset = fwdOff[(phase - temp).mod(period)]
                        lst.last()[phase].map {
                            if (it - temp != 0) it - temp + backOff[phase] - fwdOffset
                            else 0
                        }.toIntArray()
                    }
                }
            )
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

        output
    }  // double or even triple lookahead is possible for higher-range rules
    val lookaheadDepth = minOf(lookaheadDepth, maxLookaheadDepth)

    val successorLookaheadDepth = tempIndices.map { it[0].last() }.indexOf(0) + 1
    val successorLookahead = (successorLookaheadDepth == this.lookaheadDepth + 1)

    val lookaheadIndices = if (this.lookaheadDepth == 0) listOf() else tempIndices.subList(
        0, if (successorLookahead) successorLookaheadDepth else this.lookaheadDepth
    )

    val additionalDepth: Int = when (indices[0].indexOf(indices[0].min())) {
        0 -> neighbourhood[0].filter { it.y == baseCoordinates[0].y + 1 }.maxOf{ it.x } + 1 - baseCoordinates.last().x
        indices[0].size - 1 -> baseCoordinates.last().x - 1
        else -> -1
    }
    val maxWidth: Int = widthsByHeight.slice(0 .. this.lookaheadDepth).maxOf { it }

    val approximateLookahead = lookaheadIndices[0][0].last() != 0

    // Computing neighbourhoods to be memorised for lookahead
    val memorisedlookaheadNeighbourhood: List<List<Pair<Coordinate, Int>>> = lookaheadIndices.indices.map {
        if (lookaheadIndices[it][0].indexOf(lookaheadIndices[it][0].min()) == 0) {
            mainNeighbourhood.filter { (it, _) -> it.y > -centralHeight + 1 }
        } else {
            mainNeighbourhood
        }
    }.toList()
    val lookaheadNeighbourhood: List<List<Pair<Coordinate, Int>>> = lookaheadIndices.indices.map {
        if (lookaheadIndices[it][0].indexOf(lookaheadIndices[it][0].min()) == 0) {
            mainNeighbourhood.filter { (it, _) -> it.y == -centralHeight + 1 }
        } else {
            listOf()
        }
    }.toList()

    // Building lookup tables
    val numEquivalentStates: Int = rule.equivalentStates.distinct().size
    val equivalentStateSets: List<List<Int>> = run {
        val lst = List<ArrayList<Int>>(numEquivalentStates) { arrayListOf() }
        rule.equivalentStates.forEachIndexed { actualState, state -> lst[state].add(actualState) }

        lst
    }

    val cacheWidths: IntArray = IntArray(height) {
        pow(numEquivalentStates, widthsByHeight[it] - (if (it == 0) 1 else 0))
    }

    val successorTable: Array<IntArray> = Array(
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

    val combinedSuccessorArray: BooleanArray = BooleanArray(
        pow(rule.numStates, neighbourhood[0].size - baseCoordinates.size + 2)
    ) {
        var output = false
        for (i in successorTable[it].indices) {
            if (successorTable[it][i] != 0) {
                output = true
                break
            }
        }

        output
    }

    val approximateLookaheadTable: IntArray? = run {
        if (pow(rule.numStates + 1, neighbourhood[0].size + 1) < (1 shl 32)) {
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
        println(bold("Beginning search for width ${green("$width")} " +
                "spaceship with ${green("$symmetry")} symmetry moving towards ${green("$direction")} at " +
                "${green("${_k}c/$_period")}${if (rule is RuleFamily) " in ${green(rule.rulestring)}" else ""}..."))

        // Printing out some debugging information
        println(brightRed(bold("\nNeighbourhood\n----------------")), verbosity = 1)
        println((bold("Neighbourhood: ") + "${neighbourhood.toList()}"), verbosity = 1)
        println((bold("Neighbourhood Height: ") + "$centralHeight / $height"), verbosity = 1)
        println((bold("Extra Boundary Conditions: ") + "$leftBC / $rightBC"), verbosity = 1)
        println((bold("Right BC Depth: ") + "$bcDepth"), verbosity = 1)
        println((bold("Base Coordinates: ") + "$baseCoordinates"), verbosity = 1)
        println((bold("Base Coordinate Map: ") + "${baseCoordinateMap.toList()}"), verbosity = 1)
        println((bold("Continuous Base Coordinates: ") + "${reversedBaseCoordinate.toList()}"), verbosity = 1)
        println((bold("Additional Depth (for lookahead): ") + "$additionalDepth"), verbosity = 1)

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

        // Some common functions
        fun processSuccessors(successors: List<Row>): List<Row> = if (currentRow.successorSequence != null) {
            // This optimisation is possible because of the nature of depth-first search
            // The successful branch will lie in-between the unknown branches and the deadends
            val sequence = currentRow.successorSequence!!
            val index = sequence[0]
            if (sequence.size > 1) successors[index].successorSequence = sequence.copyOfRange(1, sequence.size)
            currentRow.successorSequence = null
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
                clearLines = printRLE(grid)
                clearPartial = true
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
                    val (rows, lookaheadRows) = extractRows(currentRow)
                    val successors = nextRow(currentRow, rows, lookaheadRows, depth = currentRow.depth + 1).first

                    // Adding the new rows to the linked list
                    val temp = processSuccessors(successors)
                    queueSize += temp.size
                    temp.forEach {
                        if (head == null) head = it

                        tail?.next = it
                        it.prev = tail
                        tail = it
                    }

                    // Printing out the partials
                    printPartials(bold("\nQueue Size: $queueSize / $maxQueueSize"))
                }

                if (shipsFound == numShips) break

                // Check how much time has past and see if we need to write to a backup
                if ((timeSource.markNow() - startTime).inWholeMilliseconds > (backups+1)*backupFrequency*1000) {
                    backupState("dump_${backups++}.txt", saveState())
                }

                // DFS round runs for a certain deepening increment
                val message = "Beginning depth-first search round, queue size $queueSize"
                println(bold("\n$message"))

                // Check how much time has past and see if we need to write to a backup
                if ((timeSource.markNow() - startTime).inWholeMilliseconds > (backups+1)*backupFrequency*1000) {
                    backupState("dump_${backups++}.txt", saveState())
                }

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
                        backupState("dump_${backups++}.txt", saveState())
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
    }

    override fun stop() {
        TODO("Not yet implemented")
    }

    override fun saveToFile(filename: String) {
        TODO("Not yet implemented")
    }

    override fun saveState(): String = StringBuilder().apply {
        val inQueue = HashSet<Long>(if (searchStrategy == SearchStrategy.PRIORITY_QUEUE) priorityQueue.size else queueSize)
        if (searchStrategy == SearchStrategy.PRIORITY_QUEUE)
            priorityQueue.forEach { inQueue.add(it.id) }
        else {
            var row = head
            while (row != null) {
                inQueue.add(row.id)
                row = row.next
            }
        }

        val added = HashSet<Long>(if (searchStrategy == SearchStrategy.PRIORITY_QUEUE) priorityQueue.size else queueSize)
        val queue = PriorityQueue<Row>(if (searchStrategy == SearchStrategy.PRIORITY_QUEUE) priorityQueue.size else queueSize)
        if (searchStrategy == SearchStrategy.PRIORITY_QUEUE) {
            for (row in priorityQueue) {
                row.successorSequence = null
                queue.add(row)
                added.add(row.id)
                row.applyOnPredecessor {
                    it.successorSequence = null
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
                    it.successorSequence = null
                    if (it.id !in added) {
                        queue.add(it)
                        added.add(it.id)
                    }
                }

                row = row.next
            }
        }

        append("${queue.size}\n")
        while (queue.isNotEmpty()) {
            val row = queue.poll()
            append("${row.id} ${row.predecessor?.id ?: -1} ${row.hashCode()}")
            if (row.id in inQueue) append(" *")
            append("\n")
        }
    }.toString()

    override fun loadState(string: String) {
        loadedState = true

        val lines = string.split("\n")

        val rows = HashMap<Long, Row>(lines[0].toInt())
        queueSize = 0
        for (i in 1 ..< lines.size) {
            val tokens = lines[i].split(" ")
            if (tokens.size < 3) continue

            val row = Row(
                rows[tokens[1].toLong()],
                tokens[2].toInt().toString(rule.numStates).padStart(width, '0').map { it.digitToInt() }.reversed().toIntArray(),
                this
            )
            rows[tokens[0].toLong()] = row

            if (tokens.size == 4) {
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
    fun checkEquivalentState(row: Row): Boolean {
        val rows = row.getAllPredecessors((height - 1) * period)
        if (rows.hashCode() in equivalentStates.keys) {
            var equivalent = true
            val state = equivalentStates.get(rows.hashCode())!!
            for (i in state.indices) {
                if (state[i] != rows[i].hashCode()) {
                    equivalent = false
                    break
                }
            }

            if (!equivalent) return false
        } else {
            equivalentStates.put(rows.hashCode(), rows.map { it.hashCode() }.toIntArray())
            return false
        }

        return true
    }

    /**
     * Extract the relevant rows that will be used in finding the next state given the current latest [row].
     * It will return a pair of rows - one for the regular successor search and the other for lookahead
     */
    fun extractRows(row: Row): Pair<List<Row>, List<List<Row?>>> = Pair(
        indices[(row.depth + 1).mod(period)].map { row.getPredecessor(it - 1)!! }.toList(),
        lookaheadIndices.map { it[(row.depth + 1).mod(period)].map { row.getPredecessor(it - 1) }.toList() }
    )

    /**
     * Searches for a possible next row given the previous rows provided. Returns null if row cannot be found.
     */
    fun nextRow(
        currentRow: Row?,
        rows: List<Row>,
        lookaheadRows: List<List<Row?>>,
        lookaheadDepth: Int = 0,
        lookaheadMemo: IntArray? = null,
        depth: Int = 0
    ): Pair<List<Row>, Int> {
        val rows2 = rows  // hacky workaround

        // Encodes the neighbourhood with the central cell located at coordinate
        fun encodeNeighbourhood(
            coordinate: Coordinate,
            row: IntArray? = null,
            index: Int = -1,
            partialKey: Int = -1,
            rows: List<Row?> = rows2
        ): Int {
            var key = 0
            var power = pow(rule.numStates, mainNeighbourhood.size)

            // Ignore cells that we know are background cells
            if (coordinate !in neighbourhoodWithoutBg) {
                neighbourhoodWithoutBg[coordinate] = mainNeighbourhood.filter { (it, _) ->
                    if (symmetry == ShipSymmetry.ASYMMETRIC || symmetry == ShipSymmetry.GLIDE)
                        (it + coordinate).x in 0..<width * spacing
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
                } else neighbourhoodWithoutBg[coordinate]!!.forEach { (it, p) -> key += rows[it, 0, row, depth] * p }
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

            return key
        }

        // Encodes the key used to query the inner lookup table
        fun encodeKey(coordinate: Coordinate, row: IntArray? = null, rows: List<Row?> = rows2): Int {
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
                if (lookaheadMemo != null && lookaheadMemo[it] == -1) {
                    memo[it] = successorTable[
                        encodeNeighbourhood(
                            translate(Coordinate(it, 0), depth) - lastBaseCoordinate, row,
                            index = it,
                            partialKey = lookaheadMemo[it]
                        )
                    ]
                } else memo[it] = successorTable[
                    encodeNeighbourhood(
                        translate(Coordinate(it, 0), depth) - lastBaseCoordinate, row,
                        index = it,
                        partialKey = lookaheadMemo?.get(it) ?: -1
                    )
                ]
                return memo[it]!!
            } else {
                val temp = memo[it]!!
                return temp
            }
        }

        // Running approximate lookahead for the current row
        // TODO fix this optimisation for diagonal ships
        val possibleSuccessorMemo = IntArray(width) { -1 }
        fun possibleSuccessors(it: Int): Int {
            if (possibleSuccessorMemo[it] == -1) {
                if (successorLookahead && lookaheadDepth == 0) {
                    val row = lookaheadRows.last()
                    val invert = symmetry != ShipSymmetry.GLIDE ||
                            (period.mod(2) == 0 && rows.last().phase.mod(period) == 0)
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
                    val prevRow = currentRow?.getPredecessor(fwdOff[depth.mod(period)] - 1)
                    if (prevRow != null && lookaheadDepth == 0) {
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
        val minX: Int

        // Only initialise these if they will actually be used
        if (lookaheadDepth < this.lookaheadDepth) {
            _lookaheadMemo = IntArray(width + leftBC.size) { -1 }
            if (approximateLookahead) {
                _lookaheadMemo2 = IntArray(width + leftBC.size) { -1 }
                minX = lookaheadNeighbourhood[0].minOf { (it, _) -> it.x }
            } else {
                _lookaheadMemo2 = null
                minX = 0
            }
        } else {
            _lookaheadMemo = null
            _lookaheadMemo2 = null
            minX = 0
        }

        val lookaheadDepthDiff = if (lookaheadDepth < this.lookaheadDepth)
            lookaheadIndices[lookaheadDepth][depth.mod(period)].min()
        else 0
        fun approximateLookahead(index: Int, row: Int): Boolean {
            val index = index - additionalDepth
            if (index < 0) return true

            val depth = depth - lookaheadDepthDiff
            val coordinate = translate(Coordinate(index, 0) - lastBaseCoordinate, depth)

            // Computing the lookahead neighbourhood
            var key = 0
            if (_lookaheadMemo!![index] == -1) {
                // TODO change depth to the correct depth
                for ((it, p) in memorisedlookaheadNeighbourhood[lookaheadDepth]) {
                    key += lookaheadRows[0][it + coordinate, 0, null, depth] * p
                }

                _lookaheadMemo[index] = key
            } else key = _lookaheadMemo[index]

            for ((it, p) in lookaheadNeighbourhood[0]) {
                if ((it + coordinate).x >= 0)  // TODO consider different backgrounds
                    key += getDigit(row, pow(numEquivalentStates, (it.x + minX) / spacing), numEquivalentStates) * p
                // TODO consider special case for diagonal / oblique ships
            }

            _lookaheadMemo2!![index] = key

            // Adding current cell state & next cell state
            var power = pow(rule.numStates, mainNeighbourhood.size)
            key += lookaheadRows[0][coordinate, 0, null, depth] * power
            power *= rule.numStates

            key += lookaheadRows[0][coordinate, 1, null, depth] * power
            return combinedSuccessorArray[key]
        }

        // Checks boundary conditions
        // TODO memorisation for boundary conditions
        fun checkBoundaryCondition(node: Node, bcList: List<Coordinate>, offset: Coordinate = Coordinate()): Boolean {
            if ((offset.x - offsets[(depth - offset.y * period).mod(offsets.size)]).mod(spacing) != 0) return true

            var satisfyBC = true
            val cells = node.completeRow

            bcList.forEach {
                val coordinate = -it + offset

                // Do not consider boundary conditions if they do not check valid cells (for diagonal / oblique searches)
                val index: Int
                val tempCoordinate = coordinate + lastBaseCoordinate
                if (spacing != 1) {
                    val temp = tempCoordinate.x - offsets[(depth - tempCoordinate.y * period).mod(offsets.size)]
                    if (temp.mod(spacing) != 0) return@forEach

                    index = temp / spacing
                } else index = tempCoordinate.x

                // Getting the boundary state
                val boundaryState = rows[tempCoordinate, 0, cells]
                val lookupTable = if (it in baseCoordinates) lookup(index)
                else successorTable[encodeNeighbourhood(coordinate, cells)]

                // Finally checking the boundary condition
                if (((lookupTable[encodeKey(coordinate, cells)] shr boundaryState) and 0b1) != 1) {
                    satisfyBC = false
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
                if (table[x][it.depth][it.cells.mod(num)] == -1)
                    table[x][it.depth][it.cells.mod(num)] = 0
            }
        }

        // Indicates that this node can reach a completion
        fun completedNode(node: Node) {
            for (i in 0..if (approximateLookahead) this.lookaheadDepth - lookaheadDepth else 0)
                node.applyOnPredecessor { table[i][it.depth][it.cells.mod(cacheWidths[i])] = 1 }
        }

        // Compute which boundary conditions need to be checked on the left side of the neighbourhood
        val bcList = leftBC.subList(
            0,
            if (symmetry != ShipSymmetry.ASYMMETRIC && symmetry != ShipSymmetry.GLIDE) lastBaseCoordinate.x
            else leftBC.size
        )

        // Computing the initial key for the inner lookup table
        val key = 0  //encodeKey(-lastBaseCoordinate)

        // Finally running the search
        val completedRows = arrayListOf<Row>()
        val stack = ArrayList<Node>(10)
        stack.add(
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
        var depthToCheck = width  // Ignore all depths beyond this depth
        while (stack.isNotEmpty()) {
            val node = stack.removeLast()
            maxDepth = maxOf(maxDepth, node.depth)

            // If no cells are changed before depthToCheck, the row will be rejected by lookahead again
            if (
                symmetry != ShipSymmetry.GLIDE ||
                (period.mod(2) == 0 && rows.last().phase.mod(period) == 1)
            ) {
                if (depthToCheck + additionalDepth < node.depth) continue
                else depthToCheck = width

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
            if (node.depth == 1 && bcDepth == 1 && rightBC.isNotEmpty() && !checkBoundaryCondition(node, rightBC)) continue

            // Stuff that is done at the end of the search
            if (node.depth == width) {
                // Telling algorithm which branches can be pruned and which branches can jump to the end
                completedNode(node.predecessor!!)

                if (bcDepth != 1 && rightBC.isNotEmpty() && !checkBoundaryCondition(node, rightBC)) continue

                if (checkBoundaryCondition(node, bcList, offset=Coordinate(width * spacing - 1, 0))) {
                    // Running the lookahead
                    val row = Row(currentRow, node.completeRow, this)
                    if (lookaheadDepth < this.lookaheadDepth) {
                        val newRows = lookaheadRows.mapIndexed { index, rows ->
                            val temp = lookaheadIndices[lookaheadDepth][depth.mod(period)].min()  // TODO may not be legit
                            val tempIndices = lookaheadIndices[index + lookaheadDepth][depth.mod(period)]
                            rows.mapIndexed { index, value -> if (tempIndices[index] == temp) row else value }
                        }

                        val (lookaheadOutput, temp) = nextRow(
                            row,
                            newRows.first() as List<Row>,
                            newRows.subList(1, newRows.size),
                            lookaheadDepth + 1,
                            if (approximateLookahead) _lookaheadMemo2 else _lookaheadMemo,
                            depth - lookaheadDepthDiff
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
     * Translates the [coordinate] from the internal representation to the actual coordinate on the integer lattice
     */
    private fun translate(coordinate: Coordinate, depth: Int): Coordinate {
        if (spacing == 1) return coordinate
        else return Coordinate(coordinate.x * spacing + offsets[depth.mod(offsets.size)], coordinate.y)
    }

    private operator fun List<Row?>.get(coordinate: Coordinate, generation: Int, currentRow: IntArray? = null, depth: Int = 0): Int {
        if (coordinate.x < 0) return 0  // TODO allow different backgrounds
        if (coordinate.x >= width * spacing) {
            return when (symmetry) {
                ShipSymmetry.ASYMMETRIC -> 0
                ShipSymmetry.GLIDE -> 0
                ShipSymmetry.EVEN -> this[Coordinate(2 * width * spacing - coordinate.x - 1, coordinate.y), generation, currentRow]
                ShipSymmetry.ODD -> this[Coordinate(2 * width * spacing - coordinate.x - 2, coordinate.y), generation, currentRow]
            }
        }

        if (coordinate.y == 0 && currentRow != null) {
            if (spacing != 1 && (coordinate.x - offsets[depth.mod(offsets.size)]).mod(spacing) != 0) return 0
            return currentRow[(coordinate.x - offsets[depth.mod(offsets.size)]) / spacing]
        }
        return if (coordinate.y > 0 && coordinate.y < this.size) {
            if (generation == 0) this[coordinate.y - 1]?.get(coordinate.x) ?: -1
            else if (coordinate.y == centralHeight && generation == 1) {
                if (
                    symmetry != ShipSymmetry.GLIDE ||
                    (period.mod(2) == 0 && this.last()!!.phase.mod(period) == 0)
                ) this.last()!![coordinate.x]
                else this.last()!![width * spacing - coordinate.x - 1]
            }
            else -1  // means that the cell state is not known
        } else -1
    }

    override fun println(x: Any, verbosity: Int) {
        if (verbosity <= this.verbosity && !stdin)
            t.println(x)
    }

    fun printRLE(grid: Grid, verbosity: Int = 0, style: TextStyle? = null): Int {
        var newLines = 1
        val rle = StringBuilder().apply {
            var delay = 0
            val string = grid.toRLE()
            for (i in string.indices) {
                append(string[i])
                if ((i - delay).mod(70) == 0 && i != 0) {
                    if (string[i].isDigit()) delay++
                    else {
                        append("\n")
                        newLines++
                    }
                }
            }
        }.toString()
        if (verbosity <= this.verbosity) {
            if (style != null) t.println(style("x = 0, y = 0, rule = ${rule}\n$rle"))
            else t.println("x = 0, y = 0, rule = ${rule}\n$rle")
        }

        return newLines
    }
}

expect fun multithreadedDfs(cfind: CFind): Int

expect fun multithreadedPriorityQueue(cfind: CFind)

expect fun backupState(filename: String, backup: String)

private fun getDigit(number: Int, power: Int, base: Int) = number.floorDiv(power).mod(base)

private fun pow(base: Int, exponent: Int): Int {
    if (exponent == 0) return 1
    val temp = pow(base, exponent / 2)
    return if (exponent % 2 == 0) temp * temp else base * temp * temp
}
