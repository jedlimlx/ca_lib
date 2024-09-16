package patterns

/**
 * Parses the [speed] and outputs the displacement in the x and y-directions and the period.
 */
fun parseSpeed(speed: String): Pair<Pair<Int, Int>, Int> {
    if (speed.last() == 'o' || (speed.last().isDigit() && "(" !in speed)) {  // orthogonal speed
        val tokens = speed.replace("o", "").split("c/")
        val k = if (tokens[0].isEmpty()) 1 else tokens[0].toInt()
        val p = tokens[1].toInt()
        return Pair(Pair(0, k), p)
    } else if (speed.last() == 'd') {  // diagonal speed
        val tokens = speed.split("c/")
        val k = if (tokens[0].isEmpty()) 1 else tokens[0].toInt()
        val p = tokens[1].subSequence(0, tokens[1].length-1).toString().toInt()
        return Pair(Pair(k, k), p)
    } else {
        val tokens = speed.split(")c/")
        val tokens2 = tokens[0].replace("(", "").split(",", "")
        val p = tokens[1].subSequence(0, tokens[1].length-1).toString().toInt()
        return Pair(Pair(tokens2[0].toInt(), tokens2[1].toInt()), p)
    }
}