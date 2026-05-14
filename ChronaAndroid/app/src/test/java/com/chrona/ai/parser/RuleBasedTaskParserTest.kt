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
        assertEquals(LocalDateTime.of(2026, 5, 15, 16, 0), result[0].endAt)
        assertEquals("健身", result[1].title)
        assertEquals(LocalDateTime.of(2026, 5, 14, 20, 0), result[1].startAt)
        assertEquals(LocalDateTime.of(2026, 5, 14, 21, 0), result[1].endAt)
        assertEquals("写报告", result[2].title)
        assertEquals(LocalDateTime.of(2026, 5, 16, 14, 0), result[2].startAt)
        assertEquals(LocalDateTime.of(2026, 5, 16, 16, 0), result[2].endAt)
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

    @Test
    fun keepsDateOnlyTaskAsPendingConfirmation() {
        val result = parser.parse("明天开会")

        assertEquals(1, result.size)
        assertEquals("开会", result.first().title)
        assertEquals(null, result.first().startAt)
        assertEquals(null, result.first().endAt)
        assertTrue(result.first().needsTimeConfirmation)
    }

    @Test
    fun keepsInvalidColonTimeAsPendingConfirmation() {
        val result = parser.parse("明天25:00开会")

        assertEquals(1, result.size)
        assertEquals("开会", result.first().title)
        assertEquals(null, result.first().startAt)
        assertEquals(null, result.first().endAt)
        assertTrue(result.first().needsTimeConfirmation)
    }

    @Test
    fun keepsInvalidChineseHourAsPendingConfirmation() {
        val result = parser.parse("明天99点开会")

        assertEquals(1, result.size)
        assertEquals("开会", result.first().title)
        assertEquals(null, result.first().startAt)
        assertEquals(null, result.first().endAt)
        assertTrue(result.first().needsTimeConfirmation)
    }

    @Test
    fun parsesNoonPeriodHourAsAfternoon() {
        val result = parser.parse("明天中午1点吃饭")

        assertEquals(1, result.size)
        assertEquals("吃饭", result.first().title)
        assertEquals(LocalDateTime.of(2026, 5, 15, 13, 0), result.first().startAt)
    }

    @Test
    fun preservesTitleThatStartsWithPleaseCharacter() {
        val result = parser.parse("明天请假")

        assertEquals(1, result.size)
        assertEquals("请假", result.first().title)
        assertEquals(null, result.first().startAt)
        assertTrue(result.first().needsTimeConfirmation)
    }

    @Test
    fun keepsTaskTypeDefaultWithoutDateAsPendingConfirmation() {
        val result = parser.parse("拿快递")

        assertEquals(1, result.size)
        assertEquals("拿快递", result.first().title)
        assertEquals(null, result.first().startAt)
        assertEquals(null, result.first().endAt)
        assertTrue(result.first().needsTimeConfirmation)
    }
}
