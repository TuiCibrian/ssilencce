# Proposal: ReplayGain calculation (AA-06)

## Why

ssilencce reads and applies ReplayGain tags, but it cannot measure audio or write
ReplayGain tags. Musicolet exposes read *and calculate*; the current app therefore cannot
normalize a library that has no embedded tags, and ordinary tag edits can drop the
standard ReplayGain tags a calculation feature would produce.

## What Changes

- Add an offline EBU R128 / ITU-R BS.1770-4 analyzer: 400 ms blocks, 100 ms hop, absolute
  and relative gating, integrated LUFS and linear peak.
- Add a `MediaExtractor` + `MediaCodec` PCM decoder (16-bit/float, format-change, EOS and
  encoder delay/padding handling) with no new Gradle dependency.
- Add a scanner that groups selected songs by album and computes album gain from combined
  gated block energy only when the complete album is selected. Partial selections write
  track tags only.
- Add a format-aware writer: standard `REPLAYGAIN_*` for MP3/FLAC/OGG, an explicit RIFF
  `ID3 ` chunk codec for WAV, iTunes `----:com.apple.iTunes:REPLAYGAIN_*` for MP4/M4A, and
  Opus `R128_*` gains. Overwrite the calculated fields and verify them by reading back the
  saved map through a fresh descriptor.
- Make the ReplayGain model presence-aware so a measured 0.00 dB tag is not treated as a
  missing tag, and update the extractor/processor/display paths.
- Make `MetadataWriter` preserve canonical ReplayGain tags on ordinary edits and support
  an explicit ReplayGain map for calculation writes.
- Add a Compose confirmation/progress/result activity, a song-selection menu action, and
  main English strings. Request `MediaStore.createWriteRequest` on Android 11+ and the
  legacy write permission below R.
- Add OpenSpec, ADR, feature/gap/code-map documentation. No Room schema, dependency,
  version or translated-string change; no build until the LIB-11 lease is granted.

## Capabilities

### New Capabilities

- `replaygain-calculation`: offline measurement, album grouping, tag persistence and the
  user-facing ReplayGain calculation flow.

### Modified Capabilities

- None. Existing ReplayGain playback behavior is extended, not redefined.

## Impact

- New: `data/local/replaygain/` analyzer, decoder, scanner, tag writer/constants, WAV
  ID3v2 RIFF codec; `ui/screen/replaygain/` activity and ViewModel.
- Modified: ReplayGain model/extractor/processor, `MetadataWriter`, song menu handling,
  manifest, Koin, main strings, `FEATURES`, `MISSING_FEATURES`, `CODE_MAP`, ADR-049,
  `docs/agents/domain.md`.
- Unchanged: playback queue/transport, Equalizer engines, Room schema, dependencies,
  version number, translated strings, `docs/MUSICOLET_FEATURES.md`.
