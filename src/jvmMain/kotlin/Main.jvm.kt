import rules.hrot.HROTGenerations
import search.cfind.CFind
import search.cfind.ShipSymmetry


actual fun main() {
    val generationsSearch = CFind(
        HROTGenerations("23/3/3"), 5, 1, 8, ShipSymmetry.EVEN, verbosity = 1
    )
    generationsSearch.search()
}