package rules.ruleloader

import simulation.Coordinate

/**
 * Represents a directive that encodes information about rule transitions in a ruletable
 */
abstract class RuleDirective(name: String) : Directive(name) {
    /**
     * The number of states in the rule
     */
    abstract val numStates: Int

    /**
     * The neighbourhood of the rule (excluding B0 emulation)
     */
    abstract val neighbourhood: Array<Coordinate>

    /**
     * The background that the rule cycles through (for B0 emulation)
     */
    abstract val background: IntArray

    /**
     * The transition function of the rule
     * @param cells The cells surrounding the central cell (in the order specified by [neighbourhood])
     * @param cellState The state of the central cell
     * @param generation The generation of current simulation (for alternating rules)
     * @param coordinate The coordinate of the central cell (for rules that change based on parity, etc.)
     */
    abstract fun transitionFunc(cells: IntArray, cellState: Int): Int
}