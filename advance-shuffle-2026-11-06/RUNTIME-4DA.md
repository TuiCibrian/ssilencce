# AA-05 targeted runtime retest — candidate 4DA13E67

Candidate: **4DA13E67CA0CB7E9B387FC1656A644750B69F89D552B6668C7738F157D2ADCCC**, 53,479,204 B.
Device: **emulator-5560 only**, `ssilencce-qa30`, API 30. 5554/5556/5562 untouched.

## Identity — PASS

| Check | Result |
|---|---|
| Host APK SHA-256 | `4DA13E67CA0CB7E9B387FC1656A644750B69F89D552B6668C7738F157D2ADCCC` |
| Host APK size | 53,479,204 B |
| Installed APK pulled via `pm path` | **exact match** `4DA13E67CA0CB7E9B387FC1656A644750B69F89D552B6668C7738F157D2ADCCC` |
| Pulled size | 53,479,204 B |

Evidence: `evidence-4da/installed-base-4da.apk`.

## Source-delta proof — explicit

The 0C42 source manifest (`ssilencce-verify/.../CURRENT-SOURCE-MANIFEST.csv`) lists the shuffle files. Current 4DA source hashes match it exactly for the strategy/algorithm files:

| File | Manifest (0C42) | Current (4DA) |
|---|---|---|
| `playback/shuffle/ShuffleManager.kt` | `2FD8AEB8…A60973` | `2FD8AEB8…A60973` |
| `core/model/shuffle/ShuffleGroupKeyPolicy.kt` | `B1A3ED0E…391B05` | `B1A3ED0E…391B05` |
| `core/model/shuffle/SpecialShuffleMode.kt` | `FA75DA35…9804EA` | `FA75DA35…9804EA` |
| `core/model/shuffle/ShuffleModeAvailabilityPolicy.kt` | `655E3338…91620F` | `655E3338…91620F` |

Therefore the 0C42 statistical/API-26 evidence remains applicable to 4DA: the algorithms, group keys and composer gate are byte-identical.

The delta under test is `PlayerViewModel.kt` (0C42 `628ff79f…`, 4DA `795ac020…`). The new `openSpecialShuffle` path captures `currentMediaId`, `currentPosition`, `wasPlaying`, finds the current media ID in the new list, calls `setMediaItems(mediaItems, currentIndex, currentPosition)`, and restores `playWhenReady = wasPlaying`. That is the change the runtime below exercises.

## Targeted runtime setup

21 fixtures were installed on 5560: the 20 original AA05 fixtures plus `AA05 Long Position` (30 s) to make position measurement unambiguous. MediaStore returned 21 rows; the app library showed **21 songs**. App data was cleared before the run, then the exact 4DA APK was installed. Test-only minimum-duration setting: 1 s.

## Failed case retest — **FAIL**

Test while **playing**:

| Observation | Before switch | After `Most played album artists` |
|---|---|---|
| Play state | `state=3` (playing) | `state=3` (playing) |
| Current item (metadata) | `AA05 Long Position` | `AA05 B NoComposer Control 02` |
| Position | **2,921 ms** | **0 ms** |
| Queue length | 21 | 21 |
| Queue unique | — | **true** (21 distinct IDs) |

- **Same current media item:** **FAIL** — `AA05 Long Position` became `AA05 B NoComposer Control 02`.
- **Position preserved:** **FAIL** — 2,921 ms → 0 ms.
- **Play/pause state preserved:** **PASS** — playing before and after.
- **Every eligible song exactly once:** **PASS** — 21 unique IDs in the post-switch queue.
- **No crash:** **PASS** — process alive (`pidof` returned 4403); logcat has no `FATAL EXCEPTION` or `ANR in` for the app.

The post-switch queue has the long fixture at index 5 and `AA05 B NoComposer Control 02` at index 6, i.e. playback landed on the item immediately after the preserved item, at position 0. This is consistent with the start-index/position being lost while shuffle is enabled; it is not a UI-capture artifact (the media-session metadata changed).

A **paused** retest was attempted: both snapshots reported `state=7` (`Source error`) with the same `AA05 Long Position` metadata and position 0. It is therefore **INCONCLUSIVE**, not a PASS. The source error was a playback error, not an app crash; the process stayed alive.

Evidence: `evidence-4da/ms_before_play_switch.txt`, `ms_after_play_switch.txt`, `preservation_playing.json`, `db/before_play.db`, `db/after_play.db`, `after_play_switch.png`, `logcat.txt`, `ms_before_paused_switch.txt`, `ms_after_paused_switch.txt`, `preservation_paused.json`.

## Reused 0C42 evidence

Because the strategy files are byte-identical (hashes above), the following 0C42 results remain valid and are not re-claimed as new 4DA runtime:

- Album-artist heavy/fallback and composer/unknown-group statistics.
- 60/60 full 20-song permutation checks.
- API 26 composer-hidden sheet check.

The 4DA-specific runtime claim is limited to the targeted preservation retest above.

## Verdict

**Targeted AA-05 retest: FAIL.** The new code preserves play/pause state and the queue remains a full unique set, but the current media item and its position are still not preserved when switching special shuffle modes while playing. Paused-state preservation is INCONCLUSIVE due to a source error.

## Cleanup

After the run, only emulator-5560 was cleaned: app data cleared/uninstalled, `/sdcard/Music/AA05` removed, temporary prefs removed, emulator stopped. 5554/5556/5562 were not touched. No app source or build was edited.
