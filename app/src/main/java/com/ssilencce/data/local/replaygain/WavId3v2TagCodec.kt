package com.ssilencce.data.local.replaygain

import java.io.ByteArrayOutputStream
import java.io.EOFException
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.nio.charset.Charset
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Locale
import java.util.concurrent.CancellationException

/**
 * Minimal RIFF WAV ID3v2 codec for ReplayGain tags.
 *
 * TagLib's WAV `savePropertyMap` returns true in the app process without persisting the ID3v2
 * chunk. This codec edits the RIFF `ID3 ` chunk directly, preserving the `fmt `/`data` chunks and
 * every non-ReplayGain ID3 frame. The caller reads the result back through a fresh descriptor.
 *
 * A WAV write is an in-place mutation, not an atomic URI replacement. The optional backup is the
 * recovery mechanism: it is committed atomically and marked pending before the first byte changes.
 * The caller owns the backup through its own read-back verification and then marks it committed.
 */
internal object WavId3v2TagCodec {
    private const val RIFF_HEADER_SIZE = 12L
    private const val CHUNK_HEADER_SIZE = 8L
    private const val ID3_CHUNK_ID = "ID3 "
    private const val COPY_BUFFER_SIZE = 1 shl 20
    private const val MAX_TAG_SIZE = 32 * 1024 * 1024
    private const val MAX_CHUNKS = 10_000
    private const val MAX_RIFF_SIZE = 0xFFFFFFFFL
    private const val BACKUP_MARKER_SUFFIX = ".pending"
    private const val BACKUP_PENDING = "pending"
    private const val BACKUP_COMMITTED = "committed"

    private val TRACK_FIELDS =
        setOf(
            ReplayGainTags.TRACK_GAIN,
            ReplayGainTags.TRACK_PEAK,
        )
    private val ALBUM_FIELDS =
        setOf(
            ReplayGainTags.ALBUM_GAIN,
            ReplayGainTags.ALBUM_PEAK,
        )

    data class Chunk(
        val id: String,
        val offset: Long,
        val size: Long,
    ) {
        val totalSize: Long
            get() = CHUNK_HEADER_SIZE + size + (size and 1L)
    }

    data class WriteResult(
        val success: Boolean,
        val message: String? = null,
        val backupRetained: Boolean = false,
    )

    enum class BackupAction {
        NONE,
        RESTORE,
        DISCARD,
        REFUSE,
    }

    data class BackupState(
        val action: BackupAction,
        val expectedSize: Long? = null,
        val message: String? = null,
    )

    fun interface WriteObserver {
        fun beforeCommit()
    }

    fun readReplayGain(
        read: FileChannel,
        checkpoint: () -> Unit = {},
    ): Map<String, Array<String>> {
        val chunks = readChunks(read, checkpoint)
        val id3Chunks = chunks.filter { it.id.equals(ID3_CHUNK_ID, ignoreCase = true) }
        if (id3Chunks.size > 1) {
            throw IllegalArgumentException("Multiple WAV ID3 chunks are not supported")
        }
        val id3 = id3Chunks.firstOrNull() ?: return emptyMap()
        if (id3.size > MAX_TAG_SIZE) {
            throw IllegalArgumentException("WAV ID3 chunk is too large")
        }
        val tag = readBytes(read, id3.offset + CHUNK_HEADER_SIZE, id3.size)
        val frames = parseTag(tag, checkpoint).frames
        val result = linkedMapOf<String, MutableList<String>>()
        for (frame in frames) {
            if (frame.id != "TXXX") continue
            val (description, value) = parseTxxx(frame.payload) ?: continue
            val normalized = description.trim()
            if (ReplayGainTags.suffixOf(normalized) == null) continue
            result.getOrPut(normalized) { mutableListOf() }.add(value)
        }
        return result.mapValues { it.value.toTypedArray() }
    }

