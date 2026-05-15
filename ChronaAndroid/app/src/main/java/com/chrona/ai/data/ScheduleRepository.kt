package com.chrona.ai.data

import com.chrona.ai.parser.ParsedTask
import com.chrona.ai.reminder.ReminderScheduler
import kotlinx.coroutines.flow.Flow
import java.time.Clock
import java.time.ZoneId

class ScheduleRepository(
    private val dao: ScheduleTaskDao,
    private val reminderScheduler: ReminderScheduler,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val zoneId: ZoneId = clock.zone
) {
    fun observeTasks(): Flow<List<ScheduleTask>> = dao.observeActiveTasks()

    fun observeBehaviorEvents(): Flow<List<TaskBehaviorEvent>> = dao.observeBehaviorEvents()

    suspend fun addParsedTask(parsedTask: ParsedTask): Long {
        val now = clock.millis()
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
        logEvent(taskId = id, type = BehaviorEventType.CREATED, occurredAt = now)
        return id
    }

    suspend fun markDone(taskId: Long) {
        val now = clock.millis()
        val updated = dao.markDone(taskId, now)
        if (updated > 0) {
            reminderScheduler.cancel(taskId)
            logEvent(taskId = taskId, type = BehaviorEventType.COMPLETED, occurredAt = now)
        }
    }

    suspend fun delete(taskId: Long) {
        val now = clock.millis()
        val updated = dao.markDeleted(taskId, now)
        if (updated > 0) {
            reminderScheduler.cancel(taskId)
            logEvent(taskId = taskId, type = BehaviorEventType.DELETED, occurredAt = now)
        }
    }

    private suspend fun logEvent(taskId: Long, type: BehaviorEventType, occurredAt: Long) {
        runCatching {
            dao.insertBehaviorEvent(
                TaskBehaviorEvent(
                    taskId = taskId,
                    type = type.name,
                    occurredAt = occurredAt
                )
            )
        }
    }
}
