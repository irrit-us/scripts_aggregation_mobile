package com.scripthost.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker that posts a scheduled notification once per day.
 * Enqueued as unique periodic work named "daily_<id>" so re-scheduling the
 * same id replaces the previous request.
 */
class DailyNotificationWorker(
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
        private const val WORK_NAME_PREFIX = "daily_"

        /**
         * Schedule a daily notification at [hour]:[minute] local time.
         * An existing schedule with the same [id] is replaced.
         */
        fun enqueue(context: Context, id: String, hour: Int, minute: Int, title: String, message: String) {
            val workName = WORK_NAME_PREFIX + id
            val initialDelay = NextRunCalculator.millisUntilNext(Calendar.getInstance(), hour, minute)

            val request = PeriodicWorkRequestBuilder<DailyNotificationWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .setInputData(
                    Data.Builder()
                        .putString(KEY_TITLE, title)
                        .putString(KEY_MESSAGE, message)
                        .build()
                )
                .addTag(workName)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                workName,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        /**
         * Cancel the daily notification scheduled under [id], if any.
         */
        fun cancel(context: Context, id: String) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME_PREFIX + id)
        }
    }
}
