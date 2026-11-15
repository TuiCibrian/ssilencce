# emulator-qa report — final integrated build

Date: 2026-11-14
Scope: final `_integration` tree after all 28 feature briefs, compatibility fixes and docs-sync;
combined `assembleGithubDebug`/`lintGithubDebug` gate already green
(`orchestra/logs/final-gate.log`, `BUILD SUCCESSFUL`).

## Verdict

**DONE emulator-qa.** The emulator-verifiable run is complete. One user-visible
product bug (extra-formats duplicate indexing) is filed in `bugs.md`; several
named-gate cases are genuinely unavailable on the selected API 26 host or are
API 30+ only, and are listed explicitly below instead of being counted as
passes. No physical-device, Chromecast-receiver, external-display, Android Auto
or release-keystore gate was claimed.

## Device and app identity

| Item | Value |
| --- | --- |
| AVD / serial | `ssilencce-qa26` / `emulator-5554` |
| System | Android 8.0, API 26, x86_64 `google_apis`; 540x960 override, density 240 |
| Test user | user 0; `com.ssilencce.debug` (debuggable) |
| App | `1.4.0-beta.3.debug`, versionCode 1400103, minSdk 26, targetSdk 36 |
| APK under test | `app/build/outputs/apk/github/debug/ssilencce-1.4.0-beta.3.debug-github-x86_64.apk` |
| APK SHA-256 | `E5FC0E1B1F81F180F16E1463FC71E2AE56298A1E98432FCDB019E1331F48D01A` |
| Universal SHA-256 | `72F502C80D7529902A3AAF6F64FA9DF7FF52076012227A5D868D29FD343C7EB0` |
| Install | in-place upgrade 2026-11-14 16:16, no data clear |
| Emulator | 37.1.11, `-no-window -no-snapshot -no-boot-anim -gpu off -no-audio`, persistent background process |

### Data preservation

- Full app-data tars before install:
  `backup/ssilencce-data-backup.tar` (release app), `backup/ssilencce-debug-data-backup.tar`
  (API 36 debug data), `backup/qa30-ssilencce-debug-backup.tar` (API 30 debug data).
- Release package `com.ssilencce` was not installed over or modified.
- API 26 debug app was a fresh QA instance; its data was not cleared. Test
  files were pushed under `/sdcard/Music/QA`; `bugreports`/media fixtures live
  under `runtime/verify/results`.
- Screenshots, UI dumps, logcat slices and pulled media under
  `runtime/verify/results/evidence` and `runtime/verify/results/backup`.

## Host instability (environmental, not an app crash)

API 36 (`ssilencce-mcp`) and API 30 (`ssilencce-qa30`) sessions repeatedly lost
`qemu-system-x86_64-headless.exe` with Windows APPCRASH `0xc0000005`
(WER bucket `2078776181096351838`) after 2–10 minutes under the app's rendering
load. This occurred with `swiftshader_indirect`, `guest` and `off` GPU modes.
API 26 at 540x960 with software rendering remained stable for the full run.
All failures were host process crashes; no app FATAL/ANR accompanied them.

**Concrete human action if API 30/36 positives must be exercised:** retry on a
host/emulator combination that does not hit this qemu crash (update emulator
37.1.11 or use a stable WHPX host / physical device) with the same API 30 AVD
and this APK hash. Do not treat the API 26 results as API 30 passes.

## Named OpenSpec gates

### advance-shuffle 8.6 — PASS

- Before: Long Track paused at `position=20712 ms`, active item id 4.
- Action: long-pressed Shuffle, chose **Most played**.
- After: `state=3` playing, `position=0`, active item id 0, metadata `MD One`;
  queue rebuilt to 13 weighted items (DB pull).
- No historical Source error observed. 7.5 remains superseded by the
  2026-11-10 decision and was not executed.
- Evidence: `evidence/41-shuffle-sheet.*`, `42-special-shuffle-after.png`,
  `backup/music_database.db` query in the run log.
