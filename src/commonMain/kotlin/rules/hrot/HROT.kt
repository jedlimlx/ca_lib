package rules.hrot

import hexagonal
import moore
import parseWeights
import simulation.Coordinate
import vonNeumann
import kotlin.math.abs
import kotlin.math.max

/**
 * Represents a 2-state HROT rule
 * @constructor Constructs a HROT rule with the specified rulestring
 */
class HROT(rulestring: String = "B3/S23") : BaseHROT() {
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

    override val regex: List<Regex> = listOf(
        Regex("R[0-9]+,C[02],S${transitionRegex},B${transitionRegex}${neighbourhoodRegex}"),
        Regex("([BSbs][0-8]*/?[BSbs][0-8]*|[BSbs]?[0-8]*/[BSbs]?[0-8]*)"),
        Regex("([BSbs][0-4]*/?[BSbs][0-4]*?|[BSbs]?[0-4]*/[BSbs]?[0-4]*)V"),
        Regex("([BSbs][0-6]*/?[BSbs][0-6]*|[BSbs]?[0-6]*/[BSbs]?[0-6]*)H")
    )

    private val neighbourhoodString: String?

    init {
        when {
            rulestring.matches(regex[0]) -> {
                val range = rulestring.split(",")[0].substring(1).toInt()

                // Reading neighbourhood string
                val temp = Regex("N(.*?)$").find(rulestring)
                neighbourhoodString = if (temp == null) "M" else temp.groupValues[1]

                val output = readNeighbourhood(range, neighbourhoodString)
                neighbourhood = arrayOf(output.first)
                weights = output.second

                // Loading birth and survival conditions
                val birthString = Regex("B${transitionRegex}").find(rulestring)!!.groupValues[1]
                val survivalString = Regex("S${transitionRegex}").find(rulestring)!!.groupValues[1]

                birth = readTransition(birthString)
                survival = readTransition(survivalString)
            }
            else -> {
                val tokens = Regex("[BbSs]([0-9]*)").findAll(rulestring).map { it.groupValues[1] }.toList()

                // Loading birth and survival conditions
                birth = readTransition(if ("b" in rulestring.lowercase()) tokens[0] else tokens[1], false)
                survival = readTransition(if ("b" in rulestring.lowercase()) tokens[1] else tokens[0], false)

                // Load neighbourhood
                neighbourhoodString = null
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
        return if (neighbourhoodString == null || (range == 1 && weights == null && (neighbourhoodString == "M" ||
                    neighbourhoodString == "N" || neighbourhoodString == "H"))) {
            "B${birth.sorted().joinToString("")}/S${survival.sorted().joinToString("")}" + when {
                neighbourhood[0].contentEquals(moore(1)) -> ""
                neighbourhood[0].contentEquals(vonNeumann(1)) -> "V"
                else -> "H"
            }
        } else {
            "R$range,C2,S${canoniseTransition(survival)}B${canoniseTransition(birth)}N${neighbourhoodString}"
        }
    }

    override fun transitionFunc(cells: IntArray, cellState: Int, generation: Int, coordinate: Coordinate): Int {
        val sum = cells.foldIndexed(0) { index, acc, value -> acc + (weights?.get(index) ?: 1) * value }
        return when {
            cellState == 0 && sum in birth -> 1
            cellState == 1 && sum in survival -> 1
            else -> 0
        }
    }
}