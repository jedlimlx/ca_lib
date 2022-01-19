package rules.int.transitions

import rules.ruleloader.ruletable.DisjointCyclesSymmetry
import rules.ruleloader.ruletable.Symmetry
import simulation.Coordinate

/**
 * Represents isotropic non-totalistic transitions for range 1 Moore rules using Hensel transitions
 */
class R1MooreINT(string: String): SingleLetterTransitions() {
    override val symmetry: Symmetry = DisjointCyclesSymmetry("[[(1, 3, 5, 7), (2, 4, 6, 8)], [(2, 8), (3, 7), (4, 6)]]")
    override val neighbourhood: Array<Coordinate> = arrayOf(
        Coordinate(0, 1),
        Coordinate(-1, 1),
        Coordinate(-1, 0),
        Coordinate(-1, -1),
        Coordinate(0, -1),
        Coordinate(1, -1),
        Coordinate(1, 0),
        Coordinate(1, 1)
    )

    override val transitionLookup: Array<Map<Char, List<Int>>> = readTransitionsFromResources("int/r1_moore.txt")

    init {
        parseTransition(string)
    }
}