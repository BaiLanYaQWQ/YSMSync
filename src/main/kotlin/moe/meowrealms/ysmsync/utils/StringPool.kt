package moe.meowrealms.ysmsync.utils

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

object StringPool {
    const val NONE: Int = Int.MIN_VALUE

    private val COUNTER: AtomicInteger = AtomicInteger(0)

    private val POOL: ConcurrentHashMap<String, Int> = ConcurrentHashMap<String, Int>()

    private val MAP: ConcurrentHashMap<Int, String> = ConcurrentHashMap<Int, String>()

    const val EMPTY: String = ""

    val EMPTY_ID: Int = computeIfAbsent(EMPTY)

    fun computeIfAbsent(str: String): Int {
        return POOL.computeIfAbsent(str) { k ->
            val name = COUNTER.incrementAndGet()

            MAP[name] = k

            name
        }
    }

    fun getName(str: String?): Int {
        return POOL.getOrDefault(str, NONE)
    }

    fun getString(name: Int): String {
        return MAP.getOrDefault(name, EMPTY)
    }
}