package com.chrona.ai.parser

import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

class RuleBasedTaskParser(
    private val clock: Clock = Clock.systemDefaultZone(),
    private val zone: ZoneId = ZoneId.systemDefault()
) : TaskParser {
    override fun parse(input: String): List<ParsedTask> {
        return input
            .split(segmentSeparator)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { parseSegment(it) }
    }

    private fun parseSegment(segment: String): ParsedTask {
        val date = detectDate(segment)
        val time = detectExplicitTime(segment) ?: detectDefaultTime(segment)
        val startAt = if (date != null && time != null) LocalDateTime.of(date, time) else null
        val durationHours = if (segment.contains("报告") || segment.contains("写")) 2L else 1L
        val endAt = startAt?.plusHours(durationHours)
        val title = cleanTitle(segment)
        val needsTimeConfirmation = startAt == null
        val confidenceNote = if (needsTimeConfirmation) {
            "无法确定日期或时间，需要用户确认。"
        } else {
            "基于本地规则解析。"
        }

        return ParsedTask(
            title = title.ifBlank { segment },
            startAt = startAt,
            endAt = endAt,
            sourceText = segment,
            confidenceNote = confidenceNote,
            needsTimeConfirmation = needsTimeConfirmation
        )
    }

    private fun detectDate(segment: String): LocalDate? {
        val today = LocalDate.now(clock.withZone(zone))
        return when {
            segment.contains("后天") -> today.plusDays(2)
            segment.contains("明天") -> today.plusDays(1)
            segment.contains("今天") || segment.contains("今晚") || segment.contains("晚上") -> today
            segment.contains("周末") -> today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))
            segment.contains("周六") || segment.contains("星期六") -> {
                today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))
            }
            segment.contains("周日") || segment.contains("星期日") || segment.contains("星期天") -> {
                today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
            }
            else -> null
        }
    }

    private fun detectExplicitTime(segment: String): LocalTime? {
        colonTime.find(segment)?.let { match ->
            val hour = match.groupValues[1].toIntOrNull() ?: return null
            val minute = match.groupValues[2].toIntOrNull() ?: return null
            return LocalTime.of(hour, minute)
        }

        chineseHour.find(segment)?.let { match ->
            val period = match.groupValues[1]
            var hour = match.groupValues[2].toIntOrNull() ?: return null
            if ((period == "下午" || period == "晚上" || period == "今晚") && hour in 1..11) {
                hour += 12
            }
            return LocalTime.of(hour, 0)
        }

        return null
    }

    private fun detectDefaultTime(segment: String): LocalTime? {
        return when {
            segment.contains("早上") || segment.contains("上午") -> LocalTime.of(9, 0)
            segment.contains("中午") -> LocalTime.of(12, 0)
            segment.contains("下午") -> LocalTime.of(15, 0)
            segment.contains("晚上") || segment.contains("今晚") -> LocalTime.of(20, 0)
            segment.contains("健身") -> LocalTime.of(20, 0)
            segment.contains("快递") -> LocalTime.of(15, 0)
            segment.contains("报告") || segment.contains("写") -> LocalTime.of(14, 0)
            detectDate(segment) != null -> LocalTime.of(9, 0)
            else -> null
        }
    }

    private fun cleanTitle(segment: String): String {
        return segment
            .replace(reminderTerms, "")
            .replace(dateTerms, "")
            .replace(colonTime, "")
            .replace(chineseHour, "")
            .replace(timeOfDayTerms, "")
            .trim()
    }

    private companion object {
        private val segmentSeparator = Regex("[,，、;；\\n]+|然后")
        private val colonTime = Regex("""(\d{1,2})[:：](\d{2})""")
        private val chineseHour = Regex("""(上午|早上|中午|下午|晚上|今晚)?(\d{1,2})点""")
        private val reminderTerms = Regex("""提醒我|提醒|请|帮我""")
        private val dateTerms = Regex("""后天|明天|今天|今晚|周末|周六|星期六|周日|星期日|星期天""")
        private val timeOfDayTerms = Regex("""早上|上午|中午|下午|晚上""")
    }
}
