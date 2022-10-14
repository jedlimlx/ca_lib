package rules.nontotalistic.transitions

import rules.ruleloader.ruletable.DisjointCyclesSymmetry
import rules.ruleloader.ruletable.Symmetry
import simulation.Coordinate

/**
 * Represents isotropic non-totalistic transitions for range 2 Knight rules
 */
class R2KnightINT: SingleLetterTransitions {
    override val symmetry: Symmetry = DisjointCyclesSymmetry("[[(1, 3, 5, 7), (2, 4, 6, 8)], [(1, 8), (2, 7), (3, 6), (4, 5)]]")
    override val neighbourhood: Array<Coordinate> = arrayOf(
        Coordinate(-2, 1),
        Coordinate(-1, 2),
        Coordinate(1, 2),
        Coordinate(2, 1),
        Coordinate(2, -1),
        Coordinate(1, -2),
        Coordinate(-1, -2),
        Coordinate(-2, -1)
    )

    override val transitionLookup: Array<Map<Char, List<Int>>> = readTransitionsFromResources("int/r2_knight.txt").first
    override val reverseTransitionLookup: Map<List<Int>, String> = readTransitionsFromResources("int/r2_knight.txt").second

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