package search

import rules.hrot.HROT
import rules.hrot.HROTExtendedGenerations
import rules.hrot.HROTGenerations
import rules.nontotalistic.rules.INT
import search.cfind.CFind
import search.cfind.SearchStrategy
import search.cfind.ShipSymmetry
import simulation.Coordinate
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

class Test {
    private val searchStrategies = listOf(SearchStrategy.HYBRID_BFS, SearchStrategy.PRIORITY_QUEUE)

    // Test different neighbourhoods
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
                HROT("B3/S23"), 2, 1, 5, ShipSymmetry.GLIDE, verbosity = 1,
                searchStrategy = strategy
            )
            lifeSearchP4K2.search()

            assertEquals(lifeSearchP4K2.searchResults.size, 1)
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
    fun farCornersTest() {
        for (strategy in searchStrategies) {
            val farCorners = CFind(
                HROT("R2,C2,S2-3,B3,N@891891"), 3, 1, 4, ShipSymmetry.EVEN,
                verbosity = 1, searchStrategy = strategy, numShips = 2
            )
            farCorners.search()

            assertEquals(farCorners.searchResults.size, 2)
        }
    }

    @Test
    fun farEdgesTest() {
        // TODO Figure out why no ships are found at width 4
        for (strategy in searchStrategies) {
            val farEdges = CFind(
                HROT("R3,C2,S2-3,B3,N@1000a4250008"), 2, 1, 5, ShipSymmetry.ODD,
                verbosity = 1, searchStrategy = strategy
            )
            farEdges.search()

            assertEquals(farEdges.searchResults.size, 3)
        }
    }

    // Test different rulespaces
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
    fun intTest() {
        for (strategy in searchStrategies) {
            // Checking it works for isotropic non-totalistic rules
            val search = CFind(
                INT("B2n3/S23-q"), 3, 1, 11, symmetry = ShipSymmetry.ASYMMETRIC,
                verbosity = 1, searchStrategy = strategy, numShips = 1
            )
            search.search()

            assertEquals(search.searchResults.size, 1)

            val search2 = CFind(
                INT("B2-ik3ak4-eir5-y678/S2-ce3-jry4-ackwy5-i6-a78"), 5, 4, 4,
                symmetry = ShipSymmetry.ODD, verbosity = 1, searchStrategy = SearchStrategy.HYBRID_BFS
            )
            search2.search()

            assertEquals(search2.searchResults.size, 1)
        }
    }

    @Test
    fun extendedGenerationsTest() {
        for (strategy in searchStrategies) {
            // Checking it works for extended generations rules
            val search = CFind(
                HROTExtendedGenerations("1/3/2-1"), 6, 1, 5, symmetry = ShipSymmetry.ODD,
                verbosity = 1, searchStrategy = strategy
            )
            search.search()

            assertEquals(search.searchResults.size, 3)
        }
    }

    @Test
    fun strobingTest() {
        for (strategy in searchStrategies) {
            // Checking it works for strobing rules for 2-states or n-states
            val search = CFind(
                HROT("R2,C2,S3-4,6,B0-7,N+"), 4, 1, 4, symmetry = ShipSymmetry.ODD,
                verbosity = 1, searchStrategy = strategy, numShips = 1
            )
            search.search()

            assertEquals(search.searchResults.size, 1)

            val search2 = CFind(
                HROTGenerations("01246/0134/3"), 3, 2, 6, symmetry = ShipSymmetry.EVEN,
                verbosity = 1, searchStrategy = strategy
            )
            search2.search()

            assertEquals(search2.searchResults.size, 1)
        }

    }

    // Miscellaneous tests
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
    fun diagonalTest() {
        for (strategy in searchStrategies) {
            val diagonalSearch = CFind(
                HROT("B34/S34"), 3, 1, 3, ShipSymmetry.ASYMMETRIC,
                verbosity = 1, direction = Coordinate(1, 1), searchStrategy = strategy
            )
            diagonalSearch.search()

            assertEquals(diagonalSearch.searchResults.size, 2)

            val glideDiagonalSearch = CFind(
                HROT("B3/S23"), 4, 1, 2, ShipSymmetry.GLIDE,
                verbosity = 1, direction = Coordinate(1, 1), searchStrategy = strategy, numShips = 1
            )
            glideDiagonalSearch.search()

            assertEquals(glideDiagonalSearch.searchResults.size, 1)

            val p2search = CFind(
                HROT("R2,C2,S2,B3,NN"), 2, 1, 6, ShipSymmetry.ODD,
                verbosity = 1, searchStrategy = strategy, direction = Coordinate(1 ,1)
            )
            p2search.search()
            assertEquals(p2search.searchResults.size, 21)
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

    @Test
    fun photonTest() {
        for (strategy in searchStrategies) {
            val photonSearch = CFind(
                HROT("R2,C2,S3,B3,NN"), 1, 1, 3, ShipSymmetry.ODD,
                verbosity = 1, searchStrategy = strategy
            )
            photonSearch.search()

            assertEquals(photonSearch.searchResults.size, 1)

            val seedsSearch = CFind(
                HROT("B2/S"), 1, 1, 4, ShipSymmetry.ASYMMETRIC,
                verbosity = 1, searchStrategy = strategy
            )
            seedsSearch.search()

            assertEquals(seedsSearch.searchResults.size, 2)
        }
    }

    @Test
    fun oneDimensionalTest() {
        for (strategy in searchStrategies) {
            val oneDimensionalSearch = CFind(
                HROT("R3,C2,S2,B2,5-6,N+"),
                10, 13, 1, ShipSymmetry.ASYMMETRIC,
                verbosity = 1, searchStrategy = strategy
            )
            oneDimensionalSearch.search()

            assertEquals(oneDimensionalSearch.searchResults.size, 1)
        }
    }

    @Test
    fun obliqueTest() {
        for (strategy in searchStrategies) {
            val obliqueSearch = CFind(
                INT("B2-ei3cjkr4cektyz5-cnr6-ik78/S01e2-ae3cnqry4-aeijkn5-ain6ekn7e"),
                3, 1, 4, ShipSymmetry.ASYMMETRIC,
                verbosity = 1, searchStrategy = strategy, direction = Coordinate(2, 1)
            )
            obliqueSearch.search()

            assertEquals(obliqueSearch.searchResults.size, 1)
        }
    }
}