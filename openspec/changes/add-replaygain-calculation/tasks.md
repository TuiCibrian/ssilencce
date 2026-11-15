# Tasks: ReplayGain calculation (AA-06)

**Device gate:** INCOMPLETE, not PASS. See
`combined-postfix-2026-11-06/device/DEVICE-GATE.md`; the source-only corrections below are
implemented, but a fresh frozen build and device run are still required.

## 1. Model and tag primitives

- [x] 1.1 Make `ReplayGain` gains/peaks nullable so a measured 0.00 dB is present.
- [x] 1.2 Add `ReplayGainTags` with standard, iTunes freeform, R128 and alias canonicalization.
- [x] 1.3 Update `ReplayGainTagExtractor` to shared keys, nullable fields and cache invalidation.
- [x] 1.4 Update `DataExt.replayGainStr` to display a present 0.00 dB value.

## 2. Measurement

- [x] 2.1 Add the Android-free `ReplayGainAnalyzer`/`ReplayGainMath`: K-weighting, 400 ms/100 ms
  blocks, absolute/relative gates, LUFS, peak, gain clamp.
- [x] 2.2 Add `ReplayGainDecoder` with `MediaExtractor`/`MediaCodec`, 8/16-bit and float PCM,
  format-change handling, EOS, idle guard, cancellation and mono/stereo guard.
- [x] 2.3 Add `ReplayGainScanner` album grouping, complete-album check, combined block energy,
  sequential measurement, cancellation and failure reporting.
- [x] 2.4 Ensure partial selections write track tags only and never fabricate album gain.

## 3. Writing and playback

- [x] 3.1 Add `ReplayGainTagWriter`: format-aware keys, replace aliases, preserve non-RG tags,
  read-back verification and MediaStore notify.
- [x] 3.2 Make `MetadataWriter` preserve canonical ReplayGain tags on ordinary edits and accept
  an explicit replacement map.
- [x] 3.3 Update `ReplayGainAudioProcessor` for presence-aware preamp selection and gain clamp.

## 4. UI and wiring

- [x] 4.1 Add Compose `ReplayGainScanActivity` confirmation/progress/cancel/result UI.
- [x] 4.2 Add `ReplayGainScanViewModel` state, prepare/start/cancel and error handling.
- [x] 4.3 Add the calculation action to the selection, song, album and artist menus.
- [x] 4.4 Request read access and `MediaStore.createWriteRequest` on Android 11+; legacy write
  below R.
- [x] 4.5 Register the activity in the manifest and the scanner/viewmodel in Koin.
- [x] 4.6 Add main English strings only.

## 5. Documentation and OpenSpec

- [x] 5.1 Update `FEATURES.md`, `MISSING_FEATURES.md`, `CODE_MAP.md`, `docs/agents/domain.md`
  and the AGENTS.md code map.
- [x] 5.2 Add `ADR-049-replaygain-calculation.md`.
- [x] 5.3 Create this OpenSpec change (proposal, spec, design, tasks).
- [x] 5.4 Mark AA-06's calculation gap as implemented; keep constant-volume conversion,
  use-embedded-tags, skip-or-reload and headroom UI listed as remaining gaps.

## 6. Verification

- [x] 6.1 Pure probe: compile `ReplayGainAnalyzer`/`ReplayGainTags` with Android Studio `kotlinc`
  and run against a 10 s 48 kHz stereo 1 kHz tone plus digital silence.
  - Tone: 97 blocks, peak `0.088379`, integrated `-21.067316 LUFS`, gain `+3.067316 dB`;
    ffmpeg `ebur128` reference `-21.1 LUFS` / peak `-21.1 dBFS`.
  - Silence: 17 blocks, peak `0.0`, LUFS/gain `null`.
  - Tag maps: standard/iTunes/R128 formats and canonical alias selection checked.
- [x] 6.2 Static source probe: **64/64 checks PASS** covering analyzer, decoder, scanner, writer,
  canonical/presence model, MetadataWriter preservation, menus/UI, permissions, manifest/Koin,
  main-only strings, absence of transformer dependency and no Room changes in the new package.
- [x] 6.3 `ktlint check` on all AA-06 source files: **exit 0**. `git diff --check`: **exit 0**
  (only Git LF/CRLF advisory warnings).
- [x] 6.4 `openspec validate add-replaygain-calculation --strict`: **valid**.
- [ ] 6.5 Gradle `:app:assembleGithubDebug` / `:app:lintGithubDebug` **deferred** until AA-05
  source settles and the build lease is granted. LIB-11 is closed; no build was run in this pass.
