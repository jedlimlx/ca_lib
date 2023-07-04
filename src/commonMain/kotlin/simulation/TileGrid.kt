package simulation

import rules.PLACEHOLDER_RULE
import rules.Rule
import kotlin.math.max
import kotlin.math.min


/**
 * A representation of a cellular automaton grid via a sparse matrix.
 * However, instead of storing cells individually, they are stored as 2x2 tiles.
 * This is suitable for cellular automaton that have large empty regions and a small number of states.
 * @constructor Constructs a grid based on the pattern provided
 */
class TileGrid(pattern: String = "", rule: Rule = PLACEHOLDER_RULE) : Grid() {
    internal val dictionary: HashMap<Coordinate, ULong> = hashMapOf()

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

    /**
     * Obtains the value of the tile at [coordinate]
     * @return Returns the value of the tile at [coordinate]
     */
    fun getTile(coordinate: Coordinate) = dictionary[coordinate] ?: 0UL

    override fun clear() {
        dictionary.clear()
    }

    override operator fun set(coordinate: Coordinate, state: Int) {
        // Tile coordinates are aligned at (x, y) where x, y are even
        val c = Coordinate(coordinate.x - coordinate.x.mod(2), coordinate.y - coordinate.y.mod(2))
        val tile = dictionary[c] ?: 0UL

        // Calculate bitmask for finding cell
        val result = when (coordinate - c) {
            Coordinate(0, 0) -> if (state == 1) 0b1000UL or tile else 0b0111UL and tile
            Coordinate(1, 0) -> if (state == 1) 0b0100UL or tile else 0b1011UL and tile
            Coordinate(0, 1) -> if (state == 1) 0b0010UL or tile else 0b1101UL and tile
            Coordinate(1, 1) -> if (state == 1) 0b0001UL or tile else 0b1110UL and tile
            else -> 0b0000UL  // Shouldn't hit this case
        }

        // Add to cells that have changed
        if (tile != result) cellsChanged[0].add(coordinate)

        if (result == 0UL) {
            dictionary.remove(c)

            // If a cell on the boundary is removed, the bounds are no longer accurate
            if (boundsUpdated && (coordinate.x == bounds.start.x ||
                        coordinate.y == bounds.start.y ||
                        coordinate.x == bounds.start.x ||
                        coordinate.y == bounds.end.y)
            )
                boundsUpdated = false
        } else {
            dictionary[c] = result xor (0b1111UL * background.toUInt())

            // If the cell is added outside the current bounds
            if (boundsUpdated && coordinate !in bounds) {
                val minX = min(bounds.start.x, coordinate.x)
                val maxX = max(bounds.end.x, coordinate.x)
                val minY = min(bounds.start.y, coordinate.y)
                val maxY = max(bounds.end.y, coordinate.y)

                // Set bounds to the new value
                bounds = Coordinate(minX, minY)..Coordinate(maxX, maxY)
                boundsUpdated = true
            }
        }
    }

    override operator fun get(coordinate: Coordinate, withoutBg: Boolean): Int {
        // Tile coordinates are aligned at (x, y) where x, y are even
        val c = Coordinate(coordinate.x - coordinate.x.mod(2), coordinate.y - coordinate.y.mod(2))
        val tile = dictionary[c] ?: 0UL

        // Calculate bitmask for finding cell
        val bool = when (coordinate - c) {
            Coordinate(0, 0) -> (tile and 0b1000UL) == 8UL
            Coordinate(1, 0) -> (tile and 0b0100UL) == 4UL
            Coordinate(0, 1) -> (tile and 0b0010UL) == 2UL
            Coordinate(1, 1) -> (tile and 0b0001UL) == 1UL
            else -> false  // Shouldn't hit this case
        }

        val result = if (bool) 1 else 0
        return if (!withoutBg) result xor background
        else result
    }

    override fun deepCopy(): TileGrid {
        val grid = TileGrid("", rule)
        grid.generation = generation
        grid.background = background
        grid.dictionary.putAll(dictionary)
        grid.cellsChanged = cellsChanged.map { it.toHashSet() }.toTypedArray()

        return grid
    }

    override fun iterator(): MutableIterator<Pair<Coordinate, Int>> = TileGridIterator(this, dictionary)
}

internal class TileGridIterator(
    val grid: Grid,
    dictionary: Map<Coordinate, ULong>
) : MutableIterator<Pair<Coordinate, Int>> {
    var list: ArrayList<Pair<Coordinate, Int>> = arrayListOf()
    var lastElementReturned = Coordinate()

    val iterator: MutableIterator<Pair<Coordinate, Int>>

    init {
        dictionary.forEach { (coordinate, tile) ->
            if (0b1000UL and tile == 8UL) list.add(Pair(coordinate + Coordinate(0, 0), 1))
            if (0b0100UL and tile == 4UL) list.add(Pair(coordinate + Coordinate(1, 0), 1))
            if (0b0010UL and tile == 2UL) list.add(Pair(coordinate + Coordinate(0, 1), 1))
            if (0b0001UL and tile == 1UL) list.add(Pair(coordinate + Coordinate(1, 1), 1))
        }

        iterator = list.iterator()
    }

    override fun hasNext(): Boolean = iterator.hasNext()
    override fun next(): Pair<Coordinate, Int> {
        lastElementReturned = iterator.next().first
        return Pair(lastElementReturned, grid[lastElementReturned])
    }

    override fun remove() {
        grid[lastElementReturned] = grid.background
        iterator.remove()
    }
}