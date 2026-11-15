# Tasks: Headset button mapping

> Status 2026-11-02: complete on final candidate `4F7F91311955F013F4551A8224AC2B64EFA534BD895AB138BE67CF9A5E44F305` (53,409,948 B, `2026-11-02 12:20:24`); JDK21 build/lint, compiled probe and API26 bounded runtime PASS. Hardware long-press injection was system-intercepted; the controller probe covers the long-press branch.

## 1. Pure model and policy

- [x] 1.1 Add transport-only `HeadsetButtonAction` enum.
- [x] 1.2 Add pure `HeadsetButtonConfig`, `HeadsetButtonPolicy` delay/count/immediate/guard rules.
- [x] 1.3 Add pure `HeadsetButtonController` for press count, repeat long-press latch and scheduled dispatch.
- [x] 1.4 Add focused compiled policy/controller probe and record PASS.

## 2. Service and preferences

- [x] 2.1 Replace the fixed service count runnable with the controller and action executor.
- [x] 2.2 Preserve defaults, dedicated Next/Previous/FF/REW fall-through and safe player-not-initialized fallback.
- [x] 2.3 Reset pending click/controller on headset preference changes and destroy; stop playback-state changes from cancelling pending clicks.
- [x] 2.4 Add six Preferences keys/getters/setters, normalization and reset-to-defaults.

## 3. Settings UI and strings

- [x] 3.1 Add Compose mapping dialog with four action rows, 4+ switch, delay chips, disclaimer, reset and close.
- [x] 3.2 Add Playback → Headphones XML row and PreferencesScreenFragment routing/summary updates.
- [x] 3.3 Add only main English strings.
- [x] 3.4 Update FEATURES/MISSING_FEATURES/CODE_MAP with compatibility caveats and remaining dedicated-key gap.
- [x] 3.5 Add this OpenSpec change and validate strictly.

## 4. Verification

- [x] 4.1 Run ktlint format/check and `git diff --check` (PASS).
- [x] 4.2 Run `openspec validate add-headset-button-mapping --strict` (PASS).
- [x] 4.3 Run compiled pure-policy/controller probe (PASS: defaults, counts, 4+, long-press, delay normalization).
- [x] 4.4 Build `:app:assembleGithubDebug :app:lintGithubDebug` on JDK 21 with `--no-daemon` (PASS, 3m 59s).
- [x] 4.5 API26 bounded runtime: default single/double/triple, remap, delay, 4+ on/off, dedicated Next/Prev, settings summary/reset — PASS. `input keyevent --longpress 85` was intercepted by the system Assistant, so hardware long-press injection could not reach the app; the compiled controller probe covers distinct long-press and no-repeat fallback.
- [x] 4.6 Record APK/hash/installed match and evidence; clean qa26 only, preserve `emulator-5554`.
