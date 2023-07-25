package rules

import kotlin.math.abs


/**
 * Reads the extended generations portion of rulestrings
 * @param rulestring The extended generations rulestring e.g. 0-1-2
 */
fun readExtendedGenerations(rulestring: String): Pair<Int, Set<Int>> {
    val activeCells = HashSet<Int>()

    var state = 1
    var active = true
    rulestring.split("-").forEach {
        if (active) {
            for (i in state until state + it.toInt()) {
                activeCells.add(i)
            }
        }

        active = !active
        state += it.toInt()
    }

    return Pair(state, activeCells)
}


/**
 * Outputs the canonical string representing the [activeStates] provided
 * @param numStates The number of states in the rule
 * @param activeStates The active states provided
 * @return The canonical string representing the active states provided
 */
fun canoniseExtendedGenerations(numStates: Int, activeStates: Set<Int>): String = with(StringBuilder("")) {
    var count = 0
    var start = true

    if (1 !in activeStates) {
        append(0)
        append("-")
    }

    for (state in 1 until numStates) {
        if (state in activeStates) {
            if (count < 0) {
                if (!start) append("-")
                else start = false

                append(-count)
                count = 0
            }

            count++
        } else {
            if (count > 0) {
                if (!start) append("-")
                else start = false

                append(count)
                count = 0
            }

            count--
        }
    }

    append("-")
    append(abs(count))

    this.toString()
}

