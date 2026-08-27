package com.scripthost.engine

import android.content.Context
import com.scripthost.models.InstallResult
import com.scripthost.models.Permission
import com.scripthost.models.Script
import com.scripthost.models.VerificationResult
import com.scripthost.security.SignatureVerifier
import com.scripthost.util.AndroidLogger
import com.scripthost.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Date

/**
 * Script Manager - Handles script installation, storage, and lifecycle.
 * Manages a local script repository with metadata persistence.
 *
 * @param storageDir root directory used for the repository. Injected instead of
 *                   an Android [Context] so the manager is testable on the JVM.
 * @param signatureVerifier verifier used to validate script signatures.
 * @param logger destination for internal warnings/errors.
 */
class ScriptManager(
    private val storageDir: File,
    private val signatureVerifier: SignatureVerifier = SignatureVerifier(),
    private val logger: Logger = AndroidLogger()
) {

    /**
     * Convenience constructor for app use; stores scripts in [Context.filesDir].
     */
    constructor(context: Context) : this(context.filesDir)

    private val scriptsDir = File(storageDir, "scripts")
    private val metadataFile = File(storageDir, "scripts_metadata.json")

    private val installedScripts = mutableMapOf<String, Script>()

    init {
        scriptsDir.mkdirs()
        loadInstalledScripts()
    }

    /**
     * Install a script from source code.
     *
     * When [verifySignature] is enabled, a valid signature is mandatory:
     * unsigned scripts are rejected rather than silently accepted.
     */
    suspend fun installScript(
        name: String,
        version: String,
        author: String,
        description: String,
        permissions: List<Permission>,
        sourceCode: String,
        signature: String? = null,
        verifySignature: Boolean = true
    ): InstallResult = withContext(Dispatchers.IO) {

        try {
            // Validate required inputs before doing any work
            when {
                name.isBlank() -> return@withContext InstallResult.Failure("Script name is required")
                version.isBlank() -> return@withContext InstallResult.Failure("Script version is required")
                author.isBlank() -> return@withContext InstallResult.Failure("Script author is required")
                sourceCode.isBlank() -> return@withContext InstallResult.Failure("Script source code is required")
            }

            val scriptId = generateScriptId(name, author)

            val script = Script(
                id = scriptId,
                name = name,
                version = version,
                author = author,
                description = description,
                permissions = permissions,
                sourceCode = sourceCode,
                signature = signature
            )

            if (verifySignature) {
                if (signature.isNullOrEmpty()) {
                    return@withContext InstallResult.Failure(
                        "Signature required when signature verification is enabled"
                    )
                }
                when (val result = signatureVerifier.verify(script)) {
                    is VerificationResult.Valid -> Unit
                    is VerificationResult.Invalid -> {
                        return@withContext InstallResult.Failure(
                            "Signature verification failed: ${result.reason}"
                        )
                    }
                }
            }

            saveScript(script)
            installedScripts[scriptId] = script
            saveMetadata()

            InstallResult.Success(script)

        } catch (e: Exception) {
            InstallResult.Failure("Installation failed: ${e.message}")
        }
    }

    /**
     * Install script from a file, either a raw JavaScript source or a JSON package.
     */
    suspend fun installScriptFromFile(file: File, verifySignature: Boolean = true): InstallResult {
        return try {
            val content = file.readText()

            if (content.trim().startsWith("{")) {
                installScriptFromJson(content, verifySignature)
            } else {
                // Raw JavaScript, treated as a local unsigned script with no
                // permissions until the user grants them at run time
                installScript(
                    name = file.nameWithoutExtension,
                    version = "1.0.0",
                    author = "Unknown",
                    description = "Imported script",
                    permissions = emptyList(),
                    sourceCode = content,
                    verifySignature = false
                )
            }
        } catch (e: Exception) {
            InstallResult.Failure("Failed to read file: ${e.message}")
        }
    }

    /**
     * Install script from a JSON package (see [exportScript] for the format).
     */
    private suspend fun installScriptFromJson(json: String, verifySignature: Boolean): InstallResult {
        return try {
            val jsonObject = JSONObject(json)

            val name = jsonObject.getString("name")
            val version = jsonObject.getString("version")
            val author = jsonObject.getString("author")
            val description = jsonObject.optString("description", "")
            val sourceCode = jsonObject.getString("sourceCode")
            val signature = jsonObject.optString("signature", null)

            val permissionsArray = jsonObject.optJSONArray("permissions") ?: JSONArray()
            val permissions = mutableListOf<Permission>()
            for (i in 0 until permissionsArray.length()) {
                val permName = permissionsArray.getString(i)
                Permission.fromString(permName)?.let { permissions.add(it) }
            }

            installScript(
                name = name,
                version = version,
                author = author,
                description = description,
                permissions = permissions,
                sourceCode = sourceCode,
                signature = signature,
                verifySignature = verifySignature
            )

        } catch (e: Exception) {
            InstallResult.Failure("Invalid script package: ${e.message}")
        }
    }

    /**
     * Uninstall a script.
     */
    suspend fun uninstallScript(scriptId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            installedScripts.remove(scriptId)
            File(scriptsDir, "$scriptId.js").delete()
            saveMetadata()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Update a script's version and source code.
     */
    suspend fun updateScript(scriptId: String, newVersion: String, newSourceCode: String): InstallResult {
        val existingScript = installedScripts[scriptId]
            ?: return InstallResult.Failure("Script not found")

        return installScript(
            name = existingScript.name,
            version = newVersion,
            author = existingScript.author,
            description = existingScript.description,
            permissions = existingScript.permissions,
            sourceCode = newSourceCode,
            signature = null,
            verifySignature = false
        )
    }

    /**
     * Get installed script by ID.
     */
    fun getScript(scriptId: String): Script? {
        return installedScripts[scriptId]
    }

    /**
     * Get all installed scripts.
     */
    fun getAllScripts(): List<Script> {
        return installedScripts.values.toList()
    }

    /**
     * Search scripts by name, description, or author.
     */
    fun searchScripts(query: String): List<Script> {
        val lowerQuery = query.trim().lowercase()
        if (lowerQuery.isEmpty()) return emptyList()

        return installedScripts.values.filter {
            it.name.lowercase().contains(lowerQuery) ||
                it.description.lowercase().contains(lowerQuery) ||
                it.author.lowercase().contains(lowerQuery)
        }
    }

    /**
     * Export script to a JSON package.
     */
    fun exportScript(scriptId: String): String? {
        val script = installedScripts[scriptId] ?: return null

        val jsonObject = JSONObject()
        jsonObject.put("name", script.name)
        jsonObject.put("version", script.version)
        jsonObject.put("author", script.author)
        jsonObject.put("description", script.description)
        jsonObject.put("sourceCode", script.sourceCode)

        if (script.signature != null) {
            jsonObject.put("signature", script.signature)
        }

        val permissionsArray = JSONArray()
        script.permissions.forEach { permissionsArray.put(it.name) }
        jsonObject.put("permissions", permissionsArray)

        return jsonObject.toString(2)
    }

    // Private helper methods

    private fun generateScriptId(name: String, author: String): String {
        val base = "${author.lowercase()}.${name.lowercase().replace(" ", "_")}"
        return base.replace(Regex("[^a-z0-9._]"), "")
    }

    private fun saveScript(script: Script) {
        val scriptFile = File(scriptsDir, "${script.id}.js")
        scriptFile.writeText(script.sourceCode)
    }

    private fun loadInstalledScripts() {
        if (!metadataFile.exists()) {
            return
        }

        try {
            val jsonArray = JSONArray(metadataFile.readText())

            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val script = parseScriptFromJson(jsonObject)
                installedScripts[script.id] = script
            }

        } catch (e: Exception) {
            logger.error("ScriptManager", "Failed to load installed scripts", e)
        }
    }

    private fun saveMetadata() {
        try {
            val jsonArray = JSONArray()

            installedScripts.values.forEach { script ->
                val jsonObject = JSONObject()
                jsonObject.put("id", script.id)
                jsonObject.put("name", script.name)
                jsonObject.put("version", script.version)
                jsonObject.put("author", script.author)
                jsonObject.put("description", script.description)
                jsonObject.put("createdAt", script.createdAt.time)
                jsonObject.put("updatedAt", script.updatedAt.time)

                if (script.signature != null) {
                    jsonObject.put("signature", script.signature)
                }

                if (script.iconUrl != null) {
                    jsonObject.put("iconUrl", script.iconUrl)
                }

                val permissionsArray = JSONArray()
                script.permissions.forEach { permissionsArray.put(it.name) }
                jsonObject.put("permissions", permissionsArray)

                jsonArray.put(jsonObject)
            }

            metadataFile.writeText(jsonArray.toString(2))

        } catch (e: Exception) {
            logger.error("ScriptManager", "Failed to save scripts metadata", e)
        }
    }

    private fun parseScriptFromJson(jsonObject: JSONObject): Script {
        val scriptId = jsonObject.getString("id")

        // Load source code from file
        val scriptFile = File(scriptsDir, "$scriptId.js")
        val sourceCode = if (scriptFile.exists()) {
            scriptFile.readText()
        } else {
            ""
        }

        // Parse permissions
        val permissionsArray = jsonObject.optJSONArray("permissions") ?: JSONArray()
        val permissions = mutableListOf<Permission>()
        for (i in 0 until permissionsArray.length()) {
            val permName = permissionsArray.getString(i)
            Permission.fromString(permName)?.let { permissions.add(it) }
        }

        // Legacy metadata may still carry a "category" key; it is ignored.

        return Script(
            id = scriptId,
            name = jsonObject.getString("name"),
            version = jsonObject.getString("version"),
            author = jsonObject.getString("author"),
            description = jsonObject.getString("description"),
            permissions = permissions,
            sourceCode = sourceCode,
            signature = jsonObject.optString("signature", null),
            createdAt = Date(jsonObject.optLong("createdAt", System.currentTimeMillis())),
            updatedAt = Date(jsonObject.optLong("updatedAt", System.currentTimeMillis())),
            iconUrl = jsonObject.optString("iconUrl", null)
        )
    }
}
