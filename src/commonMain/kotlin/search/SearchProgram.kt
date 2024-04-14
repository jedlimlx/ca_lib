package search

import com.github.ajalt.mordant.rendering.AnsiLevel
import com.github.ajalt.mordant.terminal.Terminal
import kotlinx.io.bytestring.ByteString
import patterns.Pattern

/**
 * Base class for various search programs
 */
abstract class SearchProgram(val verbosity: Int = -1) {
    /**
     * Stores the search results
     */
    abstract val searchResults: List<Pattern>

    /**
     * Begin running the search program
     */
    abstract fun search()

    /**
     * Stop the search program
     */
    abstract fun stop()

    /**
     * Saves the search results to a file
     */
    abstract fun saveToFile(filename: String)

    /**
     * Saves the state of the search
     */
    abstract fun saveState(): String

    /**
     * Loads the state of the search
     */
    abstract fun loadState(string: String)

    /**
     * The Mordant terminal used for all the pretty-printing
     */
    val t = Terminal(AnsiLevel.TRUECOLOR, interactive=true)

    open fun print(x: Any, verbosity: Int = 0) {
        if (verbosity <= this.verbosity)
            t.print(x)
    }

    open fun println(x: Any, verbosity: Int = 0) {
        if (verbosity <= this.verbosity)
            t.println(x)
    }
}