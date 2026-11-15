#!/usr/bin/env python3
"""Static source probe for the explicit WAV RIFF ID3v2 ReplayGain path."""

from __future__ import annotations

import argparse
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[4]
PACKAGE = ROOT / "app" / "src" / "main" / "java" / "com" / "ssilencce" / "data" / "local" / "replaygain"
CODEC = PACKAGE / "WavId3v2TagCodec.kt"
WRITER = PACKAGE / "ReplayGainTagWriter.kt"
MAIN_MODULE = ROOT / "app" / "src" / "main" / "java" / "com" / "ssilencce" / "MainModule.kt"
PROBE = ROOT / "openspec" / "changes" / "add-replaygain-calculation" / "probes" / "host-wav-id3v2" / "WavHostProbe.kt"
FIXTURES = ROOT / "openspec" / "changes" / "add-replaygain-calculation" / "probes" / "host-wav-id3v2" / "make_edge_fixtures.py"
VERIFIER = ROOT / "openspec" / "changes" / "add-replaygain-calculation" / "probes" / "host-wav-id3v2" / "verify_wav_probe.py"

results: list[tuple[str, bool]] = []


def check(name: str, condition: bool) -> None:
    results.append((name, condition))
    print(("PASS  " if condition else "FAIL  ") + name)


