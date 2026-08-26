package com.scripthost.notify

import java.util.Calendar

/**
 * Computes the delay until the next local occurrence of a wall-clock time.
 * Pure JVM logic (java.util.Calendar only) so it is unit-testable.
 */
object NextRunCalculator {

    /**
     * Milliseconds from [now] until the next occurrence of [hour]:[minute].
     * A time later today is used; if it has already passed (or is exactly now)
     * the occurrence falls on the next day.
     */
    fun millisUntilNext(now: Calendar, hour: Int, minute: Int): Long {
        require(hour in 0..23) { "hour must be in 0..23, was $hour" }
        require(minute in 0..59) { "minute must be in 0..59, was $minute" }

        val target = now.clone() as Calendar
        target.set(Calendar.HOUR_OF_DAY, hour)
        target.set(Calendar.MINUTE, minute)
        target.set(Calendar.SECOND, 0)
        target.set(Calendar.MILLISECOND, 0)

        if (target.timeInMillis <= now.timeInMillis) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }

        return target.timeInMillis - now.timeInMillis
    }
}
