package rules.nontotalistic.rules

import rules.RuleFamily
import rules.nontotalistic.transitions.INTTransitions
import rules.nontotalistic.transitions.R1MooreINT
import kotlin.random.Random

/**
 * A list of all types of non-totalistic transitions
 */
val INT_NEIGHBOURHOODS  = mapOf("M" to R1MooreINT(""))

/**
 * Parses the given isotropic non-totalistic transition string
 * @param transition The transition string to parse
 * @return Returns the isotropic non-totalistic transition corresponding to the given transition string
 */
fun parseTransition(neighbourhoodString: String = "M", transition: String): INTTransitions {
    return when (neighbourhoodString) {
        "M" -> R1MooreINT(transition)
        else -> throw IllegalArgumentException("INT Neighbourhood identifier " +
                "$neighbourhoodString is not supported.")
    }
}

/**
 * Loads the given transitions into a isotropic non-totalistic transition object
 * @param transitions The transitions to load
 * @return Returns the isotropic non-totalistic transition object corresponding to the given transitions
 */
fun fromTransitions(neighbourhoodString: String = "M", transitions: Iterable<List<Int>>): INTTransitions {
    return when (neighbourhoodString) {
        "M" -> R1MooreINT(transitions)
        else -> throw IllegalArgumentException("INT Neighbourhood identifier " +
                "$neighbourhoodString is not supported.")
    }
}

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
     * @return Returns the isotropic non-totalistic transition corresponding to the given transition
     */
    protected fun parseTransition(transition: String): INTTransitions = parseTransition(neighbourhoodString, transition)

    /**
     * Canonises the given transition
     * @param transition The transition to canonise
     * @return Returns the canonised transition string
     */
    protected fun canoniseTransition(transition: INTTransitions): String = transition.transitionString

    /**
     * Generates a random transition between within the given transition range
     * @param minTransition The minimum transition
     * @param maxTransition The maximum transition
     * @param seed The random seed to use
     * @return Returns a random transition within the given transition range
     */
    protected fun randomTransition(minTransition: INTTransitions, maxTransition: INTTransitions, seed: Int? = null): INTTransitions {
        val random = if (seed != null) Random(seed) else Random

        val diff = maxTransition.transitionStrings - minTransition.transitionStrings

        val newTransition = StringBuilder(minTransition.transitionString)
        diff.forEach { if (Random.nextBoolean()) newTransition.append(it) }

        return parseTransition(newTransition.toString())
    }
}