package rules

/**
 * Represents a family of cellular automaton rules
 */
abstract class RuleFamily: Rule() {
    /**
     * The rulestring of the rule
     */
    val rulestring: String by lazy { canoniseRulestring() }

    /**
     * Various regex expressions that will match a rulestring for this rule family.
     * Used to differentiate the rulestrings of different rule families.
     */
    abstract val regex: List<Regex>

    /**
     * Outputs the canonical rulestring of the rule
     * @return Returns the canonical rulestring of the rule
     */
    protected abstract fun canoniseRulestring(): String

    /**
     * Converts the rule into a string (in this case it returns a rulestring)
     * @return Returns the rulestring of the rule
     */
    override fun toString(): String {
        return rulestring
    }
}