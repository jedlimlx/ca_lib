package rules.ruleloader.builders

import moore
import rules.ruleloader.Directive
import rules.ruleloader.RuleDirective
import rules.ruleloader.Ruletable
import rules.ruleloader.ruletable.Symmetry
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
     * Constructs the @TABLE directive of the ruletable
     * @param numStates The number of states of the ruletable
     * @param neighbourhood The neighbourhood of the ruletable
     * @param symmetry The symmetry of the ruletable
     * @param init A function defining the key parameters of the ruletable
     */
    fun table(numStates: Int = 2, neighbourhood: Array<Coordinate> = moore(1), symmetry: Symmetry? = null,
              init: RuletableDirectiveBuilder.() -> Unit) {
        val table = RuletableDirectiveBuilder(numStates, neighbourhood, symmetry)
        table.init()

        ruleDirectives.add(table.build())
    }

    /**
     * Constructs the final ruletable
     * @return Returns the final ruletable
     */
    fun build() = Ruletable(name, directives, ruleDirectives)
}
