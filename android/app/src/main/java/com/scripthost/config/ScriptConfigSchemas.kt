package com.scripthost.config

import com.scripthost.util.AndroidLogger
import com.scripthost.util.Logger
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Script Config Schemas - per-script configuration field declarations.
 *
 * Scripts declare their configurable fields at runtime via `Config.schema(...)`;
 * the Settings UI renders each script's fields under the script's title.
 * Schemas are persisted as JSON in the app's private storage directory
 * (`script_config_schemas.json`), shaped as:
 *
 * ```json
 * {
 *   "<scriptId>": {
 *     "name": "<script display name>",
 *     "fields": [
 *       { "key": "OPENAI_API_KEY", "label": "API Key", "type": "password" },
 *       { "key": "AGENT_MODEL", "label": "Model", "type": "select",
 *         "options": ["gpt-4o-mini", "gpt-4o"], "default": "gpt-4o-mini" }
 *     ]
 *   }
 * }
 * ```
 *
 * The directory is injected instead of an Android [android.content.Context]
 * so the store is testable on the JVM (see [ConfigStore]).
 */
class ScriptConfigSchemas(
    private val storageDir: File,
    private val logger: Logger = AndroidLogger()
) {

    /** One declared config field. [default] is the string form when present. */
    data class Field(
        val key: String,
        val label: String,
        val type: String,
        val default: String? = null,
        val options: List<String>? = null
    )

    /** Persisted schema for one script. */
    data class ScriptSchema(val name: String, val fields: List<Field>)

    private val schemasFile = File(storageDir, FILE_NAME)
    private val schemas = LinkedHashMap<String, ScriptSchema>()

    init {
        schemasFile.parentFile?.mkdirs()
        load()
    }

    /** Schema declared by [scriptId], or null when it never declared one. */
    fun get(scriptId: String): ScriptSchema? = schemas[scriptId]

    /** Snapshot of all declared schemas (for the Settings UI). */
    fun all(): Map<String, ScriptSchema> = schemas.toMap()

    /** Store or replace the schema for [scriptId]. */
    fun put(scriptId: String, name: String, fields: List<Field>) {
        if (scriptId.isBlank()) return
        schemas[scriptId] = ScriptSchema(name, fields)
        save()
    }

    /** Drop the schema for [scriptId] (script uninstalled). */
    fun removeScript(scriptId: String) {
        if (schemas.remove(scriptId) != null) save()
    }

    private fun load() {
        if (!schemasFile.exists()) return
        try {
            val json = JSONObject(schemasFile.readText())
            json.keys().forEach { scriptId ->
                val entry = json.getJSONObject(scriptId)
                schemas[scriptId] = ScriptSchema(
                    name = entry.optString("name", scriptId),
                    fields = parseFieldArray(entry.optJSONArray("fields"), logger)
                )
            }
        } catch (e: Exception) {
            logger.warn(TAG, "Failed to load script config schemas", e)
        }
    }

    private fun save() {
        try {
            val json = JSONObject()
            schemas.forEach { (scriptId, schema) ->
                val entry = JSONObject()
                entry.put("name", schema.name)
                val fields = JSONArray()
                schema.fields.forEach { field ->
                    val fieldJson = JSONObject()
                    fieldJson.put("key", field.key)
                    fieldJson.put("label", field.label)
                    fieldJson.put("type", field.type)
                    field.default?.let { fieldJson.put("default", it) }
                    field.options?.let { options ->
                        val array = JSONArray()
                        options.forEach { array.put(it) }
                        fieldJson.put("options", array)
                    }
                    fields.put(fieldJson)
                }
                entry.put("fields", fields)
                json.put(scriptId, entry)
            }
            schemasFile.writeText(json.toString(2))
        } catch (e: Exception) {
            logger.warn(TAG, "Failed to save script config schemas", e)
        }
    }

    companion object {
        private const val TAG = "ScriptConfigSchemas"
        private const val FILE_NAME = "script_config_schemas.json"

        val VALID_TYPES = setOf("text", "password", "number", "boolean", "select")

        /**
         * Parse and validate the JSON string a script passed to
         * `Config.schema(...)`. Returns null when the input is not a JSON
         * array. Invalid fields are skipped with a logged warning; valid
         * fields are kept.
         */
        fun parseFields(jsonString: String, logger: Logger = AndroidLogger()): List<Field>? {
            val array = try {
                JSONArray(jsonString)
            } catch (e: Exception) {
                logger.warn(TAG, "Config.schema argument is not a JSON array: ${e.message}")
                return null
            }
            return parseFieldArray(array, logger)
        }

        private fun parseFieldArray(
            array: JSONArray?,
            logger: Logger = AndroidLogger()
        ): List<Field> {
            if (array == null) return emptyList()
            val fields = mutableListOf<Field>()
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i)
                if (obj == null) {
                    logger.warn(TAG, "Skipping non-object field at index $i")
                    continue
                }
                val field = parseField(obj, logger) ?: continue
                if (fields.any { it.key == field.key }) {
                    logger.warn(TAG, "Skipping duplicate field key '${field.key}'")
                    continue
                }
                fields.add(field)
            }
            return fields
        }

        private fun parseField(obj: JSONObject, logger: Logger): Field? {
            val key = obj.optString("key", "").trim()
            if (key.isEmpty()) {
                logger.warn(TAG, "Skipping field with missing/blank key")
                return null
            }
            val type = obj.optString("type", "")
            if (type !in VALID_TYPES) {
                logger.warn(TAG, "Skipping field '$key' with unknown type '$type'")
                return null
            }
            val label = obj.optString("label", "").ifEmpty { key }

            var options: List<String>? = null
            if (type == "select") {
                val optionsArray = obj.optJSONArray("options")
                options = optionsArray?.let { arr ->
                    (0 until arr.length()).map { arr.optString(it) }.filter { it.isNotEmpty() }
                }.orEmpty()
                if (options.isEmpty()) {
                    logger.warn(TAG, "Skipping select field '$key' without options")
                    return null
                }
            }

            val default = when {
                !obj.has("default") -> null
                obj.isNull("default") -> null
                else -> {
                    val raw = obj.get("default")
                    when (raw) {
                        is String, is Boolean, is Int, is Long, is Double -> raw.toString()
                        else -> {
                            logger.warn(TAG, "Ignoring non-scalar default on field '$key'")
                            null
                        }
                    }
                }
            }

            return Field(key = key, label = label, type = type, default = default, options = options)
        }
    }
}
