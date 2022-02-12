package rules.nontotalistic.rules

import rules.RuleFamily
import rules.nontotalistic.transitions.INTTransitions
import rules.ruleloader.Ruletable
import simulation.Coordinate

class INT : BaseINT {
    val birth: INTTransitions
    val survival: INTTransitions

    override val neighbourhood: Array<Array<Coordinate>>
    override val neighbourhoodString: String

    override val possibleSuccessors: Array<Array<IntArray>> = arrayOf(arrayOf(intArrayOf(0, 1), intArrayOf(0, 1)))

    override val regex: List<Regex> by lazy {
        INT_NEIGHBOURHOODS.map { (key, entry) ->
            Regex("[Bb]${entry.regex}/?[Ss]${entry.regex}/?[Nn]($key|${key.lowercase()})")
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
        }.flatten().joinToString("|")})").find(rulestring)?.groupValues?.get(1) ?: "M"

        // Load in the neighbourhood
        require(neighbourhoodString in INT_NEIGHBOURHOODS) { "INT Neighbourhood identifier " +
                "$neighbourhoodString is not supported." }
        neighbourhood = arrayOf(INT_NEIGHBOURHOODS[neighbourhoodString]!!.neighbourhood)

        // Load in the birth / survival conditions
        birth = parseTransition(Regex("[Bb]((${INT_NEIGHBOURHOODS[neighbourhoodString]!!.regex})*)")
            .find(rulestring)!!.groupValues[1])
        survival = parseTransition(Regex("[Ss]((${INT_NEIGHBOURHOODS[neighbourhoodString]!!.regex})*)")
            .find(rulestring)!!.groupValues[1])
    }

    override fun canoniseRulestring(): String = "B${birth.transitionString}/S${survival.transitionString}" +
            if (neighbourhoodString != "M") "/N$neighbourhoodString" else ""

    override fun fromRulestring(rulestring: String): INT = INT(rulestring)

    override fun ruleRange(transitionsToSatisfy: Iterable<List<Int>>): Pair<RuleFamily, RuleFamily> {
        TODO("Not yet implemented")
    }

    override fun enumerate(minRule: RuleFamily, maxRule: RuleFamily): Sequence<RuleFamily> {
        TODO("Not yet implemented")
    }

    override fun random(minRule: RuleFamily, maxRule: RuleFamily, seed: Int?): Sequence<RuleFamily> {
        TODO("Not yet implemented")
    }

    override fun generateRuletable(): Ruletable {
        TODO("Not yet implemented")
    }

    override fun transitionFunc(cells: IntArray, cellState: Int, generation: Int, coordinate: Coordinate): Int {
        return when (cellState) {
            0 -> if (cells in birth) 1 else 0
            else -> if (cells in survival) 1 else 0
        }
    }
}