package rules.nontotalistic.rules

import rules.RuleRange
import rules.RuleRangeable
import rules.nontotalistic.transitions.DoubleLetterTransitions
import rules.nontotalistic.transitions.INTTransitions
import rules.ruleloader.builders.ruletable
import simulation.Coordinate

/**
 * Represents an isotropic non-totalistic (INT) generations rule
 */
class INTGenerations : BaseINT, RuleRangeable<INTGenerations> {
    /**
     * The birth conditions of the INT generations rule
     */
    val birth: INTTransitions

    /**
     * The survival conditions of the INT generations rule
     */
    val survival: INTTransitions

    override val numStates: Int
    override val neighbourhood: Array<Array<Coordinate>>
    override val neighbourhoodString: String

    override val possibleSuccessors: Array<Array<IntArray>>
    override val equivalentStates: IntArray

    override val regex: List<Regex> by lazy {
        INT_NEIGHBOURHOODS.map { (key, entry) ->
            Regex("[BbSs]?${entry.regex}*/[BbSs]?${entry.regex}*/[CcGg]?[0-9]+(/[Nn]?($key|${key.lowercase()}))?")
        } + INT_NEIGHBOURHOODS.map { (key, entry) ->
            Regex("[BbSs]${entry.regex}*/?[BbSs]${entry.regex}*/?[CcGg][0-9]+(/?[Nn]($key|${key.lowercase()}))?")
        } + INT_NEIGHBOURHOODS.map { (key, entry) ->
            Regex("[CcGg][0-9]+/?[Bb]${entry.regex}*/?[Ss]${entry.regex}*(/?[Nn]($key|${key.lowercase()}))?")
        }
    }

    constructor(birth: INTTransitions, survival: INTTransitions, numStates: Int, neighbourhoodString: String) {
        this.birth = birth
        this.survival = survival
        this.numStates = numStates
        this.neighbourhoodString = neighbourhoodString

        require(neighbourhoodString in INT_NEIGHBOURHOODS) { "INT Neighbourhood identifier " +
            "$neighbourhoodString is not supported." }
        neighbourhood = arrayOf(INT_NEIGHBOURHOODS[neighbourhoodString]!!.neighbourhood)

        // Setting the possible successors of each state
        possibleSuccessors = arrayOf(Array(numStates) {
            when (it) {
                0 -> intArrayOf(0, 1)
                1 -> if (this.survival.isEmpty()) intArrayOf(2) else intArrayOf(1, 2)
                else -> intArrayOf((it + 1) % numStates)
            }
        })

        equivalentStates = intArrayOf(0, 1) + IntArray(numStates - 2) { 0 }
    }

    constructor(rulestring: String = "12/34/3") {
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
        if (Regex("[Bb](($string)*)").find(rulestring) != null) {
            birth = parseTransition(Regex("[Bb](($string)*)").find(rulestring)!!.groupValues[1])
            survival = parseTransition(Regex("[Ss](($string)*)").find(rulestring)!!.groupValues[1])

            val option1 = Regex("[CcGg]([0-9]+)/?[Bb](($string)*)/?[Ss](($string)*)").find(rulestring)
            val option2 = Regex("[Bb]?:(($string)*)/?[Ss]?:(($string)*)/?[CcGg]([0-9]+)").find(rulestring)
            if (option1 != null) numStates = option1.groupValues[1].toInt()
            else if (option2 != null) numStates = option2.groupValues[1].toInt()
            else {
                numStates = 2
                require(false) { "This rulestring is invalid!" }
            }
        } else {
            val tokens = rulestring.split("/")
            birth = parseTransition(tokens[1])
            survival = parseTransition(tokens[0])
            numStates = tokens[2].toInt()
        }

        // Setting the possible successors of each state
        possibleSuccessors = arrayOf(Array(numStates) {
            when (it) {
                0 -> intArrayOf(0, 1)
                1 -> if (this.survival.isEmpty()) intArrayOf(2) else intArrayOf(1, 2)
                else -> intArrayOf((it + 1) % numStates)
            }
        })

        equivalentStates = intArrayOf(0, 1) + IntArray(numStates - 2) { 0 }
    }

