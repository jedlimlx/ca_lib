package patterns

import readResource
import rules.hrot.HROT
import simulation.SparseGrid
import kotlin.math.round
import kotlin.test.Test
import kotlin.test.assertEquals

class SpaceshipTest {
    @Test
    fun compute_heat_correctly() {
        var tokens: List<String>
        val testCases = readResource("patterns/spaceshipStats.csv").split("\n")

        for (i in 1 until testCases.size) {
            tokens = testCases[i].trim().split(",")

            val ship = Spaceship(tokens[2].toInt(), tokens[3].toInt(), tokens[4].toInt(), SparseGrid(tokens[1], HROT(tokens[0])))
            assertEquals(tokens[5], ship.heat.minOrNull()!!.toString())
            assertEquals(tokens[6], ship.heat.maxOrNull()!!.toString())
            assertEquals(tokens[7].toDouble(), round(ship.heat.average() * 10) / 10.0)
        }
    }

    @Test
    fun compute_speed_correctly() {
        var tokens: List<String>
        val testCases = readResource("patterns/spaceshipStats.csv").split("\n")

        for (i in 1 until testCases.size) {
            tokens = testCases[i].trim().split(",")

            val ship = Spaceship(tokens[2].toInt(), tokens[3].toInt(), tokens[4].toInt(), SparseGrid(tokens[1], HROT(tokens[0])))

            // assertEquals(tokens[8], ship.speed)
            // assertEquals(tokens[9], ship.direction)
            // TODO (Parse CSVs correctly)
        }
    }
}