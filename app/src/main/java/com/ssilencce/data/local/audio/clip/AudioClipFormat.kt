package com.ssilencce.data.local.audio.clip

import com.ssilencce.extensions.hasR

/**
 * Output formats the audio cutter can produce without re-encoding.
 *
 * Constants are kept local instead of referencing [android.media.MediaMuxer.OutputFormat]
 * so older devices never resolve format constants added in newer API levels.
 */
enum class AudioClipFormat(
    val extension: String,
    val mimeType: String,
) {
    M4A("m4a", "audio/mp4"),
    OGG("ogg", "audio/ogg"),
    MP3("mp3", "audio/mpeg"),
    ;

    internal val muxerOutputFormat: Int?
        get() =
            when (this) {
                M4A -> MUXER_OUTPUT_MPEG_4
                OGG -> if (hasR()) MUXER_OUTPUT_OGG else null
                MP3 -> null
            }

    companion object {
        // MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
        private const val MUXER_OUTPUT_MPEG_4 = 0

        // MediaMuxer.OutputFormat.MUXER_OUTPUT_OGG (API 30)
        private const val MUXER_OUTPUT_OGG = 4

        private const val MIME_AAC = "audio/mp4a-latm"
        private const val MIME_MPEG = "audio/mpeg"
        private const val MIME_OPUS = "audio/opus"
        private const val MIME_VORBIS = "audio/vorbis"

        fun fromMimeType(mimeType: String?): AudioClipFormat? =
            when (mimeType) {
                MIME_AAC -> M4A
                MIME_MPEG -> MP3
                MIME_OPUS, MIME_VORBIS -> OGG
                else -> null
            }
    }
}

enum class AudioClipUnsupportedReason {
    Format,
    RequiresNewerAndroid,
    Unreadable,
}

data class AudioClipInspection(
    val format: AudioClipFormat?,
    val durationMs: Long = 0L,
    val reason: AudioClipUnsupportedReason? = null,
    val detail: String? = null,
) {
    val isSupported: Boolean
        get() = format != null
}

data class AudioClipRange(
    val startMs: Long,
    val endMs: Long,
) {
    val durationMs: Long
        get() = endMs - startMs

    val isValid: Boolean
        get() = startMs >= 0 && durationMs >= MIN_CLIP_DURATION_MS

    companion object {
        const val MIN_CLIP_DURATION_MS = 500L
    }
}

enum class AudioClipFailure {
    Unsupported,
    InvalidRange,
    NoAudio,
    Io,
    Muxer,
}

sealed interface AudioClipExportResult {
    data class Success(
        val durationMs: Long,
        val bytes: Long,
    ) : AudioClipExportResult

    data class Failure(
        val reason: AudioClipFailure,
        val detail: String? = null,
    ) : AudioClipExportResult
}
