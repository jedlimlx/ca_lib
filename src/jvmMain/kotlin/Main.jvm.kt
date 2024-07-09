import com.github.ajalt.mordant.rendering.AnsiLevel
import com.github.ajalt.mordant.terminal.Terminal
import it.skrape.core.htmlDocument
import it.skrape.fetcher.HttpFetcher
import it.skrape.fetcher.response
import it.skrape.fetcher.skrape
import it.skrape.selects.eachHref
import it.skrape.selects.html5.a
import it.skrape.selects.html5.body
import patterns.Spaceship
import patterns.gliderdb.GliderDB
import rules.hrot.HROTGenerations
import simulation.DenseGrid
import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.rendering.TextStyles.*
import rules.hrot.HROT
import rules.nontotalistic.rules.INT
import rules.nontotalistic.rules.INTGenerations
import rules.ruleloader.ruletableFromFile
import search.cfind.CFind
import search.cfind.SearchStrategy
import search.cfind.ShipSymmetry
import simulation.Coordinate
import simulation.SparseGrid
import java.io.File

actual fun main() {
//    val t = Terminal(interactive = true, ansiLevel = AnsiLevel.TRUECOLOR)
//
//    val gliderdb = GliderDB<HROTGenerations>(
//        skrape(HttpFetcher) {
//            request {
//                url = "https://raw.githubusercontent.com/jedlimlx/HROT-Glider-DB/master/R1-C3-NM-gliders.db.txt"
//            }
//            response {
//                htmlDocument { body { findFirst { text } } }
//            }
//        }
//    )
//
//    File("new-gliders.txt").writeText(gliderdb.toString())

//     val rules = skrape(HttpFetcher) {
//         request { url = "https://catagolue.hatsya.com/rules/ltl" }
//         response { htmlDocument { a { findAll { eachHref } } } }
//     }.filter { Regex("/census/r2b(.*?)").matches(it) }.map { it.split("/").last() } +
//     skrape(HttpFetcher) {
//         request { url = "https://catagolue.hatsya.com/rules/hrot" }
//         response { htmlDocument { a { findAll { eachHref } } } }
//     }.filter { Regex("/census/r2b(.*?)").matches(it) }.map { it.split("/").last() }
//     for (rulestring in rules) {
//         if (rulestring == "r2b4t4s10t17") // && ship.period == 119)
//             continue
//
//         val rule = HROT(rulestring)
//         val symmetries = skrape(HttpFetcher) {
//             request { url = "https://catagolue.hatsya.com/census/$rulestring" }
//             response { htmlDocument { a { findAll { eachHref } } } }
//         }.filter { "/census/$rulestring" in it }.map { it.split("/").last() }.filter {
//             "r2" !in it
//         }
//
//         t.println(bold(red("Checking $rulestring...")))
//         t.println(bold(red("-".repeat(30))))
//
//         val smallerDB = gliderdb.searchByRule(rule).searchByArea(1000000)
//         val originalShips = smallerDB.lst.toSet()
//         smallerDB.forEach { println("$it, ${it.ruleRange}") }
//         println()
//
//         for (symmetry in symmetries) {
//             val list: List<String> = skrape(HttpFetcher) {
//                 request { url = "https://catagolue.hatsya.com/census/$rulestring/$symmetry" }
//                 response { htmlDocument { a { findAll { eachHref } } } }
//             }.filter { Regex("/census/$rulestring/$symmetry/xq[0-9]+").containsMatchIn(it) }
//
//             val ships = list.map {
//                 "https://catagolue.hatsya.com/textcensus/$rulestring/$symmetry/" + it.split("/").last()
//             }.map {
//                 skrape(HttpFetcher) {
//                     request { url = it }
//                     response { htmlDocument { body { findFirst { text } } } }
//                 }.split(" ")
//             }.map {
//                 it.subList(1, it.size).map {
//                     it.split(",").first().replace("\"", "")
//                 }.filter { it[0] == 'x' }
//             }.map {
//                 it.map {
//                     val temp = SparseGrid(rule=rule, pattern=it)
//                     if (temp.bounds.area < 15000) temp.identify() as Spaceship
//                     else null
//                 }.filter { it != null }.map { it!! }
//             }.flatten()
//
//             val newShips = HashSet<Spaceship>()
//             val removedShips = HashSet<Spaceship>()
//             for (ship in ships) {
//                 val output = smallerDB.checkRedundant(ship)
//
//                 if (output.isEmpty()) {
//                     gliderdb.add(ship)
//                     smallerDB.add(ship)
//                     newShips.add(ship)
//                     //println("added $ship, ${ship.ruleRange}, ${ship.canonPhase.toRLE(maxLineLength = Int.MAX_VALUE)}")
//                 } else {
//                     var added = false
//                     output.map { (redundant, it) ->
//                         // if (redundant in newShips)
//                         //     println("removed $redundant, $it, " +
//                         //             "${redundant.canonPhase.toRLE(maxLineLength = Int.MAX_VALUE)}, " +
//                         //             "${it.canonPhase.toRLE(maxLineLength = Int.MAX_VALUE)} ${it in gliderdb.lst}")
//                         //if (redundant == ship) added = true
//
//                         smallerDB.lst.remove(redundant)
//                         gliderdb.lst.remove(redundant)
//                         removedShips.add(redundant)
//                         newShips.remove(redundant)
//
//                         if (added) return@map
//                         if (it == ship) {
//                             gliderdb.add(ship)
//                             smallerDB.add(ship)
//                             newShips.add(ship)
//                             removedShips.remove(ship)
//                             // println("added $ship, ${ship.canonPhase.toRLE(maxLineLength = Int.MAX_VALUE)}")
//
//                             added = true
//                         }
//                     }
//                 }
//             }
//
//             // Printing out ships that were removed and added
//             for (ship in newShips) {
//                 println(bold("Added $ship, ${ship.ruleRange} " +
//                         "from https://catagolue.hatsya.com/census/$rulestring/$symmetry"))
//                 println("x = 0, y = 0, rule = $rule\n${ship.canonPhase}\n")
//             }
//
//             for (ship in removedShips) {
//                 if (ship in originalShips) {
//                     println(bold("Removed $ship, ${ship.ruleRange}"))
//                     println("x = 0, y = 0, rule = $rule\n${ship.canonPhase}\n")
//                 }
//             }
//         }
//     }
////
//     File("new-gliders.txt").writeText(gliderdb.toString())
//
//    val speeds = arrayListOf(Pair(1, 4)) //, Pair(2, 4))
//    val symmetries = listOf(ShipSymmetry.ODD, ShipSymmetry.ASYMMETRIC, ShipSymmetry.GLIDE)
//
//    for (rule in HROTGenerations("23/3/3")..HROTGenerations("0235678/345678/3")) {
//        t.println(red(bold("Searching for ships in $rule")))
//        t.println(red(bold("------------------------------------")))
//
//        for (speed in speeds) {
//            if (
//                gliderdb.searchByRule(rule as HROTGenerations).searchBySpeed(
//                    speed.first, 0, speed.second, higherPeriod = true
//                ).isNotEmpty()
//            )
//                continue
//
//            var shipFound = false
//            var impossible = true
//            for (width in 6 .. 10) {
//                if (shipFound) break
//                for (symmetry in symmetries) {
//                    val search = CFind(
//                        rule, speed.second, speed.first, width, symmetry,
//                        verbosity = -1, lookaheadDepth = 3, numShips = 1,
//                        transpositionTableSize = 1 shl 27,
//                        searchStrategy = SearchStrategy.HYBRID_BFS,
//                        direction = Coordinate(1, 1)
//                    )
//                    search.search()
//
//                    if (search.searchResults.isNotEmpty()) {
//                        println()
//                        println(bold("Ship found, works in ${green("${(search.searchResults[0] as Spaceship).ruleRange}")}!"))
//                        println(blue("x = 0, y = 0, rule = $rule"))
//                        println(blue((search.searchResults[0] as Spaceship).canonPhase.toString()))
//
//                        gliderdb.add((search.searchResults[0] as Spaceship).canonPhase.identify() as Spaceship)
//
//                        File("new-gliders-2.txt").writeText(gliderdb.toString())
//
//                        shipFound = true
//                        break
//                    } else {
//                        println(
//                            bold(
//                                "No ${green("${speed.first}c/${speed.second}")} ship found for " +
//                                "${green("$symmetry")} symmetry at width ${green("$width")}, " +
//                                "max depth ${green("${search.maxDepth}")}."
//                            )
//                        )
//
//                        if (search.maxDepth >= 2 * search.period + 1) impossible = false
//                    }
//                }
//
//                println()
//                if (impossible) break
//            }
//        }
//    }

    // INT("B2-ei3cjkr4cektyz5-cnr6-ik78/S01e2-ae3cnqry4-aeijkn5-ain6ekn7e"),

    val oblique = CFind(
        HROT("R2,C2,S2,B3,NN"), 2, 1, 9, ShipSymmetry.ODD,
        verbosity = 1, searchStrategy = SearchStrategy.PRIORITY_QUEUE
    )
    oblique.search()
}