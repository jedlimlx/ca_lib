package rules.nontotalistic.transitions


/**
 * Represents isotropic non-totalistic transitions that use 2 letters to represent each transition.
 */
abstract class DoubleLetterTransitions: INTTransitions() {
    /**
     * A lookup table for the isotropic transitions for the inner 8 cells
     */
    abstract val isotropicTransitionLookup: Array<Map<Char, List<Int>>>

    /**
     * A lookup table for the anisotropic transitions for the outer 4 cells
     */
    abstract val anisotropicTransitionLookup: Map<Char, List<Int>>

    /**
     * A lookup table for the complete isotropic transition string given the transition
     */
    abstract val reverseTransitionLookup: Map<List<Int>, String>

    /**
     * A lookup table for the string transitions mapped to the corresponding number of outer totalistic states.
     */
    abstract val transitionsByOuterTotalistic: Array<Set<String>>

    override val regex: Regex by lazy {
        // building the normal transitions
        val ordinaryTransitions = StringBuilder("[0-8]([x")
        for (isotropicChar in isotropicTransitionLookup[isotropicTransitionLookup.size / 2].keys) {
            ordinaryTransitions.append(isotropicChar)
        }

        ordinaryTransitions.append("][")
        for (anisotropicChar in anisotropicTransitionLookup.keys) {
            ordinaryTransitions.append(anisotropicChar)
        }

        ordinaryTransitions.append("])+")

        // building the negated transitions
        val negatedTransitions = Array(neighbourhood.size + 1) { StringBuilder("(") }
        val transitionsByOuterTotalistic = Array<MutableSet<String>>(neighbourhood.size + 1) { mutableSetOf() }
        isotropicTransitionLookup.forEachIndexed { num, item ->
            item.keys.sorted().forEach { isotropicTransition ->
                anisotropicTransitionLookup.keys.sorted().forEach { anisotropicTransition ->
                    val string = "${num}${if (isotropicTransition != '!') isotropicTransition else "x"}${anisotropicTransition}"
                    val transition = item[isotropicTransition]!! + anisotropicTransitionLookup[anisotropicTransition]!!
                    transitionsByOuterTotalistic[transition.sum()].add(string)
                }
            }
        }

        negatedTransitions.forEachIndexed { index, it ->
            it.apply {
                val sortedTransitions = transitionsByOuterTotalistic[index].sorted()

                // combining them into the transition string
                var currentNum = sortedTransitions[0][0].digitToInt()
                append(currentNum)
                append("(")

                var beginning = true
                sortedTransitions.forEach {
                    val num = it[0].digitToInt()
                    if (num == currentNum) {
                        if (!beginning) append("|") else beginning = false
                        append(it.substring(1))
                    } else {
                        currentNum = num

                        append(")+")
                        append("|")
                        append(num)
                        append("(")
                        append(it.substring(1))
                    }
                }

                append(")+)+")
            }
        }

        Regex(
            negatedTransitions.mapIndexed { index, it -> "${index}x-$it" }.joinToString("|") +
                    "|$ordinaryTransitions|[0-9]+x"
        )
    }

    override val transitions: Set<List<Int>> get() = _transitions
    private val _transitions: MutableSet<List<Int>> = HashSet()

    override val transitionStrings: Set<String> get() = _transitionStrings
    private val _transitionStrings: MutableSet<String> = HashSet()

