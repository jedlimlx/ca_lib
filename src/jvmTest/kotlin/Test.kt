import rules.nontotalistic.rules.INT
import rules.nontotalistic.rules.INT_NEIGHBOURHOODS
import rules.nontotalistic.transitions.R1MooreINT
import simulation.SparseGrid
import kotlin.test.Test

class Test {
    @Test
    fun test() {
        val rule = INT("B3/S23")

        val grid = SparseGrid("oob\$boo\$bob!", rule)
        grid.step(10)

        println(grid)
    }
}