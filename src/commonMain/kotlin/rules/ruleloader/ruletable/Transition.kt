package rules.ruleloader.ruletable

/**
 * Represents a ruletable transition (with state literals and variables)
 * @property values The values (or state literals) of the transition
 * @property variables The variables of the transition
 */
data class Transition(val values: Map<Int, Int>, val variables: Map<Int, Variable>) {
    /**
     * The size of the transition
     */
    val size = values.size + variables.size

    init {
        require(size - 1 in values) { "The last part of transitions (the output) must be a state literal" }
        for (i in 0 until size)
            require(i in values || i in variables) {
                "Index $i must be contained in either the values or variables passed into Transition"
            }
    }

    /**
     * Converts the transition into a string to be placed in the ruletable
     * @return Returns the string to be placed in the ruletable
     */
    override fun toString(): String = List(size) {
        if (it in values) values[it].toString()
        else variables[it]!!.identifier(it)
    }.joinToString(" ")
}