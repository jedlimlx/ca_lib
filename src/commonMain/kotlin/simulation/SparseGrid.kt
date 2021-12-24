package simulation

import Utils
import rules.PLACEHOLDER_RULE
import rules.Rule
import rules.hrot.HROT
import kotlin.math.max
import kotlin.math.min


/**
 * A representation of a cellular automaton grid via a sparse matrix.
 * This is suitable for cellular automaton that have large empty regions.
 * For smaller, denser patterns, [DenseGrid] will be more efficient
 * @constructor Constructs a grid based on the pattern provided
 */
class SparseGrid(pattern: String = "", rule: Rule = PLACEHOLDER_RULE): Grid() {
    private val dictionary: HashMap<Coordinate, Int> = hashMapOf()

    override val population
        get() = dictionary.size

    override val populationByState: IntArray
        get() {
            val result = IntArray(rule.numStates) { 0 }
            result[background] = -1

            forEach { result[it.second]++ }
            return result
        }

    init {
        this.rule = rule
        set(Coordinate(0, 0), pattern)
    }

    override fun clear() {
        dictionary.clear()
    }

    override operator fun set(coordinate: Coordinate, state: Int) {
        if (state != this[coordinate]) cellsChanged[0].add(coordinate)

        if (state == background) {
            dictionary.remove(coordinate)

            // If a cell on the boundary is removed, the bounds are no longer accurate
            if (boundsUpdated && (coordinate.x == bounds.start.x || coordinate.y == bounds.start.y ||
                    coordinate.x == bounds.start.x || coordinate.y == bounds.end.y))
                boundsUpdated = false
        } else {
            dictionary[coordinate] = Utils.convert(state, background)

            // If the cell is added outside the current bounds
            if (boundsUpdated && coordinate !in bounds) {
                val minX = min(bounds.start.x, coordinate.x)
                val maxX = max(bounds.end.x, coordinate.x)
                val minY = min(bounds.start.y, coordinate.y)
                val maxY = max(bounds.end.y, coordinate.y)

                // Set bounds to the new value
                bounds = Coordinate(minX, minY) .. Coordinate(maxX, maxY)
                boundsUpdated = true
            }
        }
    }

    override operator fun get(coordinate: Coordinate, withoutBg: Boolean): Int {
        return if (!withoutBg) Utils.convert(dictionary[coordinate] ?: 0, background)
        else dictionary[coordinate] ?: 0
    }

    override fun deepCopy(): SparseGrid {
        val grid = SparseGrid("", rule)
        grid.generation = generation
        grid.background = background
        grid.dictionary.putAll(dictionary)
        grid.cellsChanged = cellsChanged.map { it.toHashSet() }.toTypedArray()

        return grid
    }

    override fun iterator(): MutableIterator<Pair<Coordinate, Int>> = SparseGridIterator(dictionary, background)
}

internal class SparseGridIterator(dictionary: HashMap<Coordinate, Int>, val background: Int): MutableIterator<Pair<Coordinate, Int>> {
    val iterator = dictionary.iterator()

    override fun hasNext(): Boolean = iterator.hasNext()
    override fun next(): Pair<Coordinate, Int> {
        val entry = iterator.next()
        return Pair(entry.key, Utils.convert(entry.value, background))
    }

    override fun remove() = iterator.remove()
}