package com.scripthost.config

import com.scripthost.util.AndroidLogger
import com.scripthost.util.Logger
import org.json.JSONObject
import java.io.File

/**
 * Config Store - file-backed key/value configuration for API keys and settings.
 *
 * Values are persisted as JSON in the app's private storage directory
 * (`app_config.json`). The directory is injected instead of an Android
 * [android.content.Context] so the store is testable on the JVM.
 *
 * At-rest encryption: each value is encrypted with [cipher] before it is
 * written to disk (in the app, an [AesGcmValueCipher] backed by an
 * AndroidKeyStore key; [PlaintextCipher] by default for tests).
 *
 * Migration: a value that fails to decrypt is treated as legacy plaintext
 * from before encryption was introduced. It stays readable as-is and is
 * transparently re-encrypted on the next [save].
 */
class ConfigStore(
    private val storageDir: File,
    private val cipher: ValueCipher = PlaintextCipher,
    private val logger: Logger = AndroidLogger()
) {

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
                values[key] = decryptValue(json.getString(key))
            }
        } catch (e: Exception) {
            logger.warn(TAG, "Failed to load config file", e)
        }
    }

    private fun save() {
        try {
            val json = JSONObject()
            values.forEach { (key, value) -> json.put(key, cipher.encrypt(value)) }
            configFile.writeText(json.toString(2))
        } catch (e: Exception) {
            logger.warn(TAG, "Failed to save config file", e)
        }
    }

    /**
     * Decrypt a stored value. Values that fail decryption are legacy plaintext
     * and are returned unchanged; they are re-encrypted on the next [save].
     */
    private fun decryptValue(stored: String): String {
        return try {
            cipher.decrypt(stored)
        } catch (e: Exception) {
            logger.warn(TAG, "Treating undecryptable value as legacy plaintext")
            stored
        }
    }

    private companion object {
        const val TAG = "ConfigStore"
    }
}
