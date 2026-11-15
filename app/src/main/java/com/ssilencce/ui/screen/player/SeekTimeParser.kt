package com.ssilencce.ui.screen.player

/**
 * Parses and formats seek positions for the "Seek to..." dialog.
 *
 * Accepted input is a single number of seconds, `mm:ss` or `h:mm:ss`. The formatted value
 * always uses total minutes so tracks longer than an hour remain editable.
 */
object SeekTimeParser {
    fun parse(input: String): Long? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null
        val parts = trimmed.split(':')
        if (parts.size !in 1..3) return null
        val values =
            parts.map { part ->
                part.trim().takeIf { it.isNotEmpty() }?.toLongOrNull() ?: return null
            }
        val seconds =
            when (values.size) {
                1 -> values[0]
                2 -> values[0] * 60 + values[1]
                else -> values[0] * 3600 + values[1] * 60 + values[2]
            }
        if (seconds < 0) return null
        return seconds * 1000
    }

    fun clamp(
        positionMillis: Long,
        durationMillis: Long,
    ): Long = if (durationMillis > 0) positionMillis.coerceIn(0, durationMillis) else positionMillis.coerceAtLeast(0)

    fun format(positionMillis: Long): String {
        val totalSeconds = (positionMillis.coerceAtLeast(0) / 1000)
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%d:%02d".format(minutes, seconds)
    }
}
