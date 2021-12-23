package rules.ruleloader.ruletable

import moore
import rules.ruleloader.RuleDirective
import simulation.Coordinate

/**
 * Represents the @TABLE directive in a Golly / Apgsearch ruletable
 * @constructor Constructs the directive of the specified parameters
 * @param numStates The number of states of the table directive
 * @param neighbourhood The neighbourhood of the table directive
 */
class RuletableDirective(val numStates: Int = 2,
                         var neighbourhood: Array<Coordinate> = moore(1)): RuleDirective("table") {
    // Properties of the ruletable to be exposed
    /**
     * The variables used in the subsequent transitions
     */
    val variables: List<Variable?> get() = _variables

    /**
     * The transitions in the ruletable
     */
    val transitions: List<Transition?> get() = _transitions

    /**
     * The comments within the variables section of the ruletable
     */
    val comments: List<String?> get() = _comments

    // Private Variables
    private val _variables: ArrayList<Variable?> = arrayListOf()
    private val _transitions: ArrayList<Transition?> = arrayListOf()
    private val _comments: ArrayList<String?> = arrayListOf()

    // TODO (Handle B0)

    override fun export(): String = with(StringBuilder()) {
        // Headers
        append("n_states:$numStates\n")
        append("neighborhood:[${neighbourhood.joinToString(", ")}]\n")
        append("symmetries:none\n\n")

        // Inserting variables, transitions and comments
        for (i in variables.indices) {
            append(when {
                variables[i] != null -> {
                    if (variables[i]!!.unbounded)
                        variables[i]!!.toString(neighbourhood.size) + "\n\n"
                    else "${variables[i]}\n"
                }
                transitions[i] != null -> "${transitions[i]}\n"
                comments[i] != null -> "# ${comments[i]}\n"
                else -> ""
            })
        }

        this
    }.toString()

    fun addVariable(variable: Variable) {
        _variables.add(variable)
        _transitions.add(null)
        _comments.add(null)
    }

    fun addTransition(transition: Transition) {
        _variables.add(null)
        _transitions.add(transition)
        _comments.add(null)
    }

    fun addComment(comment: String) {
        _variables.add(null)
        _transitions.add(null)
        _comments.add(comment)
    }
}