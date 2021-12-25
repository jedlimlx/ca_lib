package rules.ruleloader.builders

import moore
import rules.ruleloader.ruletable.*
import simulation.Coordinate

@DslMarker
annotation class TableDirectiveMarker

/**
 * A helper class for constructing the @TABLE directive in a ruletable
 */
@DirectiveMarker
@TableDirectiveMarker
class RuletableDirectiveBuilder(
    val numStates: Int = 2, val neighbourhood: Array<Coordinate> = moore(1),
    val background: IntArray = intArrayOf(0), val symmetry: Symmetry? = null
) {
    private val directive = RuletableDirective(numStates, neighbourhood, background)
    private val variableMap: HashMap<String, Variable> = hashMapOf()

    // TODO (Support weights)

    /**
     * Adds a comment to the @TABLE directive
     * @param comment The contents of the comment
     */
    fun comment(comment: String) = directive.addComment(comment)

    /**
     * Adds a variable to the @TABLE directive
     * @param name The name of the variable
     * @param states A function returning the states of the variable
     */
    fun variable(name: String, states: () -> Iterable<Int>) {
        val variable = Variable(name, states().toSet())
        variable.numStates = numStates

        variableMap[name] = variable
        directive.addVariable(variable)
    }

    /**
     * Adds a transition to the @TABLE directive
     * @param transitions A function returning a string representing state literals / a variable
     * @throws IllegalArgumentException Thrown if the variable is not initialised
     */
    fun transition(transitions: () -> String) {
        val transition = transitions().split(Regex("(,\\s*|\\s+)"))

        val input = transition[0]
        val output = transition[transition.size - 1]

        // Generate the transitions based on the symmetry
        val transitionsList = symmetry?.applySymmetry(transition.subList(1, transition.size - 1))
            ?.map { listOf(input) + it + listOf(output) }
            ?: listOf(transition)
        addTransitions(transitionsList)
    }

    /**
     * Adds the specified transitions to the @TABLE directive
     * @param transitions A function returning a list of transitions
     * @throws IllegalArgumentException Thrown if the variable is not initialised
     */
    fun transitions(transitions: () -> List<String>) = transitions().forEach { transition { it } }

    /**
     * Adds the specified outer-totalistic transitions to the @TABLE directive
     * @param init A function specifying the outer-totalistic transitions to be added
     */
    fun outerTotalistic(init: OuterTotalisticBuilder.() -> Unit) {
        val builder = OuterTotalisticBuilder(neighbourhood)
        builder.init()

        addTransitions(builder.build())
    }

    /**
     * Constructs the @TABLE directive
     * @return Returns the @TABLE directive
     */
    fun build(): RuletableDirective {
        // If not the transition is not specified, the cell remains in the same state
        variable("_any") { 0 until numStates }
        for (i in 0 until numStates) transition { "$i, ${"_any, ".repeat(neighbourhood.size)}$i" }

        return directive
    }

    private fun addTransitions(transitions: Iterable<List<String>>) {
        transitions.forEach {
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
            val transition = Transition(valuesMap, variablesMap)
            transition.numStates = numStates
            directive.addTransition(transition)
        }
    }
}

/**
 * A helper class for constructing outer-totalistic transitions for the @TABLE directive
 * @param neighbourhood The neighbourhood of the outer-totalistic transition
 * @param weights The weights of the outer-totalistic transition
 * @property neighbourhood The neighbourhood of the outer-totalistic transition
 * @property weights The weights of the outer-totalistic transition
 */
@TableDirectiveMarker
class OuterTotalisticBuilder(val neighbourhood: Array<Coordinate>, var weights: IntArray? = null) {
    /**
     * The input cell of the transition
     */
    var input: String = "0"

    /**
     * The output cell of the transition
     */
    var output: String = "1"

    /**
     * The transitions to be added to the @TABLE directive
     */
    private var transitions: ArrayList<List<String>> = arrayListOf()

    /**
     * Adds the specified transitions to the @TABLE directive
     * @param transition A function returning a map where the key is the variable / state-literal and
     * the value is the number of times it should appear in the transition
     */
    fun transition(transition: () -> Map<String, Int>) {
        if (weights == null) {
            // Initialising the transitions
            val initialTransition = arrayListOf<String>()
            transition().forEach { (variable, occurrences) ->
                for (i in 0 until occurrences) initialTransition.add(variable)
            }

            // Find all permutations
            transitions.addAll(PermuteSymmetry()(initialTransition).map { listOf(input) + it + listOf(output) })
        }
    }

    /**
     * Adds the specified transitions to the @TABLE directive
     * @param var0 The variable / state representing 0
     * @param var1 The variable / state representing 1
     * @param transition The number of [var1] in the transition
     */
    fun transition(var0: String = "0", var1: String = "1", transition: Int) {
        transition { mapOf(var0 to neighbourhood.size - transition, var1 to transition) }
    }

    /**
     * Adds the specified transitions to the @TABLE directive
     * @param var0 The variable / state representing 0
     * @param var1 The variable / state representing 1
     * @param transitions A function returning a list of the numbers of [var1] in each transition
     */
    fun transitions(var0: String = "0", var1: String = "1", transitions: () -> Iterable<Int>) =
        transitions().forEach { transition(var0, var1, it) }

    /**
     * Constructs the transitions to be added to the @TABLE directive
     * @return Returns the transitions to be added to the @TABLE directive
     */
    fun build() = transitions
}
