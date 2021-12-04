package patterns

import rules.Rule

/**
 * Represent a special pattern in cellular automaton (also it is meant to be immutable)
 */
abstract class Pattern {
    /**
     * The minimum rule in which the pattern functions
     */
    open val minRule: Rule? = null

    /**
     * The maximum rule in which the pattern functions
     */
    open val maxRule: Rule? = null

    /**
     * Some information about the pattern to display
     */
    abstract val information: Map<String, String>

    /**
     * A string summarising the key features of the pattern
     */
    open val summary: String by lazy {
        "Summary:\n" + information.map {
            it.key + ": " + it.value
        }.joinToString("\n")
    }
}