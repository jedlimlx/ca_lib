package rules.hrot

import hexagonal
import moore
import rules.*
import rules.ruleloader.builders.ruletable
import simulation.Coordinate
import vonNeumann
import kotlin.math.abs
import kotlin.math.max
import kotlin.random.Random


/**
 * Represents a HROT Extended Generations rule
 * @constructor Constructs a HROT Extended Generations rule with the specified rulestring
 */
class HROTExtendedGenerations : BaseHROT, RuleRangeable<HROTExtendedGenerations> {
    /**
     * The birth transitions of the HROT rule
     */
    val birth: Set<Int>

    /**
     * The survival transitions of the HROT rule
     */
    val survival: Set<Int>

    /**
     * The active states of the rule
     */
    val activeStates: Set<Int>

    override val numStates: Int
    override val alternatingPeriod: Int = 1

    override val weights: IntArray?
    override val neighbourhood: Array<Array<Coordinate>>
    override val possibleSuccessors: Array<Array<IntArray>>

    override val equivalentStates: IntArray

    //language=RegExp
    val extendedGenerations = "(0-)?[1-9][0-9]*(-[1-9][0-9]*)*"
    override val regex: List<Regex> = listOf(
        Regex("R[0-9]+,B$transitionRegex,S$transitionRegex,G$extendedGenerations$neighbourhoodRegex"),
        Regex("[BbSs]?[0-8]*/[BbSs]?[0-8]*/[CcDd]?$extendedGenerations[VH]?"),
        Regex("[CcGg]$extendedGenerations[BbSs][0-8]*[BbSs][0-8]*[VH]?"),
        Regex("[BbSs][0-8]*[BbSs][0-8]*[CcGgDd]$extendedGenerations[VH]?")
    )

    /**
     * Constructs a HROT extended generations rule with the specified parameters
     * @param birth The birth transitions of the HROT generations rule
     * @param survival The survival transitions of the HROT generations rule
     * @param neighbourhood The neighbourhood of the HROT generations rule
     * @param weights The weights of the HROT generations rule
     */
    constructor(
        birth: Iterable<Int>, survival: Iterable<Int>, numStates: Int,
        neighbourhood: Array<Coordinate> = moore(1), weights: IntArray? = null,
        activeStates: Iterable<Int>
    ) {
        this.numStates = numStates

        this.birth = birth.toHashSet()
        this.survival = survival.toHashSet()

        this.activeStates = activeStates.toHashSet()

        this.weights = weights

        this.neighbourhood = arrayOf(neighbourhood)

        // Setting the possible successors of each state
        possibleSuccessors = arrayOf(Array(numStates) {
            when (it) {
                0 -> intArrayOf(0, 1)
                in activeStates -> if (this.survival.isNotEmpty()) intArrayOf(it, (it + 1) % numStates) else intArrayOf((it + 1) % numStates)
                else -> intArrayOf((it + 1) % numStates)
            }
        })

        equivalentStates = (0..<numStates).map { if (it in activeStates) 1 else 0 }.toIntArray()
    }

