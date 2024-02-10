private external fun require(module: String): dynamic


actual val PLATFORM = "JS"

actual fun readResource(resource: String): String {
    val tokens = resource.split("/")

    // For some reason I have to pre-specify the string... :(
    // Why is JavaScript like this...
    return when (tokens[tokens.size - 1]) {
        "anisotropic.txt" -> require("anisotropic.txt") as String
        "r1_moore.txt" -> require("r1_moore.txt") as String
        "r1_hex.txt" -> require("r1_hex.txt") as String
        "r2_cross.txt" -> require("r2_cross.txt") as String
        "r2_knight.txt" -> require("r2_knight.txt") as String

        "shiftTest.csv" -> require("shiftTest.csv") as String
        "flipTest.csv" -> require("flipTest.csv") as String
        "rotateTest.csv" -> require("rotateTest.csv") as String
        "invertTest.csv" -> require("invertTest.csv") as String
        "bitwiseTest.csv" -> require("bitwiseTest.csv") as String
        "patternExportTest.csv" -> require("patternExportTest.csv") as String

        "oscillatorStats.csv" -> require("oscillatorStats.csv") as String
        "spaceshipStats.csv" -> require("spaceshipStats.csv") as String

        "hrotParsingTest.txt" -> require("hrotParsingTest.txt") as String
        "hrotSimulationTest.txt" -> require("hrotSimulationTest.txt") as String
        "hrotRuleRangeTest.txt" -> require("hrotRuleRangeTest.txt") as String

        "hrotGenerationsParsingTest.txt" -> require("hrotGenerationsParsingTest.txt") as String
        "hrotGenerationsSimulationTest.txt" -> require("hrotGenerationsSimulationTest.txt") as String
        "hrotGenerationsRuleRangeTest.txt" -> require("hrotGenerationsRuleRangeTest.txt") as String

        "hrotExtendedGenerationsParsingTest.txt" -> require("hrotExtendedGenerationsParsingTest.txt") as String
        "hrotExtendedGenerationsSimulationTest.txt" -> require("hrotExtendedGenerationsSimulationTest.txt") as String
        "hrotExtendedGenerationsRuleRangeTest.txt" -> require("hrotExtendedGenerationsRuleRangeTest.txt") as String

        "intParsingTest.txt" -> require("intParsingTest.txt") as String
        "intSimulationTest.txt" -> require("intSimulationTest.txt") as String
        "intRuleRangeTest.txt" -> require("intRuleRangeTest.txt") as String

        else -> require(tokens[tokens.size - 1]) as String
    }
}