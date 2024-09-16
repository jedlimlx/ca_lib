package cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.output.MordantHelpFormatter
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.int
import patterns.parseSpeed
import rules.fromRulestring
import search.cfind.CFind
import search.cfind.SearchStrategy
import search.cfind.ShipSymmetry
import simulation.Coordinate


fun gcd(a: Int, b: Int): Int {
    var num1 = a
    var num2 = b
    while (num2 != 0) {
        val temp = num2
        num2 = num1 % num2
        num1 = temp
    }

    return num1
}


class RequiredOptions : OptionGroup(
    name = "Required Options",
    help = "Options controlling the type of spaceship that will be searched for."
) {
    val rule by option("--rule", "-r", help="The cellular automaton rule to search in.").convert {
        println(it)
        fromRulestring(it)
    }.required()
    val speed by option("--vel", "-v", help="The speed of the ship.").required()
    val width by option("--width", "-w", help="The width of the ship.").int().required()
    val symmetry by option(
        "--symmetry", "-s", help="The symmetry that the ship should obey."
    ).choice(
        mapOf(
            "e" to ShipSymmetry.EVEN,
            "o" to ShipSymmetry.ODD,
            "a" to ShipSymmetry.ASYMMETRIC,
            "g" to ShipSymmetry.GLIDE,
            "gu" to ShipSymmetry.GUTTER
        )
    ).required()
}

class SearchOptions: OptionGroup(
    name = "Search Options",
    help = "Options controlling how the search is carried out."
) {
    val anisotropic by option(
        "--anisotropic", "-ani",
        help = "Is the rule anisotropic? (affects optimisations for asymmetric searches)"
    ).flag()

    val lookaheadDepth by option(
        "--lookahead_depth", "-ll",
        help="The depth of the lookahead to be performed during the search. " +
                "(a lookahead of a smaller depth may be performed if a lookahead of the given depth is not possible)"
    ).int().default(5)

    val transpositionTableSize by option(
        "--transposition_size", "-tt",
        help = "log2 the size of the transposition table used to prevent repeated searching of equivalent states."
    ).int().default(24)

    val minDeepeningIncrement by option(
        "--min_deep", "-d",
        help="The amount by which to deepen during the depth-first search round. " +
                "Defaults to period of the ship for the hybrid BFS search strategy and " +
                "10*period for the priority queue search strategy."
    ).int().default(-1)

    val maxQueueSize by option(
        "--max_q", "-q",
        help="log2 of the maximum size of the queue size before the DFS round is triggered."
    ).int().default(20)

    val maxTimePerRound by option(
        "--max_time", "-time",
        help = "The maximum time for each deepening round in the priority queue search strategy in seconds."
    ).int().default(600)

    val searchStrategy by option(
        "--strategy", "-st",
        help="The symmetry that the ship should obey."
    ).choice(
        mapOf(
            "bfs" to SearchStrategy.HYBRID_BFS,
            "dfs" to SearchStrategy.DFS,
            "pq" to SearchStrategy.PRIORITY_QUEUE
        )
    ).default(SearchStrategy.PRIORITY_QUEUE)
}

class OutputOptions: OptionGroup(
    name = "Output Options",
    help = "Options controlling the output provided by the program"
) {
    val numShips by option(
        "--num_ships", "-n",
        help = "The depth of the lookahead to be performed during the search. " +
                "(a lookahead of a smaller depth may be performed if a lookahead of the given depth is not possible)"
    ).int().default(5)

    val partialFrequency by option(
        "--partial_freq", "-pf",
        help = "The number of new rows to be found for which one partial is printed out."
    ).int().default(1000)

    val backupFrequency by option(
        "--backup_freq", "-bf",
        help = "The frequency at which backups should be saved in seconds."
    ).int().default(3600)

    val backupName by option(
        "--backup_name", "-bn",
        help = "The name of the file to save backups to."
    ).default("dump")

    val verbosity by option(
        "--verbosity", "-vv",
        help = "The amount of information you want the search to output. (nothing: -1, default: 0, debug: 1)"
    ).int().default(0)

    val stdin by option(
        "--stdin", "-std",
        help = "Output all partials with no other output (useful for piping output into apgsearch)."
    ).flag()
}



class CFindCommand: CliktCommand() {
    init {
        context {
            helpFormatter = { MordantHelpFormatter(it, showDefaultValues = true, requiredOptionMarker = "*") }
        }
    }

    override fun help(context: Context) = "Searches for spaceships of the given speed using the CFind algorithm."

    /* Required Parameters */
    val requiredOptions by RequiredOptions()

    /* Search Options*/
    val searchOptions by SearchOptions()

    /* Output Options */
    val outputOptions by OutputOptions()

    /* Miscellaneous Options */
    val numThreads by option(
        "--threads", "-t",
        help = "The number of threads to use to parallelise the search."
    ).int().default(1)

    override fun run() {
        // Getting the direction, displacement and period from the speed string
        val (temp, period) = parseSpeed(requiredOptions.speed)
        val displacement = gcd(temp.first, temp.second)
        val direction = Coordinate(
            temp.first / displacement,
            temp.second / displacement
        )

        // Loading parameters and running the search
        val search = CFind(
            requiredOptions.rule,
            period, displacement,
            requiredOptions.width, requiredOptions.symmetry,
            direction,
            !searchOptions.anisotropic,
            1 shl searchOptions.maxQueueSize,
            searchOptions.minDeepeningIncrement,
            searchOptions.lookaheadDepth,
            searchOptions.searchStrategy,
            outputOptions.numShips,
            outputOptions.partialFrequency,
            outputOptions.backupFrequency,
            outputOptions.backupName,
            1 shl searchOptions.transpositionTableSize,
            searchOptions.maxTimePerRound,
            numThreads,
            outputOptions.stdin,
            verbosity = outputOptions.verbosity
        )
        search.search()
    }
}