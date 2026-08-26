package com.scripthost.ui.chart

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Unit tests for [ChartScale].
 */
class ChartScaleTest {

    @Test
    fun rangeOf_emptyList_returnsDefaultRange() {
        assertThat(ChartScale.rangeOf(emptyList())).isEqualTo(0f to 1f)
    }

    @Test
    fun rangeOf_singleValue_isPaddedByOne() {
        assertThat(ChartScale.rangeOf(listOf(5f))).isEqualTo(4f to 6f)
    }

    @Test
    fun rangeOf_allEqualValues_isPaddedByOne() {
        assertThat(ChartScale.rangeOf(listOf(-2f, -2f, -2f))).isEqualTo(-3f to -1f)
    }

    @Test
    fun rangeOf_negativeValues_returnsExactMinMax() {
        assertThat(ChartScale.rangeOf(listOf(2.5f, -7.5f, 0f))).isEqualTo(-7.5f to 2.5f)
    }

    @Test
    fun normalize_midpoint_returnsHalf() {
        assertThat(ChartScale.normalize(5f, 0f, 10f)).isWithin(0.0001f).of(0.5f)
    }

    @Test
    fun normalize_outOfRangeValues_areClamped() {
        assertThat(ChartScale.normalize(-5f, 0f, 10f)).isWithin(0.0001f).of(0f)
        assertThat(ChartScale.normalize(15f, 0f, 10f)).isWithin(0.0001f).of(1f)
        assertThat(ChartScale.normalize(5f, 5f, 5f)).isWithin(0.0001f).of(0f)
    }

    @Test
    fun niceTicks_returnsCountPlusOneAscendingValues() {
        val ticks = ChartScale.niceTicks(0f, 10f, 4)
        assertThat(ticks).hasSize(5)
        assertThat(ticks).containsExactlyElementsIn(ticks.sorted()).inOrder()
    }

    @Test
    fun niceTicks_endpointsMatchMinAndMax() {
        val ticks = ChartScale.niceTicks(-4f, 8f)
        assertThat(ticks.first()).isWithin(0.0001f).of(-4f)
        assertThat(ticks.last()).isWithin(0.0001f).of(8f)
    }

    @Test
    fun parseColorOr_rgbHex_appliesFullAlpha() {
        assertThat(ChartScale.parseColorOr("#FF3B30", 0)).isEqualTo(0xFFFF3B30.toInt())
    }

    @Test
    fun parseColorOr_argbHex_preservesAlpha() {
        assertThat(ChartScale.parseColorOr("#80FF3B30", 0)).isEqualTo(0x80FF3B30.toInt())
    }

    @Test
    fun parseColorOr_invalidOrNull_returnsFallback() {
        assertThat(ChartScale.parseColorOr("not-a-color", 42)).isEqualTo(42)
        assertThat(ChartScale.parseColorOr("#12345", 42)).isEqualTo(42)
        assertThat(ChartScale.parseColorOr(null, 42)).isEqualTo(42)
    }
}
