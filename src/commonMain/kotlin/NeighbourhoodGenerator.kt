import simulation.Coordinate
import kotlin.math.abs
import kotlin.math.max

const val NEIGHBOURHOOD_SYMBOLS = "ABCGHLMNX23*+#"

/**
 * Parses the CoordCA format
 * @param range The rang eof the neighbourhood to generate
 * @param string The CoordCA string
 * @return Returns the neighbourhood as an array of coordinates
 */
fun parseCoordCA(range: Int, string: String): Array<Coordinate> {
    require(string.matches(Regex("[A-Fa-f0-9]+"))) { "Invalid character in CoordCA neighbourhood specification." }

    // Convert to binary
    var flattenedNeighbourhood: String = string.toLong(16).toString(2)
    flattenedNeighbourhood = "0".repeat(
        max(0, (2 * range + 1) * (2 * range + 1) - 1 - flattenedNeighbourhood.length)
    ) + flattenedNeighbourhood // Replace it with the corrected one

    val neighbourhood = ArrayList<Coordinate>()
    for (i in -range..range) {
        for (j in -range..range) {
            if (i != 0 || j != 0) {
                val index = (i + range) * (2 * range + 1) + (j + range)
                if (i == 0 && j > 0 || i > 0) {
                    if (flattenedNeighbourhood[index - 1] == '1') {
                        neighbourhood.add(Coordinate(-j, -i))
                    }
                } else {
                    if (flattenedNeighbourhood[index] == '1') {
                        neighbourhood.add(Coordinate(-j, -i))
                    }
                }
            }
        }
    }

    return neighbourhood.toTypedArray()
}

/**
 * Parses the weights specified in the HROT neighbourhood
 * @param range The range of the neighbourhood to generate
 * @param string The weights strings that was specified in the HROT rulestring
 * @return Returns an array of coordinates representing the neighbourhood and an array of integers representing the weights
 */
fun parseWeights(range: Int, string: String): Pair<Array<Coordinate>, IntArray> {
    return Pair(generateWithPred(range, false) { i, j ->
        string[(j + range) * (2 * range + 1) + (i + range)] != '0'
    }, string.filter { it != '0' }.map { it.toString().toInt() }.toIntArray())
}

/**
 * Generates a neighbourhood from the provided symbol
 * @param range The range of the neighbourhood to generate
 * @param symbol The symbol representing the neighbourhood (in the HROT rulestring)
 * @return Returns the neighbourhood as an array of coordinates
 */
fun parseSymbol(range: Int, symbol: Char): Pair<Array<Coordinate>, IntArray?> {
    return when (symbol) {
        'A' -> Pair(asterisk(range), null)
        'B' -> Pair(checkerboard(range), null)
        'C' -> Pair(circular(range), null)
        'G' -> gaussian(range)
        'H' -> Pair(hexagonal(range), null)
        'N' -> Pair(vonNeumann(range), null)
        'X' -> Pair(saltire(range), null)
        '2' -> Pair(euclidean(range), null)
        '3' -> Pair(tripod(range), null)
        '*' -> Pair(star(range), null)
        '+' -> Pair(cross(range), null)
        '#' -> Pair(hash(range), null)
        else -> Pair(moore(range), null)
    }
}

/**
 * Converts the provided neighbourhood and weights to a string
 * @param neighbourhood The neighbourhood to convert to a string
 * @param weights The weights to convert to a string
 * @return Returns the string representing the neighbourhood and weights
 */
fun toWeights(neighbourhood: Array<Coordinate>, weights: IntArray): String {
    val range = neighbourhood.maxOf { max(abs(it.x), abs(it.y)) }

    val result = IntArray((2 * range + 1) * (2 * range + 1)) { 0 }
    neighbourhood.forEachIndexed { index, coordinate ->
        result[(coordinate.x + range) + (2 * range + 1) * (coordinate.y + range)] = weights[index]
    }

    return result.joinToString("")
}

/**
 * Converts the provided neighbourhood to a CoordCA string
 * @param neighbourhood The neighbourhood to convert to a CoordCA string
 * @return Returns the CoordCA string representing the neighbourhood
 */
