package rules

import simulation.Coordinate


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
     * The possible successor cell states of each cell state
     */
    abstract val possibleSuccessors: Array<Array<IntArray>>

    /**
     * The transition function of the rule
     * @param cells The cells surrounding the central cell (in the order specified by [neighbourhood])
     * @param cellState The state of the central cell
     * @param generation The generation of current simulation (for alternating rules)
     * @param coordinate The coordinate of the central cell (for rules that change based on parity, etc.)
     */
    abstract fun transitionFunc(cells: IntArray, cellState: Int, generation: Int, coordinate: Coordinate): Int
}