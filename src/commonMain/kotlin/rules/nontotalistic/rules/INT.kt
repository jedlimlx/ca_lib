package rules.nontotalistic.rules

import rules.RuleFamily
import rules.RuleRange
import rules.RuleRangeable
import rules.nontotalistic.transitions.DoubleLetterTransitions
import rules.nontotalistic.transitions.INTTransitions
import rules.nontotalistic.transitions.SingleLetterTransitions
import rules.ruleloader.builders.ruletable
import simulation.Coordinate

/**
 * Represents a 2-state isotropic non-totalistic (INT) rule
 */
class INT : BaseINT, RuleRangeable<INT> {
    /**
     * The birth transitions of the INT rule
     */
    val birth: INTTransitions

    /**
     * The survival transitions of the INT rule
     */
    val survival: INTTransitions

    override val neighbourhood: Array<Array<Coordinate>>
    override val neighbourhoodString: String

    override val possibleSuccessors: Array<Array<IntArray>> = arrayOf(arrayOf(intArrayOf(0, 1), intArrayOf(0, 1)))

    override val regex: List<Regex> by lazy {
        INT_NEIGHBOURHOODS.map { (key, entry) ->
            Regex("[Bb]${entry.regex}*/?[Ss]${entry.regex}*(/?[Nn]($key|${key.lowercase()}))?")
        }
    }

    override val equivalentStates: IntArray = intArrayOf(0, 1)

    constructor(birth: INTTransitions, survival: INTTransitions, neighbourhoodString: String) {
        this.birth = birth
        this.survival = survival
        this.neighbourhoodString = neighbourhoodString

        require(neighbourhoodString in INT_NEIGHBOURHOODS) { "INT Neighbourhood identifier " +
            "$neighbourhoodString is not supported." }
        neighbourhood = arrayOf(INT_NEIGHBOURHOODS[neighbourhoodString]!!.neighbourhood)
    }

    constructor(rulestring: String = "B2n3/S23-q") {
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

    override fun between(minRule: INT, maxRule: INT): Boolean {
        return birth.containsAll(minRule.birth) && survival.containsAll(minRule.survival) &&
                maxRule.birth.containsAll(birth) && maxRule.survival.containsAll(survival)
    }

    override fun ruleRange(transitionsToSatisfy: Iterable<List<Int>>): RuleRange<INT> {
        // The minimum transitions
        val minBirth = hashSetOf<String>()
        val minSurvival = hashSetOf<String>()

        // The maximum transitions
        val maxTransition = parseTransition((0 .. neighbourhood[0].size).map {
            if (birth is DoubleLetterTransitions) it.toString() + "x" else it
        }.joinToString())
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

        return minRule .. maxRule
    }

    override fun rangeTo(maxRule: INT): RuleRange<INT> = RuleRange(this, maxRule)

    override fun enumerate(minRule: INT, maxRule: INT): Sequence<INT> {
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

    override fun random(minRule: INT, maxRule: INT, seed: Int?): Sequence<INT> {
        return generateSequence {
            val randomBirth = randomTransition(minRule.birth, maxRule.birth, seed)
            val randomSurvival = randomTransition(minRule.survival, maxRule.survival, seed)

            INT(randomBirth, randomSurvival, neighbourhoodString)
        }
    }

    override fun intersect(ruleRange1: RuleRange<INT>, ruleRange2: RuleRange<INT>): RuleRange<INT>? {
        TODO("Not yet implemented")
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

    override fun transitionFuncWithUnknowns(cells: IntArray, cellState: Int, generation: Int, coordinate: Coordinate): Int {
        val output = IntArray(2) { 0 }

        // Enumerate all possible configurations of the unknown cells
        val unknownNum = cells.count { it == -1 }
        val cellsCopy = cells.toList().toIntArray()
        for (i in 0 .. (1 shl unknownNum)) {
            var count = 0
            val string = i.toString(2).padStart(unknownNum, '0')

            // Replacing the '-1's with the respective cells
            for (j in cellsCopy.indices) {
                if (cells[j] == -1)
                    cellsCopy[j] = string[count++].digitToInt()
            }

            // Ask the transition function what the output state will be
            output[transitionFunc(cellsCopy, cellState, generation, coordinate)] = 1
            if (output.sum() == 2) break  // Quit if all possible output states have been reached
        }

        var num = 0
        if (output[0] == 1) num += 1 shl 0
        if (output[1] == 1) num += 1 shl 1

        return num
    }
}