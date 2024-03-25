package theorems

import simulation.Coordinate

/**
 * Checks the speed limit for a given unweighted [neighbourhood] and [minimumBirth] condition by
 * considering the maximum possible increase in the value of max(x + ny) for some positive integer n.
 * @return Returns the integers n, m. The speed limit is defined by x + ny <= mp and x, y <= r,
 * where (x, y) is the displacement of the ship / front and p is the period.
 */
fun speedLimit(neighbourhood: Array<Coordinate>, minimumBirth: Int): Pair<Int, Int> {
    var bound = 10000.0
    var nMin = 0
    for (n in 1 .. 20) {
        val numbers = neighbourhood.map { it.x + n * it.y }.sorted()
        val test = -numbers[minimumBirth - 1]/(1+n).toDouble()
        if (test < bound) {
            bound = test
            nMin = n
        }
    }

    val numbers = neighbourhood.map { it.x + nMin * it.y }.sorted()

    val m = -numbers[minimumBirth-1]
    return Pair(nMin, m)
}