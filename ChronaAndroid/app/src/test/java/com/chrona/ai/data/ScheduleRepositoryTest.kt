package com.chrona.ai.data

import com.chrona.ai.parser.ParsedTask
import com.chrona.ai.reminder.ReminderScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class ScheduleRepositoryTest {
    private val zoneId: ZoneId = ZoneId.of("Asia/Shanghai")
    private val clock: Clock = Clock.fixed(Instant.parse("2026-05-14T06:30:00Z"), zoneId)

    @Test
    fun timedParsedTaskPersistsMillisAndSchedulesReminder() = runTest {
        val dao = FakeScheduleTaskDao(nextId = 42)
        val scheduler = FakeReminderScheduler()
        val repository = ScheduleRepository(dao, scheduler, clock)

        val id = repository.addParsedTask(
            parsedTask(
                title = "Submit report",
                startAt = LocalDateTime.of(2026, 5, 15, 14, 0),
                endAt = LocalDateTime.of(2026, 5, 15, 16, 0)
            )
        )

        assertEquals(42, id)
        assertEquals(1, dao.inserted.size)
        assertEquals(Instant.parse("2026-05-15T06:00:00Z").toEpochMilli(), dao.inserted.single().startAt)
        assertEquals(Instant.parse("2026-05-15T08:00:00Z").toEpochMilli(), dao.inserted.single().endAt)
        assertEquals(clock.millis(), dao.inserted.single().createdAt)
        assertEquals(clock.millis(), dao.inserted.single().updatedAt)
        assertEquals(listOf(ScheduledReminder(42, "Submit report", Instant.parse("2026-05-15T06:00:00Z").toEpochMilli())), scheduler.scheduled)
        assertEquals(1, dao.events.size)
        assertEquals(42L, dao.events.single().taskId)
        assertEquals(BehaviorEventType.CREATED.name, dao.events.single().type)
        assertEquals(clock.millis(), dao.events.single().occurredAt)
    }

    @Test
    fun untimedParsedTaskPersistsButDoesNotScheduleReminder() = runTest {
        val dao = FakeScheduleTaskDao(nextId = 7)
        val scheduler = FakeReminderScheduler()
        val repository = ScheduleRepository(dao, scheduler, clock)

        val id = repository.addParsedTask(parsedTask(title = "Buy milk"))

        assertEquals(7, id)
        assertEquals(null, dao.inserted.single().startAt)
        assertEquals(null, dao.inserted.single().endAt)
        assertEquals(emptyList<ScheduledReminder>(), scheduler.scheduled)
        assertEquals(BehaviorEventType.CREATED.name, dao.events.single().type)
    }

    @Test
    fun markDoneCancelsReminderOnlyWhenTransitionSucceeds() = runTest {
        val dao = FakeScheduleTaskDao(markDoneResult = 1)
        val scheduler = FakeReminderScheduler()
        val repository = ScheduleRepository(dao, scheduler, clock)

        repository.markDone(5)

        assertEquals(listOf(StatusTransition(5, clock.millis())), dao.markDoneCalls)
        assertEquals(listOf(5L), scheduler.cancelled)
        assertEquals(1, dao.events.size)
        assertEquals(5L, dao.events.single().taskId)
        assertEquals(BehaviorEventType.COMPLETED.name, dao.events.single().type)

        dao.markDoneResult = 0
        repository.markDone(6)

        assertEquals(listOf(5L), scheduler.cancelled)
        assertEquals(1, dao.events.size)
    }

    @Test
    fun deleteCancelsReminderOnlyWhenTransitionSucceeds() = runTest {
        val dao = FakeScheduleTaskDao(markDeletedResult = 1)
        val scheduler = FakeReminderScheduler()
        val repository = ScheduleRepository(dao, scheduler, clock)

        repository.delete(9)

        assertEquals(listOf(StatusTransition(9, clock.millis())), dao.markDeletedCalls)
        assertEquals(listOf(9L), scheduler.cancelled)
        assertEquals(1, dao.events.size)
        assertEquals(9L, dao.events.single().taskId)
        assertEquals(BehaviorEventType.DELETED.name, dao.events.single().type)

        dao.markDeletedResult = 0
        repository.delete(10)

        assertEquals(listOf(9L), scheduler.cancelled)
        assertEquals(1, dao.events.size)
    }

    private fun parsedTask(
        title: String,
        startAt: LocalDateTime? = null,
        endAt: LocalDateTime? = null
    ): ParsedTask {
        return ParsedTask(
            title = title,
            startAt = startAt,
            endAt = endAt,
            sourceText = title,
            confidenceNote = "test note",
            needsTimeConfirmation = startAt == null
        )
    }
}

private class FakeScheduleTaskDao(
    private val nextId: Long = 1,
    var markDoneResult: Int = 0,
    var markDeletedResult: Int = 0
) : ScheduleTaskDao {
    val inserted = mutableListOf<ScheduleTask>()
    val markDoneCalls = mutableListOf<StatusTransition>()
    val markDeletedCalls = mutableListOf<StatusTransition>()
    val events = mutableListOf<TaskBehaviorEvent>()
    private val tasks = MutableStateFlow<List<ScheduleTask>>(emptyList())
    private val behaviorEvents = MutableStateFlow<List<TaskBehaviorEvent>>(emptyList())

    override suspend fun insert(task: ScheduleTask): Long {
        inserted += task
        return nextId
    }

    override fun observeActiveTasks(): Flow<List<ScheduleTask>> = tasks

    override suspend fun markDone(taskId: Long, updatedAt: Long): Int {
        markDoneCalls += StatusTransition(taskId, updatedAt)
        return markDoneResult
    }

    override suspend fun markDeleted(taskId: Long, updatedAt: Long): Int {
        markDeletedCalls += StatusTransition(taskId, updatedAt)
        return markDeletedResult
    }

    override suspend fun getById(taskId: Long): ScheduleTask? = null

    override suspend fun insertBehaviorEvent(event: TaskBehaviorEvent): Long {
        events += event
        behaviorEvents.value = events
        return events.size.toLong()
    }

    override fun observeBehaviorEvents(): Flow<List<TaskBehaviorEvent>> = behaviorEvents
}

private class FakeReminderScheduler : ReminderScheduler {
    val scheduled = mutableListOf<ScheduledReminder>()
    val cancelled = mutableListOf<Long>()

    override fun schedule(taskId: Long, title: String, triggerAtMillis: Long) {
        scheduled += ScheduledReminder(taskId, title, triggerAtMillis)
    }

    override fun cancel(taskId: Long) {
        cancelled += taskId
    }
}

private data class ScheduledReminder(
    val taskId: Long,
    val title: String,
    val triggerAtMillis: Long
)

private data class StatusTransition(
    val taskId: Long,
    val updatedAt: Long
)
