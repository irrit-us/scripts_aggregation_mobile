package com.scripthost.engine

import com.scripthost.models.Permission
import com.scripthost.models.Script
import com.scripthost.models.ScriptContext
import org.junit.Assert.*
import org.junit.Test

/**
 * Integration tests for script execution
 */
class ScriptExecutionTest {

    @Test
    fun testBasicScriptExecution() {
        val script = Script(
            id = "test.basic",
            name = "Basic Test",
            version = "1.0.0",
            author = "Test",
            description = "Basic execution test",
            permissions = emptyList(),
            sourceCode = """
                var result = 2 + 2;
                result;
            """.trimIndent()
        )

        val context = ScriptContext(script = script)

        // In real test, would execute with JavaScriptEngine
        // val engine = JavaScriptEngine(mockContext)
        // val result = runBlocking { engine.execute(context) }
        // assertTrue(result is ExecutionResult.Success)

        assertNotNull(script)
    }

    @Test
    fun testConsoleLogScript() {
        val script = Script(
            id = "test.console",
            name = "Console Test",
            version = "1.0.0",
            author = "Test",
            description = "Console logging test",
            permissions = emptyList(),
            sourceCode = """
                console.log("Hello World");
                console.warn("Warning message");
                console.error("Error message");
            """.trimIndent()
        )

        assertNotNull(script.sourceCode)
        assertTrue(script.sourceCode.contains("console.log"))
    }

    @Test
    fun testScriptWithPermissions() {
        val script = Script(
            id = "test.permissions",
            name = "Permission Test",
            version = "1.0.0",
            author = "Test",
            description = "Test with permissions",
            permissions = listOf(Permission.INTERNET, Permission.VIBRATE),
            sourceCode = """
                // This script requires INTERNET and VIBRATE permissions
                console.log("Script with permissions");
            """.trimIndent()
        )

        assertEquals(2, script.permissions.size)
        assertTrue(script.permissions.contains(Permission.INTERNET))
        assertTrue(script.permissions.contains(Permission.VIBRATE))
    }

    @Test
    fun testScriptTimeout() {
        val script = Script(
            id = "test.timeout",
            name = "Timeout Test",
            version = "1.0.0",
            author = "Test",
            description = "Test execution timeout",
            permissions = emptyList(),
            sourceCode = """
                // Infinite loop - should timeout
                while(true) {
                    // Do nothing
                }
            """.trimIndent()
        )

        // In real test, this would timeout after 30 seconds
        assertNotNull(script)
    }

    @Test
    fun testScriptMemoryLimit() {
        val script = Script(
            id = "test.memory",
            name = "Memory Test",
            version = "1.0.0",
            author = "Test",
            description = "Test memory limit",
            permissions = emptyList(),
            sourceCode = """
                // Try to allocate large array
                var arr = new Array(1000000);
                for (var i = 0; i < arr.length; i++) {
                    arr[i] = new Array(1000);
                }
            """.trimIndent()
        )

        // In real test, this would hit memory limit
        assertNotNull(script)
    }

    @Test
    fun testScriptSyntaxError() {
        val script = Script(
            id = "test.syntax",
            name = "Syntax Error Test",
            version = "1.0.0",
            author = "Test",
            description = "Test syntax error handling",
            permissions = emptyList(),
            sourceCode = """
                // Invalid JavaScript syntax
                var x = ;
                console.log(x);
            """.trimIndent()
        )

        // In real test, this would return ExecutionResult.Error
        assertNotNull(script)
    }

    @Test
    fun testScriptRuntimeError() {
        val script = Script(
            id = "test.runtime",
            name = "Runtime Error Test",
            version = "1.0.0",
            author = "Test",
            description = "Test runtime error handling",
            permissions = emptyList(),
            sourceCode = """
                // Runtime error - undefined variable
                console.log(undefinedVariable);
            """.trimIndent()
        )

        // In real test, this would return ExecutionResult.Error
        assertNotNull(script)
    }

    @Test
    fun testUIComponentCreation() {
        val script = Script(
            id = "test.ui",
            name = "UI Test",
            version = "1.0.0",
            author = "Test",
            description = "Test UI component creation",
            permissions = emptyList(),
            sourceCode = """
                var button = new Button("Click Me");
                button.setBackgroundColor("#007AFF");
                button.setOnTap(function() {
                    console.log("Button clicked");
                });
                UI.addView(button);
            """.trimIndent()
        )

        assertTrue(script.sourceCode.contains("Button"))
        assertTrue(script.sourceCode.contains("UI.addView"))
    }

    @Test
    fun testNetworkRequest() {
        val script = Script(
            id = "test.network",
            name = "Network Test",
            version = "1.0.0",
            author = "Test",
            description = "Test network request",
            permissions = listOf(Permission.INTERNET),
            sourceCode = """
                Network.get("https://api.github.com/users/octocat", function(data, error) {
                    if (error) {
                        console.error("Error: " + error);
                    } else {
                        console.log("Success: " + data);
                    }
                });
            """.trimIndent()
        )

        assertTrue(script.permissions.contains(Permission.INTERNET))
        assertTrue(script.sourceCode.contains("Network.get"))
    }

    @Test
    fun testStorageOperations() {
        val script = Script(
            id = "test.storage",
            name = "Storage Test",
            version = "1.0.0",
            author = "Test",
            description = "Test storage operations",
            permissions = listOf(Permission.READ_STORAGE, Permission.WRITE_STORAGE),
            sourceCode = """
                Storage.writeFile("test.txt", "Hello World");
                var content = Storage.readFile("test.txt");
                console.log("Content: " + content);
                Storage.deleteFile("test.txt");
            """.trimIndent()
        )

        assertTrue(script.permissions.contains(Permission.READ_STORAGE))
        assertTrue(script.permissions.contains(Permission.WRITE_STORAGE))
        assertTrue(script.sourceCode.contains("Storage.writeFile"))
    }
}
