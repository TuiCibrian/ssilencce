# Documentation delta — audio-cutter

The docs agent should apply the bullets below. The code in this change is the source of truth.

## docs/FEATURES.md

Add under `## 11. Utilities` (after the **Sleep timer** bullet):

- **Audio cutter / trimmer** — Song, playlist-song, now-playing and multi-select menus open a Compose trimmer with a waveform-less start/end range slider, a ±1 s minimum selection, live time labels and clipped ExoPlayer preview playback. Export copies the selected range without re-encoding via `MediaExtractor` + `MediaMuxer` (M4A/MP4 AAC, ADTS AAC, and OGG Opus/Vorbis on Android 11+), while MP3 is copied frame-aligned with its ID3v2 tag preserved; other codecs show a clear unsupported state. The destination is chosen with the Storage Access Framework.
- **Set as ringtone / notification / alarm** — Built on the trimmer: the clip is published to the matching MediaStore audio collection (Android 10+) or the legacy `Ringtones`/`Notifications`/`Alarms` folder (Android 8–9) and set through `RingtoneManager`. `WRITE_SETTINGS` is requested through `ACTION_MANAGE_WRITE_SETTINGS`; the existing default sound is never deleted.

## docs/MISSING_FEATURES.md

Remove these exact bullets from `## 8. File operations`:

- **Audio cutter / trimmer** — trim start/end or extract a portion as a new file.
- **Set as ringtone** (built on the cutter).

No other entries change.

## docs/CODE_MAP.md

Add these entries (same Purpose / Responsibilities / Inputs / Outputs / Dependencies / Consumed by / Related files / Notes format as neighbouring files):

- `data/local/audio/clip/AudioClipFormat.kt` — Audio clip models: `AudioClipFormat` (M4A/OGG/MP3 extension + MIME, muxer format), `AudioClipInspection`, `AudioClipRange` (500 ms engine minimum), `AudioClipFailure` and `AudioClipExportResult`.
  - **Responsibilities:** Map extractor MIME types to lossless output formats; gate OGG on API 30; carry inspection duration and unsupported reasons.
  - **Consumed by:** `AudioClipExporter`, `AudioCutterViewModel`, `AudioCutterScreen`, `RingtoneClipStore`.
  - **Notes:** MediaMuxer format constants are local ints so older API levels never resolve the API 30 OGG constant.
- `data/local/audio/clip/AudioClipNaming.kt` — Builds sanitized clip file names/titles as `<title> (<start>-<end>s).<ext>`.
  - **Consumed by:** `AudioCutterScreen` (SAF suggested name) and `AudioCutterViewModel` (export/ringtone names).
- `data/local/audio/clip/AudioClipExporter.kt` — Lossless range exporter.
  - **Responsibilities:** Probe the first audio track and its MIME/duration; trim AAC/AMR-era containers through `MediaMuxer` with monotonically timestamps and sync-only flags; route MP3 to the raw trimmer; write SAF output through a cache temp file; copy to a file for the legacy ringtone folder.
  - **Inputs:** source content `Uri`, `AudioClipRange`, destination stream/file, `AudioClipFormat`.
  - **Outputs:** `AudioClipExportResult.Success(durationMs, bytes)` or a typed `Failure`.
  - **Dependencies:** `MediaExtractor`, `MediaMuxer`, `Mp3RawTrimmer`, coroutine IO dispatch.
  - **Consumed by:** `AudioCutterViewModel`, `RingtoneClipStore`.
  - **Notes:** No decoding or re-encoding; cancellation deletes the temp/target file.
- `data/local/audio/clip/Mp3RawTrimmer.kt` — Streaming MPEG 1/2/2.5 Layer I–III frame parser and byte-for-byte trimmer.
  - **Responsibilities:** Copy an ID3v2 tag verbatim (including footer), resynchronize over junk, accumulate frame durations to select the range, drop a leading Xing/Info/VBRI metadata frame, handle truncated trailing frames gracefully.
  - **Consumed by:** `AudioClipExporter.trimMp3`.
- `data/local/audio/clip/RingtoneClipStore.kt` — Publishes clips as system sounds and sets them as defaults (`ClipToneKind`, `RingtoneSetResult`).
  - **Responsibilities:** MediaStore insert/publish with `IS_PENDING` and the matching `IS_RINGTONE`/`IS_NOTIFICATION`/`IS_ALARM` flag on API 29+; legacy public-folder write plus `MediaScannerConnection` on API 26–28; `WRITE_SETTINGS` and storage-permission checks; `RingtoneManager.setActualDefaultRingtoneUri`.
  - **Consumed by:** `AudioCutterViewModel`.
- `ui/screen/audiocutter/AudioCutterActivity.kt` — Compose host for the trimmer; requests audio-read (and pre-Q write) permissions before preparing the ViewModel; intent factory takes a song id.
- `ui/screen/audiocutter/AudioCutterViewModel.kt` — Loads the song and format inspection; runs SAF export and ringtone publishing on IO; exposes `AudioCutterUiState` and one-shot `AudioCutterEvent`s; guards against concurrent work.
- `ui/screen/audiocutter/AudioCutterScreen.kt` — Trim UI: range slider, labels, clipped ExoPlayer preview, SAF `CreateDocument` launcher, `WRITE_SETTINGS` launcher/retry, snackbar events and unsupported/loading/missing states.

Update these existing CODE_MAP entries:

- `MainModule.kt` — also registers `AudioClipExporter`, `RingtoneClipStore` and the parameterized `AudioCutterViewModel`.
- `ui/component/menu/MenuItemClickExt.kt` — handles `action_trim_audio` for a single song and for a one-item song list (multi-selection shows a "select a single song" toast).
- `AndroidManifest.xml` — declares `.ui.screen.audiocutter.AudioCutterActivity` and `WRITE_SETTINGS`.

## AGENTS.md

In the condensed Repository Code Map, under `### data/ — Data, repositories & network`, add:

- `data/local/audio/clip/` — Lossless audio range export (`AudioClipExporter`, `Mp3RawTrimmer`), clip naming/format models and `RingtoneClipStore` (MediaStore/legacy sound publishing + `RingtoneManager`).

Under `### ui/screen/ — Screens`, add `audiocutter/` to the list of screen packages (Compose trimmer, preview and system-sound actions).

## docs/agents/domain.md

No change: no new ADR.

## ADR

No new ADR. This is a self-contained utility feature with no change to DI, networking, navigation, persistence or the playback engine.

## Decisions

- **MP3 inside an MP4/M4A container is unsupported** (the extractor can report `audio/mpeg` but the bytes are not a bare MP3 stream). The exporter sniffs the source (`ID3` or MPEG sync) before choosing the raw-copy path and reports the format as unsupported otherwise.
- **OGG trimming requires Android 11 (API 30)** because `MediaMuxer.OutputFormat.MUXER_OUTPUT_OGG` was added there; below that the screen shows an explicit unsupported message instead of re-encoding.
- **No new preference.** The feature is stateless; defaults of existing settings are untouched.
- **Ringtone copies are published, not moved.** SAF exports remain user-owned documents; the set-as actions create a separate copy under `Ringtones|Notifications|Alarms/ssilencce` so the system can read the URI and the user keeps the exported clip.
- **Preview playback deliberately clips the selection** with a dedicated ExoPlayer instance that requests audio focus; no background playback or wakelock is added.
- **MediaStore flags:** published rows set `IS_MUSIC = 0` plus the matching system-sound flag, keeping the copies out of the normal music library.
