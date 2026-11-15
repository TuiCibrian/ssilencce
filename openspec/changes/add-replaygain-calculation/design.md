# Design: ReplayGain calculation

## Context

The read-only AA-06 audit (`replaygain-calc-2026-11-06/AUDIT.md`) confirmed:

- `ReplayGainTagExtractor` reads standard, iTunes and Opus R128 tags; `ReplayGainAudioProcessor`
  applies album/track gain, preamp and peak limiting.
- No PCM decoder, loudness analyzer or calculation UI exists.
- `MetadataWriter` filters `REPLAYGAIN_*` before TagLib `savePropertyMap`; the TagLib wrapper
  calls `FileRef::setProperties`, so the filtered map replaces the tag map and drops existing
  standard ReplayGain tags.
- The bundled Jellyfin `FfmpegAudioDecoder` is package-private and no `media3-transformer`
  dependency exists. Framework `MediaExtractor`/`MediaCodec` is the smallest no-dependency path.

The current model uses `0f` and `1f` sentinels for missing gain/peak, so a measured 0.00 dB
gain is indistinguishable from no tag.

## Goals / Non-Goals

**Goals:**

- Offline BS.1770-4 / EBU R128 track and album measurement.
- Format-aware, verified tag writing without duplicates.
- Correct 0 dB presence semantics in playback.
- A small Compose calculation flow from the existing song selection menu.
- No new Gradle dependency, Room schema, version or translated strings.

**Non-Goals:**

- "Convert ReplayGain to constant volume", use-embedded-tags toggle, skip-or-reload, or a
  user-facing headroom control.
- True-peak oversampling.
- ALAC/FFmpeg-only decode parity in this slice; unsupported files fail explicitly.
- Background/foreground worker execution; the activity owns the scan for this minimal package.
- Adding test source sets (project rule 10).

## Decisions

### Analyzer

`ReplayGainAnalyzer` is Android-free. It computes K-weighting from the ITU coefficients for the
actual sample rate:

- high-shelf pre-filter (1681.974450955533 Hz, +3.999843853973347 dB, Q 0.7071752369554196);
- RLB high-pass (38.13547087602444 Hz, Q 0.5003270373238773);
- per-frame channel-weighted energy, a circular `blockSize = 0.4 * sampleRate` window with
  `hop = 0.1 * sampleRate`;
- linear sample peak before K-weighting.

`ReplayGainMath.integratedLufs` applies the absolute -70 LUFS gate, the relative -10 LU gate
and `-0.691 + 10 log10(mean square)`. Track gain is `-18 - LUFS`; album gain is
`-18 - album LUFS` from concatenated block energies. Gain is peak-limited and clamped to
`-24..+24 dB`. Empty/silent block sets return null and are treated as per-file failures.

Only mono/stereo are accepted initially. More channels fail explicitly rather than producing a
wrong downmix.

### Decoder

`ReplayGainDecoder` uses `MediaExtractor` to select the first audio track and `MediaCodec` to
decode to PCM. It handles `INFO_OUTPUT_FORMAT_CHANGED` once, 8/16-bit and float PCM, EOS and
resource release. A format change after the first configured output or an unsupported encoding
fails the file. Decoded frames are compared with the container's declared duration (250 ms
tolerance) after subtracting `MediaFormat.KEY_ENCODER_DELAY`/`KEY_ENCODER_PADDING`; MediaCodec
trims AAC priming/padding, so the raw container duration would reject a complete decode. Partial
extractor/codec frames fail the file, so a truncated file cannot be measured as a shorter valid
track. This intentionally does not reuse the package-private FFmpeg
decoder; ALAC files that only play through the FFmpeg extension are reported as unsupported.

### Scanner and album semantics

`ReplayGainScanner` de-duplicates selected songs and groups them by `albumId`. For each group it
loads `albumById`; album tags are computed only when the album's song-ID set exactly equals the
selected group's set and all tracks measured successfully. Mixed selections therefore write
track tags for partial albums and complete album tags only for fully selected albums. There is no
silent expansion of the selection and no partial-album gain. Cancellation propagates through
`ensureActive()` and never writes album tags from an incomplete set.

### Tag writer

