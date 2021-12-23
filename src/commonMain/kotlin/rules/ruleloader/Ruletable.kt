package rules.ruleloader

/**
 * Represents a Golly / Apgsearch ruletable
 * @constructor Constructs a ruletable from the given parameters
 * @param name The name of the ruletable
 * @property name The name of the ruletable
 * @param directive The directives of the ruletable which store metadata about the rule
 * @property directive The directives of the ruletable which store metadata about the rule
 * @param ruleDirectives The directives of the ruletable which store information about the rule's transitions
 * @property ruleDirectives The directives of the ruletable which store metadata about the rule's transitions
 */
class Ruletable(val name: String, val directive: List<Directive>, val ruleDirectives: List<RuleDirective>) {
    /**
     * Exports the ruletable as a string
     */
    fun export(): String = with(StringBuilder()) {
        append("@RULE $name\n\n")
        ruleDirectives.forEach { append("$it\n\n") }  // TODO (Handle alternating rules)
        directive.forEach { append("$it\n\n") }

        this
    }.toString()

    /**
     * Converts the ruletable to a string. Has the same function as [export].
     */
    override fun toString(): String = export()
}