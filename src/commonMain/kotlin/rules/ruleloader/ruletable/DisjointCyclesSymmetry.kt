package rules.ruleloader.ruletable

/**
 * Represents symmetries as disjoint cycles like lifelib.
 * See https://groupprops.subwiki.org/wiki/Cycle_decomposition_for_permutations.
 */
class DisjointCyclesSymmetry : Symmetry {
    /**
     * The disjoint cycles used to represent the symmetry
     */
    val disjointCycles: Array<Array<IntArray>>

    /**
     * Constructs a new symmetry with an inputted string.
     * For example, [[(1, 3, 5, 7), (2, 4, 6, 8)], [(2, 8), (3, 7), (4, 6)]]
     * @param symmetry The symmetry string that represents the symmetry in the form of disjoint cycles.
     */
    constructor(symmetry: String) {
        disjointCycles = Regex("\\[(\\((\\d+,?\\s*)+\\),?\\s*)+]").findAll(symmetry).map {
            Regex("(\\d+,?\\s*)+").findAll(it.groupValues[0]).map {
                it.groupValues[0].split(Regex(",\\s*")).map { it.toInt() }.toIntArray()
            }.toList().toTypedArray()
        }.toList().toTypedArray()
    }

    /**
     * Constructs a new symmetry with the provided disjoint cycles
     * @param disjointCycles The disjoint cycles that represent the symmetry
     */
    constructor(disjointCycles: Array<Array<IntArray>>) {
        this.disjointCycles = disjointCycles
    }

    override fun <T> applySymmetry(neighbours: List<T>): Set<List<T>> {
        var symmetry: ArrayList<Int>

        // Converting disjoint cycles to permutation generators
        val symmetries = disjointCycles.map {
            symmetry = ArrayList((1..neighbours.size).toList())

            if (neighbours.isNotEmpty()) {
                for (cycle in it) {  // Definitely not taken from lifelib
                    for (i in cycle.indices) {
                        if (i - 1 < 0) symmetry[cycle[cycle.size + i - 1] - 1] = cycle[i]
                        else symmetry[cycle[i - 1] - 1] = cycle[i]
                    }
                }
            }

            symmetry
        }

        val permutations = arrayListOf(neighbours.toList())

        var i = 0 // Generating the permutations
        val used = HashSet<List<T>>()

        var currentPermutation: List<T>
        var composedPermutation: List<T>
        while (i < permutations.size) {
            currentPermutation = permutations[i++]

            // Compose the current permutation with the and include any previously unseen elements to the permutation group
            for (symmetry1 in symmetries) {
                // Composing permutations
                composedPermutation = symmetry1.map { currentPermutation[it - 1] }.toList()

                if (!used.contains(composedPermutation)) {
                    used.add(composedPermutation)
                    permutations.add(composedPermutation)
                }
            }
        }

        return permutations.toSet()
    }
}