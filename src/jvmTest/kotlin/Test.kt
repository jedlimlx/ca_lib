import kotlin.test.Test
import rules.canoniseExtendedGenerations
import rules.fromRulestring
import rules.readExtendedGenerations
import simulation.DenseGrid
import simulation.SparseGrid
import kotlin.system.measureTimeMillis

class Test {
    @Test
    fun test() {
        println(readExtendedGenerations("0-2-3-5"))
        println(canoniseExtendedGenerations(3, mutableSetOf(2)))
    }

    @Test
    fun benchmark() {
        val rule = fromRulestring("B3/S23")
        val pattern = "bo\$obo\$bo8\$8bo\$6bobo\$5b2obo2\$4b3o!"

        val sparsePattern = SparseGrid(pattern, rule=rule)
        val sparsePattern2 = SparseGrid(pattern, rule=rule)
        val sparsePattern3 = SparseGrid(pattern, rule=rule)

        val densePattern = DenseGrid(pattern, rule=rule)
        val densePattern2 = DenseGrid(pattern, rule=rule)
        val densePattern3 = DenseGrid(pattern, rule=rule)

        println("Testing sparse grids...")
        println(measureTimeMillis { sparsePattern.step(100) })
        println(measureTimeMillis { sparsePattern2.step(100) })
        println(measureTimeMillis { sparsePattern3.step(100) })
        println()

        println("Testing dense grids...")
        println(measureTimeMillis { densePattern.step(100) })
        println(measureTimeMillis { densePattern2.step(100) })
        println(measureTimeMillis { densePattern3.step(100) })
        println()

        println(sparsePattern)
        println(densePattern)
    }
}