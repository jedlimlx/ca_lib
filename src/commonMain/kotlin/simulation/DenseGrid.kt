package simulation

import Utils
import rules.Rule
import rules.hrot.HROT
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * A representation of a cellular automaton grid via an array.
 * This is suitable for cellular automaton patterns that are dense and small.
 * For larger patterns, using [SparseGrid] will be more efficient.
 * @constructor Constructs a grid based on the pattern provided
 * @param pattern The pattern to initialise the grid with
 * @param rule The rule of the grid
 * @param initialWidth The initial width of the array
 * @param initialHeight The initial height of the array
 * @param startingCoordinate The coordinate of the 1st cell in the 2D array
 * @param expansion_factor The array will expand by this much more than necessary to improve performance.
 * @property startingCoordinate The coordinate of the 1st cell in the 2D array
 * @property expansion_factor The array will expand by this much more than necessary to improve performance.
 */
class DenseGrid(pattern: String = "", rule: Rule = HROT("B3/S23"),
                initialWidth: Int = 16, initialHeight: Int = 16,
                var startingCoordinate: Coordinate = Coordinate(), val expansion_factor: Double = 1.1): Grid() {

    private var arr = Array(initialHeight) { IntArray(initialWidth) { 0 } }

    override var population = 0
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

    override fun set(coordinate: Coordinate, state: Int) {
        // If the cell to set is out of bounds, expand the array
        val maxCoordinate = startingCoordinate + Coordinate(arr[0].size - 1, arr.size - 1)
        if (startingCoordinate.x > coordinate.x || coordinate.x > maxCoordinate.x ||
            startingCoordinate.y > coordinate.y || coordinate.y > maxCoordinate.y) {
            // Expand preemptively to reduce number of times needed to expand
            var newStart = Coordinate(min(coordinate.x, startingCoordinate.x), min(coordinate.y, startingCoordinate.y))
            var newEnd = Coordinate(max(coordinate.x, maxCoordinate.x), max(coordinate.y, maxCoordinate.y))

            // Computing new width
            var (newWidth, newHeight) = newEnd - newStart + Coordinate(1, 1)
            if (newWidth > arr[0].size) newWidth = floor(newWidth * expansion_factor).toInt()
            if (newHeight > arr[1].size) newHeight = floor(newHeight * expansion_factor).toInt()

            // Modify the coordinates
            // I know the new width and height isn't actually newWidth and newHeight but it doesn't matter
            newStart -= Coordinate(newWidth - arr[0].size, newHeight - arr.size)
            newEnd += Coordinate(newWidth - arr[0].size, newHeight - arr.size)

            // Actually expand
            expand(newStart, newEnd)
            this[coordinate] = state
            return
        }

        if (state != this[coordinate]) cellsChanged[0].add(coordinate)

        // Setting the new state
        val newCoordinate = coordinate - startingCoordinate
        val prevState = arr[newCoordinate.y][newCoordinate.x]
        arr[newCoordinate.y][newCoordinate.x] = Utils.convert(state, background)

        if (state == background) {
            if (prevState != 0) population--

            // If a cell on the boundary is removed, the bounds are no longer accurate
            if (boundsUpdated && (coordinate.x == bounds.first.x || coordinate.y == bounds.first.y ||
                        coordinate.x == bounds.second.x || coordinate.y == bounds.second.y))
                boundsUpdated = false
        } else {
            if (prevState == 0) population++

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

    override fun get(coordinate: Coordinate, withoutBg: Boolean): Int {
        val maxCoordinate = startingCoordinate + Coordinate(arr[0].size - 1, arr.size - 1)
        return if (startingCoordinate.x <= coordinate.x && coordinate.x <= maxCoordinate.x &&
            startingCoordinate.y <= coordinate.y && coordinate.y <= maxCoordinate.y) {
            arr[coordinate.y - startingCoordinate.y][coordinate.x - startingCoordinate.x]
        } else if (withoutBg) 0 else background  // The cell has a background state if its outside the grid
    }

    /**
     * Expands the array of the dense grid to the new specified size.
     * @param newStart The new starting coordinate of the array
     * @param newEnd The new ending coordinate of the array
     */
    private fun expand(newStart: Coordinate, newEnd: Coordinate) {
        // Getting the new width and height
        val (newWidth, newHeight) = newEnd - newStart + Coordinate(1, 1)

        // Creating the new array and storing the old one
        val prevArr = arr
        arr = Array(newHeight) { IntArray(newWidth) { 0 } }

        // Assigning the previous values to the new array
        for (i in prevArr.indices) {
            for (j in prevArr[i].indices) {
                arr[i + startingCoordinate.y - newStart.y][j + startingCoordinate.x - newStart.x] = prevArr[i][j]
            }
        }

        // Update starting coordinate
        startingCoordinate = newStart
    }

    override fun deepCopy(): DenseGrid {
        val grid = DenseGrid("", rule)
        grid.generation = generation
        grid.background = background
        grid.arr = arr.map { it.copyOf() }.toTypedArray()
        grid.startingCoordinate = startingCoordinate
        grid.cellsChanged = cellsChanged.map { it.toHashSet() }.toTypedArray()

        return grid
    }

    override fun iterator(): MutableIterator<Pair<Coordinate, Int>> = DenseGridIterator(this, arr, startingCoordinate)
}

internal class DenseGridIterator(val grid: DenseGrid, arr: Array<IntArray>,
                                 startingCoordinate: Coordinate): MutableIterator<Pair<Coordinate, Int>> {
    var list: ArrayList<Pair<Coordinate, Int>> = arrayListOf()
    var lastElementReturned = Coordinate()

    val iterator: MutableIterator<Pair<Coordinate, Int>>

    init {
        for (i in arr.indices) {
            for (j in arr[i].indices) {
                if (arr[i][j] != 0) list.add(Pair(Coordinate(j + startingCoordinate.x, i + startingCoordinate.y), arr[i][j]))
            }
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