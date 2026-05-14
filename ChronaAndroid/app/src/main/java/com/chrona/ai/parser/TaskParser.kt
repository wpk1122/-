package com.chrona.ai.parser

import java.time.LocalDateTime

interface TaskParser {
    fun parse(input: String): List<ParsedTask>
}

data class ParsedTask(
    val title: String,
    val startAt: LocalDateTime?,
    val endAt: LocalDateTime?,
    val sourceText: String,
    val confidenceNote: String,
    val needsTimeConfirmation: Boolean
)
