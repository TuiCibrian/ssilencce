# Tasks: Queue re-shuffle (follow-up C)

## 1. Command and policy

- [x] 1.1 Add pure `QueueReshufflePolicy.canReshuffle(shuffleEnabled, mediaItemCount)`.
- [x] 1.2 Add the app-internal `Playback.RESHUFFLE_QUEUE` command and register it in
  `APP_INTERNAL_COMMANDS`.
- [x] 1.3 Add the service handler: guard shuffle-off/empty, build a fresh
  `ImprovedShuffleOrder` with the current index, new seed and current length, assign only the
  shuffle order, then call `persistentStorage.saveState()` as the only persistence call. The
  standard save path may refresh the active named queue's resume metadata; its item list/order and
  active binding remain unchanged.

## 2. ViewModel and UI

- [x] 2.1 Add `PlayerViewModel.reshuffleQueue()` with empty/shuffle-off guards, command dispatch,
  and success/invalid/failure messages through `queueMessage`.
- [x] 2.2 Add the re-shuffle item to both queue overflow menus only, hidden for an empty queue and
  disabled while shuffle is off.
- [x] 2.3 Add main English strings for the action, shuffle-off, empty, done and failure feedback.

## 3. Documentation and OpenSpec

- [x] 3.1 Update `FEATURES`, `MISSING_FEATURES` and `CODE_MAP`.
- [x] 3.2 Create this OpenSpec change.

## 4. Verification

- [x] 4.1 Run the pure `QueueReshufflePolicy` probe (7 checks).
- [x] 4.2 Run the compiled `ImprovedShuffleOrder` probe (17 checks): current index first, full
  permutation, no wrap, deterministic seed and fresh-seed difference.
- [x] 4.3 Run the static source probe (78 checks): command/registration, guard, fresh seeded order,
  `saveState()` as the only persistence call, no toggle/seek/timeline/repeat or explicit
  named-queue/snapshot writes, the documented standard `updateResumeState` refresh, ViewModel
  messages, both overflow menus, main-only strings and unchanged dependencies/schema.
- [x] 4.4 Run ktlint and `git diff --check`.
- [x] 4.5 Run `openspec validate add-queue-reshuffle --strict`.
- [x] 4.6 JDK 21 `:app:assembleGithubDebug :app:lintGithubDebug`: BUILD SUCCESSFUL, 0 lint errors
  (1,031 warnings; 5 are the expected `MissingTranslation` warnings for the new main-only strings).
  Final universal APK
  `9462EE9110A5E5CEDF1F5FFB453EB3C8E5F918B5062125BD3640F6835786C68E` (53,439,788 bytes, mtime
  `2026-11-03T02:09:09.7336723Z`), copied to `artifact/` with `SHA256.txt`.
- [x] 4.7 Device runtime on qa26 / `emulator-5556` (2026-11-02): **PASSED** on 9462.
  Four-item queue, shuffle on, repeat all: re-shuffle while paused retained `Queue Beta` at
  16,945 ms exactly and changed the order `[2,1,3,0]` -> `[1,2,3,0]`; re-shuffle while playing
  retained `Queue Beta` and advanced it 6,223 ms -> 11,717 ms while the order changed
  `[0,3,2,1]` -> `[1,2,0,3]` (current was last before, had a next after). Queue IDs/timeline
  order `[901,902,904,903]`, the active named binding and named item order stayed unchanged;
  shuffle stayed on and repeat stayed all. Force-stop/relaunch restored the new order and current
  position. Shuffle off showed the action greyed/disabled and a tap changed nothing; the empty
  queue menu omitted the action. Standard (swipe-up queue) and Aurora surfaces both passed;
  footer remained present after a natural end (revealed by scrolling the 4-item list), named-chain
  controls remained, and no multi-drag control appeared. `emulator-5554` was untouched;
  qa26/`emulator-5556` was cleaned and shut down.
  Evidence: `9462-PASS.md`, `runtime/9462-PASS/` and `runtime/evidence/`.

## 5. Spec-review follow-up (docs/spec/probe only)

- [x] 5.1 Amend spec/design/proposal/`FEATURES`/`CODE_MAP` to scope named-snapshot preservation to
  the item list/order and active binding, and explicitly allow the standard `saveState()` path to
  refresh the active named queue's resume metadata (`resume_index`, `resume_position_ms`,
  `repeat_mode`, `shuffle_enabled`, `updated_at`) as existing ADR-041/045 behavior.
- [x] 5.2 Extend the static source probe to 78 checks: `saveState()` is the only persistence call,
  no explicit `NamedQueueDao`/`PlaybackStateWriter`/snapshot/direct-preference write exists in the
  re-shuffle path, and the standard `updateResumeState` branch is documented.
- [x] 5.3 Re-freeze: `freeze/SOURCE_MANIFEST.csv` records the current repo source/docs (including
  `FEATURES`, `CODE_MAP`, proposal/design/spec/tasks) plus the 78-check source probe and the pure
  and compiled probe sources/runners, with SHA-256/size/mtime entries. `freeze/PROBE_MANIFEST.csv`
  retains the evidence-note hashes. No app source or build change.
- [x] 5.4 Verify the freeze: all 21 manifest entries match their current files; the latest app-source
  mtime (`strings.xml`, `2026-11-03T02:03:36Z`; latest Kotlin `02:03:29Z`) is older than the
  candidate APK mtime `2026-11-03T02:09:09Z`; APK SHA-256/size are unchanged.
