package search

import patterns.Pattern

/**
 * Base class for various search programs
 */
abstract class SearchProgram {
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
}