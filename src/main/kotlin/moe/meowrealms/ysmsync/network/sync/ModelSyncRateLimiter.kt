package moe.meowrealms.ysmsync.network.sync

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.ceil
import kotlin.math.max

class ModelSyncRateLimiter(
    private val mbps: Double
) {
    private val lock = ReentrantLock()
    private var nextAvailableAtNanos: Long = System.nanoTime()

    private val bytesPerNano: Double =
        if (mbps > 0.0) (mbps * 1_000_000.0 / 8.0) / 1_000_000_000.0 else Double.POSITIVE_INFINITY

    fun reset() {
        this.lock.withLock {
            this.nextAvailableAtNanos = System.nanoTime()
        }
    }

    fun reserveDelayTicks(bytes: Int): Long {
        if (this.mbps <= 0.0 || bytes <= 0) {
            return 0L
        }

        val delayNanos = this.lock.withLock {
            val now = System.nanoTime()
            val startAt = max(this.nextAvailableAtNanos, now)
            val transmitNanos = ceil(bytes / this.bytesPerNano).toLong()
            this.nextAvailableAtNanos = startAt + transmitNanos

            startAt - now
        }

        return ceil(delayNanos / NANOS_PER_TICK.toDouble()).toLong()
    }

    companion object {
        private const val NANOS_PER_TICK = 50_000_000L
    }
}
