package simulation

import Utils
import patterns.Oscillator
import patterns.Pattern
import patterns.Spaceship
import rules.PLACEHOLDER_RULE
import rules.Rule
import rules.hrot.HROT
import kotlin.math.max
import kotlin.math.min

/**
 * Represents the grid on which the cellular automaton runs on
 */
abstract class Grid: MutableIterable<Pair<Coordinate, Int>> {
    /**
     * The rule of the cellular automaton grid
     */
    var rule: Rule = PLACEHOLDER_RULE
        set(value) {
            field = value

            // When there is a new rule, all existing cellsChanged are invalidated
            cellsChanged = Array(value.background.size) { index ->
                val set = hashSetOf<Coordinate>()
                if (index == 0) forEach { set.add(it.first) }

                set
            }
        }

    /**
     * The generation of the grid
     */
    var generation = 0

    /**
     * The current neighbourhood being used by the grid
     */
    val neighbourhood
        get() = rule.neighbourhood[generation % rule.alternatingPeriod]

    /**
     * The cells that changed in the previous generation and the previous previous generation...
     * Something like {{(0, 1)...}, ...} with the length of the loaded rule's alternating period.
     */
    protected lateinit var cellsChanged: Array<MutableSet<Coordinate>>

    /**
     * The background of the grid (usually state 0 but for B0 rules it can be state 1 or higher)
     */
    var background = 0
        protected set

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

    /**
     * The current population (i.e. the number of cells with a non-background state)
     */
    abstract val population: Int

    /**
     * The current population as an array of integers.
     * Each element of the array represents the number of cells of a certain state.
     * A background state is denoted as -1.
     */
    abstract val populationByState: IntArray

    /* Simulation */

    /**
     * Steps the grid forward by [generations] generations.
     * @param generations The number of generations to step the grid forward by
     * @return Returns the modified grid ()
     */
    open fun step(generations: Int = 1): Grid {
        for (i in 0 until generations) {
            val totalSize = cellsChanged.fold(0) { acc, set -> acc + set.size }

            val cellsToCheck = HashSet<Coordinate>(totalSize)
            val neighbourhood: Array<Coordinate> = rule.neighbourhood[generation % rule.alternatingPeriod]

            // Generate set of cells to run update function on
            // Use a set to avoid duplicate
            var neighbour: Coordinate
            for (cellSet in cellsChanged) {
                for (cell in cellSet) {
                    for (neighbour2 in neighbourhood) {
                        neighbour = cell - neighbour2
                        cellsToCheck.add(neighbour)
                    }

                    cellsToCheck.add(cell)
                }
            }

            // Apply the transition function across all the cells
            var neighbours: IntArray
            val gridCopy = this.deepCopy()

            // Update the background of the current grid
            this.background = rule.background[(generation + 1) % rule.background.size]

            // Run through all the cells that could change
            for (cell in cellsToCheck) {
                // Getting neighbours
                neighbours = neighbourhood.map { gridCopy[it + cell] }.toIntArray()

                // Update the value of the cell
                if (rule.possibleSuccessors[generation % rule.alternatingPeriod][gridCopy[cell]].size == 1) {
                    this[cell] = rule.possibleSuccessors[generation % rule.alternatingPeriod][gridCopy[cell]][0]
                } else this[cell] = rule.transitionFunc(neighbours, gridCopy[cell], generation, cell)

                // Check if the cell value changed
                if (Utils.convert(this[cell], background) == Utils.convert(gridCopy[cell], gridCopy.background)) {
                    for (i in 0 until rule.background.size) {
                        if (cell in cellsChanged[i]) {
                            cellsChanged[i].remove(cell)

                            // Move the cell forward into the next entry until it can't be moved forward anymore
                            if (i < rule.background.size - 1) cellsChanged[i + 1].add(cell)
                            break
                        }
                    }
                }
            }

            generation++
        }

        // Return itself to allow command chaining
        return this
    }

    /**
     * Identifies the pattern as a oscillator, spaceship, etc.
     * @param maxGenerations The number of generations to check before giving up and returning null
     * @return Returns the pattern if identification is successful and null otherwise
     */
    open fun identify(maxGenerations: Int = 5000): Pattern? {
        var hash = this.hashCode()

        var deepCopy = this.deepCopy()
        val gridList = arrayListOf(deepCopy)
        val hashMap = hashMapOf(hash to deepCopy)

        for (i in 0 until maxGenerations) {
            // Step 1 generation forward
            this.step()

            // Check the hash
            hash = this.hashCode()
            if (hash in hashMap) {  // Potential pattern detected
                // Check for hash collisions
                updateBounds()
                hashMap[hash]!!.updateBounds()

                val diff = bounds.first - hashMap[hash]!!.bounds.first
                if (!equals(hashMap[hash]!!, diff.x, diff.y)) continue

                // Output the pattern
                gridList.add(this.deepCopy())

                val period = generation - hashMap[hash]!!.generation
                val phases = gridList.subList(gridList.size - period, gridList.size).toTypedArray()

                return if (diff.x == 0 && diff.y == 0) Oscillator(period, phases)
                else Spaceship(diff.x, diff.y, period, phases)
            } else {  // Nothing detected
                deepCopy = this.deepCopy()

                gridList.add(deepCopy)
                hashMap[hash] = deepCopy
            }
        }

        // Didn't find anything
        return null
    }

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

