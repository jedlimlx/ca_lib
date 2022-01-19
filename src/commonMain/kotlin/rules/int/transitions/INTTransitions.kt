package rules.int.transitions

import rules.ruleloader.ruletable.Symmetry
import simulation.Coordinate

/**
 * Represents a set of isotropic non-totalistic transitions
 */
abstract class INTTransitions {
    /**
     * The string representing the transition
     */
    val transitionString: String by lazy { canoniseTransition()  }

    /**
     * The symmetry of the transition
     */
    abstract val symmetry: Symmetry

    /**
     * The neighbourhood of the transition
     */
    abstract val neighbourhood: Array<Coordinate>

    /**
     * A regex that can be used to identify the transition
     */
    abstract val regex: Regex

    /**
     * A set containing all the transitions stored as a list of integers
     */
    abstract val transitions: Set<List<Int>>

    /**
     * Each individual transition expressed as a string
     */
    abstract val transitionStrings: Set<String>

    /**
     * Parses the given transition string
     * @param string The transition string to parse
     */
    protected abstract fun parseTransition(string: String)

    /**
     * Returns the canonised string representing the transition
     * @return Returns the canonised string representing the transition
     */
    protected abstract fun canoniseTransition(): String
}