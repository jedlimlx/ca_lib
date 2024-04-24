import rules.hrot.HROT
import rules.hrot.HROTGenerations
import rules.hrot.HROTExtendedGenerations
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

    val search = CFind(
        INT("B2n3/S23-q"), 3, 1, 15, symmetry = ShipSymmetry.GLIDE,
        verbosity = 1, searchStrategy = SearchStrategy.PRIORITY_QUEUE, partialFrequency = 1000,
        backupName = "dump", maxQueueSize = 1 shl 22, numThreads = 2
    )
    search.search()
}