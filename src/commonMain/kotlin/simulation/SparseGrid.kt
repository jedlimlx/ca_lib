package simulation

import Utils
import kotlin.math.max
import kotlin.math.min


/**
 * A representation of a cellular automaton grid via a sparse matrix.
 * This is suitable for cellular automaton that have large empty regions.
 * @constructor Constructs an empty grid
 */
class SparseGrid(pattern: String = ""): Grid() {
    private val dictionary: HashMap<Coordinate, Int> = hashMapOf()

    init {
        set(Coordinate(0, 0), pattern)
    }

    override fun clear() {
        dictionary.clear()
    }

    override operator fun set(coordinate: Coordinate, state: Int) {
        if (state == background) {
            dictionary.remove(coordinate)

            // If a cell on the boundary is removed, the bounds are no longer accurate
            if (boundsUpdated && (coordinate.x == bounds.first.x || coordinate.y == bounds.first.y ||
                    coordinate.x == bounds.second.x || coordinate.y == bounds.second.y))
                boundsUpdated = false
        } else {
            dictionary[coordinate] = state

            // If the cell is added outside the current bounds
            if (boundsUpdated && bounds.first.x >= coordinate.x && coordinate.x >= bounds.second.x
                && bounds.first.y >= coordinate.y && coordinate.y >= bounds.second.y) {
                val minX = min(bounds.first.x, coordinate.x)
                val maxX = max(bounds.second.x, coordinate.x)
                val minY = min(bounds.first.y, coordinate.y)
                val maxY = max(bounds.second.y, coordinate.y)

                // Set bounds to the new value
                bounds = Pair(Coordinate(minX, minY), Coordinate(maxX, maxY))
                boundsUpdated = true
            }
        }
    }

    override operator fun get(coordinate: Coordinate): Int {
        val state = dictionary[coordinate] ?: return background
        return Utils.convert(state, background)
    }

    override fun population() = dictionary.size

    override fun deepCopy(): SparseGrid {
        val grid = SparseGrid()
        grid.dictionary.putAll(dictionary)

        return grid
    }

    override fun iterator(): MutableIterator<Pair<Coordinate, Int>> = GridIterator(dictionary)
}

class GridIterator(dictionary: HashMap<Coordinate, Int>): MutableIterator<Pair<Coordinate, Int>> {
    val iterator = dictionary.iterator()

    override fun hasNext(): Boolean = iterator.hasNext()
    override fun next(): Pair<Coordinate, Int> {
        val entry = iterator.next()
        return Pair(entry.key, entry.value)
    }

    override fun remove() = iterator.remove()
}