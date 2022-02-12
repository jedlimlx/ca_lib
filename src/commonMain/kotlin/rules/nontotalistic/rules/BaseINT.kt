package rules.nontotalistic.rules

import rules.RuleFamily
import rules.nontotalistic.transitions.INTTransitions
import rules.nontotalistic.transitions.R1MooreINT

/**
 * A list of all types of non-totalistic transitions
 */
val INT_NEIGHBOURHOODS  = mapOf("M" to R1MooreINT(""))

/**
 * The base class for all non-totalistic rules
 */
abstract class BaseINT: RuleFamily() {
    /**
     * The string representing the neighbourhood of the isotropic non-totalistic rule
     */
    abstract val neighbourhoodString: String

    /**
     * Parses the given transition
     * @param transition The transition to parse
     * @return Returns the INT transition corresponding to the given transition
     */
    protected fun parseTransition(transition: String): INTTransitions {
        return when (neighbourhoodString) {
            "M" -> R1MooreINT(transition)
            else -> throw IllegalArgumentException("INT Neighbourhood identifier " +
                    "$neighbourhoodString is not supported.")
        }
    }

    /**
     * Canonises the given transition
     * @param transition The transition to canonise
     * @return Returns the canonised transition string
     */
    protected fun canoniseTransition(transition: INTTransitions): String = transition.transitionString
}