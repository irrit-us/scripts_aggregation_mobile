package com.scripthost.notify

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.test.core.app.ApplicationProvider
import androidx.work.Data
import androidx.work.ListenableWorker.Result
import androidx.work.testing.TestListenableWorkerBuilder
import com.google.common.truth.Truth.assertThat
import com.scripthost.TestApplication
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Unit tests for [OneShotNotificationWorker] verifying that valid input data
 * results in a posted notification and missing input data fails the work.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = TestApplication::class)
class OneShotNotificationWorkerTest {

    private lateinit var application: Application

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun doWork_withInputData_postsNotificationAndSucceeds() {
        val inputData = Data.Builder()
            .putString(OneShotNotificationWorker.KEY_TITLE, "Reminder")
            .putString(OneShotNotificationWorker.KEY_MESSAGE, "One-shot fired")
            .build()
        val worker = TestListenableWorkerBuilder
            .from(application, OneShotNotificationWorker::class.java)
            .setInputData(inputData)
            .build()

        val result = worker.doWork()

        assertThat(result).isEqualTo(Result.success())
        val shadowNotificationManager =
            shadowOf(application.getSystemService(NotificationManager::class.java))
        assertThat(shadowNotificationManager.allNotifications).hasSize(1)
        val posted = shadowNotificationManager.allNotifications[0]
        assertThat(posted.extras.getString(Notification.EXTRA_TITLE))
            .isEqualTo("Reminder")
        assertThat(posted.extras.getString(Notification.EXTRA_TEXT))
            .isEqualTo("One-shot fired")
        val channelIds = shadowNotificationManager.notificationChannels
            .map { (it as NotificationChannel).id }
        assertThat(channelIds).contains("scripthost_scheduled")
    }

    @Test
    fun doWork_withoutInputData_fails() {
        val worker = TestListenableWorkerBuilder
            .from(application, OneShotNotificationWorker::class.java)
            .build()

        val result = worker.doWork()

        assertThat(result).isEqualTo(Result.failure())
        val shadowNotificationManager =
            shadowOf(application.getSystemService(NotificationManager::class.java))
        assertThat(shadowNotificationManager.allNotifications).isEmpty()
    }
}
