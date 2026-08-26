package com.scripthost.bridge

import android.Manifest
import android.app.Activity
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import com.google.common.truth.Truth.assertThat
import com.scripthost.models.Permission
import com.scripthost.models.Script
import com.scripthost.security.PermissionManager
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.concurrent.atomic.AtomicReference

/**
 * Unit tests for [NotificationBridge] covering permission-gated posting and
 * scheduling/cancelling of daily notification work via WorkManager.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NotificationBridgeTest {

    private lateinit var application: Application
    private lateinit var permissionManager: PermissionManager
    private lateinit var bridge: NotificationBridge

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        WorkManagerTestInitHelper.initializeTestWorkManager(
            application,
            Configuration.Builder().build()
        )
        permissionManager = PermissionManager(application)
        grantNotificationsToScript(permissionManager, SCRIPT_ID)
        bridge = NotificationBridge(application, permissionManager, SCRIPT_ID)
    }

    @Test
    fun postNotification_withPermission_postsOnScriptsChannel() {
        bridge.postNotification("Hello", "World")

        val shadowNotificationManager =
            shadowOf(application.getSystemService(NotificationManager::class.java))
        assertThat(shadowNotificationManager.postedNotifications).hasSize(1)
        val posted = shadowNotificationManager.postedNotifications[0]
        assertThat(posted.extras.getString(Notification.EXTRA_TITLE)).isEqualTo("Hello")
        assertThat(posted.extras.getString(Notification.EXTRA_TEXT)).isEqualTo("World")
        assertThat(shadowNotificationManager.getNotificationChannel("scripthost_scripts"))
            .isNotNull()
    }

    @Test
    fun postNotification_withoutPermission_doesNotPost() {
        val unprivilegedBridge =
            NotificationBridge(application, PermissionManager(application), SCRIPT_ID)

        unprivilegedBridge.postNotification("Hello", "World")

        val shadowNotificationManager =
            shadowOf(application.getSystemService(NotificationManager::class.java))
        assertThat(shadowNotificationManager.postedNotifications).isEmpty()
    }

    @Test
    fun scheduleDaily_invalidHour_returnsFalseAndEnqueuesNothing() {
        assertThat(bridge.scheduleDaily("x", 25, 0, "Title", "Message")).isFalse()

        val infos = WorkManager.getInstance(application)
            .getWorkInfosForUniqueWork("daily_x").get()
        assertThat(infos).isEmpty()
    }

    @Test
    fun scheduleDaily_invalidMinute_returnsFalseAndEnqueuesNothing() {
        assertThat(bridge.scheduleDaily("x", 8, 75, "Title", "Message")).isFalse()

        val infos = WorkManager.getInstance(application)
            .getWorkInfosForUniqueWork("daily_x").get()
        assertThat(infos).isEmpty()
    }

    @Test
    fun scheduleDaily_validTime_enqueuesUniquePeriodicWork() {
        assertThat(bridge.scheduleDaily("x", 8, 0, "Title", "Message")).isTrue()

        val infos = WorkManager.getInstance(application)
            .getWorkInfosForUniqueWork("daily_x").get()
        assertThat(infos).isNotEmpty()
    }

    @Test
    fun scheduleDaily_withoutPermission_returnsFalse() {
        val unprivilegedBridge =
            NotificationBridge(application, PermissionManager(application), SCRIPT_ID)

        assertThat(unprivilegedBridge.scheduleDaily("x", 8, 0, "Title", "Message")).isFalse()

        val infos = WorkManager.getInstance(application)
            .getWorkInfosForUniqueWork("daily_x").get()
        assertThat(infos).isEmpty()
    }

    @Test
    fun cancelSchedule_removesEnqueuedWork() {
        assertThat(bridge.scheduleDaily("x", 8, 0, "Title", "Message")).isTrue()

        assertThat(bridge.cancelSchedule("x")).isTrue()

        val infos = WorkManager.getInstance(application)
            .getWorkInfosForUniqueWork("daily_x").get()
        assertThat(infos.all { it.state == WorkInfo.State.CANCELLED }).isTrue()
    }

    private fun grantNotificationsToScript(manager: PermissionManager, scriptId: String) {
        shadowOf(application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val script = Script(
            id = scriptId,
            name = "Test Script",
            version = "1.0.0",
            author = "Test Author",
            description = "A test script",
            permissions = listOf(Permission.NOTIFICATIONS),
            sourceCode = "console.log('hello');"
        )
        val granted = AtomicReference<Set<Permission>>()
        manager.requestPermissions(activity, script) { g, _ -> granted.set(g) }
        assertThat(granted.get()).contains(Permission.NOTIFICATIONS)
    }

    private companion object {
        const val SCRIPT_ID = "test-script"
    }
}
