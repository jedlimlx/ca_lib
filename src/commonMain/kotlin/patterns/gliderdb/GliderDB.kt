package patterns.gliderdb

import patterns.PatternCollection
import patterns.Spaceship
import patterns.parseSpeed
import patterns.fromGliderDBEntry

import kotlin.math.abs

import rules.Rule
import rules.RuleFamily
import rules.RuleRange

/**
 * Represents a database of gliders (spaceships) / oscillators.
 * @param lst A list of spaceships to initialise the database with.
 */
class GliderDB(lst: List<Spaceship>): PatternCollection<Spaceship>() {
    val lst = ArrayList(lst)

    /**
     * Builds the GliderDB database from a string
     */
    constructor(string: String): this(string.split("! ").map { fromGliderDBEntry(it) })

    // Basic properties that need to be implemented
    override val size: Int
        get() = lst.size

    override fun add(element: Spaceship): Boolean {
        return lst.add(element)
    }

    override fun addAll(elements: Collection<Spaceship>): Boolean { 
        return lst.addAll(elements)
     }

    override fun clear() { 
        return lst.clear()
    }

    override fun contains(element: Spaceship): Boolean = lst.contains(element)

    override fun containsAll(elements: Collection<Spaceship>): Boolean = lst.containsAll(elements)

    override fun isEmpty(): Boolean = lst.isEmpty()

    override fun iterator(): MutableIterator<Spaceship> = lst.iterator()

    override fun remove(element: Spaceship): Boolean = lst.remove(element)

    override fun removeAll(elements: Collection<Spaceship>): Boolean = lst.removeAll(elements)

    override fun retainAll(elements: Collection<Spaceship>): Boolean = lst.retainAll(elements)

    // Search functions for different types of spaceship

    /**
     * Searches for spaceships moving at ([dx], [dy])c/[period].
     * @param higher_periods Should ships of the same speed but higher period be returned?
     */
    fun searchBySpeed(dx: Int, dy: Int, period: Int, higherPeriod: Boolean = false): GliderDB {
        if (higherPeriod) {
            return GliderDB(
                this.filter {
                    abs(minOf(abs(it.dx), abs(it.dy)) / it.period.toDouble() - minOf(abs(dx), abs(dy)) / period.toDouble()) < 1e-12 &&
                    abs(maxOf(abs(it.dx), abs(it.dy)) / it.period.toDouble() - maxOf(abs(dx), abs(dy)) / period.toDouble()) < 1e-12
                }
            )
        } else {
            return GliderDB(
                this.filter { 
                    minOf(abs(it.dx), abs(it.dy)) == minOf(abs(dx), abs(dy)) && 
                    maxOf(abs(it.dx), abs(it.dy)) == maxOf(abs(dx), abs(dy)) && it.period == period 
                }
            )
        }
    }

    fun searchBySpeed(speed: String, higherPeriod: Boolean = false): GliderDB {
        val (displacement, period) = parseSpeed(speed)
        val (dx, dy) = displacement
        return searchBySpeed(dx, dy, period, higherPeriod)
    }

    fun searchBySlope(dx: Int, dy: Int): GliderDB {
        require(dx != 0 || dy != 0) { "(0, 0) is not a valid slope." }
        return GliderDB(
            this.filter {
                if (maxOf(abs(it.dx), abs(it.dy)) == 0) false
                else abs(minOf(abs(it.dx), abs(it.dy)) / maxOf(abs(it.dx), abs(it.dy)).toDouble() - 
                    minOf(abs(dx), abs(dy)) / maxOf(abs(dx), abs(dy)).toDouble()) < 1e-12
            }
        )
    }

    fun searchByRule(rule: RuleFamily) = GliderDB(
        this.filter { rule in (it.ruleRange!!.first as RuleFamily) .. (it.ruleRange!!.second as RuleFamily) }
    )

    fun searchByRule(ruleRange: RuleRange) = GliderDB(
        this.filter { 
            ruleRange intersect ((it.ruleRange!!.first as RuleFamily) .. (it.ruleRange!!.second as RuleFamily)) != null
        }
    )

    // Redundancy check
    fun checkRedundant(): List<Spaceship> {
        TODO("Not yet implemented")
    }

    // Reading and writing database to and from a string
    override fun toString(): String = lst.map { it.gliderdbEntry }.joinToString("\n")
}