fun toCoordCA(neighbourhood: Array<Coordinate>): String {
    val range = neighbourhood.maxOf { max(abs(it.x), abs(it.y)) }

    val result = IntArray((2 * range + 1) * (2 * range + 1) - 1) { 0 }
    neighbourhood.forEach { coordinate ->
        if (coordinate.y < 0 || (coordinate.x < 0 && coordinate.y == 0))
            result[(coordinate.x + range) + (2 * range + 1) * (coordinate.y + range)] = 1
        else
            result[(coordinate.x + range) + (2 * range + 1) * (coordinate.y + range) - 1] = 1
    }

    return result.joinToString("").toLong(2).toString(16)
}

/**
 * Converts the provided neighbourhood to a symbol
 * @param neighbourhood The neighbourhood to convert to a symbol
 * @return Returns the symbol representing the neighbourhood
 */
fun toSymbol(neighbourhood: Array<Coordinate>): Char? {
    val range = neighbourhood.maxOf { max(abs(it.x), abs(it.y)) }
    return when (neighbourhood.toHashSet()) {
        asterisk(range).toHashSet() -> 'A'
        checkerboard(range).toHashSet() -> 'B'
        circular(range).toHashSet() -> 'C'
        hexagonal(range).toHashSet() -> 'H'
        moore(range).toHashSet() -> 'M'
        vonNeumann(range).toHashSet() -> 'N'
        saltire(range).toHashSet() -> 'X'
        euclidean(range).toHashSet() -> '2'
        tripod(range).toHashSet() -> '3'
        star(range).toHashSet() -> '*'
        cross(range).toHashSet() -> '+'
        hash(range).toHashSet() -> '#'
        else -> null
    }
}

/**
 * Generates a moore neighbourhood of the specified range.
 * For more information on the moore neighbourhood, see [Moore](https://conwaylife.com/wiki/Moore_neighbourhood)
 * @param range The range of the neighbourhood to generate
 * @return Returns the neighbourhood as an array of coordinates
 */
fun moore(range: Int): Array<Coordinate> = generateWithPred(range) { _, _ -> true }

/**
 * Generates a von neumann neighbourhood of the specified range
 * For more information on the von neumann neighbourhood, see [Von Neumann](https://conwaylife.com/wiki/Von_Neumann_neighbourhood)
 * @param range The range of the neighbourhood to generate
 * @return Returns the neighbourhood as an array of coordinates
 */
fun vonNeumann(range: Int): Array<Coordinate> = generateWithPred(range) { i, j -> abs(i) + abs(j) <= range }

/**
 * Generates a cross neighbourhood of the specified range
 * @param range The range of the neighbourhood to generate
 * @return Returns the neighbourhood as an array of coordinates
 */
fun cross(range: Int): Array<Coordinate> = generateWithPred(range) { i, j -> i == 0 || j == 0 }

/**
 * Generates a saltire neighbourhood of the specified range
 * @param range The range of the neighbourhood to generate
 * @return Returns the neighbourhood as an array of coordinates
 */
fun saltire(range: Int): Array<Coordinate> = generateWithPred(range) { i, j -> abs(i) == abs(j) }

/**
 * Generates a star neighbourhood of the specified range
 * @param range The range of the neighbourhood to generate
 * @return Returns the neighbourhood as an array of coordinates
 */
fun star(range: Int): Array<Coordinate> = generateWithPred(range) { i, j -> abs(i) == abs(j) || i == 0 || j == 0 }

/**
 * Generates a circular neighbourhood of the specified range
 * @param range The range of the neighbourhood to generate
 * @return Returns the neighbourhood as an array of coordinates
 */
fun circular(range: Int): Array<Coordinate> =
    generateWithPred(range) { i, j -> (i * i + j * j) <= range * range + range }

/**
 * Generates a euclidean neighbourhood of the specified range
 * @param range The range of the neighbourhood to generate
 * @return Returns the neighbourhood as an array of coordinates
 */
