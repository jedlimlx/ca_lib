package search.cfind

data class Node(
    var predecessor: Node?,
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
                var tempNode: Node? = this
                val temp = IntArray(depth) {
                    val output = tempNode!!.prevCell
                    tempNode = tempNode?.predecessor

                    output
                }

                _completeRow = temp.reversedArray()
            }

            return _completeRow!!
        }

    fun applyOnPredecessor(f: (Node) -> Boolean) {
        if (f(this)) predecessor?.applyOnPredecessor(f)
    }

    override fun hashCode(): Int {
        return cells + depth * 100
    }

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is Node) return false
        return other.cells == cells && other.depth == depth
    }
}