package search.cfind

data class Node(
    var predecessor: Node?,
    var stackPredecessor: Node?,
    val cells: Int,
    val prevCell: Int,
    val depth: Int,
    val numStates: Int,
    val singleBaseCoordinate: Boolean = false
) {
    var _completeRow: IntArray? = null
    val completeRow: IntArray
        get() {
            if (_completeRow == null) {
                var count = depth - 1
                var tempNode: Node? = this
                val temp = IntArray(depth) { 0 }
                while (count >= 0) {
                    temp[count--] = tempNode!!.prevCell
                    tempNode = tempNode.predecessor
                }

                _completeRow = temp
            }

            return _completeRow!!
        }

    override fun hashCode(): Int {
        return cells + depth * 100
    }

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is Node) return false
        return other.cells == cells && other.depth == depth
    }
}