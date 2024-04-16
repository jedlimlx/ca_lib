package catagolue

import rules.Rule
import patterns.Spaceship

/**
 * Finds the Catagolue census corresponding to the [rule]
 */
fun getCensus(rule: Rule): String {
    TODO("Implement obtaining of Catagolue census URL from the rule")
}

/**
 * Returns a map of the number of spaceships and their relative frequency in the census
 */
fun getShips(census: String): Map<Spaceship, Double> {
    TODO("Implement getting of ships")
}