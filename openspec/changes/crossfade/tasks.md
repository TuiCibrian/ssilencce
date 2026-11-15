# Tasks

- [x] Investigate existing playback, fade, volume, gapless and queue infrastructure.
- [x] Add crossfade preference keys and accessors in `util/Preferences.kt`.
- [x] Add crossfade settings to `res/xml/preferences_screen_playback.xml`.
- [x] Add crossfade strings (English only).
- [x] Add album-id/disc-number media item extras and the gapless-album policy.
- [x] Implement `CrossfadePlayer` (secondary ExoPlayer with mirrored renderers).
- [x] Implement `CrossfadeController` (schedule, pre-buffer, equal-power ramps, commit,
      handoff, cancel paths, manual skip).
- [x] Add a crossfade multiplier to `PlaybackVolumeController`.
- [x] Intercept next commands in `AdvancedForwardingPlayer`.
- [x] Wire the controller into `PlaybackService` (create, audio session, preferences,
      transition semantics, release).
- [x] Expose crossfade controls in the Aurora Sound settings sheet.
- [x] Compile gate passes.
- [x] Self-review diff.
