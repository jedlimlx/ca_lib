data class Node<E>(val value: E, var prev: Node<E>?, var next: Node<E>?)

data class LRUCacheEntry<K, V>(override val key: K, override var value: V): MutableMap.MutableEntry<K, V> {
    override fun setValue(newValue: V): V {
        value = newValue
        return value
    }
}

class LRUCache<K, V>(
    val maxSize: Int,
) : MutableMap<K, V> {
    override val size: Int
        get() = map.size

    val map = HashMap<K, Node<Pair<K, V>>>()

    private var head: Node<Pair<K, V>>? = null
    private var tail: Node<Pair<K, V>>? = null

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>> = map.entries.map {
        LRUCacheEntry(it.key, it.value.value.second)
    }.toMutableSet()
    override val keys: MutableSet<K> = map.keys
    override val values: MutableCollection<V> = map.values.map { it.value.second }.toMutableSet()

    override fun containsKey(key: K): Boolean = key in map

    override fun containsValue(value: V): Boolean = values.contains(value)

    override fun get(key: K): V? {
        if (map[key]?.value?.second != null)
            return put(key, map[key]!!.value.second)
        return null
    }

    override fun clear() = map.clear()

    override fun isEmpty() = map.isEmpty()

    override fun remove(key: K): V? {
        val node = map.remove(key) ?: return null
        if (node.prev != null && node.next != null) {
            node.prev!!.next = node.next
            node.next!!.prev = node.prev
        } else if (node.prev != null && node.next == null) {
            tail = node.prev
        } else if (node.prev == null && node.next != null) {
            head = node.next
        }

        return node.value.second
    }

    override fun putAll(from: Map<out K, V>) = from.forEach { put(it.key, it.value) }

    override fun put(key: K, value: V): V? {
        if (key in map) remove(key)
        if (map.size > maxSize) remove(head!!.value.first)

        val node = Node(Pair(key, value), tail, null)
        if (head == null) head = node
        if (tail != null) tail!!.next = node

        tail = node
        map.put(key, node)?.value?.second
        return value
    }
}