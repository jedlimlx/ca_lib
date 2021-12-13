package soup

import simulation.Coordinate
import simulation.Grid
import simulation.SparseGrid
import kotlin.random.Random

// TODO (Add D2x, D8, C2 and C4 symmetries)

/* Random Soups */

/**
 * Generates a random soup of symmetry C1 with the specified parameters
 * @param width The width of the soup to generate
 * @param height The height of the soup to generate
 * @param density The density of the soup to generate
 * @param seed The seed of random number generator used to generate the soup
 * @return Returns the soup that was generated
 */
fun generateC1(width: Int = 16, height: Int = 16,
               density: DoubleArray = doubleArrayOf(0.5, 0.5),
               seed: Int? = null) = generateSoup(width, height, density, seed) { listOf(it) }

/**
 * Generates a random soup of symmetry D2 with the specified parameters
 * @param width The width of the soup to generate
 * @param height The height of the soup to generate
 * @param density The density of the soup to generate
 * @param seed The seed of random number generator used to generate the soup
 * @param odd If the outputted soup even or odd symmetric?
 * @return Returns the soup that was generated
 */
fun generateD2(width: Int = 16, height: Int = 16,
               density: DoubleArray = doubleArrayOf(0.5, 0.5),
               seed: Int? = null, odd: Boolean = false) = generateSoup(width, height, density, seed) {
    listOf(it, if (odd) Coordinate(-it.x, it.y) else Coordinate(-it.x - 1, it.y))
}

/**
 * Generates a random soup of symmetry D4 with the specified parameters
 * @param width The width of the soup to generate
 * @param height The height of the soup to generate
 * @param density The density of the soup to generate
 * @param seed The seed of random number generator used to generate the soup
 * @param oddX If the outputted soup even or odd symmetric about the x-axis?
 * @param oddY If the outputted soup even or odd symmetric about the y-axis?
 * @return Returns the soup that was generated
 */
fun generateD4(width: Int = 16, height: Int = 16,
               density: DoubleArray = doubleArrayOf(0.5, 0.5),
               seed: Int? = null, oddX: Boolean = false, oddY: Boolean = false) = generateSoup(width, height, density, seed) {
    if (oddY) {
        listOf(
            it,
            Coordinate(-it.x, it.y),
            Coordinate(it.x, -it.y - if (oddX) 0 else 1),
            Coordinate(-it.x, -it.y - if (oddX) 0 else 1)
        )
    } else {
        listOf(
            it,
            Coordinate(-it.x - 1, it.y),
            Coordinate(it.x, -it.y - if (oddX) 0 else 1),
            Coordinate(-it.x - 1, -it.y - if (oddX) 0 else 1)
        )
    }
}

/**
 * Generates a random soup with the specified parameters
 * @param width The width of the soup to generate
 * @param height The height of the soup to generate
 * @param density The density of the soup to generate
 * @param seed The seed of random number generator used to generate the soup
 * @param cellsToPlace A function that outputs the places to put a given state when the coordinate of a cell in inputted (used for symmetric soups)
 * @return Returns the soup that was generated
 */
fun generateSoup(width: Int = 16, height: Int = 16,
                 density: DoubleArray = doubleArrayOf(0.5, 0.5),
                 seed: Int? = null, cellsToPlace: (it: Coordinate) -> List<Coordinate>): Grid {
    // Initialise the random number generator
    val rand = if (seed == null) Random else Random(seed)

    // Generate the soup
    val soup = SparseGrid("")
    val cummulative = density.mapIndexed { index, _ -> density.slice(0 .. index).sum() }
    for (x in 0 until width) {
        for (y in 0 until height) {
            val randomNumber = rand.nextDouble()

            // Checking which state to place
            for (state in cummulative.indices) {
                if (cummulative[state] >= randomNumber) {
                    cellsToPlace(Coordinate(x, y)).forEach { soup[it] = state }
                    break
                }
            }
        }
    }

    return soup
}

/* Soup Enumeration */

/**
 * Generates a random soup of symmetry C1 with the specified parameters
 * @param width The width of the soup to generate
 * @param height The height of the soup to generate
 * @param statesToInclude The states to include in the soups
 * @return Returns the soup that was generated
 */
fun enumerateC1(width: Int = 16, height: Int = 16,
                statesToInclude: IntArray = intArrayOf(0, 1)) = enumerateSoup(width, height, statesToInclude) { listOf(it) }

/**
 * Generates a random soup of symmetry C1 with the specified parameters
 * @param width The width of the soup to generate
 * @param height The height of the soup to generate
 * @param statesToInclude The states to include in the soups
 * @param odd If the outputted soup even or odd symmetric?
 * @return Returns the soup that was generated
 */
fun enumerateD2(width: Int = 16, height: Int = 16,
                statesToInclude: IntArray = intArrayOf(0, 1), odd: Boolean = false) = enumerateSoup(width, height, statesToInclude) {
    listOf(it, if (odd) Coordinate(-it.x, it.y) else Coordinate(-it.x - 1, it.y))
}

/**
 * Generates a random soup of symmetry D4 with the specified parameters
 * @param width The width of the soup to generate
 * @param height The height of the soup to generate
 * @param statesToInclude The states to include in the soups
 * @param oddX If the outputted soup even or odd symmetric about the x-axis?
 * @param oddY If the outputted soup even or odd symmetric about the y-axis?
 * @return Returns the soup that was generated
 */
fun enumerateD4(width: Int = 16, height: Int = 16,
                statesToInclude: IntArray = intArrayOf(0, 1),
                oddX: Boolean = false, oddY: Boolean = false) = enumerateSoup(width, height, statesToInclude) {
    if (oddY) {
        listOf(
            it,
            Coordinate(-it.x, it.y),
            Coordinate(it.x, -it.y - if (oddX) 0 else 1),
            Coordinate(-it.x, -it.y - if (oddX) 0 else 1)
        )
    } else {
        listOf(
            it,
            Coordinate(-it.x - 1, it.y),
            Coordinate(it.x, -it.y - if (oddX) 0 else 1),
            Coordinate(-it.x - 1, -it.y - if (oddX) 0 else 1)
        )
    }
}

/**
 * Enumerates soups with the specified parameters
 * @param width The width of the soups to enumerate
 * @param height The height of the soups to enumerate
 * @param statesToInclude The states to include in the soups
 * @param cellsToPlace
 * @return Returns the soup that was generated
 */
fun enumerateSoup(width: Int, height: Int, statesToInclude: IntArray = intArrayOf(0, 1),
                  cellsToPlace: (it: Coordinate) -> List<Coordinate>): Sequence<Grid> {
    val stack = arrayListOf(Pair(SparseGrid(), 0))
    return sequence {
        while (stack.isNotEmpty()) {
            val (soup, index) = stack.removeLast()

            if (index >= width * height) yield(soup)
            else {
                for (state in statesToInclude) {
                    cellsToPlace(Coordinate(index % width, index / height)).forEach { soup[it] = state }
                    stack.add(Pair(soup.deepCopy(), index + 1))
                }
            }
        }
    }
}
