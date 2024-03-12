import rules.hrot.HROT
import search.cfind.CFind
import search.cfind.ShipSymmetry
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.time.TimeSource

class Test {
    @Test
    @Ignore
    fun benchmark() {
        val timeSource = TimeSource.Monotonic

        var startTime = timeSource.markNow()
        val almostLifeP4K1 = CFind(
            HROT("R2,C2,S6-11,B9-11,NW0010003330130310333000100"), 4, 1, 5, ShipSymmetry.EVEN, verbosity = -1
        )
        almostLifeP4K1.search()
        println("AlmostLife, c/4o, width 5, even: ${(timeSource.markNow() - startTime).inWholeMilliseconds / 1000.0}s")

        assert(almostLifeP4K1.searchResults.size == 0)

        startTime = timeSource.markNow()
        val lifeSearchP4K1 = CFind(
            HROT("B3/S23"), 4, 1, 7, ShipSymmetry.ODD, verbosity = -1, numShips = 1
        )
        lifeSearchP4K1.search()
        println("B3/S23, c/4o, width 7, odd: ${(timeSource.markNow() - startTime).inWholeMilliseconds / 1000.0}s")

        assert(lifeSearchP4K1.searchResults.size == 1)

        startTime = timeSource.markNow()
        val circularSearchP2K1 = CFind(
            HROT("R2,C2,S5-8,B6-7,NC"), 2, 1, 7, ShipSymmetry.EVEN, verbosity = -1
        )
        circularSearchP2K1.search()
        println("R2,C2,S5-8,B6-7,NC, c/2o, width 7, even: ${(timeSource.markNow() - startTime).inWholeMilliseconds / 1000.0}s")

        assert(circularSearchP2K1.searchResults.size == 2)

        startTime = timeSource.markNow()

        val minibugsSearch = CFind(
            HROT("R2,C2,S6-9,B7-8,NM"), 2, 1, 8, ShipSymmetry.ASYMMETRIC, verbosity = -1, numShips = 1
        )
        minibugsSearch.search()

        assert(minibugsSearch.searchResults.size == 1)

        println("Minibugs, c/2o, width 8, asymmetric: ${(timeSource.markNow() - startTime).inWholeMilliseconds / 1000.0}s")
    }
}