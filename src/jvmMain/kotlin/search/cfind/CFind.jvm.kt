package search.cfind

import com.github.ajalt.mordant.rendering.TextStyles
import java.util.concurrent.Executors
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

actual fun multithreadedDfs(
    queue: ArrayDeque<Row>,
    cfind: CFind
): Pair<ArrayDeque<Row>, Int> {
    var num = 0
    val newQueue = ArrayDeque<Row>(cfind.maxQueueSize)

    // Synchronisation primitives
    val mutex = Object()
    val mutex2 = Object()
    val mutex3 = Object()

    val executor = Executors.newFixedThreadPool(cfind.numThreads) as ThreadPoolExecutor

    // Split the queue into different batches to prevent overloading executor with too many tasks
    val batchSize = 2000
    val batches = queue.size / batchSize

    var count = 0
    var clearPartial = false
    var clearLines = 0
    for (b in 0 ..< batches) {
        executor.submit {
            val stack = arrayListOf<Row>()
            val end = if (b == batches - 1) queue.size else batchSize*(b+1)
            for (i in batchSize*b..<end) {
                val row = queue[i]
                var currentRow = row

                // Placing row within DFS stack
                stack.clear()
                stack.add(row)

                // Computing the depth that needs the row needs to be pruned until
                val maxDepth = minOf(
                    row.prunedDepth + cfind.minDeepeningIncrement,
                    row.depth + (3 * cfind.originalMinDeepening)
                )

                if (row.prunedDepth > maxDepth) {
                    synchronized(mutex) {
                        newQueue.add(row)
                        count++
                        return@submit
                    }
                }

                synchronized(mutex3) { num += maxDepth - row.depth }

                do {
                    if (stack.isEmpty()) break

                    // Get the current row that is going to be analysed
                    currentRow = stack.removeLast()
                    if (currentRow.depth == maxDepth) {
                        row.prunedDepth = maxDepth
                        synchronized(mutex) { newQueue.add(row) }
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

                synchronized(mutex2) {
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

                        cfind.t.println(
                            TextStyles.bold(
                                "\nChecked ${count - 1} / ${cfind.maxQueueSize} rows, " +
                                        "pruned ${(10000 - (newQueue.size * 10000 / count)) / 100.0}%"
                            )
                        )
                        cfind.t.println("x = 0, y = 0, rule = ${cfind.rule}\n" + rle.joinToString("\n"))
                        clearPartial = true
                    }
                }
            }
        }
    }

    executor.shutdown()
    executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)
    return Pair(newQueue, num)
}