package com.ssilencce.data.local.replaygain

import android.content.ContentResolver
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.kyant.taglib.TagLib
import com.ssilencce.data.model.Song
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

data class ReplayGainWriteResult(
    val success: Boolean,
    val message: String? = null,
)

/**
 * Writes the calculated values with TagLib and verifies them by reopening the file. The existing
 * ReplayGain aliases are removed first so repeated scans replace values instead of appending
 * duplicate frames. Containers whose property map cannot be persisted are reported as failures
 * instead of a false success.
 */
class ReplayGainTagWriter(
    private val contentResolver: ContentResolver,
    private val tempDir: File,
) {
    private val songLocks = ConcurrentHashMap<Long, Any>()

    suspend fun write(
        song: Song,
        format: ReplayGainAudioFormat,
        trackGainDb: Float,
        trackPeak: Float,
        albumGainDb: Float?,
        albumPeak: Float?,
    ): ReplayGainWriteResult =
        withContext(Dispatchers.IO) {
            val context = currentCoroutineContext()
            runCatching {
                val tagFormat = tagFormat(format, song)
                val values =
                    when (tagFormat) {
                        TagFormat.Standard, TagFormat.Wav -> {
                            ReplayGainTags.standardProperties(
                                trackGainDb = trackGainDb,
                                trackPeak = trackPeak,
                                albumGainDb = albumGainDb,
                                albumPeak = albumPeak,
                            )
                        }

                        TagFormat.Mp4 -> {
                            ReplayGainTags.mp4Properties(
                                trackGainDb = trackGainDb,
                                trackPeak = trackPeak,
                                albumGainDb = albumGainDb,
                                albumPeak = albumPeak,
                            )
                        }

                        TagFormat.Opus -> {
                            ReplayGainTags.opusProperties(
                                trackGainDb = trackGainDb,
                                trackPeak = trackPeak,
                                albumGainDb = albumGainDb,
                                albumPeak = albumPeak,
                            )
                        }
                    }
                if (tagFormat == TagFormat.Wav) {
                    val result =
                        writeWav(
                            song = song,
                            values = values,
                            replaceAlbum = albumGainDb != null,
                            checkpoint = { context.ensureActive() },
                        )
                    if (result.success) contentResolver.notifyChange(song.uri, null)
                    return@runCatching result
                }
                val descriptor =
                    contentResolver.openFileDescriptor(song.uri, "rw")
                        ?: return@withContext ReplayGainWriteResult(false, "File is not writable")
                val saved =
                    descriptor.use { fd ->
                        val current =
                            TagLib
                                .getMetadata(fd.dup().detachFd(), false)
                                ?.propertyMap
                                ?: hashMapOf()
                        val properties =
                            buildProperties(current, values, replaceAlbum = albumGainDb != null)
                        TagLib.savePropertyMap(fd.dup().detachFd(), HashMap(properties))
                    }
                if (!saved) {
                    return@withContext ReplayGainWriteResult(false, "TagLib save failed")
                }
                val readBack = readProperties(song.uri) ?: hashMapOf()
                if (!verify(values, readBack)) {
                    return@withContext ReplayGainWriteResult(
                        false,
                        persistenceFailureMessage(tagFormat),
                    )
                }
                contentResolver.notifyChange(song.uri, null)
                ReplayGainWriteResult(true)
            }.getOrElse { throwable ->
                if (throwable is CancellationException) throw throwable
                ReplayGainWriteResult(false, throwable.message ?: throwable.javaClass.simpleName)
            }
        }

    suspend fun snapshotAlbumProperties(song: Song): Map<String, Array<String>>? =
        withContext(Dispatchers.IO) {
            val context = currentCoroutineContext()
            runCatching {
                if (isWav(song)) {
                    val properties = readWavProperties(song.uri, { context.ensureActive() }) ?: return@runCatching null
                    return@runCatching albumProperties(properties)
                }
                val descriptor =
                    contentResolver.openFileDescriptor(song.uri, "r")
                        ?: return@runCatching null
                descriptor.use { fd ->
                    TagLib
                        .getMetadata(fd.dup().detachFd(), false)
                        ?.propertyMap
                        ?.let { albumProperties(it) }
                }
            }.getOrElse { throwable ->
                if (throwable is CancellationException) throw throwable
                null
            }
        }

    /**
     * Restores the album gain/peak aliases captured before a complete-album write. Track tags and
     * non-ReplayGain metadata are preserved; all other album aliases are removed.
     */
    suspend fun restoreAlbumTags(
        song: Song,
        previous: Map<String, Array<String>>,
    ): ReplayGainWriteResult =
        withContext(Dispatchers.IO) {
            val context = currentCoroutineContext()
            runCatching {
                if (isWav(song)) {
                    return@runCatching restoreWavAlbumTags(song, previous, { context.ensureActive() })
                }
                val descriptor =
                    contentResolver.openFileDescriptor(song.uri, "rw")
                        ?: return@withContext ReplayGainWriteResult(false, "File is not writable")
                val saved =
                    descriptor.use { fd ->
                        val current =
                            TagLib
                                .getMetadata(fd.dup().detachFd(), false)
                                ?.propertyMap
                                ?: hashMapOf()
                        val properties = buildAlbumRestoreProperties(current, previous)
                        TagLib.savePropertyMap(fd.dup().detachFd(), HashMap(properties))
                    }
                if (!saved) {
                    return@withContext ReplayGainWriteResult(false, "TagLib save failed")
                }
                val readBack = readProperties(song.uri) ?: hashMapOf()
                if (!verifyAlbumState(previous, readBack)) {
                    return@withContext ReplayGainWriteResult(
                        false,
                        "Album ReplayGain tags were not persisted",
                    )
                }
                contentResolver.notifyChange(song.uri, null)
                ReplayGainWriteResult(true)
            }.getOrElse { throwable ->
                if (throwable is CancellationException) throw throwable
                ReplayGainWriteResult(false, throwable.message ?: throwable.javaClass.simpleName)
            }
        }

    private fun albumProperties(tags: Map<String, Array<String>>): Map<String, Array<String>> =
        ReplayGainTags.canonicalProperties(tags).filterKeys {
            val suffix = ReplayGainTags.suffixOf(it)
            suffix == ReplayGainTags.ALBUM_GAIN || suffix == ReplayGainTags.ALBUM_PEAK
        }

    private fun buildAlbumRestoreProperties(
        current: Map<String, Array<String>>,
        previous: Map<String, Array<String>>,
    ): Map<String, Array<String>> {
        val preservedTracks =
            ReplayGainTags.canonicalProperties(current).filterKeys {
                val suffix = ReplayGainTags.suffixOf(it)
                suffix == ReplayGainTags.TRACK_GAIN || suffix == ReplayGainTags.TRACK_PEAK
            }
        val result =
            current
                .filterKeys { !ReplayGainTags.isReplayGainKey(it) }
                .mapValuesTo(HashMap()) { it.value }
        result.putAll(preservedTracks)
        previous.forEach { (key, value) -> result[key] = value }
        return result
    }

    private fun verifyAlbumState(
        expected: Map<String, Array<String>>,
        actual: Map<String, Array<String>>,
    ): Boolean =
        ALBUM_FIELDS.all { suffix ->
            val expectedEntries = expected.entries.filter { ReplayGainTags.suffixOf(it.key) == suffix }
            val actualEntries = actual.entries.filter { ReplayGainTags.suffixOf(it.key) == suffix }
            when {
                expectedEntries.isEmpty() -> {
                    actualEntries.isEmpty()
                }

                expectedEntries.size != 1 || actualEntries.size != 1 -> {
                    false
                }

                else -> {
                    val expectedEntry = expectedEntries.first()
                    val actualEntry = actualEntries.first()
                    if (ReplayGainTags.aliasPriority(expectedEntry.key) != ReplayGainTags.aliasPriority(actualEntry.key)) {
                        return@all false
                    }
                    if (actualEntry.value.size != 1) {
                        return@all false
                    }
                    val expectedValue = expectedEntry.value.firstOrNull() ?: return@all false
                    val actualValue = actualEntry.value.firstOrNull() ?: return@all false
                    valuesMatch(expectedEntry.key, expectedValue, actualValue)
                }
            }
        }

    private fun buildProperties(
        current: Map<String, Array<String>>,
        values: Map<String, String>,
        replaceAlbum: Boolean,
    ): Map<String, Array<String>> {
        val preserved = ReplayGainTags.canonicalProperties(current).toMutableMap()
        preserved.keys.removeAll {
            val suffix = ReplayGainTags.suffixOf(it)
            suffix == ReplayGainTags.TRACK_GAIN ||
                suffix == ReplayGainTags.TRACK_PEAK ||
                (replaceAlbum && (suffix == ReplayGainTags.ALBUM_GAIN || suffix == ReplayGainTags.ALBUM_PEAK))
        }

        val result =
            current
                .filterKeys { !ReplayGainTags.isReplayGainKey(it) }
                .mapValuesTo(HashMap()) { it.value }
        result.putAll(preserved)
        values.forEach { (key, value) -> result[key] = arrayOf(value) }
        return result
    }

    private fun verify(
        expected: Map<String, String>,
        actual: Map<String, Array<String>>,
    ): Boolean =
        expected.all { (key, expectedValue) ->
            val suffix = ReplayGainTags.suffixOf(key) ?: return@all false
            // Exactly one alias per field: TagLib may return the standard key for an MP4
            // freeform atom, so match by suffix instead of by the literal key.
            val aliases = actual.entries.filter { ReplayGainTags.suffixOf(it.key) == suffix }
            if (aliases.size != 1) {
                return@all false
            }
            val values = aliases.first().value
            if (values.size != 1) {
                return@all false
            }
            val actualValue = values.firstOrNull() ?: return@all false
            valuesMatch(key, expectedValue, actualValue)
        }

    private fun valuesMatch(
        key: String,
        expected: String,
        actual: String,
    ): Boolean {
        if (key.startsWith(ReplayGainTags.R128_TRACK_GAIN, ignoreCase = true) ||
            key.startsWith(ReplayGainTags.R128_ALBUM_GAIN, ignoreCase = true)
        ) {
            return actual.trim().toIntOrNull() == expected.trim().toIntOrNull()
        }
        val expectedNumber = parseNumber(expected) ?: return false
        val actualNumber = parseNumber(actual) ?: return false
        return abs(expectedNumber - actualNumber) <= NUMERIC_TOLERANCE
    }

    private fun parseNumber(raw: String): Float? =
        raw
            .replace("dB", "", ignoreCase = true)
            .replace("[^\\d+-.]".toRegex(), "")
            .toFloatOrNull()

    private fun isWav(song: Song): Boolean {
        if (extensionOf(song) in setOf("wav", "wave")) return true
        val mimeType = runCatching { contentResolver.getType(song.uri) }.getOrNull() ?: return false
        return mimeType.equals("audio/wav", ignoreCase = true) ||
            mimeType.equals("audio/x-wav", ignoreCase = true) ||
            mimeType.equals("audio/wave", ignoreCase = true) ||
            mimeType.equals("audio/vnd.wave", ignoreCase = true)
    }

    private fun writeWav(
        song: Song,
        values: Map<String, String>,
        replaceAlbum: Boolean,
        checkpoint: () -> Unit,
    ): ReplayGainWriteResult =
        synchronized(lockFor(song.id)) {
            val backup = File(tempDir, "replaygain-wav-${song.id}.bak")
            val recovered = recoverWavBackup(song, backup)
            if (recovered != null) return@synchronized recovered
            val descriptor =
                contentResolver.openFileDescriptor(song.uri, "rw")
                    ?: return@synchronized ReplayGainWriteResult(false, "File is not writable")
            val result =
                descriptor.use { fd ->
                    val readDescriptor = fd.dup()
                    val writeDescriptor = fd.dup()
                    val readStream = ParcelFileDescriptor.AutoCloseInputStream(readDescriptor)
                    val writeStream = ParcelFileDescriptor.AutoCloseOutputStream(writeDescriptor)
                    try {
                        WavId3v2TagCodec
                            .writeReplayGain(
                                read = readStream.channel,
                                write = writeStream.channel,
                                values = values,
                                removeTrack = true,
                                removeAlbum = replaceAlbum,
                                backupFile = backup,
                                backupTag = song.uri.toString(),
                                checkpoint = checkpoint,
                            ).also { writeStream.channel.force(true) }
                    } finally {
                        readStream.close()
                        writeStream.close()
                    }
                }
            if (!result.success) {
                return@synchronized ReplayGainWriteResult(
                    false,
                    result.message ?: "WAV ReplayGain write failed",
                )
            }
            val readBack =
                try {
                    readWavProperties(song.uri, checkpoint)
                } catch (cancellation: CancellationException) {
                    val cleanup = restoreAfterFailedVerification(song, backup)
                    cancellation.addSuppressed(IOException(cleanup))
                    throw cancellation
                } catch (throwable: Throwable) {
                    null
                } ?: hashMapOf()
            if (!verify(values, readBack)) {
                val restoreMessage = restoreAfterFailedVerification(song, backup)
                return@synchronized ReplayGainWriteResult(
                    false,
                    "WAV ReplayGain tags were not persisted$restoreMessage",
                )
            }
            WavId3v2TagCodec.markBackupCommitted(backup)
            WavId3v2TagCodec.deleteBackup(backup)
            ReplayGainWriteResult(true)
        }

    private fun restoreWavAlbumTags(
        song: Song,
        previous: Map<String, Array<String>>,
        checkpoint: () -> Unit,
    ): ReplayGainWriteResult =
        synchronized(lockFor(song.id)) {
            val values =
                previous
                    .mapNotNull { (key, value) ->
                        val first = value.firstOrNull() ?: return@mapNotNull null
                        key to first
                    }.toMap()
            val backup = File(tempDir, "replaygain-wav-${song.id}.bak")
            val recovered = recoverWavBackup(song, backup)
            if (recovered != null) return@synchronized recovered
            val descriptor =
                contentResolver.openFileDescriptor(song.uri, "rw")
                    ?: return@synchronized ReplayGainWriteResult(false, "File is not writable")
            val result =
                descriptor.use { fd ->
                    val readDescriptor = fd.dup()
                    val writeDescriptor = fd.dup()
                    val readStream = ParcelFileDescriptor.AutoCloseInputStream(readDescriptor)
                    val writeStream = ParcelFileDescriptor.AutoCloseOutputStream(writeDescriptor)
                    try {
                        WavId3v2TagCodec
                            .writeReplayGain(
                                read = readStream.channel,
                                write = writeStream.channel,
                                values = values,
                                removeTrack = false,
                                removeAlbum = true,
                                backupFile = backup,
                                backupTag = song.uri.toString(),
                                checkpoint = checkpoint,
                            ).also { writeStream.channel.force(true) }
                    } finally {
                        readStream.close()
                        writeStream.close()
                    }
                }
            if (!result.success) {
                return@synchronized ReplayGainWriteResult(
                    false,
                    result.message ?: "WAV album ReplayGain write failed",
                )
            }
            val readBack =
                try {
                    readWavProperties(song.uri, checkpoint)
                } catch (cancellation: CancellationException) {
                    val cleanup = restoreAfterFailedVerification(song, backup)
                    cancellation.addSuppressed(IOException(cleanup))
                    throw cancellation
                } catch (throwable: Throwable) {
                    null
                } ?: hashMapOf()
            if (!verifyAlbumState(previous, readBack)) {
                val restoreMessage = restoreAfterFailedVerification(song, backup)
                return@synchronized ReplayGainWriteResult(
                    false,
                    "Album ReplayGain tags were not persisted$restoreMessage",
                )
            }
            WavId3v2TagCodec.markBackupCommitted(backup)
            WavId3v2TagCodec.deleteBackup(backup)
            contentResolver.notifyChange(song.uri, null)
            ReplayGainWriteResult(true)
        }

    private fun recoverWavBackup(
        song: Song,
        backup: File,
    ): ReplayGainWriteResult? {
        val state = WavId3v2TagCodec.inspectBackup(backup, song.uri.toString())
        when (state.action) {
            WavId3v2TagCodec.BackupAction.NONE -> {
                return null
            }

            WavId3v2TagCodec.BackupAction.DISCARD -> {
                WavId3v2TagCodec.deleteBackup(backup)
                return null
            }

            WavId3v2TagCodec.BackupAction.REFUSE -> {
                return ReplayGainWriteResult(
                    false,
                    state.message ?: "Previous WAV write recovery failed",
                )
            }

            WavId3v2TagCodec.BackupAction.RESTORE -> {
                // Continue with the restore below.
            }
        }
        val descriptor =
            contentResolver.openFileDescriptor(song.uri, "rw")
                ?: return ReplayGainWriteResult(false, "File is not writable")
        val result =
            descriptor.use { fd ->
                val writeDescriptor = fd.dup()
                val writeStream = ParcelFileDescriptor.AutoCloseOutputStream(writeDescriptor)
                try {
                    WavId3v2TagCodec.restoreBackup(writeStream.channel, backup)
                } finally {
                    writeStream.close()
                }
            }
        if (!result.success) {
            return ReplayGainWriteResult(
                false,
                "Previous WAV write recovery failed: ${result.message}; backup retained at ${backup.absolutePath}",
            )
        }
        if (!WavId3v2TagCodec.deleteBackup(backup)) {
            return ReplayGainWriteResult(
                false,
                "Previous WAV write recovered but recovery files could not be removed: ${backup.absolutePath}",
            )
        }
        return null
    }

    private fun restoreAfterFailedVerification(
        song: Song,
        backup: File,
    ): String {
        val restored =
            try {
                contentResolver.openFileDescriptor(song.uri, "rw")?.use { fd ->
                    val writeDescriptor = fd.dup()
                    val writeStream = ParcelFileDescriptor.AutoCloseOutputStream(writeDescriptor)
                    try {
                        WavId3v2TagCodec.restoreBackup(writeStream.channel, backup)
                    } finally {
                        writeStream.close()
                    }
                }
            } catch (throwable: Throwable) {
                WavId3v2TagCodec.WriteResult(
                    false,
                    throwable.message ?: throwable.javaClass.simpleName,
                )
            }
        return if (restored?.success == true) {
            if (WavId3v2TagCodec.deleteBackup(backup)) {
                "; original bytes restored"
            } else {
                "; original bytes restored; recovery files retained at ${backup.absolutePath}"
            }
        } else {
            "; restore failed (${restored?.message ?: "file is not writable"}); " +
                "backup retained at ${backup.absolutePath}"
        }
    }

    private fun readWavProperties(
        uri: Uri,
        checkpoint: () -> Unit = {},
    ): Map<String, Array<String>>? =
        contentResolver.openFileDescriptor(uri, "r")?.use { fd ->
            val stream = ParcelFileDescriptor.AutoCloseInputStream(fd.dup())
            try {
                WavId3v2TagCodec.readReplayGain(stream.channel, checkpoint)
            } finally {
                stream.close()
            }
        }

    private fun readProperties(uri: Uri): Map<String, Array<String>>? =
        contentResolver.openFileDescriptor(uri, "r")?.use { fd ->
            TagLib
                .getMetadata(fd.dup().detachFd(), false)
                ?.propertyMap
        }

    private fun persistenceFailureMessage(tagFormat: TagFormat): String =
        when (tagFormat) {
            TagFormat.Standard -> "ReplayGain tags were not persisted"
            TagFormat.Wav -> "WAV ReplayGain tags were not persisted"
            TagFormat.Mp4 -> "M4A ReplayGain freeform tags were not persisted"
            TagFormat.Opus -> "Opus ReplayGain tags were not persisted"
        }

    private fun lockFor(songId: Long): Any = songLocks.computeIfAbsent(songId) { Any() }

    private fun extensionOf(song: Song): String = song.data.substringAfterLast('.', "").lowercase(Locale.ROOT)

    private fun tagFormat(
        format: ReplayGainAudioFormat,
        song: Song,
    ): TagFormat {
        val extension = extensionOf(song)
        return when {
            format.mimeType.equals("audio/opus", ignoreCase = true) || extension == "opus" -> {
                TagFormat.Opus
            }

            format.mimeType.equals("audio/mp4a-latm", ignoreCase = true) ||
                extension in setOf("m4a", "m4b", "m4p", "mp4") -> {
                TagFormat.Mp4
            }

            format.mimeType.equals("audio/wav", ignoreCase = true) ||
                format.mimeType.equals("audio/x-wav", ignoreCase = true) ||
                format.mimeType.equals("audio/wave", ignoreCase = true) ||
                format.mimeType.equals("audio/vnd.wave", ignoreCase = true) ||
                extension in setOf("wav", "wave") -> {
                TagFormat.Wav
            }

            else -> {
                TagFormat.Standard
            }
        }
    }

    private enum class TagFormat {
        Standard,
        Wav,
        Mp4,
        Opus,
    }

    private companion object {
        const val NUMERIC_TOLERANCE = 0.001f
        val ALBUM_FIELDS = listOf(ReplayGainTags.ALBUM_GAIN, ReplayGainTags.ALBUM_PEAK)
    }
}
