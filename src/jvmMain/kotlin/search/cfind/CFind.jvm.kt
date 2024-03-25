package search.cfind

import com.github.ajalt.mordant.rendering.TextStyles
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

actual fun multithreadedDfs(
    currentRow: Row,
    queue: ArrayDeque<Row>,
    cfind: CFind
): Pair<ArrayDeque<Row>, Int> {
    var num = 0
    var currentRow: Row = currentRow
    val newQueue = ArrayDeque<Row>(cfind.maxQueueSize)

    runBlocking {
        val mutex = Mutex()
        val mutex2 = Mutex()
        val rowsPerThread = queue.size / cfind.numThreads

        var count = 0
        var clearPartial = false
        var clearLines = 0
        for (thread in 0..<cfind.numThreads) {
            val endPoint = if (thread == cfind.numThreads - 1) queue.size else rowsPerThread*(thread+1)
            launch {
                val stack = arrayListOf<Row>()
                for (i in rowsPerThread*thread ..< endPoint) {
                    val row = queue[i]

                    // Placing row within DFS stack
                    stack.clear()
                    stack.add(row)

                    // Computing the depth that needs the row needs to be pruned until
                    val maxDepth = minOf(
                        row.prunedDepth + cfind.minDeepeningIncrement,
                        row.depth + (2 * cfind.originalMinDeepening)
                    )

                    if (row.prunedDepth > maxDepth) {
                        mutex.withLock { newQueue.add(row) }
                        continue
                    }

                    num += maxDepth - row.depth

                    do {
                        if (stack.isEmpty()) break

                        // Get the current row that is going to be analysed
                        currentRow = stack.removeLast()
                        if (currentRow.depth == maxDepth) {
                            row.prunedDepth = maxDepth
                            mutex.withLock { newQueue.add(row) }
                            break
                        }

                        // Get the rows that will need to be used to find the next row
                        val (rows, lookaheadRows) = cfind.extractRows(currentRow)
                        val successors = cfind.nextRow(currentRow, rows, lookaheadRows, depth = currentRow.depth + 1).first
                        currentRow.numSuccessors = successors.size

                        if (successors.isEmpty()) currentRow.predecessor!!.addDeadend(currentRow.hashCode())
                        else {
                            if (currentRow.deadends != null) {
                                stack.addAll(successors.filter { it.hashCode() !in currentRow.deadends!! })
                            } else stack.addAll(successors)
                        }
                    } while (true)

                    mutex2.withLock {
                        if ((count++).mod(cfind.partialFrequency) == 0) {
                            val grid = currentRow.toGrid(cfind.period, cfind.symmetry)
                            grid.rule = cfind.rule

                            if (cfind.verbosity >= 0 && clearPartial) {
                                cfind.t.cursor.move {
                                    up(3 + clearLines)
                                    startOfLine()
                                    clearScreenAfterCursor()
                                }
                                cfind.t.cursor.hide(showOnExit = true)
                            }

                            val rle = grid.toRLE().chunked(70)
                            clearLines = rle.size

                            println(
                                TextStyles.bold(
                                "\nChecked ${count - 1} / ${cfind.maxQueueSize} rows, " +
                                        "pruned ${(10000 - (newQueue.size * 10000 / count)) / 100.0}%"
                                )
                            )
                            println("x = 0, y = 0, rule = ${cfind.rule}\n" + rle.joinToString("\n"))
                            clearPartial = true
                        }
                    }
                }
            }
        }
    }

    return Pair(newQueue, num)
}