    override fun parseTransition(string: String) {
        val regex = regex

        regex.findAll(string).forEach {
            val block = it.groupValues[0]
            if (block.isNotEmpty()) {
                if (block.last() == 'x' || "x-" in block) {
                    val tokens = block.split("x")

                    // get outer-totalistic transition number
                    val outerTotalisticNumber = tokens[0].toInt()
                    loadStringTransitions(transitionsByOuterTotalistic[outerTotalisticNumber])

                    // remove transitions
                    if ("-" in tokens[1]) {
                        regex.findAll(tokens[1]).forEach {
                            // Check for individual transitions
                            val number = it.groupValues[0][0]
                            val transitions = it.groupValues[0].substring(1).chunked(2).map { number + it }
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
        val transitionsByOuterTotalistic = Array<MutableSet<String>>(neighbourhood.size + 1) { mutableSetOf() }
        for (transition in transitionStrings) {
            transitionsByOuterTotalistic[
                transition[0].digitToInt() + anisotropicTransitionLookup[transition[2]]!!.sum()
            ].add(transition)
        }

        // now deal with the ones that need to be negated
        val transitionStrings = transitionStrings.toMutableSet()
        val transitionBlocks = HashSet<String>()
        transitionsByOuterTotalistic.forEachIndexed { index, transitions ->
            val maxTransitions = this@DoubleLetterTransitions.transitionsByOuterTotalistic[index]

            val transitionBlock = StringBuilder().apply {
                if (transitions.size == maxTransitions.size) {
                    append("${index}x")
                    transitionStrings.removeAll(maxTransitions)
                } else if (transitions.size > maxTransitions.size / 2) {
                    append("${index}x-")

                    // sort transition that are to be added
                    val sortedTransitions = (maxTransitions - transitions).sorted()
                    transitionStrings.removeAll(transitions)

                    // combining them into the transition string
                    var currentNum = sortedTransitions[0][0].digitToInt()
                    append(currentNum)

                    sortedTransitions.forEach {
                        val num = it[0].digitToInt()
                        if (num == currentNum) append(it.substring(1))
                        else {
                            currentNum = num
                            append(it)
                        }
                    }
                }
            }.toString()

            transitionBlocks.add(transitionBlock)
        }

        // adding in the miscellaneous transitions
        val transitionBlock = StringBuilder()
        val sortedTransitionStrings = transitionStrings.sorted()
        if (sortedTransitionStrings.isNotEmpty()) {
            var currentNum = sortedTransitionStrings[0][0].digitToInt()
            transitionBlock.append(currentNum)

            sortedTransitionStrings.forEach {
                val num = it[0].digitToInt()
                if (num == currentNum) transitionBlock.append(it.substring(1))
                else {
                    currentNum = num
                    transitionBlocks.add(transitionBlock.toString())

                    // reset transition block
                    transitionBlock.clear()
                    transitionBlock.append(it)
                }
            }

            transitionBlocks.add(transitionBlock.toString())
        }

        // finally complete transition string
        transitionBlocks.sortedBy {
            if ("x" in it) {
                it.split("x")[0].toInt() * 2 + 1
            } else if (it.isNotEmpty()) it[0].digitToInt() * 2
            else 0
        }.forEach{ append(it) }
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
            _transitionStrings.add(stringFromTransition(transitionFromString(it)))
        }
    }

    /**
     * Removes the given transitions from the INT transition
     * @param transitions The transitions to remove
     */
    protected fun removeStringTransitions(transitions: Iterable<String>) {
        transitions.forEach {
            _transitions.removeAll(symmetry(transitionFromString(it)))
            _transitionStrings.remove(stringFromTransition(transitionFromString(it)))
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
     * Generates the reverse lookup tables and the lookup tables that map outer-totalistic transitions to their corresponding
     * isotropic transitions.
     * @return Returns the reverse lookup table and the lookup tables that map outer-totalistic transitions to their corresponding
     * isotropic transitions.
     */
    protected fun generateReverseLookup(): Pair<Map<List<Int>, String>, Array<Set<String>>> {
        val reverseTransitionLookup: MutableMap<List<Int>, String> = mutableMapOf()
        val transitionsByOuterTotalistic = Array<MutableSet<String>>(neighbourhood.size + 1) { mutableSetOf() }

        isotropicTransitionLookup.forEachIndexed { num, item ->
            item.keys.sorted().forEach { isotropicTransition ->
                anisotropicTransitionLookup.keys.sorted().forEach { anisotropicTransition ->
                    val string = "${num}${if (isotropicTransition != '!') isotropicTransition else "x"}${anisotropicTransition}"
                    val transition = item[isotropicTransition]!! + anisotropicTransitionLookup[anisotropicTransition]!!
                    if (transition !in reverseTransitionLookup) {
                        symmetry(transition).forEach { reverseTransitionLookup[it] = string }
                        transitionsByOuterTotalistic[transition.sum()].add(string)
                    }
                }
            }
        }

        return Pair(reverseTransitionLookup.toMap(), transitionsByOuterTotalistic.map { it.toSet() }.toTypedArray())
    }
}
