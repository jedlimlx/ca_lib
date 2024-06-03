package patterns

import rules.*
import simulation.Coordinate
import simulation.Grid
import simulation.SparseGrid
import simulation.Rotation
import simulation.Flip
import kotlin.math.abs

/**
 * Constructs a spaceship based on the GliderDB entry
 */
fun fromGliderDBEntry(entry: String): Spaceship {
    val tokens = entry.split(":")
    
    val period = tokens[4].split("/").first().toInt()
    val dx = tokens[5].toInt()
    val dy = tokens[6].toInt()

    val grid = SparseGrid(tokens.last(), rule=fromRulestring(tokens[2]))
    val phases = Array(period+3) { grid.step(1).deepCopy() }

    val spaceship = Spaceship(dx, dy, period, phases)

    // TODO fix this
//    if (spaceship.ruleRange!!.minRule.rulestring != tokens[2]) {
//        println("Incorrect minimum rule! Got ${tokens[2]} instead of ${spaceship.ruleRange!!.minRule.rulestring} for ${tokens.last()}")
//    }
//    if (spaceship.ruleRange!!.maxRule.rulestring != tokens[3]) {
//        println("Incorrect maximum rule! Got ${tokens[3]} instead of ${spaceship.ruleRange!!.maxRule.rulestring} for ${tokens.last()}")
//    }

    spaceship.name = tokens[0]
    spaceship.discoverer = tokens[1]
    return spaceship
}

/**
 * Represents a spaceship in a cellular automaton
 * @property dx The displacement of the spaceship in the x-direction
 * @property dy The displacement of the spaceship in the y-direction
 * @property period The period of the spaceship
 * @property phases An array of grids representing the phases of the spacship
 */
open class Spaceship(val dx: Int, val dy: Int, val period: Int, val phases: Array<Grid>) : Pattern() {
    /**
     * The rule that the spaceship operates in
     */
    override val rule = phases[0].rule

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
     * The spaceship's simplified speed outputted as a tuple of (dx, dy, p)
     */
    val simplifiedSpeed by lazy {
        fun gcd(a: Int, b: Int): Int {
            var num1 = a
            var num2 = b
            while (num2 != 0) {
                val temp = num2
                num2 = num1 % num2
                num1 = temp
            }

            return num1
        }

        val factor = gcd(gcd(abs(dx), abs(dy)), period)
        return@lazy Pair(Pair(minOf(abs(dx), abs(dy)) / factor, maxOf(abs(dx), abs(dy)) / factor), period / factor)
    }

    /**
     * The spaceship's direction (orthogonal, diagonal, knight, etc.)
     */
    val direction by lazy {
        when {
            period == 0 -> "still life"
            dx == 0 && dy == 0 -> "oscillator"
            dx == 0 || dy == 0 -> "orthogonal"
            abs(dx) == abs(dy) -> "diagonal"
            abs(dx) == 2 * abs(dy) || abs(dy) == 2 * abs(dx) -> "knight"
            abs(dx) == 3 * abs(dy) || abs(dy) == 3 * abs(dx) -> "camel"
            abs(dx) == 4 * abs(dy) || abs(dy) == 4 * abs(dx) -> "giraffe"
            abs(dx) == 5 * abs(dy) || abs(dy) == 5 * abs(dx) -> "ibis"
            abs(dx) == 6 * abs(dy) || abs(dy) == 6 * abs(dx) -> "flamingo"
            2 * abs(dx) == 3 * abs(dy) || 2 * abs(dy) == 3 * abs(dx) -> "zebra"
            3 * abs(dx) == 4 * abs(dy) || 3 * abs(dy) == 4 * abs(dx) -> "antelope"
            5 * abs(dx) == 23 * abs(dy) || 5 * abs(dy) == 23 * abs(dx) -> "waterbear"
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
     * Computes the smallest phase of the spaceship by population
     * TODO figure out the correct way of finding the canonical phase for gliderdb
     */
    open val smallestPhase by lazy { phases.minBy { it.population } }

    /**
     * Computes the canonical phase of the spaceship, defined as when the spaceship is moving to the north-west.
     * (only works if the spaceship operates in an isotropic rule)
     */
    open val canonPhase by lazy {
        val output = smallestPhase.deepCopy()
        output.updateBounds()

        // Orienting the spaceship to go in the north-west direction
        var dx = dx
        var dy = dy

        if (dy < 0) {
            dy = -dy
            output.flip(Flip.VERTICAL)
        }

        if (dx < 0) {
            dx = -dx
            output.flip(Flip.HORIZONTAL)
        }
        if (dy < dx) output.rotate(Rotation.CLOCKWISE)
        
        output
    }

    /**
     * The population of the spaceship in each generation
     */
    open val populationList by lazy { phases.map { it.population } }

    /**
     * The population of the spaceship by state in each generation
     */
    open val populationListByState by lazy { phases.map { it.populationByState } }

    /**
     * Outputs the GliderDB entry for this ship. The format is <name>:<discoverer>, <year>:<minRule>:<maxRule>:<x>:<y>:<dx>:<dy>:
     */
    open val gliderdbEntry by lazy {
        canonPhase.updateBounds()
        val size = canonPhase.bounds.endInclusive - canonPhase.bounds.start
        "$name:$discoverer:${ruleRange!!.minRule}:${ruleRange!!.maxRule}:$period:${minOf(abs(dx),abs(dy))}:${maxOf(abs(dx),abs(dy))}:${size.x}:${size.y}:${canonPhase.toRLE(Int.MAX_VALUE)}"
    }

    override val information: Map<String, String> by lazy {
        val map = hashMapOf(
            "rule" to rule.toString(),
            "speed" to speed,
            "direction" to direction,
            "heat_stats" to "${heat.minOrNull()!!} | ${heat.maxOrNull()!!} | ${heat.average()}",
            "pop_stats" to "${populationList.minOrNull()!!} | ${populationList.maxOrNull()!!} | ${populationList.average()}"
        )

        // Optional outputs
        if (name != "") map["name"] = name
        if (discoverer != "") map["discoverer"] = discoverer

        map
    }

    override val ruleRange: RuleRange<*>? by lazy {
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
     * @param other The other spaceship
     * @return Returns true if the spaceships are the same, false otherwise
     */
    override fun equals(other: Any?): Boolean {
        if (other !is Spaceship) return false
        return if (ruleRange != null) {
            ruleRange == other.ruleRange &&
            canonPhase == other.canonPhase &&
            period == other.period && dx == other.dx && dy == other.dy
        } else {
            rule == other.rule &&
            canonPhase == other.canonPhase &&
            period == other.period && dx == other.dx && dy == other.dy
        }
    }

    /**
     * Checks the hash code of the spaceship
     */
    override fun hashCode(): Int {
        var result = dx
        result = 31 * result + dy
        result = 31 * result + period
        result = 31 * result + phases.contentHashCode()
        result = 31 * result + ruleRange.hashCode()
        return result
    }

    override fun toString(): String {
        return "($dx, $dy)c/$period Spaceship"
    }
}