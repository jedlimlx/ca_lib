expect fun readResource(resource: String): String

/**
 * Various utility functions used by different classes
 */
class Utils {
    companion object {
        fun convert(state: Int, bg: Int): Int {
            return when (state) {
                bg -> 0
                0 -> bg
                else -> state
            }
        }
    }
}