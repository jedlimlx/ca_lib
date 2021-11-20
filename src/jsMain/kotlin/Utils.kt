private external fun require(module: String): dynamic

actual fun readResource(resource: String): String {
    val tokens = resource.split("/")

    // For some reason I have to pre-specify the string... :(
    // Why is JavaScript like this...
    return when (tokens[tokens.size - 1]) {
        "patternExportTest.csv" -> require("patternExportTest.csv") as String
        "shiftTest.csv" -> require("shiftTest.csv") as String
        "flipTest.csv" -> require("flipTest.csv") as String
        "rotateTest.csv" -> require("rotateTest.csv") as String
        else -> require(tokens[tokens.size - 1]) as String
    }
}