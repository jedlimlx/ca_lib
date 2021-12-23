package rules

import rules.hrot.HROT
import rules.hrot.HROTGenerations
import simulation.Grid

private val RULE_FAMILIES = listOf(HROT(), HROTGenerations())

/**
 * Creates a rule given its rulestring
 * @param rulestring The rulestring of the rule
 * @return Returns the rule corresponding to that rulestring
 */
fun fromRulestring(rulestring: String): RuleFamily {
    for (family in RULE_FAMILIES) {
        for (regex in family.regex) {
            if (rulestring.matches(regex)) return family.fromRulestring(rulestring)
        }
    }

    throw IllegalArgumentException("This rulestring is not valid!")
}

/**
 * The range of rules in which the provided evolution of patterns works in
 * @return Returns a pair of rules, the first is the minimum rule and the second is the maximum rule
 */
fun ruleRange(phases: List<Grid>): Pair<RuleFamily, RuleFamily> {
    // Obtain the transitions at the min and max rule must satisfy
    val transitionsToSatisfy = HashSet<List<Int>>(phases.fold(0) { acc, value -> acc + value.population })
    for (i in 0 until phases.size - 1) {
        // Adding in the background to ensure the background output will be correct
        transitionsToSatisfy.add(List(phases[i].neighbourhood.size + 2) {
            when (it) {
                0 -> phases[i].background
                1 -> phases[i + 1].background
                else -> phases[i].background
            }
        })

        // Adding in the other normal transitions
        phases[i].neighbours().forEach {
            transitionsToSatisfy.add(List(phases[i].neighbourhood.size + 2) { index ->
                when (index) {
                    0 -> phases[i][it]
                    1 -> phases[i + 1][it]
                    else -> phases[i][phases[i].neighbourhood[index - 2] + it]
                }
            })
        }
    }

    require(phases[0].rule is RuleFamily) { "Rule of the specified pattern does not support rule range" }
    return (phases[0].rule as RuleFamily).ruleRange(transitionsToSatisfy)
}

/**
 * The range of rules in which the provided evolution of patterns works in
 * @return Returns a pair of rules, the first is the minimum rule and the second is the maximum rule
 */
fun ruleRange(phases: Array<Grid>): Pair<RuleFamily, RuleFamily> = ruleRange(phases.toList())

/**
 * The range of rules in which the provided evolution of patterns works in
 * @param minRule The minimum rule of the rule range to enumerate
 * @param maxRule The maximum rule of the rule range to enumerate
 * @return Returns a sequence of all rules within the rule range
 */
fun enumerateRules(minRule: RuleFamily, maxRule: RuleFamily): Sequence<RuleFamily> {
    return minRule.enumerate(minRule, maxRule)
}

/**
 * An infinite sequence of random rules in which the provided evolution of patterns works in
 * @param minRule The minimum rule of the rule range to enumerate
 * @param maxRule The maximum rule of the rule range to enumerate
 * @return Returns an infinite sequence of random rules within the rule range
 */
fun randomRules(minRule: RuleFamily, maxRule: RuleFamily, seed: Int? = null): Sequence<RuleFamily> {
    return minRule.random(minRule, maxRule, seed)
}

/**
 * A random rule in which the provided evolution of patterns works in
 * @param minRule The minimum rule of the rule range to enumerate
 * @param maxRule The maximum rule of the rule range to enumerate
 * @return Returns a random rule within the rule range
 */
fun randomRule(minRule: RuleFamily, maxRule: RuleFamily, seed: Int? = null): RuleFamily {
    return minRule.random(minRule, maxRule, seed).take(1).toList()[0]
}
