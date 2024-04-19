package patterns

import rules.Rule
import rules.RuleRange

/**
 * Represents a collection of patterns
 */
abstract class PatternCollection<T : Pattern>: MutableCollection<T> {
    /**
     * Searches for all patterns discovered by [discoverer]
     */
    open fun search(discoverer: String): List<Pattern> = this.filter { discoverer in it.discoverer }.toList()
}