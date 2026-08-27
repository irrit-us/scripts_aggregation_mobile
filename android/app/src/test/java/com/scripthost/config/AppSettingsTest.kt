package com.scripthost.config

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.scripthost.TestApplication
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for [AppSettings] covering preference defaults and round-trips.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = TestApplication::class)
class AppSettingsTest {

    private lateinit var settings: AppSettings

    @Before
    fun setUp() {
        val application: Application = ApplicationProvider.getApplicationContext()
        settings = AppSettings(application)
    }

    @Test
    fun engineTimeoutSeconds_defaultsToThirty() {
        assertThat(settings.engineTimeoutSeconds)
            .isEqualTo(AppSettings.DEFAULT_ENGINE_TIMEOUT_SECONDS)
    }

    @Test
    fun engineTimeoutSeconds_roundTrips() {
        settings.engineTimeoutSeconds = 120
        assertThat(settings.engineTimeoutSeconds).isEqualTo(120)
        settings.engineTimeoutSeconds = AppSettings.DEFAULT_ENGINE_TIMEOUT_SECONDS
    }

    @Test
    fun debugMode_roundTrips() {
        settings.debugMode = true
        assertThat(settings.debugMode).isTrue()
        settings.debugMode = false
        assertThat(settings.debugMode).isFalse()
    }
}
