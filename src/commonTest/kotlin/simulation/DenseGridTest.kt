package simulation

import readResource
import soup.generateC1
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.todo

class DenseGridTest {
    @Test
    fun shift_pattern_correctly() {
        var tokens: List<String>
        val testCases = readResource("simulation/shiftTest.csv").split("\n")

        for (i in 1 until testCases.size) {
            tokens = testCases[i].trim().split(",")

            val grid = DenseGrid(tokens[0]).shift(tokens[1].toInt(), tokens[2].toInt())
            grid.updateBounds()

            assertEquals(tokens[0].replace("o", "A").replace("b", "."),
                grid.toRLE().replace("o", "A").replace("b", "."))
            assertEquals(Coordinate(tokens[1].toInt(), tokens[2].toInt()), grid.bounds.start)
        }
    }

    @Test
    fun flip_pattern_correctly() {
        var tokens: List<String>
        val testCases = readResource("simulation/flipTest.csv").split("\n")

        for (i in 1 until testCases.size) {
            tokens = testCases[i].trim().split(",")

            // Load the RLE
            val grid = DenseGrid(tokens[0])
            grid.updateBounds()

            // Store some information about the previous grid
            val prevBounds = grid.bounds
            val prevPopulation = grid.population

            // Flip the grid
            grid.flip(if (tokens[2] == "horizontal") Flip.HORIZONTAL else Flip.VERTICAL)

            // Test the different characteristics of the grid
            assertEquals(prevBounds, grid.bounds)
            assertEquals(prevPopulation, grid.population)
            assertEquals(tokens[1].replace("o", "A").replace("b", "."),
                grid.toRLE().replace("o", "A").replace("b", "."))
        }
    }

    @Test
    fun rotate_pattern_correctly() {
        var tokens: List<String>
        val testCases = readResource("simulation/rotateTest.csv").split("\n")

        for (i in 1 until testCases.size) {
            tokens = testCases[i].trim().split(",")

            // Load the RLE
            val grid = DenseGrid(tokens[0])
            grid.updateBounds()

            // Store some information about the previous grid
            val prevPopulation = grid.population

            // Flip the grid
            grid.rotate(if (tokens[4] == "clockwise") Rotation.CLOCKWISE else Rotation.ANTICLOCKWISE)

            // Test the different characteristics of the grid
            grid.updateBounds()
            //assertEquals(Coordinate(tokens[2].toInt(), tokens[3].toInt()), grid.bounds.first)

            assertEquals(prevPopulation, grid.population)
            assertEquals(tokens[1].replace("o", "A").replace("b", "."),
                grid.toRLE().replace("o", "A").replace("b", "."))
        }
    }

    @Test
    fun correctly_read_and_export_rle() {
        var tokens: List<String>
        val testCases = readResource("simulation/patternExportTest.csv").split("\n")

        for (i in 1 until testCases.size) {
            tokens = testCases[i].trim().split(",")

            val grid = DenseGrid(tokens[0])
            val correctRLE = tokens[0].replace("b", ".").replace("o", "A")

            assertEquals(tokens[2].toInt(), grid.population)
            assertEquals(correctRLE, grid.toRLE().replace("o", "A").replace("b", "."))
        }
    }

    @Test
    fun correctly_read_and_export_apgcode() {
        var tokens: List<String>
        val testCases = readResource("simulation/patternExportTest.csv").split("\n")

        for (i in 1 until testCases.size) {
            tokens = testCases[i].trim().split(",")

            val grid = DenseGrid(tokens[1])
            val correctApgcode = tokens[1].split("_").slice(1 until
                    tokens[1].split("_").size).joinToString("_")

            assertEquals(tokens[2].toInt(), grid.population)

            // Not yet implemented
            todo { assertEquals(correctApgcode, grid.toApgcode()) }
        }
    }

    @Test
    fun invert_pattern_correctly() {
        var tokens: List<String>
        val testCases = readResource("simulation/invertTest.csv").split("\n")

        for (i in 1 until testCases.size) {
            tokens = testCases[i].trim().split(",")

            // Load the RLE
            val grid = DenseGrid(tokens[0])
            grid.updateBounds()

            grid.invert(grid.bounds)
            assertEquals(tokens[1], grid.toRLE())
        }
    }

    @Test
    fun perform_bitwise_operations_correctly() {
        var tokens: List<String>
        val testCases = readResource("simulation/bitwiseTest.csv").split("\n")

        for (i in 1 until testCases.size) {
            tokens = testCases[i].trim().split(",")

            // Load the RLE
            val grid = DenseGrid(tokens[0])
            val grid2 = DenseGrid(tokens[1])

            assertEquals(tokens[2], (grid intersect grid2).toRLE())
            assertEquals(tokens[3], (grid + grid2).toRLE())
        }
    }

    @Test
    fun no_concurrent_modification_exception() {
        val grid = SparseGrid(generateC1().toRLE())
        grid.forEach { (coordinate, _) -> grid[coordinate] = 1 }
    }
}