package search.cfind

import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.rendering.TextStyles
import com.github.ajalt.mordant.rendering.TextStyles.bold
import patterns.Pattern
import patterns.Spaceship
import rules.Rule
import rules.RuleFamily
import search.SearchProgram
import simulation.Coordinate
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
    val maxQueueSize: Int = 1 shl 20,  // 2 ^ 20
    val minDeepeningIncrement: Int = 2,
    lookaheadDepth: Int = Int.MAX_VALUE,
    val dfs: Boolean = false,
    val numShips: Int = Int.MAX_VALUE,
    val partialFrequency: Int = 1000,
    verbosity: Int = 0
): SearchProgram(verbosity) {
    override val searchResults: MutableList<Pattern> = mutableListOf()

    // TODO Handle alternating stuff properly / don't support alternating neighbourhoods

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

    // Computing the backOff array to be used when gcd(k, period) > 1
    val backOff = IntArray(period) { -1 }.apply {
        this[0] = k

        var count = 0
        for (i in 0..<period) {
            var index = 0
            while (this[(count + index + k).mod(period)] != -1) { index++ }

            this[count.mod(period)] = (index + k).mod(period)
            count += this[count.mod(period)]
        }

        this[count.mod(period)] = (period - count).mod(period)
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

    val reversedBaseCoordinate: List<Coordinate> = run {
        (  // we need to fill in the gaps between the base coordinates
            (baseCoordinates.minOf { it.x } ..< baseCoordinates.maxOf { it.x } step spacing).reversed()
        ).map { Coordinate(it, -centralHeight) }.toList()
    }

    private val temp: Map<Int, Coordinate> = neighbourhood[0].map { it.x }.toSet().sorted().map { index ->
        Pair(index, neighbourhood[0].filter { it.x == index }.minBy { it.y })
    }.toMap()
    val combinedBC: List<Coordinate> = (temp.keys.min() .. temp.keys.max() step spacing).map {
        if (it in temp) {  // TODO properly handle cases where temp[it + 1] or temp[it - 1] doesn't exist
            if (it > temp.keys.min() && it < temp.keys.max()) {
                if (it < baseCoordinates.last().x) {
                    if (temp[it - 1]!!.y < temp[it]!!.y) return@map Coordinate(it, temp[it - 1]!!.y)
                    else return@map temp[it]!!
                } else {
                    if (temp[it + 1]!!.y < temp[it]!!.y) return@map Coordinate(it, temp[it + 1]!!.y)
                    else return@map temp[it]!!
                }
            }
        } else {
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
    val equivalentStates: HashMap<Int, List<Row>> = hashMapOf()

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
                    //println(lst.last()[phase].toList())
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

    val approximateLookaheadDepth = tempIndices.map { it[0].last() }.indexOf(0) + 1
    val approximateLookahead = (approximateLookaheadDepth == this.lookaheadDepth + 1)

    val lookaheadIndices = if (this.lookaheadDepth == 0) listOf() else tempIndices.subList(
        0, if (approximateLookahead) approximateLookaheadDepth else this.lookaheadDepth
    )

    val additionalDepth: Int = when (indices[0].indexOf(indices[0].min())) {
        0 -> neighbourhood[0].filter { it.y == baseCoordinates[0].y + 1 }.maxOf{ it.x } + 1 - baseCoordinates.last().x
        indices[0].size - 1 -> baseCoordinates.last().x - 1
        else -> -1
    }

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

    val approximateLookaheadTable: IntArray = IntArray(
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
        val set = rule.transitionFuncWithUnknowns(lst, currentState, 0, Coordinate(0, 0))

        var output = 0
        for (i in 0 ..< rule.numStates) {
            if (i in set) output += pow(2, i)
        }

        output
    }

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
        println((bold("Approximate Lookahead: ") + "$approximateLookaheadDepth / $approximateLookahead"), verbosity = 1)
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
        var currentRow = Row(null, IntArray(width) { 0 }, this)
        for (i in 1 .. period * (height - 1)) {
            currentRow = Row(currentRow, IntArray(width) { 0 }, this)
        }

        var queue: ArrayDeque<Row> = ArrayDeque(maxQueueSize)
        queue.add(currentRow)

        // Take note of the starting time
        val timeSource = TimeSource.Monotonic
        val startTime = timeSource.markNow()

        // Take note of number of ships found
        var shipsFound = 0

        // Main loop of algorithm
        var count = 0
        var clearPartial: Boolean
        var clearLines = 0
        while (shipsFound < numShips) {
            // BFS round runs until the queue size exceeds the maximum queue size
            clearPartial = false
            while (queue.size < maxQueueSize) {
                if (queue.isEmpty()) {
                    println(
                        bold(
                            "\nSearch terminated in ${green("${(timeSource.markNow() - startTime).inWholeMilliseconds / 1000.0}s")}. " +
                                    "${green("$shipsFound")} ship${if (shipsFound == 1) "" else "s"} found."
                        )
                    )
                    return
                }

                // Get the current row that is going to be analysed
                if (dfs) currentRow = queue.removeLast()
                else currentRow = queue.removeFirst()

                // Check if the ship is completed
                if (checkCompletedShip(currentRow)) {
                    clearPartial = false
                    if (++shipsFound == numShips) break
                }

                // Check the transposition table for looping components
                if (checkEquivalentState(currentRow)) continue

                // Get the rows that will need to be used to find the next row
                val (rows, lookaheadRows) = extractRows(currentRow)
                queue.addAll(nextRow(currentRow, rows, lookaheadRows, depth = currentRow.depth + 1).first)

                if ((count++).mod(partialFrequency) == 0) {
                    val grid = currentRow.toGrid(period, symmetry)
                    grid.rule = rule

                    if (verbosity >= 0 && clearPartial) {
                        t.cursor.move {
                            up(3 + clearLines)
                            startOfLine()
                            clearScreenAfterCursor()
                        }
                        t.cursor.hide(showOnExit = true)
                    }

                    val rle = grid.toRLE().chunked(70)
                    clearLines = rle.size

                    println(bold("\nQueue Size: ${queue.size} / $maxQueueSize"))
                    println("x = 0, y = 0, rule = ${rule}\n" + rle.joinToString("\n"))
                    clearPartial = true
                }
            }

            if (shipsFound == numShips) break

            val message = "Beginning depth-first search round, queue size ${queue.size}"
            println(bold("\n$message"))

            // DFS round runs for a certain deepening increment
            count = 0
            clearPartial = false

            val stack = arrayListOf<Row>()
            val newQueue = ArrayDeque<Row>(maxQueueSize)
            for (row in queue) {
                // Placing row within DFS stack
                stack.clear()
                stack.add(row)

                // Computing the depth that needs the row needs to be pruned until
                val maxDepth = row.prunedDepth + minDeepeningIncrement

                do {
                    if (stack.isEmpty()) break

                    // Get the current row that is going to be analysed
                    currentRow = stack.removeLast()
                    if (currentRow.depth == maxDepth) {
                        row.prunedDepth = maxDepth
                        newQueue.add(Row(row.predecessor, row.cells.toList().toIntArray(), this))
                        break
                    }

                    // Get the rows that will need to be used to find the next row
                    val (rows, lookaheadRows) = extractRows(currentRow)
                    stack.addAll(nextRow(currentRow, rows, lookaheadRows, depth = currentRow.depth + 1).first)
                } while (true)

                if ((count++).mod(partialFrequency) == 0) {
                    val grid = currentRow.toGrid(period, symmetry)
                    grid.rule = rule

                    if (verbosity >= 0 && clearPartial) {
                        t.cursor.move {
                            up(3 + clearLines)
                            startOfLine()
                            clearScreenAfterCursor()
                        }
                        t.cursor.hide(showOnExit = true)
                    }

                    val rle = grid.toRLE().chunked(70)
                    clearLines = rle.size

                    println(bold(
                        "\nChecked ${count - 1} / $maxQueueSize rows, pruned ${(1000 - (newQueue.size * 1000 / count)) / 10.0}%"
                    ))
                    println("x = 0, y = 0, rule = ${rule}\n" + rle.joinToString("\n"))
                    clearPartial = true
                }
            }

            if (verbosity >= 0 && clearPartial) {
                t.cursor.move {
                    up(4 + clearLines)
                    startOfLine()
                    clearScreenAfterCursor()
                }
                t.cursor.hide(showOnExit = true)
            }

            queue.clear()  // Clear the old queue
            queue = newQueue  // Replace the old queue
            println(bold("$message -> ${queue.size}"))
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

    /**
     * Checks if the ship is completed.
     */
    fun checkCompletedShip(row: Row): Boolean {
        if (row.completeShip((height - 1) * period) == 1) {
            val grid = row.toGrid(period, symmetry)
            grid.rule = rule

            println(brightRed(bold("\nShip found!")))
            println(brightBlue(bold("x = 0, y = 0, rule = ${rule}\n" + grid.toRLE().chunked(70).joinToString("\n"))))

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
        if (rows.hashCode() !in equivalentStates.keys || equivalentStates[rows.hashCode()]!! != rows) {
            equivalentStates[rows.hashCode()] = rows
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
                    memorisedlookaheadNeighbourhood[lookaheadDepth - 1].forEach {
                            (it, p) -> key += rows[it + coordinate, 0, row, depth] * p
                    }
                    if (index != -1) lookaheadMemo!![index] = key

                    lookaheadNeighbourhood[lookaheadDepth - 1].forEach {
                            (it, p) -> key += rows[it + coordinate, 0, row, depth] * p
                    }
                } else neighbourhoodWithoutBg[coordinate]!!.forEach { (it, p) -> key += rows[it, 0, row, depth] * p }
            } else {
                key = partialKey
                lookaheadNeighbourhood[lookaheadDepth - 1].forEach { (it, p) ->
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
            reversedBaseCoordinate.forEach {
                key += rule.equivalentStates[rows[it + coordinate, 0, row, depth]] * power
                power *= numEquivalentStates
            }

            return key
        }

        // Computing the lookup tables for the current row
        val memo = Array<IntArray?>(width + leftBC.size) { null }
        val _lookaheadMemo = IntArray(width + leftBC.size) { -1 }
        fun lookup(it: Int, row: IntArray? = null): IntArray {
            // Ensures that no effort is wasted if the row could never succeed
            if (memo[it] == null) {
                if (lookaheadMemo != null && lookaheadMemo[it] == -1) {
                    memo[it] = successorTable[
                        encodeNeighbourhood(
                            translate(Coordinate(it, 0), depth) - baseCoordinates.last(), row,
                            index = it,
                            partialKey = lookaheadMemo[it]
                        )
                    ]
                } else memo[it] = successorTable[
                    encodeNeighbourhood(
                        translate(Coordinate(it, 0), depth) - baseCoordinates.last(), row,
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
        val approximateLookaheadMemo = IntArray(width) { -1 }
        fun approximateLookahead(it: Int): Int {
            if (approximateLookaheadMemo[it] == -1) {
                if (approximateLookahead && lookaheadDepth == 0) {  // TODO depth here is used wrongly
                    val coordinate = translate(Coordinate(it, centralHeight), depth)

                    var key = 0
                    var power = 1
                    neighbourhood[0].forEach {
                        val temp = it + coordinate
                        val state = lookaheadRows.last()[temp, 0, null, depth]
                        key += (if (state == -1) rule.numStates else state) * power
                        power *= (rule.numStates + 1)
                    }

                    val cellState = lookaheadRows.last()[coordinate, 0, null, depth]
                    key += cellState * power

                    approximateLookaheadMemo[it] = approximateLookaheadTable[key]
                } else {
                    // Remember the row that evolved into this one
                    val prevRow = currentRow?.getPredecessor(fwdOff[depth.mod(period)] - 1)
                    if (prevRow != null) {
                        var output = 0
                        val array = rule.possibleSuccessors[0][prevRow.cells[it]]
                        for (i in array) output += pow(2, i)
                        approximateLookaheadMemo[it] = output
                    } else approximateLookaheadMemo[it] = 1 shl rule.numStates
                }
            }

            return approximateLookaheadMemo[it]
        }

        // Checks boundary conditions
        fun checkBoundaryCondition(node: Node, bcList: List<Coordinate>, offset: Coordinate = Coordinate()): Boolean {
            if ((offset.x - offsets[(depth - offset.y * period).mod(offsets.size)]).mod(spacing) != 0) return true

            var satisfyBC = true
            val cells = node.completeRow

            bcList.forEach {
                val coordinate = -it + offset

                val tempCoordinate = coordinate + baseCoordinates.last()
                val temp = tempCoordinate.x - offsets[(depth - tempCoordinate.y * period).mod(offsets.size)]
                if (temp.mod(spacing) != 0) return@forEach

                val boundaryState = rows[coordinate + baseCoordinates.last(), 0, cells]
                val lookupTable = if (it in baseCoordinates) lookup(temp / spacing)
                else successorTable[encodeNeighbourhood(coordinate, cells)]

                if (((lookupTable[encodeKey(coordinate, cells)] shr boundaryState) and 0b1) != 1) {
                    satisfyBC = false
                    return@forEach
                }
            }

            return satisfyBC
        }

        // Lookup table to prune and combine branches of search
        val table: Array<IntArray> = Array(width) { IntArray(pow(rule.numStates, reversedBaseCoordinate.size)) { -1 } }

        // Computing the initial key for the inner lookup table
        val key = encodeKey(-baseCoordinates.last())

        // Finally running the search
        var completedRows = arrayListOf<Row>()
        val stack = ArrayList<Node>(10)
        stack.add(
            Node(
                null,
                key,
                key.mod(numEquivalentStates),
                0,
                rule.numStates,
                baseCoordinates.size == 1
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
            }

            // Check extra boundary conditions at the start
            if (node.depth == 1 && bcDepth == 1 && rightBC.isNotEmpty() && !checkBoundaryCondition(node, rightBC)) continue

            // Stuff that is done at the end of the search
            if (node.depth == width) {
                // Telling algorithm which branches can be pruned and which branches can jump to the end
                node.predecessor!!.applyOnPredecessor { table[it.depth][it.cells] = 1 }

                if (bcDepth != 1 && rightBC.isNotEmpty() && !checkBoundaryCondition(node, rightBC)) continue

                val bcList = leftBC.subList(
                    0,
                    if (symmetry != ShipSymmetry.ASYMMETRIC && symmetry != ShipSymmetry.GLIDE) baseCoordinates.last().x
                    else leftBC.size
                )

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
                            row,  // TODO apply possible successors optimisation to lookahead as well
                            newRows.first() as List<Row>,
                            newRows.subList(1, newRows.size),
                            lookaheadDepth + 1,
                            _lookaheadMemo,
                            depth - lookaheadIndices[lookaheadDepth][depth.mod(period)].min()
                        )

                        if (lookaheadOutput.isEmpty()) {
                            depthToCheck = maxOf(depthToCheck, temp)
                        } else completedRows.add(row)
                    } else {
                        completedRows.add(row)
                        if (this.lookaheadDepth != 0) return Pair(completedRows, maxDepth)
                    }
                }

                continue
            }

            // Pruning branches that are known to be deadends
            val finalNodes = table[node.depth][node.cells]
            if (finalNodes == 0) continue

            var deadend = true
            val row = node.completeRow
            val possibleSuccessors = approximateLookahead(node.depth)
            val shifted = (node.cells * numEquivalentStates).mod(pow(numEquivalentStates, reversedBaseCoordinate.size))

            // TODO Fix possible successors
            for (i in 0..<rule.numStates) {
                val newKey = if (baseCoordinates.size > 1) shifted + rule.equivalentStates[i] else 0
                val stateMask = lookup(node.depth, row)[if (baseCoordinates.size > 1) node.cells else 0] and possibleSuccessors
                if (
                    ((stateMask shr i) and 0b1) == 1 &&
                    (node.depth + 1 == width || table[node.depth + 1][newKey] != 0)
                ) {
                    deadend = false
                    stack.add(
                        Node(
                            node,
                            newKey, i,
                            node.depth + 1,
                            rule.numStates,
                            baseCoordinates.size == 1
                        )
                    )
                }
            }

            // Telling the algorithm to prune these branches if they are ever seen again
            if (deadend) node.applyOnPredecessor {
                if (table[it.depth][it.cells] == -1)
                    table[it.depth][it.cells] = 0
            }
        }

        return Pair(completedRows, maxDepth)
    }

    /**
     * Translates the [coordinate] from the internal representation to the actual coordinate on the integer lattice
     */
    private fun translate(coordinate: Coordinate, depth: Int) =
        Coordinate(coordinate.x * spacing + offsets[depth.mod(offsets.size)], coordinate.y)

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
            if ((coordinate.x - offsets[depth.mod(offsets.size)]).mod(spacing) != 0) return 0
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
}

private fun getDigit(number: Int, power: Int, base: Int) = number.floorDiv(power).mod(base)

private fun pow(base: Int, exponent: Int): Int {
    if (exponent == 0) return 1
    val temp = pow(base, exponent / 2)
    return if (exponent % 2 == 0) temp * temp else base * temp * temp
}