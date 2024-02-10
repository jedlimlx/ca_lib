package rules.nontotalistic.transitions

import rules.ruleloader.ruletable.DisjointCyclesSymmetry
import rules.ruleloader.ruletable.Symmetry
import simulation.Coordinate

/**
 * Represents isotropic non-totalistic transitions for range 1 Hex rules using notation based on arene substitution patterns
 */
class R1HexINT: SingleLetterTransitions {
    override val symmetry: Symmetry = DisjointCyclesSymmetry("[[(1, 2, 3, 4, 5, 6)], [(1, 6), (2, 5), (3, 4)]]")
    override val neighbourhood: Array<Coordinate> = arrayOf(
        Coordinate(0, -1),
        Coordinate(1, 0),
        Coordinate(1, 1),
        Coordinate(0, 1),
        Coordinate(-1, 0),
        Coordinate(-1, -1)
    )

    override val transitionLookup: Array<Map<Char, List<Int>>> = readIsotropicTransitionsFromResources("int/r1_hex.txt").first
    override val reverseTransitionLookup: Map<List<Int>, String> = readIsotropicTransitionsFromResources("int/r1_hex.txt").second

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

    override fun clone(): R1HexINT = R1HexINT(transitionString)
}