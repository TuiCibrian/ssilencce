package com.ssilencce.data.local.replaygain

import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.util.concurrent.CancellationException

private var passed = 0
private var failed = 0
private val failures = mutableListOf<String>()

private fun check(
    name: String,
    condition: Boolean,
    detail: String = "",
) {
    if (condition) {
        passed++
        println("PASS  $name")
    } else {
        failed++
        failures += "$name${if (detail.isEmpty()) "" else ": $detail"}"
        println("FAIL  $name${if (detail.isEmpty()) "" else ": $detail"}")
    }
}

private fun sha256(bytes: ByteArray): String =
    MessageDigest
        .getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { "%02x".format(it.toInt() and 0xFF) }

private fun sha256(path: Path): String = sha256(Files.readAllBytes(path))

private data class Chunk(
    val id: String,
    val offset: Long,
    val size: Long,
)

private fun chunks(path: Path): List<Chunk> {
    val bytes = Files.readAllBytes(path)
    require(bytes.size >= 12)
    require(String(bytes, 0, 4, Charsets.US_ASCII) == "RIFF")
    require(String(bytes, 8, 4, Charsets.US_ASCII) == "WAVE")
    val result = mutableListOf<Chunk>()
    var offset = 12
    while (offset + 8 <= bytes.size) {
        val id = String(bytes, offset, 4, Charsets.US_ASCII)
        val size = ByteBuffer.wrap(bytes, offset + 4, 4).order(ByteOrder.LITTLE_ENDIAN).int.toLong() and 0xFFFFFFFFL
        result += Chunk(id, offset.toLong(), size)
        offset += 8 + size.toInt() + (size.toInt() and 1)
    }
    return result
}

private fun dataHash(path: Path): String {
    val bytes = Files.readAllBytes(path)
    val data = chunks(path).firstOrNull { it.id == "data" } ?: error("no data chunk")
    return sha256(bytes.copyOfRange((data.offset + 8).toInt(), (data.offset + 8 + data.size).toInt()))
}

private fun id3Count(path: Path): Int = chunks(path).count { it.id.equals("ID3 ", ignoreCase = true) }

private fun chunkPayload(path: Path, id: String): ByteArray {
    val bytes = Files.readAllBytes(path)
    val chunk = chunks(path).first { it.id == id }
    return bytes.copyOfRange((chunk.offset + 8).toInt(), (chunk.offset + 8 + chunk.size).toInt())
}

private fun riffSizeOk(path: Path): Boolean {
    val bytes = Files.readAllBytes(path)
    if (bytes.size < 12) return false
    val declared = ByteBuffer.wrap(bytes, 4, 4).order(ByteOrder.LITTLE_ENDIAN).int.toLong() and 0xFFFFFFFFL
    return declared == bytes.size.toLong() - 8
}

private fun tagValues(path: Path): Map<String, Array<String>> =
    FileChannel.open(path, StandardOpenOption.READ).use { WavId3v2TagCodec.readReplayGain(it) }

private fun write(
    path: Path,
    values: Map<String, String>,
    removeTrack: Boolean,
    removeAlbum: Boolean,
): WavId3v2TagCodec.WriteResult =
    FileChannel.open(path, StandardOpenOption.READ, StandardOpenOption.WRITE).use { channel ->
        WavId3v2TagCodec.writeReplayGain(
            read = channel,
            write = channel,
            values = values,
            removeTrack = removeTrack,
            removeAlbum = removeAlbum,
        )
    }

