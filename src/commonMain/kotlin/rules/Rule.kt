package rules

import Colour
import simulation.Coordinate
import simulation.Grid
import kotlin.math.pow


/**
 * Represents a cellular automaton rule.
 * Note that rules are supposed to be immutable and thus **should not** be modified after initialisation.
 * Doing so **will** result in undefined behaviour.
 */
abstract class Rule {
    /**
     * The number of states in the rule
     */
    open val numStates: Int = 2

    /**
     * The alternating period of the rule (excluding B0 emulation)
     */
    open val alternatingPeriod: Int = 1

    /**
     * The neighbourhood of the rule (excluding B0 emulation)
     */
    abstract val neighbourhood: Array<Array<Coordinate>>

    /**
     * The tiles around a given tile that can affect its next state
     */
    private val tileNeighbourhood by lazy {
        neighbourhood.map { neighbourhood ->
            val list = ArrayList<Coordinate>(36)
            for (coordinate in listOf(Coordinate(0, 0), Coordinate(1, 0), Coordinate(0, 1), Coordinate(1, 1))) {
                for (neighbour in neighbourhood) {
                    val neighbour2 = coordinate - neighbour
                    list.add(
                        Coordinate(
                            neighbour2.x - neighbour2.x.mod(2),
                            neighbour2.y - neighbour2.y.mod(2)
                        )
                    )
                }
            }

            list.toSet().toTypedArray()
        }.toTypedArray()
    }

    /**
     * The cells around a given tile that can affect its next state
     */
    private val tileNeighbourhood2 by lazy {
        neighbourhood.map { neighbourhood ->
            val list = ArrayList<Coordinate>(36)
            for (coordinate in listOf(Coordinate(0, 0), Coordinate(1, 0), Coordinate(0, 1), Coordinate(1, 1))) {
                for (neighbour in neighbourhood) {
                    val neighbour2 = coordinate - neighbour
                    list.add(neighbour2)
                }
            }

            list.toSet().toTypedArray()
        }.toTypedArray()
    }

    /**
     * The background that the rule cycles through (for B0 emulation)
     */
    open val background: IntArray by lazy {
        val bgList = arrayListOf(0)
        while (bgList.size == 1 || (bgList[bgList.size - 1] != bgList[0] && (bgList.size - 1) % alternatingPeriod == 0)) {
            bgList.add(
                transitionFunc(
                    IntArray(neighbourhood[(bgList.size - 1) % alternatingPeriod].size) { bgList[bgList.size - 1] },
                    bgList[bgList.size - 1],
                    neighbourhood[(bgList.size - 1) % alternatingPeriod].size, Coordinate()
                )
            )
        }

        bgList.slice(0 until bgList.size - 1).toIntArray()
    }

    /**
     * The colours representing the rule
     */
    open val colours: Array<Colour> by lazy {
        Array(numStates) {
            when (it) {
                0 -> Colour(0, 0, 0)
                1 -> if (numStates > 2) Colour(255, 0, 0) else Colour(255, 255, 255)
                else -> Colour(255, (it - 1) * 255 / (numStates - 2), 0)
            }
        }
    }

    /**
     * The lookup table for 2x2 tiles
     */
    @OptIn(ExperimentalUnsignedTypes::class)
    protected val lookup2x2 = ULongArray(2.0.pow(17).toInt()) { ULong.MAX_VALUE }

    /**
     * The possible successor cell states of each cell state
     */
    abstract val possibleSuccessors: Array<Array<IntArray>>

    /**
     * Shows which states are equivalent to with other states
     * (i.e. can be swapped around within a neighbourhood without changing the output)
     */
    abstract val equivalentStates: IntArray

    /**
     * The transition function of the rule
     * @param cells The cells surrounding the central cell (in the order specified by [neighbourhood])
     * @param cellState The state of the central cell
     * @param generation The generation of current simulation (for alternating rules)
     * @param coordinate The coordinate of the central cell (for rules that change based on parity, etc.)
     */
    abstract fun transitionFunc(cells: IntArray, cellState: Int, generation: Int, coordinate: Coordinate): Int

    /**
     * The set of all possible states the cell could be in given a certain neighbourhood \
     * with unknown cells marked as -1 encoded in binary
     * @param cells The cells surrounding the central cell (in the order specified by [neighbourhood])
     * @param cellState The state of the central cell
     * @param generation The generation of current simulation (for alternating rules)
     * @param coordinate The coordinate of the central cell (for rules that change based on parity, etc.)
     */
    abstract fun transitionFuncWithUnknowns(cells: IntArray, cellState: Int, generation: Int, coordinate: Coordinate): Int

    /**
     * Steps the grid forward by [generations] generations.
     * @param generations The number of generations to step the grid forward by
     * @return Returns the modified grid
     */
    open fun step(grid: Grid, generations: Int = 1) {
        for (i in 0 until generations) {
            val totalSize = grid.cellsChanged.fold(0) { acc, set -> acc + set.size }
            val neighbourhood: Array<Coordinate> = neighbourhood[grid.generation % alternatingPeriod]

            val cellsToCheck = HashSet<Coordinate>(totalSize)

            // Generate set of cells to run update function on
            // Use a set to avoid duplicate
            var neighbour: Coordinate
            for (cellSet in grid.cellsChanged) {
                for (cell in cellSet) {
                    for (neighbour2 in neighbourhood) {
                        neighbour = cell - neighbour2
                        cellsToCheck.add(neighbour)
                    }

                    cellsToCheck.add(cell)
                }
            }

            // Apply the transition function across all the cells
            var neighbours: IntArray
            val gridCopy = grid.deepCopy()

            // Update the background of the current grid
            grid.background = background[(grid.generation + 1) % background.size]

            // Run through all the cells that could change
            for (cell in cellsToCheck) {
                // Getting neighbours
                neighbours = neighbourhood.map { gridCopy[it + cell] }.toIntArray()

                // Update the value of the cell
                if (possibleSuccessors[grid.generation % alternatingPeriod][gridCopy[cell]].size == 1) {
                    grid[cell] = possibleSuccessors[grid.generation % alternatingPeriod][gridCopy[cell]][0]
                } else grid[cell] = transitionFunc(neighbours, gridCopy[cell], grid.generation, cell)

                // Check if the cell value changed
                if (Utils.convert(grid[cell], grid.background) == Utils.convert(gridCopy[cell], gridCopy.background)) {
                    for (j in background.indices) {
                        if (cell in grid.cellsChanged[j]) {
                            grid.cellsChanged[j].remove(cell)

                            // Move the cell forward into the next entry until it can't be moved forward anymore
                            if (j < background.size - 1) grid.cellsChanged[j + 1].add(cell)
                            break
                        }
                    }
                }
            }

            grid.generation++
        }
    }
}