import rules.hrot.HROT
import search.cfind.CFind
import search.cfind.ShipSymmetry


actual fun main() {
    val lifeSearchP4K1 = CFind(
        HROT("B3/S23"), 4, 1, 7, ShipSymmetry.ODD, verbosity = 1, numShips = 1
    )
    lifeSearchP4K1.search()

    val minibugsSearch = CFind(
        HROT("R2,C2,S6-9,B7-9,NM"), 2, 1, 10, ShipSymmetry.GLIDE, verbosity = 1
    )
    minibugsSearch.search()
}