package search.cfind

/**
 * The various search strategies avaliable for CFind to use.
 * - HYBRID_BFS is the search strategy used by gfind and qfind. The breath-first search round runs until
 * the queue reaches a maximum size, at which point, a depth-first search round will take over and check
 * if any of the rows cannot be extended by a given number of times. The depth-first round is multi-threaded
 * like that which is done in qfind.
 * - DFS is the search strategy that is used by zfind. This is just a pure depth-first search algorithm,
 * which consumes very little memory.
 * - PRIORITY_QUEUE is the search strategy used by ikpx2. A priority queue of partials is maintained and
 * each thread will pop out one from the queue to extend by a given depth. If the partial cannot be extended
 * it is discarded. If the partial can be extended, some of the initial rows from the DFS tree are added
 * back into the priority queue.
 */
enum class SearchStrategy {
    HYBRID_BFS,
    DFS,
    PRIORITY_QUEUE
}