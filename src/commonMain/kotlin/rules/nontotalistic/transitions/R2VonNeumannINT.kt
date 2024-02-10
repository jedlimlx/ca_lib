package rules.nontotalistic.transitions

import rules.ruleloader.ruletable.DisjointCyclesSymmetry
import rules.ruleloader.ruletable.Symmetry
import simulation.Coordinate

/**
 * Represents isotropic non-totalistic transitions for range 1 Moore rules using Hensel notation
 */
class R2VonNeumannINT: DoubleLetterTransitions {
    override val symmetry: Symmetry = DisjointCyclesSymmetry(
        "[[(1, 3, 5, 7), (2, 4, 6, 8), (9, 10, 11, 12)], [(2, 8), (3, 7), (4, 6), (10, 12)]]"
    )
    override val neighbourhood: Array<Coordinate> = arrayOf(
        Coordinate(0, 1),
        Coordinate(-1, 1),
        Coordinate(-1, 0),
        Coordinate(-1, -1),
        Coordinate(0, -1),
        Coordinate(1, -1),
        Coordinate(1, 0),
        Coordinate(1, 1),
        Coordinate(0, 2),
        Coordinate(-2, 0),
        Coordinate(0, -2),
        Coordinate(2, 0)
    )

    override val isotropicTransitionLookup: Array<Map<Char, List<Int>>> = readDoubleTransitionsFromResources(
        "int/r1_moore.txt", "int/anisotropic.txt"
    ).first
    override val anisotropicTransitionLookup: Map<Char, List<Int>> = readDoubleTransitionsFromResources(
        "int/r1_moore.txt", "int/anisotropic.txt"
    ).second
    override val reverseTransitionLookup: Map<List<Int>, String> = generateReverseLookup().first
    override val transitionsByOuterTotalistic: Array<Set<String>> = generateReverseLookup().second

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

    override fun clone(): R2VonNeumannINT = R2VonNeumannINT(transitionString)
}