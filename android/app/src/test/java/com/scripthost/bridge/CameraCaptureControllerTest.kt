package com.scripthost.bridge

import com.google.common.truth.Truth.assertThat
import com.scripthost.ui.CameraCaptureController
import org.junit.Test

/**
 * Unit tests for [CameraCaptureController.scaledDimensions]: longest side
 * scaled to the cap, aspect ratio preserved, smaller images unchanged.
 * Pure JVM math — no Robolectric needed.
 */
class CameraCaptureControllerTest {

    @Test
    fun scaledDimensions_landscape_scalesLongestSideToMax() {
        val (w, h) = CameraCaptureController.scaledDimensions(2560, 1440, 1280)
        assertThat(w).isEqualTo(1280)
        assertThat(h).isEqualTo(720)
    }

    @Test
    fun scaledDimensions_portrait_scalesLongestSideToMax() {
        val (w, h) = CameraCaptureController.scaledDimensions(1440, 2560, 1280)
        assertThat(w).isEqualTo(720)
        assertThat(h).isEqualTo(1280)
    }

    @Test
    fun scaledDimensions_smallerImage_leftUnchanged() {
        val (w, h) = CameraCaptureController.scaledDimensions(800, 600, 1280)
        assertThat(w).isEqualTo(800)
        assertThat(h).isEqualTo(600)
    }

    @Test
    fun scaledDimensions_exactlyAtMax_leftUnchanged() {
        val (w, h) = CameraCaptureController.scaledDimensions(1280, 720, 1280)
        assertThat(w).isEqualTo(1280)
        assertThat(h).isEqualTo(720)
    }

    @Test
    fun scaledDimensions_preservesAspectRatio() {
        val (w, h) = CameraCaptureController.scaledDimensions(3000, 1000, 1280)
        assertThat(w).isEqualTo(1280)
        val dstAspect = w.toDouble() / h.toDouble()
        assertThat(dstAspect).isWithin(0.01).of(3.0)
    }
}
