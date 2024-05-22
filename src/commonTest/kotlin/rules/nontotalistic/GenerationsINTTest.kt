package rules.nontotalistic

import PLATFORM
import readResource
import rules.enumerateRules
import rules.nontotalistic.rules.INTGenerations
import rules.randomRules
import simulation.SparseGrid
import kotlin.test.Test
import kotlin.test.assertEquals

class GenerationsINTTest {
    @Test
    fun canonise_rulestring_correctly() {
        // Loading test cases
        val testCases = readResource("rules/nontotalistic/generationsIntParsingTest.txt").split("\n").map { it.trim() }

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
                        println(rulestring)
                        val rule = INTGenerations(rulestring)
                        assertEquals(canonRulestring, rule.rulestring)
                    } catch (ignored: IllegalArgumentException) {}
                }
            }
        }
    }

    @Test
    fun simulate_rule_correctly() {
        // Loading test cases
        val testCases = readResource("rules/nontotalistic/generationsIntSimulationTest.txt").split("\n").map { it.trim() }

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
                        val grid = SparseGrid(initial, INTGenerations(rulestring))
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
                        val grid = SparseGrid(rle, INTGenerations(rulestring))
                        val pattern = grid.identify()

                        assertEquals(type, pattern.toString())
                        assertEquals(minRule, pattern!!.ruleRange!!.minRule.rulestring)
                        assertEquals(maxRule, pattern.ruleRange!!.maxRule.rulestring)
                    } catch (ignored: IllegalArgumentException) {}
                }
            }
        }
    }

    @Test
    fun enumerate_all_rules() {
        if (PLATFORM != "JS")
            assertEquals(
                16,
                enumerateRules(INTGenerations("23/3/3"), INTGenerations("234ace/2n3/3")).count()
            )
    }

    @Test
    fun check_deterministic() {
        assertEquals(
            randomRules(INTGenerations("B2n3/S23-q/G3"), INTGenerations("B2aen34-q/S2367e8/G3"), 10).take(100).map { it.toString() }.toList(),
            randomRules(INTGenerations("B2n3/S23-q/G3"), INTGenerations("B2aen34-q/S2367e8/G3"), 10).take(100).map { it.toString() }.toList()
        )
    }
}