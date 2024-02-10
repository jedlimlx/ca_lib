import kotlin.test.Test
import rules.canoniseExtendedGenerations
import rules.fromRulestring
import rules.nontotalistic.transitions.R1MooreINT
import rules.nontotalistic.transitions.R2VonNeumannINT
import rules.readExtendedGenerations
import simulation.DenseGrid
import simulation.SparseGrid
import kotlin.system.measureTimeMillis

class Test {
    @Test
    fun test() {
        println(R1MooreINT("2x").transitionStrings)

        println(R2VonNeumannINT("2x").regex)
        println(R2VonNeumannINT("2x-1ed8x9x").transitionStrings.size)
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