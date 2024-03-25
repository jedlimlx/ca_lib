package theorems

import moore
import kotlin.test.Test
import kotlin.test.assertEquals

class SpeedLimitTest {
    @Test
    fun hrotTest() {
        assertEquals(speedLimit(moore(1), 2), Pair(1, 1))
        assertEquals(speedLimit(moore(1), 3), Pair(2, 1))
    }
}