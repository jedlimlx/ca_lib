package patterns

import rules.Rule
import rules.ruleRange
import simulation.Coordinate
import simulation.Grid

/**
 * Represents a spaceship in a cellular automaton
 * @property dx The displacement of the spaceship in the x-direction
 * @property dy The displacement of the spaceship in the y-direction
 * @property period The period of the spaceship
 * @property phases An array of grids representing the phases of the spacship
 */
open class Spaceship(val dx: Int, val dy: Int, val period: Int, val phases: Array<Grid>): Pattern() {
    /**
     * The rule that the spaceship operates in
     */
    val rule = phases[0].rule

    /**
     * The spaceship's speed formatted properly as a string
     */
    val speed by lazy {
        when {
            period == 0 -> "Still Life"
            dx == 0 && dy == 0 -> "P${period}"
            dx == 0 -> "${if (dy != 1) dy else ""}c/${period}o"
            dy == 0 -> "${if (dx != 1) dx else ""}c/${period}o"
            dx == dy -> "${if (dx != 1) dx else ""}c/${period}d"
            else -> "($dx, $dy)c/$period"
        }
    }

    /**
     * The spaceship's direction (orthogonal, diagonal, knight, etc.)
     */
    val direction by lazy {
        when {
            period == 0 -> "still life"
            dx == 0 && dy == 0 -> "oscillator"
            dx == 0 || dy == 0 -> "orthogonal"
            dx == dy -> "diagonal"
            dx == 2 * dy || dy == 2 * dx -> "knight"
            dx == 3 * dy || dy == 3 * dx -> "camel"
            dx == 4 * dy || dy == 4 * dx -> "giraffe"
            dx == 5 * dy || dy == 5 * dx -> "ibis"
            dx == 6 * dy || dy == 6 * dx -> "flamingo"
            2 * dx == 3 * dy || 2 * dy == 3 * dx -> "zebra"
            3 * dx == 4 * dy || 3 * dy == 4 * dx -> "antelope"
            5 * dx == 23 * dy || 5 * dy == 23 * dx -> "waterbear"
            else -> "oblique"
        }
    }

    /**
     * The number of cells that change in from each phase of the spaceship
     */
    open val heat by lazy {
        val checked: HashSet<Coordinate> = hashSetOf()
        val result = IntArray(phases.size) { 0 }

        var prevPhase = phases[phases.size - 1].deepCopy().shift(-dx, -dy)
        for (i in phases.indices) {
            checked.clear()
            result[i] = phases[i].fold(0) { acc, value ->
                checked.add(value.first)
                acc + if (phases[i][value.first, true] == prevPhase[value.first, true]) 0 else 1
            } + prevPhase.fold(0) { acc, value ->
                if (value.first !in checked)  // Avoid double counting
                    acc + if (phases[i][value.first, true] == prevPhase[value.first, true]) 0 else 1
                else acc
            }

            prevPhase = phases[i]
        }

        result
    }

    /**
     * The population of the spaceship in each generation
     */
    open val populationList by lazy { phases.map { it.population } }

    /**
     * The population of the spaceship by state in each generation
     */
    open val populationListByState by lazy { phases.map { it.populationByState } }

    override val information: Map<String, String> by lazy {
        mapOf(
            "rule" to rule.toString(),
            "speed" to speed,
            "direction" to direction,
            "heat_stats" to "${heat.minOrNull()!!} | ${heat.maxOrNull()!!} | ${heat.average()}",
            "pop_stats" to "${populationList.minOrNull()!!} | ${populationList.maxOrNull()!!} | ${populationList.average()}"
        )
    }

    override val ruleRange: Pair<Rule, Rule>? by lazy {
        val newPhases = phases.toMutableList()
        newPhases.add(newPhases[newPhases.size - 1].deepCopy().step())

        ruleRange(newPhases)
    }

    /**
     * Constructs a spaceship given only 1 phase of the spaceship
     */
    constructor(dx: Int, dy: Int, period: Int, phase: Grid) :
            this(dx, dy, period, Array(period) { phase.step(1).deepCopy() })

    /**
     * Checks if 2 spaceships are the same
     * @param
     * @return Returns true if the spaceships are the same, false otherwise
     */
    override fun equals(other: Any?): Boolean {
        if (other !is Spaceship) return false
        return phases.contentEquals(other.phases) && period == other.period && dx == other.dx && dy == other.dy
    }

    /**
     * Checks the hash code of the spaceship
     */
    override fun hashCode(): Int {
        var result = dx
        result = 31 * result + dy
        result = 31 * result + period
        result = 31 * result + phases.contentHashCode()
        result = 31 * result + rule.hashCode()
        return result
    }

    override fun toString(): String {
        return "($dx, $dy)c/$period Spaceship"
    }
}