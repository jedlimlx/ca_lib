package rules.ruleloader

/**
 * Represents a directive within a ruletable
 * @param name The name of the directive
 * @property name The name of the directive
 */
abstract class Directive(val name: String) {
    /**
     * The body of the direction when placed in an actual ruletable
     */
    abstract fun export(): String

    /**
     * Converts the directive to a string
     */
    override fun toString(): String {
        return "@${name.uppercase()}\n${export()}"
    }
}