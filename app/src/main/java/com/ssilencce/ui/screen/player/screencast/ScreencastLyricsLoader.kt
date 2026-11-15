package com.ssilencce.ui.screen.player.screencast

import com.ssilencce.data.local.repository.LyricsRepository
import com.ssilencce.data.model.Song
import com.ssilencce.data.model.lyrics.LyricsSource
import com.ssilencce.data.model.lyrics.LyricsSourceChoice
import com.ssilencce.data.model.lyrics.LyricsSourceSelection
import com.ssilencce.data.model.lyrics.RawLyrics

/**
 * Loads the best local lyrics for the presentation. The lyrics screen can download from the
 * network; an external display must not trigger that implicitly, so only on-device sources are
 * used here.
 */
class ScreencastLyricsLoader(
    private val repository: LyricsRepository,
) {
    suspend fun load(song: Song): ScreencastLyrics {
        if (song.id == Song.emptySong.id) return ScreencastLyrics.None
        val selection =
            runCatching { repository.lyricsSourceSelection(song.id) }
                .getOrElse { LyricsSourceSelection.Automatic }

        if (selection.source == LyricsSourceChoice.File) {
            repository.userFileLyrics(song)?.let { raw ->
                parse(song, raw)?.let { return it }
            }
        }

        var plainLyrics: String? = null
        for (source in sourcesFor(selection.source)) {
            val raw = rawLyrics(song, source) ?: continue
            if (raw is RawLyrics.Stored && raw.instrumental) {
                return ScreencastLyrics.Instrumental
            }
            val parsed = repository.parseRawLyrics(song, raw)
            if (parsed?.hasContent == true) {
                return ScreencastLyrics.Synced(parsed)
            }
            if (plainLyrics.isNullOrBlank() && !raw.lyrics.isNullOrBlank()) {
                plainLyrics = raw.lyrics
            }
        }

        val lines =
            plainLyrics
                ?.lineSequence()
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                ?.toList()
                .orEmpty()
        return if (lines.isEmpty()) {
            ScreencastLyrics.None
        } else {
            ScreencastLyrics.Plain(lines)
        }
    }

    private suspend fun parse(
        song: Song,
        rawLyrics: RawLyrics,
    ): ScreencastLyrics? {
        if (rawLyrics is RawLyrics.Stored && rawLyrics.instrumental) {
            return ScreencastLyrics.Instrumental
        }
        val parsed = repository.parseRawLyrics(song, rawLyrics)
        if (parsed?.hasContent == true) {
            return ScreencastLyrics.Synced(parsed)
        }
        val plain =
            rawLyrics.lyrics
                ?.lineSequence()
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                ?.toList()
                .orEmpty()
        return if (plain.isEmpty()) null else ScreencastLyrics.Plain(plain)
    }

    private suspend fun rawLyrics(
        song: Song,
        source: LyricsSource,
    ): RawLyrics? =
        when (source) {
            LyricsSource.File -> repository.fileLyrics(song)
            LyricsSource.Embedded -> repository.embeddedLyrics(song)
            LyricsSource.Downloaded -> repository.storedLyrics(song, allowDownload = false)
        }

    private fun sourcesFor(source: LyricsSourceChoice): List<LyricsSource> =
        when (source) {
            LyricsSourceChoice.Automatic,
            LyricsSourceChoice.File,
            -> listOf(LyricsSource.File, LyricsSource.Embedded, LyricsSource.Downloaded)

            LyricsSourceChoice.Embedded -> listOf(LyricsSource.Embedded)

            LyricsSourceChoice.Downloaded -> listOf(LyricsSource.Downloaded)
        }
}
