package patterns.gliderdb

import patterns.PatternCollection
import patterns.Spaceship
import patterns.parseSpeed
import patterns.fromGliderDBEntry

import kotlin.math.abs

import rules.RuleFamily
import rules.RuleRange
import rules.RuleRangeable

/**
 * Represents a database of gliders (spaceships) / oscillators.
 * @param lst A list of spaceships to initialise the database with.
 */
class GliderDB<R>(lst: List<Spaceship>): PatternCollection<Spaceship>() where R : RuleFamily, R : RuleRangeable<R> {
    /**
     * The list containing all the spaceships.
     */
    val lst = ArrayList(lst)

    /**
     * Builds the GliderDB database from a string.
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
     * @param higherPeriod Should ships of the same speed but higher period be returned?
     */
    fun searchBySpeed(dx: Int, dy: Int, period: Int, higherPeriod: Boolean = false): GliderDB<R> {
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

    /**
     * Searches for spaceships moving at [speed].
     * @param higherPeriod Should ships of the same speed but higher period be returned?
     */
    fun searchBySpeed(speed: String, higherPeriod: Boolean = false): GliderDB<R> {
        val (displacement, period) = parseSpeed(speed)
        val (dx, dy) = displacement
        return searchBySpeed(dx, dy, period, higherPeriod)
    }

    /**
     * Searches for spaceships moving along the slope ([dx], [dy]).
     */
    fun searchBySlope(dx: Int, dy: Int): GliderDB<R> {
        require(dx != 0 || dy != 0) { "(0, 0) is not a valid slope." }
        return GliderDB(
            this.filter {
                if (maxOf(abs(it.dx), abs(it.dy)) == 0) false
                else abs(minOf(abs(it.dx), abs(it.dy)) / maxOf(abs(it.dx), abs(it.dy)).toDouble() - 
                    minOf(abs(dx), abs(dy)) / maxOf(abs(dx), abs(dy)).toDouble()) < 1e-12
            }
        )
    }

    /**
     * Searches for ships that work in [rule].
     */
    fun searchByRule(rule: R) = GliderDB<R>(
        this.filter { rule.between(it.ruleRange!!.minRule as R, it.ruleRange!!.maxRule as R) }
    )

    /**
     * Searches for ships that work in [ruleRange].
     */
    fun searchByRule(ruleRange: RuleRange<R>) = GliderDB<R>(
        this.filter { 
            ruleRange intersect (it.ruleRange!! as RuleRange<R>) != null
        }
    )

    /**
     * Searches for ships with population less than the given [population]
     */
    fun searchByPopulation(population: Int) = GliderDB<R>(
        this.filter { it.smallestPhase.population < population }
    )

    /**
     * Searches for ships whose area is less than the given [area]
     */
    fun searchByArea(area: Int) = GliderDB<R>(
        this.filter { it.canonPhase.bounds.area < area }
    )

    /**
     * Searches for ships with a smaller width and height than the given [width] and [height]
     */
    fun searchByBoundingBox(width: Int, height: Int) = GliderDB<R>(
        this.filter { it.canonPhase.bounds.width < width && it.canonPhase.bounds.width < height }
    )

    /**
     * Checks if a given [spaceship] is redundant or makes another ship redundant and
     * outputs the other spaceship.
     */
    fun checkRedundant(spaceship: Spaceship): List<Pair<Spaceship, Spaceship>> {
        val output = ArrayList<Pair<Spaceship, Spaceship>>()
        for (i in lst.indices) {
            if (lst[i].simplifiedSpeed == spaceship.simplifiedSpeed) {
                // Check if the rule ranges are the same
                if (lst[i].ruleRange!! == spaceship.ruleRange!!) {
                    if (lst[i].canonPhase.bounds.area > spaceship.canonPhase.bounds.area)
                        output.add(Pair(lst[i], spaceship))
                    else if (lst[i].canonPhase.bounds.area < spaceship.canonPhase.bounds.area)
                        output.add(Pair(spaceship, lst[i]))
                    else if (lst[i].canonPhase.population > spaceship.canonPhase.population)
                        output.add(Pair(lst[i], spaceship))
                    else
                        output.add(Pair(spaceship, lst[i]))
                } else {
                    // Check if either rule range contains the other
                    val intersection =
                        lst[i].ruleRange!! as RuleRange<R> intersect spaceship.ruleRange!! as RuleRange<R>
//                    println("${lst[i].ruleRange} ${spaceship.ruleRange} $intersection " +
//                            "${lst[i].canonPhase.bounds.area} ${spaceship.canonPhase.bounds.area}" +
//                            "${spaceship.canonPhase.toRLE(maxLineLength = Int.MAX_VALUE)}")
                    if (intersection == lst[i].ruleRange) {
                        // The ship is only redundant if it covers both a smaller rule range and has a larger bounding box
                        if (lst[i].canonPhase.bounds.area > spaceship.canonPhase.bounds.area)
                            output.add(Pair(lst[i], spaceship))
                    } else if (intersection == spaceship.ruleRange) {
                        // The ship is only redundant if it covers both a smaller rule range and has a larger bounding box
                        if (lst[i].canonPhase.bounds.area < spaceship.canonPhase.bounds.area)
                            output.add(Pair(spaceship, lst[i]))
                    }
                }
            }
        }

        return output
    }

    // Reading and writing database to and from a string
    override fun toString(): String = lst.map { it.gliderdbEntry }.joinToString("\n")
}