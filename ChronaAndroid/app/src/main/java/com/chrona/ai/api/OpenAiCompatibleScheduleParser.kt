package com.chrona.ai.api

import com.chrona.ai.parser.ParsedTask
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeParseException
import java.time.format.DateTimeFormatter
import org.json.JSONArray
import org.json.JSONObject

interface RemoteScheduleParser {
    suspend fun parse(input: String, settings: ApiSettings): List<ParsedTask>
}

class OpenAiCompatibleScheduleParser(
    private val clock: Clock = Clock.systemDefaultZone(),
    private val zoneId: ZoneId = ZoneId.systemDefault(),
    private val maxSegmentChars: Int = ScheduleInputPreprocessor.DefaultMaxChars,
    private val maxOutputTokens: Int = 900,
    private val requester: suspend (String, ApiSettings) -> String = ::requestModelContent
) : RemoteScheduleParser {
    override suspend fun parse(input: String, settings: ApiSettings): List<ParsedTask> {
        val segments = ScheduleInputPreprocessor.splitForApi(input, maxSegmentChars)
        return segments.flatMap { segment ->
            val responseText = requester(buildRequestBody(segment, settings), settings)
            val trimmedResponse = responseText.trim()
            val content = if (trimmedResponse.startsWith("{")) {
                val response = JSONObject(trimmedResponse)
                response
                    .optJSONArray("choices")
                    ?.getJSONObject(0)
                    ?.getJSONObject("message")
                    ?.getString("content")
                    ?: trimmedResponse
            } else {
                trimmedResponse
            }

            parseTasksFromModelContent(content, segment)
        }
    }

    internal fun buildRequestBody(input: String, settings: ApiSettings): String {
        val now = LocalDateTime.now(clock.withZone(zoneId))
            .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val systemPrompt = """
            You convert natural-language schedule requests into JSON only.
            Current local date-time: $now
            Time zone: ${zoneId.id}
            Resolve relative dates from the current local date-time.
            Return a JSON array. Each item must include:
            title, startAt, endAt, confidenceNote, needsTimeConfirmation.
            startAt and endAt must be ISO local date-time strings or null.
            If the user says tomorrow, tonight, next week, weekend, or another relative time, calculate it from the current local date-time above.
            Preserve the user's language in titles.
        """.trimIndent()

        return JSONObject()
            .put("model", settings.model.trim())
            .put("temperature", 0)
            .put("max_tokens", maxOutputTokens)
            .put(
                "messages",
                JSONArray()
                    .put(JSONObject().put("role", "system").put("content", systemPrompt))
                    .put(JSONObject().put("role", "user").put("content", input))
            )
            .toString()
    }

    companion object {
        fun parseTasksFromModelContent(content: String, sourceText: String): List<ParsedTask> {
            val array = JSONArray(extractJsonArray(content))
            return (0 until array.length()).mapNotNull { index ->
                val item = array.optJSONObject(index) ?: return@mapNotNull null
                val title = item.optString("title").ifBlank { sourceText.take(60) }
                val startAt = item.optionalLocalDateTime("startAt")
                val endAt = item.optionalLocalDateTime("endAt")

                ParsedTask(
                    title = title,
                    startAt = startAt,
                    endAt = endAt,
                    sourceText = sourceText,
                    confidenceNote = item.optString("confidenceNote").ifBlank { "AI parsed this task." },
                    needsTimeConfirmation = item.optBoolean("needsTimeConfirmation", startAt == null)
                )
            }
        }

        private fun extractJsonArray(content: String): String {
            val withoutFence = content
                .trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()
            val start = withoutFence.indexOf('[')
            val end = withoutFence.lastIndexOf(']')
            require(start >= 0 && end >= start) { "Model response did not contain a JSON array." }
            return withoutFence.substring(start, end + 1)
        }

        private fun JSONObject.optionalLocalDateTime(name: String): LocalDateTime? {
            val value = optString(name).trim()
            if (value.isBlank() || value.equals("null", ignoreCase = true)) {
                return null
            }
            return try {
                LocalDateTime.parse(value)
            } catch (exception: DateTimeParseException) {
                null
            }
        }
    }
}

private suspend fun requestModelContent(requestBody: String, settings: ApiSettings): String {
    val connection = (URL(settings.chatCompletionsUrl).openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        connectTimeout = 15_000
        readTimeout = 20_000
        doOutput = true
        setRequestProperty("Authorization", "Bearer ${settings.apiKey}")
        setRequestProperty("Content-Type", "application/json; charset=utf-8")
        setRequestProperty("Accept", "application/json")
    }

    try {
        OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
            writer.write(requestBody)
        }

        val responseCode = connection.responseCode
        if (responseCode in 200..299) {
            return connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        }

        val errorText = connection.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
        throw IllegalStateException("API request failed: $responseCode ${errorText.orEmpty()}")
    } finally {
        connection.disconnect()
    }
}
