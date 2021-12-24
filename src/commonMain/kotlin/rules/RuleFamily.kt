package rules

import rules.ruleloader.Ruletable

/**
 * Represents a family of cellular automaton rules
 */
abstract class RuleFamily : Rule() {
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
     * Creates a rule with the given rulestring
     * @param rulestring The rulestring of the rule
     * @return Returns the rule corresponding to that rulestring
     */
    internal abstract fun fromRulestring(rulestring: String): RuleFamily

    /**
     * The range of rules in which the provided transitions will occur
     * @return Returns a pair of rules, the first is the minimum rule and the second is the maximum rule
     */
    internal abstract fun ruleRange(transitionsToSatisfy: Iterable<List<Int>>): Pair<RuleFamily, RuleFamily>

    /**
     * Outputs a sequence containing all rules within the specified rule range
     * @param minRule The minimum rule of the rule range
     * @param maxRule The maximum rule of the rule range
     * @return Returns a sequence containing all rules within the specified rule range
     */
    internal abstract fun enumerate(minRule: RuleFamily, maxRule: RuleFamily): Sequence<RuleFamily>

    /**
     * Outputs an infinite sequence of random rules within the specified rule range
     * @param minRule The minimum rule of the rule range
     * @param maxRule The maximum rule of the rule range
     * @return Returns an infinite sequence of random rules
     */
    internal abstract fun random(minRule: RuleFamily, maxRule: RuleFamily, seed: Int? = null): Sequence<RuleFamily>

    /**
     * Outputs a sequence containing all rules within the specified rule range
     * @param maxRule The maximum rule of the rule range
     * @return Returns a sequence containing all rules within the specified rule range
     */
    operator fun rangeTo(maxRule: RuleFamily) = enumerate(this, maxRule)

    /**
     * Generates a ruletable for the rule which can be used in Golly / Apgsearch
     * @return Returns the ruletable
     */
    abstract fun generateRuletable(): Ruletable

    /**
     * Converts the rule into a string (in this case it returns a rulestring)
     * @return Returns the rulestring of the rule
     */
    override fun toString(): String {
        return rulestring
    }
}