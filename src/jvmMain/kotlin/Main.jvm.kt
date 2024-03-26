import rules.hrot.HROT
import rules.hrot.HROTGenerations
import rules.nontotalistic.rules.INT
import search.cfind.CFind
import search.cfind.ShipSymmetry
import simulation.Coordinate


actual fun main() {
//    val lifeSearchP4K1 = CFind(
//        HROT("B3/S23"), 4, 1, 7, ShipSymmetry.ODD, verbosity = 1,
//        maxQueueSize = 2 shl 13
//    )
//    lifeSearchP4K1.search()

//    val vonNeumannSearch = CFind(
//        HROT("R2,C2,S2,B3,NN"), 4, 1, 6, ShipSymmetry.ODD, verbosity = 1,
//        maxQueueSize = 1 shl 20
//    )
//    vonNeumannSearch.search()

//    val factorioSearch = CFind(
//        HROT("R2,C2,S2,B3,NN"), 3, 1, 7, ShipSymmetry.GLIDE, verbosity = 1
//    )
//    factorioSearch.search()

//    val hashSearch = CFind(
//        HROT("R2,C2,S4-6,B5-6,N#"), 2, 1, 7, ShipSymmetry.EVEN,
//        verbosity = 1, numShips = 1
//    )
//    hashSearch.search()

    val minibugsSearch = CFind(
        HROT("R2,C2,S6-9,B7-8,NM"), 4, 1, 6, ShipSymmetry.ODD, verbosity = 1
    )
    minibugsSearch.search()

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