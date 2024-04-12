import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class DataStructureTest {
    @Test
    fun priorityQueueTest() {
        var size = 0
        val priorityQueue = PriorityQueue<Int>(10)
        for (i in 0 .. 10000) {
            if (Random.nextInt(2) == 0) {
                priorityQueue.add(Random.nextInt(100))
                size++
            } else if (priorityQueue.isNotEmpty())  {
                priorityQueue.poll()
                size--
            }
        }

        assertEquals(size, priorityQueue.size)  // check the size is correct

        val list = arrayListOf<Int>()
        while (priorityQueue.isNotEmpty()) { list.add(priorityQueue.poll()) }
        assertEquals(list, list.sorted().toList())  // check that elements will get spat out in a sorted order
    }

    @Test
    fun lruCacheTest() {
        val lruCache = LRUCache<Int, String>(10)
        for (i in 0 .. 10000) {
            if (Random.nextInt(3) != 0) {
                val num = Random.nextInt(100)
                lruCache[num] = num.toString()
            } else if (lruCache.size > 0) {
                lruCache.remove(Random.nextInt(100))
            }
        }

        lruCache.forEach { (key, value) -> assertEquals(key.toString(), value) }
    }
}