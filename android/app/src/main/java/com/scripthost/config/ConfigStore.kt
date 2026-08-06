package com.scripthost.config

import org.json.JSONObject
import java.io.File

/**
 * Config Store - file-backed key/value configuration for API keys and settings.
 *
 * Values are persisted as JSON in the app's private storage directory
 * (`app_config.json`). The directory is injected instead of an Android
 * [android.content.Context] so the store is testable on the JVM.
 *
 * Note: values are stored in plaintext inside app-private storage. This keeps
 * the implementation transparent and dependency-light; the Android app
 * sandbox prevents other apps from reading the file.
 */
class ConfigStore(private val storageDir: File) {

    private val configFile = File(storageDir, "app_config.json")
    private val values = LinkedHashMap<String, String>()

    init {
        configFile.parentFile?.mkdirs()
        load()
    }

    /**
     * Get a configured value by key (e.g. "OPENAI_API_KEY").
     */
    fun get(key: String): String? = values[key]

    /**
     * Whether a value exists for [key].
     */
    fun contains(key: String): Boolean = values.containsKey(key)

    /**
     * Snapshot of all configured entries.
     */
    fun all(): Map<String, String> = values.toMap()

    /**
     * Store or replace a value. Blank keys are ignored.
     */
    fun put(key: String, value: String) {
        if (key.isBlank()) return
        values[key] = value
        save()
    }

    /**
     * Remove a key. Returns true when the key existed.
     */
    fun remove(key: String): Boolean {
        val removed = values.remove(key) != null
        if (removed) save()
        return removed
    }

    /**
     * Remove all configured entries.
     */
    fun clear() {
        values.clear()
        save()
    }

    private fun load() {
        if (!configFile.exists()) return
        try {
            val json = JSONObject(configFile.readText())
            json.keys().forEach { key ->
                values[key] = json.getString(key)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun save() {
        try {
            val json = JSONObject()
            values.forEach { (key, value) -> json.put(key, value) }
            configFile.writeText(json.toString(2))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
