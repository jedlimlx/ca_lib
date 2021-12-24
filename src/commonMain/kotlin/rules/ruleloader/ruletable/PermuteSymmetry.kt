package rules.ruleloader.ruletable

/**
 * Represents Golly's permute symmetry
 */
class PermuteSymmetry : Symmetry() {
    override fun <T> applySymmetry(neighbours: List<T>): Set<List<T>> {
        if (neighbours.size == 1) return setOf(neighbours)

        val perms = mutableListOf<List<T>>()
        val toInsert = neighbours[0]
        for (perm in applySymmetry(neighbours.drop(1))) {
            for (i in 0..perm.size) {
                val newPerm = perm.toMutableList()
                newPerm.add(i, toInsert)
                perms.add(newPerm)
            }
        }

        return perms.toSet()
    }
}