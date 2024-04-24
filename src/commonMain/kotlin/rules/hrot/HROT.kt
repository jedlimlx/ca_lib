package rules.hrot

import hexagonal
import moore
import rules.RuleFamily
import rules.RuleRange
import rules.ruleloader.builders.ruletable
import simulation.Coordinate
import vonNeumann
import kotlin.math.abs
import kotlin.math.max
import kotlin.random.Random

/**
 * Represents a 2-state HROT rule
 */
class HROT : BaseHROT {
    /**
     * The birth transitions of the HROT rule
     */
    val birth: Set<Int>

    /**
     * The survival transitions of the HROT rule
     */
    val survival: Set<Int>

    override val numStates: Int = 2
    override val alternatingPeriod: Int = 1

    override val weights: IntArray?
    override val neighbourhood: Array<Array<Coordinate>>
    override val possibleSuccessors: Array<Array<IntArray>>

    override val equivalentStates: IntArray = intArrayOf(0, 1)

    override val regex: List<Regex> = listOf(
        Regex("R[0-9]+,C[02],S${transitionRegex},B${transitionRegex}${neighbourhoodRegex}"),
        Regex("([BSbs][0-8]*/?[BSbs][0-8]*|[BSbs]?[0-8]*/[BSbs]?[0-8]*)"),
        Regex("([BSbs][0-4]*/?[BSbs][0-4]*?|[BSbs]?[0-4]*/[BSbs]?[0-4]*)V"),
        Regex("([BSbs][0-6]*/?[BSbs][0-6]*|[BSbs]?[0-6]*/[BSbs]?[0-6]*)H")
    )

    /**
     * Constructs a HROT rule with the provided transitions and neighbourhood
     * @param birth The birth transitions of the HROT rule
     * @param survival The survival transitions of the HROT rule
     * @param neighbourhood The neighbourhood of the HROT rule
     * @param weights The weights of the HROT rule
     */
    constructor(
        birth: Iterable<Int>, survival: Iterable<Int>,
        neighbourhood: Array<Coordinate> = moore(1), weights: IntArray? = null
    ) {
        this.birth = birth.toHashSet()
        this.survival = survival.toHashSet()

        this.weights = weights
        this.neighbourhood = arrayOf(neighbourhood)

        // Setting the possible successors of each state
        possibleSuccessors = arrayOf(Array(2) {
            if (it == 0) intArrayOf(0, 1)
            else {
                if (this.survival.isEmpty()) intArrayOf(0)
                else intArrayOf(0, 1)
            }
        })
    }

    /**
     * Constructs a HROT rule with the provided rulestring
     * @param rulestring The rulestring of the HROT rule
     */
    constructor(rulestring: String = "R2,C2,S6-9,B7-8,NM") {
        when {
            rulestring.matches(regex[0]) -> {
                val range = rulestring.split(",")[0].substring(1).toInt()

                // Reading neighbourhood string
                val temp = Regex("N(.*?)$").find(rulestring)
                val neighbourhoodString = if (temp == null) "M" else temp.groupValues[1]

                val output = readNeighbourhood(range, neighbourhoodString)
                neighbourhood = arrayOf(output.first)
                weights = output.second

                // Loading birth and survival conditions
                val birthString = Regex("B$transitionRegex").find(rulestring)!!.groupValues[1]
                val survivalString = Regex("S$transitionRegex").find(rulestring)!!.groupValues[1]

                birth = readTransition(birthString)
                survival = readTransition(survivalString)
            }
            else -> {
                if ("b" in rulestring.lowercase()) {
                    val birthToken = Regex("[Bb]([0-8]*)").findAll(rulestring).map { it.groupValues[1] }.toList()
                    val survivalToken = Regex("[Ss]([0-8]*)").findAll(rulestring).map { it.groupValues[1] }.toList()

                    // Loading birth and survival conditions
                    birth = readTransition(birthToken[0], false)
                    survival = readTransition(survivalToken[0], false)
                } else {
                    val tokens = rulestring.replace(Regex("[VH]"), "").split("/")

                    // Loading birth and survival conditions
                    birth = readTransition(tokens[1], false)
                    survival = readTransition(tokens[0], false)
                }

                // Load neighbourhood
                neighbourhood = when {
                    rulestring.matches(regex[1]) -> arrayOf(moore(1))
                    rulestring.matches(regex[2]) -> arrayOf(vonNeumann(1))
                    rulestring.matches(regex[3]) -> arrayOf(hexagonal(1))
                    else -> throw IllegalArgumentException("Rulestring is not valid!")
                }

                weights = null
            }
        }

        // Setting the possible successors of each state
        possibleSuccessors = arrayOf(Array(2) {
            if (it == 0) intArrayOf(0, 1)
            else {
                if (survival.isEmpty()) intArrayOf(0)
                else intArrayOf(0, 1)
            }
        })
    }

