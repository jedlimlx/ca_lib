package search

import rules.hrot.HROT
import rules.hrot.HROTGenerations
import search.cfind.CFind
import search.cfind.ShipSymmetry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.TimeSource

class Test {
    @Test
    fun factorioTest() {
        // Checking it works for factorio and cross rules more generally
        val factorioSearchP2K1 = CFind(
            HROT("R3,C2,S2,B3,N+"), 2, 1, 3, ShipSymmetry.ODD, verbosity = 1
        )
        factorioSearchP2K1.search()

        assertEquals(factorioSearchP2K1.searchResults.size, 1)
    }

    @Test
    fun vonNeumannTest() {
        // Checking it works for von neumann rules
        val almostLifeP3K1 = CFind(
            HROT("R2,C2,S6-11,B9-11,NW0010003330130310333000100"), 3, 1, 6, ShipSymmetry.EVEN, verbosity = 1
        )
        almostLifeP3K1.search()

        assertEquals(almostLifeP3K1.searchResults.size, 1)
    }

    @Test
    fun generationsTest() {
        // Checking it works for generations rules
        val generationsSearch = CFind(
            HROTGenerations("23/34/3"), 2, 1, 6, ShipSymmetry.ODD, verbosity = 1
        )
        generationsSearch.search()

        assertEquals(generationsSearch.searchResults.size, 3)
    }

    @Test
    fun lifeTest() {
        // Finding the c/3o turtle
        val lifeSearchP3K1 = CFind(
            HROT("B3/S23"), 3, 1, 6, ShipSymmetry.EVEN, verbosity = 1
        )
        lifeSearchP3K1.search()

        assertEquals(lifeSearchP3K1.searchResults.size, 1)
    }
}