fun euclidean(range: Int): Array<Coordinate> = generateWithPred(range) { i, j -> (i * i + j * j) <= range * range }

/**
 * Generates a hash neighbourhood of the specified range
 * @param range The range of the neighbourhood to generate
 * @return Returns the neighbourhood as an array of coordinates
 */
fun hash(range: Int): Array<Coordinate> = generateWithPred(range) { i, j -> abs(i) == 1 || abs(j) == 1 }

/**
 * Generates a checkerboard neighbourhood of the specified range
 * @param range The range of the neighbourhood to generate
 * @return Returns the neighbourhood as an array of coordinates
 */
fun checkerboard(range: Int): Array<Coordinate> = generateWithPred(range) { i, j -> abs(i) % 2 != abs(j) % 2 }

/**
 * Generates a hexagonal neighbourhood of the specified range.
 * For more information on the hexagonal neighbourhood, see [Hexagonal](https://conwaylife.com/wiki/Hexagonal_neighbourhood)
 * @param range The range of the neighbourhood to generate
 * @return Returns the neighbourhood as an array of coordinates
 */
fun hexagonal(range: Int): Array<Coordinate> = generateWithPred(range) { i, j ->
    i >= 0 && j >= 0 || i <= 0 && j <= 0 || i <= range + j && j < 0 || i >= -(range - j) && j > 0
}

/**
 * Generates a tripod neighbourhood of the specified range
 * @param range The range of the neighbourhood to generate
 * @return Returns the neighbourhood as an array of coordinates
 */
fun tripod(range: Int): Array<Coordinate> = generateWithPred(range) { i, j ->
    (j <= 0 && i <= 0 && (i == 0 || j == 0)) || (j > 0 && i == j)
}

/**
 * Generates a asterisk neighbourhood of the specified range
 * @param range The range of the neighbourhood to generate
 * @return Returns the neighbourhood as an array of coordinates
 */
fun asterisk(range: Int): Array<Coordinate> = generateWithPred(range) { i, j -> i == j || i == 0 || j == 0 }

/**
 * Generates a gaussian neighbourhood of the specified range
 * @param range The range of the neighbourhood to generate
 * @return Returns the neighbourhood as an array of coordinates
 */
fun gaussian(range: Int): Pair<Array<Coordinate>, IntArray> {
    val weights = IntArray((2 * range + 1) * (2 * range + 1))
    val neighbourhood = generateWithPred(range, false) { _, _ -> true }
    for (i in neighbourhood.indices) {
        weights[i] = (range + 1 - abs(neighbourhood[i].x)) *
                (range + 1 - abs(neighbourhood[i].y))
    }

    return Pair(neighbourhood, weights)
}

/**
 * Generates a neighbourhood of the specified range with cells that satisfy the given predicate
 * @param range The range of the neighbourhood to generate
 * @param predicate The predicate that needs to be satisfied
 * @param ignoreCentre Should the centre cellbe ignored in the final neighbourhood
 * @return Returns the neighbourhood as an array of coordinates
 */
private fun generateWithPred(
    range: Int,
    ignoreCentre: Boolean = true,
    predicate: (i: Int, j: Int) -> Boolean
): Array<Coordinate> {
    val lst = arrayListOf<Coordinate>()
    for (j in -range..range) {
        for (i in -range..range) {
            if (i == 0 && j == 0 && ignoreCentre) continue
            if (predicate(i, j)) lst.add(Coordinate(i, j))
        }
    }

    return lst.toTypedArray()
}

/**
 * Outputs the [neighbourhood] nicely represented as a string
 */
fun prettyPrintNeighbourhood(neighbourhood: Array<Coordinate>): String = StringBuilder().apply {
    val range = neighbourhood.maxOf { maxOf(abs(it.x), abs(it.y)) }
    for (i in -range .. range) {
        for (j in -range .. range) {
            if (i == 0 && j == 0) append("x ")
            else if (Coordinate(i, j) in neighbourhood) append("* ")
            else append(". ")
        }

        append("\n")
    }
}.toString()