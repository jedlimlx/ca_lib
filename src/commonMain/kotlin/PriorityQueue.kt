import kotlin.Comparator

internal fun <T> Array<T>.exch(i: Int, j: Int) {
    val temp = this[i]
    this[i] = this[j]
    this[j] = temp
}

class PriorityQueue<T>(size: Int, val comparator: Comparator<T>? = null) : Collection<T> {
    public override var size: Int = 0
        private set
    private var arr = Array<Comparable<T>?>(size) { null } as Array<T?>

    fun add(element: T) {
        if (size + 1 == arr.size)
            resize()

        arr[++size] = element
        swim(size)
    }

    fun peek(): T {
        if (isEmpty()) throw NoSuchElementException()
        return arr[1]!!
    }

    fun poll(): T {
        if (isEmpty()) throw NoSuchElementException()

        val res = peek()
        arr.exch(1, size--)
        sink(1)
        arr[size + 1] = null
        if ((isNotEmpty()) && (size == (arr.size - 1) / 4)) {
            resize()
        }
        return res
    }

    private fun swim(n: Int) {
        Companion.swim(arr, n, comparator)
    }

    private fun sink(n: Int) {
        Companion.sink(arr, n, size, comparator)
    }

    private fun resize() {
        arr = Array(arr.size * 2) {
            if (it < size + 1) arr[it] as Comparable<T>?
            else null
        } as Array<T?>
    }

    override fun isEmpty(): Boolean = size == 0

    override fun contains(element: T): Boolean {
        for (obj in this)
            if (obj == element) return true
        return false
    }

    override fun containsAll(elements: Collection<T>): Boolean {
        for (element in elements)
            if (!contains(element)) return false
        return true
    }

    override fun iterator(): Iterator<T> {
        return arr.copyOfRange(1, size + 1).map { it!! }.iterator()
    }

    companion object {
        private fun<T> greater(arr: Array<T?>, i: Int, j: Int, comparator: Comparator<T>? = null): Boolean {
            if (comparator != null) {
                return comparator.compare(arr[i]!!, arr[j]!!) > 0
            } else {
                val left = arr[i]!! as Comparable<T>
                return left > arr[j]!!
            }
        }

        fun<T> sink(arr: Array<T?>, a: Int, size: Int, comparator: Comparator<T>? = null) {
            var k = a
            while (2 * k <= size) {
                var j = 2 * k
                if (j < size && greater(arr, j, j + 1, comparator)) j++
                if (!greater(arr, k, j, comparator)) break
                arr.exch(k, j)
                k = j
            }
        }

        fun<T> swim(arr: Array<T?>, size: Int, comparator: Comparator<T>? = null) {
            var n = size
            while (n > 1 && greater(arr, n / 2, n, comparator)) {
                arr.exch(n, n / 2)
                n /= 2
            }
        }
    }
}