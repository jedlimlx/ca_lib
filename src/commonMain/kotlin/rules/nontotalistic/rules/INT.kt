package rules.nontotalistic.rules

import rules.RuleFamily
import rules.nontotalistic.transitions.INTTransitions
import rules.ruleloader.builders.ruletable
import simulation.Coordinate

class INT : BaseINT {
    val birth: INTTransitions
    val survival: INTTransitions

    override val neighbourhood: Array<Array<Coordinate>>
    override val neighbourhoodString: String

    override val possibleSuccessors: Array<Array<IntArray>> = arrayOf(arrayOf(intArrayOf(0, 1), intArrayOf(0, 1)))

    override val regex: List<Regex> by lazy {
        INT_NEIGHBOURHOODS.map { (key, entry) ->
            Regex("[Bb]${entry.regex}/?[Ss]${entry.regex}(/?[Nn]($key|${key.lowercase()}))?")
        }
    }

    constructor(birth: INTTransitions, survival: INTTransitions, neighbourhoodString: String) {
        this.birth = birth
        this.survival = survival
        this.neighbourhoodString = neighbourhoodString

        require(neighbourhoodString in INT_NEIGHBOURHOODS) { "INT Neighbourhood identifier " +
            "$neighbourhoodString is not supported." }
        neighbourhood = arrayOf(INT_NEIGHBOURHOODS[neighbourhoodString]!!.neighbourhood)
    }

    constructor(rulestring: String) {
        // Get the neighbourhood string
        neighbourhoodString = Regex("/?[Nn]?(${INT_NEIGHBOURHOODS.keys.map {
            listOf(it.lowercase(), it.uppercase())
        }.flatten().joinToString("|")})$").find(rulestring)?.groupValues?.get(1) ?: "M"

        // Load in the neighbourhood
        require(neighbourhoodString in INT_NEIGHBOURHOODS) { "INT Neighbourhood identifier " +
                "$neighbourhoodString is not supported." }
        neighbourhood = arrayOf(INT_NEIGHBOURHOODS[neighbourhoodString]!!.neighbourhood)

        // Load in the birth / survival conditions
        val string = INT_NEIGHBOURHOODS[neighbourhoodString]!!.regex.pattern

        "[Bb](($string)*)"  // This useless line makes the unit tests pass, don't question
        birth = parseTransition(Regex("[Bb](($string)*)").find(rulestring)!!.groupValues[1])
        survival = parseTransition(Regex("[Ss](($string)*)").find(rulestring)!!.groupValues[1])
    }

    override fun canoniseRulestring(): String = "B${birth.transitionString}/S${survival.transitionString}" +
            if (neighbourhoodString != "M") "/N$neighbourhoodString" else ""

    override fun fromRulestring(rulestring: String): INT = INT(rulestring)

    override fun between(minRule: RuleFamily, maxRule: RuleFamily): Boolean {
        if (minRule !is INT || maxRule !is INT) return false
        return birth.containsAll(minRule.birth) && survival.containsAll(minRule.survival) &&
                maxRule.birth.containsAll(birth) && maxRule.survival.containsAll(survival)
    }

    override fun ruleRange(transitionsToSatisfy: Iterable<List<Int>>): Pair<RuleFamily, RuleFamily> {
        // The minimum transitions
        val minBirth = hashSetOf<String>()
        val minSurvival = hashSetOf<String>()

        // The maximum transitions
        val maxTransition = parseTransition((0 .. neighbourhood[0].size).joinToString(""))
        val maxBirth = maxTransition.transitionStrings.toHashSet()
        val maxSurvival = maxTransition.transitionStrings.toHashSet()

        transitionsToSatisfy.forEach {
            val transition = it.subList(2, it.size)
            val string = birth.stringFromTransition(transition)

            when {
                it[0] == 0 && it[1] == 0 -> maxBirth.remove(string)  // No birth
                it[0] == 0 && it[1] == 1 -> minBirth.add(string)  // Birth
                it[0] == 1 && it[1] == 0 -> maxSurvival.remove(string)  // No survival
                it[0] == 1 && it[1] == 1 -> minSurvival.add(string) // Survival
            }
        }

        val minRule = INT(
            fromStringTransitions(neighbourhoodString, minBirth),
            fromStringTransitions(neighbourhoodString, minSurvival),
            neighbourhoodString
        )
        val maxRule = INT(
            fromStringTransitions(neighbourhoodString, maxBirth),
            fromStringTransitions(neighbourhoodString, maxSurvival),
            neighbourhoodString
        )

        return Pair(minRule, maxRule)
    }

    override fun enumerate(minRule: RuleFamily, maxRule: RuleFamily): Sequence<RuleFamily> {
        require(minRule is INT && maxRule is INT) { "minRule and maxRule must be an instance of INT" }

        // Get the difference between the birth and survival transitions of the min and max rules
        val birthDiff = (maxRule.birth.transitionStrings - minRule.birth.transitionStrings).toList()
        val survivalDiff = (maxRule.survival.transitionStrings - minRule.survival.transitionStrings).toList()

        val stack = arrayListOf(Pair(minRule, 0))  // Emulate a recursion stack
        return sequence {
            while (stack.isNotEmpty()) {
                val (rule, index) = stack.removeAt(stack.lastIndex)

                if (index == birthDiff.size + survivalDiff.size) yield(rule)  // Base case
                else {
                    // Add the transition to the rule
                    val newRule = if (index < birthDiff.size)
                        INT(rule.birth + setOf(birthDiff[index]), rule.survival, neighbourhoodString)
                    else
                        INT(
                            rule.birth, rule.survival + setOf(survivalDiff[index - birthDiff.size]),
                            neighbourhoodString
                        )

                    // 2 cases -> transition added and transition not added
                    stack.add(Pair(newRule, index + 1))
                    stack.add(Pair(rule, index + 1))
                }
            }
        }
    }

    override fun random(minRule: RuleFamily, maxRule: RuleFamily, seed: Int?): Sequence<RuleFamily> {
        require(minRule is INT && maxRule is INT) { "minRule and maxRule must be an instance of INT" }

        return generateSequence {
            val randomBirth = randomTransition(minRule.birth, maxRule.birth, seed)
            val randomSurvival = randomTransition(minRule.survival, maxRule.survival, seed)

            INT(randomBirth, randomSurvival, neighbourhoodString)
        }
    }

    override fun generateRuletable() = ruletable {
        name = rulestring.replace("/", "_")
        table(neighbourhood = neighbourhood[0], background = background) {
            variable("any") { 0 .. 1 }

            comment("Birth")
            intTransition {
                input = "0"
                output = "1"

                transition(birth.transitionString)
            }

            comment("Survival")
            intTransition {
                input = "1"
                output = "1"

                transition(survival.transitionString)
            }

            comment("Everything else dies")
            transition { "1 ${"any ".repeat(neighbourhood.size)}0" }
        }

        colours(numStates, background) { colours[it] }
    }

    override fun transitionFunc(cells: IntArray, cellState: Int, generation: Int, coordinate: Coordinate): Int {
        return when (cellState) {
            0 -> if (cells in birth) 1 else 0
            else -> if (cells in survival) 1 else 0
        }
    }
}