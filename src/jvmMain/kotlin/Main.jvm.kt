import rules.hrot.HROT
import search.cfind.CFind
import search.cfind.ShipSymmetry


actual fun main() {
    val minibugsSearch = CFind(
    HROT("R2,C2,S6-9,B7-8,NM"), 3, 1, 7, ShipSymmetry.ODD
)
    minibugsSearch.search()
}