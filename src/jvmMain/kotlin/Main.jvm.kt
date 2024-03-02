import rules.hrot.HROT
import search.cfind.CFind
import search.cfind.ShipSymmetry

actual fun main() {
    val circularSearchP2K1 = CFind(
        HROT("R2,C2,S5-8,B6-7,NC"), 2, 1, 7, ShipSymmetry.EVEN
    )
    circularSearchP2K1.search()
}