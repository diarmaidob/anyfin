package io.github.diarmaidob.anyfin.core.common

import java.time.Instant
import java.time.format.DateTimeParseException

object JellyfinUtils {
    private const val TICKS_PER_MS = 10_000L

    fun ticksToMillis(ticks: Long?): Long {
        return (ticks ?: 0L) / TICKS_PER_MS
    }

    fun millisToTicks(millis: Long): Long {
        return millis * TICKS_PER_MS
    }

    fun parseIsoDate(dateString: String?): Instant {
        if (dateString.isNullOrBlank()) return Instant.now()
        return try {
            Instant.parse(dateString)
        } catch (e: DateTimeParseException) {
            Instant.now()
        }
    }
}