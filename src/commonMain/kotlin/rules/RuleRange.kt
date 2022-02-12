package rules

/**
 * Represents a range of rules between the minimum and maximum rules
 */
class RuleRange(val minRule: RuleFamily, val maxRule: RuleFamily): Sequence<RuleFamily> {
    private val enumerationIterator = minRule.enumerate(minRule, maxRule).iterator()

    /**
     * Generates a random rule with the given seed
     * @param seed The seed to use when generating the rule
     * @return Returns the generated rule
     */
    fun random(seed: Int? = null): RuleFamily = randomSequence(seed).first()

    /**
     * Generates an infinite sequence of random rules with the given seed
     * @param seed The seed to use when generating the rules
     * @return Returns the sequence of generated rules
     */
    fun randomSequence(seed: Int? = null): Sequence<RuleFamily> = minRule.random(minRule, maxRule)

    /**
     * Checks if the given rule is in the rule range
     * @param ruleFamily The rule to check
     * @return Returns true if the rule is within the rule range, false otherwise
     */
    operator fun contains(ruleFamily: RuleFamily): Boolean = ruleFamily.between(minRule, maxRule)

    override fun iterator(): Iterator<RuleFamily> = enumerationIterator
}
