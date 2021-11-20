package simulation

/**
 * Represents a coordinate (x, y) in the grid
 * @constructor Constructs coordinate (x, y)
 */
class Coordinate(val x: Int = 0, val y: Int) {
    /**
     * Adds the coordinates together and returns a new coordinate
     * @param other The other coordinate to add to this coordinate
     * @return Returns the new coordinate
     */
    operator fun plus(other: Coordinate): Coordinate = Coordinate(x + other.x, y + other.y)

    /**
     * Subtracts other coordinate from the current one and returns a new coordinate
     * @param other The other coordinate to subtract from this coordinate
     * @return Returns the new coordinate
     */
    operator fun minus(other: Coordinate): Coordinate = Coordinate(x - other.x, y - other.y)

    /**
     * (x, y) -> (-x, -y)
     * @return Returns the new coordinate
     */
    operator fun unaryMinus(): Coordinate = Coordinate(-x, -y)

    /**
     * Compares two coordinates
     * @param other The other coordinate to compare with
     */
    operator fun compareTo(other: Coordinate): Int {
        if (x == other.x) return y - other.y
        return x - other.x
    }

    // To use in destructing declaration
    operator fun component1(): Int {
        return x
    }

    // To use in destructing declaration
    operator fun component2(): Int {
        return y
    }

    override fun equals(other: Any?): Boolean {
        if (other != null && other is Coordinate) return x == other.x && y == other.y
        return false
    }

    override fun hashCode(): Int = x + 10000 * y
    override fun toString(): String ="($x, $y)"
}