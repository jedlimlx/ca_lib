package rules.ruleloader.ruletable

import moore
import rules.ruleloader.RuleDirective
import simulation.Coordinate

internal fun convertWithIndex(state: Int, bg: Int, bgIndex: Int, numStates: Int): Int {
    if (state == 0) {
        return if (bg != 0) bg + ((numStates - 1) * bgIndex) else 0
    } else if (state == bg) return 0
    return state + (numStates - 1) * bgIndex
}

/**
 * Represents the @TABLE directive in a Golly / Apgsearch ruletable
 * @constructor Constructs the directive of the specified parameters
 * @param numStates The number of states of the table directive
 * @param neighbourhood The neighbourhood of the table directive
 */
class RuletableDirective(
    override val numStates: Int = 2,
    override var neighbourhood: Array<Coordinate> = moore(1),
    override val background: IntArray
) : RuleDirective("table") {
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

    override fun transitionFunc(cells: IntArray, cellState: Int): Int = TODO("Not yet implemented")

    override fun export(): String = with(StringBuilder()) {
        // Headers
        append("n_states:${1 + background.size * (numStates - 1)}\n")
        append("neighborhood:[${neighbourhood.joinToString(", ")}]\n")
        append("symmetries:none\n\n")

        // Inserting variables, transitions and comments
        for (j in background.indices) {
            val bg = background[j]
            for (i in variables.indices) {
                append(
                    when {
                        variables[i] != null -> {
                            variables[i]!!.background = bg
                            variables[i]!!.backgroundIndex = j

                            if (variables[i]!!.unbounded)
                                variables[i]!!.toString(neighbourhood.size) + "\n\n"
                            else "${variables[i]}\n"
                        }
                        transitions[i] != null -> {
                            transitions[i]!!.background = background
                            transitions[i]!!.backgroundIndex = j

                            "${transitions[i]}\n"
                        }
                        comments[i] != null -> "\n# ${comments[i]}\n"
                        else -> ""
                    }
                )
            }

            append("\n")
        }

        this
    }.toString()

    internal fun addVariable(variable: Variable) {
        _variables.add(variable)
        _transitions.add(null)
        _comments.add(null)
    }

    internal fun addTransition(transition: Transition) {
        _variables.add(null)
        _transitions.add(transition)
        _comments.add(null)
    }

    internal fun addComment(comment: String) {
        _variables.add(null)
        _transitions.add(null)
        _comments.add(comment)
    }
}