package rules.ruleloader.ruletable

/**
 * Represents the symmetry of a ruletable
 */
abstract class Symmetry {
    /**
     * Applies the symmetry on the provided array list
     * @param neighbours The neighbours of the cell that the symmetry should be applied on
     * @param <T> The type of the array list
     * @return Returns an array list of array lists represents the permutations
    </T> */
    abstract fun <T> applySymmetry(neighbours: List<T>): Set<List<T>>

    /**
     * Applies the symmetry on the provided array list
     * @param neighbours The neighbours of the cell that the symmetry should be applied on
     * @param <T> The type of the array list
     * @return Returns an array list of array lists represents the permutations
    </T> */
    operator fun <T> invoke(neighbours: List<T>): Set<List<T>> = applySymmetry(neighbours)
}