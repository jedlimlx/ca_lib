package rules.ruleloader.builders

import Colour
import moore
import rules.ruleloader.ColourDirective
import rules.ruleloader.Directive
import rules.ruleloader.RuleDirective
import rules.ruleloader.Ruletable
import rules.ruleloader.ruletable.Symmetry
import rules.ruleloader.ruletree.RuletreeDirective
import simulation.Coordinate

@DslMarker
annotation class DirectiveMarker

/**
 * Constructs a ruletable with the specified parameters.
 * @param init The function defining the key parameters of the ruletable
 * @return Returns the constructed ruletable
 */
fun ruletable(init: RuletableBuilder.() -> Unit): Ruletable {
    val ruletable = RuletableBuilder()
    ruletable.init()

    return ruletable.build()
}

/**
 * A helper class for building a ruletable
 */
@DirectiveMarker
class RuletableBuilder {
    /**
     * The name of the ruletable
     */
    var name = ""

    private var directives: ArrayList<Directive> = arrayListOf()
    private var ruleDirectives: ArrayList<RuleDirective> = arrayListOf()

    /**
     * Sets the colours of the ruletable
     * @param numStates The number of states of the rule
     * @param background The background of the rule
     * @param colours A function returning the colour of the specified state
     */
    fun colours(numStates: Int, background: IntArray = intArrayOf(0), colours: (state: Int) -> Colour) {
        directives.add(ColourDirective(Array(numStates) { colours(it) }, background))
    }

    /**
     * Constructs the @TABLE directive of the ruletable
     * @param numStates The number of states of the ruletable
     * @param neighbourhood The neighbourhood of the ruletable
     * @param symmetry The symmetry of the ruletable
     * @param init A function defining the key parameters of the ruletable
     */
    fun table(
        numStates: Int = 2, neighbourhood: Array<Coordinate> = moore(1), background: IntArray = intArrayOf(0),
        symmetry: Symmetry? = null, init: RuletableDirectiveBuilder.() -> Unit
    ) {
        val table = RuletableDirectiveBuilder(numStates, neighbourhood, background, symmetry)
        table.init()

        ruleDirectives.add(table.build())
    }

    /**
     * Constructs the @TREE directive of the ruletable
     * @param numStates The number of states of the ruletable
     * @param neighbourhood The neighbourhood of the ruletable
     * @param background The background of the rule
     * @param f The transition function of the rule
     */
    fun tree(
        numStates: Int = 2,
        neighbourhood: Array<Coordinate> = moore(1),
        background: IntArray = intArrayOf(0),
        f: (IntArray, Int) -> Int
    ) {
        directives.add(RuletreeDirective(numStates, neighbourhood, background, f))
    }

    /**
     * Constructs the final ruletable
     * @return Returns the final ruletable
     */
    fun build() = Ruletable(name, directives, ruleDirectives)
}
