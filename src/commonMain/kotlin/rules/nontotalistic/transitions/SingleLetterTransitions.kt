package rules.nontotalistic.transitions

import PLATFORM
import readResource

/**
 * Represents isotropic non-totalistic transitions that only use a single letter to represent each transition.
 */
abstract class SingleLetterTransitions: INTTransitions() {
    /**
     * A lookup table for the transitions
     */
    abstract val transitionLookup: Array<Map<Char, List<Int>>>

    /**
     * A lookup table for the transition string given the transition
     */
    abstract val reverseTransitionLookup: Map<List<Int>, String>

    override val regex: Regex by lazy {
        Regex("(" + transitionLookup.mapIndexed { index, charMap ->
            val letters = charMap.keys.joinToString("")
            index.toString() + if (charMap.size > 1) "(-[$letters]+|[$letters]*)" else ""
        }.joinToString("|") + ")")
    }

    override val transitions: Set<List<Int>> get() = _transitions
    private val _transitions: MutableSet<List<Int>> = HashSet()

    override val transitionStrings: Set<String> get() = _transitionStrings
    private val _transitionStrings: MutableSet<String> = HashSet()

    override fun parseTransition(string: String) {
        val regex = if (PLATFORM == "NATIVE") {
            Regex("(" + transitionLookup.mapIndexed { index, charMap ->
                val letters = charMap.keys.joinToString("")
                index.toString() + if (charMap.size > 1) "(-[$letters]+|[$letters]*)" else ""
            }.joinToString("|") + ")")
        } else regex

        regex.findAll(string).forEach {
            val block = it.groupValues[1]

            if (block.isNotEmpty()) {
                // Check for individual transitions
                val number = block[0].digitToInt()
                if (block.length > 1) {
                    if (block[1] == '-') {
                        val forbidden = block.substring(2)
                        transitionLookup[number].forEach { (key, value) ->
                            if (key !in forbidden) {
                                _transitions.addAll(symmetry(value))
                                _transitionStrings.add("$number$key")
                            }
                        }
                    } else block.substring(1).forEach {
                        transitionLookup[number][it]?.let { it1 ->
                            _transitions.addAll(symmetry(it1))
                            _transitionStrings.add("$number$it")
                        }
                    }
                } else {  // Handle singular outer-totalistic transition
                    transitionLookup[number].forEach { (key, value) ->
                        _transitions.addAll(symmetry(value))
                        _transitionStrings.add("$number$key")
                    }
                }
            }
        }
    }

    override fun canoniseTransition(): String = StringBuilder().apply {
        var currNum = -1
        val block = StringBuilder()
        (transitionStrings.sorted() + listOf("")).forEach {
            if (it.isNotEmpty() && it[0].digitToInt() == currNum) block.append(it[1])
            else {
                // Clear the previous transition block
                if (currNum != -1 && block.length - 1 > transitionLookup[currNum].size / 2) {
                    append(currNum)  // Append the number to the builder
                    if (block.length < transitionLookup[currNum].size + 1) append("-")
                    transitionLookup[currNum].keys.sorted().forEach {
                        if (it !in block) append(it)
                    }
                } else append(block)

                block.clear()

                // Add a new transition block
                if (it.isNotEmpty()) {
                    currNum = it[0].digitToInt()
                    block.append(it)
                }
            }
        }
    }.toString()

    override fun stringFromTransition(transition: List<Int>): String {
        var value: String? = null
        symmetry(transition).forEach {
            if (it in reverseTransitionLookup) {
                value = reverseTransitionLookup[it]!!
                return@forEach
            }
        }

        return value ?: throw IllegalArgumentException("Invalid transition: $transition")
    }

    override fun transitionFromString(string: String): List<Int> = transitionLookup[string[0].toString().toInt()][string[1]]!!

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
     * Loads the given transitions into the INT transition
     * @param transitions The transitions to load
     */
    protected fun loadTransitions(transitions: Iterable<List<Int>>) {
        transitions.forEach {
            _transitions.addAll(symmetry(it))
            _transitionStrings.add(reverseTransitionLookup[it] ?: throw IllegalArgumentException("Invalid transition: $it"))
        }
    }
}