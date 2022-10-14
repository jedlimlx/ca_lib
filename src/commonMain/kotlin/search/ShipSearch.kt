package search

import patterns.Pattern
import rules.Rule
import simulation.*
import soup.generateC1
import kotlin.math.abs
import kotlin.math.log
import kotlin.random.Random

/**
 * Searches for spaceships using Genetic Algorithms
 */
class GeneticShipSearch(
    val rule: Rule,
    val period: Int,
    val dx: Int,
    val dy: Int,
    val width: Int,
    val height: Int,
    val startingPopulation: Collection<Grid> = listOf()
): SearchProgram() {
    val POPULATION_SIZE: Int = 40000

    val population = ArrayList<Grid>(POPULATION_SIZE)
    val populationHashSet = HashSet<Int>(POPULATION_SIZE)

    var generation = 0
        private set

    var improvement = true
        private set

    var currentBest = 10000000.0
        private set

    override val searchResults: MutableList<Pattern> = mutableListOf()

    private fun fitness(pattern: Grid): Double {
        if (pattern.population == 0) return 10000000.0

        // Compute the difference after 3 periods
        var fitness = 0.0
        val afterPeriod = pattern.deepCopy()
        val target = pattern.deepCopy()
        for (i in 1 .. 4) {
            afterPeriod.step(period)
            target.shift(dx, dy)

            afterPeriod.updateBounds()
            pattern.updateBounds()

            // Compute 1 - IoU (Intersection over Union) -> Reward partials that maintain their shape
            val intersection = afterPeriod intersect target
            val iou = (1 - (intersection.population.toDouble() / (afterPeriod.population +
                    target.population - intersection.population)))

            fitness += (4 - i) * 30 * log(iou + 1, 5.0)

            // Encourages partials to have a larger population and not go down the local optimum of an empty grid
            if (i < 2)
                fitness += (5 - 2 * i) / (0.0001 + afterPeriod.population.toDouble())

            fitness += 10 * abs(afterPeriod.population - pattern.population) / pattern.population

            // Encourages partials to move
            if (afterPeriod.population == 0) fitness += 5 * i
            else fitness += (5 * i * ((afterPeriod.bounds.start - pattern.bounds.start) - Coordinate(i * dx, i * dy)).manhattan).toDouble()
        }

        return fitness
    }

    private fun mutate(pattern: Grid, mutationRate: Int = 1): Grid {
        val prob = Random.nextDouble(1.0)
        val newPattern = pattern.deepCopy()

        when {
            prob < 0.7 -> {
                val coordinates = newPattern.toList().map { (coordinate, _) -> coordinate }
                if (coordinates.isNotEmpty()) {
                    for (i in 0..Random.nextInt(1, 3 * mutationRate)) {
                        val newCoordinate = coordinates[Random.nextInt(coordinates.size)] +
                                Coordinate(Random.nextInt(-2, 3), Random.nextInt(-2, 3))
                        newPattern[newCoordinate] = if (newPattern[newCoordinate] == 1) 0 else 1
                    }
                }
            }
            prob < 0.95 -> newPattern.step(Random.nextInt(1, 5))
        }

        return newPattern
    }

    private fun mate(father: Grid, mother: Grid): Grid {
        val prob = Random.nextDouble(1.0)

        val child: Grid
        when {
            prob < 0.5 -> {
                child = father.deepCopy()
                mother.forEach { (coordinate, state) -> if (Random.nextInt(2) == 0) child[coordinate] = state }
            }
            prob < 1.0 -> {
                child = mother.deepCopy()
                father.forEach { (coordinate, state) -> if (Random.nextInt(2) == 0) child[coordinate] = state }
            }
            prob < 0.75 -> {
                child = father.deepCopy()
                child.updateBounds()

                val bounds = child.bounds
                child.clear(bounds.start .. Coordinate((bounds.start.x + bounds.end.x) / 2, bounds.end.y))

                child[bounds.start] = mother[bounds.start .. Coordinate((bounds.start.x + bounds.end.x) / 2, bounds.end.y)]
            }
            else -> {
                child = mother.deepCopy()
                child.updateBounds()

                val bounds = child.bounds
                child.clear(bounds.start .. Coordinate((bounds.start.x + bounds.end.x) / 2, bounds.end.y))

                child[bounds.start] = father[bounds.start .. Coordinate((bounds.start.x + bounds.end.x) / 2, bounds.end.y)]
            }
        }

        return father.deepCopy()
    }

    private fun pickParents(sortedFitness: List<Pair<Grid, Double>>): Pair<Grid, Grid> {
        val k = 15
        val selected = List(k) { sortedFitness[Random.nextInt(sortedFitness.size)] }.sortedBy { it.second }
        return Pair(selected[0].first, selected[1].first)
    }

    private fun generateRandomChromosome(): Grid {
        var pattern: Grid
        var density = Random.nextDouble(0.2, 0.7)
        pattern = generateC1(width, height, density = doubleArrayOf(1 - density, density)).also { it.rule = rule }

        while (pattern.population <= 0) {
            density = Random.nextDouble(0.2, 0.7)
            pattern = generateC1(width, height, density = doubleArrayOf(1 - density, density)).also { it.rule = rule }
        }

        return pattern.step(Random.nextInt(0, 5))
    }

    fun rank(partials: List<Grid>) = partials.sortedBy { fitness(it) }.map { Pair(fitness(it), it) }

    fun fitnessReport(pattern: Grid) {
        // Compute the difference after 3 periods
        var fitness = 0.0
        val afterPeriod = pattern.deepCopy()
        val target = pattern.deepCopy()
        for (i in 1 .. 4) {
            println("Period $i")
            println("---------------------------")
            afterPeriod.step(period)
            target.shift(dx, dy)

            afterPeriod.updateBounds()
            pattern.updateBounds()

            // Compute 1 - IoU (Intersection over Union) -> Reward partials that maintain their shape
            val intersection = afterPeriod intersect target
            val iou = (1 - (intersection.population.toDouble() / (afterPeriod.population +
                    target.population - intersection.population)))

            println("IoU: ${(4 - i) * 10 * log(iou + 1, 5.0)}")
            fitness += (4 - i) * 10 * log(iou + 1, 5.0)

            // Encourages partials to have a larger population and not go down the local optimum of an empty grid
            if (i < 2) {
                fitness += (5 - 2 * i) / (0.0001 + afterPeriod.population.toDouble())
                println("Population Size: ${(5 - 2 * i) / (0.0001 + afterPeriod.population.toDouble())}")
            }

            fitness += 10 * abs(afterPeriod.population - pattern.population) / pattern.population
            println("Population Size 2: ${10 * abs(afterPeriod.population - pattern.population) / pattern.population}")

            // Encourages partials to move
            if (afterPeriod.population == 0) fitness += 5 * i
            else fitness += (i * ((afterPeriod.bounds.start - pattern.bounds.start) - Coordinate(i * dx, i * dy)).manhattan).toDouble()
            println("Movement: ${(i * ((afterPeriod.bounds.start - pattern.bounds.start) - Coordinate(i * dx, i * dy)).manhattan).toDouble()}")

            println()
        }

        println()
        println("Total Fitness: $fitness")
        println()
    }

    override fun search() {
        // First, initialise the population
        for (i in 0 until POPULATION_SIZE) population.add(mutate(startingPopulation.random()))

        // Running the generations
        for (j in 0 .. 1000) {
            println("Generation $generation\n------------")

            // Compute the fitness function
            val fitness = population.map { Pair(it, fitness(it)) }
            val sortedFitness = fitness.sortedBy { (_, fitness) -> fitness }

            println("Minimum Fitness: ${sortedFitness[0].second}")
            println("Average Fitness: ${(sortedFitness.sumOf { it.second } * 1.0) / sortedFitness.size}")
            println("Average Population: ${sortedFitness.sumOf { it.first.population } * 1.0 / sortedFitness.size}\n")

            println("Top 5 Individuals\n----------------")
            for (i in 1 .. 5) println("$i. ${sortedFitness[i - 1].first.toRLE()}")
            println()

            println("Median Individual: ${sortedFitness[sortedFitness.size / 2].first.toRLE()}")
            println()

            if (currentBest < sortedFitness[0].second) improvement = false
            else {
                currentBest = sortedFitness[0].second
                improvement = true
            }

            // Pick parents to give birth and replenish the population
            population.clear()
            populationHashSet.clear()

            // Keep the elites
            for (i in 0 until 500) {
                val hash = sortedFitness[i].first.hashCode()

                if (hash !in populationHashSet) {
                    population.add(sortedFitness[i].first)
                    populationHashSet.add(hash)
                }
            }

            var count = 0  // Populate the rest with the children
            while (count < POPULATION_SIZE - 500 - 500) {
                val (father, mother) = pickParents(sortedFitness)

                val child = mutate(mate(father, mother), 2)
                val hash = child.hashCode()

                if (hash !in populationHashSet) {
                    population.add(child)
                    populationHashSet.add(hash)

                    count++
                }
            }

            generation++
        }
    }

    override fun stop() {
        TODO("Not yet implemented")
    }

    override fun saveToFile(filename: String) {
        TODO("Not yet implemented")
    }
}