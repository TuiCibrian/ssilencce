package com.ssilencce.data.local.replaygain

import android.util.Log
import com.ssilencce.data.local.ReplayGainTagExtractor
import com.ssilencce.data.local.repository.Repository
import com.ssilencce.data.model.Song
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

data class ReplayGainScanProgress(
    val completed: Int,
    val total: Int,
    val currentTitle: String?,
    val failed: Int,
)

data class ReplayGainScanFailure(
    val songId: Long,
    val title: String,
    val reason: String,
)

data class ReplayGainScanResult(
    val written: Int,
    val failed: Int,
    val cancelled: Boolean,
    val failures: List<ReplayGainScanFailure>,
)

/**
 * Groups selected songs by album, measures them, and writes tags. Album gain is only written when
 * the selected set contains every track of the album and every track measured successfully.
 *
 * Complete-album writes capture the previous album tags first. If any track write fails, every
 * track that already received album tags is restored to its previous album state before the rest
 * of the album continues with track tags only. A snapshot failure also withholds new album tags.
 */
class ReplayGainScanner(
    private val repository: Repository,
    private val decoder: ReplayGainDecoder,
    private val tagWriter: ReplayGainTagWriter,
) {
    suspend fun scan(
        songs: List<Song>,
        onProgress: (ReplayGainScanProgress) -> Unit,
    ): ReplayGainScanResult =
        withContext(Dispatchers.IO) {
            val selected = songs.filter { it != Song.emptySong }.distinctBy { it.id }
            if (selected.isEmpty()) {
                return@withContext ReplayGainScanResult(0, 0, false, emptyList())
            }

            val total = selected.size
            var completed = 0
            val writtenSongs = linkedSetOf<Long>()
            val failures = mutableListOf<ReplayGainScanFailure>()

            fun recordFailure(
                song: Song,
                throwable: Throwable,
            ) {
                if (failures.none { it.songId == song.id }) {
                    val failure = failure(song, throwable)
                    Log.w(TAG, "ReplayGain failed for ${song.title}: ${failure.reason}")
                    failures += failure
                }
            }

            fun addFailureReason(
                song: Song,
                suffix: String,
            ) {
                val index = failures.indexOfFirst { it.songId == song.id }
                if (index >= 0) {
                    failures[index] = failures[index].copy(reason = failures[index].reason + suffix)
                }
            }

            try {
                for (group in selected.groupBy { it.albumId }.values) {
                    val album =
                        if (group.first().albumId > 0) {
                            try {
                                repository.albumById(group.first().albumId)
                            } catch (cancellation: CancellationException) {
                                throw cancellation
                            } catch (_: Throwable) {
                                null
                            }
                        } else {
                            null
                        }
                    val completeAlbum =
                        album != null &&
                            album.songs.isNotEmpty() &&
                            album.songs.map { it.id }.toSet() == group.map { it.id }.toSet()

                    val measurements = mutableListOf<TrackMeasurement>()
                    for (song in group) {
                        ensureActive()
                        onProgress(ReplayGainScanProgress(completed, total, song.title, failures.size))
                        try {
                            measurements += measure(song)
                        } catch (cancellation: CancellationException) {
                            throw cancellation
                        } catch (throwable: Throwable) {
                            recordFailure(song, throwable)
                            completed++
                            onProgress(
                                ReplayGainScanProgress(completed, total, song.title, failures.size),
                            )
                        }
                    }

                    val albumValues =
                        if (completeAlbum && measurements.size == group.size) {
                            measureAlbum(measurements)
                        } else {
                            null
                        }
                    val albumSnapshots = captureAlbumSnapshots(measurements, albumValues)
                    val effectiveAlbumValues = if (albumSnapshots != null) albumValues else null
                    var suppressAlbumTags = effectiveAlbumValues == null
                    val albumTagged = mutableListOf<Pair<TrackMeasurement, Map<String, Array<String>>>>()

                    for (measurement in measurements) {
                        ensureActive()
                        val albumForWrite = if (suppressAlbumTags) null else effectiveAlbumValues
                        val result =
                            tagWriter.write(
                                song = measurement.song,
                                format = measurement.format,
                                trackGainDb = measurement.trackGainDb,
                                trackPeak = measurement.peak,
                                albumGainDb = albumForWrite?.gainDb,
                                albumPeak = albumForWrite?.peak,
                            )
                        if (result.success) {
                            writtenSongs += measurement.song.id
                            if (albumForWrite != null) {
                                albumSnapshots
                                    ?.get(measurement.song.id)
                                    ?.let { snapshot -> albumTagged += measurement to snapshot }
                            }
                        } else {
                            writtenSongs -= measurement.song.id
                            recordFailure(
                                measurement.song,
                                ReplayGainDecodeException(result.message ?: "Tag write failed"),
                            )
                            if (albumForWrite != null) {
                                suppressAlbumTags = true
                                withContext(NonCancellable) {
                                    for ((taggedMeasurement, snapshot) in albumTagged) {
                                        val rollback =
                                            tagWriter.restoreAlbumTags(taggedMeasurement.song, snapshot)
                                        if (!rollback.success) {
                                            writtenSongs -= taggedMeasurement.song.id
                                            recordFailure(
                                                taggedMeasurement.song,
                                                ReplayGainDecodeException(
                                                    "Album tag rollback failed: " +
                                                        (rollback.message ?: "unknown error"),
                                                ),
                                            )
                                        }
                                    }
                                    albumSnapshots
                                        ?.get(measurement.song.id)
                                        ?.let { snapshot ->
                                            val rollback =
                                                tagWriter.restoreAlbumTags(measurement.song, snapshot)
                                            if (!rollback.success) {
                                                addFailureReason(
                                                    measurement.song,
                                                    "; album rollback failed: " +
                                                        (rollback.message ?: "unknown error"),
                                                )
                                            }
                                        }
                                }
                                albumTagged.clear()
                            }
                        }
                        completed++
                        onProgress(
                            ReplayGainScanProgress(completed, total, measurement.song.title, failures.size),
                        )
                    }
                }
            } catch (cancellation: CancellationException) {
                return@withContext ReplayGainScanResult(
                    written = writtenSongs.size,
                    failed = failures.size,
                    cancelled = true,
                    failures = failures,
                )
            }

            ReplayGainScanResult(
                written = writtenSongs.size,
                failed = failures.size,
                cancelled = false,
                failures = failures,
            )
        }

    private suspend fun captureAlbumSnapshots(
        measurements: List<TrackMeasurement>,
        albumValues: AlbumValues?,
    ): Map<Long, Map<String, Array<String>>>? {
        if (albumValues == null) return null
        val snapshots = mutableMapOf<Long, Map<String, Array<String>>>()
        for (measurement in measurements) {
            val snapshot = tagWriter.snapshotAlbumProperties(measurement.song) ?: return null
            snapshots[measurement.song.id] = snapshot
        }
        return snapshots
    }

    private suspend fun measure(song: Song): TrackMeasurement {
        var analyzer: ReplayGainAnalyzer? = null
        val format =
            decoder.decode(
                song = song,
                onFormat = { audioFormat ->
                    analyzer = ReplayGainAnalyzer(audioFormat.sampleRate, audioFormat.channelCount)
                },
                onSamples = { samples, frameCount ->
                    analyzer?.accept(samples, frameCount)
                },
            )
        val analysis = analyzer?.finish() ?: throw ReplayGainDecodeException("No audio decoded")
        val lufs =
            ReplayGainMath.integratedLufs(analysis.blockEnergies)
                ?: throw ReplayGainDecodeException("No measurable audio (silent or too short)")
        return TrackMeasurement(
            song = song,
            format = format,
            trackGainDb = ReplayGainMath.trackGainDb(lufs, analysis.peak),
            peak = analysis.peak,
            analysis = analysis,
        )
    }

    private fun measureAlbum(measurements: List<TrackMeasurement>): AlbumValues? {
        val blockEnergies =
            measurements
                .flatMap { it.analysis.blockEnergies.asIterable() }
                .toDoubleArray()
        val lufs = ReplayGainMath.integratedLufs(blockEnergies) ?: return null
        val peak = measurements.maxOf { it.peak }
        return AlbumValues(
            gainDb = ReplayGainMath.albumGainDb(lufs, peak),
            peak = peak,
        )
    }

    private fun failure(
        song: Song,
        throwable: Throwable,
    ) = ReplayGainScanFailure(
        songId = song.id,
        title = song.title,
        reason = throwable.message ?: throwable.javaClass.simpleName,
    )

    private data class TrackMeasurement(
        val song: Song,
        val format: ReplayGainAudioFormat,
        val trackGainDb: Float,
        val peak: Float,
        val analysis: ReplayGainAnalysis,
    )

    private data class AlbumValues(
        val gainDb: Float,
        val peak: Float,
    )

    private companion object {
        const val TAG = "ReplayGainScan"
    }
}
