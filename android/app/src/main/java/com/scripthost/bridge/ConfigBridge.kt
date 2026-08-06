package com.scripthost.bridge

import com.eclipsesource.v8.V8
import com.eclipsesource.v8.V8Array
import com.eclipsesource.v8.V8Object
import com.scripthost.config.ConfigStore
import com.scripthost.engine.ScriptBridge
import com.scripthost.models.Permission
import com.scripthost.security.PermissionManager

/**
 * Config Bridge - Exposes the app's configured keys and settings to scripts.
 *
 * The [Permission.CONFIG] permission is non-dangerous, so it is auto-granted
 * to any installed script that declares it. Any script with the CONFIG
 * permission can read the keys the user configured in Settings. Users should
 * therefore only install scripts they trust.
 */
class ConfigBridge(
    private val configStore: ConfigStore,
    private val permissionManager: PermissionManager
) : ScriptBridge {

    private var runtime: V8? = null

    override fun register(runtime: V8) {
        this.runtime = runtime

        val configObject = V8Object(runtime)
        runtime.add("Config", configObject)
        configObject.registerJavaMethod(this, "getConfig", "get",
            arrayOf(String::class.java))
        configObject.registerJavaMethod(this, "listKeys", "keys", emptyArray())
        configObject.release()
    }

    override fun unregister() {
        runtime = null
    }

    /**
     * Get a configured value by key (e.g. "OPENAI_API_KEY").
     */
    @Suppress("unused")
    fun getConfig(key: String): String? {
        if (!permissionManager.hasPermission(Permission.CONFIG)) {
            return null
        }
        return configStore.get(key)
    }

    /**
     * List all configured keys. Values are not exposed.
     */
    @Suppress("unused")
    fun listKeys(): V8Array? {
        val runtime = this.runtime ?: return null
        if (!permissionManager.hasPermission(Permission.CONFIG)) {
            return null
        }

        val array = V8Array(runtime)
        configStore.all().keys.forEach { key -> array.push(key) }
        return array
    }
}
