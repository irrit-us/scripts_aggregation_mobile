package com.scripthost

import android.app.Application
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

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize managers
        scriptManager = ScriptManager(this)
        permissionManager = PermissionManager(this)
    }

    companion object {
        lateinit var instance: ScriptHostApplication
            private set
    }
}
