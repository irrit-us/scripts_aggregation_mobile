package com.scripthost.engine

import com.google.common.truth.Truth.assertThat
import com.scripthost.models.InstallResult
import com.scripthost.models.Permission
import com.scripthost.models.Script
import com.scripthost.models.ScriptCategory
import com.scripthost.security.SignatureVerifier
import com.scripthost.util.ConsoleLogger
import kotlinx.coroutines.runBlocking
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
        scriptManager = ScriptManager(tempFolder.root, logger = ConsoleLogger())
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
        assertThat(result is InstallResult.Success).isTrue()

        val script = (result as InstallResult.Success).script
        assertThat(script.id).isEqualTo("testauthor.hello_world")
        assertThat(scriptManager.getScript(script.id)).isEqualTo(script)

        val sourceFile = File(tempFolder.root, "scripts/testauthor.hello_world.js")
        assertThat(sourceFile.exists()).isTrue()
        assertThat(sourceFile.readText()).isEqualTo("console.log('hello');")
        assertThat(File(tempFolder.root, "scripts_metadata.json").exists()).isTrue()
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
        assertThat(result is InstallResult.Failure).isTrue()
        assertThat((result as InstallResult.Failure).error).contains("Signature")
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
        assertThat(result is InstallResult.Failure).isTrue()
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

        val managerWithKey = ScriptManager(tempFolder.root, verifier, logger = ConsoleLogger())
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

        assertThat(result is InstallResult.Success).isTrue()
    }

    @Test
    fun installScript_rejectsBlankFields() = runBlocking {
        assertThat(installUnsigned(name = "   ") is InstallResult.Failure).isTrue()
        assertThat(installUnsigned(version = "") is InstallResult.Failure).isTrue()
        assertThat(installUnsigned(author = " ") is InstallResult.Failure).isTrue()
        assertThat(installUnsigned(sourceCode = "") is InstallResult.Failure).isTrue()
    }

    @Test
    fun updateScript_changesVersionAndSource() = runBlocking {
        installUnsigned()

        val result = scriptManager.updateScript("testauthor.hello_world", "2.0.0", "console.log('v2');")
        assertThat(result is InstallResult.Success).isTrue()

        val updated = scriptManager.getScript("testauthor.hello_world")
        assertThat(updated).isNotNull()
        assertThat(updated?.version).isEqualTo("2.0.0")
        assertThat(updated?.sourceCode).isEqualTo("console.log('v2');")
    }

    @Test
    fun updateScript_unknownIdFails() = runBlocking {
        val result = scriptManager.updateScript("missing.script", "1.0.0", "code")
        assertThat(result is InstallResult.Failure).isTrue()
    }

    @Test
    fun uninstallScript_removesScriptAndFile() = runBlocking {
        installUnsigned()
        assertThat(scriptManager.getScript("testauthor.hello_world")).isNotNull()

        val removed = scriptManager.uninstallScript("testauthor.hello_world")
        assertThat(removed).isTrue()
        assertThat(scriptManager.getScript("testauthor.hello_world")).isNull()
        assertThat(File(tempFolder.root, "scripts/testauthor.hello_world.js").exists()).isFalse()
    }

    @Test
    fun searchScripts_filtersByNameDescriptionAuthor() = runBlocking {
        installUnsigned(name = "Counter", author = "Alice")
        installUnsigned(name = "Weather", author = "Bob", description = "Fetches forecast")

        assertThat(scriptManager.searchScripts("counter")).hasSize(1)
        assertThat(scriptManager.searchScripts("forecast")).hasSize(1)
        assertThat(scriptManager.searchScripts("bob")).hasSize(1)
        assertThat(scriptManager.searchScripts("")).isEmpty() // blank -> empty result set
    }

    @Test
    fun getScriptsByCategory_filters() = runBlocking {
        installUnsigned(name = "Utility Tool", author = "A", category = ScriptCategory.UTILITY)
        installUnsigned(name = "Game", author = "B", category = ScriptCategory.ENTERTAINMENT)

        val utility = scriptManager.getScriptsByCategory(ScriptCategory.UTILITY)
        assertThat(utility).hasSize(1)
        assertThat(utility.first().name).isEqualTo("Utility Tool")
    }

    @Test
    fun exportScript_producesReimportablePackage() = runBlocking {
        installUnsigned(name = "Export Me", author = "Alice")

        val exported = scriptManager.exportScript("alice.export_me")
        assertThat(exported).isNotNull()
        assertThat(exported).contains("\"name\": \"Export Me\"")

        // Re-import the package into a fresh manager
        val jsonFile = File(tempFolder.root, "package.json")
        jsonFile.writeText(exported!!)
        val freshManager = ScriptManager(tempFolder.root, logger = ConsoleLogger())

        val result = freshManager.installScriptFromFile(jsonFile, verifySignature = false)
        assertThat(result is InstallResult.Success).isTrue()
        assertThat((result as InstallResult.Success).script.name).isEqualTo("Export Me")
    }

    @Test
    fun installScriptFromFile_acceptsRawJavaScript() = runBlocking {
        val scriptFile = File(tempFolder.root, "my_script.js")
        scriptFile.writeText("console.log('raw');")

        val result = scriptManager.installScriptFromFile(scriptFile, verifySignature = false)
        assertThat(result is InstallResult.Success).isTrue()
        assertThat((result as InstallResult.Success).script.name).isEqualTo("my_script")
        assertThat(scriptManager.getAllScripts().map { it.name }).contains("my_script")
    }

    @Test
    fun scriptsPersistAcrossManagerInstances() = runBlocking {
        installUnsigned()

        val reloaded = ScriptManager(tempFolder.root, logger = ConsoleLogger())
        val script = reloaded.getScript("testauthor.hello_world")
        assertThat(script).isNotNull()
        assertThat(script?.sourceCode).isEqualTo("console.log('hello');")
    }
}
