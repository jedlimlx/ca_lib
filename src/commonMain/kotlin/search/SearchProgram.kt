package search

import com.github.ajalt.mordant.rendering.AnsiLevel
import com.github.ajalt.mordant.terminal.Terminal
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
     * The Mordant terminal used for all the pretty-printing
     */
    protected val t = Terminal(AnsiLevel.TRUECOLOR, interactive=true)

    protected fun print(x: Any, verbosity: Int = 0) {
        if (verbosity <= this.verbosity)
            t.print(x)
    }

    protected fun println(x: Any, verbosity: Int = 0) {
        if (verbosity <= this.verbosity)
            t.println(x)
    }
}