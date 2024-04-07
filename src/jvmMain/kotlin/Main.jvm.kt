import rules.hrot.HROT
import search.cfind.CFind
import search.cfind.ShipSymmetry


actual fun main() {
//    for (i in 1 .. 2) {
//        println(shrinkBoundingBox(moore(i)))
//    }

//    for (rule in HROT("R2,C2,S2,B3,NN") .. HROT("R2,C2,S2,5-12,B3,5-12,NN")) {
//        rule as HROT
//        println("B${rule.birth.sorted().joinToString("x")}x/S${rule.survival.sorted().joinToString("x")}xV2")
//    }

//    val minibugsSearch = CFind(
//        HROT("R2,C2,S6-9,B7-8,NM"), 2, 1, 8,
//        ShipSymmetry.ASYMMETRIC, verbosity = 1, maxQueueSize = 1 shl 16, numShips = 1
//    )
//    minibugsSearch.search()

//    val pigsSearch = CFind(
//        HROT("R2,C2,S2,B4,NM"), 3, 4, 12, ShipSymmetry.ODD, verbosity = 1,
//        maxQueueSize = 1 shl 20
//    )
//    pigsSearch.search()

//    val factorioSearch = CFind(
//        HROT("R3,C2,S2,B3,N+"), 3, 1, 3, ShipSymmetry.ODD, verbosity = 1,
//        direction = Coordinate(1, 1)
//    )
//    factorioSearch.search()

//    val vonNeumannSearch = CFind(
//        HROT("R2,C2,S2,B3,NN"), 3, 1, 6, ShipSymmetry.ODD, verbosity = 1
//    )
//    vonNeumannSearch.search()

//    val factorioSearch = CFind(
//        HROT("R3,C2,S2,B3,N+"), 4, 3, 7, ShipSymmetry.EVEN, verbosity = 1, dfs = true
//    )
//    factorioSearch.search()

//    val hashSearch = CFind(
//        HROT("R2,C2,S4-6,B5-6,N#"), 3, 1, 7, ShipSymmetry.EVEN,
//        verbosity = 1, numShips = 1
//    )
//    hashSearch.search()

    val speeds = listOf(
        Pair(3, 1), Pair(3, 2), Pair(4, 3), Pair(4, 1), Pair(5, 4), Pair(5, 3), Pair(5, 2), Pair(5, 1)
    )
    val startingWidth = listOf(8, 10, 6, 7, 6, 6, 5)
    val symmetries = listOf(ShipSymmetry.ODD, ShipSymmetry.EVEN, ShipSymmetry.ASYMMETRIC, ShipSymmetry.GLIDE)

    for (i in speeds.indices) {
        val speed = speeds[i]
        for (symmetry in symmetries) {
            var width = startingWidth[i]
            if (speed == Pair(3, 2) && (symmetry == ShipSymmetry.EVEN || symmetry == ShipSymmetry.ODD))
                width = 10

            if (speed == Pair(3, 2) && (symmetry == ShipSymmetry.GLIDE))
                continue

            var OOM = false
            var foundShip = false
            while (width < 11 && !OOM && !foundShip) {
                try {
                    val minibugsSearch = CFind(
                        HROT("R2,C2,S6-9,B7-8,NM"), speed.first, speed.second, width++, symmetry, verbosity = 1,
                        maxQueueSize = 1 shl 21, partialFrequency = 10, stdin = true
                    )
                    minibugsSearch.search()
                    if (minibugsSearch.searchResults.size >= 1) foundShip = true
                } catch (error: Error) { OOM = true }
            }
        }
    }

//    val hashSearch = CFind(
//        HROT("R3,C2,S5-9,B7-9,N#"), 4, 1, 5,
//        ShipSymmetry.EVEN, verbosity = 1, maxQueueSize = 1 shl 18
//    )
//    hashSearch.search()

//    val generationsSearch = CFind(
//        HROTGenerations("345/367/3"), 2, 1, 5, ShipSymmetry.ODD,
//        verbosity = 1
//    )
//    generationsSearch.search()
}