    /**
     * Constructs a HROT extended generations rule with the provided rulestring
     * @param rulestring The rulestring of the HROT rule
     */
    constructor(rulestring: String = "12/34/0-1-1") {
        when {
            rulestring.matches(regex[0]) -> {
                // Reading range and number of states
                val range = rulestring.split(",")[0].substring(1).toInt()

                // Reading neighbourhood string
                val temp = Regex("N(.*?)(,|$)").find(rulestring)
                val output = readNeighbourhood(range, if (temp == null) "M" else temp.groupValues[1])
                neighbourhood = arrayOf(output.first)
                weights = output.second

                // Loading birth and survival conditions
                val birthString = Regex("B${transitionRegex}").find(rulestring)!!.groupValues[1]
                val survivalString = Regex("S${transitionRegex}").find(rulestring)!!.groupValues[1]

                birth = readTransition(birthString)
                survival = readTransition(survivalString)

                // Loading the active cells
                val pair = readExtendedGenerations(Regex("G($extendedGenerations)").find(rulestring)!!.groupValues[1])
                numStates = pair.first
                activeStates = pair.second
            }
            rulestring.matches(regex[1]) -> {
                val tokens = rulestring.replace(Regex("[VH]"), "").split("/")
                val tokensStripped = rulestring.replace(Regex("[BbSsCcGgDdVH]"), "").split("/")

                // Reading birth and survival conditions
                birth =
                    readTransition(if ("b" in tokens[0].lowercase()) tokensStripped[0] else tokensStripped[1], false)
                survival =
                    readTransition(if ("s" in tokens[1].lowercase()) tokensStripped[1] else tokensStripped[0], false)

                // Reading number of states
                val pair = readExtendedGenerations(tokens[2].replace(Regex("[CcDdGg]"), ""))
                numStates = pair.first
                activeStates = pair.second

                // Loading neighbourhood
                neighbourhood = arrayOf(
                    when (rulestring[rulestring.length - 1]) {
                        'V' -> vonNeumann(1)
                        'H' -> hexagonal(1)
                        else -> moore(1)
                    }
                )

                weights = null
            }
            else -> {
                val birthToken = Regex("[Bb]([0-8]*)").findAll(rulestring).map { it.groupValues[1] }.toList()[0]
                val survivalToken = Regex("[Ss]([0-8]*)").findAll(rulestring).map { it.groupValues[1] }.toList()[0]

                // Loading birth and survival transitions
                birth = readTransition(birthToken, false)
                survival = readTransition(survivalToken, false)

                // Reading number of states
                val pair = readExtendedGenerations(
                    Regex("[DdCcGg]($extendedGenerations)").findAll(rulestring).map { it.groupValues[1] }.toList()[0]
                )
                numStates = pair.first
                activeStates = pair.second

                // Loading neighbourhood
                neighbourhood = arrayOf(
                    when (rulestring[rulestring.length - 1]) {
                        'V' -> vonNeumann(1)
                        'H' -> hexagonal(1)
                        else -> moore(1)
                    }
                )

                weights = null
            }
        }

        // Setting the possible successors of each state
        possibleSuccessors = arrayOf(Array(numStates) {
            //(0..<numStates).toList().toIntArray()
            when (it) {
                0 -> intArrayOf(0, 1)
                in activeStates -> if (this.survival.isNotEmpty()) intArrayOf(it, (it + 1) % numStates) else intArrayOf((it + 1) % numStates)
                else -> intArrayOf((it + 1) % numStates)
            }
        })

        equivalentStates = (0..<numStates).map { if (it in activeStates) 1 else 0 }.toIntArray()
    }

    override fun canoniseRulestring(): String {
        val range = neighbourhood[0].maxOf { max(abs(it.x), abs(it.y)) }
        val extendedGenerationString = canoniseExtendedGenerations(numStates, activeStates)
        return if (range == 1 && weights == null && neighbourhoodString in "ABCHMN2*+#") {
            "${survival.sorted().joinToString("")}/${birth.sorted().joinToString("")}/$extendedGenerationString" + when {
                neighbourhood[0].contentEquals(moore(1)) -> ""
                neighbourhood[0].contentEquals(vonNeumann(1)) -> "V"
                else -> "H"
            }
        } else {
            "R$range,B${canoniseTransition(birth)}S${canoniseTransition(survival)}G$extendedGenerationString,N${neighbourhoodString}"
        }
    }

    override fun fromRulestring(rulestring: String): HROTExtendedGenerations = HROTExtendedGenerations(rulestring)

    override fun between(minRule: HROTExtendedGenerations, maxRule: HROTExtendedGenerations): Boolean {
        return birth.containsAll(minRule.birth) && survival.containsAll(minRule.survival) &&
                maxRule.birth.containsAll(birth) && maxRule.survival.containsAll(survival)
    }