    fun writeReplayGain(
        read: FileChannel,
        write: FileChannel,
        values: Map<String, String>,
        removeTrack: Boolean,
        removeAlbum: Boolean,
        backupFile: File? = null,
        backupTag: String = "",
        checkpoint: () -> Unit = {},
        observer: WriteObserver = WriteObserver { },
    ): WriteResult {
        val marker = backupFile?.let { backupMarker(it) }
        var backupCreated = false
        return try {
            checkpoint()
            val fileSize = read.size()
            val chunks = readChunks(read, checkpoint)
            val id3Chunks = chunks.filter { it.id.equals(ID3_CHUNK_ID, ignoreCase = true) }
            if (id3Chunks.size > 1) {
                throw IllegalArgumentException("Multiple WAV ID3 chunks are not supported")
            }
            val oldChunk = id3Chunks.firstOrNull()
            if (oldChunk != null && oldChunk.size > MAX_TAG_SIZE) {
                throw IllegalArgumentException("WAV ID3 chunk is too large")
            }
            val oldTag = oldChunk?.let { readBytes(read, it.offset + CHUNK_HEADER_SIZE, it.size) }
            val oldParsed = oldTag?.let { parseTag(it, checkpoint) }
            val version = oldParsed?.version ?: 4
            val retained =
                oldParsed
                    ?.frames
                    ?.filterNot { frame ->
                        if (frame.id != "TXXX") {
                            false
                        } else {
                            val description = parseTxxx(frame.payload)?.first?.trim()
                            val suffix = description?.let { ReplayGainTags.suffixOf(it) }
                            suffix != null &&
                                (
                                    (removeTrack && suffix in TRACK_FIELDS) ||
                                        (removeAlbum && suffix in ALBUM_FIELDS)
                                )
                        }
                    }
                    ?: emptyList()
            val newTag =
                buildTag(
                    version = version,
                    frames = retained + values.map { (field, value) -> txxxFrame(field, value, version) },
                    checkpoint = checkpoint,
                )
            val newChunkTotal = CHUNK_HEADER_SIZE + newTag.size + (newTag.size and 1)
            val oldChunkTotal = oldChunk?.totalSize ?: 0L
            val newSize = fileSize - oldChunkTotal + newChunkTotal
            if (newSize > MAX_RIFF_SIZE) {
                throw IllegalArgumentException("WAV file is too large to rewrite")
            }
            if (backupFile != null) {
                writeVerifiedBackup(read, backupFile, checkpoint)
                backupCreated = true
                writePendingMarker(marker, fileSize, backupTag)
            }
            checkpoint()
            if (oldChunk == null) {
                writeChunk(write, fileSize, newTag)
            } else {
                val delta = newChunkTotal - oldChunkTotal
                val shiftStart = oldChunk.offset + oldChunkTotal
                if (shiftStart < fileSize) {
                    shift(write, read, shiftStart, fileSize, delta, checkpoint)
                }
                writeChunk(write, oldChunk.offset, newTag)
            }
            observer.beforeCommit()
            writeRiffSize(write, newSize)
            if (newSize < fileSize) {
                write.truncate(newSize)
            }
            write.force(true)
            WriteResult(true)
        } catch (throwable: Throwable) {
            val restoreError =
                if (backupCreated) {
                    restoreFromFile(write, backupFile)
                } else {
                    null
                }
            val restored = backupCreated && restoreError == null
            val cleanupComplete =
                if (restored) {
                    backupFile?.let { deleteBackup(it) } ?: true
                } else {
                    false
                }
            if (throwable is CancellationException) {
                if (restoreError != null) {
                    throwable.addSuppressed(IOException(restoreError))
                }
                throw throwable
            }
            val message =
                buildString {
                    append(throwable.message ?: throwable.javaClass.simpleName)
                    if (restoreError != null) {
                        append("; ")
                        append(restoreError)
                    }
                    if (backupCreated && !restored) {
                        append("; original backup retained at ")
                        append(backupFile?.absolutePath)
                    } else if (restored && !cleanupComplete) {
                        append("; recovery files retained at ")
                        append(backupFile?.absolutePath)
                    }
                }
            WriteResult(false, message, backupRetained = backupCreated && !restored)
        }
    }

