package com.chrona.ai.reminder

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import kotlin.math.max

interface ReminderScheduler {
    fun schedule(taskId: Long, title: String, triggerAtMillis: Long)

    fun cancel(taskId: Long)
}

class WorkManagerReminderScheduler(private val context: Context) : ReminderScheduler {
    override fun schedule(taskId: Long, title: String, triggerAtMillis: Long) {
        val delayMillis = ReminderWork.delayMillis(
            triggerAtMillis = triggerAtMillis,
            nowMillis = System.currentTimeMillis()
        )
        val inputData = Data.Builder()
            .putLong(ReminderWorker.KEY_TASK_ID, taskId)
            .putString(ReminderWorker.KEY_TITLE, title)
            .build()
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            ReminderWork.uniqueName(taskId),
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    override fun cancel(taskId: Long) {
        WorkManager.getInstance(context).cancelUniqueWork(ReminderWork.uniqueName(taskId))
    }
}

internal object ReminderWork {
    fun uniqueName(taskId: Long): String = "reminder-$taskId"

    fun delayMillis(triggerAtMillis: Long, nowMillis: Long): Long {
        return max(0L, triggerAtMillis - nowMillis)
    }
}