private fun copy(
    source: Path,
    destination: Path,
) {
    Files.createDirectories(destination.parent)
    Files.copy(source, destination, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
}

private fun valuesMatch(
    name: String,
    actual: Map<String, Array<String>>,
    expected: Map<String, String>,
) {
    check("$name keys", actual.keys == expected.keys, "actual=${actual.keys} expected=${expected.keys}")
    for ((field, value) in expected) {
        val entries = actual[field]
        check("$name $field count", entries?.size == 1, "count=${entries?.size}")
        check("$name $field value", entries?.firstOrNull() == value, "actual=${entries?.firstOrNull()} expected=$value")
    }
}

fun main(args: Array<String>) {
    val fixtures = Path.of(args[0])
    val out = Path.of(args[1])
    Files.createDirectories(out)

    val trackValues = linkedMapOf("REPLAYGAIN_TRACK_GAIN" to "2.68 dB", "REPLAYGAIN_TRACK_PEAK" to "0.10000610")
    val replacementTrackValues = linkedMapOf("REPLAYGAIN_TRACK_GAIN" to "3.21 dB", "REPLAYGAIN_TRACK_PEAK" to "0.12500000")
    val albumValues = linkedMapOf("REPLAYGAIN_ALBUM_GAIN" to "-4.54 dB", "REPLAYGAIN_ALBUM_PEAK" to "0.19952393")
    val replacementAlbumValues = linkedMapOf("REPLAYGAIN_ALBUM_GAIN" to "-3.75 dB", "REPLAYGAIN_ALBUM_PEAK" to "0.25000000")

    // 1. Single-track write and repeated replacement.
    run {
        val target = out.resolve("single/rg_wav_track.wav")
        copy(fixtures.resolve("rg_wav_track.wav"), target)
        val beforeLayout = chunks(target)
        val beforeAudio = dataHash(target)
        val beforeFmt = beforeLayout.first { it.id == "fmt " }
        val beforeData = beforeLayout.first { it.id == "data" }
        check("single starts with one ID3 chunk", id3Count(target) == 1)
        val first = write(target, trackValues, removeTrack = true, removeAlbum = false)
        check("single first write", first.success, first.message ?: "")
        check("single first ID3 count", id3Count(target) == 1, "count=${id3Count(target)}")
        check("single first RIFF size", riffSizeOk(target))
        valuesMatch("single first", tagValues(target), trackValues)
        val afterLayout = chunks(target)
        check("single fmt unchanged", afterLayout.first { it.id == "fmt " } == beforeFmt)
        check("single data unchanged", afterLayout.first { it.id == "data" } == beforeData)
        check("single audio unchanged", dataHash(target) == beforeAudio)
        val second = write(target, replacementTrackValues, removeTrack = true, removeAlbum = false)
        check("single replacement write", second.success, second.message ?: "")
        check("single replacement ID3 count", id3Count(target) == 1, "count=${id3Count(target)}")
        check("single replacement RIFF size", riffSizeOk(target))
        valuesMatch("single replacement", tagValues(target), replacementTrackValues)
        check("single replacement audio unchanged", dataHash(target) == beforeAudio)
    }

    // 2. WAV without an existing ID3 chunk: append one and preserve audio.
    run {
        val target = out.resolve("no-id3/plain-wav.wav")
        copy(fixtures.resolve("plain-wav.wav"), target)
        val beforeAudio = dataHash(target)
        check("no-id3 starts without ID3", id3Count(target) == 0)
        val result = write(target, trackValues, removeTrack = true, removeAlbum = false)
        check("no-id3 write", result.success, result.message ?: "")
        check("no-id3 ID3 count", id3Count(target) == 1, "count=${id3Count(target)}")
        check("no-id3 RIFF size", riffSizeOk(target))
        valuesMatch("no-id3", tagValues(target), trackValues)
        check("no-id3 audio unchanged", dataHash(target) == beforeAudio)
    }

    // 3. Complete album, repeated album replacement.
    run {
        val albumTracks =
            listOf(
                Triple("01.wav", "2.68 dB", "0.10000610"),
                Triple("02.wav", "7.99 dB", "0.05010986"),
                Triple("03.wav", "-7.12 dB", "0.19952393"),
            )
        for ((name, gain, peak) in albumTracks) {
            val target = out.resolve("complete/$name")
            copy(fixtures.resolve("wav_album/$name"), target)
            val beforeAudio = dataHash(target)
            val values = linkedMapOf("REPLAYGAIN_TRACK_GAIN" to gain, "REPLAYGAIN_TRACK_PEAK" to peak) + albumValues
            val result = write(target, values, removeTrack = true, removeAlbum = true)
            check("complete $name write", result.success, result.message ?: "")
            check("complete $name RIFF size", riffSizeOk(target))
            valuesMatch("complete $name", tagValues(target), values)
            check("complete $name audio unchanged", dataHash(target) == beforeAudio)
            val replacement = linkedMapOf("REPLAYGAIN_TRACK_GAIN" to gain, "REPLAYGAIN_TRACK_PEAK" to peak) + replacementAlbumValues
            val second = write(target, replacement, removeTrack = true, removeAlbum = true)
            check("complete $name replacement", second.success, second.message ?: "")
            check("complete $name replacement RIFF size", riffSizeOk(target))
            valuesMatch("complete $name replacement", tagValues(target), replacement)
            check("complete $name replacement audio unchanged", dataHash(target) == beforeAudio)
        }
        val albumGains =
            listOf("01.wav", "02.wav", "03.wav").map {
                tagValues(out.resolve("complete/$it"))["REPLAYGAIN_ALBUM_GAIN"]?.firstOrNull()
            }
        check("complete album gain identical", albumGains.distinct().size == 1, "values=$albumGains")
    }

    // 3. Partial album: track tags only; unselected third track untouched.
    run {
        val beforeThird = sha256(fixtures.resolve("wav_partial/03.wav"))
        copy(fixtures.resolve("wav_partial/03.wav"), out.resolve("partial/03.wav"))
        for ((name, gain, peak) in listOf(
            Triple("01.wav", "2.68 dB", "0.10000610"),
            Triple("02.wav", "7.99 dB", "0.05010986"),
        )) {
            val target = out.resolve("partial/$name")
            copy(fixtures.resolve("wav_partial/$name"), target)
            val beforeAudio = dataHash(target)
            val values = linkedMapOf("REPLAYGAIN_TRACK_GAIN" to gain, "REPLAYGAIN_TRACK_PEAK" to peak)
            val result = write(target, values, removeTrack = true, removeAlbum = false)
            check("partial $name write", result.success, result.message ?: "")
            check("partial $name RIFF size", riffSizeOk(target))
            valuesMatch("partial $name", tagValues(target), values)
            check("partial $name no album tags", tagValues(target).keys.none { it.startsWith("REPLAYGAIN_ALBUM_") })
            check("partial $name audio unchanged", dataHash(target) == beforeAudio)
            val second = write(target, replacementTrackValues, removeTrack = true, removeAlbum = false)
            check("partial $name replacement", second.success, second.message ?: "")
            check("partial $name replacement RIFF size", riffSizeOk(target))
            valuesMatch("partial $name replacement", tagValues(target), replacementTrackValues)
            check("partial $name replacement audio unchanged", dataHash(target) == beforeAudio)
        }
        check("partial third untouched", sha256(out.resolve("partial/03.wav")) == beforeThird)
    }

    // 4. Track-only write preserves pre-existing album tags; album restore preserves track tags.
    run {
        val target = out.resolve("preserve/rg_wav_track.wav")
        copy(fixtures.resolve("rg_wav_track.wav"), target)
        val complete = linkedMapOf("REPLAYGAIN_TRACK_GAIN" to "2.68 dB", "REPLAYGAIN_TRACK_PEAK" to "0.10000610") + albumValues
        check("preserve seed", write(target, complete, removeTrack = true, removeAlbum = true).success)
        val trackOnly = linkedMapOf("REPLAYGAIN_TRACK_GAIN" to "3.21 dB", "REPLAYGAIN_TRACK_PEAK" to "0.12500000")
        check("preserve track-only write", write(target, trackOnly, removeTrack = true, removeAlbum = false).success)
        val preserved = tagValues(target)
        check("preserve track replaced", preserved["REPLAYGAIN_TRACK_GAIN"]?.firstOrNull() == "3.21 dB")
        check("preserve album kept", preserved["REPLAYGAIN_ALBUM_GAIN"]?.firstOrNull() == "-4.54 dB")
        check("preserve album peak kept", preserved["REPLAYGAIN_ALBUM_PEAK"]?.firstOrNull() == "0.19952393")
        val restored = linkedMapOf("REPLAYGAIN_ALBUM_GAIN" to "-6.00 dB", "REPLAYGAIN_ALBUM_PEAK" to "0.95000000")
        check("preserve album restore", write(target, restored, removeTrack = false, removeAlbum = true).success)
        val afterRestore = tagValues(target)
        check("restore track kept", afterRestore["REPLAYGAIN_TRACK_GAIN"]?.firstOrNull() == "3.21 dB")
        check("restore album replaced", afterRestore["REPLAYGAIN_ALBUM_GAIN"]?.firstOrNull() == "-6.00 dB")
        check("restore album peak replaced", afterRestore["REPLAYGAIN_ALBUM_PEAK"]?.firstOrNull() == "0.95000000")
        check("restore RIFF size", riffSizeOk(target))
    }

    // 5. Mid-file ID3 chunk before fmt/data: growth and shrink.
    run {
        val target = out.resolve("midfile/midfile-id3.wav")
        copy(fixtures.resolve("midfile-id3.wav"), target)
        val beforeLayout = chunks(target)
        val beforeFmt = chunkPayload(target, "fmt ")
        val beforeData = chunkPayload(target, "data")
        val beforeAudio = dataHash(target)
        check("midfile starts with ID3", beforeLayout.first().id.equals("ID3 ", ignoreCase = true))
        val growth = write(target, trackValues + albumValues, removeTrack = true, removeAlbum = true)
        check("midfile growth write", growth.success, growth.message ?: "")
        check("midfile growth RIFF size", riffSizeOk(target))
        check("midfile growth ID3 count", id3Count(target) == 1)
        valuesMatch("midfile growth", tagValues(target), trackValues + albumValues)
        check("midfile growth fmt unchanged", chunkPayload(target, "fmt ").contentEquals(beforeFmt))
        check("midfile growth data unchanged", chunkPayload(target, "data").contentEquals(beforeData))
        check("midfile growth audio unchanged", dataHash(target) == beforeAudio)
        val shrink = write(target, replacementTrackValues, removeTrack = true, removeAlbum = true)
        check("midfile shrink write", shrink.success, shrink.message ?: "")
        check("midfile shrink RIFF size", riffSizeOk(target))
        check("midfile shrink ID3 count", id3Count(target) == 1)
        valuesMatch("midfile shrink", tagValues(target), replacementTrackValues)
        check("midfile shrink fmt unchanged", chunkPayload(target, "fmt ").contentEquals(beforeFmt))
        check("midfile shrink data unchanged", chunkPayload(target, "data").contentEquals(beforeData))
        check("midfile shrink audio unchanged", dataHash(target) == beforeAudio)
    }

    // 6. Unsupported ID3 variants fail closed without changing bytes.
    for (name in listOf(
        "multi-id3.wav",
        "flag-v23-compression.wav",
        "flag-v23-encryption.wav",
        "flag-v23-grouping.wav",
        "flag-v24-grouping.wav",
        "flag-v24-unsync.wav",
        "flag-v24-dli.wav",
        "tag-size-under.wav",
        "tag-size-over.wav",
        "odd-final-no-pad.wav",
        "trailing-garbage.wav",
        "huge-tag.wav",
        "many-chunks.wav",
    )) {
        val target = out.resolve("reject/$name")
        copy(fixtures.resolve(name), target)
        val before = sha256(target)
        val result = write(target, trackValues, removeTrack = true, removeAlbum = false)
        check("$name rejected", !result.success, result.message ?: "")
        check("$name unchanged", sha256(target) == before)
    }
    check(
        "multi-id3 read rejected",
        runCatching { tagValues(out.resolve("reject/multi-id3.wav")) }.isFailure,
    )

    // 7. Injected mid-write failure restores the original bytes from the backup.
    run {
        val target = out.resolve("midwrite/rg_wav_track.wav")
        copy(fixtures.resolve("rg_wav_track.wav"), target)
        val before = sha256(target)
        val backup = out.resolve("midwrite-backup.bak").toFile()
        val result =
            FileChannel.open(target, StandardOpenOption.READ, StandardOpenOption.WRITE).use { channel ->
                WavId3v2TagCodec.writeReplayGain(
                    read = channel,
                    write = channel,
                    values = trackValues,
                    removeTrack = true,
                    removeAlbum = false,
                    backupFile = backup,
                    observer = WavId3v2TagCodec.WriteObserver { throw IOException("injected mid-write failure") },
                )
            }
        check("mid-write failure reported", !result.success, result.message ?: "")
        check("mid-write original restored", sha256(target) == before)
        check("mid-write backup deleted", !backup.exists())
        check("mid-write RIFF size", riffSizeOk(target))
    }

    // 8. Crash recovery: a leftover backup restores the original bytes on the next write.
    run {
        val target = out.resolve("crash-recovery/rg_wav_track.wav")
        copy(fixtures.resolve("rg_wav_track.wav"), target)
        val original = sha256(target)
        val backup = Files.createTempFile(out, "crash-backup-", ".bak").toFile()
        Files.copy(target, backup.toPath(), StandardCopyOption.REPLACE_EXISTING)
        FileChannel.open(target, StandardOpenOption.READ, StandardOpenOption.WRITE).use { channel ->
            channel.truncate(100)
            channel.write(ByteBuffer.wrap(byteArrayOf(1, 2, 3)), 0)
        }
        check("crash target corrupted", sha256(target) != original)
        val result =
            FileChannel.open(target, StandardOpenOption.READ, StandardOpenOption.WRITE).use { channel ->
                WavId3v2TagCodec.restoreBackup(channel, backup)
            }
        check("crash recovery success", result.success, result.message ?: "")
        check("crash recovery restored", sha256(target) == original)
        check("crash backup deleted", backup.delete())
    }

    // 9. Backup state machine: stale, committed, missing, partial and size-mismatched backups.
    run {
        val dir = out.resolve("backup-state")
        Files.createDirectories(dir)
        val source = fixtures.resolve("rg_wav_track.wav")
        check("backup none", WavId3v2TagCodec.inspectBackup(dir.resolve("none.bak").toFile(), "test-uri").action == WavId3v2TagCodec.BackupAction.NONE)
        run {
            val backup = dir.resolve("stale.bak")
            Files.copy(source, backup, StandardCopyOption.REPLACE_EXISTING)
            check(
                "backup stale discarded",
                WavId3v2TagCodec.inspectBackup(backup.toFile(), "test-uri").action == WavId3v2TagCodec.BackupAction.DISCARD,
            )
        }
        run {
            val backup = dir.resolve("missing.bak")
            Files.writeString(dir.resolve("missing.bak.pending"), "pending size=1 tag=test-uri")
            check(
                "backup missing refused",
                WavId3v2TagCodec.inspectBackup(backup.toFile(), "test-uri").action == WavId3v2TagCodec.BackupAction.REFUSE,
            )
        }
        run {
            val backup = dir.resolve("committed.bak")
            Files.copy(source, backup, StandardCopyOption.REPLACE_EXISTING)
            Files.writeString(dir.resolve("committed.bak.pending"), "committed")
            check(
                "backup committed discarded",
                WavId3v2TagCodec.inspectBackup(backup.toFile(), "test-uri").action == WavId3v2TagCodec.BackupAction.DISCARD,
            )
        }
        run {
            val backup = dir.resolve("pending.bak")
            Files.copy(source, backup, StandardCopyOption.REPLACE_EXISTING)
            Files.writeString(dir.resolve("pending.bak.pending"), "pending size=${Files.size(backup)} tag=test-uri")
            val inspected = WavId3v2TagCodec.inspectBackup(backup.toFile(), "test-uri")
            check(
                "backup pending restored",
                inspected.action == WavId3v2TagCodec.BackupAction.RESTORE && inspected.expectedSize == Files.size(backup),
            )
        }
        run {
            val backup = dir.resolve("mismatch.bak")
            Files.copy(source, backup, StandardCopyOption.REPLACE_EXISTING)
            Files.writeString(dir.resolve("mismatch.bak.pending"), "pending size=${Files.size(backup) + 1} tag=test-uri")
            check(
                "backup size mismatch refused",
                WavId3v2TagCodec.inspectBackup(backup.toFile(), "test-uri").action == WavId3v2TagCodec.BackupAction.REFUSE,
            )
        }
        run {
            val backup = dir.resolve("tag-mismatch.bak")
            Files.copy(source, backup, StandardCopyOption.REPLACE_EXISTING)
            Files.writeString(dir.resolve("tag-mismatch.bak.pending"), "pending size=${Files.size(backup)} tag=other-uri")
            check(
                "backup tag mismatch discarded",
                WavId3v2TagCodec.inspectBackup(backup.toFile(), "test-uri").action == WavId3v2TagCodec.BackupAction.DISCARD,
            )
        }
        run {
            val backup = dir.resolve("partial-marker.bak")
            Files.copy(source, backup, StandardCopyOption.REPLACE_EXISTING)
            Files.writeString(dir.resolve("partial-marker.bak.pending"), "pending size=")
            check(
                "backup partial marker restored",
                WavId3v2TagCodec.inspectBackup(backup.toFile(), "test-uri").action == WavId3v2TagCodec.BackupAction.RESTORE,
            )
        }
    }

    // 10. Restore failure retains the only good copy and reports its path.
    run {
        val dir = out.resolve("restore-fail")
        Files.createDirectories(dir)
        val target = dir.resolve("rg_wav_track.wav")
        copy(fixtures.resolve("rg_wav_track.wav"), target)
        val original = sha256(target)
        val backup = dir.resolve("target.bak").toFile()
        val channel = FileChannel.open(target, StandardOpenOption.READ, StandardOpenOption.WRITE)
        val result =
            WavId3v2TagCodec.writeReplayGain(
                read = channel,
                write = channel,
                values = trackValues,
                removeTrack = true,
                removeAlbum = false,
                backupFile = backup,
                backupTag = "test-uri",
                observer = WavId3v2TagCodec.WriteObserver { channel.close() },
            )
        check("restore failure reported", !result.success, result.message ?: "")
        check("restore failure retains backup", result.backupRetained && backup.exists())
        check("restore failure retains marker", WavId3v2TagCodec.backupMarker(backup).exists())
        check("restore failure names backup", result.message?.contains("retained") == true, result.message ?: "")
        FileChannel.open(target, StandardOpenOption.READ, StandardOpenOption.WRITE).use { fresh ->
            val restored = WavId3v2TagCodec.restoreBackup(fresh, backup)
            check("manual restore after retention", restored.success, restored.message ?: "")
        }
        check("manual restore bytes", sha256(target) == original)
        check("manual restore cleanup", WavId3v2TagCodec.deleteBackup(backup))
    }

    // 11. Cancellation restores the original bytes and removes the recovery files.
    run {
        val dir = out.resolve("cancel")
        Files.createDirectories(dir)
        val target = dir.resolve("rg_wav_track.wav")
        copy(fixtures.resolve("rg_wav_track.wav"), target)
        val original = sha256(target)
        val backup = dir.resolve("target.bak").toFile()
        val marker = WavId3v2TagCodec.backupMarker(backup)
        var cancelled: Throwable? = null
        try {
            FileChannel.open(target, StandardOpenOption.READ, StandardOpenOption.WRITE).use { channel ->
                WavId3v2TagCodec.writeReplayGain(
                    read = channel,
                    write = channel,
                    values = trackValues,
                    removeTrack = true,
                    removeAlbum = false,
                    backupFile = backup,
                    backupTag = "test-uri",
                    checkpoint = {
                        if (backup.exists() && marker.exists()) {
                            throw CancellationException("injected cancellation")
                        }
                    },
                )
            }
        } catch (throwable: Throwable) {
            cancelled = throwable
        }
        check("cancellation propagates", cancelled is CancellationException, cancelled?.javaClass?.name ?: "none")
        check("cancellation restores bytes", sha256(target) == original)
        check("cancellation removes backup", !backup.exists())
        check("cancellation removes marker", !marker.exists())
    }

    // 12. Success lifecycle: backup survives until the caller commits, then is removed.
    run {
        val dir = out.resolve("commit")
        Files.createDirectories(dir)
        val target = dir.resolve("rg_wav_track.wav")
        copy(fixtures.resolve("rg_wav_track.wav"), target)
        val backup = dir.resolve("target.bak").toFile()
        val result =
            FileChannel.open(target, StandardOpenOption.READ, StandardOpenOption.WRITE).use { channel ->
                WavId3v2TagCodec.writeReplayGain(
                    read = channel,
                    write = channel,
                    values = trackValues,
                    removeTrack = true,
                    removeAlbum = false,
                    backupFile = backup,
                    backupTag = "test-uri",
                )
            }
        check("commit write success", result.success, result.message ?: "")
        check("commit backup retained for readback", backup.exists())
        check("commit marker pending", WavId3v2TagCodec.backupMarker(backup).exists())
        WavId3v2TagCodec.markBackupCommitted(backup)
        check(
            "commit marker committed",
            WavId3v2TagCodec.inspectBackup(backup, "test-uri").action == WavId3v2TagCodec.BackupAction.DISCARD,
        )
        check("commit cleanup", WavId3v2TagCodec.deleteBackup(backup))
        check("commit cleanup files", !backup.exists() && !WavId3v2TagCodec.backupMarker(backup).exists())
    }

    // 13. Read-back failure primitive: restoring the retained backup returns the original bytes.
    run {
        val dir = out.resolve("readback-fail")
        Files.createDirectories(dir)
        val target = dir.resolve("rg_wav_track.wav")
        copy(fixtures.resolve("rg_wav_track.wav"), target)
        val original = sha256(target)
        val backup = dir.resolve("target.bak").toFile()
        val result =
            FileChannel.open(target, StandardOpenOption.READ, StandardOpenOption.WRITE).use { channel ->
                WavId3v2TagCodec.writeReplayGain(
                    read = channel,
                    write = channel,
                    values = trackValues,
                    removeTrack = true,
                    removeAlbum = false,
                    backupFile = backup,
                    backupTag = "test-uri",
                )
            }
        check("readback write success", result.success, result.message ?: "")
        check("readback changed bytes", sha256(target) != original)
        FileChannel.open(target, StandardOpenOption.READ, StandardOpenOption.WRITE).use { channel ->
            val restored = WavId3v2TagCodec.restoreBackup(channel, backup)
            check("readback restore success", restored.success, restored.message ?: "")
        }
        check("readback restore bytes", sha256(target) == original)
        check("readback cleanup", WavId3v2TagCodec.deleteBackup(backup))
    }

    // 14. iTunes/R128/case aliases are replaced by exactly one standard TXXX per field.
    for (name in listOf("alias-itunes.wav", "alias-r128.wav", "alias-case.wav", "duplicate-alias.wav")) {
        val target = out.resolve("alias/$name")
        copy(fixtures.resolve(name), target)
        val result = write(target, trackValues, removeTrack = true, removeAlbum = false)
        check("$name write", result.success, result.message ?: "")
        valuesMatch("$name", tagValues(target), trackValues)
        check(
            "$name no alias keys",
            tagValues(target).keys.none { key ->
                key.contains("iTunes", ignoreCase = true) ||
                    key.contains("R128", ignoreCase = true) ||
                    key != key.uppercase()
            },
        )
    }

    println("RESULT pass=$passed fail=$failed")
    if (failures.isNotEmpty()) {
        failures.forEach { println("FAILURE $it") }
    }
    if (failed != 0) {
        kotlin.system.exitProcess(1)
    }
}




