package rules.ruleloader

import Colour

/**
 * Constructs a @COLORS directive given the [contents] found in a ruletable under that directive
 */
fun colourDirectiveFromString(contents: String): ColourDirective {
    return ColourDirective(
        colours = contents.split("\n").map {
            val tokens = it.split(" ")
            Colour(tokens[1].toInt(), tokens[2].toInt(), tokens[3].toInt())
        }.toTypedArray()
    )
}

/**
 * Represents the @COLORS directive in ruletables for specifying the colour of different states
 * @param colours The colours of each state (colours\[i\] gives colour of state i)
 * @property colours The colours of each state (colours\[i\] gives colour of state i)
 * @param background The background of the rule
 * @property background The background of the rule
 */
class ColourDirective(val colours: Array<Colour>, val background: IntArray = intArrayOf(1)) : Directive("colors") {
    override fun export(): String = with(StringBuilder()) {
        for (i in 0..<1 + (colours.size - 1) * background.size) {
            var index = i % (colours.size - 1)
            if (index == 0 && i > 0) index = colours.size - 1

            val (red, green, blue) = colours[index]
            append("$i $red $green $blue\n")
        }

        this
    }.toString()
}