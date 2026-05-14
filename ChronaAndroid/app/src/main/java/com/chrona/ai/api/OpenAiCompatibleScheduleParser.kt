package com.chrona.ai.api

import com.chrona.ai.parser.ParsedTask
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDateTime
import java.time.format.DateTimeParseException
import org.json.JSONArray
import org.json.JSONObject

interface RemoteScheduleParser {
    suspend fun parse(input: String, settings: ApiSettings): List<ParsedTask>
}

class OpenAiCompatibleScheduleParser : RemoteScheduleParser {
    override suspend fun parse(input: String, settings: ApiSettings): List<ParsedTask> {
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
                writer.write(buildRequestBody(input, settings))
            }

            val responseCode = connection.responseCode
            val responseText = if (responseCode in 200..299) {
                connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } else {
                val errorText = connection.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                throw IllegalStateException("API request failed: $responseCode ${errorText.orEmpty()}")
            }

            val response = JSONObject(responseText)
            val content = response
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")

            return parseTasksFromModelContent(content, input)
        } finally {
            connection.disconnect()
        }
    }

    private fun buildRequestBody(input: String, settings: ApiSettings): String {
        val systemPrompt = """
            You convert natural-language schedule requests into JSON only.
            Return a JSON array. Each item must include:
            title, startAt, endAt, confidenceNote, needsTimeConfirmation.
            startAt and endAt must be ISO local date-time strings or null.
            Preserve the user's language in titles.
        """.trimIndent()

        return JSONObject()
            .put("model", settings.model.trim())
            .put("temperature", 0)
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
