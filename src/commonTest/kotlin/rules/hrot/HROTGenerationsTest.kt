package rules.hrot

import readResource
import rules.RuleFamily
import rules.enumerateRules
import rules.randomRules
import simulation.SparseGrid
import kotlin.test.Test
import kotlin.test.assertEquals

class HROTGenerationsTest {
    @Test
    fun parse_transitions_correctly() {
        // Loading test cases
        val testCases = readResource("rules/hrot/hrotGenerationsParsingTest.txt")
            .split("\n").map { it.trim() }

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
                    val hrot = HROTGenerations(rulestring)
                    assertEquals(birth, hrot.birth)
                    assertEquals(survival, hrot.survival)
                }
            }
        }
    }

    @Test
    fun canonise_rulestring_correctly() {
        // Loading test cases
        val testCases = readResource("rules/hrot/hrotGenerationsParsingTest.txt").split("\n").map { it.trim() }

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
                    val hrot = HROTGenerations(rulestring)
                    assertEquals(canonRulestring, hrot.rulestring)
                }
            }
        }
    }

    @Test
    fun simulate_rule_correctly() {
        // Loading test cases
        val testCases = readResource("rules/hrot/hrotGenerationsSimulationTest.txt").split("\n").map { it.trim() }

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
                    val grid = SparseGrid(initial, HROTGenerations(rulestring))
                    grid.step(generations)
                    assertEquals(final, grid.toRLE())
                }
            }
        }
    }

    @Test
    fun calculate_rule_range_correctly() {
        // Loading test cases
        val testCases = readResource("rules/hrot/hrotGenerationsRuleRangeTest.txt").split("\n").map { it.trim() }

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
                    val grid = SparseGrid(rle, HROTGenerations(rulestring))
                    val pattern = grid.identify()

                    assertEquals(type, pattern.toString())
                    assertEquals(minRule, pattern!!.ruleRange!!.minRule.rulestring)
                    assertEquals(maxRule, pattern.ruleRange!!.maxRule.rulestring)
                }
            }
        }
    }

    @Test
    fun enumerate_all_rules() {
        assertEquals(128, enumerateRules(HROTGenerations("23/3/3"),
            HROTGenerations("23678/35678/3")).count())
    }


    @Test
    fun check_deterministic() {
        assertEquals(
            randomRules(HROTGenerations("23/3/3"), HROTGenerations("23678/35678/3"), 10)
                .take(100).map { it.toString() }.toList(),
            randomRules(HROTGenerations("23/3/3"), HROTGenerations("23678/35678/3"), 10)
                .take(100).map { it.toString() }.toList()
        )
    }
}