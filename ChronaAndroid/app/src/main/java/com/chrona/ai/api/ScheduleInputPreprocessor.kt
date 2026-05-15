package com.chrona.ai.api

object ScheduleInputPreprocessor {
    const val DefaultMaxChars: Int = 900

    fun splitForApi(input: String, maxChars: Int = DefaultMaxChars): List<String> {
        val trimmed = input.trim()
        if (trimmed.isBlank()) return emptyList()
        if (trimmed.length <= maxChars) return listOf(trimmed)

        val pieces = trimmed
            .split(Regex("(?<=[。；;\\n])"))
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val result = mutableListOf<String>()
        var current = StringBuilder()
        pieces.forEach { piece ->
            if (piece.length > maxChars) {
                flush(current, result)
                hardSplit(piece, maxChars, result)
            } else if (current.isEmpty()) {
                current.append(piece)
            } else if (current.length + piece.length <= maxChars) {
                current.append(piece)
            } else {
                flush(current, result)
                current.append(piece)
            }
        }
        flush(current, result)
        return result
    }

    private fun hardSplit(piece: String, maxChars: Int, result: MutableList<String>) {
        var start = 0
        while (start < piece.length) {
            val end = (start + maxChars).coerceAtMost(piece.length)
            result += piece.substring(start, end).trim()
            start = end
        }
    }

    private fun flush(current: StringBuilder, result: MutableList<String>) {
        val text = current.toString().trim()
        if (text.isNotBlank()) {
            result += text
        }
        current.clear()
    }
}
