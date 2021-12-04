package patterns

import readResource
import rules.hrot.HROT
import simulation.SparseGrid
import kotlin.math.round
import kotlin.test.Test
import kotlin.test.assertEquals

class OscillatorTest {
    @Test
    fun compute_heat_correctly() {
        var tokens: List<String>
        val testCases = readResource("patterns/oscillatorStats.csv").split("\n")

        for (i in 1 until testCases.size) {
            tokens = testCases[i].trim().split(",")

            val osc = Oscillator(tokens[2].toInt(), SparseGrid(tokens[1], HROT(tokens[0])))
            assertEquals(tokens[3], osc.heat.minOrNull()!!.toString())
            assertEquals(tokens[4], osc.heat.maxOrNull()!!.toString())
            assertEquals(tokens[5].toDouble(), round(osc.heat.average() * 10) / 10.0)
        }
    }

    @Test
    fun compute_activity_correctly() {
        var tokens: List<String>
        val testCases = readResource("patterns/oscillatorStats.csv").split("\n")

        for (i in 1 until testCases.size) {
            tokens = testCases[i].trim().split(",")

            val osc = Oscillator(tokens[2].toInt(), SparseGrid(tokens[1], HROT(tokens[0])))

            assertEquals(tokens[6], osc.rotorCells.toString())
            assertEquals(tokens[7], osc.statorCells.toString())
            assertEquals(tokens[8], osc.activeCells.toString())
            assertEquals(tokens[9], osc.nonTrivial.toString())
        }
    }

    @Test
    fun compute_volatility_correctly() {
        var tokens: List<String>
        val testCases = readResource("patterns/oscillatorStats.csv").split("\n")

        for (i in 1 until testCases.size) {
            tokens = testCases[i].trim().split(",")

            val osc = Oscillator(tokens[2].toInt(), SparseGrid(tokens[1], HROT(tokens[0])))

            assertEquals(tokens[10].toDouble(), round(osc.volatility * 100) / 100.0)
            assertEquals(tokens[11].toDouble(), round(osc.strictVolatility * 100) / 100.0)
        }
    }
}