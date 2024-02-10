package rules.nontotalistic.transitions

import rules.ruleloader.ruletable.DisjointCyclesSymmetry
import rules.ruleloader.ruletable.Symmetry
import simulation.Coordinate

/**
 * Represents isotropic non-totalistic transitions for range 2 cross rules
 */
class R2CrossINT: SingleLetterTransitions {
    override val symmetry: Symmetry = DisjointCyclesSymmetry("[[(1, 2, 3, 4), (5, 6, 7, 8)], [(2, 4), (6, 8)]]")
    override val neighbourhood: Array<Coordinate> = arrayOf(
        Coordinate(0, 2),
        Coordinate(2, 0),
        Coordinate(0, -2),
        Coordinate(-2, 0),
        Coordinate(0, 1),
        Coordinate(1, 0),
        Coordinate(0, -1),
        Coordinate(-1, 0)
    )

    override val transitionLookup: Array<Map<Char, List<Int>>> = readIsotropicTransitionsFromResources("int/r2_cross.txt").first
    override val reverseTransitionLookup: Map<List<Int>, String> = readIsotropicTransitionsFromResources("int/r2_cross.txt").second

    override val size: Int

    constructor(string: String) {
        parseTransition(string)
        size = transitions.size
    }

    // string is a useless variable to ensure that the overloading works
    constructor(transitions: Iterable<String>, string: String) {
        loadStringTransitions(transitions)
        size = this.transitions.size
    }

    constructor(transitions: Iterable<List<Int>>) {
        loadTransitions(transitions)
        size = this.transitions.size
    }

    override fun clone(): R2CrossINT = R2CrossINT(transitionString)
}