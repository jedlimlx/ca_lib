package simulation

import kotlin.test.Test
import kotlin.test.assertEquals

class CoordinateTest {
    @Test
    fun testPlus() {
        assertEquals(Coordinate(1, 2) + Coordinate(3, 5), Coordinate(4, 7))
        assertEquals(Coordinate(2, -2) + Coordinate(3, 5), Coordinate(5, 3))
    }

    @Test
    fun testMinus() {
        assertEquals(Coordinate(1, 2) - Coordinate(3, 5), Coordinate(-2, -3))
        assertEquals(Coordinate(2, -2) - Coordinate(3, -5), Coordinate(-1, 3))
    }
}