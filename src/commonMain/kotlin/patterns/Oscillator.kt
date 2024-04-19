package patterns

import simulation.Coordinate
import simulation.Grid

/**
 * Represents an oscillator in a cellular automaton rule
 * @property period The period of the oscillator
 * @property phases An array of grids representing the phases of the oscillator
 */
class Oscillator(period: Int, phases: Array<Grid>) : Spaceship(0, 0, period, phases) {
    /**
     * A dictionary containing the period at which a cell at the given coordinate oscillates at
     */
    val cellPeriods by lazy {
        // Recording down the list of states that each cell goes through
        val cells: HashMap<Coordinate, IntArray> = hashMapOf()
        for (i in phases.indices) {
            phases[i].forEach {
                if (it.first !in cells) cells[it.first] = IntArray(phases.size) { 0 }
                cells[it.first]!![i] = phases[i][it.first, true]
            }
        }

        cells.map {
            // Detect cycles in the aforementioned list
            var cycles = period
            while (cycles >= 1) {
                // Skip if its not divisible
                if (period % cycles != 0) {
                    cycles--
                    continue
                }

                val cycleLength = period / cycles
                val check = it.value.slice(0 until cycleLength)

                var broke = false
                for (i in 1 until cycles) {
                    if (it.value.slice(i * cycleLength until (i + 1) * cycleLength) != check) {
                        broke = true
                        break
                    }
                }

                if (!broke) return@map it.key to period / cycles

                cycles--
            }

            it.key to period
        }.toMap()
    }

    /**
     * The number of rotor cells in the oscillator
     */
    val rotorCells by lazy { cellPeriods.count { it.value > 1 } }

    /**
     * The number of stator cells in the oscillator
     */
    val statorCells by lazy { cellPeriods.count { it.value == 1 } }

    /**
     * The number of active cells in the oscillator
     */
    val activeCells by lazy { cellPeriods.size }

    /**
     * Is the oscillator is non-trivial?
     */
    val nonTrivial by lazy { (cellPeriods.count { it.value == period }) >= 1 }

    /**
     * The number of cells that change in from each phase of the oscillator
     */
    override val heat by lazy { super.heat }

    /**
     * Computes the smallest phase of the spaceship by population
     */
    override val smallestPhase by lazy { super.smallestPhase }

    /**
     * The population of the oscillator in each generation
     */
    override val populationList by lazy { super.populationList }

    /**
     * The population of the oscillator by state in each generation
     */
    override val populationListByState by lazy { super.populationListByState }

    /**
     * The temperature of the oscillator
     */
    val temperature by lazy { heat.map { it / activeCells.toDouble() } }

    /**
     * The volatility of the oscillator
     */
    val volatility by lazy { rotorCells / activeCells.toDouble() }

    /**
     * The strict volatility of the oscillator
     */
    val strictVolatility by lazy { cellPeriods.count { it.value == period } / activeCells.toDouble() }

    override val information: Map<String, String> by lazy {
        mapOf(
            "rule" to rule.toString(),
            "speed" to speed,
            "direction" to direction,
            "cell_stats" to "$rotorCells | $statorCells | $activeCells",
            "heat_stats" to "${heat.minOrNull()!!} | ${heat.maxOrNull()!!} | ${heat.average()}",
            "temp_stats" to "${temperature.minOrNull()!!} | ${temperature.maxOrNull()!!} | ${temperature.average()}",
            "vola_stats" to "$volatility | $strictVolatility",
            "pop_stats" to "${populationList.minOrNull()!!} | ${populationList.maxOrNull()!!} | ${populationList.average()}"
        )
    }

    /**
     * Constructs an oscillator given only 1 phase of the oscillator
     */
    constructor(period: Int, phase: Grid) : this(period, Array(period) { phase.step().deepCopy() })

    override fun toString(): String {
        return "P$period Oscillator"
    }
}