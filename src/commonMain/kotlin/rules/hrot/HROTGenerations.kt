package rules.hrot

import hexagonal
import moore
import rules.RuleFamily
import rules.RuleRange
import rules.RuleRangeable
import rules.ruleloader.builders.ruletable
import simulation.Coordinate
import vonNeumann
import kotlin.math.abs
import kotlin.math.max
import kotlin.random.Random

/**
 * Represents a HROT Generations rule
 * @constructor Constructs a HROT Generations rule with the specified rulestring
 */
class HROTGenerations : BaseHROT, RuleRangeable<HROTGenerations> {
    /**
     * The birth transitions of the HROT rule
     */
    val birth: Set<Int>

    /**
     * The survival transitions of the HROT rule
     */
    val survival: Set<Int>

    override val numStates: Int
    override val alternatingPeriod: Int = 1

    override val weights: IntArray?
    override val neighbourhood: Array<Array<Coordinate>>
    override val possibleSuccessors: Array<Array<IntArray>>

    override val equivalentStates: IntArray

    val stateWeights: IntArray?

    override val regex: List<Regex> = listOf(
        Regex("R[0-9]+,C[0-9]+,S$transitionRegex,B$transitionRegex$neighbourhoodRegex(,([A-Fa-f0-9]+))?"),
        Regex("[BbSs]?[0-8]*/[BbSs]?[0-8]*/[Cc]?[0-9]+[VH]?"),
        Regex("[CcGg][0-9]+[BbSs][0-8]*[BbSs][0-8]*[VH]?")
    )

