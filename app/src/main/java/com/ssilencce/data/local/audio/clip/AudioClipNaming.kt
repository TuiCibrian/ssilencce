package com.ssilencce.data.local.audio.clip

object AudioClipNaming {
    private val INVALID_FILE_CHARS = Regex("[\\\\/:*?\"<>|\\p{Cntrl}]")

    fun clipFileName(
        title: String,
        startMs: Long,
        endMs: Long,
        extension: String,
    ): String {
        val base = clipBaseName(title, startMs, endMs)
        return "$base.$extension"
    }

    fun clipTitle(
        title: String,
        startMs: Long,
        endMs: Long,
    ): String = clipBaseName(title, startMs, endMs)

    private fun clipBaseName(
        title: String,
        startMs: Long,
        endMs: Long,
    ): String {
        val cleanTitle =
            title
                .replace(INVALID_FILE_CHARS, "_")
                .trim()
                .trimStart('.')
                .take(MAX_TITLE_CHARS)
                .ifBlank { DEFAULT_TITLE }
        val startSeconds = startMs.coerceAtLeast(0) / 1000
        val endSeconds = endMs.coerceAtLeast(0) / 1000
        return "$cleanTitle ($startSeconds-${endSeconds}s)"
    }

    private const val MAX_TITLE_CHARS = 80
    private const val DEFAULT_TITLE = "Clip"
}
