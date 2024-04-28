package rules.ruleloader

import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import rules.Rule
import rules.RuleFamily
import rules.ruleloader.ruletree.ruletreeDirectiveFromString
import simulation.Coordinate

/**
 * Generates a ruletable from the rule file at [filePath]
 */
fun ruletableFromFile(filePath: String): Ruletable {
    // Reading from a file
    val contents = SystemFileSystem.source(Path(filePath)).buffered().readString()

    // Generating the rule directives
    val directives = ArrayList<Directive>(2)
    val ruleDirectives = ArrayList<RuleDirective>(1)

    var name = ""
    var currentDirective = ""
    val stringBuilder = StringBuilder()
    contents.split("\n").forEach {
        if (it.startsWith("@")) {
            // Adding new directive
            val content = stringBuilder.toString()
            val newDirective = when (currentDirective) {
                "COLORS" -> colourDirectiveFromString(content)
                "TREE" ->  ruletreeDirectiveFromString(content)
                else -> null
            }

            if (newDirective != null) {
                directives.add(newDirective)
                if (newDirective is RuleDirective)
                    ruleDirectives.add(newDirective)
            }

            // Setting new directive
            stringBuilder.clear()
            currentDirective = it.split(" ").first().substring(1)
            if (currentDirective == "RULE") name = it.split(" ").last()
        } else stringBuilder.append(it).append("\n")
    }

    // Adding new directive
    val content = stringBuilder.toString()
    val newDirective = when (currentDirective) {
        "COLORS" -> colourDirectiveFromString(content)
        "TREE" ->  ruletreeDirectiveFromString(content)
        else -> null
    }

    if (newDirective != null) {
        directives.add(newDirective)
        if (newDirective is RuleDirective)
            ruleDirectives.add(newDirective)
    }

    return Ruletable(name, directives, ruleDirectives)
}

/**
 * Represents a Golly / Apgsearch ruletable
 * @constructor Constructs a ruletable from the given parameters
 * @param name The name of the ruletable
 * @property name The name of the ruletable
 * @param directive The directives of the ruletable which store metadata about the rule
 * @property directive The directives of the ruletable which store metadata about the rule
 * @param ruleDirectives The directives of the ruletable which store information about the rule's transitions
 * @property ruleDirectives The directives of the ruletable which store metadata about the rule's transitions
 */
class Ruletable(val name: String, val directive: List<Directive>, val ruleDirectives: List<RuleDirective>): Rule() {
    override val numStates: Int = ruleDirectives[0].numStates
    override val neighbourhood: Array<Array<Coordinate>>
        get() = ruleDirectives.map { it.neighbourhood }.toTypedArray()
    override val possibleSuccessors: Array<Array<IntArray>> = Array(alternatingPeriod) {
        Array(numStates) { IntArray(numStates) { it } }
    }
    override val equivalentStates: IntArray = IntArray(numStates) { it }

    override fun transitionFunc(cells: IntArray, cellState: Int, generation: Int, coordinate: Coordinate): Int =
        ruleDirectives[generation % ruleDirectives.size].transitionFunc(cells, cellState)

    override fun transitionFuncWithUnknowns(
        cells: IntArray,
        cellState: Int,
        generation: Int,
        coordinate: Coordinate
    ): Int = (1 shl numStates) - 1

    /**
     * Exports the ruletable as a string
     */
    fun export(): String = with(StringBuilder()) {
        append("@RULE $name\n\n")
        ruleDirectives.forEach { append("$it\n\n") }  // TODO (Handle alternating rules)
        directive.forEach { append("$it\n\n") }

        this
    }.toString()

    /**
     * Converts the ruletable to a string. Has the same function as [export].
     */
    override fun toString(): String = name
}