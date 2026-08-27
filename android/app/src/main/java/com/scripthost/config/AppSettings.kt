package com.scripthost.config

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

/**
 * App-level settings (debug mode, light/dark appearance).
 *
 * Persisted in SharedPreferences, separate from [ConfigStore]: ConfigStore
 * holds script-readable key/value config (API keys etc.), while these are
 * app-only preferences scripts cannot read.
 */
class AppSettings(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** When true, script `console.*` messages are also logged to Logcat. */
    var debugMode: Boolean
        get() = prefs.getBoolean(KEY_DEBUG_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_DEBUG_MODE, value).apply()

    /**
     * One of [AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM] (default),
     * [AppCompatDelegate.MODE_NIGHT_NO] or [AppCompatDelegate.MODE_NIGHT_YES].
     * Applied at app start and immediately when changed.
     */
    var nightMode: Int
        get() = prefs.getInt(KEY_NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        set(value) = prefs.edit().putInt(KEY_NIGHT_MODE, value).apply()

    companion object {
        private const val PREFS_NAME = "app_settings"
        private const val KEY_DEBUG_MODE = "debug_mode"
        private const val KEY_NIGHT_MODE = "night_mode"
    }
}
