package rules.nontotalistic.rules

import rules.nontotalistic.transitions.INTTransitions
import rules.ruleloader.builders.ruletable
import simulation.Coordinate


/**
 * Represents a deficient isotropic non-totalistic (INT) rule
 * TODO Fix this for cfind
 */
class DeficientINT : BaseINT {
    /**
     * The birth transitions of the INT rule
     */
    val birth: INTTransitions

    /**
     * The survival transitions of the INT rule
     */
    val survival: INTTransitions

    /**
     * Do deficient cells stay as deficient cells forever or do they turn into normal cells?
     */
    val permanentDeficiency: Boolean

    /**
     * Converts the state to the corresponding transition string
     */
    val stateLookup: Array<String>

    /**
     * Converts the transition string to the corresponding state
     */
    val transitionLookup: Map<String, Int>

    override val neighbourhood: Array<Array<Coordinate>>
    override val neighbourhoodString: String

    override val numStates: Int
    override val possibleSuccessors: Array<Array<IntArray>>

    override val regex: List<Regex> by lazy {
        INT_NEIGHBOURHOODS.map { (key, entry) ->
            Regex("[Bb]${entry.regex}/?[Ss]${entry.regex}/?[Dd][Pp]?(/?[Nn]($key|${key.lowercase()}))?")
        }
    }

    override val equivalentStates: IntArray

    constructor(birth: INTTransitions, survival: INTTransitions, neighbourhoodString: String, permanentDeficiency: Boolean) {
        this.birth = birth
        this.survival = survival
        this.neighbourhoodString = neighbourhoodString

        require(neighbourhoodString in INT_NEIGHBOURHOODS) { "INT Neighbourhood identifier " +
            "$neighbourhoodString is not supported." }
        neighbourhood = arrayOf(INT_NEIGHBOURHOODS[neighbourhoodString]!!.neighbourhood)

        // Check if the rule is permanently deficient
        this.permanentDeficiency = permanentDeficiency

        // Compute other attributes about the rule
        numStates = 2 + birth.transitionStrings.size
        equivalentStates = IntArray(numStates) { it }
        possibleSuccessors = arrayOf(Array(numStates) {
            when (it) {
                0 -> IntArray(numStates) { it }
                1 -> intArrayOf(0, 1)
                else -> {
                    if (permanentDeficiency) intArrayOf(0, it)
                    else intArrayOf(0, 1)
                }
            }
        })

        stateLookup = arrayOf("", "") + birth.transitionStrings.toList().sorted().toTypedArray()
        transitionLookup = stateLookup.mapIndexed { index, it -> it to index }.toMap()
    }

    constructor(rulestring: String = "B2n3/S23-q") {
        // Get the neighbourhood string
        neighbourhoodString = Regex(
            "/?[Nn]?(${
                INT_NEIGHBOURHOODS.keys.map {
                    listOf(it.lowercase(), it.uppercase())
                }.flatten().joinToString("|")
            })$"
        ).find(rulestring)?.groupValues?.get(1) ?: "M"

        // Load in the neighbourhood
        require(neighbourhoodString in INT_NEIGHBOURHOODS) {
            "INT Neighbourhood identifier " +
                    "$neighbourhoodString is not supported."
        }
        neighbourhood = arrayOf(INT_NEIGHBOURHOODS[neighbourhoodString]!!.neighbourhood)

        // Load in the birth / survival conditions
        val string = INT_NEIGHBOURHOODS[neighbourhoodString]!!.regex.pattern

        "[Bb](($string)*)"  // This useless line makes the unit tests pass, don't question
        birth = parseTransition(Regex("[Bb](($string)*)").find(rulestring)!!.groupValues[1])
        survival = parseTransition(Regex("[Ss](($string)*)").find(rulestring)!!.groupValues[1])

        // Check if the rule is permanently deficient
        permanentDeficiency = Regex("[Bb]?:(($string)*)/?[Ss]?:(($string)*)/?[Dd]([Pp])").find(rulestring) != null

        // Compute other attributes about the rule
        numStates = 2 + birth.transitionStrings.size
        equivalentStates = IntArray(numStates) { it }
        possibleSuccessors = arrayOf(Array(numStates) {
            when (it) {
                0 -> intArrayOf(0) + IntArray(numStates - 2) { it + 2 }
                1 -> intArrayOf(0, 1)
                else -> {
                    if (permanentDeficiency) intArrayOf(0, it)
                    else intArrayOf(0, 1)
                }
            }
        })

        stateLookup = arrayOf("", "") + birth.transitionStrings.toList().sorted().toTypedArray()
        transitionLookup = stateLookup.mapIndexed { index, it -> it to index }.toMap()
    }

    override fun canoniseRulestring(): String = "B${birth.transitionString}/S${survival.transitionString}/" +
            if (permanentDeficiency) "DP" else "D" +
            if (neighbourhoodString != "M") "/N$neighbourhoodString" else ""

    override fun fromRulestring(rulestring: String): DeficientINT = DeficientINT(rulestring)

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
        val temp = cells.map { minOf(it, 1) }
        val forbidden = cells.map { stateLookup[it] }

        return when (cellState) {
            0 -> if (temp in birth) {
                val transition = birth.stringFromTransition(temp)
                if (transition !in forbidden) transitionLookup[transition]!!
                else 0
            } else 0
            else -> if (temp in survival) {
                if (permanentDeficiency) cellState else 1
            } else 0
        }
    }

    override fun transitionFuncWithUnknowns(cells: IntArray, cellState: Int, generation: Int, coordinate: Coordinate): Int {
        val output = IntArray(numStates) { 0 }

        // Enumerate all possible configurations of the unknown cells
        val unknownNum = cells.count { it == -1 }
        val cellsCopy = cells.map { if (it > 0) 1 else 0 }.toMutableList()
        for (i in 0 .. (1 shl unknownNum)) {
            var count = 0
            val string = i.toString(2).padStart(unknownNum, '0')

            // Replacing the '-1's with the respective cells
            for (j in cellsCopy.indices) {
                if (cells[j] == -1)
                    cellsCopy[j] = string[count++].digitToInt()
            }

            // Ask the transition function what the output state will be
            if (cellState >= 1) {
                if (cellsCopy in survival.transitions) output[1] = 1
                else output[0] = 1

                if (output.sum() == 2) break  // Quit if all possible output states have been reached
            } else {
                if (cellsCopy in birth.transitions)
                    output[transitionLookup[birth.stringFromTransition(cellsCopy)]!!] = 1
            }
        }

        var num = 0
        if (cellState >= 1) {
            if (output[0] == 1) num += 1 shl 0
            if (output[1] == 1) num += 1 shl 1
        } else {
            num += 1 shl 0  // As long as unknown cells exist, we can never be sure that the cell will be born
            for (i in 2..numStates)
                if (output[i] == 1) num += 1 shl i
        }

        return num
    }
}