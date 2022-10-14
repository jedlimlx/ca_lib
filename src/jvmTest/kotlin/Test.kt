import org.junit.Test
import rules.hrot.HROT
import search.GeneticShipSearch
import simulation.SparseGrid
import java.io.File

class Test {
    fun test() {
        val partials = File("partials.txt").readText().split("\n").map { SparseGrid(it, rule=HROT("B3/S23")) }

        val shipSearch = GeneticShipSearch(
            HROT("B3/S23"),
            period = 4,
            dx = 0,
            dy = -1,
            width = 7,
            height = 7,
            startingPopulation = partials
        )
        /*
        shipSearch.rank(partials).map {
            println()
            println(it.second)
            println()
            shipSearch.fitnessReport(it.second)
        }
         */
        shipSearch.search()
    }
}