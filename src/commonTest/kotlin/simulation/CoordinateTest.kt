package simulation

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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

    @Test
    fun test() {
        assertContains((Coordinate()..Coordinate(5, 5)) as Iterable<Coordinate>, Coordinate(1, 2))
        assertContains((Coordinate()..Coordinate(5, 5)) as Iterable<Coordinate>, Coordinate(0, 5))

        assertTrue { Coordinate(6, 1) !in Coordinate()..Coordinate(5, 5) }
        assertTrue { Coordinate(2, 10) !in Coordinate()..Coordinate(5, 5) }
        assertTrue { Coordinate(0, 10) !in Coordinate()..Coordinate(5, 5) }
    }
}