package com.scripthost.bridge

import android.app.Activity
import android.app.Application
import android.os.Environment
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.scripthost.TestApplication
import com.scripthost.models.Permission
import com.scripthost.models.Script
import com.scripthost.security.PermissionManager
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowStatFs
import org.robolectric.util.ReflectionHelpers
import java.io.File
import java.util.concurrent.atomic.AtomicReference

/**
 * Unit tests for [SystemBridge] covering permission-gated file storage and
 * confinement of storage paths to the `script_storage` subdirectory of the
 * app's private files directory.
 *
 * NOTE: under Robolectric `context.filesDir` points to a real temp dir, so
 * tests use unique file names and clean up what they create.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = TestApplication::class)
class SystemBridgeTest {

    private lateinit var application: Application
    private lateinit var permissionManager: PermissionManager
    private lateinit var bridge: SystemBridge

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        permissionManager = PermissionManager(application)
        grantStorageToScript(permissionManager, SCRIPT_ID)
        // No register() call: a live V8 runtime is unavailable on the JVM.
        bridge = SystemBridge(application, permissionManager, SCRIPT_ID)
    }

    @Test
    fun writeFileThenReadFile_roundTripsContent() {
        val name = uniqueName("roundtrip")

        assertThat(bridge.writeFile(name, "hello storage")).isTrue()
        assertThat(bridge.readFile(name)).isEqualTo("hello storage")

        storageFile(name).delete()
    }

    @Test
    fun writeFile_createsFileInScriptStorageDir() {
        val name = uniqueName("created")

        assertThat(bridge.writeFile(name, "data")).isTrue()

        val onDisk = storageFile(name)
        assertThat(onDisk.exists()).isTrue()
        assertThat(onDisk.readText()).isEqualTo("data")
        onDisk.delete()
    }

    @Test
    fun readFile_missingFile_returnsNull() {
        assertThat(bridge.readFile(uniqueName("missing"))).isNull()
    }

    @Test
    fun deleteFile_removesFile() {
        val name = uniqueName("delete")
        assertThat(bridge.writeFile(name, "bye")).isTrue()
        assertThat(storageFile(name).exists()).isTrue()

        assertThat(bridge.deleteFile(name)).isTrue()
        assertThat(storageFile(name).exists()).isFalse()
    }

    @Test
    fun storage_withoutPermission_failsClosedAndCreatesNothing() {
        val unprivileged = SystemBridge(application, PermissionManager(application), SCRIPT_ID)
        val name = uniqueName("denied")

        assertThat(unprivileged.writeFile(name, "nope")).isFalse()
        assertThat(storageFile(name).exists()).isFalse()
        assertThat(unprivileged.readFile(name)).isNull()
        assertThat(unprivileged.deleteFile(name)).isFalse()
    }

    @Test
    fun writeFile_parentTraversal_isRejected() {
        // `..` from script_storage lands in filesDir, still out of bounds
        val escaped = File(application.filesDir, "outside-traversal.txt")
        escaped.delete() // guard against leftovers from earlier runs

        assertThat(bridge.writeFile("../outside-traversal.txt", "escape")).isFalse()
        assertThat(escaped.exists()).isFalse()
    }

    @Test
    fun readFile_relativeEscapeOutsideFilesDir_returnsNull() {
        assertThat(bridge.readFile("../../etc/passwd")).isNull()
    }

    @Test
    fun writeFile_absolutePathOutsideFilesDir_isRejected() {
        val escaped = File(application.filesDir.parentFile, "absolute-escape.txt")
        escaped.delete() // guard against leftovers from earlier runs

        assertThat(bridge.writeFile(escaped.absolutePath, "escape")).isFalse()
        assertThat(escaped.exists()).isFalse()
    }

    @Test
    fun listFiles_withoutV8Runtime_returnsNull() {
        // listFiles builds a V8Array, which requires a live V8 runtime; without
        // register() the bridge has none and returns null. Full behavior is
        // otherwise untestable on the JVM because J2V8 needs a native runtime.
        assertThat(bridge.listFiles(".")).isNull()
    }

    @Test
    fun vibrate_withoutPermission_doesNotThrow() {
        val unprivileged = SystemBridge(application, PermissionManager(application), SCRIPT_ID)
        unprivileged.vibrate(100)
    }

    @Test
    fun getTime_returnsCurrentEpochMillis() {
        val before = System.currentTimeMillis().toDouble()

        val time = bridge.getTime()

        assertThat(time).isAtLeast(before)
        assertThat(time).isAtMost((System.currentTimeMillis() + 5000).toDouble())
    }

    @Test
    fun getTimeZone_returnsDefaultZoneId() {
        assertThat(bridge.getTimeZone()).isEqualTo(java.util.TimeZone.getDefault().id)
    }

    @Test
    fun getDeviceName_prefersSettingsDeviceName() {
        android.provider.Settings.Global.putString(
            application.contentResolver, "device_name", "Test Phone"
        )

        assertThat(bridge.getDeviceName()).isEqualTo("Test Phone")
    }

    @Test
    fun getDeviceName_withoutSetting_fallsBackToModel() {
        android.provider.Settings.Global.putString(
            application.contentResolver, "device_name", ""
        )

        assertThat(bridge.getDeviceName()).isEqualTo(android.os.Build.MODEL)
    }

    @Test
    fun memoryStats_reflectsInjectedMemoryInfo() {
        val activityManager =
            application.getSystemService(android.content.Context.ACTIVITY_SERVICE)
                as android.app.ActivityManager
        val memoryInfo = android.app.ActivityManager.MemoryInfo().apply {
            totalMem = 4L * 1024 * 1024 * 1024
            availMem = 2L * 1024 * 1024 * 1024
            lowMemory = true
        }
        shadowOf(activityManager).setMemoryInfo(memoryInfo)

        val stats = bridge.memoryStats()

        assertThat(stats.totalMB).isEqualTo(4096)
        assertThat(stats.availableMB).isEqualTo(2048)
        assertThat(stats.lowMemory).isTrue()
    }

    @Test
    fun storageStats_reflectsRegisteredStatFs() {
        // ShadowStatFs uses a fixed 4096-byte block size; args are
        // (path, blockCount, freeBlocks, availableBlocks)
        ShadowStatFs.registerStats(Environment.getDataDirectory(), 1024, 512, 256)
        try {
            val stats = bridge.storageStats()

            assertThat(stats.totalMB).isEqualTo(4) // 1024 * 4096
            assertThat(stats.freeMB).isEqualTo(1) // 256 * 4096
            assertThat(stats.usedMB).isEqualTo(3)
        } finally {
            ShadowStatFs.reset()
        }
    }

    @Test
    fun getMemoryInfo_withoutV8Runtime_throws() {
        // getMemoryInfo builds a V8Object, which requires a live V8 runtime;
        // the underlying memoryStats() is covered separately above.
        assertThrows(IllegalStateException::class.java) { bridge.getMemoryInfo() }
    }

    @Test
    fun getStorageInfo_withoutV8Runtime_throws() {
        assertThrows(IllegalStateException::class.java) { bridge.getStorageInfo() }
    }

    @Test
    fun systemStats_returnsBuildDefaults() {
        val stats = bridge.systemStats()

        assertThat(stats.sdkVersion).isEqualTo(34) // @Config(sdk = [34])
        assertThat(stats.androidVersion).isNotEmpty()
        assertThat(stats.supportedAbis).isNotEmpty()
        assertThat(stats.supportedAbis).contains(stats.abi)
    }

    @Test
    fun systemStats_reflectsOverriddenBuildFields() {
        ReflectionHelpers.setStaticField(android.os.Build.VERSION::class.java, "SDK_INT", 33)
        ReflectionHelpers.setStaticField(android.os.Build.VERSION::class.java, "RELEASE", "15")
        ReflectionHelpers.setStaticField(
            android.os.Build::class.java, "SUPPORTED_ABIS",
            arrayOf("arm64-v8a", "armeabi-v7a")
        )

        val stats = bridge.systemStats()

        assertThat(stats.sdkVersion).isEqualTo(33)
        assertThat(stats.androidVersion).isEqualTo("15")
        assertThat(stats.abi).isEqualTo("arm64-v8a")
        assertThat(stats.supportedAbis).containsExactly("arm64-v8a", "armeabi-v7a").inOrder()
    }

    @Test
    fun getSystemInfo_withoutV8Runtime_throws() {
        // getSystemInfo builds a V8Object, which requires a live V8 runtime;
        // the underlying systemStats() is covered separately above.
        assertThrows(IllegalStateException::class.java) { bridge.getSystemInfo() }
    }

    private fun grantStorageToScript(manager: PermissionManager, scriptId: String) {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val script = Script(
            id = scriptId,
            name = "Storage Test Script",
            version = "1.0.0",
            author = "Test Author",
            description = "A test script",
            permissions = listOf(Permission.READ_STORAGE, Permission.WRITE_STORAGE),
            sourceCode = "console.log('hello');"
        )
        val granted = AtomicReference<Set<Permission>>()
        manager.requestPermissions(activity, script) { g, _ -> granted.set(g) }
        assertThat(granted.get()).containsAtLeast(
            Permission.READ_STORAGE, Permission.WRITE_STORAGE
        )
    }

    private fun uniqueName(prefix: String) = "$prefix-${System.nanoTime()}.txt"

    private fun storageFile(name: String) = File(File(application.filesDir, "script_storage"), name)

    private companion object {
        const val SCRIPT_ID = "storage-test"
    }
}
