# Tasks - merge lockscreen-player

- [x] Applied `lockscreen-player.patch` with `git apply --3way` on top of the green 23-brief checkpoint; `AndroidManifest.xml`, `PlaybackService.kt`, `Preferences.kt` and `styles.xml` needed manual resolution.
- [x] Manifest: kept both `MiniPlayerActivity` (with its VIEW intent filters) and `LockScreenPlayerActivity`; no activity was dropped.
- [x] Styles: kept the full `Theme.Ssilencce.Dialog` style and added `Theme.Ssilencce.LockScreen`; both themes remain.
- [x] Preferences imports: kept `MenuCustomizationGroup`/`MenuCustomizationState` and added `LockScreenWallpaper` (menu-customization prefs untouched).
- [x] `PlaybackService.onPlayWhenReadyChanged`: added the `LockScreenPlaybackClock`/`updateLockScreenReceiver` calls while preserving the Cast-guarded audio-focus path and the Cast-guarded end-of-media queue reset; resume-autoplay policy remains before them.
- [x] `PlaybackService.onIsPlayingChanged`: added `updateLockScreenReceiver()` and kept the mute/zero-volume guard, Cast crossfade cancel, persistence and A-B scheduling blocks intact.
- [x] Lock-screen receiver/clock now read `activePlayer` (logical local or Cast session player) instead of the local player, both at service start and in `updateLockScreenReceiver()`.
- [x] Receiver lifecycle preserved: registered only when `lockScreenPlayerEnabled` and the active player is playing/playWhenReady; unregistered on pause and explicitly on `onDestroy`; no polling or wake locks.
- [x] Crossfade, sleep timer, queue stop/repeat, shuffle, A-B/per-song speed and all Cast guards remain as merged in previous rounds.
- [x] Defaults preserved: `LOCK_SCREEN_PLAYER_ENABLED` defaults to false; no version, Room, dependency or translated-string changes.
- [x] Compile gate green (`powershell -NoProfile -ExecutionPolicy Bypass -File .orchestra\build-gate.ps1`, exit 0).
- [x] Single self-review: no conflict markers, no unrelated edits, no commits/history, nothing outside `_integration`.
- [x] Merge evidence and doc corrections captured in `DOCS_DELTA.md`.
