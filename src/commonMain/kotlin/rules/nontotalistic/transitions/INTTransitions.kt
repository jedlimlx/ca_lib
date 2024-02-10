package rules.nontotalistic.transitions

import readResource
import rules.ruleloader.ruletable.Symmetry
import simulation.Coordinate

/**
 * Represents a set of isotropic non-totalistic transitions
 */
abstract class INTTransitions : Collection<List<Int>> {
    /**
     * The string representing the transition
     */
    val transitionString: String by lazy { canoniseTransition() }

    /**
     * The symmetry of the transition
     */
    abstract val symmetry: Symmetry

    /**
     * The neighbourhood of the transition
     */
    abstract val neighbourhood: Array<Coordinate>

    /**
     * A regex that can be used to identify the transition
     */
    abstract val regex: Regex

    /**
     * A set containing all the transitions stored as a list of integers
     */
    abstract val transitions: Set<List<Int>>

    /**
     * Each individual transition expressed as a string
     */
    abstract val transitionStrings: Set<String>

    /**
     * The number of transitions in the INT transition set
     */
    abstract override val size: Int

    /**
     * Reads the isotropic transition lookup table from a txt resource file
     * @param resource The contents of the txt resource file
     * @return Returns the lookup table and the reversed lookup table
     */
    protected fun readIsotropicTransitionsFromResources(resource: String):
            Pair<Array<Map<Char, List<Int>>>, Map<List<Int>, String>> {
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
                    val tokens = line.trim().split(" ")
                    if (tokens.size > 1) {
                        val lst = tokens.subList(1, neighbourhood.size + 1).map { it.toInt() }
                        transitionLookup.last()[line[0]] = lst
                        reverseTransitionLookup[lst] = "$currDigit${line[0]}"
                    } else break
                }
            }
        }

        return Pair(transitionLookup.toTypedArray(), reverseTransitionLookup)
    }

    /**
     * Reads the anisotropic transition lookup table from a txt resource file
     * @param resource The contents of the txt resource file
     * @return Returns the lookup table and the reversed lookup table
     */
    protected fun readDoubleTransitionsFromResources(resource: String, resource2: String):
            Pair<Array<Map<Char, List<Int>>>, Map<Char, List<Int>>> {
        val transitionLookup: ArrayList<MutableMap<Char, List<Int>>> = arrayListOf()

        var string = readResource(resource)
        for (line in string.split("\n")) {
            if (line.trim().isNotEmpty()) {
                if (line.trim()[0].isDigit()) {
                    transitionLookup.add(HashMap())
                } else {
                    val tokens = line.trim().split(" ")
                    if (tokens.size > 1) {
                        val lst = tokens.subList(1, neighbourhood.size - 4 + 1).map { it.toInt() }
                        transitionLookup.last()[if (line[0] != '!') line[0] else 'x'] = lst
                    } else break
                }
            }
        }


        val anisotropicTransitionLookup: MutableMap<Char, List<Int>> = mutableMapOf()

        string = readResource(resource2)
        for (line in string.split("\n")) {
            if (!line.matches("\\s*".toRegex())) {
                val tokens = line.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val transition = List(tokens.size - 1) { tokens[it + 1].toInt() }

                anisotropicTransitionLookup[tokens[0][0]] = transition
            }
        }


        return Pair(transitionLookup.toTypedArray(), anisotropicTransitionLookup)
    }

    /**
     * Checks if the given transition is contained in this transition
     * @param array The transition to check
     * @return Returns true if the given transition is contained in this transition and false otherwise
     */
    operator fun contains(array: IntArray): Boolean = array.toList() in this

    /**
     * Checks if the given transition is contained in this transition
     * @param element The transition to check
     * @return Returns true if the given transition is contained in this transition and false otherwise
     */
    override operator fun contains(element: List<Int>): Boolean = element in transitions

    /**
     * Checks if all given transitions are contained in this transition
     * @param elements The transitions to check
     * @return Returns true if the given transitions are contained in this transition and false otherwise
     */
    override fun containsAll(elements: Collection<List<Int>>): Boolean = elements.all { it in this.transitions }

    /**
     * Checks if all given transitions are contained in this transition
     * @param transitions The transitions to check
     * @return Returns true if the given transitions are contained in this transition and false otherwise
     */
    fun containsAll(transitions: INTTransitions): Boolean = transitions.transitions.all { it in this.transitions }

    /**
     * Adds the given transitions to the transition set
     */
    operator fun plus(stringList: Iterable<String>): INTTransitions {
        val new = this.clone()
        new.parseTransition(stringList.joinToString(""))

        return new
    }

    /**
     * Converts the given [transition] into a string
     * @return Returns the string corresponding to the given transition
     */
    abstract fun stringFromTransition(transition: List<Int>): String

    /**
     * Converts the given [string] into the transition
     * @return Returns the transition corresponding to the given string
     */
    abstract fun transitionFromString(string: String): List<Int>

    /**
     * Parses the given transition string and adds the transition to the transition set
     * @param string The transition string to parse
     */
    protected abstract fun parseTransition(string: String)

    /**
     * Returns the canonised string representing the transition
     * @return Returns the canonised string representing the transition
     */
    protected abstract fun canoniseTransition(): String

    /**
     * Creates a deep copy of the transition
     * @return Returns a deep copy of the transition
     */
    abstract fun clone(): INTTransitions

    override fun iterator(): Iterator<List<Int>> = transitions.iterator()

    override fun isEmpty(): Boolean = transitions.isEmpty()

    override fun toString(): String = transitionString
}