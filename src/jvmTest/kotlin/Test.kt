import org.junit.Ignore
import rules.hrot.HROT
import search.GeneticShipSearch
import simulation.SparseGrid
import java.io.File
import kotlin.test.Test

class Test {
    fun test() {
        val shipSearch = GeneticShipSearch(HROT("B3/S23"), period = 4, dx = 0, dy = 1, width = 7, height = 7)

        val file = File("C:\\Users\\jedli\\Documents\\CA\\qfind\\partials.txt")
        val lines = file.readText().split("\n\n")
        val partials = lines.map {
            val lines = it.split("\n")
            if (lines.size == 1)
                SparseGrid(lines.subList(1, lines.size).joinToString(""), rule=HROT("B3/S23"))
            else SparseGrid(lines.subList(2, lines.size).joinToString(""), rule=HROT("B3/S23"))
        }

        val ranked = shipSearch.rank(partials)

        val rankedFile = File("ranked.txt")
        val stringBuilder = StringBuilder()
        ranked.forEach {
            stringBuilder.append("$it\n")
        }

        rankedFile.writeText(stringBuilder.toString())
    }
}