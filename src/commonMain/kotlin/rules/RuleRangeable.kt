package rules

/**
 * Represents rule families on which rule ranges can be defined. Should only be implemented by a RuleFamily [R].
 */
interface RuleRangeable<R> where R : RuleFamily, R : RuleRangeable<R> {
    /**
     * Checks if a rule is between 2 other rules
     * @param minRule The minimum rule
     * @param maxRule The maximum rule
     * @return Returns true if the rule is between the two rules, false otherwise
     */
    fun between(minRule: R, maxRule: R): Boolean

    /**
     * The range of rules in which the provided transitions will occur
     * @return Returns a pair of rules, the first is the minimum rule and the second is the maximum rule
     */
    fun ruleRange(transitionsToSatisfy: Iterable<List<Int>>): RuleRange<R>

    /**
     * Outputs a sequence containing all rules within the specified rule range
     * @param minRule The minimum rule of the rule range
     * @param maxRule The maximum rule of the rule range
     * @return Returns a sequence containing all rules within the specified rule range
     */
    fun enumerate(minRule: R, maxRule: R): Sequence<R>

    /**
     * Outputs an infinite sequence of random rules within the specified rule range
     * @param minRule The minimum rule of the rule range
     * @param maxRule The maximum rule of the rule range
     * @return Returns an infinite sequence of random rules
     */
    fun random(minRule: R, maxRule: R, seed: Int? = null): Sequence<R>

    /**
     * Computes the intersection between [ruleRange1] and [ruleRange2]
     * @param ruleRange1 One rule range to use in the intersection
     * @param ruleRange2 The other rule range to use in the intersection
     * @return A new rule range containining the rules in both [ruleRange1] and [ruleRange2]. Returns null if no intersection.
     */
    fun intersect(ruleRange1: RuleRange<R>, ruleRange2: RuleRange<R>): RuleRange<R>?

    /**
     * Outputs a sequence containing all rules within the specified rule range
     * @param maxRule The maximum rule of the rule range
     * @return Returns a sequence containing all rules within the specified rule range
     */
    operator fun rangeTo(maxRule: R): RuleRange<R>
}