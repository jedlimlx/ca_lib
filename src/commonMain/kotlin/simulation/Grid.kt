package simulation

import kotlin.math.max
import kotlin.math.min

/**
 * Represents the grid on which the cellular automaton runs on
 * @constructor Constructs a grid with the given pattern
 */
abstract class Grid: MutableIterable<Pair<Coordinate, Int>> {
    /**
     * The background of the grid (usually state 0 but for B0 rules it can be state 1 or higher)
     */
    var background = 0

    /**
     * The bounds of the pattern within the grid. Note that bounds **do not** update automatically.
     * To update them use [updateBounds].
     */
    var bounds = Pair(Coordinate(Int.MAX_VALUE, Int.MAX_VALUE), Coordinate(-Int.MAX_VALUE, -Int.MAX_VALUE))
        protected set

    /**
     * True if bounds are updated, false otherwise
     */
    protected var boundsUpdated = false

    /* Pattern Manipulation */
    // TODO (Implement and, or and other bitwise operations for the grid)

    /**
     * Shifts the grid by (dx, dy)
     * @param dx The amount to shift the grid in the x-direction
     * @param dy The amount to shift the grid in the y-direction
     * @return Returns the grid object (the object is the same as the one on which the command is called)
     */
    open fun shift(dx: Int, dy: Int): Grid {
        updateBounds()
        return shift(bounds.first, bounds.second, dx, dy)
    }

    /**
     * Shifts the cells within the specified bounds by (dx, dy)
     * @param startCoordinate The starting coordinates of the bounds
     * @param endCoordinate The end coordinates of the bounds
     * @param dx The amount to shift the cells in the x-direction
     * @param dy The amount to shift the cells in the y-direction
     * @return Returns the grid object (the object is the same as the one on which the command is called)
     */
    open fun shift(startCoordinate: Coordinate, endCoordinate: Coordinate, dx: Int, dy: Int): Grid {
        // Make a deep-copy to take reference from
        val grid = deepCopy()

        // Clear the portion that will be shifted
        clear(startCoordinate, endCoordinate)

        // Shift stuff
        val shiftCoordinate = Coordinate(dx, dy)
        grid.iterateOverRectangle(startCoordinate, endCoordinate) {
            this[it.first + shiftCoordinate] = it.second
        }

        // Return to allow chaining of commands
        return this
    }

    /**
     * Flips the grid horizontally or vertically
     * @param flip The direction to flip the grid
     * @return Returns the grid object (the object is the same as the one on which the command is called)
     */
    open fun flip(flip: Flip): Grid {
        updateBounds()
        return flip(bounds.first, bounds.second, flip)
    }

    /**
     * Flips the cells within the specified bounds horizontally or vertically
     * @param flip The direction to flip the cells
     * @return Returns the grid object (the object is the same as the one on which the command is called)
     */
    open fun flip(startCoordinate: Coordinate, endCoordinate: Coordinate, flip: Flip): Grid {
        // Make a deep-copy to take reference from
        val gridCopy = deepCopy()

        if (flip == Flip.HORIZONTAL) {
            for (x in startCoordinate.x .. endCoordinate.x) {
                for (y in startCoordinate.y .. endCoordinate.y) {
                    this[endCoordinate.x - x + startCoordinate.x, y] = gridCopy[x, y]
                }
            }
        } else {
            for (x in startCoordinate.x .. endCoordinate.x) {
                for (y in startCoordinate.y .. endCoordinate.y) {
                    this[x, endCoordinate.y - y + startCoordinate.y] = gridCopy[x, y]
                }
            }
        }

        return this
    }

    /**
     * Rotates the grid clockwise or anti-clockwise
     * @param rotation The direction to rotate the grid
     */
    open fun rotate(rotation: Rotation) {
        updateBounds()
        rotate(bounds.first, bounds.second, rotation)
    }

    /**
     * Rotates the cells within the specified bounds clockwise or anti-clockwise
     * @param startCoordinate The starting coordinates of the bounds
     * @param endCoordinate The end coordinates of the bounds
     * @param rotation The direction to rotate the cells
     */
    open fun rotate(startCoordinate: Coordinate, endCoordinate: Coordinate, rotation: Rotation) {
        val grid = deepCopy() // Make a deep copy for reference

        // Allow editing of the variable values
        var endCoordinate = endCoordinate

        if ((endCoordinate.x - startCoordinate.x) % 2 == 1) {
            endCoordinate = Coordinate(endCoordinate.x + 1, endCoordinate.y)
        }

        if ((endCoordinate.y - startCoordinate.y) % 2 == 1) {
            endCoordinate = Coordinate(endCoordinate.x, endCoordinate.y + 1)
        }

        val centerX = (endCoordinate.x - startCoordinate.x) / 2 + startCoordinate.x
        val centerY = (endCoordinate.y - startCoordinate.y) / 2 + startCoordinate.y

        clear(startCoordinate, endCoordinate)

        // Perform the rotation
        // TODO (Make the pattern rotate about the centre?)
        for (x in startCoordinate.x .. endCoordinate.x) {
            for (y in startCoordinate.y .. endCoordinate.y) {
                val dx = x - centerX
                val dy = y - centerY

                if (rotation == Rotation.CLOCKWISE) this[centerX - dy, centerY + dx] = grid[x, y]
                else this[centerX + dy, centerY - dx] = grid[x, y]
            }
        }
    }

