package com.scripthost.engine

import com.google.common.truth.Truth.assertThat
import com.scripthost.config.ScriptConfigSchemas
import com.scripthost.models.InstallResult
import com.scripthost.models.Permission
import com.scripthost.models.Script
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
        sourceCode: String = "console.log('hello');"
    ): InstallResult = runBlocking {
        scriptManager.installScript(
            name = name,
            version = version,
            author = author,
            description = description,
            permissions = permissions,
            sourceCode = sourceCode,
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
    fun uninstallScript_dropsDeclaredConfigSchema() = runBlocking {
        installUnsigned()
        val schemas = ScriptConfigSchemas(tempFolder.root, ConsoleLogger())
        schemas.put(
            "testauthor.hello_world", "Hello World",
            listOf(ScriptConfigSchemas.Field("KEY", "Key", "text"))
        )

        assertThat(scriptManager.uninstallScript("testauthor.hello_world")).isTrue()

        val reloaded = ScriptConfigSchemas(tempFolder.root, ConsoleLogger())
        assertThat(reloaded.get("testauthor.hello_world")).isNull()
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
    fun legacyMetadataWithCategory_isTolerated() = runBlocking {
        installUnsigned()

        // Simulate metadata written by an older version that still carried a
        // "category" key; the current parser must ignore it and load the script.
        val metadataFile = File(tempFolder.root, "scripts_metadata.json")
        val legacy = metadataFile.readText().replace("\"description\": \"A test script\"",
            "\"description\": \"A test script\",\n    \"category\": \"ENTERTAINMENT\"")
        metadataFile.writeText(legacy)

        val reloaded = ScriptManager(tempFolder.root, logger = ConsoleLogger())
        val script = reloaded.getScript("testauthor.hello_world")
        assertThat(script).isNotNull()
        assertThat(script?.name).isEqualTo("Hello World")
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
    fun installScriptFromFile_displayOverrides_applyToRawJavaScript() = runBlocking {
        val scriptFile = File(tempFolder.root, "todo_list.js")
        scriptFile.writeText("console.log('raw');")

        val result = scriptManager.installScriptFromFile(
            scriptFile,
            verifySignature = false,
            displayName = "Todo List",
            displayDescription = "A compact checklist"
        )
        assertThat(result is InstallResult.Success).isTrue()
        val script = (result as InstallResult.Success).script
        assertThat(script.name).isEqualTo("Todo List")
        assertThat(script.description).isEqualTo("A compact checklist")
        // A prettified filename override generates the same id as the raw
        // filename, so both spellings report installed and reinstalls
        // overwrite the same entry
        assertThat(script.id).isEqualTo("unknown.todo_list")
        assertThat(scriptManager.isInstalled("Todo List")).isTrue()
        assertThat(scriptManager.isInstalled("todo_list")).isTrue()
        assertThat(scriptManager.isInstalled("Other Script")).isFalse()
    }

    @Test
    fun scriptsPersistAcrossManagerInstances() = runBlocking {
        installUnsigned()

        val reloaded = ScriptManager(tempFolder.root, logger = ConsoleLogger())
        val script = reloaded.getScript("testauthor.hello_world")
        assertThat(script).isNotNull()
        assertThat(script?.sourceCode).isEqualTo("console.log('hello');")
    }

    @Test
    fun corruptMetadataFile_constructsWithNoScripts() {
        // Garbage bytes written before the manager is constructed; the manager
        // must tolerate the corrupt metadata and start empty.
        File(tempFolder.root, "scripts_metadata.json").writeText("{ not valid json !!!")

        val manager = ScriptManager(tempFolder.root, logger = ConsoleLogger())

        assertThat(manager.getAllScripts()).isEmpty()
    }

    @Test
    fun exportScript_unknownId_returnsNull() {
        assertThat(scriptManager.exportScript("unknown-id")).isNull()
    }

    @Test
    fun uninstallScript_unknownId_completesWithoutThrowing() = runBlocking {
        // Current implementation removes nothing, deletes a non-existent file,
        // saves metadata, and still reports success.
        assertThat(scriptManager.uninstallScript("unknown-id")).isTrue()
    }

    @Test
    fun installScriptFromFile_invalidJsonPackage_fails() = runBlocking {
        val scriptFile = File(tempFolder.root, "broken.json")
        scriptFile.writeText("{invalid json")

        val result = scriptManager.installScriptFromFile(scriptFile, verifySignature = false)

        assertThat(result is InstallResult.Failure).isTrue()
    }
}
