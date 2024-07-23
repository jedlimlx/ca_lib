package search.cfind

import simulation.Coordinate
import simulation.Grid
import simulation.SparseGrid

class Row(
    val predecessor: Row?, val cells: IntArray, val search: CFind,
    hash: Int? = null, reverseHash: Int? = null
): Comparable<Row> {
    companion object { var counter: Long = 0L }

    // unique id for each row
    val id = counter++

    val depthPeriod = search.spacing

    val hash = run {
        if (hash == null) {
            var _hash = 0
            var p = 1
            for (i in cells.indices) {
                _hash += cells[i] * p
                p *= search.rule.numStates
            }

            _hash
        } else hash
    }

    val reverseHash = run {
        if (reverseHash == null) {
            var _hash = 0
            if (
                search.isotropic &&
                (search.symmetry == ShipSymmetry.GLIDE || search.symmetry == ShipSymmetry.ASYMMETRIC)
            ) {
                var p = 1
                for (i in cells.indices) {
                    _hash += cells[cells.size - i - 1] * p
                    p *= search.rule.numStates
                }
            }

            _hash
        } else reverseHash
    }

    // information about how the cells are read from the row
    val useArray = search.rule.numStates == 2 && search.width < 31

    // information about the row and its position within the larger ship
    var depth = 0
    val prunedDepth: Int
        get() = depth + (successorSequence?.size ?: 0)

    val phase: Int
        get() { return depth.mod(search.period) }

    val offset: Int
        get() { return search.offsets[depth.mod(search.offsets.size)] }

    val background: Int
        get() { return search.background[depth.mod(search.background.size)] }

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
//        if (search.spacing != 1 && (index - offset).mod(search.spacing) != 0) {
//            println("crap $depth $index $offset ${(index - offset).mod(search.spacing)}")
//            return 0
//        }
        return if (useArray) {
            if (search.spacing == 1) (hash and (1 shl index)) shr index
            else (hash and (1 shl (index / search.spacing))) shr (index / search.spacing)
        } else {
            if (search.spacing == 1) cells[index]
            else cells[index / search.spacing]
        }
    }

    operator fun get(_startIndex: Int, _endIndex: Int): Int {
        if (_startIndex < 0 && _endIndex < 0) {
            if (background == 0) return 0
            else return (1 shl ((_endIndex - _startIndex + 1) / search.spacing)) - 1
        }
        return if (search.spacing == 1) {
            val startIndex = maxOf(_startIndex, 0)
            val endIndex = minOf(maxOf(_endIndex, 0), search.width - 1)
            val mask = ((1 shl (endIndex - startIndex + 1)) - 1) shl startIndex

            if (background == 1) {
                val output = (1 shl (_endIndex - _startIndex + 1)) - 1
                output - if (_startIndex < 0) (hash.inv() and mask) shl -_startIndex
                else (hash.inv() and mask) shr _startIndex
            } else {
                if (_startIndex < 0) (hash and mask) shl -_startIndex
                else (hash and mask) shr _startIndex
            }
        } else {
            val startIndex = maxOf(_startIndex, 0) / search.spacing
            val endIndex = minOf(maxOf(_endIndex, 0), search.width * search.spacing - 1) / search.spacing
            val mask = ((1 shl (endIndex - startIndex + 1)) - 1) shl startIndex

            val output = if (background == 1) {
                val output = (1 shl ((_endIndex - _startIndex) / search.spacing + 1)) - 1
                output - if (_startIndex < 0)
                    (hash.inv() and mask) shl ((-_startIndex + search.spacing - 1) / search.spacing)
                else (hash.inv() and mask) shr startIndex
            } else {
                if (_startIndex < 0) (hash and mask) shl ((-_startIndex + search.spacing - 1) / search.spacing)
                else (hash and mask) shr startIndex
            }

            return output
        }
    }

    fun getPredecessor(n: Int): Row? {  // TODO take width into account when getting the predecessor
        if (n < 0) return null
        if (n == 0) return this

        return predecessor?.getPredecessor(n - this.depth + predecessor.depth)
    }

    fun getAllPredecessors(n: Int, deepCopy: Boolean = true): List<Row> {
        val list = mutableListOf(this)
        if (n == 0) return list

        var predecessor: Row? = this.predecessor

        while (predecessor != null) {
            if (depth - n == predecessor.depth) break

            list.add(
                if (deepCopy) Row(null, predecessor.cells, search, predecessor.hash, predecessor.reverseHash)
                else predecessor
            )
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

        getAllPredecessors(-1, deepCopy = false).forEach {
            if (!it.isEmpty()) return 1
        }

        return 2
    }

    fun isEmpty(): Boolean = hash == search.emptyHash[background]

    fun toGrid(period: Int, symmetry: ShipSymmetry): Grid {
        val grid = SparseGrid()
        var temp: Row?
        var predecessor = this
        while (predecessor.background != 0) predecessor = predecessor.predecessor!!

        var counter = this.offset
        while (true) {
            predecessor.cells.forEachIndexed { index, state ->
                grid[translate(index * search.spacing + predecessor.offset, -counter)] = state
                when(symmetry) {
                    ShipSymmetry.EVEN -> grid[translate(2 * predecessor.cells.size * search.spacing - 1 -
                            (index * search.spacing + predecessor.offset), -counter)] = state
                    ShipSymmetry.ODD -> grid[translate(2 * predecessor.cells.size * search.spacing - 2 -
                            (index * search.spacing + predecessor.offset), -counter)] = state
                    ShipSymmetry.GUTTER -> grid[translate(2 * predecessor.cells.size * search.spacing -
                            (index * search.spacing + predecessor.offset), -counter)] = state
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
            (-x * search.direction.x + y * search.direction.y) / search.spacing,
            (y * search.direction.x + x * search.direction.y) / search.spacing
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

    override fun hashCode() = hash + 293 * depth.mod(depthPeriod)
    fun reverseHashCode() = reverseHash + 293 * depth.mod(depthPeriod)
    override fun compareTo(other: Row): Int = this.depth - other.depth

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Row

        return cells.contentEquals(other.cells)
    }
}