    override fun canoniseRulestring(): String = "${survival.transitionString}/${birth.transitionString}/${numStates}" +
            if (neighbourhoodString != "M") "/$neighbourhoodString" else ""

    override fun fromRulestring(rulestring: String): INTGenerations = INTGenerations(rulestring)

    override fun between(minRule: INTGenerations, maxRule: INTGenerations): Boolean {
        return birth.containsAll(minRule.birth) && survival.containsAll(minRule.survival) &&
                maxRule.birth.containsAll(birth) && maxRule.survival.containsAll(survival)
    }

    override fun ruleRange(transitionsToSatisfy: Iterable<List<Int>>): RuleRange<INTGenerations> {
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
                it[0] == 1 && it[1] == 2 -> maxSurvival.remove(string)  // No survival
                it[0] == 1 && it[1] == 1 -> minSurvival.add(string) // Survival
            }
        }

        val minRule = INTGenerations(
            fromStringTransitions(neighbourhoodString, minBirth),
            fromStringTransitions(neighbourhoodString, minSurvival),
            numStates, neighbourhoodString
        )
        val maxRule = INTGenerations(
            fromStringTransitions(neighbourhoodString, maxBirth),
            fromStringTransitions(neighbourhoodString, maxSurvival),
            numStates, neighbourhoodString
        )

        return minRule .. maxRule
    }

    override fun rangeTo(maxRule: INTGenerations): RuleRange<INTGenerations> = RuleRange(this, maxRule)

    override fun enumerate(minRule: INTGenerations, maxRule: INTGenerations): Sequence<INTGenerations> {
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
                        INTGenerations(
                            rule.birth + setOf(birthDiff[index]), rule.survival,
                            numStates, neighbourhoodString
                        )
                    else
                        INTGenerations(
                            rule.birth, rule.survival + setOf(survivalDiff[index - birthDiff.size]),
                            numStates, neighbourhoodString
                        )

                    // 2 cases -> transition added and transition not added
                    stack.add(Pair(newRule, index + 1))
                    stack.add(Pair(rule, index + 1))
                }
            }
        }
    }

    override fun random(minRule: INTGenerations, maxRule: INTGenerations, seed: Int?): Sequence<INTGenerations> {
        return generateSequence {
            val randomBirth = randomTransition(minRule.birth, maxRule.birth, seed)
            val randomSurvival = randomTransition(minRule.survival, maxRule.survival, seed)

            INTGenerations(randomBirth, randomSurvival, numStates, neighbourhoodString)
        }
    }

    override fun intersect(ruleRange1: RuleRange<INTGenerations>, ruleRange2: RuleRange<INTGenerations>): RuleRange<INTGenerations>? {
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
            for (i in 1..<numStates)
                transition { "$i ${"any ".repeat(neighbourhood.size)}${(i + 1) % numStates}" }
        }

        colours(numStates, background) { colours[it] }
    }

    override fun transitionFunc(cells: IntArray, cellState: Int, generation: Int, coordinate: Coordinate): Int {
        val temp = cells.map { equivalentStates[it] }
        return when (cellState) {
            0 -> if (temp in birth) 1 else 0
            1 -> if (temp in survival) 1 else 2
            else -> (cellState + 1) % numStates
        }
    }

    override fun transitionFuncWithUnknowns(cells: IntArray, cellState: Int, generation: Int, coordinate: Coordinate): Int {
        if (cellState >= 2) return 1 shl (cellState + 1).mod(numStates)

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
            output[equivalentStates[transitionFunc(cellsCopy, cellState, generation, coordinate)]] = 1
            if (output.sum() == 2) break  // Quit if all possible output states have been reached
        }

        var num = 0
        if (output[0] == 1) num += 1 shl (if (cellState == 0) 0 else 2)
        if (output[1] == 1) num += 1 shl 1

        return num
    }
}