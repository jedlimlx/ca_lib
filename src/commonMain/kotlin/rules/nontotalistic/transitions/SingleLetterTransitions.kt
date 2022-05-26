package rules.nontotalistic.transitions

import readResource

/**
 * Represents isotropic non-totalistic transitions that only use a single letter to represent the transition
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
        regex.findAll(string).forEach {
            val block = it.groupValues[1]

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
                    transitionLookup[currNum].keys.forEach {
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

    /**
     * Reads the transition lookup table from a txt resource file
     * @param resource The contents of the txt resource file
     * @return Returns the lookup table and the reversed lookup table
     */
    protected fun readTransitionsFromResources(resource: String): Pair<Array<Map<Char, List<Int>>>, Map<List<Int>, String>> {
        val transitionLookup: ArrayList<MutableMap<Char, List<Int>>> = arrayListOf()
        val reverseTransitionLookup: MutableMap<List<Int>, String> = mutableMapOf()

        var currDigit = '0'
        val string = readResource(resource)
        for (line in string.split("\n")) {
            if (line.trim().isNotEmpty()) {
                if (line.trim()[0].isDigit()) {
                    currDigit = line.trim()[0]
                    transitionLookup.add(HashMap())
                } else {
                    val lst = line.trim().split(" ").subList(1, neighbourhood.size + 1).map { it.toInt() }
                    transitionLookup.last()[line[0]] = lst
                    reverseTransitionLookup[lst] = "$currDigit${line[0]}"
                }
            }
        }

        return Pair(transitionLookup.toTypedArray(), reverseTransitionLookup)
    }
}