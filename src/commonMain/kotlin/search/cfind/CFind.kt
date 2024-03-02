package search.cfind

import com.github.ajalt.mordant.animation.animation
import com.github.ajalt.mordant.rendering.AnsiLevel
import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.rendering.TextStyles.*
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.widgets.progressLayout
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
    val period: Int,
    val k: Int,
    val width: Int,
    val symmetry: ShipSymmetry,
    val direction: Direction = Direction.NORTH,
    val maxQueueSize: Int = 1 shl 26,  // 2 ^ 20
    val minDeepeningIncrement: Int = 2,
    lookaheadDepth: Int = Int.MAX_VALUE,
    val numShips: Int = Int.MAX_VALUE,
    val partialFrequency: Int = 1000,
    verbosity: Int = 0
): SearchProgram(verbosity) {
    override val searchResults: MutableList<Pattern> = mutableListOf()

    // TODO Handle alternating stuff properly / don't support alternating neighbourhoods

    // Rotate the direction of the neighbour so the ship will go north
    val neighbourhood: List<List<Coordinate>> = rule.neighbourhood.map {
        it.map {
            when (direction) {
                Direction.SOUTH -> Coordinate(it.x, -it.y)
                Direction.EAST -> Coordinate(it.y, -it.x)
                Direction.WEST -> Coordinate(-it.y, it.x)
                else -> it
            }
        }.toList()
    }.toList()

    // Compute various statistics about the neighbourhood
    // TODO Get neighbourhood coordinate direction conventions right
    val height: Int = neighbourhood.maxOf { it.maxOf { it.y } - it.minOf { it.y } } + 1
    val centralHeight: Int = -neighbourhood.minOf { it.minOf { it.y } }

    val baseCoordinates: List<Coordinate> = neighbourhood[0].filter { it.y == -centralHeight }.sortedBy { it.x }
    val baseCoordinateMap: IntArray = baseCoordinates.map { neighbourhood[0].indexOf(it) }.toIntArray()

    val continuousBaseCoordinates: Boolean = (baseCoordinates.maxOf { it.x } -
            baseCoordinates.minOf { it.x } + 1) == baseCoordinates.size

    private val temp: Map<Int, Coordinate> = neighbourhood[0].map { it.x }.toSet().sorted().map { index ->
        Pair(index, neighbourhood[0].filter { it.x == index }.minBy { it.y })
    }.toMap()
    val combinedBC: List<Coordinate> = (temp.keys.min() .. temp.keys.max()).map {
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
    val leftBC: List<Coordinate> = combinedBC.filter { it.x < baseCoordinates.last().x }
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
    val indices = IntArray(height) {
        if (it < height - 1) (it + 1) * period
        else centralHeight * period - k
    }

    // double or even triple lookahead is possible for higher-range rules
    val maxLookaheadDepth = centralHeight - k.floorDiv(period)
    val lookaheadDepth = minOf(lookaheadDepth, maxLookaheadDepth)
    val lookaheadIndices = (0..<this.lookaheadDepth).map { depth ->
        indices.map { it - indices.sorted()[depth] }
    }

    val additionalDepth: Int = when (indices.indexOf(indices.min())) {
        0 -> neighbourhood[0].filter { it.y == baseCoordinates[0].y + 1 }.maxOf{ it.x } + 1 - baseCoordinates.last().x
        indices.size - 1 -> baseCoordinates.last().x - 1
        else -> -1
    }

    // Building lookup tables
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
        IntArray(pow(rule.numStates, baseCoordinates.size - 1)) {
            var power = 1
            for (i in 1 ..< baseCoordinates.size) {
                lst[baseCoordinateMap[baseCoordinateMap.size - i - 1]] = getDigit(it, power, rule.numStates)
                power *= rule.numStates
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

    override fun search() {
        println(bold("Beginning search for width ${green("$width")} spaceship moving towards ${green("$direction")} at " +
                "${green("${k}c/$period")}${if (rule is RuleFamily) " in ${green(rule.rulestring)}" else ""}..."))

        // Printing out some debugging information
        println(brightRed(bold("\nNeighbourhood\n----------------")), verbosity = 1)
        println((bold("Neighbourhood Height: ") + "$centralHeight / $height"), verbosity = 1)
        println((bold("Extra Boundary Conditions: ") + "$leftBC / $rightBC"), verbosity = 1)
        println((bold("Right BC Depth: ") + "$bcDepth"), verbosity = 1)
        println((bold("Base Coordinates: ") + "$baseCoordinates"), verbosity = 1)
        println((bold("Base Coordinate Map: ") + "${baseCoordinateMap.toList()}"), verbosity = 1)
        println((bold("Continuous Base Coordinates: ") + "$continuousBaseCoordinates"), verbosity = 1)
        println((bold("Additional Depth (for lookahead): ") + "$additionalDepth"), verbosity = 1)

        println(brightRed(bold("\nMisc\n----------------")), verbosity = 1)
        println((bold("Successor Table Size: ") + "${successorTable.size}"), verbosity = 1)
        println((bold("Maximum Lookahead Depth: ") + "$maxLookaheadDepth"), verbosity = 1)
        println((bold("Lookahead Depth: ") + "$lookaheadDepth"), verbosity = 1)
        println(
            (
                bold("Row Indices: ") +
                        "\n${indices.toList()}\n${lookaheadIndices.map { it.toList() }.joinToString("\n")}"
            ), verbosity = 1
        )

        // Initialising BFS queue with (height - 1) * period empty rows
        var currentRow = Row(null, IntArray(width) { 0 }, rule.numStates)
        for (i in 1 .. period * (height - 1)) {
            currentRow = Row(currentRow, IntArray(width) { 0 }, rule.numStates)
        }

        var queue: ArrayDeque<Row> = ArrayDeque(period * height)
        queue.add(currentRow)

        // Take note of the starting time
        val timeSource = TimeSource.Monotonic
        val startTime = timeSource.markNow()

        // Take note of number of ships found
        var shipsFound = 0

        // Main loop of algorithm
        var count = 0
        var clearPartial = false
        var clearLines = 0
        while (shipsFound < numShips) {
            // BFS round runs until the queue size exceeds the maximum queue size
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
                currentRow = queue.removeFirst()

                // Check if the ship is completed
                if (checkCompletedShip(currentRow)) {
                    clearPartial = false
                    if (++shipsFound == numShips) break
                }

                // Check the transposition table for looping components
                if (checkEquivalentState(currentRow)) continue

                // Get the rows that will need to be used to find the next row
                val (rows, lookaheadRows) = extractRows(currentRow)
                queue.addAll(nextRow(currentRow, rows, lookaheadRows).first)

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

            println(bold("\nBeginning depth-first search round, queue size ${queue.size} "))

            // DFS round runs for a certain deepening increment
            val t = Terminal(AnsiLevel.TRUECOLOR, interactive=true)
            val progress = progressLayout {
                text("DFS Progress:")
                progressBar()
                percentage()
                completed()
            }
            val animation = t.animation<Int> { progress.build(completed = it.toLong(), total = queue.size.toLong()) }

            count = 0
            val stack = arrayListOf<Row>()
            val newQueue = ArrayDeque<Row>(queue.size / 40)
            for (row in queue) {
                // Placing row within DFS stack
                stack.clear()
                stack.add(row)

                // Computing the depth that needs the row needs to be pruned until
                val maxDepth = row.prunedDepth + minDeepeningIncrement

                do {
                    // Get the current row that is going to be analysed
                    currentRow = stack.removeLast()

                    // Check if the ship is completed
                    checkCompletedShip(currentRow)

                    // Check the transposition table for looping components
                    if (checkEquivalentState(currentRow)) continue

                    // Get the rows that will need to be used to find the next row
                    val (rows, lookaheadRows) = extractRows(currentRow)
                    stack.addAll(nextRow(currentRow, rows, lookaheadRows).first)
                } while (stack.isNotEmpty() && currentRow.depth < maxDepth)

                animation.update(count++)

                if (stack.isNotEmpty()) newQueue.add(row)
            }

            queue = newQueue  // Replace the old queue
            print(bold("-> ${queue.size}"))
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
        indices.map { row.getPredecessor(it - 1)!! }.toList(),
        lookaheadIndices.map { it.map { row.getPredecessor(it - 1) }.toList() }
    )

    /**
     * Searches for a possible next row given the previous rows provided. Returns null if row cannot be found.
     */
    fun nextRow(
        currentRow: Row, rows: List<Row>, lookaheadRows: List<List<Row?>>, lookaheadDepth: Int = 0
    ): Pair<List<Row>, Int> {
        // Keeping track of time taken for each segment for profiling
        // val timeSource = TimeSource.Monotonic

        // Encodes the neighbourhood with the central cell located at coordinate
        fun encodeNeighbourhood(coordinate: Coordinate, row: IntArray? = null): Int {
//            var key = 0
//            var power = 1
//            neighbourhood[0].forEach {
//                if (it !in baseCoordinates) {
//                    key += rows[it + coordinate, 0, row] * power
//                    power *= rule.numStates
//                }
//            }
//
//            // Adding current cell state & next cell state
//            key += rows[coordinate, 0, row] * power
//            power *= rule.numStates
//
//            key += rows[coordinate, 1, row] * power
//            power *= rule.numStates

            var key = 0
            var power = pow(rule.numStates, mainNeighbourhood.size)

            if (coordinate !in neighbourhoodWithoutBg) {
                neighbourhoodWithoutBg[coordinate] = mainNeighbourhood.filter { (it, _) ->
                    if (symmetry == ShipSymmetry.ASYMMETRIC) (it + coordinate).x in 0..<width
                    else (it + coordinate).x >= 0
                }.toList()
            }

            neighbourhoodWithoutBg[coordinate]!!.forEach { (it, p) -> key += rows[it + coordinate, 0, row] * p }

            // Adding current cell state & next cell state
            key += rows[coordinate, 0, row] * power
            power *= rule.numStates

            key += rows[coordinate, 1, row] * power

            return key
        }

        // Encodes the key used to query the inner lookup table
        fun encodeKey(coordinate: Coordinate, row: IntArray? = null): Int {
            var key = 0
            var power = 1
            baseCoordinates.subList(0, baseCoordinates.size - 1).reversed().forEach {
                key += rows[it + coordinate, 0, row] * power
                power *= rule.numStates
            }

            return key
        }

        // Computing the lookup tables for the current row
        val memo = Array<IntArray?>(width + leftBC.size) { null }
        fun lookup(it: Int, row: IntArray? = null): IntArray {
            if (indices.last() == 0 || memo[it] == null) {  // Ensures that no effort is wasted if the row could never succeed
                // val startTime = timeSource.markNow()
                memo[it] = successorTable[encodeNeighbourhood(Coordinate(it, 0) - baseCoordinates.last(), row)]
                // println("Successor lookup performed in ${(timeSource.markNow() - startTime).inWholeNanoseconds}ns", verbosity = 2)
                return memo[it]!!
            } else {
                // val startTime = timeSource.markNow()
                val temp = memo[it]!!
                // println("Successor memorised lookup performed in ${(timeSource.markNow() - startTime).inWholeNanoseconds}ns", verbosity = 2)
                return temp
            }
        }

        // Checks boundary conditions
        fun checkBoundaryCondition(node: Node, bcList: List<Coordinate>, offset: Coordinate = Coordinate()): Boolean {
            // val startTime = timeSource.markNow()

            var satisfyBC = true
            val cells = node.completeRow.toIntArray()

            bcList.forEach {
                val coordinate = -it + offset
                val boundaryState = rows[coordinate + baseCoordinates.last(), 0, cells]
                val lookupTable = if (it in baseCoordinates) lookup(coordinate.x + baseCoordinates.last().x)
                else successorTable[encodeNeighbourhood(coordinate, cells)]

                if (((lookupTable[encodeKey(coordinate, cells)] shr boundaryState) and 0b1) != 1) {
                    satisfyBC = false
                    return@forEach
                }
            }

            // println("${bcList.size} BCs checked in ${(timeSource.markNow() - startTime).inWholeNanoseconds}ns", verbosity = 2)

            return satisfyBC
        }

        // Lookup table to prune and combine branches of search
        val table: Array<IntArray> = Array(width) {
            IntArray(pow(rule.numStates, baseCoordinates.size - 1)) { -1 }
        }

        // Computing the initial key for the inner lookup table
        val key = encodeKey(-baseCoordinates.last())

        // Finally running the search
        val completedRows = arrayListOf<Row>()
        val stack = ArrayList<Node>(10)
        stack.add(
            Node(
                null,
                key,
                key.mod(rule.numStates),
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

            if (depthToCheck + additionalDepth < node.depth) continue
            else depthToCheck = width

            // Check extra boundary conditions at the start
            if (rightBC.isNotEmpty() && bcDepth == 1 && node.depth == 1 && !checkBoundaryCondition(node, rightBC)) continue

            if (node.depth == width) {
                // Telling algorithm which branches can be pruned and which branches can jump to the end
                node.predecessor!!.applyOnPredecessor {
                    // if (table[it.depth][it.cells] == -1) {
                    //     table[it.depth][it.cells] = arrayListOf(node)
                    // } else table[it.depth][it.cells]!!.add(node)
                    table[it.depth][it.cells] = 1
                }

                if (bcDepth != 1 && rightBC.isNotEmpty() && !checkBoundaryCondition(node, rightBC)) continue

                val bcList = leftBC.reversed().subList(
                    0, if (symmetry != ShipSymmetry.ASYMMETRIC) baseCoordinates.last().x else leftBC.size
                )
                if (checkBoundaryCondition(node, bcList, offset=Coordinate(width - 1, 0))) {
                    val row = Row(currentRow, node.completeRow.toIntArray(), rule.numStates)
                    if (lookaheadDepth < this.lookaheadDepth) {
                        val newRows = lookaheadRows.mapIndexed { index, rows ->
                            val temp = lookaheadIndices[lookaheadDepth].min()
                            val tempIndices = lookaheadIndices[index + lookaheadDepth]
                            rows.mapIndexed { index, value -> if (tempIndices[index] == temp) row else value }
                        }

                        val (lookaheadOutput, temp) = nextRow(
                            row,
                            newRows.first() as List<Row>,
                            newRows.subList(1, newRows.size),
                            lookaheadDepth + 1
                        )

                        if (lookaheadOutput.isEmpty()) {
                            depthToCheck = temp
                        } else completedRows.add(row)
                    } else {
                        completedRows.add(row)
                        return Pair(completedRows, maxDepth)
                    }
                }

                continue
            }

            // Pruning branches that are known to be deadends
            // var startTime = timeSource.markNow()

            val finalNodes = table[node.depth][node.cells]
            if (finalNodes == 0) continue
            // else if (finalNodes != null) {
            //     finalNodes.forEach {
            //         val finalNode = it.changePredecessor(node)
            //         stack.add(finalNode)
            //     }
            //     continue
            // }

            // println("Branches pruned in ${(timeSource.markNow() - startTime).inWholeNanoseconds}ns", verbosity = 2)

            // Adding successors
            // startTime = timeSource.markNow()

            var deadend = true
            val row = (node.completeRow + List(width - node.depth) { -1 }).toIntArray()
            val shifted = (node.cells * rule.numStates).mod(pow(rule.numStates, baseCoordinates.size - 1))
            for (i in 0 ..< rule.numStates) {
                if (((lookup(node.depth, row)[if (baseCoordinates.size > 1) node.cells else 0] shr i) and 0b1) == 1) {
                    deadend = false
                    stack.add(
                        Node(
                            node,
                            if (continuousBaseCoordinates) (
                                    if (baseCoordinates.size > 1) shifted + i else 0
                            ) else encodeKey(
                                Coordinate(node.depth, 0) - baseCoordinates.last(),
                                node.completeRow.toIntArray()
                            ), i,
                            node.depth + 1,
                            rule.numStates,
                            baseCoordinates.size == 1
                        )
                    )
                }
            }

            // Telling the algorithm to prune these branches of they are ever seen again
            if (deadend) node.applyOnPredecessor {
                // if (table[it.depth][it.cells] == null)
                //    table[it.depth][it.cells] = ArrayList()
                if (table[it.depth][it.cells] == -1)
                    table[it.depth][it.cells] = 0
            }

            // println("Successor states added in ${(timeSource.markNow() - startTime).inWholeNanoseconds}ns", verbosity = 2)
            // println("\n-------------------------\n", verbosity = 2)
        }

        return Pair(completedRows, maxDepth)
    }

    private operator fun List<Row>.get(coordinate: Coordinate, generation: Int, currentRow: IntArray? = null): Int {
        if (coordinate.x < 0) return 0  // TODO allow different backgrounds
        if (coordinate.x >= width) {
            return when (symmetry) {
                ShipSymmetry.ASYMMETRIC -> 0
                ShipSymmetry.EVEN -> this[Coordinate(2 * width - coordinate.x - 1, coordinate.y), generation, currentRow]
                ShipSymmetry.ODD -> this[Coordinate(2 * width - coordinate.x - 2, coordinate.y), generation, currentRow]
                ShipSymmetry.GLIDE -> 0
            }
        }

        if (coordinate.y == 0 && currentRow != null) return currentRow[coordinate.x]
        return if (coordinate.y > 0 && coordinate.y < this.size) {
            if (generation == 0) this[coordinate.y - 1].cells[coordinate.x]
            else if (coordinate.y == centralHeight && generation == 1) this.last().cells[coordinate.x]
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