    /**
     * Constructs a HROT generations rule with the specified parameters
     * @param birth The birth transitions of the HROT generations rule
     * @param survival The survival transitions of the HROT generations rule
     * @param neighbourhood The neighbourhood of the HROT generations rule
     * @param weights The weights of the HROT generations rule
     * @param stateWeights The state weights of the HROT generations rule
     */
    constructor(
        birth: Iterable<Int>, survival: Iterable<Int>, numStates: Int,
        neighbourhood: Array<Coordinate> = moore(1), weights: IntArray? = null,
        stateWeights: IntArray? = null
    ) {
        this.numStates = numStates

        this.birth = birth.toHashSet()
        this.survival = survival.toHashSet()

        this.weights = weights
        this.stateWeights = stateWeights

        this.neighbourhood = arrayOf(neighbourhood)

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

    /**
     * Constructs a HROT generations rule with the provided rulestring
     * @param rulestring The rulestring of the HROT rule
     */
    constructor(rulestring: String = "12/34/3") {
        when {
            rulestring.matches(regex[0]) -> {
                // Reading range and number of states
                val range = rulestring.split(",")[0].substring(1).toInt()
                numStates = rulestring.split(",")[1].substring(1).toInt()

                // Reading neighbourhood string
                val temp = Regex("N(.*?)(,|$)").find(rulestring)
                val output = readNeighbourhood(range, if (temp == null) "M" else temp.groupValues[1])
                neighbourhood = arrayOf(output.first)
                weights = output.second

                // Reading state weights
                val temp2 = Regex(",([A-Fa-f0-9]+)$").find(rulestring)
                stateWeights = if (temp2 == null) null else temp2.groupValues[1].map {
                    when (it.lowercase()) {
                        "a" -> 10
                        "b" -> 11
                        "c" -> 12
                        "d" -> 13
                        "e" -> 14
                        "f" -> 15
                        else -> it.digitToInt()
                    }
                }.toIntArray()

                // Loading birth and survival conditions
                val birthString = Regex("B${transitionRegex}").find(rulestring)!!.groupValues[1]
                val survivalString = Regex("S${transitionRegex}").find(rulestring)!!.groupValues[1]

                birth = readTransition(birthString)
                survival = readTransition(survivalString)
            }
            rulestring.matches(regex[1]) -> {
                val tokens = rulestring.replace(Regex("[VH]"), "").split("/")
                val tokensStripped = rulestring.replace(Regex("[BbSsCcGgVH]"), "").split("/")

                // Reading birth and survival conditions
                birth =
                    readTransition(if ("b" in tokens[0].lowercase()) tokensStripped[0] else tokensStripped[1], false)
                survival =
                    readTransition(if ("s" in tokens[1].lowercase()) tokensStripped[1] else tokensStripped[0], false)

                // Reading number of states
                numStates = tokens[2].replace(Regex("[CcGg]"), "").toInt()

                // Loading neighbourhood
                neighbourhood = arrayOf(
                    when (rulestring[rulestring.length - 1]) {
                        'V' -> vonNeumann(1)
                        'H' -> hexagonal(1)
                        else -> moore(1)
                    }
                )

                weights = null
                stateWeights = null
            }
            else -> {
                val birthToken = Regex("[Bb]([0-8]*)").findAll(rulestring).map { it.groupValues[1] }.toList()[0]
                val survivalToken = Regex("[Ss]([0-8]*)").findAll(rulestring).map { it.groupValues[1] }.toList()[0]

                // Loading birth and survival transitions
                birth = readTransition(birthToken, false)
                survival = readTransition(survivalToken, false)

                // Reading number of states
                numStates = Regex("[CcGg]([0-9]+)").findAll(rulestring).map { it.groupValues[1] }.toList()[0].toInt()

                // Loading neighbourhood
                neighbourhood = arrayOf(
                    when (rulestring[rulestring.length - 1]) {
                        'V' -> vonNeumann(1)
                        'H' -> hexagonal(1)
                        else -> moore(1)
                    }
                )

                weights = null
                stateWeights = null
            }
        }

        possibleSuccessors = arrayOf(Array(numStates) {
            when (it) {
                0 -> intArrayOf(0, 1)
                1 -> if (survival.isEmpty()) intArrayOf(2) else intArrayOf(1, 2)
                else -> intArrayOf((it + 1) % numStates)
            }
        })

        equivalentStates = intArrayOf(0, 1) + IntArray(numStates - 2) { 0 }
    }

    override fun canoniseRulestring(): String {
        val range = neighbourhood[0].maxOf { max(abs(it.x), abs(it.y)) }
        return if (range == 1 && weights == null && neighbourhoodString in "ABCHMN2*+#") {
            "${survival.sorted().joinToString("")}/${birth.sorted().joinToString("")}/${numStates}" + when {
                neighbourhood[0].contentEquals(moore(1)) -> ""
                neighbourhood[0].contentEquals(vonNeumann(1)) -> "V"
                else -> "H"
            }
        } else if (stateWeights != null) {
            "R$range,C${numStates},S${canoniseTransition(survival)}" +
                    "B${canoniseTransition(birth)}N${neighbourhoodString}" + stateWeights.map {
                when (it) {
                    10 -> "a"
                    11 -> "b"
                    12 -> "c"
                    13 -> "d"
                    14 -> "e"
                    15 -> "f"
                    else -> it.toString()
                }
            }.joinToString("")
        } else {
            "R$range,C${numStates},S${canoniseTransition(survival)}B${canoniseTransition(birth)}N${neighbourhoodString}"
        }
    }

    override fun fromRulestring(rulestring: String): HROTGenerations = HROTGenerations(rulestring)

    override fun between(minRule: HROTGenerations, maxRule: HROTGenerations): Boolean {
        return birth.containsAll(minRule.birth) && survival.containsAll(minRule.survival) &&
                maxRule.birth.containsAll(birth) && maxRule.survival.containsAll(survival)
    }

    override fun ruleRange(transitionsToSatisfy: Iterable<List<Int>>): RuleRange<HROTGenerations> {
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
                acc + (weights?.get(index) ?: 1) * if (value == 1) 1 else 0
            }

            when {
                it[0] == 0 && it[1] == 0 -> maxBirth.remove(count)  // No birth
                it[0] == 0 && it[1] == 1 -> minBirth.add(count)  // Birth
                it[0] == 1 && it[1] == 2 -> maxSurvival.remove(count)  // No survival
                it[0] == 1 && it[1] == 1 -> minSurvival.add(count) // Survival
            }
        }

