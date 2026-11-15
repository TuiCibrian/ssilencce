# Tasks - merge sleep-timer-plus

- [x] Applied `sleep-timer-plus.patch` with `git apply --3way` on top of the green six-brief checkpoint; only `App.kt` and `PlaybackService.kt` needed manual conflict resolution.
- [x] `App.kt`: treated the import conflict as additive, keeping `AppVisibility` plus the existing `Preferences` import, and registered `AppVisibility` in `Application.onCreate`.
- [x] `PlaybackService`: removed the patch's `ValueAnimator` fade helpers (`launchMusicFadeOut`, `cancelSleepTimerFadeOut`, `restorePlayerVolume`) instead of reintroducing a second volume ramp.
- [x] `PlaybackService`: routed sleep-timer fade completion through `PlaybackFadeController.fadeOut` and `AdvancedForwardingPlayer.pauseWithoutFade`, followed by the foreground-only quit policy; non-fade completion uses `stopPlaybackForSleepTimer`.
- [x] Composed `pauseAtEndOfMediaItems` from the queue stop position and the sleep-timer stop-at-end state; all writers (queue remap, set-stop-position, media-item transition, sleep-timer listener, end-of-media reset) now update the combined flag.
- [x] Preserved `RepeatPolicy` (including `LoadNextAndPause`): automatic media-item transitions still feed the sleep-timer song counter, and the repeat policy keeps its own pause/seek-at-end behavior.
- [x] Kept `PlaybackVolumeController` as the single owner of output volume, including equalizer volume, audio-focus ducking and fade multipliers.
- [x] Kept the already-integrated lyrics, artwork, A-B/per-song tempo, queue actions, library data and playback-transport code unchanged.
- [x] Added the foreground-only stop preference (`sleep_timer_stop_music_only_in_foreground`, default off), its playback-settings switch and English strings only.
- [x] Added `SleepTimerHistory.kt`, song-count mode, timer history and the Compose sheet additions; no Room, dependency, version, test or translated-resource changes.
- [x] Compile gate green (`powershell -NoProfile -ExecutionPolicy Bypass -File .orchestra\build-gate.ps1`).
- [x] Self-review of the merge diff; no debug code, unrelated edits or stray conflict markers.
- [x] Integration corrections and doc updates documented in `DOCS_DELTA.md`.
