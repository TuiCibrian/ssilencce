# Tasks: Playback resilience and focus controls

> Status 2026-10-23 batch 2: 18/18. Gate-7 accepts per-track resume (2.4), session recovery
> enabled/disabled (4.4), tile no-queue, and GSM Normal/ResumeAfterCall. Batch 2 closes 3.4/5.3
> with real runtime ducking and service-destroy cleanup on `emulator-5554` (gate-8 APK
> `0BD1A064...`): the platform ducked `LOSS_TRANSIENT_CAN_DUCK` by -14 dB while playback stayed
> PLAYING and restored the 0.5 user volume; the app callback was not delivered
> (`notified:false`), so the app-side `restoreVolume` path was not exercised. Service destroy via
> supported `am stopservice` removed the service/session/focus/notification while the process
> stayed alive and the final state/track position were persisted. Force-stop recovery remains
> outside the guarantee. Earlier-artifact E/F/K/tile evidence remains attributable because those
> source paths are unchanged.

## 1. Quick Settings tile (C)

- [x] 1.1 Add a lifecycle-bounded `TileService` that connects to the existing media session and reflects playing, paused and unavailable states.
- [x] 1.2 Toggle play/pause on click and update the tile from player events without polling.
- [x] 1.3 Add the manifest service entry and English labels; verify the tile appears and tracks playback on the emulator.

## 2. Per-track remembered position (E)

- [x] 2.1 Add `TrackPositionEntity`/DAO and Room v7 to v8 migration; register in `MainModule`.
- [x] 2.2 Add the opt-in preference and apply stored positions on track start, excluding the restored queue item.
- [x] 2.3 Persist positions on pause/transition/destroy with bounded cadence; clear on completion and near-end seeks.
- [x] 2.4 Verify long-track resume, completion reset, toggle-off behavior and migration on the emulator.
  - Long-track pause/resume, seek-then-skip and completion reset passed on the earlier E artifact; `TrackPositionWriter` and the E paths are unchanged.
  - Toggle-off passed on gate-5 (`a-resume-toggle-off.txt` / `a-resume-confirm.txt`).
  - Room v7 to v8 migration and `user_version=8` passed on the earlier runtime; the schema is unchanged.

## 3. Audio focus modes (F)

- [x] 3.1 Add the focus mode preference with normal, ignore, ask and resume-after-call values.
- [x] 3.2 Implement a single `AudioFocusController` and wire play/pause/focus events; keep the existing ignore switch behavior compatible.
- [x] 3.3 Implement ask confirmation UI and paused-when-unavailable behavior.
- [x] 3.4 Verify transient loss/gain, ducking, call-style interruption, ask mode and service destroy cleanup.
  - Transient loss/gain and Ask mode passed on the accepted gate-6 artifact.
  - GSM call-style Normal and ResumeAfterCall passed on gate-7 (`runtime/verify/results/GATE7-RESULTS.md`) with exact focus codes and paused/playing states recorded.
  - Batch-2 ducking runtime PASS (`runtime/verify/results/BATCH2-RESILIENCE-RESULTS.md`): helper
    requested `AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK`; focus stack showed ssilencce
    `loss=LOSS_TRANSIENT_CAN_DUCK, notified=false`, media stayed **PLAYING**, audio_flinger
    `VS dB=-14` during the duck and `VS=0 / L,R=-6 dB` (0.5 user volume) after. The platform
    applies ducking on API 26+; the app's own `restoreVolume` branch was not notified and is not
    claimed as exercised. The earlier helper/source-only checks are not counted as passes.
  - Batch-2 service-destroy cleanup PASS: after Home unbound the controller, supported
    `am stopservice` removed the PlaybackService record, media session, audio-focus owner and
    media notification; PID stayed alive; `was_playing=true` and position 188,584 ms were
    persisted, and a later relaunch with recovery enabled resumed at 191,642 ms. Not force-stop.
  - No source change was required; the gate-8 APK is the artifact.

## 4. Session recovery (K)

- [x] 4.1 Persist whether playback was active and playWhenReady with the existing playback state.
- [x] 4.2 Restore and optionally resume once after a system service/process restart; guard against loops and never claim force-stop support.
- [x] 4.3 Add the opt-in setting and documentation that force-stop/OS kills are outside the guarantee.
- [x] 4.4 Verify service restart recovery on the emulator with the feature on and off.
  - Recovery enabled, including pause-before-kill, passed on the earlier K artifact; the K source paths are unchanged.
  - Recovery disabled passed on gate-7: PID 7376 was killed without force-stop, relaunch PID 8029 restored the queue and stayed PAUSED at 457 ms (`g7b-recovery-*`).
  - Force-stop recovery remains outside the guarantee and is not claimed.

## 5. Integration and verification

- [x] 5.1 Add strings in `values/strings.xml` only; update CODE_MAP, FEATURES, MISSING_FEATURES, ADR-011/012 and the new resilience ADR.
- [x] 5.2 Run ktlint on edited Kotlin files, then `:app:assembleGithubDebug` and `:app:lintGithubDebug` on JDK 21.
- [x] 5.3 Emulator-check tile state, per-track resume, focus modes and restart recovery; record exact evidence.
  - Tile playing/paused/Ask and no-queue unavailable passed on gate-5 and gate-7.
  - Per-track resume and restart recovery enabled/disabled passed via 2.4/4.4 evidence.
  - Batch-2 adds real ducking and service-destroy cleanup PASS
    (`runtime/verify/results/BATCH2-RESILIENCE-RESULTS.md`), including the exact
    `notified=false` platform-duck limitation and the supported `am stopservice` lifecycle.
    Force-stop recovery remains outside the guarantee and is not claimed.
