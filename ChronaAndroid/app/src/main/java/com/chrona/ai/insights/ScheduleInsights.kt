package com.chrona.ai.insights

import com.chrona.ai.data.BehaviorEventType
import com.chrona.ai.data.ScheduleTask
import com.chrona.ai.data.TaskBehaviorEvent
import com.chrona.ai.data.TaskStatus
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

data class ScheduleInsight(
    val todayTaskCount: Int,
    val todayCompletedCount: Int,
    val completionRatePercent: Int,
    val productiveHourLabel: String,
    val overduePendingCount: Int,
    val suggestions: List<String>
)

object ScheduleInsightCalculator {
    fun calculate(
        tasks: List<ScheduleTask>,
        events: List<TaskBehaviorEvent>,
        now: Instant,
        zoneId: ZoneId
    ): ScheduleInsight {
        val today = now.atZone(zoneId).toLocalDate()
        val todayCreatedEvents = events.filter {
            it.type == BehaviorEventType.CREATED.name &&
                Instant.ofEpochMilli(it.occurredAt).atZone(zoneId).toLocalDate() == today
        }
        val todayCompletedEvents = events.filter {
            it.type == BehaviorEventType.COMPLETED.name &&
                Instant.ofEpochMilli(it.occurredAt).atZone(zoneId).toLocalDate() == today
        }

        val todayTaskCount = todayCreatedEvents
            .mapNotNull { it.taskId }
            .distinct()
            .count()
        val todayCompletedCount = todayCompletedEvents
            .mapNotNull { it.taskId }
            .distinct()
            .count()
        val completionRate = if (todayTaskCount == 0) {
            0
        } else {
            ((todayCompletedCount.toDouble() / todayTaskCount.toDouble()) * 100).roundToInt()
        }
        val productiveHour = todayCompletedEvents
            .groupBy { Instant.ofEpochMilli(it.occurredAt).atZone(zoneId).hour }
            .maxWithOrNull(compareBy<Map.Entry<Int, List<TaskBehaviorEvent>>> { it.value.size }.thenBy { -it.key })
            ?.key
        val productiveHourLabel = productiveHour?.let { formatHourWindow(it) } ?: "暂无记录"
        val nowMillis = now.toEpochMilli()
        val overduePendingCount = tasks.count {
            it.status == TaskStatus.PENDING && it.startAt != null && it.startAt < nowMillis
        }

        return ScheduleInsight(
            todayTaskCount = todayTaskCount,
            todayCompletedCount = todayCompletedCount,
            completionRatePercent = completionRate,
            productiveHourLabel = productiveHourLabel,
            overduePendingCount = overduePendingCount,
            suggestions = buildSuggestions(
                todayTaskCount = todayTaskCount,
                todayCompletedCount = todayCompletedCount,
                completionRate = completionRate,
                productiveHourLabel = productiveHourLabel,
                overduePendingCount = overduePendingCount
            )
        )
    }

    private fun formatHourWindow(hour: Int): String {
        val start = DateTimeFormatter.ofPattern("HH:mm")
            .format(java.time.LocalTime.of(hour, 0))
        val end = DateTimeFormatter.ofPattern("HH:mm")
            .format(java.time.LocalTime.of((hour + 1) % 24, 0))
        return "$start-$end"
    }

    private fun buildSuggestions(
        todayTaskCount: Int,
        todayCompletedCount: Int,
        completionRate: Int,
        productiveHourLabel: String,
        overduePendingCount: Int
    ): List<String> {
        if (todayTaskCount == 0) {
            return listOf(
                "先安排一件最重要的事，Chrona 会从今天开始学习你的节奏。",
                "完成或删除任务后，我会把这些行为变成更贴近你的建议。"
            )
        }

        val suggestions = mutableListOf(
            "今天已完成 $todayCompletedCount/$todayTaskCount 件任务，完成率 $completionRate%。"
        )
        if (productiveHourLabel != "暂无记录") {
            suggestions += "你在 $productiveHourLabel 更容易完成任务，可以把重点事项放在这个时段。"
        }
        if (overduePendingCount > 0) {
            suggestions += "还有 $overduePendingCount 件任务已经过了计划时间，我可以帮你先处理最重要的一件。"
        }
        if (completionRate < 60) {
            suggestions += "今天先缩小范围，保留最关键的两件事会更稳。"
        } else {
            suggestions += "节奏不错，明天可以继续沿用今天表现好的时间段。"
        }
        return suggestions
    }
}
