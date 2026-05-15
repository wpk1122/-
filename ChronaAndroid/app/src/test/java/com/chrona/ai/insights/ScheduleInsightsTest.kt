package com.chrona.ai.insights

import com.chrona.ai.data.BehaviorEventType
import com.chrona.ai.data.ScheduleTask
import com.chrona.ai.data.TaskBehaviorEvent
import com.chrona.ai.data.TaskStatus
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleInsightsTest {
    private val zoneId = ZoneId.of("Asia/Shanghai")
    private val now = Instant.parse("2026-05-15T13:00:00Z")

    @Test
    fun noHistoryReturnsGentleDefaults() {
        val insight = ScheduleInsightCalculator.calculate(
            tasks = emptyList(),
            events = emptyList(),
            now = now,
            zoneId = zoneId
        )

        assertEquals(0, insight.todayTaskCount)
        assertEquals(0, insight.todayCompletedCount)
        assertEquals(0, insight.completionRatePercent)
        assertEquals("暂无记录", insight.productiveHourLabel)
        assertEquals(0, insight.overduePendingCount)
        assertTrue(insight.suggestions.any { it.contains("先安排一件最重要的事") })
    }

    @Test
    fun completedEventsProduceProgressAndProductiveHour() {
        val events = listOf(
            event(1, BehaviorEventType.CREATED, "2026-05-15T01:00:00Z"),
            event(1, BehaviorEventType.COMPLETED, "2026-05-15T12:10:00Z"),
            event(2, BehaviorEventType.CREATED, "2026-05-15T02:00:00Z"),
            event(2, BehaviorEventType.COMPLETED, "2026-05-15T12:30:00Z"),
            event(3, BehaviorEventType.CREATED, "2026-05-15T03:00:00Z")
        )
        val tasks = listOf(
            task(
                id = 3,
                title = "Review notes",
                startAt = Instant.parse("2026-05-15T09:00:00Z").toEpochMilli()
            )
        )

        val insight = ScheduleInsightCalculator.calculate(
            tasks = tasks,
            events = events,
            now = now,
            zoneId = zoneId
        )

        assertEquals(3, insight.todayTaskCount)
        assertEquals(2, insight.todayCompletedCount)
        assertEquals(67, insight.completionRatePercent)
        assertEquals("20:00-21:00", insight.productiveHourLabel)
        assertEquals(1, insight.overduePendingCount)
        assertTrue(insight.suggestions.any { it.contains("20:00-21:00") })
        assertTrue(insight.suggestions.any { it.contains("1 件") })
    }

    private fun event(taskId: Long, type: BehaviorEventType, instant: String): TaskBehaviorEvent {
        return TaskBehaviorEvent(
            taskId = taskId,
            type = type.name,
            occurredAt = Instant.parse(instant).toEpochMilli()
        )
    }

    private fun task(id: Long, title: String, startAt: Long): ScheduleTask {
        return ScheduleTask(
            id = id,
            title = title,
            note = null,
            startAt = startAt,
            endAt = null,
            status = TaskStatus.PENDING,
            sourceText = title,
            createdAt = startAt,
            updatedAt = startAt
        )
    }
}
