# emulator-qa bug briefs

Device: `emulator-5554`, AVD `ssilencce-qa26`, API 26, app
`com.ssilencce.debug` 1.4.0-beta.3.debug (versionCode 1400103),
APK SHA-256 `E5FC0E1B…D01A`.

---

## BUG-1 — Extra-format scanner indexes the same AIFF twice under two path forms

**Severity:** medium (duplicate songs, duplicated counts/search/shuffle entries)
**Feature:** extra-formats (Library > Index extra audio formats)
**Status on build:** reproducible, not a crash.

### Fixed

Confirmed in the integration worktree: `ExtraFormatScanner` now canonicalizes discovered paths
(`File.canonicalPath`, resolving `/sdcard` and `/storage/emulated/0` to the same storage root)
before computing the stable negative id, and dedupes by canonical path plus size and mtime.
Persisted `files/extra_formats/library.json` entries are canonicalized and deduped on load and
on the next update, so existing users self-heal without re-enabling the scan.

Verified: `:app:compileGithubDebugKotlin`, `:app:compileFdroidDebugKotlin` and
`:app:lintGithubDebug` all pass.

### Reproduction

1. Install the build and place one AIFF file at
   `/sdcard/Music/QA/Formats/qa.aiff`.
2. Settings > Library > Library maintenance > **Index extra audio formats** =
   ON.
3. Observe logcat: `I/ExtraFormatScanner: Indexed 2 extra-format file(s)` for a
   single physical file.
4. Inspect `files/extra_formats/library.json` via
   `adb shell run-as com.ssilencce.debug cat files/extra_formats/library.json`.
5. Open Songs: the AIFF appears twice as a `qa` row in addition to the real
   `qa.wav` (three `qa`-titled rows are visible in the list).

### Observed

`library.json` contains two tracks for the same file:

- id `-3141894815413120335`, path `/sdcard/Music/QA/Formats/qa.aiff`
- id `-678212332508487914`, path `/storage/emulated/0/Music/QA/Formats/qa.aiff`

Both have `durationMs=20000`, same size/mtime. The two stable negative ids mean
the library treats them as two different songs. Tags are not read for this
AIFF (`title="qa"`, empty artist/album), which may be a separate format
limitation.

### Expected

One extra-format entry per physical file. Canonicalize/dedupe the path before
indexing (e.g. resolve `/sdcard` and `/storage/emulated/0` to the same storage
volume, or dedupe by canonical path/file identity) and keep one stable id.

### Evidence

- `evidence/222-library-extra-switch.png` (switch ON, one AIFF in Formats)
- logcat `ExtraFormatScanner: Indexed 2 extra-format file(s)`
- `library.json` contents (quoted above)
- `evidence/227-songs-aiff2.xml` / `.png` (duplicate `qa` rows)
- Playback of the duplicate row works: metadata `qa, Unknown Album, null`,
  state=3 (AIFF path is playable; the duplication is the defect).

---

## Observations that are NOT filed as product bugs

- **Quick list search keeps focus/IME across list screens.** After using the
  quick-search field once, returning to a list can re-show the IME and the
  floating search bar/FAB can cover lower rows. Back dismisses it and normal
  navigation still works; this is a UX annoyance/harness complication, not a
  blocked user action. No bug filed.
- **`adb shell cmd media_session dispatch pause` crashes the app** with
  `IllegalArgumentException: packageName should be nonempty` in Media3's legacy
  `MediaSessionCompat` controller info. A normal media key
  (`input keyevent 127`) pauses cleanly, so this is a shell/framework path, not
  reproduced as app behavior. Stack preserved in `evidence/13-home-menu.xml`.
- **WAV batch text write** is honestly reported `Unsupported by this file`
  (1 failed, no false success). **WMA** batch write fails and restores the
  original (`The write failed and the original file was restored`). These are
  format limitations reported correctly, not silent corruption.
- **Host emulator instability**: API 30/36 qemu `0xc0000005` APPCRASH after
  2-10 minutes; API 26 software-rendered session is stable. Environmental.
