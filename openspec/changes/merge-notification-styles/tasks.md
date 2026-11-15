# Tasks - merge notification-styles

- [x] Applied `notification-styles.patch` with `git apply --3way` on top of the compile-green 26-feature checkpoint; `PlaybackService.kt` needed manual resolution.
- [x] Kept all Cast properties (`castPlayback`, `sessionPlayer`, `activePlayer`, `isCasting`) and exactly one `mediaSession` property, merged with the new system/custom notification providers.
- [x] Kept both preference handler groups: `GHOST_MODE_ENABLED` (notification refresh + widgets) and `NOTIFICATION_STYLE` + background/always-expanded handlers.
- [x] Kept the existing A-B `scheduleAbRepeat()` call in `onIsPlayingChanged`; the notification progress call was added after widget updates without duplicating A-B scheduling.
- [x] Notification progress now uses `activePlayer` (local or Cast facade), runs only for styles whose `showsSeekBar` is true and while playback is active, and is removed from `uiHandler` on pause and `onDestroy`.
- [x] GhostMode masking preserved: the custom provider masks title, artist, artwork, palette and up-next for ghost songs; the system provider masks title/artist through its content-title/text overrides. Masking is based on the current item's `Song` tag.
- [x] `Media3 system style` remains the default (`NotificationStyle.System`), and both providers share the single MediaSession.
- [x] Verified the worker's new style model, preference keys, playback settings entries, arrays/strings, custom RemoteViews and background-color dialog integration remain intact.
- [x] Preserved all previous features (Cast, crossfade, lock screen, ghost mode, sleep/queue/shuffle/A-B/per-song-speed, EQ).
- [x] No versions, tests, translations, project docs, commits/history or edits outside `_integration`.
- [x] Compile gate green (`powershell -NoProfile -ExecutionPolicy Bypass -File .orchestra\build-gate.ps1`, exit 0).
- [x] Single self-review: no conflict markers, no unrelated edits.
- [x] Integration adjustments documented in `DOCS_DELTA.md`.
