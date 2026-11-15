package com.ssilencce.ui.screen.player.screencast

import android.net.Uri
import androidx.compose.runtime.Immutable
import com.ssilencce.data.model.lyrics.SyncedLyrics

/**
 * An external display that can host the Now Playing presentation.
 */
@Immutable
data class ScreencastDisplayInfo(
    val id: Int,
    val name: String,
    val widthPx: Int = 0,
    val heightPx: Int = 0,
)

/**
 * Lyrics for the external display. The picker does not download lyrics; it reuses whatever the
 * user has on disk or has already stored for the song.
 */
sealed interface ScreencastLyrics {
    data object None : ScreencastLyrics

    data object Instrumental : ScreencastLyrics

    data class Plain(
        val lines: List<String>,
    ) : ScreencastLyrics

    data class Synced(
        val lyrics: SyncedLyrics,
    ) : ScreencastLyrics
}

/**
 * Snapshot rendered by the presentation. Only the progress fields change on the ticker; lyrics
 * are replaced when the current media item changes.
 */
@Immutable
data class ScreencastNowPlayingState(
    val hasMedia: Boolean = false,
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val artworkUri: Uri? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val lyrics: ScreencastLyrics = ScreencastLyrics.None,
) {
    val progress: Float
        get() =
            if (durationMs <= 0L) {
                0f
            } else {
                (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
            }
}
