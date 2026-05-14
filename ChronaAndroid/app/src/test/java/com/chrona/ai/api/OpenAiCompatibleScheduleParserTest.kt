package com.chrona.ai.api

import com.chrona.ai.parser.ParsedTask
import com.chrona.ai.parser.TaskParser
import java.time.LocalDateTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenAiCompatibleScheduleParserTest {
    @Test
    fun settingsAreCompleteOnlyWhenAllFieldsExist() {
        assertFalse(ApiSettings("", "key", "model").isComplete)
        assertFalse(ApiSettings("https://api.example.com", "", "model").isComplete)
        assertFalse(ApiSettings("https://api.example.com", "key", "").isComplete)
        assertTrue(ApiSettings("https://api.example.com", "key", "model").isComplete)
    }

    @Test
    fun appendsChatCompletionsPathWhenBaseUrlDoesNotContainIt() {
        val settings = ApiSettings("https://api.example.com/v1", "key", "model")

        assertEquals(
            "https://api.example.com/v1/chat/completions",
            settings.chatCompletionsUrl
        )
    }

    @Test
    fun keepsExplicitChatCompletionsUrl() {
        val settings = ApiSettings("https://api.example.com/v1/chat/completions", "key", "model")

        assertEquals(
            "https://api.example.com/v1/chat/completions",
            settings.chatCompletionsUrl
        )
    }

    @Test
    fun parsesOpenAiCompatibleJsonContentIntoParsedTasks() {
        val content = """
            [
              {
                "title": "Pick up package",
                "startAt": "2026-05-15T15:00:00",
                "endAt": "2026-05-15T16:00:00",
                "confidenceNote": "AI parsed exact time.",
                "needsTimeConfirmation": false
              }
            ]
        """.trimIndent()

        val result = OpenAiCompatibleScheduleParser.parseTasksFromModelContent(
            content = content,
            sourceText = "Remind me to pick up package tomorrow at 3pm"
        )

        assertEquals(1, result.size)
        assertEquals("Pick up package", result.first().title)
        assertEquals(LocalDateTime.of(2026, 5, 15, 15, 0), result.first().startAt)
        assertEquals(LocalDateTime.of(2026, 5, 15, 16, 0), result.first().endAt)
        assertEquals("Remind me to pick up package tomorrow at 3pm", result.first().sourceText)
        assertEquals("AI parsed exact time.", result.first().confidenceNote)
        assertFalse(result.first().needsTimeConfirmation)
    }

    @Test
    fun parseServiceUsesLocalParserWhenSettingsAreMissing() = runTest {
        val service = ScheduleParseService(
            settingsProvider = { ApiSettings("", "", "") },
            remoteParser = object : RemoteScheduleParser {
                override suspend fun parse(input: String, settings: ApiSettings): List<ParsedTask> {
                    error("Remote parser should not be called")
                }
            },
            fallbackParser = fixedLocalParser()
        )

        val result = service.parse("local-only")

        assertEquals(ParseSource.LOCAL_RULES, result.source)
        assertEquals("Used local parser because API settings are incomplete.", result.message)
        assertEquals("Local task", result.tasks.first().title)
    }

    @Test
    fun parseServiceFallsBackWhenRemoteParserFails() = runTest {
        val service = ScheduleParseService(
            settingsProvider = { ApiSettings("https://api.example.com/v1", "key", "model") },
            remoteParser = object : RemoteScheduleParser {
                override suspend fun parse(input: String, settings: ApiSettings): List<ParsedTask> {
                    error("network unavailable")
                }
            },
            fallbackParser = fixedLocalParser()
        )

        val result = service.parse("needs fallback")

        assertEquals(ParseSource.LOCAL_RULES, result.source)
        assertEquals("AI parsing failed, so Chrona used local rules.", result.message)
        assertEquals("Local task", result.tasks.first().title)
    }

    private fun fixedLocalParser(): TaskParser {
        return object : TaskParser {
            override fun parse(input: String): List<ParsedTask> {
                return listOf(
                    ParsedTask(
                        title = "Local task",
                        startAt = null,
                        endAt = null,
                        sourceText = input,
                        confidenceNote = "local",
                        needsTimeConfirmation = true
                    )
                )
            }
        }
    }
}
