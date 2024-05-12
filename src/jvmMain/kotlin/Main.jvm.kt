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
import rules.ruleloader.builders.ruletable
import rules.ruleloader.ruletableFromFile
import rules.ruleloader.ruletree.ruletreeDirectiveFromString
import search.cfind.CFind
import search.cfind.SearchStrategy
import search.cfind.ShipSymmetry
import simulation.*
import java.io.File
import kotlin.random.Random

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

//    val ruletable = ruletable {
//        name = "B1S235F23K4L035"
//
//        val birth = setOf(1)
//        val survival = setOf(2, 3, 5)
//        val forcing = setOf(2, 3)
//        val killing = setOf(4)
//        val living = setOf(0, 3, 5)
//
//        tree(
//            numStates = 3,
//            neighbourhood = moore(1),
//            background = intArrayOf(0)
//        ) { neighbours, cellState ->
//            val sum1 = neighbours.count { it == 1 }
//            val sum2 = neighbours.count { it == 2 }
//
//            when (cellState) {
//                1 -> {
//                    if (killing.contains(sum2)) 0
//                    else if (survival.contains(sum1)) 1
//                    else 2
//                }
//                2 -> {
//                    if (living.contains(sum1)) 0 else 2
//                }
//                else -> {
//                    if (birth.contains(sum1) && forcing.contains(sum2)) 1 else 0
//                }
//            }
//        }
//    }

//    val ruletable = ruletableFromFile("SoManyShips3.rule")
//
//    // B2-ei3cjkr4cektyz5-cnr6-ik78/S01e2-ae3cnqry4cqrtwyz5-ain6ekn7e
    val search = CFind(
        DeficientINT("B2/S/D"), 2, 1, 6, ShipSymmetry.ODD,
        verbosity = 1, searchStrategy = SearchStrategy.PRIORITY_QUEUE, lookaheadDepth = 1
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