def main() -> int:
    global CODEC, WRITER
    parser = argparse.ArgumentParser()
    parser.add_argument("--codec", type=Path, default=CODEC)
    parser.add_argument("--writer", type=Path, default=WRITER)
    args = parser.parse_args()
    CODEC = args.codec
    WRITER = args.writer
    codec = CODEC.read_text(encoding="utf-8")
    writer = WRITER.read_text(encoding="utf-8")
    main_module = MAIN_MODULE.read_text(encoding="utf-8")
    probe = PROBE.read_text(encoding="utf-8")
    fixtures = FIXTURES.read_text(encoding="utf-8")
    verifier = VERIFIER.read_text(encoding="utf-8")
    write_wav = writer[writer.index("private fun writeWav("):writer.index("private fun restoreWavAlbumTags(")]
    restore_wav = writer[writer.index("private fun restoreWavAlbumTags("):writer.index("private fun readWavProperties(")]
    verify = writer[writer.index("private fun verify("):writer.index("private fun valuesMatch(")]
    recover = writer[writer.index("private fun recoverWavBackup("):writer.index("private fun restoreAfterFailedVerification(")]

    check("codec file exists", CODEC.is_file())
    check("codec writes uppercase ID3 chunk", 'ID3_CHUNK_ID = "ID3 "' in codec)
    check("codec parses RIFF chunks", "private fun readChunks" in codec)
    check("codec reads ReplayGain", "fun readReplayGain" in codec)
    check("codec writes ReplayGain", "fun writeReplayGain" in codec)
    check("codec replaces/removes TXXX frames", "removeTrack" in codec and "removeAlbum" in codec)
    check("codec preserves retained frames", "retained" in codec and "filterNot" in codec)
    check("codec builds ID3v2 tags", "private fun buildTag" in codec and "private fun parseTag" in codec)
    check("codec writes TXXX frames", "private fun txxxFrame" in codec and "private fun parseTxxx" in codec)
    check("codec handles lowercase id3 chunk", "equals(ID3_CHUNK_ID, ignoreCase = true)" in codec)
    check("codec shifts later chunks", "private fun shift(" in codec and "newChunkTotal" in codec)
    check("codec updates RIFF size", "private fun writeRiffSize" in codec)
    check("codec truncates shrinking files", "write.truncate(newSize)" in codec)
    check("codec forces write", "write.force(true)" in codec)
    check("codec has no Android imports", "import android." not in codec)
    check("codec has no TagLib dependency", "import com.kyant.taglib" not in codec)
    check("writer routes WAV explicitly", "if (tagFormat == TagFormat.Wav)" in writer)
    check("writer calls WAV writer", "writeWav(" in writer and "replaceAlbum = albumGainDb != null" in writer)
    check("WAV writer uses RIFF codec", "WavId3v2TagCodec" in write_wav and "writeReplayGain" in write_wav)
    check("WAV writer opens rw descriptor", 'openFileDescriptor(song.uri, "rw")' in write_wav)
    check("WAV writer uses duplicated descriptors", "fd.dup()" in write_wav and "AutoCloseInputStream" in write_wav and "AutoCloseOutputStream" in write_wav)
    check("WAV writer reads back through fresh descriptor", "readWavProperties(song.uri, checkpoint)" in write_wav)
    check("WAV writer requires persisted values", 'WAV ReplayGain tags were not persisted' in write_wav)
    check("WAV snapshot uses codec", "isWav(song)" in writer and "readWavProperties(song.uri" in writer)
    check("WAV restore is wired", "restoreWavAlbumTags(song, previous" in writer)
    check("WAV restore preserves tracks", "removeTrack = false" in restore_wav and "removeAlbum = true" in restore_wav)
    check("WAV restore verifies album state", "verifyAlbumState(previous, readBack)" in restore_wav)
    check("WAV restore preserves alias keys", "key to first" in restore_wav and ".toMap()" in restore_wav)
    check("verify rejects duplicate values", "values.size != 1" in verify)
    check("WAV writer has no TagLib save path", "TagLib" not in write_wav and "savePropertyMap" not in write_wav)
    check("WAV reader opens read descriptor", 'openFileDescriptor(uri, "r")' in writer[writer.index("private fun readWavProperties("):])
    check("codec stages a verified backup", "backupFile: File? = null" in codec and "writeVerifiedBackup" in codec)
    check("codec restores on failure", "restoreFromFile" in codec and "backupCreated" in codec)
    check("codec exposes mid-write observer", "WriteObserver" in codec and "beforeCommit" in codec)
    check("codec has v2.3 frame masks", all(mask in codec for mask in ("0x0080", "0x0040", "0x0020")))
    check("codec has v2.4 frame masks", all(mask in codec for mask in ("0x0040", "0x0008", "0x0004", "0x0002", "0x0001")))
    check("writer stages a deterministic backup", 'replaygain-wav-${song.id}.bak' in write_wav and "backupFile = backup" in write_wav)
    check("writer has a temp directory", "private val tempDir: File" in writer)
    check("DI supplies a temp directory", "tempDir = androidContext()." in main_module)
    check("rollback verifier rejects duplicate aliases", "actualEntries.size != 1" in writer and "actualEntry.value.size != 1" in writer)
    check("host probe covers mid-file ID3", "midfile-id3.wav" in probe)
    check("host probe covers version flags", "flag-v24-unsync.wav" in probe and "flag-v23-compression.wav" in probe)
    check("host probe covers mid-write restore", "injected mid-write failure" in probe and "mid-write original restored" in probe)
    check("codec exposes crash restore", "fun restoreBackup" in codec)
    check("writer recovers a leftover backup", "recoverWavBackup" in writer)
    check("codec bounds ID3 tag size", "MAX_TAG_SIZE" in codec and "WAV ID3 chunk is too large" in codec)
    check("codec bounds chunk count", "MAX_CHUNKS" in codec and "Too many RIFF chunks" in codec)
    check("codec rejects multiple ID3 on read", codec.count("Multiple WAV ID3 chunks are not supported") >= 2)
    check("codec rejects declared tag size mismatch", "ID3 tag size exceeds chunk" in codec and "ID3 tag has undeclared frames" in codec)
    check("codec rejects missing pad and trailing bytes", "Malformed RIFF chunk padding" in codec and "Trailing bytes after RIFF chunks" in codec)
    check("codec normalizes aliases by suffix", "ReplayGainTags.suffixOf(normalized)" in codec and "ReplayGainTags.suffixOf(it)" in codec)
    check("codec removes iTunes/R128 aliases", "suffix in TRACK_FIELDS" in codec and "suffix in ALBUM_FIELDS" in codec)
    check("codec exposes a checkpoint", "checkpoint: () -> Unit = {}" in codec)
    check("codec rethrows cancellation after restore", "throwable is CancellationException" in codec and "throw throwable" in codec)
    check("codec writes a pending marker", "writePendingMarker" in codec and "BACKUP_PENDING" in codec)
    check("codec writes a committed marker", "markBackupCommitted" in codec and "BACKUP_COMMITTED" in codec)
    check("codec checks backup deletion", "fun deleteBackup" in codec and "marker.delete()" in codec)
    check("codec validates restore header and size", "WAV backup is not a RIFF WAVE file" in codec and "WAV backup restore size mismatch" in codec)
    check("writer marks the backup committed", "markBackupCommitted(backup)" in write_wav and "markBackupCommitted(backup)" in restore_wav)
    check("writer deletes only after readback", write_wav.index("markBackupCommitted(backup)") > write_wav.index("verify(values, readBack)"))
    check("writer restores on readback failure", "restoreAfterFailedVerification(song, backup)" in write_wav and "restoreAfterFailedVerification(song, backup)" in restore_wav)
    check("writer is MIME-aware for WAV", "contentResolver.getType(song.uri)" in writer and '"audio/vnd.wave"' in writer)
    check("writer serializes per song", "ConcurrentHashMap<Long, Any>" in writer and "synchronized(lockFor(song.id))" in writer)
    check("writer rethrows cancellation in snapshot", "if (throwable is CancellationException) throw throwable" in writer)
    check("writer passes checkpoints to the codec", "checkpoint = { context.ensureActive() }" in writer and "checkpoint = checkpoint" in write_wav)
    check("codec classifies backup states", "fun inspectBackup" in codec and "BackupAction.DISCARD" in codec and "BackupAction.REFUSE" in codec and "BackupAction.RESTORE" in codec)
    check("writer uses backup classification", "inspectBackup" in recover and "BackupAction.RESTORE" in recover)
    check("host probe covers hostile fixtures", all(name in probe for name in ("tag-size-under.wav", "odd-final-no-pad.wav", "trailing-garbage.wav", "huge-tag.wav", "many-chunks.wav")))
    check("host probe covers alias fixtures", all(name in probe for name in ("alias-itunes.wav", "alias-r128.wav", "alias-case.wav", "duplicate-alias.wav")))
    check("fixtures generate hostile cases", all(name in fixtures for name in ("tag-size-under.wav", "odd-final-no-pad.wav", "trailing-garbage.wav", "huge-tag.wav", "many-chunks.wav")))
    check("verifier checks alias cleanup", "no unexpected RG keys" in verifier and "alias" in verifier)
    check("no shared MetadataWriter backup helper", "MetadataWriter" not in codec and "MetadataWriter" not in writer)

    passed = sum(1 for _, condition in results if condition)
    failed = len(results) - passed
    print(f"RESULT {passed}/{len(results)} PASS")
    return 0 if failed == 0 else 1


if __name__ == "__main__":
    sys.exit(main())
