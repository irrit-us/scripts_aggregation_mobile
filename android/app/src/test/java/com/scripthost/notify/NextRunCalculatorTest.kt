package com.scripthost.notify

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import java.util.Calendar

/**
 * Unit tests for [NextRunCalculator] using explicitly built Calendar instances.
 */
class NextRunCalculatorTest {

    private fun calendar(hour: Int, minute: Int, second: Int = 0): Calendar {
        return Calendar.getInstance().apply {
            set(2024, Calendar.JANUARY, 15, hour, minute, second)
            set(Calendar.MILLISECOND, 0)
        }
    }

    @Test
    fun millisUntilNext_laterSameDay_returnsDelayUntilTarget() {
        val now = calendar(10, 0)
        // 10:00 -> 14:30 = 4.5 hours
        assertThat(NextRunCalculator.millisUntilNext(now, 14, 30)).isEqualTo(16_200_000L)
    }

    @Test
    fun millisUntilNext_earlierTime_rollsToNextDay() {
        val now = calendar(18, 0)
        // 18:00 -> 09:00 next day = 15 hours
        assertThat(NextRunCalculator.millisUntilNext(now, 9, 0)).isEqualTo(54_000_000L)
    }

    @Test
    fun millisUntilNext_exactNow_rollsToNextDay() {
        val now = calendar(9, 30)
        // Exactly 09:30:00.000 -> next occurrence is 24 hours later
        assertThat(NextRunCalculator.millisUntilNext(now, 9, 30)).isEqualTo(86_400_000L)
    }

    @Test
    fun millisUntilNext_endOfDayBoundary() {
        val now = calendar(23, 58)
        // 23:58 -> 23:59 = 1 minute
        assertThat(NextRunCalculator.millisUntilNext(now, 23, 59)).isEqualTo(60_000L)
    }

    @Test
    fun millisUntilNext_invalidHour_throws() {
        val now = calendar(12, 0)
        assertThrows(IllegalArgumentException::class.java) {
            NextRunCalculator.millisUntilNext(now, -1, 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            NextRunCalculator.millisUntilNext(now, 24, 0)
        }
    }

    @Test
    fun millisUntilNext_invalidMinute_throws() {
        val now = calendar(12, 0)
        assertThrows(IllegalArgumentException::class.java) {
            NextRunCalculator.millisUntilNext(now, 12, 60)
        }
    }
}