- Task 8.6 ticked with this evidence.

### queue-multi-drag 5.6/5.9 retest — PASS

- API 30 session: long-press selected 2 queue rows, dragged the row-0 handle
  downward; `QueueEntity` order changed `[24,25,26] -> [26,24,25]` and the
  selection cleared. The original defect was “order unchanged, 2 selected
  remained”; neither symptom reproduced.
- Evidence: `evidence/24-selection2.*`, `25-after-drag-row0.*`.
- API 26 long-press injection was unreliable; the behavior is API-independent
  and the integrated-build retest is recorded in task 5.15.

### queues-library-category 4.8 — build/freeze PASS; category presence verified

- Frozen final-gate APKs: x86_64 `E5FC…D01A`, universal `72F5…7EB0`.
- Runtime: `Queues` appears in **Appearance > Library categories** as a
  selectable category (`evidence/255..257`); the Aurora drawer does not list it
  (`evidence/05/07`). The five-slot classic navigation was not reconfigured in
  this pass, so the Queues browser itself was not opened.
- Task 4.8 ticked with the frozen hashes.

### ReplayGain device gate — PASS for all feasible API 26 cases; two cases genuinely unavailable

| Case | Result | Evidence |
| --- | --- | --- |
| MP3 complete album | PASS — album + track gain/peak written and read back | `rg-alpha-one/two.mp3` |
| FLAC/M4A/OGG complete album | PASS — album + track tags (OGG Vorbis comments) | `rg2-qa.flac/m4a/ogg` |
| M4A partial selection | PASS — track-only tags, no fabricated album gain | `rg-qa.m4a` |
| WAV single complete album | PASS — RIFF `ID3 ` chunk with `REPLAYGAIN_*` | `rg-qa.wav` + chunk scan |
| Corrupt + valid album | PASS — 1 updated, 1 failed (`Decoded 703008 of 1763999 frames`); corrupt file untouched | `149-rg-corrupt-result` |
| Measured zero gain | PASS — `-0.00 dB` tag persisted, peak `0.177948` | `zero-gain3.mp3` |
| Cancellation mid-scan | PASS — 0/4 progress, Cancel → “0 songs were updated”; first track has no tags | `157/158`, `cancel-1.mp3` |
| Read-only / write-fail | **UNAVAILABLE on API 26**: `chmod` on /sdcard returns EPERM (FUSE); revoking WRITE raises the app permission gate before write | `165-rg-writefail`, chmod output |
| Permission allow | PASS (READ/WRITE granted, all writes above worked) | runs above |
| APE/Opus ReplayGain | Not run — Opus/WMA were used for the tag matrix; APE decoder is unavailable by design | — |

No false success, no corruption and no album-tag fabrication observed. The
remaining cases are not counted as passes.

### Batch tag editor format matrix — PASS where supported; unsupported paths reported honestly

| Format | Result | External verification |
| --- | --- | --- |
| MP3 | PASS — Genre written on both files, ReplayGain preserved, undo restored | `mp3batch-*`, `mp3undo-a1.mp3` |
| FLAC | PASS — Genre + undo | `batch-qa.flac` / `undo-qa.flac` |
| M4A | PASS — Genre + undo | `batch-qa.m4a` |
| OGG | PASS — Vorbis `GENRE=...` + undo | `batch-qa.ogg`, strings |
| Opus | PASS — Genre in Vorbis comment + undo | `matrix-qa.opus` |
| WAV | **UNSUPPORTED (honest)** — `Genre: Unsupported by this file`; 1 failed, no false success | `172-wav-result` |
| WMA | **FAILED + RESTORED (honest)** — 4 updated/1 failed, “write failed and original restored”, `Genre: Not attempted` | `184-5format-result`, `matrix-qa.wma` |
| APE / DSF / DFF | Not available — no valid fixtures/encoders on this host; APE/WMA decoding is documented unavailable | — |

