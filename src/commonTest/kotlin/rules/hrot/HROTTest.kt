package rules.hrot

import readResource
import rules.RuleFamily
import rules.enumerateRules
import simulation.SparseGrid
import kotlin.test.Test
import kotlin.test.assertEquals

class HROTTest {
    @Test
    fun parse_transitions_correctly() {
        // Loading test cases
        val testCases = readResource("rules/hrot/hrotParsingTest.txt").split("\n").map { it.trim() }

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
        val testCases = readResource("rules/hrot/hrotParsingTest.txt").split("\n").map { it.trim() }

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
        val testCases = readResource("rules/hrot/hrotSimulationTest.txt").split("\n").map { it.trim() }

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

    @Test
    fun calculate_rule_range_correctly() {
        // Loading test cases
        val testCases = readResource("rules/hrot/hrotRuleRangeTest.txt").split("\n").map { it.trim() }

        var rulestring = ""
        var rle = ""
        var type = ""
        var minRule = ""
        var maxRule = ""

        for (line in testCases) {
            when {
                // Loading rulestring
                line.startsWith("#R") -> rulestring = line.replace("#R ", "")

                // Loading RLE
                line.startsWith("#I") -> rle = line.replace("#I ", "")

                // Loading pattern type
                line.startsWith("#T") -> type = line.replace("#T ", "")

                // Loading rule range
                line.startsWith("#MIN") -> minRule = line.replace("#MIN ", "")
                line.startsWith("#MAX") -> maxRule = line.replace("#MAX ", "")

                // Running the test case
                !line.startsWith("#") -> {
                    val grid = SparseGrid(rle, HROT(rulestring))
                    val pattern = grid.identify()

                    assertEquals(type, pattern.toString())
                    assertEquals(minRule, (pattern!!.ruleRange!!.first as RuleFamily).rulestring)
                    assertEquals(maxRule, (pattern.ruleRange!!.second as RuleFamily).rulestring)
                }
            }
        }
    }

    @Test
    fun enumerate_all_rules() {
        assertEquals(128, enumerateRules(HROT("B3/S23"), HROT("B35678/S23678")).count())
    }
}