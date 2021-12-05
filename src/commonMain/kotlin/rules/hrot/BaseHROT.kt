package rules.hrot

import NEIGHBOURHOOD_SYMBOLS
import parseCoordCA
import parseSymbol
import parseWeights
import rules.RuleFamily
import simulation.Coordinate

/**
 * The base class for HROT rules
 */
abstract class BaseHROT: RuleFamily() {
    /**
     * The weights of the HROT rule
     */
    abstract val weights: IntArray?

    /**
     * Reads the neighbourhood specifier in the HROT rulestring
     * @param range The range of the neighbourhood
     * @param neighbourhood The neighbourhood specifier in the HROT rulestring
     * @return Returns the neighbourhood as an array of coordinates
     */
    protected fun readNeighbourhood(range: Int, neighbourhood: String): Pair<Array<Coordinate>, IntArray?> {
        return when (neighbourhood[0]) {
            '@' -> return Pair(parseCoordCA(range, neighbourhood.substring(1)), null)
            'W' -> return parseWeights(range, neighbourhood.substring(1))
            else -> parseSymbol(range, neighbourhood[0])
        }
    }

    /**
     * Reads the HROT / OT syntax for transitions
     * @param transition The transition specifier from the rulestring
     * @param higherRange Is the transition specifier from a higher range rule?
     * @return Returns the transitions
     */
    protected fun readTransition(transition: String, higherRange: Boolean = true): Set<Int> {
        return if (higherRange) {  // HROT Syntax
            val set = hashSetOf<Int>()
            transition.split(",").forEach {
                if ("-" in it) {
                    val tokens = it.split("-")
                    for (i in tokens[0].toInt() .. tokens[1].toInt()) set.add(i)
                } else if (it.isNotEmpty()) set.add(it.toInt())
            }

            set
        } else HashSet(transition.map { it.toString().toInt() })
    }

    /**
     * Generates the canonical transition specification for the rule
     * @param transitions The transitions
     */
    protected fun canoniseTransition(transitions: Iterable<Int>): String {
        val sortedTransitions = transitions.sorted()

        // Code that I totally didn't steal from somewhere
        var idx = 0
        var idx2 = 0
        val len: Int = sortedTransitions.size

        val rulestring = StringBuilder()
        while (idx < len) {
            while (++idx2 < len && sortedTransitions[idx2] - sortedTransitions[idx2 - 1] == 1);
            if (idx2 - idx > 1) {
                rulestring.append("${sortedTransitions[idx]}-${sortedTransitions[idx2 - 1]},")
                idx = idx2
            } else {
                while (idx < idx2) {
                    rulestring.append("${sortedTransitions[idx]},")
                    idx++
                }
            }
        }

        if (rulestring.isEmpty()) rulestring.append(",")
        return rulestring.toString()
    }

    companion object {
        const val transitionRegex = "(((\\d,(?=\\d))|(\\d-(?=\\d))|\\d)+)?"
        const val neighbourhoodRegex = "(,N(@([A-Fa-f0-9]+)?[HL]?|W[A-Fa-f0-9]+[HL]?|[$NEIGHBOURHOOD_SYMBOLS]))?"
    }
}