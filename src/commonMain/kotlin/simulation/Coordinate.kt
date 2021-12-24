package simulation

import rules.hrot.HROT

/**
 * Represents a coordinate (x, y) in the grid
 * @constructor Constructs coordinate (x, y)
 */
class Coordinate(val x: Int = 0, val y: Int = 0) : Comparable<Coordinate> {
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
    override operator fun compareTo(other: Coordinate): Int {
        if (x == other.x) return y - other.y
        return x - other.x
    }

    operator fun rangeTo(other: Coordinate) = CoordinateRange(this, other)

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

/**
 * Represents a range of coordinates
 * @param start The starting coordinate of the range
 * @param endInclusive The end coordinate of the range
 * @property start The starting coordinate of the range
 * @property endInclusive The end coordinate of the range
 */
class CoordinateRange(override val start: Coordinate,
                      override val endInclusive: Coordinate) : Iterable<Coordinate>, ClosedRange<Coordinate> {
    /**
     * An alias of [endInclusive]
     */
    val end get() = endInclusive

    /**
     * The area covered by the coordinate range
     */
    val area get() = (endInclusive.x - start.x + 1) * (endInclusive.y - start.y + 1)

    override fun iterator(): Iterator<Coordinate> = CoordinateIterator(start, endInclusive)

    override fun contains(value: Coordinate) = start.x <= value.x && value.x <= endInclusive.x
            && start.y <= value.y && value.y <= endInclusive.y

    operator fun component1(): Coordinate = start
    operator fun component2(): Coordinate = endInclusive
}

internal class CoordinateIterator(val start: Coordinate, val endInclusive: Coordinate) : Iterator<Coordinate> {
    var initValue = start + Coordinate(-1, 0)

    override fun hasNext(): Boolean {
        return initValue.x <= endInclusive.x && initValue.y <= endInclusive.y
    }

    override fun next(): Coordinate {
        initValue = if (initValue.x < endInclusive.x) initValue + Coordinate(1, 0)
        else Coordinate(start.x, initValue.y + 1)

        return initValue
    }
}