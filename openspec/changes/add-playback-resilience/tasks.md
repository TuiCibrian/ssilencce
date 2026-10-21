# Tasks: Playback resilience and focus controls

> Status 2026-10-21: the F source/helper repair passes 23 checks and gate 5 passed; current-APK Ask rotation/repeated-play runtime acceptance remains open.

## 1. Quick Settings tile (C)

- [x] 1.1 Add a lifecycle-bounded `TileService` that connects to the existing media session and reflects playing, paused and unavailable states.
- [x] 1.2 Toggle play/pause on click and update the tile from player events without polling.
- [x] 1.3 Add the manifest service entry and English labels; verify the tile appears and tracks playback on the emulator.

## 2. Per-track remembered position (E)

- [x] 2.1 Add `TrackPositionEntity`/DAO and Room v7 to v8 migration; register in `MainModule`.
- [x] 2.2 Add the opt-in preference and apply stored positions on track start, excluding the restored queue item.
- [x] 2.3 Persist positions on pause/transition/destroy with bounded cadence; clear on completion and near-end seeks.
- [ ] 2.4 Verify long-track resume, completion reset, toggle-off behavior and migration on the emulator.

## 3. Audio focus modes (F)

- [x] 3.1 Add the focus mode preference with normal, ignore, ask and resume-after-call values.
- [x] 3.2 Implement a single `AudioFocusController` and wire play/pause/focus events; keep the existing ignore switch behavior compatible.
- [x] 3.3 Implement ask confirmation UI and paused-when-unavailable behavior.
- [ ] 3.4 Verify transient loss/gain, ducking, call-style interruption, ask mode and service destroy cleanup.

## 4. Session recovery (K)

- [x] 4.1 Persist whether playback was active and playWhenReady with the existing playback state.
- [x] 4.2 Restore and optionally resume once after a system service/process restart; guard against loops and never claim force-stop support.
- [x] 4.3 Add the opt-in setting and documentation that force-stop/OS kills are outside the guarantee.
- [ ] 4.4 Verify service restart recovery on the emulator with the feature on and off.

## 5. Integration and verification

- [x] 5.1 Add strings in `values/strings.xml` only; update CODE_MAP, FEATURES, MISSING_FEATURES, ADR-011/012 and the new resilience ADR.
- [x] 5.2 Run ktlint on edited Kotlin files, then `:app:assembleGithubDebug` and `:app:lintGithubDebug` on JDK 21.
- [ ] 5.3 Emulator-check tile state, per-track resume, focus modes and restart recovery; record exact evidence.
