package com.scripthost.security

import android.Manifest
import android.app.Activity
import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.scripthost.TestApplication
import com.scripthost.models.Permission
import com.scripthost.models.Script
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.concurrent.atomic.AtomicReference

/**
 * Unit tests for [PermissionManager] covering system-level and per-script
 * permission checks, including the API 33+ notification runtime permission.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class)
class PermissionManagerTest {

    private lateinit var application: Application
    private lateinit var permissionManager: PermissionManager

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        permissionManager = PermissionManager(application)
    }

    @Config(sdk = [28])
    @Test
    fun hasPermission_notifications_isTrueBelowApi33() {
        assertThat(permissionManager.hasPermission(Permission.NOTIFICATIONS)).isTrue()
    }

    @Config(sdk = [34])
    @Test
    fun hasPermission_notifications_isFalseWithoutGrant() {
        shadowOf(application).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        assertThat(permissionManager.hasPermission(Permission.NOTIFICATIONS)).isFalse()
    }

    @Config(sdk = [34])
    @Test
    fun hasPermission_notifications_isTrueAfterGrant() {
        shadowOf(application).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        shadowOf(application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        assertThat(permissionManager.hasPermission(Permission.NOTIFICATIONS)).isTrue()
    }

    @Test
    fun hasPermission_nonDangerousPermissions_areTrue() {
        assertThat(permissionManager.hasPermission(Permission.INTERNET)).isTrue()
        assertThat(permissionManager.hasPermission(Permission.SSH)).isTrue()
        assertThat(permissionManager.hasPermission(Permission.CONFIG)).isTrue()
    }

    @Test
    fun hasPermission_camera_isFalseByDefaultAndTrueAfterGrant() {
        shadowOf(application).denyPermissions(Manifest.permission.CAMERA)
        assertThat(permissionManager.hasPermission(Permission.CAMERA)).isFalse()

        shadowOf(application).grantPermissions(Manifest.permission.CAMERA)
        assertThat(permissionManager.hasPermission(Permission.CAMERA)).isTrue()
    }

    @Config(sdk = [34])
    @Test
    fun scriptPermissions_grantedAfterRequestAndClearedByRevoke() {
        shadowOf(application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val script = Script(
            id = "test-script",
            name = "Test Script",
            version = "1.0.0",
            author = "Test Author",
            description = "A test script",
            permissions = listOf(Permission.INTERNET, Permission.NOTIFICATIONS),
            sourceCode = "console.log('hello');"
        )

        assertThat(permissionManager.hasScriptPermission(script.id, Permission.INTERNET)).isFalse()

        val result = AtomicReference<Pair<Set<Permission>, Set<Permission>>>()
        permissionManager.requestPermissions(activity, script) { granted, denied ->
            result.set(granted to denied)
        }

        assertThat(result.get()).isNotNull()
        assertThat(result.get().first).contains(Permission.INTERNET)
        assertThat(result.get().first).contains(Permission.NOTIFICATIONS)
        assertThat(permissionManager.hasScriptPermission(script.id, Permission.INTERNET)).isTrue()
        assertThat(permissionManager.hasScriptPermission(script.id, Permission.NOTIFICATIONS)).isTrue()
        assertThat(permissionManager.hasScriptPermission(script.id, Permission.CAMERA)).isFalse()

        permissionManager.revokePermissions(script.id)
        assertThat(permissionManager.hasScriptPermission(script.id, Permission.INTERNET)).isFalse()
        assertThat(permissionManager.hasScriptPermission(script.id, Permission.NOTIFICATIONS)).isFalse()
    }

    @Config(sdk = [34])
    @Test
    fun revokePermission_removesOnlyThatPermission() {
        shadowOf(application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val script = Script(
            id = "test-script",
            name = "Test Script",
            version = "1.0.0",
            author = "Test Author",
            description = "A test script",
            permissions = listOf(Permission.INTERNET, Permission.NOTIFICATIONS),
            sourceCode = "console.log('hello');"
        )

        permissionManager.requestPermissions(activity, script) { _, _ -> }
        assertThat(permissionManager.getGrantedPermissions(script.id))
            .containsExactly(Permission.INTERNET, Permission.NOTIFICATIONS)

        permissionManager.revokePermission(script.id, Permission.NOTIFICATIONS)
        assertThat(permissionManager.getGrantedPermissions(script.id))
            .containsExactly(Permission.INTERNET)

        // Revoking a permission that was never granted is a no-op
        permissionManager.revokePermission(script.id, Permission.CAMERA)
        assertThat(permissionManager.getGrantedPermissions(script.id))
            .containsExactly(Permission.INTERNET)
    }

    @Config(sdk = [34])
    @Test
    fun getGrantedPermissions_reflectsRequestedPermissions() {
        shadowOf(application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val script = Script(
            id = "test-script",
            name = "Test Script",
            version = "1.0.0",
            author = "Test Author",
            description = "A test script",
            permissions = listOf(Permission.INTERNET),
            sourceCode = "console.log('hello');"
        )

        assertThat(permissionManager.getGrantedPermissions(script.id)).isEmpty()

        permissionManager.requestPermissions(activity, script) { _, _ -> }

        assertThat(permissionManager.getGrantedPermissions(script.id))
            .containsExactly(Permission.INTERNET)

        permissionManager.revokePermissions(script.id)
        assertThat(permissionManager.getGrantedPermissions(script.id)).isEmpty()
    }
}
