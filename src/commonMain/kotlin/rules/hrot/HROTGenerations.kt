package rules.hrot

import hexagonal
import moore
import simulation.Coordinate
import vonNeumann
import kotlin.math.abs
import kotlin.math.max

/**
 * Represents a HROT Generations rule
 * TODO (Support state weights)
 * @constructor Constructs a HROT Generations rule with the specified rulestring
 */
class HROTGenerations(rulestring: String = "12/34/3") : BaseHROT() {
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

    override val regex: List<Regex> = listOf(
        Regex("R[0-9]+,C[0-9]+,S$transitionRegex,B$transitionRegex$neighbourhoodRegex"),
        Regex("[BbSs]?[0-8]*/[BbSs]?[0-8]*/[Cc]?[0-9]+[VH]?"),
        Regex("[CcGg][0-9]+[BbSs][0-8]*[BbSs][0-8]*[VH]?")
    )

    private val neighbourhoodString: String?

    init {
        when {
            rulestring.matches(regex[0]) -> {
                // Reading range and number of states
                val range = rulestring.split(",")[0].substring(1).toInt()
                numStates = rulestring.split(",")[1].substring(1).toInt()

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
            rulestring.matches(regex[1]) -> {
                val tokens = rulestring.split("/")
                val tokensStripped = rulestring.replace(Regex("[BbSsCcGg]"), "").split("/")

                // Reading birth and survival conditions
                birth = readTransition(if ("b" in tokens[0].lowercase()) tokensStripped[0] else tokensStripped[1], false)
                survival = readTransition(if ("s" in tokens[1].lowercase()) tokensStripped[1] else tokensStripped[0], false)

                // Reading number of states
                numStates = tokens[2].replace(Regex("[CcGg]") , "").toInt()

                // Loading neighbourhood
                neighbourhoodString = null
                neighbourhood = arrayOf(when (rulestring[rulestring.length - 1]) {
                    'V' -> vonNeumann(1)
                    'H' -> hexagonal(1)
                    else -> moore(1)
                })

                weights = null
            }
            else -> {
                println(rulestring)

                val birthToken = Regex("[Bb]([0-8]*)").findAll(rulestring).map { it.groupValues[1] }.toList()[0]
                val survivalToken = Regex("[Ss]([0-8]*)").findAll(rulestring).map { it.groupValues[1] }.toList()[0]

                // Loading birth and survival transitions
                birth = readTransition(birthToken, false)
                survival = readTransition(survivalToken, false)

                // Reading number of states
                numStates = Regex("[CcGg]([0-9]+)").findAll(rulestring).map { it.groupValues[1] }.toList()[0].toInt()

                // Loading neighbourhood
                neighbourhoodString = null
                neighbourhood = arrayOf(when (rulestring[rulestring.length - 1]) {
                    'V' -> vonNeumann(1)
                    'H' -> hexagonal(1)
                    else -> moore(1)
                })

                weights = null
            }
        }

        possibleSuccessors = arrayOf(Array(numStates) {
            when (it) {
                0 -> intArrayOf(0, 1)
                1 -> if (survival.isEmpty()) intArrayOf(2) else intArrayOf(1, 2)
                else -> intArrayOf((it + 1) % numStates)
            }
        })
    }

    override fun canoniseRulestring(): String {
        val range = neighbourhood[0].maxOf { max(abs(it.x), abs(it.y)) }
        return if (neighbourhoodString == null || (range == 1 && weights == null && (neighbourhoodString == "M" ||
                    neighbourhoodString == "N" || neighbourhoodString == "H"))) {
            "${survival.sorted().joinToString("")}/${birth.sorted().joinToString("")}/${numStates}" + when {
                neighbourhood[0].contentEquals(moore(1)) -> ""
                neighbourhood[0].contentEquals(vonNeumann(1)) -> "V"
                else -> "H"
            }
        } else {
            "R$range,C${numStates},S${canoniseTransition(survival)}B${canoniseTransition(birth)}N${neighbourhoodString}"
        }
    }

    override fun transitionFunc(cells: IntArray, cellState: Int, generation: Int, coordinate: Coordinate): Int {
        val sum = cells.foldIndexed(0) { index, acc, value ->
            acc + (weights?.get(index) ?: 1) * if (value == 1) 1 else 0
        }

        return when {
            cellState == 0 && sum in birth -> 1  // Birth
            cellState == 1 && sum in survival -> 1  // Survival
            cellState == 0 -> 0  // 0 stays as 0
            else -> (cellState + 1) % numStates  // Decay
        }
    }
}