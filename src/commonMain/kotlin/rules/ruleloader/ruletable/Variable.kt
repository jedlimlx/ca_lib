package rules.ruleloader.ruletable

/**
 * Represents a variable within a ruletable
 */
data class Variable(val name: String, val values: Set<Int>, val unbounded: Boolean = true) {
    internal var numStates = 0
    internal var background = 0
    internal var backgroundIndex = 0

    fun identifier(index: Int) = if (unbounded) "$name.$index.$backgroundIndex" else name

    fun toString(maxUsages: Int): String = List(maxUsages + 1) {
        "var $name.$it.$backgroundIndex = {${values.map {
            convertWithIndex(it, background, backgroundIndex, numStates)
        }.joinToString(", ")}}"
    }.joinToString("\n")

    override fun toString(): String = "var $name.$backgroundIndex = {${values.map {
        convertWithIndex(it, background, backgroundIndex, numStates)
    }.joinToString(", ")}}"
}