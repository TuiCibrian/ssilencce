# Tasks: Sleep timer exact end time

> Status 2026-11-02: complete and verified on the final candidate `C955BEE04034134D2E05872BF59A171AC6D78B2FAC998939DA5ADD97E8E6258F` (53,384,876 B, `2026-11-02 10:56:54`).

## 1. End-time calculation and persistence

- [x] 1.1 Add pure `SleepTimerEndTime` delay/format/next-full-hour helper with strict-after, seconds and midnight rules.
- [x] 1.2 Add DataStore keys for last confirmed mode/hour/minute without changing the existing store.
- [x] 1.3 Persist confirmed mode/hour/minute only after successful exact-alarm scheduling.
- [x] 1.4 Resolve the next full hour from local date/time with zone rules and a strictly-future check; reuse one default pair; do not overwrite end-at values on Duration starts.

## 2. Compose UI

- [x] 2.1 Add Duration/End at mode controls with `selectableGroup` semantics to the existing Compose sleep-timer sheet.
- [x] 2.2 Add a Compose Material 3 24-hour time input dialog; no platform TimePickerDialog.
- [x] 2.3 Show selected/ends-at time, countdown, disabled controls, cancel and default next-full-hour behavior.
- [x] 2.4 Add only main English strings.

## 3. Documentation and OpenSpec

- [x] 3.1 Add this OpenSpec change and validate strictly.
- [x] 3.2 Update FEATURES, MISSING_FEATURES and CODE_MAP.
- [x] 3.3 Document DST gap/overlap and next-full-hour overlap acceptance; add API26 end-at pause/cancel acceptance requirements.

## 4. Verification

- [x] 4.1 Run ktlint format/check (PASS).
- [x] 4.2 Run git diff --check (PASS).
- [x] 4.3 Build `:app:assembleGithubDebug :app:lintGithubDebug` on JDK 21 with `--no-daemon` (PASS, 6m 23s).
- [x] 4.4 Compile/run the helper probe against the actual compiled class (PASS, including `01:00`/`01:30` fall-back -> `02:00-05:00`).
- [x] 4.5 API26 end-at pause (`state=2` after target `11:07`), cancel (playback continued past `11:11`, no live alarm) and CSV unchanged-code smoke (84 B, exact header/row) — PASS.
- [x] 4.6 Record APK hash/size (`C955BEE0…`, 53,384,876 B) and clean qa26 only (`emulator-5554` untouched).
