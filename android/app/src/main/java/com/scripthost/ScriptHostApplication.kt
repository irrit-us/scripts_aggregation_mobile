package com.scripthost

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.scripthost.config.AesGcmValueCipher
import com.scripthost.config.AppSettings
import com.scripthost.config.ConfigStore
import com.scripthost.config.KeystoreKeyProvider
import com.scripthost.config.ScriptConfigSchemas
import com.scripthost.engine.ScriptManager
import com.scripthost.security.PermissionManager

/**
 * Application class - Initializes global components
 */
class ScriptHostApplication : Application() {

    lateinit var scriptManager: ScriptManager
        private set

    lateinit var permissionManager: PermissionManager
        private set

    lateinit var configStore: ConfigStore
        private set

    lateinit var scriptConfigSchemas: ScriptConfigSchemas
        private set

    lateinit var appSettings: AppSettings
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize managers
        scriptManager = ScriptManager(this)
        permissionManager = PermissionManager(this)
        configStore = ConfigStore(filesDir, AesGcmValueCipher(KeystoreKeyProvider().getOrCreateKey()))
        scriptConfigSchemas = ScriptConfigSchemas(filesDir)
        appSettings = AppSettings(this)

        // Apply persisted light/dark preference before any activity starts
        AppCompatDelegate.setDefaultNightMode(appSettings.nightMode)
    }

    companion object {
        lateinit var instance: ScriptHostApplication
            private set
    }
}