    override fun ruleRange(transitionsToSatisfy: Iterable<List<Int>>): RuleRange<HROTExtendedGenerations> {
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
                it[0] in activeStates && it[1] == (it[0] + 1) % numStates -> maxSurvival.remove(count)  // No survival
                it[0] in activeStates && it[1] == it[0] -> minSurvival.add(count) // Survival
            }
        }

        return newRuleWithTransitions(minBirth, minSurvival) .. newRuleWithTransitions(maxBirth, maxSurvival)
    }

    override fun rangeTo(maxRule: HROTExtendedGenerations): RuleRange<HROTExtendedGenerations> = RuleRange(this, maxRule)

    override fun enumerate(
        minRule: HROTExtendedGenerations,
        maxRule: HROTExtendedGenerations
    ): Sequence<HROTExtendedGenerations> {
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

    override fun random(
        minRule: HROTExtendedGenerations,
        maxRule: HROTExtendedGenerations,
        seed: Int?
    ): Sequence<HROTExtendedGenerations> {
        val random = if (seed != null) Random(seed) else Random
        return generateSequence {
            minRule.newRuleWithTransitions(
                randomTransition(minRule.birth, maxRule.birth, random.nextInt()),
                randomTransition(minRule.survival, maxRule.survival, random.nextInt())
            )
        }
    }

    override fun intersect(
        ruleRange1: RuleRange<HROTExtendedGenerations>,
        ruleRange2: RuleRange<HROTExtendedGenerations>
    ): RuleRange<HROTExtendedGenerations>? {
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

        return newRuleWithTransitions(newMinBirth, newMinSurvival) .. 
            newRuleWithTransitions(newMaxBirth, newMaxSurvival)
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

            for (state in activeStates) {
                comment("Survival $state")
                outerTotalistic {
                    input = "$state"
                    output = "$state"
                    transitions("dying") { survival }
                }
            }

            comment("Everything else dies")
            for (i in 1 until numStates)
                transition { "$i ${"any ".repeat(neighbourhood.size)}${(i + 1) % numStates}" }
        }

        colours(numStates, background) { colours[it] }
    }

    override fun transitionFunc(cells: IntArray, cellState: Int, generation: Int, coordinate: Coordinate): Int {
        val sum = cells.foldIndexed(0) { index, acc, value ->
            acc + (weights?.get(index) ?: 1) * (if (value in activeStates) 1 else 0)
        }

        return when {
            cellState == 0 && sum in birth -> 1  // Birth
            cellState in activeStates && sum in survival -> cellState  // Survival
            cellState == 0 -> 0  // 0 stays as 0
            else -> (cellState + 1) % numStates  // Decay
        }
    }

    override fun transitionFuncWithUnknowns(cells: IntArray, cellState: Int, generation: Int, coordinate: Coordinate): Int {
        if (cellState !in activeStates && cellState != 0) return 1 shl (cellState + 1).mod(numStates)

        var unknowns = 0
        var live = 0
        cells.forEachIndexed { index, it ->
            if (it == -1) unknowns += 1
            else if (it in activeStates) live += 1
        }

        var count = 0
        for (i in live..(live+unknowns)) {
            if (if (cellState == 0) i in birth else i in survival)
                count++
        }

        var state = 0
        if (count != 0) {
            if (cellState == 0) state += 1 shl 1
            else state += 1 shl cellState
        }
        if (count < unknowns + 1) {
            if (cellState == 0) state += 1 shl 0
            else state += 1 shl ((cellState + 1).mod(numStates))
        }

        return state
    }

    private fun newRuleWithTransitions(birth: Iterable<Int>, survival: Iterable<Int>): HROTExtendedGenerations =
        HROTExtendedGenerations(birth, survival, numStates, neighbourhood[0], weights, activeStates)
}