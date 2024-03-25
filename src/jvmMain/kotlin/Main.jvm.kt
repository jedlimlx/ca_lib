import rules.hrot.HROT
import rules.hrot.HROTGenerations
import rules.nontotalistic.rules.INT
import search.cfind.CFind
import search.cfind.ShipSymmetry
import simulation.Coordinate


actual fun main() {
//    fun temp(r: Int, neighbourhood: Array<Coordinate>) {
//        for (birth in 2 .. neighbourhood.count { it.y < 0 }) {
//            print("B$birth: ")
//
//            var bound = 10000.0
//            var nMin = 0
//            for (n in 1 .. 20) {
//                val numbers = neighbourhood.map { it.x + n * it.y }.sorted()
//                val test = -numbers[birth-1]/(1+n).toDouble()
//                if (test < bound) {
//                    bound = test
//                    nMin = n
//                }
//            }
//
//            val numbers = neighbourhood.map { it.x + nMin * it.y }.sorted()
//
//            val m = -numbers[birth-1]
//            if (m > nMin * r)
//                println("(${m-nMin*r},${r*nMin})c/${nMin}, ${m}c/${1+nMin}d")
//            else println("${m}c/${nMin}o, ${m}c/${1+nMin}d")
//        }
//    }
//
//    for (r in 1 .. 4) {
//        println("R$r Moore")
//        println("----------")
//        val neighbourhood = moore(r)
//        temp(r, neighbourhood)
//        println()
//    }
//
//    println("===================================\n")
//
//    for (r in 1 .. 4) {
//        println("R$r Von Neumann")
//        println("----------")
//        val neighbourhood = vonNeumann(r)
//        temp(r, neighbourhood)
//        println()
//    }
//
//    println("===================================\n")
//
//    for (r in 1 .. 4) {
//        println("R$r Circular")
//        println("----------")
//        val neighbourhood = circular(r)
//        temp(r, neighbourhood)
//        println()
//    }
//
//    println("===================================\n")
//
//    for (r in 1 .. 4) {
//        println("R$r Euclidean")
//        println("----------")
//        val neighbourhood = euclidean(r)
//        temp(r, neighbourhood)
//        println()
//    }
//
//    println("===================================\n")
//
//    for (r in 1 .. 4) {
//        println("R$r Cross")
//        println("----------")
//        val neighbourhood = cross(r)
//        temp(r, neighbourhood)
//        println()
//    }

//    val lifeSearchP4K1 = CFind(
//        HROT("B3/S23"), 4, 1, 7, ShipSymmetry.ODD, verbosity = 1,
//        maxQueueSize = 2 shl 13
//    )
//    lifeSearchP4K1.search()

    val vonNeumannSearch = CFind(
        HROT("R2,C2,S2,B3,NN"), 3, 1, 8, ShipSymmetry.ODD, verbosity = 1
    )
    vonNeumannSearch.search()

//    val factorioSearch = CFind(
//        HROT("R2,C2,S2,B3,NN"), 3, 1, 7, ShipSymmetry.GLIDE, verbosity = 1
//    )
//    factorioSearch.search()

//    val hashSearch = CFind(
//        HROT("R2,C2,S4-6,B5-6,N#"), 2, 1, 7, ShipSymmetry.EVEN,
//        verbosity = 1, numShips = 1
//    )
//    hashSearch.search()

//    val minibugsSearch = CFind(
//        HROT("R2,C2,S6-9,B7-8,NM"), 5, 1, 4, ShipSymmetry.ODD, verbosity = 1
//    )
//    minibugsSearch.search()

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