package search.cfind

import com.github.ajalt.mordant.rendering.TextStyles
import java.util.concurrent.Executors
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

actual fun multithreadedDfs(cfind: CFind): Int {
    var num = 0

    // Synchronisation primitives
    val mutex = Object()
    val mutex2 = Object()

    // Other shared variables
    var count = 0
    var prunedCount = 0

    var clearPartial = false
    var clearLines = 0

    var row: Row? = cfind.head

    val threadLst = arrayListOf<Thread>()
    for (i in 0 ..< cfind.numThreads) {
        val thread = Thread {
            val stack = arrayListOf<Row>()
            while (true) {
                // Placing row within DFS stack
                stack.clear()

                val maxDepth: Int
                var largerThanMaxDepth = false

                // Obtaining the row that should be used and checking if the thread should quit
                var done = false
                val temp: Row?
                synchronized(mutex) {
                    if (row == null) {
                        done = true
                        temp = null
                    } else {
                        temp = row!!
                        row = row!!.next
                    }
                }

                if (done) break

                val internalRow = temp!!
                stack.add(internalRow)

                // Computing the depth that needs the row needs to be pruned until
                maxDepth = minOf(
                    internalRow.prunedDepth + cfind.minDeepeningIncrement,
                    internalRow.depth + (2 * cfind.originalMinDeepening)
                )

                if (internalRow.prunedDepth > maxDepth) largerThanMaxDepth = true
                if (!largerThanMaxDepth) num += maxDepth - internalRow.depth

                // Beginning the DFS round for that row
                var currentRow: Row = internalRow
                do {
                    if (stack.isEmpty()) {
                        synchronized(mutex) {
                            if (cfind.head!!.id == internalRow.id) cfind.head = internalRow.next
                            if (cfind.tail!!.id == internalRow.id) cfind.tail = internalRow.prev

                            internalRow.pop()

                            cfind.queueSize--
                            prunedCount++
                        }

                        break
                    }

                    // Get the current row that is going to be analysed
                    currentRow = stack.removeLast()
                    if (currentRow.depth == maxDepth) {
                        internalRow.prunedDepth = maxDepth
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

                        if (cfind.verbosity >= 0 && clearPartial && !cfind.stdin) {
                            cfind.t.cursor.move {
                                up(3 + clearLines)
                                startOfLine()
                                clearScreenAfterCursor()
                            }
                            cfind.t.cursor.hide(showOnExit = true)
                        }

                        println(
                            TextStyles.bold(
                                "\nChecked ${count - 1} / ${cfind.maxQueueSize} rows, " +
                                        "pruned ${(10000 - ((count - prunedCount) * 10000 / count)) / 100.0}%"
                            )
                        )
                        clearLines = cfind.printRLE(grid)
                        clearPartial = true
                    }
                }
            }
        }
        threadLst.add(thread)
        thread.start()
    }

    // Wait for all threads to be done
    threadLst.forEach { it.join() }
    return num
}