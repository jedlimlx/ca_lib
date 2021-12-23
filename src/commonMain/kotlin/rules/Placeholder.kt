package rules

import simulation.Coordinate

internal val PLACEHOLDER_RULE = Placeholder()

internal class Placeholder: Rule() {
    override val neighbourhood: Array<Array<Coordinate>> = arrayOf(arrayOf())
    override val possibleSuccessors: Array<Array<IntArray>> = arrayOf(arrayOf(intArrayOf(0, 1), intArrayOf(0, 1)))

    override fun transitionFunc(cells: IntArray, cellState: Int, generation: Int, coordinate: Coordinate): Int = 0
}