package com.scripthost.config

import com.google.common.truth.Truth.assertThat
import com.scripthost.util.ConsoleLogger
import org.json.JSONObject
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import javax.crypto.KeyGenerator

/**
 * Unit tests for [ConfigStore] backed by a temporary storage directory.
 */
class ConfigStoreTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun putAndGet_roundTripsValues() {
        val store = ConfigStore(tempFolder.root, PlaintextCipher, ConsoleLogger())
        store.put("OPENAI_API_KEY", "sk-test-123")
        store.put("MONITOR_URL", "https://example.com/health")

        assertThat(store.get("OPENAI_API_KEY")).isEqualTo("sk-test-123")
        assertThat(store.get("MONITOR_URL")).isEqualTo("https://example.com/health")
        assertThat(store.contains("OPENAI_API_KEY")).isTrue()
        assertThat(store.contains("MISSING")).isFalse()
    }

    @Test
    fun get_missingKey_returnsNull() {
        val store = ConfigStore(tempFolder.root, PlaintextCipher, ConsoleLogger())
        assertThat(store.get("DOES_NOT_EXIST")).isNull()
        assertThat(store.contains("DOES_NOT_EXIST")).isFalse()
        assertThat(store.all()).isEmpty()
    }

    @Test
    fun put_replacesExistingValue() {
        val store = ConfigStore(tempFolder.root, PlaintextCipher, ConsoleLogger())
        store.put("API_KEY", "first")
        store.put("API_KEY", "second")
        assertThat(store.get("API_KEY")).isEqualTo("second")
    }

    @Test
    fun put_blankKeyIsIgnored() {
        val store = ConfigStore(tempFolder.root, PlaintextCipher, ConsoleLogger())
        store.put("   ", "value")
        assertThat(store.all()).isEmpty()
    }

    @Test
    fun configPersistsAcrossStoreInstances() {
        val store = ConfigStore(tempFolder.root, PlaintextCipher, ConsoleLogger())
        store.put("API_KEY", "persisted-value")

        val reloaded = ConfigStore(tempFolder.root, PlaintextCipher, ConsoleLogger())
        assertThat(reloaded.get("API_KEY")).isEqualTo("persisted-value")
        assertThat(File(tempFolder.root, "app_config.json").exists()).isTrue()
    }

    @Test
    fun remove_deletesKeyAndPersists() {
        val store = ConfigStore(tempFolder.root, PlaintextCipher, ConsoleLogger())
        store.put("API_KEY", "value")
        assertThat(store.remove("API_KEY")).isTrue()
        assertThat(store.get("API_KEY")).isNull()

        val reloaded = ConfigStore(tempFolder.root, PlaintextCipher, ConsoleLogger())
        assertThat(reloaded.get("API_KEY")).isNull()
        assertThat(reloaded.all()).isEmpty()
    }

    @Test
    fun remove_unknownKey_returnsFalse() {
        val store = ConfigStore(tempFolder.root, PlaintextCipher, ConsoleLogger())
        assertThat(store.remove("DOES_NOT_EXIST")).isFalse()
    }

    @Test
    fun clear_removesAllEntries() {
        val store = ConfigStore(tempFolder.root, PlaintextCipher, ConsoleLogger())
        store.put("A", "1")
        store.put("B", "2")
        store.clear()
        assertThat(store.all()).isEmpty()
    }

    @Test
    fun putWithCipher_encryptsValuesOnDisk() {
        val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        val cipher = AesGcmValueCipher(key)
        val store = ConfigStore(tempFolder.root, cipher, ConsoleLogger())

        store.put("OPENAI_API_KEY", "sk-super-secret")

        val raw = File(tempFolder.root, "app_config.json").readText()
        assertThat(raw).doesNotContain("sk-super-secret")
        assertThat(store.get("OPENAI_API_KEY")).isEqualTo("sk-super-secret")

        val reloaded = ConfigStore(tempFolder.root, cipher, ConsoleLogger())
        assertThat(reloaded.get("OPENAI_API_KEY")).isEqualTo("sk-super-secret")
    }

    @Test
    fun legacyPlaintextFile_isMigratedOnNextSave() {
        File(tempFolder.root, "app_config.json")
            .writeText(JSONObject().put("KEY", "secret").toString())

        val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        val cipher = AesGcmValueCipher(key)
        val store = ConfigStore(tempFolder.root, cipher, ConsoleLogger())

        assertThat(store.get("KEY")).isEqualTo("secret")

        store.put("OTHER", "x")

        val raw = File(tempFolder.root, "app_config.json").readText()
        assertThat(raw).doesNotContain("secret")
    }
}