        return HROTGenerations(minBirth, minSurvival, numStates, neighbourhood[0], weights) ..
            HROTGenerations(maxBirth, maxSurvival, numStates, neighbourhood[0], weights)
    }

    override fun rangeTo(maxRule: HROTGenerations): RuleRange<HROTGenerations> = RuleRange(this, maxRule)

    override fun enumerate(minRule: HROTGenerations, maxRule: HROTGenerations): Sequence<HROTGenerations> {
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

    override fun random(minRule: HROTGenerations, maxRule: HROTGenerations, seed: Int?): Sequence<HROTGenerations> {
        val random = if (seed != null) Random(seed) else Random
        return generateSequence {
            minRule.newRuleWithTransitions(
                randomTransition(minRule.birth, maxRule.birth, random.nextInt()),
                randomTransition(minRule.survival, maxRule.survival, random.nextInt())
            )
        }
    }

    override fun intersect(
        ruleRange1: RuleRange<HROTGenerations>,
        ruleRange2: RuleRange<HROTGenerations>
    ): RuleRange<HROTGenerations>? {
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

        return HROTGenerations(newMinBirth, newMinSurvival, numStates, neighbourhood[0], weights) .. 
            HROTGenerations(newMaxBirth, newMaxSurvival, numStates, neighbourhood[0], weights)
    }

    override fun generateRuletable() = ruletable {
        name = rulestring.replace(Regex("[,@/]"), "_")
        table(numStates, neighbourhood[0], background) {
            variable("any") { 0 until numStates }
            variable("dying") { (2 until numStates) + 0 }

            comment("Birth")
            outerTotalistic {
                input = "0"
                output = "1"
                transitions("dying") { birth }
            }

            comment("Survival")
            outerTotalistic {
                input = "1"
                output = "1"
                transitions("dying") { survival }
            }

            comment("Everything else dies")
            for (i in 1..<numStates)
                transition { "$i ${"any ".repeat(neighbourhood.size)}${(i + 1) % numStates}" }
        }

        colours(numStates, background) { colours[it] }
    }

    override fun transitionFunc(cells: IntArray, cellState: Int, generation: Int, coordinate: Coordinate): Int {
        val sum = cells.foldIndexed(0) { index, acc, value ->
            acc + (weights?.get(index) ?: 1) * (stateWeights?.get(value) ?: (if (value == 1) 1 else 0))
        }

        return when {
            cellState == 0 && sum in birth -> 1  // Birth
            cellState == 1 && sum in survival -> 1  // Survival
            cellState == 0 -> 0  // 0 stays as 0
            else -> (cellState + 1) % numStates  // Decay
        }
    }

    override fun transitionFuncWithUnknowns(cells: IntArray, cellState: Int, generation: Int, coordinate: Coordinate): Int {
        if (cellState > 1) return 1 shl (cellState + 1).mod(numStates)

        if (weights == null) {
            var unknowns = 0
            var live = 0
            cells.forEachIndexed { index, it ->
                if (it == -1) unknowns += 1
                else if (it == 1) live += 1
            }

            var count = 0
            for (i in live..(live+unknowns)) {
                if (if (cellState == 1) i in survival else i in birth)
                    count++
            }

            var state = 0b00
            if (count != 0) state += 1 shl 1
            if (count < unknowns + 1) {
                if (cellState == 0) state += 1 shl 0
                else state += 1 shl (cellState + 1).mod(numStates)
            }

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
            if (dead) {
                if (cellState == 0) state += 1 shl 0
                else state += 1 shl (cellState + 1).mod(numStates)
            }

            return state
        }
    }

    private fun newRuleWithTransitions(birth: Iterable<Int>, survival: Iterable<Int>): HROTGenerations =
        HROTGenerations(birth, survival, numStates, neighbourhood[0], weights, stateWeights)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as HROTGenerations

        if (birth != other.birth) return false
        if (survival != other.survival) return false
        if (numStates != other.numStates) return false
        if (!neighbourhood[0].contentEquals(other.neighbourhood[0])) return false
        if (!weights.contentEquals(other.weights)) return false
        if (!stateWeights.contentEquals(other.stateWeights)) return false

        return true
    }
}