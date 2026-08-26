package com.scripthost.bridge

import android.app.Activity
import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.scripthost.TestApplication
import com.scripthost.config.ConfigStore
import com.scripthost.config.PlaintextCipher
import com.scripthost.models.Permission
import com.scripthost.models.Script
import com.scripthost.security.PermissionManager
import com.scripthost.util.ConsoleLogger
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.atomic.AtomicReference

/**
 * Unit tests for [ConfigBridge] covering permission-gated access to the
 * configured keys stored in [ConfigStore].
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = TestApplication::class)
class ConfigBridgeTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var application: Application
    private lateinit var configStore: ConfigStore
    private lateinit var permissionManager: PermissionManager
    private lateinit var bridge: ConfigBridge

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        configStore = ConfigStore(tempFolder.root, PlaintextCipher, ConsoleLogger())
        configStore.put("API_KEY", "secret-value")
        permissionManager = PermissionManager(application)
        grantConfigToScript(permissionManager, SCRIPT_ID)
        // No register() call: a live V8 runtime is unavailable on the JVM.
        bridge = ConfigBridge(configStore, permissionManager, SCRIPT_ID)
    }

    @Test
    fun getConfig_withPermission_returnsConfiguredValue() {
        assertThat(bridge.getConfig("API_KEY")).isEqualTo("secret-value")
    }

    @Test
    fun getConfig_withPermission_missingKey_returnsNull() {
        assertThat(bridge.getConfig("MISSING")).isNull()
    }

    @Test
    fun getConfig_withoutPermission_returnsNull() {
        val unprivileged = ConfigBridge(configStore, PermissionManager(application), SCRIPT_ID)

        assertThat(unprivileged.getConfig("API_KEY")).isNull()
    }

    // listKeys() is untested here: it builds a V8Array and therefore requires
    // a live V8 runtime, which is unavailable in JVM unit tests.

    private fun grantConfigToScript(manager: PermissionManager, scriptId: String) {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val script = Script(
            id = scriptId,
            name = "Config Test Script",
            version = "1.0.0",
            author = "Test Author",
            description = "A test script",
            permissions = listOf(Permission.CONFIG),
            sourceCode = "console.log('hello');"
        )
        val granted = AtomicReference<Set<Permission>>()
        manager.requestPermissions(activity, script) { g, _ -> granted.set(g) }
        assertThat(granted.get()).contains(Permission.CONFIG)
    }

    private companion object {
        const val SCRIPT_ID = "config-test"
    }
}
