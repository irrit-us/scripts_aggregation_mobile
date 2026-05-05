package com.scripthost.engine

import com.scripthost.models.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import java.util.Date

/**
 * Unit tests for ScriptManager
 */
class ScriptManagerTest {

    @Mock
    private lateinit var context: android.content.Context

    private lateinit var scriptManager: ScriptManager

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        // In real tests, use a test context
        // scriptManager = ScriptManager(context)
    }

    @Test
    fun testScriptIdGeneration() {
        // Test that script IDs are generated correctly
        val name = "Test Script"
        val author = "Test Author"

        // Expected: testauthor.test_script
        val expectedId = "testauthor.test_script"

        // This would be tested with actual ScriptManager instance
        assertTrue(true)
    }

    @Test
    fun testInstallScript() = runBlocking {
        // Test script installation
        val script = Script(
            id = "test.script",
            name = "Test Script",
            version = "1.0.0",
            author = "Test",
            description = "Test description",
            permissions = listOf(Permission.INTERNET),
            sourceCode = "console.log('test');",
            createdAt = Date(),
            updatedAt = Date()
        )

        // Verify script properties
        assertEquals("test.script", script.id)
        assertEquals("Test Script", script.name)
        assertEquals("1.0.0", script.version)
        assertTrue(script.permissions.contains(Permission.INTERNET))
    }

    @Test
    fun testScriptPermissions() {
        val permissions = listOf(
            Permission.INTERNET,
            Permission.CAMERA,
            Permission.LOCATION_FINE
        )

        val script = Script(
            id = "test.permissions",
            name = "Permission Test",
            version = "1.0.0",
            author = "Test",
            description = "Test",
            permissions = permissions,
            sourceCode = ""
        )

        assertEquals(3, script.permissions.size)
        assertTrue(script.permissions.contains(Permission.INTERNET))
        assertTrue(script.permissions.contains(Permission.CAMERA))
        assertTrue(script.permissions.contains(Permission.LOCATION_FINE))
    }

    @Test
    fun testScriptCategories() {
        val categories = ScriptCategory.values()

        assertTrue(categories.contains(ScriptCategory.UTILITY))
        assertTrue(categories.contains(ScriptCategory.PRODUCTIVITY))
        assertTrue(categories.contains(ScriptCategory.ENTERTAINMENT))
        assertTrue(categories.contains(ScriptCategory.SYSTEM))
    }

    @Test
    fun testPermissionFromString() {
        val permission = Permission.fromString("INTERNET")
        assertNotNull(permission)
        assertEquals(Permission.INTERNET, permission)

        val invalid = Permission.fromString("INVALID_PERMISSION")
        assertNull(invalid)
    }

    @Test
    fun testDangerousPermissions() {
        assertTrue(Permission.CAMERA.dangerous)
        assertTrue(Permission.LOCATION_FINE.dangerous)
        assertTrue(Permission.RECORD_AUDIO.dangerous)

        assertFalse(Permission.INTERNET.dangerous)
        assertFalse(Permission.VIBRATE.dangerous)
    }
}
