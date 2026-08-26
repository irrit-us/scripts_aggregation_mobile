package com.scripthost.util

/**
 * Logger - minimal logging abstraction so library classes stay testable on
 * the JVM (where android.util.Log is unavailable).
 */
interface Logger {
    /** Log a warning, optionally with a [throwable]. */
    fun warn(tag: String, message: String, throwable: Throwable? = null)

    /** Log an error, optionally with a [throwable]. */
    fun error(tag: String, message: String, throwable: Throwable? = null)
}

/**
 * Logger backed by android.util.Log. Used inside the app.
 */
class AndroidLogger : Logger {
    override fun warn(tag: String, message: String, throwable: Throwable?) {
        android.util.Log.w(tag, message, throwable)
    }

    override fun error(tag: String, message: String, throwable: Throwable?) {
        android.util.Log.e(tag, message, throwable)
    }
}

/**
 * Logger that prints to stdout/stderr. Used in JVM unit tests.
 */
class ConsoleLogger : Logger {
    override fun warn(tag: String, message: String, throwable: Throwable?) {
        println("WARN [$tag] $message")
        throwable?.printStackTrace()
    }

    override fun error(tag: String, message: String, throwable: Throwable?) {
        System.err.println("ERROR [$tag] $message")
        throwable?.printStackTrace()
    }
}
