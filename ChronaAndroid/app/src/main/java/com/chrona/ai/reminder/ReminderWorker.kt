package com.chrona.ai.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.chrona.ai.R

class ReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val taskId = inputData.getLong(KEY_TASK_ID, -1L)
        if (!ReminderNotification.isValidTaskId(taskId)) return Result.failure()

        val title = inputData.getString(KEY_TITLE) ?: return Result.failure()

        createNotificationChannel()

        if (!canPostNotifications()) {
            return Result.success()
        }

        val notificationBuilder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Chrona 提醒")
            .setContentText(title)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        createContentIntent(taskId)?.let(notificationBuilder::setContentIntent)

        return try {
            NotificationManagerCompat.from(applicationContext)
                .notify(notificationId(taskId), notificationBuilder.build())
            Result.success()
        } catch (_: SecurityException) {
            Result.success()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val notificationManager = applicationContext.getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun canPostNotifications(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
    }

    private fun createContentIntent(taskId: Long): PendingIntent? {
        val launchIntent = applicationContext.packageManager
            .getLaunchIntentForPackage(applicationContext.packageName)
            ?.apply { addFlags(ReminderNotification.launchIntentFlags) }
            ?: return null

        return PendingIntent.getActivity(
            applicationContext,
            notificationId(taskId),
            launchIntent,
            ReminderNotification.pendingIntentFlags
        )
    }

    private fun notificationId(taskId: Long): Int {
        return (taskId xor (taskId ushr 32)).toInt()
    }

    companion object {
        const val KEY_TASK_ID = "task_id"
        const val KEY_TITLE = "title"

        private const val CHANNEL_ID = "chrona_reminders"
        private const val CHANNEL_NAME = "Chrona reminders"
    }
}

internal object ReminderNotification {
    const val launchIntentFlags: Int = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    const val pendingIntentFlags: Int = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

    fun isValidTaskId(taskId: Long): Boolean = taskId >= 0
}
