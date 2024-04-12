package search

import rules.hrot.HROT
import rules.hrot.HROTGenerations
import search.cfind.CFind
import search.cfind.SearchStrategy
import search.cfind.ShipSymmetry
import simulation.Coordinate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.TimeSource

class Test {
    val searchStrategies = listOf(SearchStrategy.HYBRID_BFS, SearchStrategy.PRIORITY_QUEUE)

    @Test
    fun factorioTest() {
        for (strategy in searchStrategies) {
            // Checking it works for factorio and cross rules more generally
            val factorioSearchP2K1 = CFind(
                HROT("R3,C2,S2,B3,N+"), 2, 1, 3, ShipSymmetry.ODD,
                verbosity = 1, searchStrategy = strategy
            )
            factorioSearchP2K1.search()

            assertEquals(factorioSearchP2K1.searchResults.size, 1)
        }
    }

    @Test
    fun vonNeumannTest() {
        for (strategy in searchStrategies) {
            // Checking it works for von neumann rules
            val almostLifeP3K1 = CFind(
                HROT("R2,C2,S6-11,B9-11,NW0010003330130310333000100"), 3, 1, 6, ShipSymmetry.EVEN,
                verbosity = 1, searchStrategy = strategy
            )
            almostLifeP3K1.search()

            assertEquals(almostLifeP3K1.searchResults.size, 1)
        }
    }

    @Test
    fun generationsTest() {
        for (strategy in searchStrategies) {
            // Checking it works for generations rules
            val generationsSearch = CFind(
                HROTGenerations("23/34/3"), 2, 1, 6, ShipSymmetry.ODD, verbosity = 1,
                searchStrategy = strategy
            )
            generationsSearch.search()

            assertEquals(generationsSearch.searchResults.size, 3)
        }
    }

    @Test
    fun lifeTest() {
        for (strategy in searchStrategies) {
            // Finding the c/3o turtle
            val lifeSearchP3K1 = CFind(
                HROT("B3/S23"), 3, 1, 6, ShipSymmetry.EVEN, verbosity = 1,
                searchStrategy = strategy
            )
            lifeSearchP3K1.search()

            assertEquals(lifeSearchP3K1.searchResults.size, 1)

            // Finding the LWSS
            val lifeSearchP4K2 = CFind(
                HROT("B3/S23"), 2, 1, 7, ShipSymmetry.GLIDE, verbosity = 1,
                searchStrategy = strategy
            )
            lifeSearchP4K2.search()

            assertEquals(lifeSearchP4K2.searchResults.size, 2)
        }
    }

    @Test
    fun dfsTest() {
        for (strategy in searchStrategies) {
            val lifeSearchP4K1 = CFind(
                HROT("B3/S23"), 4, 1, 7, ShipSymmetry.ODD, verbosity = 1, numShips = 1,
                maxQueueSize = 2 shl 13, searchStrategy = strategy
            )
            lifeSearchP4K1.search()

            assertEquals(lifeSearchP4K1.searchResults.size, 1)
        }
    }

    @Test
    fun diagonalTest() {  // TODO get diagonal searches working for other neighbourhoods
        for (strategy in searchStrategies) {
//            val diagonalSearch = CFind(  TODO figure out why this test keeps giving inconsistent results
//                HROT("B34/S34"), 3, 1, 3, ShipSymmetry.ASYMMETRIC,
//                verbosity = 1, direction = Coordinate(1, 1), searchStrategy = strategy
//            )
//            diagonalSearch.search()
//
//            assertEquals(diagonalSearch.searchResults.size, 2)

            val glideDiagonalSearch = CFind(
                HROT("B3/S23"), 4, 1, 2, ShipSymmetry.GLIDE,
                verbosity = 1, direction = Coordinate(1, 1), searchStrategy = strategy
            )
            glideDiagonalSearch.search()

            assertEquals(glideDiagonalSearch.searchResults.size, 1)
        }
    }

    @Test
    fun hashTest() {
        for (strategy in searchStrategies) {
            val hashSearch = CFind(
                HROT("R2,C2,S4-6,B5-6,N#"), 2, 1, 7, ShipSymmetry.EVEN,
                verbosity = 1, numShips = 1, searchStrategy = strategy
            )
            hashSearch.search()

            assertEquals(hashSearch.searchResults.size, 1)
        }
    }

    @Test
    fun superluminalTest() {
        for (strategy in searchStrategies) {
            val superluminalSearch = CFind(
                HROT("R2,C2,S4,B4,NM"), 2, 3, 5, ShipSymmetry.EVEN,
                verbosity = 1, searchStrategy = strategy
            )
            superluminalSearch.search()

            assertEquals(superluminalSearch.searchResults.size, 1)

            val superluminalGlideSearch = CFind(
                HROT("R2,C2,S4,B4,NM"), 2, 3, 6, ShipSymmetry.GLIDE,
                verbosity = 1, searchStrategy = strategy
            )
            superluminalGlideSearch.search()

            assertEquals(superluminalSearch.searchResults.size, 1)
        }
    }
}