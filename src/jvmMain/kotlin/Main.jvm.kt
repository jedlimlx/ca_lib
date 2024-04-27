import rules.hrot.HROT
import rules.hrot.HROTGenerations
import rules.nontotalistic.rules.INT
import search.cfind.CFind
import search.cfind.SearchStrategy
import search.cfind.ShipSymmetry

import it.skrape.core.*
import it.skrape.fetcher.*
import it.skrape.selects.*
import it.skrape.selects.html5.*

import simulation.Coordinate
import simulation.DenseGrid
import patterns.Spaceship
import patterns.gliderdb.GliderDB
import rules.hrot.HROTExtendedGenerations
import rules.ruleloader.Ruletable
import rules.ruleloader.builders.RuletableBuilder
import rules.ruleloader.builders.ruletable
import java.io.File

actual fun main() {
    // val rule = HROT("R2,C2,S6-9,14-20,B7-8,15-24,NM")
    // val rulestring = "r2bffc0c0s0fe1e0"
    // val symmetry = "cfind_stdin"

    // val list: List<String> = skrape(HttpFetcher) {
    //     request {
    //         url = "https://catagolue.hatsya.com/census/$rulestring/$symmetry"
    //     }
    //     response {
    //         htmlDocument {
    //             a {
    //                 findAll {
    //                     eachHref
    //                 }
    //             }
    //         }
    //     }
    // }

    // val output = list.filter { Regex("/census/$rulestring/$symmetry/(.*?)").containsMatchIn(it) }.map {
    //     "https://catagolue.hatsya.com/textcensus/$rulestring/$symmetry/" + it.split("/").last()
    // }.map {
    //     skrape(HttpFetcher) {
    //         request { url = it }
    //         response { htmlDocument { body { findFirst { text } } } }
    //     }.split(" ")
    // }.map {
    //     it.subList(1, it.size).map { it.split(",").first().replace("\"", "") }.filter { it[0] == 'x' }
    // }.map {
    //     it.map {
    //         DenseGrid(rule=rule, pattern=it).identify() as Spaceship
    //     }
    // }.flatten().filter { it.dx != 0 || it.dy != 0 }

    // val gliderdb = GliderDB(output)
    // println(gliderdb.searchByRule(HROT("R2,C2,S6-9,B7-8,NM")..HROT("R2,C2,S6-9,14-24,B7-8,NM")))

    // val output = skrape(HttpFetcher) {
    //     request {
    //         url = "https://raw.githubusercontent.com/jedlimlx/gliderdb-reader/main/src/assets/R1-C3-NM-gliders.db.txt"
    //     }
    //     response {
    //         htmlDocument { body { findFirst { text } } }
    //     }
    // }
    
    // val gliderdb = GliderDB(output)
    // println(gliderdb.searchByRule(HROTGenerations("/2/3")..HROTGenerations("012345678/2345678/3")).map {
    //     "x = 0, y = 0, rule = ${it.ruleRange!!.first}\n${it.canonPhase}"
    // }.joinToString("\n\n"))

//    val search = CFind(
//        HROT("R2,C2,S6-9,14-20,B7-8,15-24,NM"), 4, 3, 8, symmetry = ShipSymmetry.ODD,
//        verbosity = 1, searchStrategy = SearchStrategy.PRIORITY_QUEUE, partialFrequency = 1000,
//        backupName = "backups/minibugs_2c3o_w11_even", maxQueueSize = 1 shl 22, //stdin = true,
//        backupFrequency = 10
//    )
//    search.loadState(File("/mnt/c/users/jedli/Documents/CA/cfind_dumps/minibugs_variant_3c4o_w8_odd.txt").readText())
//    search.displayPartials()
    //search.search()

    val ruletable = ruletable {
        name = "Test"

        tree(numStates = 2, neighbourhood = moore(1), background = intArrayOf(0, 1)) { neighbourhood, state ->
            val sum = neighbourhood.sum()
            when {
                state == 0 && sum in setOf(0, 1, 5) -> 1
                state == 1 && sum in setOf(2, 3) -> 1
                else -> 0
            }
        }
    }.toString()

    val search = CFind(
        HROT("R2,C2,S6-9,B7-8,NM"), 2, 1, 8,
        ShipSymmetry.ASYMMETRIC, verbosity = 1, searchStrategy = SearchStrategy.HYBRID_BFS, numShips = 1
    )
    search.search()

//    val search = CFind(
//        HROT("R2,C2,S9-14,B9-14,16,NW0010003330130310333000100"), 3, 1, 4, symmetry = ShipSymmetry.ODD,
//        verbosity = 1, searchStrategy = SearchStrategy.PRIORITY_QUEUE, partialFrequency = 1000,
//        backupName = "dump", maxQueueSize = 1 shl 22, numThreads = 1, direction = Coordinate(1, 1),
//        backupFrequency = 600, lookaheadDepth = 0
//    )
//    search.search()

//    val search = CFind(
//        HROT("R2,C2,S6-9,B7-8,NM"), 3, 2, 11, symmetry = ShipSymmetry.EVEN,
//        verbosity = 1, searchStrategy = SearchStrategy.PRIORITY_QUEUE, partialFrequency = 10,
//        backupName = "backups/minibugs_2c3o_w11_even", maxQueueSize = 1 shl 22, stdin = true,
//        backupFrequency = 60*60
//    )
//    search.loadState(File("backups/minibugs_2c3o_w11_even_2.txt").readText())
//    search.search()
}