package io.github.diarmaidob.anyfin.core.common

import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import kotlin.time.Duration


class ActionThrottler @Inject constructor() {


    private val lastRunTime = AtomicLong(0)

    fun attempt(windowDuration: Duration, block: () -> Unit) {
        if (checkAndMark(windowDuration)) {
            block()
        }
    }

    suspend fun <T> attemptSuspend(
        windowDuration: Duration,
        block: suspend () -> T
    ): T? {
        if (checkAndMark(windowDuration)) {
            return block()
        }
        return null
    }

    fun <T> force(block: () -> T): T? {
        lastRunTime.set(System.currentTimeMillis())
        return block()
    }

    suspend fun <T> forceSuspend(block: suspend () -> T): T {
        lastRunTime.set(System.currentTimeMillis())
        return block()
    }

    private fun checkAndMark(windowDuration: Duration): Boolean {
        val now = System.currentTimeMillis()
        val last = lastRunTime.get()
        if (now - last > windowDuration.inWholeMilliseconds) {
            lastRunTime.set(now)
            return true
        }
        return false
    }
}