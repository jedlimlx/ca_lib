import it.skrape.core.*
import it.skrape.fetcher.*
import it.skrape.selects.*
import it.skrape.selects.html5.*
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.writeString
import patterns.Spaceship
import rules.hrot.HROT
import rules.hrot.HROTGenerations
import rules.nontotalistic.rules.DeficientINT
import rules.nontotalistic.rules.INT
import rules.nontotalistic.rules.INTGenerations
import rules.nontotalistic.transitions.R2VonNeumannINT
import rules.ruleloader.builders.ruletable
import rules.ruleloader.ruletableFromFile
import rules.ruleloader.ruletree.ruletreeDirectiveFromString
import search.cfind.CFind
import search.cfind.SearchStrategy
import search.cfind.ShipSymmetry
import simulation.*
import java.io.File
import kotlin.random.Random
import patterns.Oscillator
import rules.hrot.HROTExtendedGenerations

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

    // val pattern = "b3o\$2o2bo\$2o2bo\$b4o\$2b2o9\$2b2o\$b4o\$2o2bo\$2o2bo\$b3o!"
    // val ship = SparseGrid("b3o\$2o2bo\$2o2bo\$b4o\$2b2o!", HROT("R2,C2,S6-9,B7-8,NM")).identify()!!

    // var count = 0
    // val range = ship.ruleRange!!.first as HROT .. ship.ruleRange!!.second as HROT
    // //println("${range.size} rules to search.")
    // for (i in range.randomSequence()) {
    //     val test = DenseGrid(pattern, i)
    //     val output = test.identify(200)
    //     if (output != null && (output as Spaceship).period > 1)
    //         println("$i, ${output}")
        
    //     if ((count++).mod(1000) == 0)
    //         println("Searched $count...")
    // }

    val ruletable = ruletableFromFile("SoManyShips3.rule")

    // B2-ei3cjkr4cektyz5-cnr6-ik78/S01e2-ae3cnqry4cqrtwyz5-ain6ekn7e
    // B2ac3anr4-ijkz5cjkry6-cn7c8/S12i3aejy4nqtw5ceny6-kn7c
    // HROT("R2,C2,S6-11,B4,9-11,NW0020003330230320333000200")
//    val search = CFind(
//        HROT("R2,C2,S2,B3,NN"), 4, 2, 5, ShipSymmetry.ASYMMETRIC,
//        verbosity = 1, searchStrategy = SearchStrategy.HYBRID_BFS, numThreads = 8,
//        //direction = Coordinate(1, 1), lookaheadDepth = 3, numShips = 30
//    )
//    search.search()

    val search = CFind(
        HROTGenerations("134/24/3"), 4, 3, 19, symmetry = ShipSymmetry.ODD,
        verbosity = 1, searchStrategy = SearchStrategy.PRIORITY_QUEUE, lookaheadDepth = 1
    )
    search.search()
}