        if (rule.numStates == 2) rleString = rleString.replace(".", "b").replace("A", "o")
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
    open operator fun set(x: Int, y: Int, state: Int) {
        this[Coordinate(x, y)] = state
    }

    /**
     * Adds the pattern at the specified starting coordinate
     * @param coordinate The coordinate where the top-left corner of the pattern is placed
     * @param pattern The pattern to add (automatically detects if its an apgcode or an RLE)
     */
    open operator fun set(coordinate: Coordinate, pattern: String) {
        if (pattern.matches(Regex("(x[pqs]_)?[0-9a-z_]*"))) addApgcode(coordinate, pattern)
        else addRLE(coordinate, pattern)
    }

    /**
     * Gets the state of the cell at the specified coordinate
     * @param coordinate Coordinate of the cell to get the state for
     * @return Returns the state of the cell
     */
    abstract operator fun get(coordinate: Coordinate, withoutBg: Boolean = false): Int

    /**
     * Gets the state of the cell at (x, y)
     * @param x x-coordinate of the cell to get the state for
     * @param y y-coordinate of the cell to get the state for
     * @return Returns the state of the cell
     */
    open operator fun get(x: Int, y: Int, withoutBg: Boolean = false): Int = this[Coordinate(x, y), withoutBg]

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
    open operator fun invoke(dx: Int, dy: Int) {
        shift(dx, dy)
    }

    /**
     * Rotates the grid clockwise or anti-clockwise
     * @param rotation The direction to rotate the grid
     */
    open operator fun invoke(rotation: Rotation) {
        rotate(rotation)
    }

    /**
     * Flips the grid horizontally or vertically
     * @param flip The direction to flip the grid
     */
    open operator fun invoke(flip: Flip) {
        flip(flip)
    }

    /* Misc Functions */

    /**
     * Iterate over all non-zero cells in a rectangle
     */
    private fun iterateOverRectangle(startCoordinate: Coordinate, endCoordinate: Coordinate,
                                     func: (it: Pair<Coordinate, Int>) -> Unit) {
        // Check for the more efficient approach
        if (population < (endCoordinate.x - startCoordinate.x) * (endCoordinate.y - startCoordinate.y)) {
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
     * Gets the neighbours of the non-background cells (including the cells themselves) in the neighbourhood
     * @return Returns a set of cells that are neighbours of the non-background cells
     */
    open fun neighbours(): Set<Coordinate> {
        val set = HashSet<Coordinate>(population)
        forEach {
            set.add(it.first)
            for (neighbour in neighbourhood) {
                set.add(it.first + neighbour)
            }
        }

        return set
    }

    /**
     * Creates a deep copy of the current grid
     * @return Returns the deep copy
     */
    abstract fun deepCopy(): Grid

    /**
     * Converts the grid to a string (in this case an RLE)
     */
    override fun toString(): String {
        return toRLE()
    }

    /**
     * Checks if 2 grids are equal in the patterns that they display and the parity of their
     * generations (based on the alternating period of the rule).
     * @param other The other grid to compare to
     * @param dx The translation in the x-direction of the other grid with respect to this one
     * @param dy The translation in the y-direction of the other grid with respect to this one
     * @return Returns true if the grids are equal and false otherwise
     */
    open fun equals(other: Grid, dx: Int, dy: Int): Boolean {
        val check = background == other.background && population == other.population &&
                generation % rule.background.size == other.generation % rule.background.size
        return if (check) {
            val translation = Coordinate(dx, dy)
            forEach { if (it.second != other[it.first - translation]) return false }

            return true
        } else false
    }

    /**
     * Checks if the hash of 2 grids are equal (there could be hash collisions).
     * @param other The other grid to check
     * @return Returns true if the grids are equal and false otherwise
     */
    override fun equals(other: Any?): Boolean {
        if (other !is Grid) return false
        return this.hashCode() == other.hashCode()
    }

    /**
     * Gets the hash of the grid.
     * @return Returns the grid's hash (uses Golly's hash algorithm).
     */
    override fun hashCode(): Int {
        updateBounds()
        return hashCode(bounds.first, bounds.second)
    }

    /**
     * Gets the hash of the grid.
     * @param startCoordinate The start coordinate of the region where the hash is calculated
     * @param endCoordinate The end coordinate of the region where the hash is calculated
     * @return Returns the grid's hash (uses Golly's hash algorithm).
     */
    open fun hashCode(startCoordinate: Coordinate, endCoordinate: Coordinate): Int {
        var hash = 31415962
        for (y in startCoordinate.y .. endCoordinate.y) {
            val yShift: Int = y - startCoordinate.y
            for (x in startCoordinate.x .. endCoordinate.x) {
                if (this[x, y] > 0) {
                    hash = hash * 1000003 xor yShift
                    hash = hash * 1000003 xor x - startCoordinate.x
                    hash = hash * 1000003 xor this[x, y]
                }
            }
        }

        return hash + 31 * generation % rule.background.size
    }
}