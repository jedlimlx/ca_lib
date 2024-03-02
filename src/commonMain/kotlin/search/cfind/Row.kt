package search.cfind

import simulation.DenseGrid
import simulation.Grid

class Row(val predecessor: Row?, val cells: IntArray, val numStates: Int) {
    private val hash by lazy {
        cells.reduceIndexed { index, acc, state ->
            acc + state * pow(numStates, index)
        }
    }

    var depth = 0
    var prunedDepth = 0

    init {
        if (predecessor != null) {
            depth = predecessor.depth + 1
            prunedDepth = maxOf(depth, predecessor.prunedDepth)
        }
    }

    fun getPredecessor(n: Int): Row? {
        if (n < 0) return null
        if (n == 0) return this
        if (n == 1) return predecessor
        return predecessor?.getPredecessor(n - 1)
    }

    fun getAllPredecessors(n: Int): List<Row> {
        val list = mutableListOf(this)
        var predecessor: Row? = this.predecessor

        while (predecessor != null) {
            if (depth - n == predecessor.depth) break

            list.add(Row(null, predecessor.cells, numStates))
            predecessor = predecessor.predecessor
        }

        return list
    }

    fun completeShip(n: Int): Int {
        var predecessor = this
        for (i in 0..<n) {
            if (!predecessor.isEmpty()) return 0
            if (predecessor.predecessor == null) return 0
            predecessor = predecessor.predecessor!!
        }

        getAllPredecessors(-1).forEach {
            if (!it.isEmpty()) return 1
        }

        return 2
    }

    fun isEmpty(): Boolean = hash == 0

    fun toGrid(period: Int, symmetry: ShipSymmetry): Grid {
        val grid = DenseGrid()
        var temp: Row?
        var predecessor = this

        var counter = 0
        while (true) {
            predecessor.cells.forEachIndexed { index, state ->
                grid[index, -counter] = state
                when(symmetry) {
                    ShipSymmetry.EVEN -> grid[2 * predecessor.cells.size - 1 - index, -counter] = state
                    ShipSymmetry.ODD -> grid[2 * predecessor.cells.size - 2 - index, -counter] = state
                    else -> {}
                }
            }

            temp = predecessor.getPredecessor(period)
            if (temp == null) return grid

            predecessor = temp
            counter++
        }
    }

    override fun hashCode() = hash

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Row

        if (!cells.contentEquals(other.cells)) return false

        return true
    }

    private fun pow(base: Int, exponent: Int): Int {
        if (exponent == 0) return 1
        val temp = pow(base, exponent / 2)
        return if (exponent % 2 == 0) temp * temp else base * temp * temp
    }
}