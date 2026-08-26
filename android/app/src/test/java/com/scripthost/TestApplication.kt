package com.scripthost

import android.app.Application

/**
 * Application class for Robolectric tests. Skips the production
 * [ScriptHostApplication] initialisation, which requires the AndroidKeyStore
 * (unavailable on the JVM).
 */
class TestApplication : Application()