- [ ] 6.6 Device runtime (permission allow/deny, format matrix, album/partial album, cancellation,
  current-track behavior) deferred behind the same AA-05/lease gate.
- [x] 6.7 LIB-11 and AA-09 evidence untouched: no edits to `add-list-display-info`,
  `add-backup-restore-types` or their verification artifacts.

## 7. Post-review corrections (F1-F5)

- [x] 7.1 Fix alias priority to Standard > iTunes > R128 via `ReplayGainTagResolver`; add the
  conflicting-alias probe.
- [x] 7.2 Treat blank/malformed/non-finite gain and peak as missing; preserve measured 0.00 dB and
  zero peak; add malformed/zero probes.
- [x] 7.3 Suppress new album tags for a complete album if any track write fails: snapshot previous
  album tags, roll back already-tagged tracks, finish remaining tracks with track tags only, and
  report rollback failures.
- [x] 7.4 Preserve completed/failed counts on forced cancellation instead of resetting them.
- [x] 7.5 Enforce peak > 1 in playback by sharing `ReplayGainMath.limitByPeak`.
- [x] 7.6 Extend the host writer corpus with zero-gain and album-write-failure cases; add baseline
  and comparator scripts plus `writer/RUNBOOK.md`; self-test the comparator 25/25.
- [x] 7.7 Run the pure resolver/limiter probe: **36/36 PASS** (`probes/tags/run.log`) and the static
  source probe: **16/16 PASS** (`probes/source-probe.log`).
- [ ] 7.8 Re-run the device writer gate on the next frozen APK. The **4DA13E67** gate was
  executed on emulator-5562/API 28 and remains **INCOMPLETE**: WAV/M4A capability gaps and the
  read-only/write-fail fixtures are not enforceable on API 28 FUSE; see
  combined-aa06fix-aa05preserve-2026-11-07/device/DEVICE-GATE-4DA.md.

## 8. Device-gate source corrections (2026-11-07)

- [x] 8.1 Reopen the saved URI with a fresh read descriptor after a write; WAV fails closed with
  an explicit unsupported-container result and M4A fails closed if its freeform tags do not
  persist. (Historical: the WAV guard was removed by 8.11; the M4A writer now uses standard
  property names that TagLib persists as freeform atoms, per 8.12.)
- [x] 8.2 Reject materially short or partial decodes using the container duration and
  `MediaExtractor`/`MediaCodec` partial-frame flags.
- [x] 8.3 Render each failed song title/reason in the finished result UI and log it under the
  `ReplayGainScan` tag.
- [x] 8.4 Harness: add Opus MediaStore insertion fallback and verify the indexed row.
- [x] 8.5 Harness/fixture: generate the zero-gain source at -18 dBFS so the expected tag is
  `0.00 dB` / `0.1259`.
- [x] 8.6 Harness: cancel only after at least one of five tracks completes.
- [x] 8.7 Harness: map lowercase `expected_rg` keys and parse MP3 `TIPL.people` without aborting
  metadata collection.
- [x] 8.8 Harness: record `failure_details` and filtered `ReplayGainScan` logcat per case.
- [x] 8.9 Source probe: **20/20 PASS**; ktlint **exit 0**; `openspec validate --strict` **valid**;
  comparator self-test **25/25**, and the saved-state recheck reports the expected 5 failures
  (corrupt, M4A, Opus, WAV, zero-gain).
- [ ] 8.10 Fresh frozen build and device gate. AA-06 remains **INCOMPLETE**; do not mark PASS
  until the new candidate records W-01/W-02/W-03/W-12/W-14 and the mid-scan cancellation case.
  The WAV/M4A source capability is implemented in 8.11-8.13 but still requires this fresh
  build/device gate (8.15).
- [x] 8.11 WAV capability (historical): remove the unsupported-container guard and write standard
  `REPLAYGAIN_*` properties through TagLib. The 4DA/8DB device gate showed that TagLib's WAV
  `savePropertyMap` returns true in the app process without persisting; this path is superseded by
  8.16.
- [x] 8.12 M4A capability: write standard property names for MP4/M4A; TagLib persists them as
  `----:com.apple.iTunes:REPLAYGAIN_*` freeform atoms. The previous prefixed-key path returned
  `true` but silently dropped the atoms (the false success seen in the 0C423B6D gate).
