package rules.ruleloader.builders

import moore
import rules.ruleloader.ruletable.RuletableDirective
import rules.ruleloader.ruletable.Symmetry
import rules.ruleloader.ruletable.Transition
import rules.ruleloader.ruletable.Variable
import simulation.Coordinate

/**
 * A helper class for constructing the @TABLE directive in a ruletable
 */
class RuletableDirectiveBuilder(val numStates: Int = 2, val neighbourhood: Array<Coordinate> = moore(1),
                                val symmetry: Symmetry? = null) {
    private val directive = RuletableDirective(numStates, neighbourhood)
    private val variableMap: HashMap<String, Variable> = hashMapOf()

    /**
     * Adds a variable to the @TABLE directive
     * @param name The name of the variable
     * @param states A function returning the states of the variable
     */
    fun variable(name: String, states: () -> Iterable<Int>) {
        val variable = Variable(name, states().toSet())
        variableMap[name] = variable
        directive.addVariable(variable)
    }

    /**
     * Adds a transition to the @TABLE directive
     * @param transitions A function returning a string representing state literals / a variable
     * @throws IllegalArgumentException Thrown if the variable is not initialised
     */
    fun transition(transitions: () -> String) {
        val transition = transitions().split(" ")

        val input = transition[0]
        val output = transition[transition.size - 1]

        // Generate the transitions based on the symmetry
        val transitionsList = symmetry?.applySymmetry(transition.subList(1, transition.size - 1))?.map { listOf(input) + it + listOf(output) }
            ?: listOf(transition)
        transitionsList.forEach {
            val valuesMap = mutableMapOf<Int, Int>()
            val variablesMap = mutableMapOf<Int, Variable>()
                it.forEachIndexed { index, element ->
                    if (element.all { it in '0'..'9' }) valuesMap[index] = element.toInt()  // State literal
                    else {  // Variable
                        variablesMap[index] = if (element !in variableMap)
                            throw IllegalArgumentException("Variable $element has not been initialised!")
                        else variableMap[element]!!
                    }
                }

            // Add completed transition to the directive
            directive.addTransition(Transition(valuesMap, variablesMap))
        }
    }

    /**
     * Adds the specified transitions to the @TABLE directive
     * @param transitions A function returning a list of transitions
     * @throws IllegalArgumentException Thrown if the variable is not initialised
     */
    fun transitions(transitions: () -> List<String>) = transitions().forEach { transition { it } }

    /**
     * Adds a comment to the @TABLE directive
     * @param comment The contents of the comment
     */
    fun comment(comment: String) = directive.addComment(comment)

    /**
     * Constructs the @TABLE directive
     * @return Returns the @TABLE directive
     */
    fun build(): RuletableDirective = directive
}