`ReplayGainTagWriter` opens the existing `content://` URI with `ContentResolver.openFileDescriptor`
in `rw` mode, reads `TagLib.getMetadata`, removes existing ReplayGain/R128 keys, adds the new
format-specific keys, and saves with `TagLib.savePropertyMap` for non-WAV containers. WAV takes a
separate branch: duplicated read/write descriptors feed `WavId3v2TagCodec`, which edits the RIFF
`ID3 ` chunk in place. The writer stages a verified backup, keeps it through its own read-back
verification, and restores on verification failure; restore failure retains the backup and reports
its path. Both branches close the write descriptor and reopen a fresh read descriptor before
verifying the persisted map. Non-ReplayGain tags are preserved. Missing album values on a track-only
scan preserve existing album tags. Verification requires exactly one alias per ReplayGain field and
compares numeric gain/peak values with tolerance and R128 integer values exactly. A container that
still cannot persist the tags reports an explicit failure instead of success.

TagLib maps standard property names to `----:com.apple.iTunes:REPLAYGAIN_*` freeform atoms when
saving MP4/M4A. Passing the prefixed keys directly returns true but silently drops the atoms, so
the writer deliberately writes the standard names for MP4. WAV is handled by
`WavId3v2TagCodec`, an explicit RIFF `ID3 ` chunk reader/writer. TagLib's WAV
`savePropertyMap` returns true in the app process without persisting, so the codec preserves
`fmt `/`data` and every non-ReplayGain ID3 frame, replaces the ReplayGain `TXXX` frames, updates
the RIFF size, shifts later chunks when the tag size changes, and reads the result back through a
fresh descriptor. The writer stages a verified backup, keeps it through read-back verification, and
restores on verification failure; restore failure retains the backup. Version-aware v2.3/v2.4 frame
flags fail closed; malformed tag sizes, missing final pad, trailing bytes, multiple ID3 chunks and
oversized tags/chunk tables fail closed; iTunes/R128/case aliases are removed on write. A mid-file
ID3 chunk before `fmt `/`data` is host-fixtured for growth and shrink. The unsupported-container
guard remains gone.

Format selection is based on the decoder MIME with an extension fallback:

