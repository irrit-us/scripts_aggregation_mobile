package com.scripthost.bridge

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.eclipsesource.v8.V8
import com.eclipsesource.v8.V8Object
import com.scripthost.engine.ScriptBridge
import com.scripthost.models.Permission
import com.scripthost.notify.DailyNotificationWorker
import com.scripthost.notify.NextRunCalculator
import com.scripthost.security.PermissionManager
import java.util.Calendar
import java.util.concurrent.atomic.AtomicInteger

/**
 * Notification Bridge - Exposes notifications and daily scheduling to scripts
 * Provides the `Notify` global for immediate notifications and the
 * `Scheduler` global for WorkManager-backed daily notifications.
 */
class NotificationBridge(
    private val context: Context,
    private val permissionManager: PermissionManager,
    private val scriptId: String
) : ScriptBridge {

    private var runtime: V8? = null
    private val notificationIdCounter = AtomicInteger(1000)

    override fun register(runtime: V8) {
        this.runtime = runtime

        // Notify API
        val notifyObject = V8Object(runtime)
        runtime.add("Notify", notifyObject)
        notifyObject.registerJavaMethod(this, "postNotification", "post",
            arrayOf(String::class.java, String::class.java))
        notifyObject.release()

        // Scheduler API
        val schedulerObject = V8Object(runtime)
        runtime.add("Scheduler", schedulerObject)
        schedulerObject.registerJavaMethod(this, "scheduleDaily", "scheduleDaily",
            arrayOf(String::class.java, Int::class.java, Int::class.java,
                String::class.java, String::class.java))
        schedulerObject.registerJavaMethod(this, "cancelSchedule", "cancel",
            arrayOf(String::class.java))
        schedulerObject.release()
    }

    override fun unregister() {
        runtime = null
    }

    // Notify API

    /**
     * Post an immediate notification. Silently ignored without the
     * NOTIFICATIONS permission or when notifications are disabled.
     */
    @Suppress("unused")
    fun postNotification(title: String, message: String) {
        if (!permissionManager.hasScriptPermission(scriptId, Permission.NOTIFICATIONS)) {
            return
        }

        createChannel()

        val notificationManager = NotificationManagerCompat.from(context)
        if (!notificationManager.areNotificationsEnabled()) {
            return
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .build()

        try {
            notificationManager.notify(notificationIdCounter.incrementAndGet(), notification)
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS denied at runtime
        }
    }

    // Scheduler API

    /**
     * Schedule a daily notification at hour:minute local time under [id].
     * Returns false when permission is missing or the time is invalid.
     */
    @Suppress("unused")
    fun scheduleDaily(id: String, hour: Int, minute: Int, title: String, message: String): Boolean {
        if (!permissionManager.hasScriptPermission(scriptId, Permission.NOTIFICATIONS)) {
            return false
        }

        // Validates the time and computes the initial delay.
        try {
            NextRunCalculator.millisUntilNext(Calendar.getInstance(), hour, minute)
        } catch (e: IllegalArgumentException) {
            return false
        }

        DailyNotificationWorker.enqueue(context, id, hour, minute, title, message)
        return true
    }

    /**
     * Cancel the daily notification scheduled under [id].
     */
    @Suppress("unused")
    fun cancelSchedule(id: String): Boolean {
        if (!permissionManager.hasScriptPermission(scriptId, Permission.NOTIFICATIONS)) {
            return false
        }

        DailyNotificationWorker.cancel(context, id)
        return true
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Script notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "scripthost_scripts"
    }
}
