# Tasks - merge cast-chromecast

- [x] Applied `cast-chromecast.patch` with `git apply --3way` on top of the assembled/lint-green 22-brief checkpoint; `PlaybackService.kt` and `ic_cast_24dp.xml` needed manual resolution.
- [x] `PlaybackService`: kept both import groups (crossfade + cast) and composed pause handling as `crossfadeController.cancel()` + `activePlayer.pause()`.
- [x] `PlaybackService`: merged the sleep-timer listener so the Cast branch pauses the receiver directly, while local fade/stop still uses `PlaybackFadeController`; crossfade is cancelled first in both paths.
- [x] `PlaybackService`: merged `onIsPlayingChanged` (mute/zero-volume guard, persistence, A-B scheduling) with the Cast guard and an explicit local-crossfade cancel while remote playback owns the session.
- [x] `PlaybackService`: kept `ResumeAutoplayPolicy.onPlaybackStarted()`, the mute/deferred-play gates, and routed resume paths through `activePlayer`; preserved the merged transition handler, `transitionReason`, notification artwork policy, repeat policy and media-button layout.
- [x] `PlaybackService`: kept the checkpoint `refreshMediaButtonCustomLayout()` implementation (Android Auto preferences) and made its timeline check use `activePlayer`.
- [x] `PlaybackService`: merged the system-volume collector (muted/deferred handling) with Cast suppression; queue/shuffle/repeat/A-B/per-song-speed and latest EQ band/system-engine logic remain intact.
- [x] Added `!isCasting` to `isCrossfadeAllowed`, so a local crossfade is never scheduled while the Cast receiver owns playback; local scheduling resumes after disconnect.
- [x] `ic_cast_24dp.xml`: kept the single checkpoint icon (valid cast glyph already used by EQ output routing and now by the toolbar/Aurora overflow); removed the duplicate patch path.
- [x] Verified flavor separation: `src/cast` + `media3-cast` only for `github`/`playstore`; `fdroid` gets `src/nocast` and no Cast dependency or manifest overlay. `CastPlaybackProvider` exists only in the flavor source set per flavor.
- [x] Compile gate green: `:app:compileGithubDebugKotlin` (exit 0).
- [x] Compile gate green: `:app:compileFdroidDebugKotlin` (exit 0).
- [x] Single self-review: no conflict markers, no version bumps, no translated strings, no tests, no commits/history, no edits outside `_integration`.
- [x] Merge evidence and doc corrections captured in `DOCS_DELTA.md`.
