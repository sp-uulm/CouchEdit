package de.uulm.se.couchedit.util.collection

import java.util.*
import kotlin.collections.HashMap

/**
 * Cache store with a Time-To-Live after which contents are purged.
 */
class TTLMap<K, V>(val map: MutableMap<K, V> = mutableMapOf(), var ttl: Int = 60 * 1000) : Map<K, V> by map {
    /**
     * Stores last access time for every key in [map]
     */
    private val accessTimes = HashMap<K, Long>()

    /**
     * Ordered set of keys in its order of access
     */
    private val accessOrder = TreeSet<K> { key1, key2 ->
        accessTimes[key1]?.compareTo(accessTimes[key2] ?: -1) ?: -1
    }

    override fun get(key: K): V? {
        updateTimeStamp(key)

        return map[key]
    }

    fun remove(key: K): V? {
        accessOrder.remove(key)
        accessTimes.remove(key)

        return map.remove(key)
    }

    fun put(key: K, value: V): V? {
        updateTimeStamp(key)

        return map.put(key, value)
    }

    fun putAll(from: Map<out K, V>) {
        val time = System.currentTimeMillis()

        synchronized(accessTimes) {
            accessTimes.putAll(from.keys.map { Pair(it, time) })
            accessOrder.removeAll(from.keys)
            accessOrder.addAll(from.keys)
        }

        return map.putAll(from)
    }

    /**
     * Removes all items that were stored at an earlier point in time than [TTL]
     */
    fun cleanUp() {
        val currentTime = System.currentTimeMillis()

        synchronized(accessTimes) {
            do {
                val oldest = accessOrder.first()
                val oldestTime = (accessTimes[oldest] ?: currentTime)

                val hasOlderThanTTL = oldestTime <= currentTime - ttl

                if (hasOlderThanTTL) {
                    this.remove(oldest)
                }
            } while (hasOlderThanTTL)
        }
    }

    /**
     * Registers the current time as the timestamp for [key]
     */
    private fun updateTimeStamp(key: K) {
        synchronized(accessTimes) {
            accessTimes[key] = System.currentTimeMillis()
            accessOrder.remove(key)
            accessOrder.add(key)
        }
    }
}
