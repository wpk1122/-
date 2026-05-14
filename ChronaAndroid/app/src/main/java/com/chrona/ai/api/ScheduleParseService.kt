package com.chrona.ai.api

import com.chrona.ai.parser.ParsedTask
import com.chrona.ai.parser.TaskParser
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ScheduleParseService(
    private val settingsProvider: () -> ApiSettings,
    private val remoteParser: RemoteScheduleParser,
    private val fallbackParser: TaskParser,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    suspend fun parse(input: String): ParseResult {
        val settings = settingsProvider()
        if (!settings.isComplete) {
            return parseLocally(input, "Used local parser because API settings are incomplete.")
        }

        return try {
            val remoteTasks = withContext(dispatcher) {
                remoteParser.parse(input, settings)
            }
            if (remoteTasks.isNotEmpty()) {
                ParseResult(
                    tasks = remoteTasks,
                    source = ParseSource.USER_API,
                    message = "AI parsed schedule from your API."
                )
            } else {
                parseLocally(input, "AI returned no tasks, so Chrona used local rules.")
            }
        } catch (exception: Exception) {
            parseLocally(input, "AI parsing failed, so Chrona used local rules.")
        }
    }

    private suspend fun parseLocally(input: String, message: String): ParseResult {
        return ParseResult(
            tasks = withContext(dispatcher) { fallbackParser.parse(input) },
            source = ParseSource.LOCAL_RULES,
            message = message
        )
    }
}

data class ParseResult(
    val tasks: List<ParsedTask>,
    val source: ParseSource,
    val message: String
)

enum class ParseSource {
    USER_API,
    LOCAL_RULES
}