    override fun canoniseRulestring(): String {
        val range = neighbourhood[0].maxOf { max(abs(it.x), abs(it.y)) }
        return if (range == 1 && weights == null && neighbourhoodString in "ABCHMN2*+#") {
            "B${birth.sorted().joinToString("")}/S${survival.sorted().joinToString("")}" + when {
                neighbourhood[0].contentEquals(moore(1)) -> ""
                neighbourhood[0].contentEquals(vonNeumann(1)) -> "V"
                else -> "H"
            }
        } else {
            "R$range,C2,S${canoniseTransition(survival)}B${canoniseTransition(birth)}N${neighbourhoodString}"
        }
    }

    override fun fromRulestring(rulestring: String): HROT = HROT(rulestring)

    override fun between(minRule: RuleFamily, maxRule: RuleFamily): Boolean {
        if (minRule !is HROT || maxRule !is HROT) return false
        return birth.containsAll(minRule.birth) && survival.containsAll(minRule.survival) &&
                maxRule.birth.containsAll(birth) && maxRule.survival.containsAll(survival)
    }

    override fun ruleRange(transitionsToSatisfy: Iterable<List<Int>>): Pair<HROT, HROT> {
        val maxCount = weights?.sum() ?: neighbourhood[0].size

        // The minimum possible transitions
        val minBirth = hashSetOf<Int>()
        val minSurvival = hashSetOf<Int>()

        // The maximum possible transitions
        val maxBirth = (0..maxCount).toHashSet()
        val maxSurvival = (0..maxCount).toHashSet()

        transitionsToSatisfy.forEach {
            // Compute the weighted sum of its neighbours
            val count = it.subList(2, it.size).foldIndexed(0) { index, acc, value ->
                acc + (weights?.get(index) ?: 1) * value
            }

            when {
                it[0] == 0 && it[1] == 0 -> maxBirth.remove(count)  // No birth
                it[0] == 0 && it[1] == 1 -> minBirth.add(count)  // Birth
                it[0] == 1 && it[1] == 0 -> maxSurvival.remove(count)  // No survival
                it[0] == 1 && it[1] == 1 -> minSurvival.add(count) // Survival
            }
        }

        return Pair(
            HROT(minBirth, minSurvival, neighbourhood[0], weights),
            HROT(maxBirth, maxSurvival, neighbourhood[0], weights)
        )
    }

    override fun enumerate(minRule: RuleFamily, maxRule: RuleFamily): Sequence<HROT> {
        require(minRule is HROT && maxRule is HROT) { "minRule and maxRule must be an instances of HROT" }

        // Obtain the difference in the birth conditions
        val birthDiff = maxRule.birth.toMutableList()
        birthDiff.removeAll(minRule.birth)

        // Obtain the difference in the survival conditions
        val survivalDiff = maxRule.survival.toMutableList()
        survivalDiff.removeAll(minRule.survival)

        val stack = arrayListOf(Pair(minRule, 0))  // Emulate a recursion stack
        return sequence {
            while (stack.isNotEmpty()) {
                val (rule, index) = stack.removeLast()

                if (index == birthDiff.size + survivalDiff.size) yield(rule)  // Base case
                else {
                    // Add the transition to the rule
                    val newRule = if (index < birthDiff.size)
                        rule.newRuleWithTransitions(rule.birth + setOf(birthDiff[index]), rule.survival)
                    else
                        rule.newRuleWithTransitions(
                            rule.birth,
                            rule.survival + setOf(survivalDiff[index - birthDiff.size])
                        )

                    // 2 cases -> transition added and transition not added
                    stack.add(Pair(newRule, index + 1))
                    stack.add(Pair(rule, index + 1))
                }
            }
        }
    }

