package simulation

import Colour
import Utils
import patterns.Oscillator
import patterns.Pattern
import patterns.Spaceship
import rules.PLACEHOLDER_RULE
import rules.Rule
import kotlin.math.max
import kotlin.math.min

/**
 * Represents the grid on which the cellular automaton runs on
 */
abstract class Grid : MutableIterable<Pair<Coordinate, Int>> {
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
    internal lateinit var cellsChanged: Array<MutableSet<Coordinate>>

    /**
     * The background of the grid (usually state 0 but for B0 rules it can be state 1 or higher)
     */
    var background = 0
        internal set

    /**
     * The bounds of the pattern within the grid. Note that bounds **do not** update automatically.
     * To update them use [updateBounds].
     */
    var bounds = Coordinate(Int.MAX_VALUE, Int.MAX_VALUE)..Coordinate(-Int.MAX_VALUE, -Int.MAX_VALUE)
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
        rule.step(this, generations)

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

                val diff = bounds.start - hashMap[hash]!!.bounds.start
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
        return shift(bounds, dx, dy)
    }

    /**
     * Shifts the cells within the specified bounds by (dx, dy)
     * @param range The bounds containing the cells to be shifted
     * @param dx The amount to shift the cells in the x-direction
     * @param dy The amount to shift the cells in the y-direction
     * @return Returns the grid object (the object is the same as the one on which the command is called)
     */
    open fun shift(range: CoordinateRange, dx: Int, dy: Int): Grid {
        // Make a deep-copy to take reference from
        val grid = deepCopy()

        // Clear the portion that will be shifted
        clear(range)

        // Shift stuff
        val shiftCoordinate = Coordinate(dx, dy)
        grid.iterateOverRectangle(range) {
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
        return flip(bounds, flip)
    }

    /**
     * Flips the cells within the specified bounds horizontally or vertically
     * @param flip The direction to flip the cells
     * @return Returns the grid object (the object is the same as the one on which the command is called)
     */
    open fun flip(range: CoordinateRange, flip: Flip): Grid {
        // Make a deep-copy to take reference from
        val gridCopy = deepCopy()
        val (startCoordinate, endCoordinate) = range

        if (flip == Flip.HORIZONTAL) {
            for ((x, y) in range)
                this[endCoordinate.x - x + startCoordinate.x, y] = gridCopy[x, y]
        } else {
            for ((x, y) in range)
                this[x, endCoordinate.y - y + startCoordinate.y] = gridCopy[x, y]
        }

        return this
    }

    /**
     * Rotates the grid clockwise or anti-clockwise
     * @param rotation The direction to rotate the grid
     */
    open fun rotate(rotation: Rotation) {
        updateBounds()
        rotate(bounds, rotation)
    }

    /**
     * Rotates the cells within the specified bounds clockwise or anti-clockwise
     * @param range The bounds containing the cells to be rotated
     * @param rotation The direction to rotate the cells
     */
    open fun rotate(range: CoordinateRange, rotation: Rotation) {
        val grid = deepCopy() // Make a deep copy for reference

        // Allow editing of the variable values
        var (startCoordinate, endCoordinate) = range

        if ((endCoordinate.x - startCoordinate.x) % 2 == 1) {
            endCoordinate = Coordinate(endCoordinate.x + 1, endCoordinate.y)
        }

        if ((endCoordinate.y - startCoordinate.y) % 2 == 1) {
            endCoordinate = Coordinate(endCoordinate.x, endCoordinate.y + 1)
        }

        val centerX = (endCoordinate.x - startCoordinate.x) / 2 + startCoordinate.x
        val centerY = (endCoordinate.y - startCoordinate.y) / 2 + startCoordinate.y

        clear(range)

        // Perform the rotation
        // TODO (Make the pattern rotate about the centre?)
        for ((x, y) in range) {
            val dx = x - centerX
            val dy = y - centerY

            if (rotation == Rotation.CLOCKWISE) this[centerX - dy, centerY + dx] = grid[x, y]
            else this[centerX + dy, centerY - dx] = grid[x, y]
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
     * @param range The bounds containing the cells to be cleared
     */
    open fun clear(range: CoordinateRange) {
        for (coordinate in range) {
            this[coordinate] = background
        }
    }

    /**
     * Adds the 2 grids together.
     * If both grids have live cells in the same place, the cells of the first grid take precedence.
     * @param other The other grid to add to the current grid
     * @return Returns the new grid
     */
    operator fun plus(other: Grid): Grid {
        val newGrid = this.deepCopy()
        other.forEach { (coordinate, state) -> newGrid[coordinate] = state }

        return newGrid
    }

    /**
     * Adds the 2 grids together.
     * If both grids have live cells in the same place, the cells of the first grid take precedence.
     * @param other The other grid to add to the current grid
     * @return Returns the new grid
     */
    infix fun union(other: Grid): Grid = this + other

    /**
     * Performs the bitwise operation AND on the grids cell by cell.
     * @param other The other grid to perform the operation with
     * @return Returns the new grid
     */
    infix fun and(other: Grid): Grid {
        val newGrid = this.deepCopy()
        newGrid.clear()

        forEach { (coordinate, state) -> if (other[coordinate] == state) newGrid[coordinate] = state }
        return newGrid
    }

    /**
     * Performs the bitwise operation AND on the grids cell by cell.
     * @param other The other grid to perform the operation with
     * @return Returns the new grid
     */
    infix fun intersect(other: Grid): Grid = this and other

    /**
     * Inverts the selected portion of the grid
     * @param range The rectangular region in which the grid should be inverted
     */
    fun invert(range: CoordinateRange) {
        for (coordinate in range) {
            this[coordinate] = when (this[coordinate]) {
                0 -> 1
                else -> 0
            }
        }
    }

    /* Pattern Matching */

    // TODO (More efficient pattern matching)

    /**
     * Finds the coordinate of the top-left corner of a portion of the grid that matches the given pattern.
     * If unsuccessful, returns null.
     * @param grid The pattern to search for
     * @return Returns the coordinate of the portion of the grid that matches the given pattern if successful, else null
     */
    fun find(grid: Grid): Coordinate? {
        updateBounds()
        grid.updateBounds()

        for (coordinate in bounds) {
            var broken = false
            for (coordinate2 in grid.bounds) {
                if (grid[coordinate2] != this[coordinate + coordinate2 - grid.bounds.start]) {
                    broken = true
                    break
                }
            }

            if (!broken) return coordinate
        }

        return null
    }

    /**
     * Finds all possible portions of the grid that match the given pattern
     * @param grid The pattern to search for
     * @return Returns a list of all possible coordinates of portions of the grid that match the given pattern
     */
    fun findAll(grid: Grid): List<Coordinate> {
        updateBounds()
        grid.updateBounds()

        val list = arrayListOf<Coordinate>()
        for (coordinate in bounds) {
            var broken = false
            for (coordinate2 in grid.bounds) {
                if (grid[coordinate2] != this[coordinate + coordinate2 - grid.bounds.start]) {
                    broken = true
                    break
                }
            }

            if (!broken) list.add(coordinate)
        }

        return list
    }

    /**
     * Finds all instances of the specified pattern in the grid and replaces it with a new pattern
     * @param old The pattern to be replaced
     * @param replacement The pattern to replace the old pattern with
     */
    fun replace(old: Grid, replacement: Grid) = findAll(old).forEach { this[it] = replacement }

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
    open fun toRLE(maxLineLength: Int = 70): String {
        updateBounds()
        return toRLE(bounds, maxLineLength = maxLineLength)
    }

    /**
     * Converts the grid between the start and end coordinate to an RLE
     * @param range The range of coordinates to convert to RLE
     * @return Returns the RLE
     */
    open fun toRLE(range: CoordinateRange, maxLineLength: Int = 70): String {
        val (startCoordinate, endCoordinate) = range

        // First, add characters to a string
        val buffer = arrayListOf<Char>()
        val rleArray = arrayListOf<Char>()
        for (y in startCoordinate.y..endCoordinate.y) {
            for (x in startCoordinate.x..endCoordinate.x) {
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

        // Adding new lines
        return StringBuilder().apply {
            var delay = 0
            val string = rleString
            for (i in string.indices) {
                append(string[i])
                if ((i - delay).mod(maxLineLength) == 0 && i != 0) {
                    if (string[i].isDigit()) delay++
                    else append("\n")
                }
            }
        }.toString() + "!"
    }

    /**
     * Converts the grid between the start and end coordinate to an apgcode
     * @param range The range of coordinates to convert to RLE
     * @return Returns the apgcode
     */
    open fun toApgcode(range: CoordinateRange = bounds): String {
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
     * Adds the pattern at the specified starting coordinate
     * @param x The x coordinate of the pattern
     * @param y The y coordinate of the pattern
     * @param pattern The pattern to add (automatically detects if its an apgcode or an RLE)
     */
    open operator fun set(x: Int, y: Int, pattern: String) {
        this[Coordinate(x, y)] = pattern
    }

    /**
     * Adds the pattern at the specified starting coordinate
     * @param coordinate The coordinate where the top-left corner of the pattern is placed
     * @param pattern The pattern to add
     */
    open operator fun set(coordinate: Coordinate, pattern: Grid) {
        pattern.updateBounds()
        pattern.forEach { this[it.first + coordinate - pattern.bounds.start] = it.second }
    }

    /**
     * Adds the pattern at the specified starting coordinate
     * @param x The x coordinate of the pattern
     * @param y The y coordinate of the pattern
     * @param pattern The pattern to add
     */
    open operator fun set(x: Int, y: Int, pattern: Grid) {
        this[Coordinate(x, y)] = pattern
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
     * @return Returns the new grid
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
     * Generates a new grid with the cells within the specified coordinate range
     * @param range The range of coordinates
     * @return Returns the new grid
     */
    open operator fun get(range: CoordinateRange): Grid {
        val newGrid = deepCopy()
        newGrid.clear()

        for ((x, y) in range) newGrid[x, y] = this[x, y]
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

    /* Animation */

    /**
     * Creates a static svg displaying the cells in the grid
     * @param transparent Is the background of the svg transparent?
     * @param colours The colours of the cells in the svg
     * @param cellSize The size of the cells in the svg
     * @param emulated Should the svg should an emulation for B0 rules?
     */
    fun staticSvg(transparent: Boolean = true, colours: Array<Colour> = rule.colours, cellSize: Int = 5,
                  emulated: Boolean = true): String {
        updateBounds()
        return staticSvg(bounds, transparent, colours, cellSize, emulated)
    }

    /**
     * Creates a static svg displaying the cells in the specified range of coordinates
     * @param range The range of coordinates
     * @param transparent Is the background of the svg transparent?
     * @param colours The colours of the cells in the svg
     * @param cellSize The size of the cells in the svg
     * @param emulated Should the svg should an emulation for B0 rules?
     */
    fun staticSvg(range: CoordinateRange, transparent: Boolean = true,
                  colours: Array<Colour> = rule.colours, cellSize: Int = 5,
                  emulated: Boolean = true): String = with(StringBuilder()) {
        // SVG Header
        append("<svg height=\"${cellSize * (range.end.y - range.start.y + 1)}\" " +
                "width=\"${cellSize * (range.end.x - range.start.x + 1)}\" " +
                "xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink= \"http://www.w3.org/1999/xlink\">\n")

        // Loop through all the cells
        for (coordinate in range) {
            if (transparent && this@Grid[coordinate, emulated] == 0) continue  // Ignore 0 if background is transparent

            // Adding the cell as a rectangle
            val (red, green, blue) = colours[this@Grid[coordinate, emulated]]
            append("<rect x=\"${cellSize * (coordinate.x - range.start.x)}\" y=\"${cellSize * (coordinate.y - range.start.y)}\" " +
                    "width=\"$cellSize\" height=\"$cellSize\" style=\"fill:rgb($red,$green,$blue);stroke-width:0\"/>\n")
        }

        append("</svg>")

        this
    }.toString()

    /**
     * Creates an animated svg displaying the cells in the grid
     * @param generations The number of generations to generate the svg for
     * @param step The step size that should be taken in generating the svg
     * @param transparent Is the background of the svg transparent?
     * @param colours The colours of the cells in the svg
     * @param cellSize The size of the cells in the svg
     * @param duration The duration of the animation in milliseconds
     * @param emulated Should the svg should an emulation for B0 rules?
     */
    fun animatedSvg(generations: Int, step: Int = 1, transparent: Boolean = true,
                    colours: Array<Colour> = rule.colours, cellSize: Int = 5, duration: Int = 10000,
                    emulated: Boolean = true): String {
        val newGrid = this.deepCopy().step(generations)
        newGrid.updateBounds()
        return animatedSvg(newGrid.bounds, generations, step, transparent, colours, cellSize, duration, emulated)
    }

    /**
     * Creates an animated svg displaying the cells in the specified range of coordinates
     * @param range The range of coordinates
     * @param generations The number of generations to generate the svg for
     * @param step The step size that should be taken in generating the svg
     * @param transparent Is the background of the svg transparent?
     * @param colours The colours of the cells in the svg
     * @param cellSize The size of the cells in the svg
     * @param duration The duration of the animation in milliseconds
     * @param emulated Should the svg should an emulation for B0 rules?
     */
    fun animatedSvg(range: CoordinateRange, generations: Int, step: Int = 1, transparent: Boolean = true,
                    colours: Array<Colour> = rule.colours, cellSize: Int = 5, duration: Int = 10000,
                    emulated: Boolean = true): String {
        val phases = generateSequence(this.deepCopy()) { it.deepCopy().step(step) }.take(generations / step).toList()
        return with(StringBuilder()) {
            // SVG Header
            append("<svg height=\"${cellSize * (range.end.y - range.start.y + 1)}\" " +
                    "width=\"${cellSize * (range.end.x - range.start.x + 1)}\" " +
                    "xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink= \"http://www.w3.org/1999/xlink\">\n")

            // Loop through all the cells
            for (coordinate in range) {
                // Adding the cell as a rectangle
                append("<rect x=\"${cellSize * (coordinate.x - range.start.x)}\" " +
                        "y=\"${cellSize * (coordinate.y - range.start.y)}\" " +
                        "width=\"$cellSize\" height=\"$cellSize\" style=\"stroke-width:0\">\n")

                // Generate list of cell colours
                val animation = phases.joinToString(";") {
                    val (red, green, blue) = colours[it[coordinate, emulated]]

                    if (transparent && it[coordinate, emulated] == 0) "rgba($red,$green,$blue,0)"
                    else "rgb($red,$green,$blue)"
                }

                append("<animate attributeName=\"fill\" values=\"$animation\" dur=\"${duration}ms\" repeatCount=\"indefinite\" />\n")
                append("</rect>\n")
            }

            append("</svg>")

            this
        }.toString()
    }

    /* Misc Functions */

    /**
     * Iterate over all non-zero cells in a rectangle
     */
    private fun iterateOverRectangle(
        range: CoordinateRange,
        func: (it: Pair<Coordinate, Int>) -> Unit
    ) {
        // Check for the more efficient approach
        if (population < range.area) forEach { if (it.first in range) func(it) }
        else for ((x, y) in range) func(Pair(Coordinate(x, y), this[x, y]))
    }

    /**
     * Updates the bounds of the pattern on the grid
     */
    open fun updateBounds() {
        // Don't do anything if the bounds are still updated
        if (boundsUpdated) return

        // Reset bounds
        bounds = Coordinate(Int.MAX_VALUE, Int.MAX_VALUE)..Coordinate(-Int.MAX_VALUE, -Int.MAX_VALUE)

        // Use vars instead of the immutable Coordinate
        var (minX, minY) = bounds.start
        var (maxX, maxY) = bounds.end

        // Loop over all cells
        forEach {
            minX = min(it.first.x, minX)
            maxX = max(it.first.x, maxX)
            minY = min(it.first.y, minY)
            maxY = max(it.first.y, maxY)
        }

        // Set bounds to the new value
        bounds = Coordinate(minX, minY)..Coordinate(maxX, maxY)
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
        return hashCode(bounds)
    }

    /**
     * Gets the hash of the grid.
     * @param range The region to be hashed
     * @return Returns the region's hash (uses Golly's hash algorithm).
     */
    open fun hashCode(range: CoordinateRange): Int {
        val (startCoordinate, endCoordinate) = range

        var hash = 31415962
        for (y in startCoordinate.y..endCoordinate.y) {
            val yShift: Int = y - startCoordinate.y
            for (x in startCoordinate.x..endCoordinate.x) {
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