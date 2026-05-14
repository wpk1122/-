package com.chrona.ai.data

import com.chrona.ai.parser.ParsedTask
import com.chrona.ai.reminder.ReminderScheduler
import kotlinx.coroutines.flow.Flow
import java.time.ZoneId

class ScheduleRepository(
    private val dao: ScheduleTaskDao,
    private val reminderScheduler: ReminderScheduler,
    private val zoneId: ZoneId = ZoneId.systemDefault()
) {
    fun observeTasks(): Flow<List<ScheduleTask>> = dao.observeActiveTasks()

    suspend fun addParsedTask(parsedTask: ParsedTask): Long {
        val now = System.currentTimeMillis()
        val startAtMillis = parsedTask.startAt?.atZone(zoneId)?.toInstant()?.toEpochMilli()
        val endAtMillis = parsedTask.endAt?.atZone(zoneId)?.toInstant()?.toEpochMilli()
        val id = dao.insert(
            ScheduleTask(
                title = parsedTask.title,
                note = parsedTask.confidenceNote,
                startAt = startAtMillis,
                endAt = endAtMillis,
                status = TaskStatus.PENDING,
                sourceText = parsedTask.sourceText,
                createdAt = now,
                updatedAt = now
            )
        )
        if (startAtMillis != null) {
            reminderScheduler.schedule(id, parsedTask.title, startAtMillis)
        }
        return id
    }

    suspend fun markDone(taskId: Long) {
        dao.updateStatus(taskId, TaskStatus.DONE, System.currentTimeMillis())
    }

    suspend fun delete(taskId: Long) {
        dao.updateStatus(taskId, TaskStatus.DELETED, System.currentTimeMillis())
    }
}