    /**
     * Clears the entire grid
     */
    open fun clear() {
        this.forEach { this[it.first] = background }
    }

    /**
     * Clears all cells within the specified bounds
     * @param startCoordinate The starting coordinates of the bounds
     * @param endCoordinate The end coordinates of the bounds
     */
    open fun clear(startCoordinate: Coordinate, endCoordinate: Coordinate) {
        for (x in startCoordinate.x .. endCoordinate.x) {
            for (y in startCoordinate.y .. endCoordinate.y) {
                this[x, y] = background
            }
        }
    }

    /* RLE and Apgcode Loading */

    /**
     * Adds the specified rle at the specified starting coordinate
     * @param coordinate The coordinate where the top-left corner of the RLE is placed
     * @param rle The RLE to add
     */
    open fun addRLE(coordinate: Coordinate, rle: String) {
        var x = coordinate.x
        var y = coordinate.y

        var rleGroup: String
        var lastChar: Char
        var num: Int
        Regex("([0-9]+)?[ob$.A-Z]").findAll(rle).forEach {
            rleGroup = it.groupValues[0]
            lastChar = rleGroup[rleGroup.length - 1]

            num = if (rleGroup.substring(0, rleGroup.length - 1).isNotEmpty()) // Check if a number is there
                rleGroup.substring(0, rleGroup.length - 1).toInt() else 1 // If not set it to one by default

            for (i in 0 until num) {
                when (lastChar) {
                    'b', '.' -> x++
                    '$' -> {
                        y++
                        x = coordinate.x
                    }
                    'o' -> this[x++, y] = 1
                    else -> this[x++, y] = lastChar.code - 64
                }
            }
        }
    }

    /**
     * Adds the specified apgcode at the specified starting coordinate
     * *Definitely not* directly translated from Catagolue's js code
     * @param coordinate The coordinate where the top-left corner of the apgcode is placed
     * @param apgcode The apgcode to add
     */
    open fun addApgcode(coordinate: Coordinate, apgcode: String) {
        val tokens: Array<String> = apgcode.split("_").toTypedArray()
        val apgcode = tokens.slice(1 until tokens.size).joinToString("_")

        val chars = "0123456789abcdefghijklmnopqrstuvwxyz"

        var blank = false
        var x = 0
        var y = 0
        var state = 1
        for (c in apgcode) {
            when {
                blank -> {
                    x += chars.indexOf(c)
                    blank = false
                }
                c == 'y' -> {
                    x += 4
                    blank = true
                }
                c == 'x' -> x += 3
                c == 'w' -> x += 2
                c == 'z' -> {
                    x = 0
                    y += 5
                }
                c == '_' -> {
                    x = 0
                    y = 0
                    state += 1
                }
                else -> {
                    for (j in 0..4) {
                        if (chars.indexOf(c) and (1 shl j) != 0) {
                            // TODO (Handle multi-state apgcodes properly)
                            this[x + coordinate.x, y + j + coordinate.y] = state
                        }
                    }

                    x += 1
                }
            }
        }
    }

    /**
     * Converts the entire grid to an RLE
     * @return Returns the RLE
     */
    open fun toRLE(): String {
        updateBounds()
        return toRLE(bounds.first, bounds.second)
    }

    /**
     * Converts the grid between the start and end coordinate to an RLE
     * @param startCoordinate The start coordinate
     * @param endCoordinate The end coordinate
     * @return Returns the RLE
     */
    open fun toRLE(startCoordinate: Coordinate, endCoordinate: Coordinate): String {
        // First, add characters to a string
        val buffer = arrayListOf<Char>()
        val rleArray = arrayListOf<Char>()
        for (y in startCoordinate.y .. endCoordinate.y) {
            for (x in startCoordinate.x .. endCoordinate.x) {
                if (this[x, y] == 0) {
                    buffer.add('.')
                } else {
                    rleArray.addAll(buffer)
                    rleArray.add((this[x, y] + 64).toChar())
                    buffer.clear()
                }
            }

            if (y != endCoordinate.y) {
                rleArray.add('$')
                buffer.clear()
            }
        }

        // Next, compress it
        val n: Int = rleArray.size
        var rleString = ""

        var i = 0
        while (i < n) {
            // Count occurrences of current character
            var count = 1
            while (i < n - 1 && rleArray[i] == rleArray[i + 1]) {
                count++
                i++
            }

            // Add to the RLE
            if (count > 1) {
                rleString += count
                rleString += rleArray[i]
            } else rleString += rleArray[i]

            i++
        }

        // Finish off the encoding
        // Don't forget the '!'
        rleString = rleString.replace(Regex("[0-9.$]*$"), "")
        return "$rleString!"
    }