    fun restoreBackup(
        write: FileChannel,
        backupFile: File?,
    ): WriteResult {
        val error = restoreFromFile(write, backupFile)
        return if (error == null) WriteResult(true) else WriteResult(false, error)
    }

    /**
     * Marks the backup non-restorable. The caller must call this only after its own read-back
     * verification succeeded. Recovery discards a committed backup instead of restoring it.
     */
    fun markBackupCommitted(backupFile: File) {
        val marker = backupMarker(backupFile)
        ensureParentDirectory(marker)
        FileOutputStream(marker).use { output ->
            output.write(BACKUP_COMMITTED.toByteArray(Charsets.UTF_8))
            output.fd.sync()
        }
    }

    /**
     * Removes the marker first, then the backup. If the marker cannot be removed the backup is
     * retained so recovery can still restore it; a marker-free backup is stale and never restored.
     */
    fun deleteBackup(backupFile: File): Boolean {
        val marker = backupMarker(backupFile)
        if (marker.exists() && !marker.delete()) {
            return false
        }
        if (backupFile.exists()) {
            backupFile.delete()
        }
        return !backupFile.exists()
    }

    internal fun backupMarker(backupFile: File): File = File(backupFile.parentFile, backupFile.name + BACKUP_MARKER_SUFFIX)

    /**
     * Classifies a leftover backup without touching the audio file. The caller performs the
     * restore or discard action. This keeps the crash-recovery state machine testable without an
     * Android ContentResolver.
     */
    fun inspectBackup(
        backupFile: File,
        backupTag: String,
    ): BackupState {
        val marker = backupMarker(backupFile)
        val backupExists = backupFile.isFile
        val markerExists = marker.isFile
        if (!backupExists && !markerExists) {
            return BackupState(BackupAction.NONE)
        }
        if (backupExists && !markerExists) {
            return BackupState(BackupAction.DISCARD)
        }
        if (!backupExists) {
            return BackupState(
                BackupAction.REFUSE,
                message = "Previous WAV write recovery is incomplete: backup is missing; refusing to write",
            )
        }
        val markerContent = runCatching { marker.readText(Charsets.UTF_8) }.getOrDefault("")
        if (markerContent.startsWith(BACKUP_COMMITTED)) {
            return BackupState(BackupAction.DISCARD)
        }
        val tag = Regex("(?:^|\\s)tag=(\\S+)").find(markerContent)?.groupValues?.get(1)
        if (tag != null && tag != backupTag) {
            return BackupState(BackupAction.DISCARD)
        }
        val expectedSize =
            Regex("size=(\\d+)")
                .find(markerContent)
                ?.groupValues
                ?.get(1)
                ?.toLongOrNull()
        if (expectedSize != null && backupFile.length() != expectedSize) {
            return BackupState(
                BackupAction.REFUSE,
                expectedSize,
                "Previous WAV write recovery failed: backup size mismatch; backup retained at ${backupFile.absolutePath}",
            )
        }
        return BackupState(BackupAction.RESTORE, expectedSize)
    }

    private fun writeVerifiedBackup(
        read: FileChannel,
        backupFile: File,
        checkpoint: () -> Unit,
    ) {
        ensureParentDirectory(backupFile)
        val parent = backupFile.parentFile
        val temp =
            if (parent != null) {
                File.createTempFile(backupFile.name + ".tmp-", null, parent)
            } else {
                File.createTempFile(backupFile.name + ".tmp-", null)
            }
        try {
            FileOutputStream(temp).use { output ->
                val channel = output.channel
                val size = read.size()
                var position = 0L
                while (position < size) {
                    checkpoint()
                    val length = minOf(COPY_BUFFER_SIZE.toLong(), size - position).toInt()
                    val bytes = readBytes(read, position, length.toLong())
                    writeBytes(channel, position, bytes)
                    position += length
                }
                channel.force(true)
            }
            if (temp.length() != read.size()) {
                throw IOException("WAV backup size mismatch")
            }
            moveAtomically(temp, backupFile)
        } finally {
            if (temp.exists()) {
                temp.delete()
            }
        }
    }