Album/artist routing into the batch editor was verified (album overflow and
artist overflow both open “N songs selected”). Session undo restored mixed
formats (“4 files restored, 0 failed”), and WMA failure left the original intact.

### Widget accessibility 6.x — PARTIAL

- All three providers registered:
  `SsilencceGlanceWidgetReceiver`, `CardWidgetReceiver`, `FullWidgetReceiver`.
- Card widget added to the launcher through the real widget picker; configuration
  activity showed General theme / Album art / Wallpaper / Transparency and saved.
- Empty state rendered: `Nothing playing`, `Open ssilencce`.
- Playing state: the Card widget exposes `Card`, `Album Art`, `Previous`,
  `Play/Pause` and `Next` accessibility nodes. At density 240 (1.5x), each
  clickable control parent is 72x72 px = **48 dp**; the 36x36 px ImageViews carry
  the labels. Re-measured at `font_scale 2.0` with the same 48 dp clickable
  bounds and labels; restored to 1.0. Evidence: `260-widget-playing.*`,
  `262-widget-font2.*`.
- Not verified: the exact size matrix (220x48…360x305.8), TalkBack focus/
  announcement/activation (6.4), dark no-artwork contrast and the
  transparency matrix (6.5), wallpaper derivation/fallback (6.6),
  per-instance independence/reboot/delete (6.7), Card/Full empty pages (6.8),
  API 26/27 `reconfigurable` (6.9), and the full action inventory (6.10).
  These require a dedicated widget-host/launcher-resize + TalkBack pass and
  are not claimed.

### Extra-formats smoke — PARTIAL; **BUG-1 filed**

- `<Library> Index extra audio formats` ON; logcat
  `ExtraFormatScanner: Indexed 2 extra-format file(s)` for one AIFF.
- AIFF plays from the synthetic extra-format id (`state=3`, metadata `qa`,
  active id 16).
- **BUG-1:** the same AIFF is indexed twice under `/sdcard/...` and
  `/storage/emulated/0/...` with different negative ids; Songs shows duplicate
  `qa` rows. Full reproduction and evidence are in `bugs.md`.

## 28-brief coverage matrix

Legend: **PASS** = runtime behavior verified; **PARTIAL** = representative UI/behavior
or one branch verified, remaining branches listed; **BLOCKED** = no receiver/display/Auto
host; **N/A-API** = feature needs API 30+ or an unavailable fixture on this host.

