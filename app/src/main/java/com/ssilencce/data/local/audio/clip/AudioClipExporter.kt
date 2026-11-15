package com.ssilencce.data.local.audio.clip

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import com.ssilencce.extensions.hasR
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStream
import java.nio.ByteBuffer
import java.util.UUID

/**
 * Lossless audio range extraction.
 *
 * Container formats supported by [MediaMuxer] are copied sample by sample; MP3 is copied as
 * frame-aligned bytes. Nothing is decoded or re-encoded, so exported clips keep the source quality
 * and the work stays light on battery.
 */
class AudioClipExporter(
    private val context: Context,
) {
    suspend fun inspect(uri: Uri): AudioClipInspection =
        withContext(Dispatchers.IO) {
            val extractor = MediaExtractor()
            try {
                extractor.setDataSource(context, uri, null)
                val trackIndex =
                    findAudioTrack(extractor)
                        ?: return@withContext AudioClipInspection(
                            format = null,
                            reason = AudioClipUnsupportedReason.Format,
                            detail = "No audio track",
                        )
                val trackFormat = extractor.getTrackFormat(trackIndex)
                val mimeType = trackFormat.getString(MediaFormat.KEY_MIME)
                val durationMs =
                    if (trackFormat.containsKey(MediaFormat.KEY_DURATION)) {
                        (trackFormat.getLong(MediaFormat.KEY_DURATION) / 1000L).coerceAtLeast(0L)
                    } else {
                        0L
                    }
                val format = AudioClipFormat.fromMimeType(mimeType)
                when {
                    format == null -> {
                        AudioClipInspection(
                            format = null,
                            durationMs = durationMs,
                            reason = AudioClipUnsupportedReason.Format,
                            detail = mimeType,
                        )
                    }

                    format == AudioClipFormat.MP3 && !isMp3Stream(uri) -> {
                        AudioClipInspection(
                            format = null,
                            durationMs = durationMs,
                            reason = AudioClipUnsupportedReason.Format,
                            detail = "MP3 stream not found inside the container",
                        )
                    }

                    format == AudioClipFormat.OGG && !hasR() -> {
                        AudioClipInspection(
                            format = null,
                            durationMs = durationMs,
                            reason = AudioClipUnsupportedReason.RequiresNewerAndroid,
                            detail = mimeType,
                        )
                    }

                    else -> {
                        AudioClipInspection(format, durationMs = durationMs)
                    }
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                AudioClipInspection(
                    format = null,
                    reason = AudioClipUnsupportedReason.Unreadable,
                    detail = throwable.message,
                )
            } finally {
                extractor.release()
            }
        }

    suspend fun export(
        source: Uri,
        range: AudioClipRange,
        destination: OutputStream,
        format: AudioClipFormat,
    ): AudioClipExportResult =
        withContext(Dispatchers.IO) {
            if (!range.isValid) return@withContext invalidRange()
            val tempFile = createTempFile(format)
            try {
                val result = trim(source, range, tempFile, format)
                if (result !is AudioClipExportResult.Success) return@withContext result
                destination.use { output ->
                    tempFile.inputStream().use { input ->
                        input.copyTo(output, COPY_BUFFER_SIZE)
                    }
                }
                result.copy(bytes = tempFile.length())
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                AudioClipExportResult.Failure(AudioClipFailure.Io, throwable.message)
            } finally {
                tempFile.delete()
            }
        }

    suspend fun exportToFile(
        source: Uri,
        range: AudioClipRange,
        destination: File,
        format: AudioClipFormat,
    ): AudioClipExportResult =
        withContext(Dispatchers.IO) {
            if (!range.isValid) return@withContext invalidRange()
            try {
                val result = trim(source, range, destination, format)
                if (result !is AudioClipExportResult.Success) {
                    destination.delete()
                }
                result
            } catch (cancellation: CancellationException) {
                destination.delete()
                throw cancellation
            }
        }

    private suspend fun trim(
        source: Uri,
        range: AudioClipRange,
        outputFile: File,
        format: AudioClipFormat,
    ): AudioClipExportResult =
        when {
            format == AudioClipFormat.MP3 -> trimMp3(source, range, outputFile)
            format.muxerOutputFormat != null -> trimWithMuxer(source, range, outputFile, format)
            else -> AudioClipExportResult.Failure(AudioClipFailure.Unsupported, "Unsupported output format")
        }

    private suspend fun trimMp3(
        source: Uri,
        range: AudioClipRange,
        outputFile: File,
    ): AudioClipExportResult {
        val sourceStream =
            context.contentResolver.openInputStream(source)
                ?: return AudioClipExportResult.Failure(AudioClipFailure.Io, "Could not open the source file")
        val coroutineContext = currentCoroutineContext()
        return sourceStream.use { input ->
            outputFile.outputStream().use { output ->
                try {
                    val result =
                        Mp3RawTrimmer().trim(
                            source = input,
                            destination = output,
                            startUs = range.startMs * 1000,
                            endUs = range.endMs * 1000,
                            checkActive = { coroutineContext.ensureActive() },
                        )
                    AudioClipExportResult.Success(result.durationMs, result.bytes)
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (throwable: Throwable) {
                    AudioClipExportResult.Failure(AudioClipFailure.Io, throwable.message)
                }
            }
        }
    }

    private suspend fun trimWithMuxer(
        source: Uri,
        range: AudioClipRange,
        outputFile: File,
        format: AudioClipFormat,
    ): AudioClipExportResult {
        val muxerFormat =
            format.muxerOutputFormat
                ?: return AudioClipExportResult.Failure(AudioClipFailure.Unsupported, "No muxer for $format")
        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null
        var muxerStarted = false
        try {
            extractor.setDataSource(context, source, null)
            val trackIndex =
                findAudioTrack(extractor)
                    ?: return AudioClipExportResult.Failure(AudioClipFailure.Unsupported, "No audio track")
            val trackFormat = extractor.getTrackFormat(trackIndex)
            val mimeType = trackFormat.getString(MediaFormat.KEY_MIME)
            if (AudioClipFormat.fromMimeType(mimeType) != format) {
                return AudioClipExportResult.Failure(AudioClipFailure.Unsupported, mimeType)
            }
            extractor.selectTrack(trackIndex)
            extractor.seekTo(range.startMs * 1000, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

            muxer = MediaMuxer(outputFile.absolutePath, muxerFormat)
            val outputTrack = muxer.addTrack(trackFormat)
            muxer.start()
            muxerStarted = true

            val bufferSize =
                if (trackFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                    maxOf(trackFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE), MIN_MUXER_BUFFER_SIZE)
                } else {
                    MIN_MUXER_BUFFER_SIZE
                }
            val buffer = ByteBuffer.allocateDirect(bufferSize)
            val bufferInfo = MediaCodec.BufferInfo()
            val startUs = range.startMs * 1000
            val endUs = range.endMs * 1000
            var baselineUs = -1L
            var lastPresentationUs = -1L
            var writtenSamples = 0
            var writtenBytes = 0L

            while (true) {
                currentCoroutineContext().ensureActive()
                val sampleTime = extractor.sampleTime
                if (sampleTime < 0 || sampleTime >= endUs) break
                buffer.clear()
                val sampleSize = extractor.readSampleData(buffer, 0)
                if (sampleSize < 0) break
                if (sampleTime >= startUs) {
                    if (baselineUs < 0) baselineUs = sampleTime
                    var presentationUs = (sampleTime - baselineUs).coerceAtLeast(0L)
                    if (presentationUs <= lastPresentationUs) {
                        presentationUs = lastPresentationUs + 1
                    }
                    val sampleFlags =
                        if (extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0) {
                            MediaCodec.BUFFER_FLAG_KEY_FRAME
                        } else {
                            0
                        }
                    bufferInfo.set(0, sampleSize, presentationUs, sampleFlags)
                    muxer.writeSampleData(outputTrack, buffer, bufferInfo)
                    lastPresentationUs = presentationUs
                    writtenSamples++
                    writtenBytes += sampleSize
                }
                extractor.advance()
            }

            if (writtenSamples == 0) {
                return AudioClipExportResult.Failure(AudioClipFailure.NoAudio, "No samples in the selected range")
            }
            muxer.stop()
            muxerStarted = false
            val durationMs = (lastPresentationUs.coerceAtLeast(0L) / 1000L)
            return AudioClipExportResult.Success(durationMs, writtenBytes)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            return AudioClipExportResult.Failure(AudioClipFailure.Muxer, throwable.message)
        } finally {
            if (muxerStarted) {
                runCatching { muxer?.stop() }
            }
            runCatching { muxer?.release() }
            extractor.release()
            if (outputFile.exists() && outputFile.length() == 0L) {
                outputFile.delete()
            }
        }
    }

    private fun findAudioTrack(extractor: MediaExtractor): Int? =
        (0 until extractor.trackCount).firstOrNull { index ->
            extractor
                .getTrackFormat(index)
                .getString(MediaFormat.KEY_MIME)
                ?.startsWith("audio/") == true
        }

    private fun isMp3Stream(uri: Uri): Boolean =
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val header = ByteArray(3)
                val read = input.read(header)
                (read >= 3 && header[0] == 'I'.code.toByte() && header[1] == 'D'.code.toByte() && header[2] == '3'.code.toByte()) ||
                    (read >= 2 && (header[0].toInt() and 0xFF) == 0xFF && (header[1].toInt() and 0xE0) == 0xE0)
            } ?: false
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) throw throwable
            false
        }

    private fun createTempFile(format: AudioClipFormat): File {
        val directory = File(context.cacheDir, TEMP_DIRECTORY)
        directory.mkdirs()
        return File(directory, "clip-${UUID.randomUUID()}.${format.extension}")
    }

    private fun invalidRange(): AudioClipExportResult =
        AudioClipExportResult.Failure(
            AudioClipFailure.InvalidRange,
            "Select at least ${AudioClipRange.MIN_CLIP_DURATION_MS} ms",
        )

    private companion object {
        const val MIN_MUXER_BUFFER_SIZE = 256 * 1024
        const val COPY_BUFFER_SIZE = 64 * 1024
        const val TEMP_DIRECTORY = "audio-clips"
    }
}
