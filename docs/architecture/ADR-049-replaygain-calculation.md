# ADR-049: Offline ReplayGain Calculation and Verified Tag Writes

## Status

Proposed (2026-11-06)

## Context

ssilencce already reads ReplayGain tags and applies them through
`ReplayGainAudioProcessor` (ADR-014). It has no offline measurement path, so songs without
embedded tags cannot be normalized. The AA-06 audit confirmed that:

- the bundled TagLib wrapper reads/writes metadata but cannot decode PCM or measure loudness;
- `MetadataWriter` filters `REPLAYGAIN_*` before `TagLib.savePropertyMap`, and TagLib's
  `FileRef::setProperties` replaces the tag map, so ordinary tag edits can drop existing
  standard ReplayGain tags;
- the bundled Jellyfin FFmpeg decoder is package-private and `media3-transformer` is not a
  dependency, so the smallest no-dependency decode path is `MediaExtractor` + `MediaCodec`;
- `ReplayGain` used `0f`/`1f` sentinels, which cannot distinguish a measured 0.00 dB value from
  a missing tag.

## Decision

- Add an Android-free `ReplayGainAnalyzer` implementing ITU-R BS.1770-4 / EBU R128:
  rate-aware K-weighting, 400 ms blocks with a 100 ms hop, absolute -70 LUFS and relative -10 LU
  gates, integrated LUFS and linear sample peak.
- Use the -18 LUFS ReplayGain 2.0 reference. Track gain is `-18 - LUFS`; album gain is
  `-18 - album LUFS` from concatenated block energies. Peak-limit and clamp gains to a safe range.
- Decode with framework `MediaExtractor` + `MediaCodec` (8/16-bit or float PCM, one output-format
  change, EOS, cancellation). Mono/stereo only; unsupported codecs/encodings fail per file. Count
  decoded frames and reject a materially short decode or partial frame against the declared
  duration so a truncated file cannot be measured as a shorter valid track.
- Group selected songs by album. Write album gain only when the selection contains the complete
  album and every track measured successfully; partial selections write track tags only. Never
  silently expand the selection or fabricate partial album gain.
- Write format-aware tags: standard uppercase keys for MP3/FLAC/OGG, an explicit RIFF `ID3 `
  chunk codec for WAV, iTunes freeform keys for MP4/M4A, and Opus R128 gains. Remove old aliases,
  preserve non-ReplayGain tags, close the write descriptor, reopen the URI with a fresh read
  descriptor, and verify the persisted map. WAV uses `WavId3v2TagCodec` because TagLib's WAV
  `savePropertyMap` returns true in the app process without persisting; M4A fails closed when its
  freeform keys do not persist.
- Make `ReplayGain` nullable/presence-aware. A present 0.00 dB gain uses the with-tag preamp and
  remains visible; missing values use the without-tag preamp.
- Make `MetadataWriter` canonicalize and preserve existing ReplayGain values on ordinary edits,
  with an explicit `replayGainProperties(...)` replacement map.
- Expose a Compose `ReplayGainScanActivity` from the song-selection, album and artist menus. It
  requests read access and `MediaStore.createWriteRequest` (Android 11+), shows
  confirmation/progress/cancel/result with per-file failure title/reason, and keeps the scan
  activity-scoped for this minimal package.
- Add no Gradle dependency, Room schema/migration, version change or translated-string change.

## Consequences

- ReplayGain calculation is offline and independent of the real-time playback pipeline, so it
  can normalize files that are not currently playing.
- Framework codec coverage is smaller than the FFmpeg playback path; ALAC/rare codecs may fail to
  scan and are reported per file. A future change can add `media3-transformer` or a native
  loudness/FFmpeg path without changing the tag/write contract.
- In-place tag rewriting is not transactional; fresh-descriptor read-back verification catches
  silent write failures. WAV stages an app-private persistent backup under
  `noBackupFilesDir/replaygain-wav-backup` and restores the original bytes on any caught failure,
  including an injected mid-write failure. The directory survives process death/reboot and is
  excluded by the platform from cloud and device-to-device backups; it is not a user-facing
  backup feature. A process crash or power loss can still leave a backup for recovery, but
  recovery is per-song on the next write: there is no global startup scan or age/size sweep, so
  stale backups for songs never rewritten can persist. M4A fails explicitly when its freeform
  tags do not persist.
- Recovery copies created by earlier cache-backed builds are not migrated or swept by this
  change. They may remain in `cacheDir` until evicted and are not used by the
  `noBackupFilesDir` path, so a pre-upgrade crash recovery copy is not guaranteed to be found
  after upgrade.
- The current AA-06 codec/writer checkpoint (WAV RIFF ID3v2, M4A freeform, AAC delay handling) is
  source-unvalidated. Final independent Spec/Standards review, a frozen build and device WAV/M4A
  positive/negative gates remain pending; source probes are not acceptance.
- Large scans are CPU/thermal heavy and activity-scoped. A foreground worker is a follow-up if
  scans must survive the activity.
- The active playback processor keeps its current gain until the next media-item transition;
  cache invalidation makes the next read correct. A refresh command can be added later.
- `MetadataWriter` no longer drops canonical ReplayGain tags on ordinary edits, fixing a
  pre-existing data-loss path.
