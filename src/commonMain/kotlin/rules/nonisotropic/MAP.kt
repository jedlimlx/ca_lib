package rules.nonisotropic

import simulation.Coordinate

/**
 * Represents a 2-state isotropic non-totalistic (INT) rule
 */
class MAP : BaseMAP {
    /**
     * The transitions of the MAP rule
     */
    val transitions: BooleanArray

    override val neighbourhood: Array<Array<Coordinate>> = arrayOf(
        arrayOf(
            Coordinate(1, 1),
            Coordinate(0, 1),
            Coordinate(-1, 1),
            Coordinate(1, 0),
            Coordinate(-1, 0),
            Coordinate(1, -1),
            Coordinate(0, -1),
            Coordinate(-1, -1),
        )
    )

    val weights = listOf(1 shl 0, 1 shl 1, 1 shl 2, 1 shl 3, 1 shl 5, 1 shl 6, 1 shl 7, 1 shl 8)

    override val possibleSuccessors: Array<Array<IntArray>> = arrayOf(arrayOf(intArrayOf(0, 1), intArrayOf(0, 1)))

    override val regex: List<Regex> = listOf(
        Regex(
            "MAP[Bb]$mapRegex[/+][Ss]$mapRegex"
        )
    )

    override val equivalentStates: IntArray = intArrayOf(0, 1)

    constructor(transitions: BooleanArray) {
        this.transitions = transitions
    }

    constructor(rulestring: String = "MAPARYXfhZofugWaH7oaIDogBZofuhogOiAaIDogIAAgAAWaH7oaIDogGiA6ICAAIAAaIDogIAAgACAAIAAAAAAAA") {
        transitions = decodeTransition(Regex("MAP($mapRegex)").find(rulestring)!!.groupValues[1])
    }

    override fun canoniseRulestring(): String = "MAP${encodeTransition(transitions).replace("=", "")}"

    override fun fromRulestring(rulestring: String): MAP = MAP(rulestring)

    override fun generateRuletable() = TODO("Not yet implemented")

    override fun transitionFunc(cells: IntArray, cellState: Int, generation: Int, coordinate: Coordinate): Int {
        val transition = transitions[cells.mapIndexed { index, it -> weights[index] * it }.sum() + cellState * (1 shl 4)]
        return if (transition) 1 else 0
    }

    override fun transitionFuncWithUnknowns(cells: IntArray, cellState: Int, generation: Int, coordinate: Coordinate): Int {
        if (cellState == 2) return 3

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