package com.scripthost.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Unit tests for [ConfigStore] backed by a temporary storage directory.
 */
class ConfigStoreTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun putAndGet_roundTripsValues() {
        val store = ConfigStore(tempFolder.root)
        store.put("OPENAI_API_KEY", "sk-test-123")
        store.put("MONITOR_URL", "https://example.com/health")

        assertEquals("sk-test-123", store.get("OPENAI_API_KEY"))
        assertEquals("https://example.com/health", store.get("MONITOR_URL"))
        assertTrue(store.contains("OPENAI_API_KEY"))
        assertFalse(store.contains("MISSING"))
    }

    @Test
    fun get_missingKey_returnsNull() {
        val store = ConfigStore(tempFolder.root)
        assertNull(store.get("DOES_NOT_EXIST"))
        assertFalse(store.contains("DOES_NOT_EXIST"))
        assertTrue(store.all().isEmpty())
    }

    @Test
    fun put_replacesExistingValue() {
        val store = ConfigStore(tempFolder.root)
        store.put("API_KEY", "first")
        store.put("API_KEY", "second")
        assertEquals("second", store.get("API_KEY"))
    }

    @Test
    fun put_blankKeyIsIgnored() {
        val store = ConfigStore(tempFolder.root)
        store.put("   ", "value")
        assertTrue(store.all().isEmpty())
    }

    @Test
    fun configPersistsAcrossStoreInstances() {
        val store = ConfigStore(tempFolder.root)
        store.put("API_KEY", "persisted-value")

        val reloaded = ConfigStore(tempFolder.root)
        assertEquals("persisted-value", reloaded.get("API_KEY"))
        assertTrue(File(tempFolder.root, "app_config.json").exists())
    }

    @Test
    fun remove_deletesKeyAndPersists() {
        val store = ConfigStore(tempFolder.root)
        store.put("API_KEY", "value")
        assertTrue(store.remove("API_KEY"))
        assertNull(store.get("API_KEY"))

        val reloaded = ConfigStore(tempFolder.root)
        assertNull(reloaded.get("API_KEY"))
        assertTrue(reloaded.all().isEmpty())
    }

    @Test
    fun remove_unknownKey_returnsFalse() {
        val store = ConfigStore(tempFolder.root)
        assertFalse(store.remove("DOES_NOT_EXIST"))
    }

    @Test
    fun clear_removesAllEntries() {
        val store = ConfigStore(tempFolder.root)
        store.put("A", "1")
        store.put("B", "2")
        store.clear()
        assertTrue(store.all().isEmpty())
    }
}
