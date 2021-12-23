package rules.ruleloader.ruletable

/**
 * Represents a variable within a ruletable
 */
data class Variable(val name: String, val values: Set<Int>, val unbounded: Boolean = true) {
    fun identifier(index: Int) = if (unbounded) "$name.$index" else name

    fun toString(maxUsages: Int): String = List(maxUsages) {
        "var $name.$it = {${values.joinToString(", ")}}"
    }.joinToString("\n")

    override fun toString(): String = "var $name = {${values.joinToString(", ")}}"
}