    /**
     * Converts the grid between the start and end coordinate to an apgcode
     * @param startCoordinate The start coordinate
     * @param endCoordinate The end coordinate
     * @return Returns the apgcode
     */
    open fun toApgcode(startCoordinate: Coordinate = bounds.first, endCoordinate: Coordinate = bounds.second): String {
        TODO("Implement grid -> apgcode converter")
    }

    /* Operator Overloading */

    /**
     * Sets the cell at the specified coordinate to the specified state
     * @param coordinate Coordinate of the cell to modify
     * @param state State to set the cell to
     */
    abstract operator fun set(coordinate: Coordinate, state: Int)

    /**
     * Sets the cell at (x, y) to the specified state
     * @param x x-coordinate of the cell
     * @param y y-coordinate of the cell
     * @param state State to set the cell to
     */
    operator fun set(x: Int, y: Int, state: Int) {
        this[Coordinate(x, y)] = state
    }

    /**
     * Adds the pattern at the specified starting coordinate
     * @param coordinate The coordinate where the top-left corner of the pattern is placed
     * @param pattern The pattern to add (automatically detects if its an apgcode or an RLE)
     */
    operator fun set(coordinate: Coordinate, pattern: String) {
        if (pattern.matches(Regex("(x[pqs]_)?[0-9a-z_]*"))) addApgcode(coordinate, pattern)
        else addRLE(coordinate, pattern)
    }

    /**
     * Gets the state of the cell at the specified coordinate
     * @param coordinate Coordinate of the cell to get the state for
     * @return Returns the state of the cell
     */
    abstract operator fun get(coordinate: Coordinate): Int

    /**
     * Gets the state of the cell at (x, y)
     * @param x x-coordinate of the cell to get the state for
     * @param y y-coordinate of the cell to get the state for
     * @return Returns the state of the cell
     */
    operator fun get(x: Int, y: Int): Int = this[Coordinate(x, y)]

    /**
     * Generates a new grid with the cells with x coordinate within xIntRange and with y coordinate within yIntRange.
     * @param xIntRange The range of x coordinates
     * @param yIntRange The range of y coordinates
     */
    open operator fun get(xIntRange: IntRange, yIntRange: IntRange): Grid {
        val newGrid = deepCopy()
        newGrid.clear()

        for (x in xIntRange) {
            for (y in yIntRange) {
                newGrid[x, y] = this[x, y]
            }
        }

        return newGrid
    }

    /**
     * Shifts the grid by (dx, dy)
     * @param dx The amount to shift the grid in the x-direction
     * @param dy The amount to shift the grid in the y-direction
     */
    operator fun invoke(dx: Int, dy: Int) {
        shift(dx, dy)
    }

    /**
     * Rotates the grid clockwise or anti-clockwise
     * @param rotation The direction to rotate the grid
     */
    operator fun invoke(rotation: Rotation) {
        rotate(rotation)
    }

    /**
     * Flips the grid horizontally or vertically
     * @param flip The direction to flip the grid
     */
    operator fun invoke(flip: Flip) {
        flip(flip)
    }

    /* Misc Functions */

    /**
     * Iterate over all non-zero cells in a rectangle
     */
    private fun iterateOverRectangle(startCoordinate: Coordinate, endCoordinate: Coordinate,
                                     func: (it: Pair<Coordinate, Int>) -> Unit) {
        // Check for the more efficient approach
        if (population() < (endCoordinate.x - startCoordinate.x) * (endCoordinate.y - startCoordinate.y)) {
            forEach { func(it) }
        } else {
            for (x in startCoordinate.x .. endCoordinate.x) {
                for (y in startCoordinate.y .. endCoordinate.y) {
                    func(Pair(Coordinate(x, y), this[x, y]))
                }
            }
        }
    }

    /**
     * Updates the bounds of the pattern on the grid
     */
    open fun updateBounds() {
        // Don't do anything if the bounds are still updated
        if (boundsUpdated) return

        // Reset bounds
        bounds = Pair(Coordinate(Int.MAX_VALUE, Int.MAX_VALUE), Coordinate(-Int.MAX_VALUE, -Int.MAX_VALUE))

        // Use vars instead of the immutable Coordinate
        var (minX, minY) = bounds.first
        var (maxX, maxY) = bounds.second

        // Loop over all cells
        forEach {
            minX = min(it.first.x, minX)
            maxX = max(it.first.x, maxX)
            minY = min(it.first.y, minY)
            maxY = max(it.first.y, maxY)
        }

        // Set bounds to the new value
        bounds = Pair(Coordinate(minX, minY), Coordinate(maxX, maxY))
        boundsUpdated = true
    }

    /**
     * Gets the current population (i.e. the number of cells with a non-background state)
     */
    abstract fun population(): Int

    /**
     * Creates a deep copy of the current grid
     * @return Returns the deep copy
     */
    abstract fun deepCopy(): Grid

    override fun toString(): String {
        return toRLE()
    }
}