| # | Brief | Status | Runtime evidence / remaining gap |
| --- | --- | --- | --- |
| 1 | playback-transport | PASS | Playback settings: previous-button behavior, FF/RW amounts, long-audio-only, fade in/out, gapless; playback/notification working. Fade ramp timing not measured. `228..231` |
| 2 | queue-actions | PARTIAL | Song menu: Pause after this song, Play after current song, Load next and pause; auto-delete oldest queue and shuffle settings. Repeat-load-next-and-pause fourth state and named-queue merge not exercised. `191`, `229`, `199` |
| 3 | ab-repeat-speed | PARTIAL | A-B menu action set A and showed `A 0:20 · set B` chip; a valid A<B loop and per-song speed apply/reset were not exercised. `192` |
| 4 | lyrics-tools | PARTIAL | Full lyrics screen opens; “No lyrics found” for the AIFF/no-tag test track. Selection, per-song source and sync editor not exercised with a lyric-bearing song. `196` |
| 5 | nowplaying-extras | PARTIAL | Player menu: Open fullscreen art, Seek to…; song menu Share; display options. Keep-screen-on/quick-access and the art viewer actions were not exercised. `191`, `252` |
| 6 | library-data | PARTIAL | Sort keys “Last played date”, “Most played”, “Least played” present (`253`); album-artist fallback setting present (`217`). Multi-disc headers **N/A-API**: API 26 MediaStore has no `disc_number` column (`SQLiteException: no such column: disc_number`), so all songs are disc 1. Genre duration-filter and search-history runtime not exercised. |
| 7 | sleep-timer-plus | PASS | Duration/End at/Songs tabs, 3-songs preset, Stop after this song, foreground-only pref, fade-out. `193/194` |
| 8 | shuffle-plus | PASS/PARTIAL | Randomize cycle persisted (`off → shuffle → randomize`), smart modes sheet, shuffle-behavior settings. CAB “Shuffle and then…” not exercised. `41`, `229` |
| 9 | notification-plus | PASS | Playing notification with 4 actions; notification button/compact arrangement, Replace Close with Add-to-playlist and lock-screen artwork settings. Favorite/add-to-playlist button toggles not individually exercised. `230/231` |
| 10 | folders-plus | PARTIAL | Folder actions menu observed (Folder info, Rename folder, Blacklist directory); folder info/create/rename/exclude/move/copy on the stable API 26 session and remove-missing-songs were not exercised. `26-shuffle-sheet.xml` (API 36), `210` |
| 11 | sound-plus | PARTIAL | EQ screen exposes ReplayGain modes, Reverb and effects; mono toggle/custom reverb parameters not exercised. `233/234` |
| 12 | system-ux | PASS/PARTIAL | Advanced: Show unimportant settings, Battery & background, Ghost mode; Appearance: accent color, disable album art. OEM-specific battery intents not exercised. `209/210`, `254` |
| 13 | setup-wizard | PASS | First-run welcome → folders → scanning → ready; Rescan now → “Scan finished.”; Finish → Home 13 songs. `27..32` |
| 14 | bookmarks-notes | PASS | Add bookmark created `/sdcard/Android/data/com.ssilencce.debug/files/notes/1725 - Beta One.txt` with `0:20.035` under `[Bookmarks]`. Notes editor/share not exercised. `195`, file pull |
| 15 | audio-cutter | PASS | Trimmer opened for MP3: “copied without re-encoding”, start/end/length, preview, Export clip. Actual exported file not written. `200` |
| 16 | tag-editor-finish | PASS | Batch editor from album/artist; MP3/FLAC/M4A/OGG/Opus writes verified; session undo restored; WMA failure restored original. `102..103`, `118..119`, `184..185` |
| 17 | queue-drag-fix | PASS | Row-0 downward block changed order and cleared selection (API 30). `24/25` |
| 18 | open-intents | PASS | VIEW intent chooser → Mini player “Playing from another app”, seek, Open in app. Preview menu present; resume/mute prefs present in Playback settings. `241/242`, `235` |
| 19 | auto-browse | BLOCKED | No Android Auto host/emulator in this environment; not runtime-tested. |
| 20 | menu-customization | PASS | Appearance > Menu actions > Song menu with selectable/reorderable actions. `237` |
| 21 | eq-bands | PARTIAL | Legacy engine exposes fixed 5 bands with the documented disabled explanation; DSP 3/5/10/15/31 switch, engine incl. System EQ and per-cast EQ not exercised. `233` |
| 22 | crossfade | PARTIAL | Gapless/crossfade settings and notification behavior present; crossfade duration/manual-skip/actual overlap not exercised. `230/231` |
| 23 | cast-chromecast | BLOCKED | Cast icon present in player; no Cast receiver/device available. |
| 24 | screencast | BLOCKED | “Screencast to display” menu present (`191`); no external display/Miracast target. |
| 25 | lockscreen-player | PASS | Settings screen, Preview activity, cover/title/artist/album, 0:20, Artist/Folder/Favorites shortcuts. `206..208` |
| 26 | ghost-mode | PASS/PARTIAL | Ghost mode settings (folders, PIN, open search) and guarded Ghost search “Ghost mode is not set up” screen. Actual folder-hide filter/count/shuffle exclusion not exercised. `211/212` |
| 27 | extra-formats | PARTIAL + BUG-1 | AIFF indexed and played; duplicate path indexing bug filed. DSF/DFF/APE unavailable; APE/WMA playback documented unplayable. `222..227`, `bugs.md` |
| 28 | notification-styles | PASS | Selected Big with upcoming songs; notification now uses custom RemoteViews `contentView=0x7f0d032b` / `bigContentView=0x7f0d032a`, 4 actions, playing. `232`, dumpsys |

