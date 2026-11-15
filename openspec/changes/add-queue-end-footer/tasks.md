# Tasks: Queue end footer (follow-up A)

## 1. Ended state and actions

- [x] 1.1 Add pure `QueueEndPolicy` (ended only with items, footer visibility, replay mapping).
- [x] 1.2 Add `isQueueEndedFlow` to PlayerViewModel, driven by `STATE_ENDED` + item count and
  reset on play/seek/timeline/media-item/empty.
- [x] 1.3 Add dedicated `replayQueue()` using logical queue position 0 and preserving
  shuffle/repeat.

## 2. Queue UI

- [x] 2.1 Add the ended footer to `QueueScreen` only when ended and non-empty.
- [x] 2.2 Footer Clear uses `QueueClearingBehavior.RemoveAllSongs`; existing overflow Clear keeps
  the user preference.
- [x] 2.3 Add main English footer strings.

## 3. Documentation and OpenSpec

- [x] 3.1 Update `FEATURES`, `MISSING_FEATURES` and `CODE_MAP`.
- [x] 3.2 Create this OpenSpec change.

## 4. Verification

- [x] 4.1 Run the pure `QueueEndPolicy` probe (27 checks), including shuffle-aware displayed
  current restart, out-of-range positive mappings/fallbacks and empty-timeline clamping.
- [x] 4.2 Run the static source probe (60 checks), including row-tap-at-ended with bounded mapping,
  shuffle-aware generic Play/toggle restart, no-op next/forward guards and event-driven
  `playMediaId` reset.
- [x] 4.3 Run ktlint and `git diff --check`.
- [x] 4.4 Run `openspec validate add-queue-end-footer --strict`.
- [x] 4.5 JDK 21 `:app:assembleGithubDebug :app:lintGithubDebug`: BUILD SUCCESSFUL, 0 lint
  errors. Final universal APK
  `1AD61A2BEA7ECFE04F8501422D97414165B5067DA87F442D87D6FE199666C024` (53,433,156 bytes,
  mtime `2026-11-03T01:16:26Z`), copied to `artifact/` with `SHA256.txt`.
- [x] 4.6 Device runtime on qa26 / `emulator-5556` (2026-11-02): **PASSED** on 1AD6:
  repeat/shuffle, auto-clear on/off, empty close, footer Replay/Clear, row-tap-at-ended,
  generic Play (shuffle OFF/ON) and no-op seek checks.
  **1AD6 runtime 2026-11-02 on qa26/`emulator-5556`: PASSED.** Footer shown only when ended and
  non-empty; header Play/toggle restarted the displayed current with shuffle OFF (Beta `2 / 2`,
  active item id `1`) and ON (Alpha `2 / 2`, active item id `0`); non-current row tap restarted
  the mapped row; no-op Next/Forward left the footer; effective seek hid it; Replay started the
  first item; Clear removed all items; auto-clear off/on passed; repeat/shuffle preserved. The
  empty sheet staying open is pre-existing 7F9B behavior.
  Evidence: `1AD6-PASS.md` and `runtime/1AD6-PASS/`.
  `emulator-5554` was untouched; qa26/`emulator-5556` was cleaned and shut down.
  Earlier-candidate history (superseded): C4DB exposed the reset defects that 1AD6 now covers;
  the E8BC generic-Play "failure" was a mistap on a scrolled queue row (header Play at y=182,
  not y=419) and is retracted. Diagnosis:
  `C:/Users/geode/AppData/Local/Temp/opencode/ssilencce-verify/queue-end-footer-2026-11-02/GENERIC-PLAY-DIAGNOSIS.md`.
