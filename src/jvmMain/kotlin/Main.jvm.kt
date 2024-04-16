import rules.hrot.HROT
import search.cfind.CFind
import search.cfind.SearchStrategy
import search.cfind.ShipSymmetry

import it.skrape.core.*
import it.skrape.fetcher.*
import it.skrape.selects.*
import it.skrape.selects.html5.*

import simulation.DenseGrid
import patterns.Spaceship

actual fun main() {
    val rule = HROT("R2,C2,S6-9,14-20,B7-8,15-24,NM")
    val rulestring = "r2bffc0c0s0fe1e0"
    val symmetry = "cfind_stdin"

    val list: List<String> = skrape(HttpFetcher) {
        request {
            url = "https://catagolue.hatsya.com/census/$rulestring/$symmetry"
        }
        response {
            htmlDocument {
                a {
                    findAll {
                        eachHref
                    }
                }
            }
        }
    }

    val output = list.filter { Regex("/census/$rulestring/$symmetry/(.*?)").containsMatchIn(it) }.map {
        "https://catagolue.hatsya.com/textcensus/$rulestring/$symmetry/" + it.split("/").last()
    }.map {
        skrape(HttpFetcher) {
            request { url = it }
            response { htmlDocument { body { findFirst { text } } } }
        }.split(" ")
    }.map {
        it.subList(1, it.size).map { it.split(",").first().replace("\"", "") }.filter { it[0] == 'x' }
    }.map {
        it.map {
            val spaceship = DenseGrid(rule=rule, pattern=it).identify() as Spaceship
            spaceship.discoverer = "Lemon41625, 2024"
            spaceship.gliderdbEntry
        }
    }
    println(output)
}