package patterns

import rules.PLACEHOLDER_RULE
import rules.Rule
import rules.RuleRange

/**
 * Represents a special pattern in cellular automaton
 */
abstract class Pattern {
    /**
     * The name of the pattern
     */
    open var name: String = ""

    /**
     * The discoverer of the pattern
     */
    open var discoverer: String = ""

    /**
     * The rule the pattern can be found in
     */
    abstract val rule: Rule

    /**
     * The rule range in which the pattern functions
     */
    open val ruleRange: RuleRange<*>? = null

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