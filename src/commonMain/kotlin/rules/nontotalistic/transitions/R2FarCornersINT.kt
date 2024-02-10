package rules.nontotalistic.transitions

import rules.ruleloader.ruletable.DisjointCyclesSymmetry
import rules.ruleloader.ruletable.Symmetry
import simulation.Coordinate

/**
 * Represents isotropic non-totalistic transitions for range 2 far corners rules using Hensel notation
 */
class R2FarCornersINT: SingleLetterTransitions {
    override val symmetry: Symmetry = DisjointCyclesSymmetry("[[(1, 3, 5, 7), (2, 4, 6, 8)], [(4, 8), (1, 3), (5, 7)]]")
    override val neighbourhood: Array<Coordinate> = arrayOf(
        Coordinate(0, 1),
        Coordinate(2, 2),
        Coordinate(1, 0),
        Coordinate(2, -2),
        Coordinate(0, -1),
        Coordinate(-2, -2),
        Coordinate(-1, 0),
        Coordinate(-2, 2)
    )

    override val transitionLookup: Array<Map<Char, List<Int>>> = readIsotropicTransitionsFromResources("int/r1_moore.txt").first
    override val reverseTransitionLookup: Map<List<Int>, String> = readIsotropicTransitionsFromResources("int/r1_moore.txt").second

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

    override fun clone(): R2FarCornersINT = R2FarCornersINT(transitionString)
}