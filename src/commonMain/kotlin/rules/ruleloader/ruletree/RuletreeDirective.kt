package rules.ruleloader.ruletree

import moore
import rules.ruleloader.ColourDirective
import rules.ruleloader.RuleDirective
import rules.ruleloader.ruletable.convertWithIndex
import simulation.Coordinate

/**
 * Constructs a @TREE directive given the [contents] found in a ruletable under that directive
 */
fun ruletreeDirectiveFromString(contents: String): RuletreeDirective {
    var numStates = 2
    var nodes = Array(0) { intArrayOf() }
    val neighbourhood = arrayOf(
        Coordinate(-1, -1),
        Coordinate(1, -1),
        Coordinate(-1, 1),
        Coordinate(1, 1),
        Coordinate(0, -1),
        Coordinate(-1, 0),
        Coordinate(1, 0),
        Coordinate(0, 1)
    )

    var count = 0
    contents.split("\n").forEach {
        when {
            it.startsWith("num_states") -> numStates = it.split("=").last().toInt()
            //it.startsWith("num_neighbours") || it.startsWith("num_neighbors") -> neighbourhood = it.split("=").last()
            it.startsWith("num_nodes") -> nodes = Array(it.split("=").last().toInt()) { intArrayOf() }
            it.isNotEmpty() && it[0].isDigit() -> nodes[count++] = it.split(" ").map { it.toInt() }.toIntArray()
        }
    }

    return RuletreeDirective(
        numStates, neighbourhood, intArrayOf(0)
    ) { neighbours, cellState ->
        var currentNode = nodes.last()
        for (state in neighbours)
            currentNode = nodes[currentNode[state + 1]]

        return@RuletreeDirective currentNode[cellState + 1]
    }
}

/**
 * Represents the @TREE directive of the ruletable
 * @param numStates The number of states of the ruletable
 * @param neighbourhood The neighbourhood of the ruletable
 * @param background The background of the rule
 * @param f The transition function of the rule
 */
class RuletreeDirective(
    override val numStates: Int,
    override val neighbourhood: Array<Coordinate>,
    override val background: IntArray,
    val f: (IntArray, Int) -> Int
) : RuleDirective("tree") {
    private var nodeSeq = 0
    private val params = IntArray(neighbourhood.size + 1)
    private val r = ArrayList<IntArray>()
    private val world = HashMap<IntArray, Int>()

    private val _numStates = background.size * (numStates - 1) + 1

    // Emulates the strobing background
    private fun _f(neighbourhood: IntArray, state: Int): Int {
        if (state == 0 && neighbourhood.sum() == 0) return 0

        // Finding the state that isn't 0
        val bg = (maxOf(state, neighbourhood.maxOf { it }) - 1) / (numStates - 1)

        // Converting to the actual things
        val actualState = Utils.convert(if (state == 0) 0 else state - bg * (numStates - 1), background[bg])
        val actualNeighbourhood = neighbourhood.map {
            Utils.convert(if (it == 0) it else it - bg * (numStates - 1), background[bg])
        }

        // Converting back to the emulated representation
        val newBg = (bg + 1) % background.size
        val newState = Utils.convert(
            f(actualNeighbourhood.toIntArray(), actualState),
            background[newBg]
        )
        return if (newState == 0) newState else newState + newBg * (numStates - 1)
    }

    private fun getNode(n: IntArray): Int {
        if (world[n] == null) {
            world[n] = nodeSeq++
            r.add(n)
        }

        return world[n]!!
    }

    private fun recur(at: Int): Int {
        if (at == 0) return _f(IntArray(params.size - 1) { params[it] }, params[params.size - 1])

        val arr = IntArray(_numStates + 1)
        arr[0] = at
        for (i in 0 until _numStates) {
            params[neighbourhood.size + 1 - at] = i
            arr[i + 1] = recur(at - 1)
        }

        return getNode(arr)
    }

    override fun transitionFunc(cells: IntArray, cellState: Int): Int = f(cells, cellState)

    override fun export(): String {
        // Generating the rule tree
        recur(neighbourhood.size + 1)

        // Writing the rule tree headers
        val output = StringBuilder().apply {
            append("num_states=").append(_numStates).append("\n")

            val neighbourhoodString = neighbourhood.contentToString()
            append("num_neighbors=").append("[").append(
                neighbourhoodString, 1,
                neighbourhoodString.length - 1
            ).append(", (0, 0), (0, 0)]\n") // Add the 2 (0, 0)s at the back
            append("num_nodes=").append(r.size).append("\n")

            // Ruletree body
            for (key in r) {
                for (value in key) append(value).append(" ")
                append("\n")
            }
        }.toString()

        return output
    }
}