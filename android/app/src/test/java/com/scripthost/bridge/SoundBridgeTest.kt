package com.scripthost.bridge

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.math.abs

/**
 * Unit tests for [SoundBridge.synthesizeSine]: sample count, amplitude
 * bounds, volume scaling, and the click-avoiding fades at both ends.
 * Pure JVM math — no Robolectric needed.
 */
class SoundBridgeTest {

    @Test
    fun synthesizeSine_sampleCount_matchesDurationAndSampleRate() {
        val samples = SoundBridge.synthesizeSine(440.0, 100, 22050, 1.0f)
        assertThat(samples.size).isEqualTo(100 * 22050 / 1000)
    }

    @Test
    fun synthesizeSine_samples_stayWithinAmplitudeBounds() {
        val samples = SoundBridge.synthesizeSine(440.0, 500, 22050, 1.0f)
        for (sample in samples) {
            assertThat(abs(sample.toInt())).isAtMost(Short.MAX_VALUE.toInt())
        }
    }

    @Test
    fun synthesizeSine_respectsVolumeScaling() {
        val full = SoundBridge.synthesizeSine(440.0, 500, 22050, 1.0f)
        val half = SoundBridge.synthesizeSine(440.0, 500, 22050, 0.5f)

        // Compare steady-state peaks, skipping the fade regions
        val fade = 22050 * 5 / 1000
        val fullPeak = full.drop(fade).dropLast(fade).maxOf { abs(it.toInt()) }
        val halfPeak = half.drop(fade).dropLast(fade).maxOf { abs(it.toInt()) }

        assertThat(fullPeak).isGreaterThan(0)
        assertThat(halfPeak.toDouble())
            .isWithin(fullPeak * 0.01)
            .of(fullPeak * 0.5)
    }

    @Test
    fun synthesizeSine_fadesToZeroAtBothEnds() {
        val samples = SoundBridge.synthesizeSine(440.0, 500, 22050, 1.0f)
        assertThat(samples.first().toInt()).isEqualTo(0)
        assertThat(samples.last().toInt()).isEqualTo(0)
    }

    @Test
    fun synthesizeSine_clampsVolumeAboveOne() {
        val clamped = SoundBridge.synthesizeSine(440.0, 500, 22050, 2.0f)
        for (sample in clamped) {
            assertThat(abs(sample.toInt())).isAtMost(Short.MAX_VALUE.toInt())
        }
    }
}
