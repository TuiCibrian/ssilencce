# Host WAV ID3v2 fixture probe

This is a source-only host test for `WavId3v2TagCodec.kt` and the WAV branch of
`ReplayGainTagWriter.kt`. It compiles the Android-free codec with the project's Kotlin compiler and
runs it against copies of the AA-06 WAV fixtures; no Gradle build and no device are used.

## What it covers

- single-track WAV write (`REPLAYGAIN_TRACK_GAIN`/`_PEAK`);
- WAV with no ID3 chunk (append);
- complete three-track WAV album (`TRACK_*` + `ALBUM_*`, identical album values);
- partial album (track tags only, third track byte-identical);
- repeated replacement (one `TXXX` per field, old value replaced, no duplicates);
- pre-existing album tags preserved by a track-only write;
- album restore preserving track tags and replacing album tags;
- mid-file ID3 chunk before `fmt `/`data` with growth and shrink;
- version-aware ID3 frame flags (v2.3 compression/encryption/grouping; v2.4
  grouping/unsync/data-length) fail closed without changing bytes;
- multiple ID3 chunks fail closed on both read and write without changing bytes;
- tag-size under/over declaration, missing final pad and trailing garbage fail closed;
- a real >32 MiB ID3 tag and >10,000 RIFF chunks fail closed without OOM;
- iTunes-prefixed, R128, lowercase and duplicate TXXX aliases are replaced by exactly one
  standard TXXX per field;
- injected mid-write failure restores the original bytes from the backup;
- restore failure retains the backup and reports its path;
- cancellation restores the original bytes and removes the recovery files;
- backup state machine: stale, committed, missing, size-mismatched, tag-mismatched and partial
  markers are classified without touching the audio file;
- success lifecycle: the backup survives codec success until the caller marks it committed;
- read-back failure primitive restores the retained backup;
- `fmt `/`data` chunk payloads, decoded audio bytes and RIFF size unchanged.

`WavHostProbe.kt` uses the real `WavId3v2TagCodec` source. `verify_wav_probe.py` independently checks
the generated files with mutagen, ffmpeg and a RIFF chunk parser.

## Run

```powershell
# Compile the Android-free codec plus the probe with the Gradle-cached Kotlin compiler.
# Use the same compiler/stdlib/reflect/coroutines/annotations jars as the repo toolchain.
kotlinc -no-stdlib -no-reflect `
  -cp <kotlin-stdlib.jar> `
  -d <out-dir> `
  app/src/main/java/com/ssilencce/data/local/replaygain/ReplayGainTags.kt `
  app/src/main/java/com/ssilencce/data/local/replaygain/WavId3v2TagCodec.kt `
  probes/host-wav-id3v2/WavHostProbe.kt

python probes/host-wav-id3v2/make_edge_fixtures.py `
  --out <fixtures-dir> --source <sine-fixture.wav>

java -cp "<out-dir>;<kotlin-stdlib.jar>" com.ssilencce.data.local.replaygain.WavHostProbeKt `
  <fixtures-dir> <out-dir>

python probes/host-wav-id3v2/verify_wav_probe.py `
  --fixtures <fixtures-dir> --out <out-dir>
```

The fixture directory must contain `rg_wav_track.wav`, `wav_album/01..03.wav` and
`wav_partial/01..03.wav` from the AA-06 writer corpus. Run
`make_edge_fixtures.py --out <fixtures-dir> --source <sine-fixture.wav>` first to generate the
mid-file ID3, multiple-ID3, version-flag, alias, tag-size, odd-pad, trailing-garbage, huge-tag and
many-chunk fixtures.

To test a TEMP candidate codec without touching live Kotlin, compile that file instead of the
`app/src/...` path. The verifier accepts `--fixtures` and `--out`, so probe outputs can stay outside
the repository.

## Result history

### 2026-11-08 (historical, pre-`inspectBackup`)

- Kotlin host probe: **198/198 PASS**.
- Independent mutagen/ffprobe verification: **133/133 PASS**.
- Static source probe: **46/46 PASS**.
- ktlint 1.8.0: **exit 0**.

These counts are historical; they do not cover the later backup state machine, alias cleanup or
malformed-input matrix.

### 2026-11-11 (current host evidence)

- **Live `4776C9155D86CDFE47CCECD102FDD2638774A403BA46EA4A03DD7F3BB662CE07`:
  268 PASS / 1 FAIL.** The failure is `backup tag mismatch discarded`: `inspectBackup` looked for
  a line starting with `tag=` but the marker is a single line, so the content-identity mismatch
  guard never fired. This is the failing baseline; do not promote it.
- **TEMP candidate `AF01BF4D465A026F011134C032E37DDAE683849D2653B41784D42CD8DCB16196`:
  269/269 PASS.** The candidate changes only the marker tag parsing to
  `Regex("(?:^|\\s)tag=(\\S+)")`. It is **not applied** to live source.
- **Independent mutagen/ffprobe verification on the candidate output: 191/191 PASS.** The first
  run was 184/7; all seven were verifier-side (chunk-offset comparison and R128 classification),
  corrected before the final count.
- **Static source probe: 72/72 PASS on live `4776C915` and 72/72 PASS on the candidate.** The
  static probe does not exercise the marker tag parsing; the Kotlin host probe does.

No Gradle build, APK, or device run was made. The candidate is author/TEMP evidence only; K/H
review and the exact-APK gate remain open.
