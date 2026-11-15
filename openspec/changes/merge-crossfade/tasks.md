# Tasks - merge crossfade

- [x] Applied `crossfade.patch` with `git apply --3way` on top of the green 21-brief checkpoint; only `AdvancedForwardingPlayer.kt` and `PlaybackService.kt` needed manual conflict resolution.
- [x] `AdvancedForwardingPlayer`: combined the additive `onSeekToNextRequested` / `onSeekToPreviousRequested` crossfade hooks with the existing mute/preview `canStartPlayback` / `onPlaybackBlocked` guards; neither side was dropped.
- [x] `PlaybackService`: kept the open-intents `ResumeAutoplayPolicy`, mute deferral and preview guards, and the `PlaybackTileService`/audio-focus wiring; crossfade cancel is composed into `pausePlayback()` and the pause fade callback.
- [x] `PlaybackService`: preserved the merged transition handler (play counts/scrobbles, notification artwork policy, per-song playback params, A-B reset, repeat policy, track-position restore, media-button layout) and mapped a crossfade commit seek to an automatic transition.
- [x] Preserved the sleep-timer shared fade/stop policy: crossfade is cancelled before the shared fade ramp and on stop-at-end arm; disarm refreshes scheduling.
- [x] Preserved default-off crossfade and suppression for A-B repeat, queue stop-after-song, sleep-timer stop-at-end and repeat-load-next-and-pause; repeat-one is inherently excluded because the next index equals the current index.
- [x] Kept the gapless-album opt-out and `GaplessGateAudioSink` behavior; crossfade respects the existing gapless preference.
- [x] Kept all processors (balance, mono, ReplayGain) mirrored into the secondary player and the shared audio-session EQ/system-engine strategy (audio session id forwarded and kept in sync).
- [x] Kept A-B/per-song speed: playback-parameter changes abort an in-flight crossfade, and the A-B range still suppresses scheduling.
- [x] Added `CrossfadePlayer`, `CrossfadeController`, `GaplessAlbumDetector`, `MediaItemExtras`, the Compose `CrossfadeSettingsCard`, preference XML, drawable and English strings only; no Room, dependency, version, test or translated-resource changes.
- [x] Compile gate green (`powershell -NoProfile -ExecutionPolicy Bypass -File .orchestra\build-gate.ps1`), exit 0.
- [x] Single self-review of the merge diff; no conflict markers, debug code or unrelated edits.
- [x] Integration corrections and doc updates documented in `DOCS_DELTA.md`.
