package com.scripthost.notify

import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import androidx.test.core.app.ApplicationProvider
import androidx.work.Data
import androidx.work.ListenableWorker.Result
import androidx.work.testing.TestWorkerBuilder
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Unit tests for [DailyNotificationWorker] verifying that valid input data
 * results in a posted notification and missing input data fails the work.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class DailyNotificationWorkerTest {

    private lateinit var application: Application

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun doWork_withInputData_postsNotificationAndSucceeds() {
        val inputData = Data.Builder()
            .putString(DailyNotificationWorker.KEY_TITLE, "Daily Reminder")
            .putString(DailyNotificationWorker.KEY_MESSAGE, "Time to run your script")
            .build()
        val worker = TestWorkerBuilder<DailyNotificationWorker>(
            application,
            inputData = inputData
        ).build()

        val result = worker.doWork()

        assertThat(result).isEqualTo(Result.success())
        val shadowNotificationManager =
            shadowOf(application.getSystemService(NotificationManager::class.java))
        assertThat(shadowNotificationManager.postedNotifications).hasSize(1)
        val posted = shadowNotificationManager.postedNotifications[0]
        assertThat(posted.extras.getString(Notification.EXTRA_TITLE))
            .isEqualTo("Daily Reminder")
        assertThat(posted.extras.getString(Notification.EXTRA_TEXT))
            .isEqualTo("Time to run your script")
        assertThat(shadowNotificationManager.getNotificationChannel("scripthost_scheduled"))
            .isNotNull()
    }

    @Test
    fun doWork_withoutInputData_fails() {
        val worker = TestWorkerBuilder<DailyNotificationWorker>(application).build()

        val result = worker.doWork()

        assertThat(result).isEqualTo(Result.failure())
        val shadowNotificationManager =
            shadowOf(application.getSystemService(NotificationManager::class.java))
        assertThat(shadowNotificationManager.postedNotifications).isEmpty()
    }
}