## Bugs

1. **BUG-1 (filed, medium): extra-format scanner indexes one AIFF twice.**
   `library.json` holds `/sdcard/.../qa.aiff` and `/storage/emulated/0/.../qa.aiff`
   with different stable negative ids; the Songs list shows duplicate `qa` rows.
   Full reproduction in `bugs.md`. No product code was changed during QA.

### Observations, not bugs

- **Quick list search focus/IME** persists across list screens; the floating
  search bar and shuffle FAB can cover lower rows. Back dismisses the IME and
  normal navigation still works; no user action was proven blocked. Recorded as
  a harness/UX observation only.
- **`cmd media_session dispatch pause`** (a shell harness command) crashes the
  app with `IllegalArgumentException: packageName should be nonempty` in
  Media3's legacy `MediaSessionCompat` controller-info path. Normal
  `input keyevent 127` paused cleanly; classified as harness/framework-only.
  Stack: `evidence/13-home-menu.xml`.
- WAV text writes and WMA writes fail honestly and restore the original; not
  silent corruption.
- Host qemu APPCRASH is environmental (see above).

## Remaining gates and exact human actions

### Emulator-verifiable but host-blocked

1. **API 30/36-only positive branches** (composer category/most-played composer
   shuffle, MediaStore `disc_number` multi-disc headers, scoped-storage
   createWriteRequest flows, API 30+ notification behavior): retry this exact
   APK (`E5FC…D01A`) on a stable API 30 emulator after updating/switching the
   emulator host; the current host crashes qemu (`0xc0000005`).
2. **Widget accessibility 6.1–6.10**: a dedicated widget-host/launcher automation
   + TalkBack pass on a stable emulator for the exact size matrix, dark/
   transparency/wallpaper/per-instance/reboot/reconfigurable cases. The Card
   widget binds, configures, exposes 48 dp labelled controls at 1.0 and 2.0 font
   scale, but the full matrix is not claimed.
3. **Queues browser**: reconfigure the five navigation slots (or use a stable
   API 30 UI) to select `Queues` and open the saved-queue browser; the category
   is present in the picker and Aurora-excluded, but the browser itself was not
   opened.
4. **ReplayGain read-only/write-fail**: needs a storage/provider where a file
   can be made non-writable (API 26 FUSE returns EPERM for chmod; revoking WRITE
   triggers the app permission gate). Use a device with enforceable read-only
   media or a fail-injection provider.
5. **Batch APE/DSF/DFF**: supply valid fixtures (APE encoder/sample; DSF/DFF
   files) and rerun the batch matrix; APE/WMA decoding remains documented
   unplayable by design.
6. **Physical/receiver gates**: Chromecast receiver, Miracast/external display,
   Android Auto, Bluetooth/headset/OEM battery/auto-start behavior, and the
   production release keystore gate are unavailable here and are not claimed.

## Evidence index

- `runtime/verify/results/evidence/` — screenshots, UI XML dumps, logcat slices
  (numbered 01–259 by run step), emulator launch logs.
- `runtime/verify/results/backup/` — app-data tars, installed APK, pulled media
  with ReplayGain/tag verification.
- `runtime/verify/results/fixtures/QA/` — generated MP3/FLAC/M4A/OGG/Opus/WAV/
  AIFF/WMA, zero-gain, corrupt, cancel and multi-disc fixtures.
- `runtime/verify/results/qa-progress.md` — incremental monitoring notes.
- `runtime/verify/results/bugs.md` — BUG-1 reproduction and observations.
