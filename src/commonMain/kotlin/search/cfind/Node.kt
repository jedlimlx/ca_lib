package search.cfind

data class Node(
    var predecessor: Node?,
    val cells: Int,
    val prevCell: Int,
    val depth: Int,
    val numStates: Int,
    val singleBaseCoordinate: Boolean = false
) {
    val completeRow: IntArray? by lazy {
        var count = depth - 1
        var tempNode: Node? = this
        val temp = IntArray(depth) { 0 }
        while (count >= 0) {
            temp[count--] = tempNode!!.prevCell
            tempNode = tempNode.predecessor
        }

        temp
        //predecessor?.completeRow?.plus(intArrayOf(prevCell)) ?: intArrayOf()
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