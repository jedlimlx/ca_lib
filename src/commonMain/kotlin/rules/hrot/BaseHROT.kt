package rules.hrot

import NEIGHBOURHOOD_SYMBOLS
import parseCoordCA
import parseSymbol
import parseWeights
import rules.RuleFamily
import simulation.Coordinate
import toCoordCA
import toSymbol
import toWeights
import kotlin.random.Random

/**
 * The base class for HROT rules
 */
abstract class BaseHROT : RuleFamily() {
    /**
     * The weights of the HROT rule
     */
    abstract val weights: IntArray?

    /**
     * The string specifying the neighbourhood of the rule in the rulestring
     */
    protected val neighbourhoodString: String by lazy {
        if (weights == null) toSymbol(neighbourhood[0])?.toString() ?: ("@" + toCoordCA(neighbourhood[0]))
        else "W" + toWeights(neighbourhood[0], weights!!)
    }

    /**
     * Reads the neighbourhood specifier in the HROT rulestring
     * @param range The range of the neighbourhood
     * @param neighbourhood The neighbourhood specifier in the HROT rulestring
     * @return Returns the neighbourhood as an array of coordinates
     */
    protected fun readNeighbourhood(range: Int, neighbourhood: String): Pair<Array<Coordinate>, IntArray?> {
        return when (neighbourhood[0]) {
            '@' -> return Pair(parseCoordCA(range, neighbourhood.substring(1)), null)
            'W' -> return parseWeights(range, neighbourhood.substring(1))
            else -> parseSymbol(range, neighbourhood[0])
        }
    }

    /**
     * Reads the HROT / OT syntax for transitions
     * @param transition The transition specifier from the rulestring
     * @param higherRange Is the transition specifier from a higher range rule?
     * @return Returns the transitions
     */
    protected fun readTransition(transition: String, higherRange: Boolean = true): Set<Int> {
        return if (higherRange) {  // HROT Syntax
            val set = hashSetOf<Int>()
            transition.split(",").forEach {
                if ("-" in it) {
                    val tokens = it.split("-")
                    for (i in tokens[0].toInt()..tokens[1].toInt()) set.add(i)
                } else if (it.isNotEmpty()) set.add(it.toInt())
            }

            set
        } else HashSet(transition.map { it.toString().toInt() })
    }

    /**
     * Generates the canonical transition specification for the rule
     * @param transitions The transitions
     */
    protected fun canoniseTransition(transitions: Iterable<Int>): String {
        val sortedTransitions = transitions.sorted()

        // Code that I totally didn't steal from somewhere
        var idx = 0
        var idx2 = 0
        val len: Int = sortedTransitions.size

        val rulestring = StringBuilder()
        while (idx < len) {
            while (++idx2 < len && sortedTransitions[idx2] - sortedTransitions[idx2 - 1] == 1);
            if (idx2 - idx > 1) {
                rulestring.append("${sortedTransitions[idx]}-${sortedTransitions[idx2 - 1]},")
                idx = idx2
            } else {
                while (idx < idx2) {
                    rulestring.append("${sortedTransitions[idx]},")
                    idx++
                }
            }
        }

        if (rulestring.isEmpty()) rulestring.append(",")
        return rulestring.toString()
    }

    /**
     * Generates a random transition between the 2 minimum and maximum transitions
     * @return Returns the random transition
     */
    protected fun randomTransition(min: Iterable<Int>, max: Iterable<Int>, seed: Int? = null): Set<Int> {
        val new = min.toHashSet()

        val random = if (seed != null) Random(seed) else Random
        (max - min).forEach { if (random.nextBoolean()) new.add(it) }

        return new
    }

    /**
     * Generates a range of possible transitions that is the intersection of the 2 transition ranges.
     * Returns null if there is no intersection.
     */
    protected fun intersectTransitionRange(
        min: Iterable<Int>, max: Iterable<Int>, 
        min2: Iterable<Int>, max2: Iterable<Int>
    ): Pair<Iterable<Int>, Iterable<Int>>? {
        // Initialise the 2 arrays that will store the transitions
        val transitionArray1 = IntArray((weights?.sum() ?: neighbourhood[0].size) + 1) { 0 }
        min.forEach { transitionArray1[it] = 1 }
        max.forEach { if (transitionArray1[it] == 0) transitionArray1[it] = -1 }
        
        val transitionArray2 = IntArray((weights?.sum() ?: neighbourhood[0].size) + 1) { 0 }
        min2.forEach { transitionArray2[it] = 1 }
        max2.forEach { if (transitionArray2[it] == 0) transitionArray2[it] = -1 }

        // Compute their intersection
        var quit = false
        val newTransitionArray = IntArray((weights?.sum() ?: neighbourhood[0].size) + 1) {
            if (
                transitionArray1[it] != transitionArray2[it] && 
                transitionArray1[it] != -1 && transitionArray2[it] != -1
            ) quit = true

            maxOf(transitionArray1[it], transitionArray2[it])
         }

         // Check if the transitions outputted are actually valid
         if (!quit) {
            // Decode the array back into transitions
            val newMin = hashSetOf<Int>()
            val newMax = hashSetOf<Int>()
            newTransitionArray.forEachIndexed { index, it -> 
                if (it == 1) {
                    newMin.add(index)
                    newMax.add(index)
                } else if (it == -1) {
                    newMax.add(index)
                }
            }

            return Pair(newMin, newMax)
         } 
         
         return null
    }

    fun getAllSubsetSums(weights: MutableMap<Int, Int>): IntArray {
        if (weights.isEmpty()) return intArrayOf(1)

        val maxCount = weights.map { (key, value) -> key * value }.sum()

        val weight = weights.keys.max()
        val numWeights = weights[weight]!!
        weights.remove(weight)
        
        val old = getAllSubsetSums(weights)
        val newArray = IntArray(maxCount+1) { if (it < old.size) old[it] else 0 }

        for (i in old.indices) {
            if (old[i] == 1) {
                for (j in 1 .. numWeights) 
                    newArray[i + j*weight] = 1
            }
        }

        return newArray
    }

    companion object {
        const val transitionRegex = "(((\\d,(?=\\d))|(\\d-(?=\\d))|\\d)+)?"
        const val neighbourhoodRegex = "(,N(@([A-Fa-f0-9]+)?[HL]?|W[A-Fa-f0-9]+[HL]?|[$NEIGHBOURHOOD_SYMBOLS]))?"
    }
}