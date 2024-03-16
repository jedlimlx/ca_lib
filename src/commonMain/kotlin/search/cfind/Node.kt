package search.cfind

data class Node(
    var predecessor: Node?,
    val cells: Int,
    val prevCell: Int,
    val depth: Int,
    val numStates: Int,
    val singleBaseCoordinate: Boolean = false
) {
    val completeRow: IntArray by lazy {
        predecessor?.completeRow?.plus(intArrayOf(prevCell)) ?: intArrayOf()
    }

    fun applyOnPredecessor(f: (Node) -> Unit) {
        f(this)
        predecessor?.applyOnPredecessor(f)
    }

    override fun hashCode(): Int {
        return cells + depth * 100
    }

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is Node) return false
        return other.cells == cells && other.depth == depth
    }
}