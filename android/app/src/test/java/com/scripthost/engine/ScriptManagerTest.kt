package com.scripthost.engine

import com.scripthost.models.InstallResult
import com.scripthost.models.Permission
import com.scripthost.models.Script
import com.scripthost.models.ScriptCategory
import com.scripthost.security.SignatureVerifier
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Unit tests for [ScriptManager] backed by a temporary storage directory.
 */
class ScriptManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var scriptManager: ScriptManager

    @Before
    fun setUp() {
        scriptManager = ScriptManager(tempFolder.root)
    }

    private fun installUnsigned(
        name: String = "Hello World",
        version: String = "1.0.0",
        author: String = "Test Author",
        description: String = "A test script",
        permissions: List<Permission> = listOf(Permission.INTERNET),
        sourceCode: String = "console.log('hello');",
        category: ScriptCategory = ScriptCategory.UTILITY
    ): InstallResult = runBlocking {
        scriptManager.installScript(
            name = name,
            version = version,
            author = author,
            description = description,
            permissions = permissions,
            sourceCode = sourceCode,
            category = category,
            verifySignature = false
        )
    }

    @Test
    fun installScript_persistsScriptAndSourceFile() = runBlocking {
        val result = installUnsigned()
        assertTrue(result is InstallResult.Success)

        val script = (result as InstallResult.Success).script
        assertEquals("testauthor.hello_world", script.id)
        assertEquals(script, scriptManager.getScript(script.id))

        val sourceFile = File(tempFolder.root, "scripts/testauthor.hello_world.js")
        assertTrue(sourceFile.exists())
        assertEquals("console.log('hello');", sourceFile.readText())
        assertTrue(File(tempFolder.root, "scripts_metadata.json").exists())
    }

    @Test
    fun installScript_rejectsMissingSignatureWhenVerificationEnabled() = runBlocking {
        val result = scriptManager.installScript(
            name = "Unsigned",
            version = "1.0.0",
            author = "Author",
            description = "",
            permissions = emptyList(),
            sourceCode = "console.log('x');",
            signature = null,
            verifySignature = true
        )
        assertTrue(result is InstallResult.Failure)
        assertTrue((result as InstallResult.Failure).error.contains("Signature"))
    }

    @Test
    fun installScript_rejectsInvalidSignature() = runBlocking {
        val result = scriptManager.installScript(
            name = "Bad Signature",
            version = "1.0.0",
            author = "Author",
            description = "",
            permissions = emptyList(),
            sourceCode = "console.log('x');",
            signature = "not-a-valid-signature",
            verifySignature = true
        )
        assertTrue(result is InstallResult.Failure)
    }

    @Test
    fun installScript_acceptsValidSignature() = runBlocking {
        val keyPair = SignatureVerifier().generateKeyPair()
        val verifier = SignatureVerifier(keyPair.public)

        // The manager derives the id from name + author; sign the exact data
        // the manager will verify (canonical id included).
        val signedScript = Script(
            id = "testauthor.hello_world",
            name = "Hello World",
            version = "1.0.0",
            author = "Test Author",
            description = "Signed script",
            permissions = listOf(Permission.INTERNET),
            sourceCode = "console.log('signed');"
        )
        val signature = verifier.sign(signedScript, keyPair.private)

        val managerWithKey = ScriptManager(tempFolder.root, verifier)
        val result = managerWithKey.installScript(
            name = signedScript.name,
            version = signedScript.version,
            author = signedScript.author,
            description = signedScript.description,
            permissions = signedScript.permissions,
            sourceCode = signedScript.sourceCode,
            signature = signature,
            verifySignature = true
        )

        assertTrue(result is InstallResult.Success)
    }

    @Test
    fun installScript_rejectsBlankFields() = runBlocking {
        assertTrue(installUnsigned(name = "   ") is InstallResult.Failure)
        assertTrue(installUnsigned(version = "") is InstallResult.Failure)
        assertTrue(installUnsigned(author = " ") is InstallResult.Failure)
        assertTrue(installUnsigned(sourceCode = "") is InstallResult.Failure)
    }

    @Test
    fun updateScript_changesVersionAndSource() = runBlocking {
        installUnsigned()

        val result = scriptManager.updateScript("testauthor.hello_world", "2.0.0", "console.log('v2');")
        assertTrue(result is InstallResult.Success)

        val updated = scriptManager.getScript("testauthor.hello_world")
        assertNotNull(updated)
        assertEquals("2.0.0", updated?.version)
        assertEquals("console.log('v2');", updated?.sourceCode)
    }

    @Test
    fun updateScript_unknownIdFails() = runBlocking {
        val result = scriptManager.updateScript("missing.script", "1.0.0", "code")
        assertTrue(result is InstallResult.Failure)
    }

    @Test
    fun uninstallScript_removesScriptAndFile() = runBlocking {
        installUnsigned()
        assertNotNull(scriptManager.getScript("testauthor.hello_world"))

        val removed = scriptManager.uninstallScript("testauthor.hello_world")
        assertTrue(removed)
        assertNull(scriptManager.getScript("testauthor.hello_world"))
        assertFalse(File(tempFolder.root, "scripts/testauthor.hello_world.js").exists())
    }

    @Test
    fun searchScripts_filtersByNameDescriptionAuthor() = runBlocking {
        installUnsigned(name = "Counter", author = "Alice")
        installUnsigned(name = "Weather", author = "Bob", description = "Fetches forecast")

        assertEquals(1, scriptManager.searchScripts("counter").size)
        assertEquals(1, scriptManager.searchScripts("forecast").size)
        assertEquals(1, scriptManager.searchScripts("bob").size)
        assertEquals(0, scriptManager.searchScripts("").size) // blank -> empty result set
    }

    @Test
    fun getScriptsByCategory_filters() = runBlocking {
        installUnsigned(name = "Utility Tool", author = "A", category = ScriptCategory.UTILITY)
        installUnsigned(name = "Game", author = "B", category = ScriptCategory.ENTERTAINMENT)

        val utility = scriptManager.getScriptsByCategory(ScriptCategory.UTILITY)
        assertEquals(1, utility.size)
        assertEquals("Utility Tool", utility.first().name)
    }

    @Test
    fun exportScript_producesReimportablePackage() = runBlocking {
        installUnsigned(name = "Export Me", author = "Alice")

        val exported = scriptManager.exportScript("alice.export_me")
        assertNotNull(exported)
        assertTrue(exported!!.contains("\"name\": \"Export Me\""))

        // Re-import the package into a fresh manager
        val jsonFile = File(tempFolder.root, "package.json")
        jsonFile.writeText(exported)
        val freshManager = ScriptManager(tempFolder.root)

        val result = freshManager.installScriptFromFile(jsonFile, verifySignature = false)
        assertTrue(result is InstallResult.Success)
        assertEquals("Export Me", (result as InstallResult.Success).script.name)
    }

    @Test
    fun installScriptFromFile_acceptsRawJavaScript() = runBlocking {
        val scriptFile = File(tempFolder.root, "my_script.js")
        scriptFile.writeText("console.log('raw');")

        val result = scriptManager.installScriptFromFile(scriptFile, verifySignature = false)
        assertTrue(result is InstallResult.Success)
        assertEquals("my_script", (result as InstallResult.Success).script.name)
        assertTrue(scriptManager.getAllScripts().any { it.name == "my_script" })
    }

    @Test
    fun scriptsPersistAcrossManagerInstances() = runBlocking {
        installUnsigned()

        val reloaded = ScriptManager(tempFolder.root)
        val script = reloaded.getScript("testauthor.hello_world")
        assertNotNull(script)
        assertEquals("console.log('hello');", script?.sourceCode)
    }
}
