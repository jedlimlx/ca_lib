import rules.hrot.HROT
import rules.hrot.HROTGenerations
import rules.nontotalistic.rules.INT
import search.cfind.CFind
import search.cfind.ShipSymmetry
import simulation.Coordinate


actual fun main() {
//    val lifeSearch = CFind(
//        INT("B2n3/S23-q"), 3, 1, 13, ShipSymmetry.GLIDE, verbosity = 1,
//        maxQueueSize = 2 shl 17
//    )
//    lifeSearch.search()

//    val hashSearch = CFind(
//        HROT("R2,C2,S4-6,B5-6,N#"), 2, 1, 7, ShipSymmetry.EVEN,
//        verbosity = 1, numShips = 1
//    )
//    hashSearch.search()

//    val minibugsSearch = CFind(
//        HROT("R2,C2,S6-9,B7-8,NM"), 3, 1, 3,
//        ShipSymmetry.ODD, verbosity = 1, direction = Coordinate(1, 1)
//    )
//    minibugsSearch.search()

//    val hashSearch = CFind(
//        HROT("R3,C2,S5-9,B7-9,N#"), 3, 1, 5,
//        ShipSymmetry.EVEN, verbosity = 1, maxQueueSize = 2 shl 18, minDeepeningIncrement = 8
//    )
//    hashSearch.search()

    val generationsSearch = CFind(
        HROTGenerations("23/3/3"), 4, 1, 7, ShipSymmetry.ODD, verbosity = 1
    )
    generationsSearch.search()
}