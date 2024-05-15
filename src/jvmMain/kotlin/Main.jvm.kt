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

    //val ruletable = ruletableFromFile("SoManyShips3.rule")

    // B2-ei3cjkr4cektyz5-cnr6-ik78/S01e2-ae3cnqry4cqrtwyz5-ain6ekn7e
    // HROT("R2,C2,S6-11,B4,9-11,NW0020003330230320333000200")
    val search = CFind(
        HROT("R2,C2,S0,B2,N+"), 2, 1, 8, ShipSymmetry.ODD,
        verbosity = 1, searchStrategy = SearchStrategy.HYBRID_BFS, //lookaheadDepth = 1, //stdin = true,
        direction = Coordinate(1, 1), numThreads = 8, lookaheadDepth = 2
    )
    search.search()

//    val ship = SparseGrid(
//        "obo\$bo4\$bo\$bo\$3o\$obo!",
//        rule = INT("B2cei3-eijr4ceirtw5ciqy6-e7c/S1c2-c3-aiqy4ikntw5nqr6-a")
//    ).identify() as Spaceship
//    val phases = ship.phases.filter { it.population < 50 }
//
//    while (true) {
//        val soup = SparseGrid()
//        for (i in 2..Random.nextInt(3) + 2) {
//            val pattern = phases[Random.nextInt(phases.size)]
////            .apply {
////                for (j in 0 .. Random.nextInt(3))
////                    rotate(Rotation.CLOCKWISE)
////                if (Random.nextInt(2) == 0) flip(Flip.VERTICAL)
////                if (Random.nextInt(2) == 0) flip(Flip.HORIZONTAL)
////            }
//            soup[Coordinate(Random.nextInt(100), Random.nextInt(100))] = pattern
//        }
//
//        println("x = 0, y = 0, rule = B2cei3-eijr4ceirtw5ciqy6-e7c/S1c2-c3-aiqy4ikntw5nqr6-a")
//        println(soup.toRLE())
//    }
}