- `audio/opus`/`.opus` → `R128_TRACK_GAIN`, `R128_ALBUM_GAIN`, standard peak keys;
- MP4 MIME or `.m4a`/`.mp4`/`.m4b` → standard property names (TagLib emits iTunes freeform atoms);
- WAV MIME or `.wav`/`.wave` → explicit RIFF ID3v2 `TXXX` codec (TagLib's WAV save is not used);
- everything else → standard uppercase keys.

### WAV backup lifecycle and crash limits

- Backup path: `replaygain-wav-<song.id>.bak` under the injected temp directory; a per-song lock
  serializes WAV writes inside the process.
- Commit: stream the original to a unique temp sibling, `force(true)`, require the byte count to
  match, then atomic rename (fallback replace). A partial or zero `.bak` cannot become
  restore-eligible.
- Marker: `pending size=<n> tag=<uri>` after the backup commit and before the first mutation;
  `committed` after read-back verification. `inspectBackup` classifies
  `NONE`/`RESTORE`/`DISCARD`/`REFUSE` for missing, stale, committed, tag-mismatched,
  size-mismatched and partial markers.
- Recovery: restore only a pending, size-matching, same-content-identity backup; remove the marker
  first, then the backup; retain both on restore failure and report the path. A committed or
  marker-free backup is never restored.
- Read-back: the codec leaves the backup in place; the writer marks it committed only after its
  fresh-descriptor verification, otherwise it restores the original.
- Cancellation: checkpoints in backup copy, shift and parse loops; restore before rethrowing
  `CancellationException`.
- Limits: there is no atomic content-URI replacement. A process kill after `force(true)` but before
  the committed marker can revert that one write. The root-selected proposal is
  `noBackupFilesDir.resolve("replaygain-wav-backup")` (Option B), a stable app-private directory
  excluded from Android backup/transfer; it is pending C-owned `MainModule` integration and K/H
  review. Until it lands, `cacheDir` remains evictable. Serialization is process-local.
- Directory choice: Option B avoids adding a device-transfer exclusion for transient recovery files
  and does not add a user-facing backup feature. Parent-directory fsync is not performed, so
  power-loss guarantees remain qualified.
- The helper is local to `WavId3v2TagCodec`/`ReplayGainTagWriter`; it is not a shared
  `MetadataWriter` transaction helper. Future batch/metadata writes need their own leased design.

### Presence-aware model and writer preservation

`ReplayGain` fields become nullable (`Float?`); `ReplayGain.Empty` has all nulls. The extractor
only sets keys that exist, the processor uses the with-tag preamp when a gain is present (even
0.00 dB), and `DataExt.replayGainStr` displays a present 0.00 dB value.

`MetadataWriter` canonicalizes existing ReplayGain keys by suffix, removes duplicate aliases,
and preserves one canonical value per suffix on ordinary edits. An explicit
`replayGainProperties(...)` map replaces the preserved values for a calculation write. This keeps
the existing duplicate workaround while avoiding data loss.

### UI and permissions

A Compose `ReplayGainScanActivity` receives selected song IDs. It uses `AbsBaseActivity` for the
read/legacy write permission and requests `MediaStore.createWriteRequest` for the selected URIs
on Android 11+. The ViewModel loads songs, runs the scanner, publishes `Loading/Ready/Running/
Finished/Error` state, and cancels on activity teardown. The screen shows the selection count, a
note that album gain requires a complete album, a progress bar/current title, a cancel action and
a written/failed summary that lists each failed song title and reason.

The menu action is added to the shared multi-select menu and handled by `List<Song>.onSongsMenu`;
album/artist selection already falls back to the same handler. A single-song menu entry is added
for direct access. Only the main English string file is edited.

### Playback refresh limitation

The scanner invalidates `ReplayGainTagExtractor` cache entries. The active `ReplayGainProcessor`
continues using its current gain until the next media-item transition; this is documented in
`FEATURES`/the change because the minimal package does not add a new session command.

## Risks / Trade-offs

- Framework codec coverage is smaller than the FFmpeg playback path; ALAC and rare codecs may
  fail to scan. The failure is explicit and per-file.
- In-place TagLib rewriting is not transactional; verification catches silent write failures but
  cannot roll back a damaged file. Test on copies first.
- K-weighting coefficient/gating errors produce plausible but wrong gains; validate against
  ffmpeg/libebur128 reference vectors.
- MP4/iTunes, WAV/ID3v2 and Opus key mapping is format-specific; the read-back check and format
  matrix are mandatory. TagLib silently drops prefixed MP4 freeform keys, so the writer must use
  standard names for MP4.
- A scan can be CPU/thermal heavy; it is sequential, cancellable and activity-scoped.
- Current-track gain is not refreshed until the next transition.

## Verification

- Pure probe for `ReplayGainMath`/`ReplayGainAnalyzer` (known block energies, gating, peak,
  clamp) and no Android imports.
- Static source probe for the analyzer/decoder/scanner/writer, presence-aware model, menu/UI,
  permissions, MetadataWriter preservation, manifest/Koin wiring, main-only strings, no new
  dependency/schema/version and no translated-string edits.
- `probes/source-wav-m4a-probe.py` for the WAV/MP4 capability, AAC encoder-delay handling and
  fail-closed read-back: **28/28 PASS**; device TagLib persistence evidence in
  `replaygain-validation-2026-11-06/probes/` and `lib02-5562/taglib-probe/`.
- `probes/source-wav-id3v2-probe.py` for the explicit WAV RIFF `ID3 ` path: **72/72 PASS** on live
  `4776C915` and **72/72 PASS** on the TEMP candidate `AF01BF4D`.
- `probes/host-wav-id3v2/` host fixture test: live `4776C915` **268 PASS / 1 FAIL** (new defect
  `backup tag mismatch discarded`); TEMP candidate `AF01BF4D` **269/269 PASS**; independent
  mutagen/ffprobe verification on the candidate output **191/191 PASS** for
  single/complete/partial/replacement, mid-file ID3 growth/shrink, version-flag rejection,
  multiple-ID3 rejection, alias cleanup, malformed-input rejection, injected mid-write restore,
  restore-failure retention, cancellation and backup state transitions. The candidate is not
  applied; K/H review and a new integration gate are required.
- The 2026-11-08 **198/198**, **133/133** and **46/46** counts are historical pre-`inspectBackup`
  evidence and are not current.
- No Gradle build in this pass; the WAV fix remains source-only until the next frozen build and
  device gate.
- `openspec validate add-replaygain-calculation --strict`, ktlint and `git diff --check`.
- No Gradle build in this pass; runtime/build verification is deferred until LIB-11 runtime
  closes and the build lease is granted.
