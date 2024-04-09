package search.cfind

import simulation.Coordinate
import simulation.DenseGrid
import simulation.Grid

class Row(val predecessor: Row?, val cells: IntArray, var search: CFind? = null) {
    companion object { var counter: Long = 0L }

    // unique id for each row
    val id = counter++

    private val hash = run {
        var hash = 0
        for (i in cells.indices) {
            hash += cells[i] * pow(search!!.rule.numStates, i)
        }

        hash
    }

    // information about the row and its position within the larger ship
    var depth = 0
    val prunedDepth: Int
        get() = depth + (successorSequence?.size ?: 0)

    val phase: Int
        get() { return depth.mod(search!!.period) }

    val offset: Int
        get() { return search!!.offsets[depth.mod(search!!.offsets.size)] }

    // information about the row in relation to its siblings in the tree
    var successorSequence: IntArray? = null
    var successorNum: Int = -1

    // represent the queue as a linked list
    var next: Row? = null
    var prev: Row? = null

    init {
        if (predecessor != null)
            depth = predecessor.depth + 1
    }

    operator fun get(index: Int): Int {
        if (search!!.spacing != 1 && (index - offset).mod(search!!.spacing) != 0) return 0
        if (search!!.spacing == 1) return cells[index]
        else return cells[(index - offset) / search!!.spacing]
    }

    fun getPredecessor(n: Int): Row? {  // TODO take width into account when getting the predecessor
        if (n < 0) return null
        if (n == 0) return this

        return predecessor?.getPredecessor(n - this.depth + predecessor.depth)
    }

    fun getAllPredecessors(n: Int, deepCopy: Boolean = true): List<Row> {
        val list = mutableListOf(this)
        var predecessor: Row? = this.predecessor

        while (predecessor != null) {
            if (depth - n == predecessor.depth) break

            list.add(if (deepCopy) Row(null, predecessor.cells, search!!) else predecessor)
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

        var counter = this.offset
        while (true) {
            predecessor.cells.forEachIndexed { index, state ->
                grid[translate(index * search!!.spacing + predecessor.offset, -counter)] = state.toInt()
                when(symmetry) {
                    ShipSymmetry.EVEN -> grid[translate(2 * predecessor.cells.size * search!!.spacing - 1 -
                            (index * search!!.spacing + predecessor.offset), -counter)] = state.toInt()
                    ShipSymmetry.ODD -> grid[translate(2 * predecessor.cells.size * search!!.spacing - 2 -
                            (index * search!!.spacing + predecessor.offset), -counter)] = state.toInt()
                    else -> {}
                }
            }

            temp = predecessor.getPredecessor(period)
            if (temp == null) return grid

            predecessor = temp
            counter++
        }
    }

    fun applyOnPredecessor(f: (Row) -> Unit) {
        if (predecessor != null) {
            f(predecessor)
            predecessor.applyOnPredecessor { f(it) }
        }
    }

    private fun translate(y: Int, x: Int): Coordinate {
        return Coordinate(
            (-x * search!!.direction.x + y * search!!.direction.y) / search!!.spacing,
            (y * search!!.direction.x + x * search!!.direction.y) / search!!.spacing
        )
    }

    fun pop(): Row {
        // link the pointers of the neighbouring rows
        this.prev?.next = this.next
        this.next?.prev = this.prev

        // remove pointers within the row
        this.next = null
        this.prev = null
        return this
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