    private fun moveAtomically(
        source: File,
        target: File,
    ) {
        try {
            Files.move(
                source.toPath(),
                target.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING,
            )
        } catch (exception: AtomicMoveNotSupportedException) {
            Files.move(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private fun writePendingMarker(
        marker: File?,
        originalSize: Long,
        backupTag: String,
    ) {
        if (marker == null) return
        ensureParentDirectory(marker)
        FileOutputStream(marker).use { output ->
            output.write("$BACKUP_PENDING size=$originalSize tag=$backupTag".toByteArray(Charsets.UTF_8))
            output.fd.sync()
        }
    }

    private fun ensureParentDirectory(file: File) {
        val parent = file.parentFile ?: return
        if (!parent.isDirectory && !parent.mkdirs()) {
            throw IOException("Unable to create WAV backup directory")
        }
    }

    private fun restoreFromFile(
        write: FileChannel,
        file: File?,
    ): String? {
        if (file == null || !file.isFile) return "WAV backup is missing"
        return try {
            val size = file.length()
            if (size < RIFF_HEADER_SIZE) return "WAV backup is too small"
            FileInputStream(file).use { input ->
                val channel = input.channel
                val header = readBytes(channel, 0, RIFF_HEADER_SIZE)
                if (header.copyOfRange(0, 4).toString(Charsets.US_ASCII) != "RIFF" ||
                    header.copyOfRange(8, 12).toString(Charsets.US_ASCII) != "WAVE"
                ) {
                    return "WAV backup is not a RIFF WAVE file"
                }
                write.truncate(0)
                var position = 0L
                while (position < size) {
                    val length = minOf(COPY_BUFFER_SIZE.toLong(), size - position).toInt()
                    val bytes = readBytes(channel, position, length.toLong())
                    writeBytes(write, position, bytes)
                    position += length
                }
                write.force(true)
                if (write.size() != size) {
                    return "WAV backup restore size mismatch"
                }
            }
            null
        } catch (throwable: Throwable) {
            "WAV backup restore failed: ${throwable.message ?: throwable.javaClass.simpleName}"
        }
    }

    private fun readChunks(
        read: FileChannel,
        checkpoint: () -> Unit,
    ): List<Chunk> {
        val fileSize = read.size()
        if (fileSize < RIFF_HEADER_SIZE) {
            throw IllegalArgumentException("Not a RIFF WAV file")
        }
        val header = readBytes(read, 0, RIFF_HEADER_SIZE)
        if (header.copyOfRange(0, 4).toString(Charsets.US_ASCII) != "RIFF" ||
            header.copyOfRange(8, 12).toString(Charsets.US_ASCII) != "WAVE"
        ) {
            throw IllegalArgumentException("Not a RIFF WAVE file")
        }
        val chunks = mutableListOf<Chunk>()
        var offset = RIFF_HEADER_SIZE
        while (offset + CHUNK_HEADER_SIZE <= fileSize) {
            if (chunks.size >= MAX_CHUNKS) {
                throw IllegalArgumentException("Too many RIFF chunks")
            }
            checkpoint()
            val headerBytes = readBytes(read, offset, CHUNK_HEADER_SIZE)
            val id = headerBytes.copyOfRange(0, 4).toString(Charsets.US_ASCII)
            val size = uint32(headerBytes, 4)
            val payloadEnd = offset + CHUNK_HEADER_SIZE + size
            if (payloadEnd > fileSize) {
                throw IllegalArgumentException("Malformed RIFF chunk at $offset")
            }
            val paddedEnd = payloadEnd + (size and 1L)
            if (paddedEnd > fileSize) {
                throw IllegalArgumentException("Malformed RIFF chunk padding at $offset")
            }
            chunks += Chunk(id, offset, size)
            offset = paddedEnd
        }
        if (offset != fileSize) {
            throw IllegalArgumentException("Trailing bytes after RIFF chunks")
        }
        return chunks
    }

    private fun shift(
        write: FileChannel,
        read: FileChannel,
        start: Long,
        end: Long,
        delta: Long,
        checkpoint: () -> Unit,
    ) {
        if (delta == 0L) return
        if (delta > 0) {
            var position = end
            while (position > start) {
                checkpoint()
                val blockStart = maxOf(start, position - COPY_BUFFER_SIZE)
                val length = (position - blockStart).toInt()
                val bytes = readBytes(read, blockStart, length.toLong())
                writeBytes(write, blockStart + delta, bytes)
                position = blockStart
            }
        } else {
            var position = start
            while (position < end) {
                checkpoint()
                val length = minOf(COPY_BUFFER_SIZE.toLong(), end - position).toInt()
                val bytes = readBytes(read, position, length.toLong())
                writeBytes(write, position + delta, bytes)
                position += length
            }
        }
    }

    private fun readBytes(
        read: FileChannel,
        position: Long,
        length: Long,
    ): ByteArray {
        if (length < 0 || length > Int.MAX_VALUE) {
            throw IllegalArgumentException("WAV chunk is too large")
        }
        val result = ByteArray(length.toInt())
        val buffer = ByteBuffer.wrap(result)
        var offset = position
        while (buffer.hasRemaining()) {
            val count = read.read(buffer, offset)
            if (count < 0) throw EOFException("Unexpected end of WAV file")
            if (count == 0) continue
            offset += count
        }
        return result
    }

    private fun writeBytes(
        write: FileChannel,
        position: Long,
        bytes: ByteArray,
    ) {
        val buffer = ByteBuffer.wrap(bytes)
        var offset = position
        while (buffer.hasRemaining()) {
            val count = write.write(buffer, offset)
            if (count <= 0) throw IllegalStateException("Unable to write WAV file")
            offset += count
        }
    }

    private fun writeChunk(
        write: FileChannel,
        position: Long,
        tag: ByteArray,
    ) {
        val header = ByteBuffer.allocate(CHUNK_HEADER_SIZE.toInt()).order(ByteOrder.LITTLE_ENDIAN)
        header.put(ID3_CHUNK_ID.toByteArray(Charsets.US_ASCII))
        header.putInt(tag.size)
        writeBytes(write, position, header.array())
        writeBytes(write, position + CHUNK_HEADER_SIZE, tag)
        if (tag.size and 1 == 1) {
            writeBytes(write, position + CHUNK_HEADER_SIZE + tag.size, byteArrayOf(0))
        }
    }

    private fun writeRiffSize(
        write: FileChannel,
        newSize: Long,
    ) {
        val buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
        buffer.putInt((newSize - 8).toInt())
        writeBytes(write, 4, buffer.array())
    }

    private data class Frame(
        val id: String,
        val flags: Int,
        val payload: ByteArray,
    )

    private data class Tag(
        val version: Int,
        val frames: List<Frame>,
    )

    private fun parseTag(
        tag: ByteArray,
        checkpoint: () -> Unit,
    ): Tag {
        if (tag.size < 10) throw IllegalArgumentException("ID3 tag too short")
        if (tag[0] != 'I'.code.toByte() || tag[1] != 'D'.code.toByte() || tag[2] != '3'.code.toByte()) {
            throw IllegalArgumentException("Missing ID3 tag")
        }
        val version = tag[3].toInt() and 0xFF
        if (version != 3 && version != 4) {
            throw IllegalArgumentException("Unsupported ID3 version $version")
        }
        val tagFlags = tag[5].toInt() and 0xFF
        if (tagFlags != 0) {
            throw IllegalArgumentException("Unsupported ID3 tag flags 0x%02X".format(tagFlags))
        }
        val declaredSize = 10L + syncsafe(tag, 6)
        if (declaredSize > tag.size.toLong()) {
            throw IllegalArgumentException("ID3 tag size exceeds chunk")
        }
        if (declaredSize < tag.size.toLong()) {
            for (index in declaredSize.toInt() until tag.size) {
                if (tag[index].toInt() != 0) {
                    throw IllegalArgumentException("ID3 tag has undeclared frames")
                }
            }
        }
        val end = declaredSize.toInt()
        val frames = mutableListOf<Frame>()
        var offset = 10
        while (offset + 10 <= end) {
            if (frames.size % 64 == 0) checkpoint()
            if (tag[offset].toInt() == 0 &&
                tag[offset + 1].toInt() == 0 &&
                tag[offset + 2].toInt() == 0 &&
                tag[offset + 3].toInt() == 0
            ) {
                break
            }
            val id = tag.copyOfRange(offset, offset + 4).toString(Charsets.US_ASCII)
            val frameSize = if (version == 4) syncsafe(tag, offset + 4) else bigEndian(tag, offset + 4)
            val frameFlags = ((tag[offset + 8].toInt() and 0xFF) shl 8) or (tag[offset + 9].toInt() and 0xFF)
            val frameEnd = offset.toLong() + 10L + frameSize
            if (frameSize < 0 || frameEnd > end.toLong()) {
                throw IllegalArgumentException("Malformed ID3 frame $id")
            }
            val unsupportedFlags = unsupportedFrameFlags(version, frameFlags)
            if (unsupportedFlags != null) {
                throw IllegalArgumentException("Unsupported ID3v2.$version frame $id $unsupportedFlags")
            }
            frames += Frame(id, frameFlags, tag.copyOfRange(offset + 10, frameEnd.toInt()))
            offset = frameEnd.toInt()
        }
        return Tag(version, frames)
    }

    private fun unsupportedFrameFlags(
        version: Int,
        flags: Int,
    ): String? =
        if (version == 3) {
            when {
                flags and 0x0080 != 0 -> "compression"
                flags and 0x0040 != 0 -> "encryption"
                flags and 0x0020 != 0 -> "grouping"
                else -> null
            }
        } else {
            when {
                flags and 0x0040 != 0 -> "grouping"
                flags and 0x0008 != 0 -> "compression"
                flags and 0x0004 != 0 -> "encryption"
                flags and 0x0002 != 0 -> "unsynchronisation"
                flags and 0x0001 != 0 -> "data-length indicator"
                else -> null
            }
        }

    private fun buildTag(
        version: Int,
        frames: List<Frame>,
        checkpoint: () -> Unit,
    ): ByteArray {
        val body = ByteArrayOutputStream()
        for (frame in frames) {
            checkpoint()
            if (frame.id.length != 4) continue
            body.write(frame.id.toByteArray(Charsets.US_ASCII))
            val size = if (version == 4) syncsafeBytes(frame.payload.size) else intBytes(frame.payload.size)
            body.write(size)
            body.write((frame.flags shr 8) and 0xFF)
            body.write(frame.flags and 0xFF)
            body.write(frame.payload)
            if (body.size() > MAX_TAG_SIZE) {
                throw IllegalArgumentException("ID3 tag is too large")
            }
        }
        val bodyBytes = body.toByteArray()
        if (bodyBytes.size > MAX_TAG_SIZE) {
            throw IllegalArgumentException("ID3 tag is too large")
        }
        val header = ByteArray(10)
        header[0] = 'I'.code.toByte()
        header[1] = 'D'.code.toByte()
        header[2] = '3'.code.toByte()
        header[3] = version.toByte()
        header[4] = 0
        header[5] = 0
        writeSyncsafe(header, 6, bodyBytes.size)
        return header + bodyBytes
    }

    private fun txxxFrame(
        field: String,
        value: String,
        version: Int,
    ): Frame {
        val encoding = if (version == 4) 3 else 0
        val charset: Charset = if (encoding == 3) Charsets.UTF_8 else Charsets.ISO_8859_1
        val payload = ByteArrayOutputStream()
        payload.write(encoding)
        payload.write(field.toByteArray(charset))
        payload.write(0)
        payload.write(value.toByteArray(charset))
        return Frame("TXXX", 0, payload.toByteArray())
    }

    private fun parseTxxx(payload: ByteArray): Pair<String, String>? {
        if (payload.isEmpty()) return null
        val encoding = payload[0].toInt() and 0xFF
        val charset: Charset =
            when (encoding) {
                0 -> Charsets.ISO_8859_1
                1 -> Charsets.UTF_16
                2 -> Charsets.UTF_16BE
                3 -> Charsets.UTF_8
                else -> return null
            }
        val terminator = if (encoding == 1 || encoding == 2) 2 else 1
        val split = findTerminator(payload, 1, terminator) ?: return null
        val description = String(payload, 1, split - 1, charset)
        val valueStart = split + terminator
        if (valueStart > payload.size) return null
        val value = String(payload, valueStart, payload.size - valueStart, charset).trimEnd('\u0000')
        return description to value
    }

    private fun findTerminator(
        bytes: ByteArray,
        start: Int,
        width: Int,
    ): Int? {
        var index = start
        while (index + width <= bytes.size) {
            var zero = true
            for (offset in 0 until width) {
                if (bytes[index + offset].toInt() != 0) {
                    zero = false
                    break
                }
            }
            if (zero) return index
            index += width
        }
        return null
    }

    private fun syncsafe(
        bytes: ByteArray,
        offset: Int,
    ): Int =
        ((bytes[offset].toInt() and 0x7F) shl 21) or
            ((bytes[offset + 1].toInt() and 0x7F) shl 14) or
            ((bytes[offset + 2].toInt() and 0x7F) shl 7) or
            (bytes[offset + 3].toInt() and 0x7F)

    private fun writeSyncsafe(
        bytes: ByteArray,
        offset: Int,
        value: Int,
    ) {
        bytes[offset] = ((value shr 21) and 0x7F).toByte()
        bytes[offset + 1] = ((value shr 14) and 0x7F).toByte()
        bytes[offset + 2] = ((value shr 7) and 0x7F).toByte()
        bytes[offset + 3] = (value and 0x7F).toByte()
    }

    private fun syncsafeBytes(value: Int): ByteArray {
        val bytes = ByteArray(4)
        writeSyncsafe(bytes, 0, value)
        return bytes
    }

    private fun bigEndian(
        bytes: ByteArray,
        offset: Int,
    ): Int =
        ((bytes[offset].toInt() and 0xFF) shl 24) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 16) or
            ((bytes[offset + 2].toInt() and 0xFF) shl 8) or
            (bytes[offset + 3].toInt() and 0xFF)

    private fun intBytes(value: Int): ByteArray =
        byteArrayOf(
            ((value shr 24) and 0xFF).toByte(),
            ((value shr 16) and 0xFF).toByte(),
            ((value shr 8) and 0xFF).toByte(),
            (value and 0xFF).toByte(),
        )

    private fun uint32(
        bytes: ByteArray,
        offset: Int,
    ): Long =
        (bytes[offset].toLong() and 0xFF) or
            ((bytes[offset + 1].toLong() and 0xFF) shl 8) or
            ((bytes[offset + 2].toLong() and 0xFF) shl 16) or
            ((bytes[offset + 3].toLong() and 0xFF) shl 24)
}
