package com.chrona.ai.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class RuleBasedTaskParserTest {
    private val zone = ZoneId.of("Asia/Shanghai")
    private val clock = Clock.fixed(Instant.parse("2026-05-14T02:00:00Z"), zone)
    private val parser = RuleBasedTaskParser(clock, zone)

    @Test
    fun parsesThreeChineseTasksWithDefaultTimes() {
        val result = parser.parse("明天提醒我拿快递，晚上健身，周末写报告")

        assertEquals(3, result.size)
        assertEquals("拿快递", result[0].title)
        assertEquals(LocalDateTime.of(2026, 5, 15, 15, 0), result[0].startAt)
        assertEquals("健身", result[1].title)
        assertEquals(LocalDateTime.of(2026, 5, 14, 20, 0), result[1].startAt)
        assertEquals("写报告", result[2].title)
        assertEquals(LocalDateTime.of(2026, 5, 16, 14, 0), result[2].startAt)
    }

    @Test
    fun parsesExplicitAfternoonHour() {
        val result = parser.parse("明天下午3点开会")

        assertEquals(1, result.size)
        assertEquals("开会", result.first().title)
        assertEquals(LocalDateTime.of(2026, 5, 15, 15, 0), result.first().startAt)
    }

    @Test
    fun keepsTaskWithoutTimeAsPendingConfirmation() {
        val result = parser.parse("整理书桌")

        assertEquals(1, result.size)
        assertEquals("整理书桌", result.first().title)
        assertEquals(null, result.first().startAt)
        assertTrue(result.first().needsTimeConfirmation)
        assertNotNull(result.first().confidenceNote)
    }
}
