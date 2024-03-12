import rules.hrot.HROT
import rules.hrot.HROTGenerations
import rules.nontotalistic.rules.INT
import search.cfind.CFind
import search.cfind.ShipSymmetry
import simulation.Coordinate


actual fun main() {
    val glideDiagonalSearch = CFind(
        INT("B2n3/S23-q"), 3, 1, 13, ShipSymmetry.GLIDE,
        verbosity = 1, maxQueueSize = 2 shl 18
    )
    glideDiagonalSearch.search()
}