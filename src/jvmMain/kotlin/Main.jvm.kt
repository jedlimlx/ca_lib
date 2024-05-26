import it.skrape.core.htmlDocument
import it.skrape.fetcher.HttpFetcher
import it.skrape.fetcher.response
import it.skrape.fetcher.skrape
import it.skrape.selects.eachHref
import it.skrape.selects.html5.a
import it.skrape.selects.html5.body
import patterns.Spaceship
import patterns.gliderdb.GliderDB
import rules.hrot.HROT
import rules.hrot.HROTGenerations
import rules.nontotalistic.rules.INT
import simulation.DenseGrid
import simulation.SparseGrid
import simulation.Coordinate
import search.cfind.CFind
import search.cfind.ShipSymmetry
import search.cfind.SearchStrategy
import kotlin.random.Random

actual fun main() {
//     val gliderdb = GliderDB<HROTGenerations>(
//         skrape(HttpFetcher) {
//             request {
//                 url = "https://raw.githubusercontent.com/jedlimlx/gliderdb-reader/main/public/R1-C3-NM-gliders.db.txt"
//             }
//             response {
//                 htmlDocument { body { findFirst { text } } }
//             }
//         }
//     )
// //    println(gliderdb.searchByRule(HROTGenerations("/2/3")).map {
// //        "x = 0, y = 0, rule = ${it.ruleRange!!.minRule}\n${it.canonPhase}"
// //    }.joinToString("\n\n"))

//     val rules = skrape(HttpFetcher) {
//         request { url = "https://catagolue.hatsya.com/rules/generations" }
//         response { htmlDocument { a { findAll { eachHref } } } }
//     }.filter { Regex("/census/g3b[0-8]+s[0-8]+").matches(it) }.map { it.split("/").last() }
//     for (rulestring in rules) {
//         val rule = HROTGenerations(rulestring)
//         val symmetries = skrape(HttpFetcher) {
//             request { url = "https://catagolue.hatsya.com/census/$rulestring" }
//             response { htmlDocument { a { findAll { eachHref } } } }
//         }.filter { "/census/$rulestring" in it }.map { it.split("/").last() }

//         for (symmetry in symmetries) {
//             val list: List<String> = skrape(HttpFetcher) {
//                 request { url = "https://catagolue.hatsya.com/census/$rulestring/$symmetry" }
//                 response { htmlDocument { a { findAll { eachHref } } } }
//             }.filter { Regex("/census/$rulestring/$symmetry/xq[0-9]+").containsMatchIn(it) }

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
//                 it.map { DenseGrid(rule=rule, pattern=it).identify() as Spaceship }
//             }.flatten()

//             println("Checking $rulestring...")
//             println("-".repeat(30))

//             val smallerDB = gliderdb.searchByRule(rule)
//             smallerDB.forEach { println("$it, ${it.ruleRange}") }
//             println()

//             for (ship in ships) {
//                 val output = smallerDB.checkRedundant(ship)
//                 if (output.isEmpty()) {
//                     println("Added $ship, ${ship.ruleRange}")
//                     gliderdb.add(ship)
//                     smallerDB.add(ship)
//                 }
//             }

//             println()
//         }
//     }

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

    // val transitions: MutableList<List<Int>> = arrayListOf()
    // val weights = arrayOf(3, 3, 3, 3, 3, 3, 3, 3, 2, 2, 2, 2)
    // for (i in 0..<(1 shl 12)) {
    //     val string = i.toString(2).padStart(12, '0')
    //     val cells = string.map { it.digitToInt() }

    //     val sum = cells.mapIndexed { index, it -> weights[index] * it }.sum()
    //     if (sum in 9 .. 11 || sum == 4)
    //         transitions.add(cells)
    // }

    // println(R2VonNeumannINT(transitions).transitionString)

    // B2-ei3cjkr4cektyz5-cnr6-ik78/S01e2-ae3cnqry4cqrtwyz5-ain6ekn7e
    // B2ac3anr4-ijkz5cjkry6-cn7c8/S12i3aejy4nqtw5ceny6-kn7c
    // HROT("R2,C2,S6-11,B4,9-11,NW0020003330230320333000200")
//   val search = CFind(
//       HROT("R2,C2,S7-10,B7-8,NW1111111111111111111111111"), 2, 1, 8, ShipSymmetry.ASYMMETRIC,
//       verbosity = 1, searchStrategy = SearchStrategy.HYBRID_BFS, numThreads = 8,
//       //direction = Coordinate(1, 1), lookaheadDepth = 3
//   )
    //R2,C2,S6-9,B7-8,NM
    val search = CFind(
        INT("B2n3/S23-q"), 3, 1, 12, ShipSymmetry.ODD,
        verbosity = 1, searchStrategy = SearchStrategy.PRIORITY_QUEUE, numShips = 1
        //numThreads = 8, direction = Coordinate(1, 1), lookaheadDepth = 3
    )

    search.search()
}