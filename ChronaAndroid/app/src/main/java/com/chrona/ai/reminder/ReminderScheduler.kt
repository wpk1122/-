package com.chrona.ai.reminder

interface ReminderScheduler {
    fun schedule(taskId: Long, title: String, triggerAtMillis: Long)
}
