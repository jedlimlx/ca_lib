package rules.nontotalistic

import PLATFORM
import readResource
import rules.RuleFamily
import rules.enumerateRules
import rules.nontotalistic.rules.INT
import rules.randomRules
import simulation.SparseGrid
import kotlin.test.Test
import kotlin.test.assertEquals

class INTTest {
    @Test
    fun canonise_rulestring_correctly() {
        // Loading test cases
        val testCases = readResource("rules/nontotalistic/intParsingTest.txt").split("\n").map { it.trim() }

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
                    try {
                        val rule = INT(rulestring)
                        assertEquals(canonRulestring, rule.rulestring)
                    } catch (ignored: IllegalArgumentException) {}
                }
            }
        }
    }

    @Test
    fun simulate_rule_correctly() {
        // Loading test cases
        val testCases = readResource("rules/nontotalistic/intSimulationTest.txt").split("\n").map { it.trim() }

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
                    try {
                        val grid = SparseGrid(initial, INT(rulestring))
                        grid.step(generations)
                        assertEquals(final, grid.toRLE())
                    } catch (ignored: IllegalArgumentException) {}
                }
            }
        }
    }

    @Test
    fun calculate_rule_range_correctly() {
        // Loading test cases
        val testCases = readResource("rules/nontotalistic/intRuleRangeTest.txt").split("\n").map { it.trim() }

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
                    try {
                        val grid = SparseGrid(rle, INT(rulestring))
                        val pattern = grid.identify()

                        assertEquals(type, pattern.toString())
                        assertEquals(minRule, (pattern!!.ruleRange!!.first as RuleFamily).rulestring)
                        assertEquals(maxRule, (pattern.ruleRange!!.second as RuleFamily).rulestring)
                    } catch (ignored: IllegalArgumentException) {}
                }
            }
        }
    }

    @Test
    fun enumerate_all_rules() {
        if (PLATFORM != "JS")
            assertEquals(16384, enumerateRules(INT("B3/S23"), INT("B2n3/S234")).count())
    }

    @Test
    fun check_deterministic() {
        assertEquals(
            randomRules(INT("B2n3/S23-q"), INT("B2aen34-q/S2367e8"), 10).take(100).map { it.toString() }.toList(),
            randomRules(INT("B2n3/S23-q"), INT("B2aen34-q/S2367e8"), 10).take(100).map { it.toString() }.toList()
        )
    }
}