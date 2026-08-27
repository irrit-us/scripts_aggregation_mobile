package com.scripthost.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker that posts a single scheduled notification.
 * Enqueued as unique one-time work named "oneshot_<id>" so re-scheduling the
 * same id replaces the previous request and [cancel] removes it.
 */
class OneShotNotificationWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        return try {
            val title = inputData.getString(KEY_TITLE) ?: return Result.failure()
            val message = inputData.getString(KEY_MESSAGE) ?: return Result.failure()

            createChannel()

            val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .build()

            // Stable id derived from the unique work name, carried as a tag.
            val workName = tags.firstOrNull { it.startsWith(WORK_NAME_PREFIX) }
            val notificationId = workName?.hashCode() ?: 0

            NotificationManagerCompat.from(applicationContext).notify(notificationId, notification)
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Scheduled notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val KEY_TITLE = "title"
        const val KEY_MESSAGE = "message"

        private const val CHANNEL_ID = "scripthost_scheduled"
        private const val WORK_NAME_PREFIX = "oneshot_"

        /**
         * Schedule a one-shot notification [delayMs] milliseconds from now.
         * WorkManager delays are inexact (battery-friendly); treat this as
         * minute-scale precision, not an alarm clock. An existing schedule
         * with the same [id] is replaced.
         */
        fun enqueueIn(context: Context, id: String, delayMs: Long, title: String, message: String) {
            enqueue(context, id, delayMs.coerceAtLeast(0), title, message)
        }

        /**
         * Schedule a one-shot notification at the absolute time [epochMs]
         * (milliseconds since the Unix epoch). Times in the past fire as soon
         * as possible.
         */
        fun enqueueAt(context: Context, id: String, epochMs: Long, title: String, message: String) {
            val delayMs = (epochMs - System.currentTimeMillis()).coerceAtLeast(0)
            enqueue(context, id, delayMs, title, message)
        }

        /**
         * Cancel the one-shot notification scheduled under [id], if any.
         */
        fun cancel(context: Context, id: String) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME_PREFIX + id)
        }

        private fun enqueue(context: Context, id: String, delayMs: Long, title: String, message: String) {
            val workName = WORK_NAME_PREFIX + id

            val request = OneTimeWorkRequestBuilder<OneShotNotificationWorker>()
                .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                .setInputData(
                    Data.Builder()
                        .putString(KEY_TITLE, title)
                        .putString(KEY_MESSAGE, message)
                        .build()
                )
                .addTag(workName)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                workName,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
