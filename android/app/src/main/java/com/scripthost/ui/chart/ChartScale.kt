package com.scripthost.ui.chart

/**
 * Scaling helpers for [SimpleChartView]. Pure Kotlin with no Android
 * dependencies so it can be exercised by plain JVM unit tests.
 */
object ChartScale {

    /**
     * Value range of [data]. Empty input yields 0..1; a degenerate range
     * (all values equal) is padded by +/-1 so charts never divide by zero.
     */
    fun rangeOf(data: List<Float>): Pair<Float, Float> {
        if (data.isEmpty()) return 0f to 1f
        var min = data[0]
        var max = data[0]
        for (value in data) {
            if (value < min) min = value
            if (value > max) max = value
        }
        if (min == max) return (min - 1f) to (max + 1f)
        return min to max
    }

    /** Normalized 0..1 position of [value] within [min]..[max], clamped. */
    fun normalize(value: Float, min: Float, max: Float): Float {
        if (max <= min) return 0f
        return ((value - min) / (max - min)).coerceIn(0f, 1f)
    }

    /** [count] + 1 evenly spaced tick values from [min] to [max], inclusive. */
    fun niceTicks(min: Float, max: Float, count: Int = 4): List<Float> {
        if (count <= 0) return listOf(min)
        return (0..count).map { min + (max - min) * it / count }
    }

    /**
     * Parse "#RRGGBB" or "#AARRGGBB" into an ARGB int. Implemented without
     * android.graphics.Color so JVM tests can call it; returns [fallback]
     * for null or malformed input.
     */
    fun parseColorOr(hex: String?, fallback: Int): Int {
        val digits = hex?.trim()?.removePrefix("#") ?: return fallback
        if (digits.length != 6 && digits.length != 8) return fallback
        if (!digits.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) return fallback
        val value = digits.toLong(16)
        return if (digits.length == 6) (0xFF000000L or value).toInt() else value.toInt()
    }
}
