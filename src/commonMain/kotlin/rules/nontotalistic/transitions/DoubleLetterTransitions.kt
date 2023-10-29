package rules.nontotalistic.transitions
/*

import PLATFORM

abstract class DoubleLetterTransitions: INTTransitions() {
    /**
     * A lookup table for the isotropic transitions for the inner 8 cells
     */
    abstract val isotropicTransitionLookup: Array<Map<Char, List<Int>>>

    /**
     * A lookup table for the isotropic transition string for the inner 8 cells given the transition
     */
    abstract val isotropicReverseTransitionLookup: Map<List<Int>, String>

    /**
     * A lookup table for the anisotropic transitions for the outer 4 cells
     */
    abstract val anisotropicTransitionLookup: Map<Char, List<Int>>

    /**
     * A lookup table for the anisotropic transition string given the transition for the outer 4 cells
     */
    abstract val anisotropicReverseTransitionLookup: Map<List<Int>, String>

    /**
     * A lookup table for the complete isotropic transition string given the transition
     */
    abstract val reverseTransitionLookup: Map<List<Int>, String>

    override val regex: Regex by lazy {
        val ordinaryTransitions = StringBuilder("[0-8]([x")
        for (isotropicChar in isotropicTransitionLookup[isotropicTransitionLookup.size / 2].keys) {
            ordinaryTransitions.append(isotropicChar)
        }

        ordinaryTransitions.append("][")
        for (anisotropicChar in anisotropicTransitionLookup.keys) {
            ordinaryTransitions.append(anisotropicChar)
        }

        ordinaryTransitions.append("])+")

        Regex("[0-9]+x-($ordinaryTransitions)+|$ordinaryTransitions|[0-9]+x")
    }

    override val transitions: Set<List<Int>> get() = _transitions
    private val _transitions: MutableSet<List<Int>> = HashSet()

    override val transitionStrings: Set<String> get() = _transitionStrings
    private val _transitionStrings: MutableSet<String> = HashSet()

    override fun parseTransition(string: String) {
        val regex = regex

        regex.findAll(string).forEach {
            val block = it.groupValues[1]

            if (block.isNotEmpty()) {
                if ('x' in block) {
                    val tokens = block.split("x")

                    // get outer-totalistic transition number
                    val outerTotalisticNumber = tokens[0].toInt()


                    // remove transitions
                    if ("-" in tokens[1]) {
                        regex.findAll(tokens[1]).forEach {
                            // Check for individual transitions
                            val number = block[0]
                            val transitions = block.substring(1).chunked(2).map { number + it }
                            removeStringTransitions(transitions)
                        }
                    }
                } else {
                    // Check for individual transitions
                    val number = block[0]
                    val transitions = block.substring(1).chunked(2).map { number + it }
                    loadStringTransitions(transitions)
                }
            }
        }
    }

    override fun canoniseTransition(): String = StringBuilder().apply {
        // computing numbers of each outer-totalistic transition
        val count = IntArray(neighbourhood.size)
        for (transition in transitionStrings) {
            count[transition[0].digitToInt() + anisotropicTransitionLookup[transition[2]]!!.sum()]++
        }

        // now deal with the ones that need to be negated
        val sortedTransitionStrings = transitionStrings.toSortedSet()
        count.forEachIndexed { index, num ->
            val maxCount = ...
            if (num == maxCount)
                append("${index}x")
            else if (num > maxCount) {

            }
        }
    }.toString()

    override fun stringFromTransition(transition: List<Int>): String =
        reverseTransitionLookup[transition] ?: throw IllegalArgumentException("Invalid transition: $transition")

    override fun transitionFromString(string: String): List<Int> = (
        isotropicTransitionLookup[string[0].digitToInt()][string[1]] ?:
        throw IllegalArgumentException("Invalid transition: $string")
    ) + (
        anisotropicTransitionLookup[string[2]] ?:
        throw IllegalArgumentException("Invalid transition: $string")
    )

    /**
     * Loads the given transitions into the INT transition
     * @param transitions The transitions to load
     */
    protected fun loadStringTransitions(transitions: Iterable<String>) {
        transitions.forEach {
            _transitions.addAll(symmetry(transitionFromString(it)))
            _transitionStrings.add(it)
        }
    }

    /**
     * Removes the given transitions from the INT transition
     * @param transitions The transitions to remove
     */
    protected fun removeStringTransitions(transitions: Iterable<String>) {
        transitions.forEach {
            _transitions.removeAll(symmetry(transitionFromString(it)))
            _transitionStrings.remove(it)
        }
    }

    /**
     * Loads the given transitions into the INT transition
     * @param transitions The transitions to load
     */
    protected fun loadTransitions(transitions: Iterable<List<Int>>) {
        transitions.forEach {
            _transitions.addAll(symmetry(it))
            _transitionStrings.add(
                (
                    reverseTransitionLookup[it]
                    ) ?: throw IllegalArgumentException("Invalid transition: $it")
            )
        }
    }

    /**
     * Reads the transition lookup table from a txt resource file
     * @param resource The contents of the txt resource file
     * @return Returns the lookup table and the reversed lookup table
     */
    protected fun readTransitionsFromResources(resource: String): Pair<Array<Map<Char, List<Int>>>, Map<List<Int>, String>> {

    }
}
 */