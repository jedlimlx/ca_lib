import org.junit.Test
import rules.fromRulestring
import rules.hrot.HROT
import search.GeneticShipSearch
import simulation.DenseGrid
import simulation.SparseGrid
import java.io.File
import kotlin.system.measureTimeMillis

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

    @Test
    fun benchmark() {
        val sparsePattern = SparseGrid("b2o\$2o\$bo!", rule=fromRulestring("B3/S23"))
        val densePattern = DenseGrid("b2o\$2o\$bo!", rule=fromRulestring("B3/S23"))

        println(measureTimeMillis { sparsePattern.step(100) })
        println(measureTimeMillis { densePattern.step(100) })

        val sparsePattern2 = SparseGrid("b2o\$2o\$bo!", rule=fromRulestring("B3/S23"))
        val densePattern2 = DenseGrid("b2o\$2o\$bo!", rule=fromRulestring("B3/S23"))

        println(measureTimeMillis { sparsePattern2.step(100) })
        println(measureTimeMillis { densePattern2.step(100) })
    }
}