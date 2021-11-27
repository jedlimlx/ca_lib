package rules.hrot

import readResource
import simulation.SparseGrid
import kotlin.test.Test
import kotlin.test.assertEquals

class HROTTest {
    @Test
    fun parse_transitions_correctly() {
        // Loading test cases
        val testCases = readResource("rules/HROT/hrotParsingTest.txt").split("\n").map { it.trim() }

        var rulestring = ""
        var birth = setOf<Int>()
        var survival = setOf<Int>()
        for (line in testCases) {
            when {
                // Loading rulestring
                line.startsWith("#R") -> rulestring = line.replace("#R ", "")

                // Loading birth & survival conditions
                line.startsWith("#B") -> birth = line.replace("#B ", "").split(",")
                    .map { it.toInt() }.toSet()
                line.startsWith("#S") -> survival = line.replace("#S ", "").split(",")
                    .map { it.toInt() }.toSet()

                // Running the test case
                !line.startsWith("#") -> {
                    val hrot = HROT(rulestring)
                    assertEquals(birth, hrot.birth)
                    assertEquals(survival, hrot.survival)
                }
            }
        }
    }

    @Test
    fun canonise_rulestring_correctly() {
        // Loading test cases
        val testCases = readResource("rules/HROT/hrotParsingTest.txt").split("\n").map { it.trim() }

        var rulestring = ""
        var canonRulestring = ""
        for (line in testCases) {
            when {
                // Loading rulestring
                line.startsWith("#R") -> rulestring = line.replace("#R ", "")

                // Loading canon rulestring
                line.startsWith("#C") -> canonRulestring = line.replace("#C ", "")

                // Running the test case
                !line.startsWith("#") -> {
                    val hrot = HROT(rulestring)
                    assertEquals(canonRulestring, hrot.rulestring)
                }
            }
        }
    }

    @Test
    fun simulate_rule_correctly() {
        // Loading test cases
        val testCases = readResource("rules/HROT/hrotSimulationTest.txt").split("\n").map { it.trim() }

        var rulestring = ""
        var generations = 0
        var initial = ""
        var final = ""
        for (line in testCases) {
            when {
                // Loading rulestring
                line.startsWith("#R") -> rulestring = line.replace("#R ", "")

                // Loading generations to run
                line.startsWith("#G") -> generations = line.replace("#G ", "").toInt()

                // Loading initial and final to patterns
                line.startsWith("#I") -> initial = line.replace("#I ", "")
                line.startsWith("#O") -> final = line.replace("#O ", "")

                // Running the test case
                !line.startsWith("#") -> {
                    val grid = SparseGrid(initial, HROT(rulestring))
                    grid.step(generations)
                    assertEquals(final, grid.toRLE())
                }
            }
        }
    }
}