- [x] 8.13 Decoder: subtract `MediaFormat.KEY_ENCODER_DELAY`/`KEY_ENCODER_PADDING` from the
  expected frame count so a complete AAC decode with encoder priming is not rejected as short.
  The M4A fixture declares 49,152 samples of delay; 470 AAC frames decode to 432,128 samples,
  which is complete against 431,871 trimmed expected frames.
- [x] 8.14 Source probe and docs: `probes/source-wav-m4a-probe.py` **28/28 PASS**; ktlint
  **exit 0**; OpenSpec spec/design/tasks and `docs/CODE_MAP.md`, `docs/FEATURES.md`,
  `docs/MISSING_FEATURES.md`, `AGENTS.md` updated. No build was run.
- [ ] 8.15 Fresh frozen build and device gate for WAV/M4A (W-01/W-02) plus the full AA-06 matrix;
  AA-06 remains INCOMPLETE until this passes.
- [x] 8.16 WAV persistence fix: add `WavId3v2TagCodec`, an explicit RIFF `ID3 ` chunk reader/writer.
  `ReplayGainTagWriter` now routes WAV through it instead of TagLib `savePropertyMap`. The codec
  preserves `fmt `/`data` and non-ReplayGain ID3 frames, replaces ReplayGain `TXXX` frames, updates
  the RIFF size, shifts later chunks when needed, and the writer verifies through a fresh read
  descriptor. Album snapshot/restore also uses the WAV codec. The writer stages a cache backup and
  the codec restores it on any failure; version-aware v2.3/v2.4 frame flags fail closed; the
  mid-file ID3 shift path is host-fixtured.
- [x] 8.17 WAV host verification: `probes/source-wav-id3v2-probe.py` **46/46 PASS**;
  `probes/host-wav-id3v2/` Kotlin host probe **198/198 PASS**; independent mutagen/ffprobe
  verification **133/133 PASS** for single, complete album, partial album, repeated replacement,
  mid-file ID3 growth/shrink, version-flag rejection, multiple-ID3 rejection and injected mid-write
  restore; ktlint 1.8.0 **exit 0**. No Gradle build or device run. These counts are historical
  pre-`inspectBackup` evidence and are superseded by 8.19-8.21.
- [ ] 8.18 Fresh frozen build and WAV-only device rerun (single, complete album, partial album,
  repeated replacement) plus W-05/W-07/W-08/W-09 regression; AA-06 remains INCOMPLETE until this
  passes.
- [x] 8.19 WAV host probe expansion: backup state machine (stale/committed/missing/size/tag
  mismatch/partial), restore-failure retention, cancellation, success/readback lifecycle, alias
  cleanup and malformed-input fixtures (tag-size, odd pad, trailing garbage, >32 MiB tag,
  >10,000 chunks). Live `4776C915` baseline: **268 PASS / 1 FAIL**; see 8.20.
- [x] 8.20 New defect found by 8.19: `inspectBackup` parsed the marker tag with a line-prefix
  check, but the marker is single-line (`pending size=<n> tag=<uri>`), so the content-identity
  mismatch guard never fired. Live `4776C915` host result is **268/1**, failing test
  `backup tag mismatch discarded`. Do not promote 4776 as final.
- [x] 8.21 TEMP candidate `AF01BF4D` (one-line marker tag regex) is **269/269 PASS** on the Kotlin
  host probe and **191/191 PASS** on the independent mutagen/ffprobe verifier. Static probe is
  **72/72** on both live 4776 and candidate AF01. The candidate is not applied; K/H review and a
  new integration gate are required before live integration.
- [ ] 8.22 Apply `AF01BF4D` after K candidate review and the new integration gate; rerun
  host/static/verifier on the exact live files and freeze the final manifest.
- [ ] 8.23 Independent K Standards + H Spec review of the final exact hashes; exact-APK
  WAV/M4A positive+negative device gate. AA-06 remains INCOMPLETE.
- [ ] 8.24 C-owned MainModule proposal: root selected Option B,
  `noBackupFilesDir.resolve("replaygain-wav-backup")` (stable app-private, excluded from Android
  backup/transfer), exact proposal `DFAD2ECA…` (13,831 B), before `C37CE682…`. MainModule remains
  C-authored/integrated and pending K/H review. The AA-06 backup helper is codec/writer-local, not
  a shared `MetadataWriter` transaction helper.
- [ ] 8.25 H independent Spec review of the final candidate and Option B; host counts are
  author/cross-library evidence, not non-author approval. AA-06 remains INCOMPLETE.


