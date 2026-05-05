package com.scripthost.engine

import android.content.Context
import com.scripthost.models.*
import com.scripthost.security.SignatureVerifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Script Manager - Handles script installation, storage, and lifecycle
 * Manages local script repository with metadata persistence
 */
class ScriptManager(private val context: Context) {

    private val scriptsDir = File(context.filesDir, "scripts")
    private val metadataFile = File(context.filesDir, "scripts_metadata.json")
    private val signatureVerifier = SignatureVerifier()

    private val installedScripts = mutableMapOf<String, Script>()

    init {
        scriptsDir.mkdirs()
        loadInstalledScripts()
    }

    /**
     * Install a script from source code
     */
    suspend fun installScript(
        name: String,
        version: String,
        author: String,
        description: String,
        permissions: List<Permission>,
        sourceCode: String,
        signature: String? = null,
        category: ScriptCategory = ScriptCategory.UTILITY,
        verifySignature: Boolean = true
    ): InstallResult = withContext(Dispatchers.IO) {

        try {
            // Generate unique ID
            val scriptId = generateScriptId(name, author)

            // Create script object
            val script = Script(
                id = scriptId,
                name = name,
                version = version,
                author = author,
                description = description,
                permissions = permissions,
                sourceCode = sourceCode,
                signature = signature,
                category = category
            )

            // Verify signature if required
            if (verifySignature && signature != null) {
                when (val result = signatureVerifier.verify(script)) {
                    is VerificationResult.Valid -> {
                        // Signature valid, continue
                    }
                    is VerificationResult.Invalid -> {
                        return@withContext InstallResult.Failure("Signature verification failed: ${result.reason}")
                    }
                }
            }

            // Save script to disk
            saveScript(script)

            // Add to installed scripts
            installedScripts[scriptId] = script

            // Save metadata
            saveMetadata()

            InstallResult.Success(script)

        } catch (e: Exception) {
            InstallResult.Failure("Installation failed: ${e.message}")
        }
    }

    /**
     * Install script from file
     */
    suspend fun installScriptFromFile(file: File, verifySignature: Boolean = true): InstallResult {
        return try {
            val content = file.readText()

            // Try to parse as JSON package
            if (content.trim().startsWith("{")) {
                installScriptFromJson(content, verifySignature)
            } else {
                // Treat as raw JavaScript
                installScript(
                    name = file.nameWithoutExtension,
                    version = "1.0.0",
                    author = "Unknown",
                    description = "Imported script",
                    permissions = listOf(Permission.INTERNET),
                    sourceCode = content,
                    verifySignature = false
                )
            }
        } catch (e: Exception) {
            InstallResult.Failure("Failed to read file: ${e.message}")
        }
    }

    /**
     * Install script from JSON package
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
            val category = ScriptCategory.valueOf(
                jsonObject.optString("category", ScriptCategory.UTILITY.name)
            )

            // Parse permissions
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
                category = category,
                verifySignature = verifySignature
            )

        } catch (e: Exception) {
            InstallResult.Failure("Invalid script package: ${e.message}")
        }
    }

    /**
     * Uninstall a script
     */
    suspend fun uninstallScript(scriptId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // Remove from installed scripts
            installedScripts.remove(scriptId)

            // Delete script file
            val scriptFile = File(scriptsDir, "$scriptId.js")
            scriptFile.delete()

            // Save metadata
            saveMetadata()

            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Update a script
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
            category = existingScript.category,
            verifySignature = false
        )
    }

    /**
     * Get installed script by ID
     */
    fun getScript(scriptId: String): Script? {
        return installedScripts[scriptId]
    }

    /**
     * Get all installed scripts
     */
    fun getAllScripts(): List<Script> {
        return installedScripts.values.toList()
    }

    /**
     * Get scripts by category
     */
    fun getScriptsByCategory(category: ScriptCategory): List<Script> {
        return installedScripts.values.filter { it.category == category }
    }

    /**
     * Search scripts by name or description
     */
    fun searchScripts(query: String): List<Script> {
        val lowerQuery = query.lowercase()
        return installedScripts.values.filter {
            it.name.lowercase().contains(lowerQuery) ||
            it.description.lowercase().contains(lowerQuery) ||
            it.author.lowercase().contains(lowerQuery)
        }
    }

    /**
     * Export script to JSON package
     */
    fun exportScript(scriptId: String): String? {
        val script = installedScripts[scriptId] ?: return null

        val jsonObject = JSONObject()
        jsonObject.put("name", script.name)
        jsonObject.put("version", script.version)
        jsonObject.put("author", script.author)
        jsonObject.put("description", script.description)
        jsonObject.put("sourceCode", script.sourceCode)
        jsonObject.put("category", script.category.name)

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
            val json = metadataFile.readText()
            val jsonArray = JSONArray(json)

            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val script = parseScriptFromJson(jsonObject)
                installedScripts[script.id] = script
            }

        } catch (e: Exception) {
            e.printStackTrace()
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
                jsonObject.put("category", script.category.name)
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
            e.printStackTrace()
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

        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)

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
            iconUrl = jsonObject.optString("iconUrl", null),
            category = ScriptCategory.valueOf(
                jsonObject.optString("category", ScriptCategory.UTILITY.name)
            )
        )
    }
}
