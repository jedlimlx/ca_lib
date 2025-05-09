package search.cfind

import com.github.ajalt.mordant.rendering.TextStyles
import java.util.concurrent.Semaphore
import kotlin.math.max
import kotlin.random.Random
import kotlin.time.TimeSource

fun processSuccessors(currentRow: Row, successors: List<Row>): List<Row> = if (currentRow.successorSequence != null) {
    // This optimisation is possible because of the nature of depth-first search
    // The successful branch will lie in-between the unknown branches and the deadends
    val sequence = currentRow.successorSequence!!
    val index = sequence[0]
    if (sequence.size > 1) successors[index].successorSequence = sequence.copyOfRange(1, sequence.size)
    currentRow.successorSequence = null
    successors.subList(0, index + 1)
} else successors


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
                maxDepth = internalRow.prunedDepth + cfind.minDeepeningIncrement

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
                            cfind.maxDepth = max(internalRow.depth, cfind.maxDepth)

                            cfind.linkedListSize--
                            prunedCount++
                        }

                        break
                    }

                    // Get the current row that is going to be analysed
                    currentRow = stack.removeLast()
                    if (currentRow.depth == maxDepth) {
                        // Adding the successor sequence to the row
                        val predecessors = currentRow.getAllPredecessors(
                            maxDepth - internalRow.depth, deepCopy = false
                        ).reversed()
                        internalRow.successorSequence = IntArray(maxDepth - internalRow.depth) { predecessors[it].successorNum }
                        break
                    }

                    // Get the rows that will need to be used to find the next row
                    val (rows, lookaheadRows) = cfind.extractRows(currentRow)
                    val successors = cfind.nextRow(currentRow, rows, lookaheadRows, depth = currentRow.depth + 1).first

                    // Adding the successors to the stack
                    stack.addAll(processSuccessors(currentRow, successors))
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

                        cfind.println(
                            TextStyles.bold(
                                "\nChecked ${count - 1} / ${cfind.maxQueueSize} rows, " +
                                        "pruned ${(10000 - ((count - prunedCount) * 10000 / count)) / 100.0}%"
                            )
                        )
                        clearLines = cfind.printRLE(grid, write=count.mod(cfind.partialFileFrequency) == 0)
                        clearPartial = true
                    } else if (count.mod(cfind.partialFileFrequency) == 0) {
                        val grid = currentRow.toGrid(cfind.period, cfind.symmetry)
                        grid.rule = cfind.rule
                        
                        cfind.printRLE(grid, write=true, print=false)
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

actual fun multithreadedPriorityQueue(cfind: CFind) {
    // Take note of the starting time
    val timeSource = TimeSource.Monotonic
    val startTime = timeSource.markNow()

    // Synchronisation primitives
    val mutex = Object()
    val equivalentStatesMutex = Object()
    val printingMutex = Object()
    val anyProcessingMutex = Object()
    val quitMutex = Object()

    val anyProcessing = Semaphore(1)

    // Other shared variables
    var count = 0
    var backups = 0
    var pruning = 0.8
    var shipsFound = 0
    var numProcessing = 0
    var longestPartialSoFar = cfind.priorityQueue.peek().depth

    var clearPartial = false
    var clearLines = 0

    var quitProcessing = false

    fun printPartials(currentRow: Row, message: Any, numLines: Int = 3, forcePrint: Boolean = false) {
        if ((count++).mod(cfind.partialFrequency) == 0 || forcePrint) {
            val grid = currentRow.toGrid(cfind.period, cfind.symmetry)
            grid.rule = cfind.rule

            if (cfind.verbosity >= 0 && clearPartial && !cfind.stdin) {
                cfind.t.cursor.move {
                    up(numLines + clearLines)
                    startOfLine()
                    clearScreenAfterCursor()
                }
                cfind.t.cursor.hide(showOnExit = true)
            }
            
            cfind.println(message)
            clearLines = cfind.printRLE(grid, write=count.mod(cfind.partialFileFrequency) == 0)
            clearPartial = true
        } else if (count.mod(cfind.partialFileFrequency) == 0) {
            val grid = currentRow.toGrid(cfind.period, cfind.symmetry)
            grid.rule = cfind.rule
            
            cfind.printRLE(grid, write=true, print=false)
        }
    }

    val threads = arrayListOf<Thread>()
    for (i in 0 ..< cfind.numThreads) {
        val thread = Thread {
            Thread.sleep(i * 50L)

            // Begin the search
            var row: Row
            var currentRow: Row

            var head: Row?
            var tail: Row?
            var stackSize: Int
            while (true) {
                // Check if the queue is empty
                var emptyQueue = false
                synchronized(quitMutex) {
                    if (cfind.priorityQueue.isEmpty()) {
                        quitProcessing = true
                        anyProcessing.acquire()
                        quitProcessing = false
                        if (cfind.priorityQueue.isEmpty()) {
                            emptyQueue = true
                            row = Row(null, intArrayOf(0), cfind)
                        } else {
                            row = cfind.priorityQueue.poll()
                            cfind.maxDepth = max(row.depth, cfind.maxDepth)
                        }
                        anyProcessing.release()
                    } else synchronized(mutex) {
                        row = cfind.priorityQueue.poll()
                        cfind.maxDepth = max(row.depth, cfind.maxDepth)
                    }
                }

                if (emptyQueue) break

                // Acquire the saving state lock
                synchronized(anyProcessingMutex) {
                    if (++numProcessing == 1) anyProcessing.acquire()
                }

                head = row
                tail = row
                stackSize = 0
                row.next = null
                row.prev = null

                // Decide what depth we should reach
                val maxDepth = row.prunedDepth + cfind.minDeepeningIncrement
                val roundStartTime = timeSource.markNow()

                do {
                    // Check if stack is empty
                    if (tail == null) {
                        pruning = 0.99 * pruning + 0.01
                        break
                    }

                    // Get the current row that is going to be analysed
                    currentRow = tail
                    tail = tail.prev
                    if (tail == null) head = null
                    stackSize--

                    // Check if we should exit this round
                    if (
                        currentRow.depth == maxDepth || quitProcessing ||
                        (timeSource.markNow() - roundStartTime).inWholeSeconds > cfind.maxTimePerRound
                    ) {
                        pruning *= 0.99

                        // Compute the predecessors
                        val predecessors = currentRow.getAllPredecessors(
                            currentRow.depth - row.depth, deepCopy = false
                        ).reversed()

                        // Decide how many rows to add to the priority queue
                        var rowsAdded = 0
                        var finalDepth = -1
                        val maxRowsAdded = (cfind.maxQueueSize / (cfind.priorityQueue.size + 0.0001) * (1.0 - pruning)).toInt()
                        for (depth in row.depth + 1..currentRow.depth) {
                            var curr = head
                            val lst = arrayListOf<Row>()
                            while (curr != null) {
                                if (curr.depth == depth) lst.add(curr)
                                curr = curr.next
                            }

                            rowsAdded += lst.size

                            if (rowsAdded < 1.5 * maxRowsAdded || depth == row.depth + 1) {
                                synchronized(mutex) { lst.forEach { cfind.priorityQueue.add(it) } }
                                finalDepth = depth
                            } else break
                        }

                        if (finalDepth == -1) finalDepth = currentRow.depth
                        val temp = currentRow.getPredecessor(currentRow.depth - finalDepth)!!

                        // Adding the successor sequence to the row
                        if (currentRow.depth > temp.depth)
                            temp.successorSequence = IntArray(currentRow.depth - temp.depth) {
                                predecessors[temp.depth - row.depth + it].successorNum
                            }

                        synchronized(mutex) { cfind.priorityQueue.add(temp) }
                        break
                    }

                    var foundEnoughShips = false
                    var equivalentState = false
                    synchronized(equivalentStatesMutex) {
                        // Check if the ship is completed
                        if (cfind.checkCompletedShip(currentRow)) {
                            clearPartial = false
                            if (++shipsFound == cfind.numShips) foundEnoughShips = true
                        }

                        // Check the transposition table for looping components
                        if (currentRow.successorSequence == null &&
                            cfind.checkEquivalentState(currentRow)) equivalentState = true
                    }
                    if (foundEnoughShips) break
                    if (equivalentState) continue

                    // Get the rows that will need to be used to find the next row
                    val (rows, lookaheadRows) = cfind.extractRows(currentRow)
                    val successors = cfind.nextRow(currentRow, rows, lookaheadRows, depth = currentRow.depth + 1).first

                    // Adding the new rows to the stack
                    processSuccessors(currentRow, successors).forEach {
                        if (tail != null) {
                            tail!!.next = it
                            it.prev = tail
                        } else head = it

                        tail = it
                        stackSize++
                    }

                    // Printing out the partials
                    if (cfind.verbosity >= 0) {
                        synchronized(printingMutex) {
                            // Insert some randomness in order to increase variety of partials shown to user
                            if (currentRow.depth > longestPartialSoFar && Random.nextInt(1, 5) == 1) {
                                longestPartialSoFar = currentRow.depth
                                printPartials(
                                    currentRow,
                                    TextStyles.bold("\nDepth: ${currentRow.depth}"),
                                    numLines = 4,
                                    forcePrint = true
                                )
                                clearPartial = false
                                clearLines--
                            } else {
                                printPartials(
                                    currentRow,
                                    TextStyles.bold(
                                        "\nPriority Queue Size: ${cfind.priorityQueue.size} / ${cfind.maxQueueSize}" +
                                                "\nThread $i / ${numProcessing}, " +
                                                "Stack Size: ${stackSize}, Depth: ${currentRow.depth} / $maxDepth"
                                    ), numLines = 4
                                )
                            }
                        }
                    }
                } while (true)

                // Release the saving state lock
                synchronized(anyProcessingMutex) {
                    if (--numProcessing == 0) anyProcessing.release()
                }

                // Check if sufficiently many ships have been found
                var foundEnoughShips = false
                synchronized(equivalentStatesMutex) { if (shipsFound == cfind.numShips) foundEnoughShips = true }
                if (foundEnoughShips) break

                // Check how much time has past and see if we need to write to a backup
                if ((timeSource.markNow() - startTime).inWholeMilliseconds > (backups+1)*cfind.backupFrequency*1000) {
                    synchronized(quitMutex) {
                        quitProcessing = true
                        anyProcessing.acquire()
                        quitProcessing = false
                    }
                    if ((timeSource.markNow() - startTime).inWholeMilliseconds > (backups+1)*cfind.backupFrequency*1000)
                        cfind.backupState("${cfind.backupName}_${backups++}.txt", cfind.saveState())
                    anyProcessing.release()
                }
            }
        }
        thread.start()
        threads.add(thread)
    }

    threads.forEach { it.join() }
}