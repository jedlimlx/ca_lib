import rules.hrot.HROT
import rules.hrot.HROTGenerations
import simulation.SparseGrid
import soup.generateC1
import java.io.File
import kotlin.test.Test

class Test {
    @Test
    fun test() {
        val grid = SparseGrid(generateC1(32, 32).toRLE(), rule = HROTGenerations("/04/4V"))

        val file = File("temp.svg")
        file.writeText(grid.animatedSvg(1200, transparent = false, step = 4, duration = 30000))
    }
}