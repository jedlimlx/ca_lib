import org.junit.Test
import rules.canoniseExtendedGenerations
import rules.fromRulestring
import rules.readExtendedGenerations
import simulation.DenseGrid
import simulation.SparseGrid
import simulation.TileGrid
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

        val sparsePattern = SparseGrid("bo\$obo\$bo8\$8bo\$6bobo\$5b2obo2\$4b3o!", rule=rule)
        val sparsePattern2 = SparseGrid("bo\$obo\$bo8\$8bo\$6bobo\$5b2obo2\$4b3o!", rule=rule)
        val densePattern = DenseGrid("bo\$obo\$bo8\$8bo\$6bobo\$5b2obo2\$4b3o!", rule=rule)
        val densePattern2 = DenseGrid("bo\$obo\$bo8\$8bo\$6bobo\$5b2obo2\$4b3o!", rule=rule)
        val tilePattern = TileGrid("bo\$obo\$bo8\$8bo\$6bobo\$5b2obo2\$4b3o!", rule=rule)
        val tilePattern2 = TileGrid("bo\$obo\$bo8\$8bo\$6bobo\$5b2obo2\$4b3o!", rule=rule)

        println(measureTimeMillis { sparsePattern.step(100) })
        println(measureTimeMillis { sparsePattern2.step(100) })
        println(measureTimeMillis { densePattern.step(100) })
        println(measureTimeMillis { densePattern2.step(100) })
        println(measureTimeMillis { tilePattern.step(100) })
        println(measureTimeMillis { tilePattern2.step(100) })

        println(sparsePattern)
        println(tilePattern2)
    }
}