    override fun random(minRule: RuleFamily, maxRule: RuleFamily, seed: Int?): Sequence<HROT> {
        require(minRule is HROT && maxRule is HROT) { "minRule and maxRule must be an instances of HROT" }

        val random = if (seed != null) Random(seed) else Random
        return generateSequence {
            minRule.newRuleWithTransitions(
                randomTransition(minRule.birth, maxRule.birth, random.nextInt()),
                randomTransition(minRule.survival, maxRule.survival, random.nextInt())
            )
        }
    }

    override fun intersect(ruleRange1: RuleRange, ruleRange2: RuleRange): RuleRange? {
        require(
            ruleRange1.minRule is HROT &&
            ruleRange1.maxRule is HROT &&
            ruleRange2.minRule is HROT &&
            ruleRange2.maxRule is HROT
        ) { "minRule and maxRule must be an instances of HROT" }

        val (newMinBirth, newMaxBirth) = intersectTransitionRange(
            ruleRange1.minRule.birth,
            ruleRange1.maxRule.birth,
            ruleRange2.minRule.birth,
            ruleRange2.maxRule.birth
        ) ?: return null
        val (newMinSurvival, newMaxSurvival) = intersectTransitionRange(
            ruleRange1.minRule.survival,
            ruleRange1.maxRule.survival,
            ruleRange2.minRule.survival,
            ruleRange2.maxRule.survival
        )?: return null

        return HROT(newMinBirth, newMinSurvival, neighbourhood[0], weights) .. 
            HROT(newMaxBirth, newMaxSurvival, neighbourhood[0], weights)
    }

    override fun generateRuletable() = ruletable {
        name = rulestring.replace(Regex("[,@/]"), "_")
        table(neighbourhood = neighbourhood[0], background = background) {
            variable("any") { 0..1 }

            comment("Birth")
            outerTotalistic {
                input = "0"
                output = "1"
                transitions { birth }
            }

            comment("Survival")
            outerTotalistic {
                input = "1"
                output = "1"
                transitions { survival }
            }

            comment("Everything else dies")
            transition { "1 ${"any ".repeat(neighbourhood.size)}0" }
        }

        colours(numStates, background) { colours[it] }
    }

    override fun transitionFunc(cells: IntArray, cellState: Int, generation: Int, coordinate: Coordinate): Int {
        val sum = cells.foldIndexed(0) { index, acc, value -> acc + (weights?.get(index) ?: 1) * value }
        return when {
            cellState == 0 && sum in birth -> 1
            cellState == 1 && sum in survival -> 1
            else -> 0
        }
    }

    override fun transitionFuncWithUnknowns(cells: IntArray, cellState: Int, generation: Int, coordinate: Coordinate): Int {
        if (weights == null) {
            var unknowns = 0
            var live = 0
            cells.forEachIndexed { index, it ->
                if (it == -1) unknowns += (weights?.get(index) ?: 1)
                else if (it == 1) live += (weights?.get(index) ?: 1)
            }

            var count = 0
            for (i in live..(live+unknowns)) {
                if (if (cellState == 1) i in survival else i in birth)
                    count++
            }

            var state = 0b00
            if (count != 0) state += 0b10
            if (count < unknowns + 1) state += 0b01

            return state
        } else {
            var live = 0
            val unknowns = HashMap<Int, Int>()
            cells.forEachIndexed { index, it ->
                if (it == -1) {
                    if (weights[index] !in unknowns) unknowns[weights[index]] = 1
                    else unknowns[weights[index]] = unknowns[weights[index]]!! + 1
                } else if (it == 1) live += weights[index]
            }

            var count = 0
            var dead = false
            val array = getAllSubsetSums(unknowns)
            for (i in array.indices) {
                if (array[i] == 1) {
                    if (if (cellState == 1) (live + i) in survival else (live + i) in birth) {
                        count++
                    } else dead = true
                }
            }

            var state = 0b00
            if (count != 0) state += 0b10
            if (dead) state += 0b01

            return state
        }
    }

    private fun newRuleWithTransitions(birth: Iterable<Int>, survival: Iterable<Int>): HROT =
        HROT(birth, survival, neighbourhood[0], weights)
}