# Documentation delta - merge crossfade

Apply `openspec/changes/crossfade/DOCS_DELTA.md` (FEATURES.md, MISSING_FEATURES.md,
AURORA_REFERENCE.md, CODE_MAP.md, AGENTS.md, docs/agents/domain.md and ADR-052) together with
the integration corrections below. The corrections describe the merged code; where both
disagree, this file wins.

## Merge-specific corrections

- **Manual-skip and previous hooks:** `AdvancedForwardingPlayer` now has both the crossfade
  hooks (`onSeekToNextRequested`, `onSeekToPreviousRequested`) and the open-intents guards
  (`canStartPlayback`, `onPlaybackBlocked`) plus the existing fade hook
  (`onPauseRequested`). `seekToNext`/`seekToNextMediaItem` ask the crossfade controller first;
  `seekToPrevious`/`seekToPreviousMediaItem` cancel an in-flight crossfade before the normal
  restart-vs-previous behavior. The Android Auto/notification "next" and "previous" commands
  go through the same hooks.
- **Pause path:** the pause fade callback now cancels an in-flight crossfade before delegating
  to `PlaybackFadeController.requestPause`, and `pausePlayback()` cancels as well (it also
  keeps `ResumeAutoplayPolicy.onUserPause()` and the audio-focus `onUserPause()`). Muted-output
  and deferred-play guards are unchanged.
- **Sleep timer:** the shared `PlaybackFadeController` ramp remains the only fade path; the
  crossfade is cancelled before the sleep-timer fade or stop, and the stop-at-end listener
  cancels when armed and refreshes when disarmed. No second volume animator exists.
- **Transition semantics:** a crossfade commit reaches the player as a seek; the service
  converts that single transition to `MEDIA_ITEM_TRANSITION_REASON_AUTO` before play
  counts/scrobbles, sleep-timer song counting, repeat policy and remembered-position restore.
  Notification artwork policy, A-B reset, per-song playback parameters and media-button layout
  refresh are unchanged.
- **Suppression:** A-B repeat, queue stop-after-song, sleep-timer stop-at-end and
  repeat-load-next-and-pause suppress crossfade; repeat-one never schedules because the next
  index equals the current index. Defaults stay off (`crossfade_duration = 0`).
- **Processors and EQ:** the secondary player mirrors balance, mono and ReplayGain, shares the
  main audio session id (updated on session changes) and therefore keeps the equalizer,
  reverb/virtualizer and system-equalizer session strategy effective during the overlap.
- **Lint fixes:** the earlier lint cleanups are preserved; no unused imports or conflict
  markers remain.

## docs/CODE_MAP.md - additional note

Update `playback/AdvancedForwardingPlayer.kt`:

- Responsibilities: add "intercepts manual next/previous commands for crossfade" and keep
  the existing "mute/preview play gate" wording.

Update `playback/PlaybackService.kt`:

- Notes: add "crossfade cancellation is composed with the pause fade, sleep-timer fade/stop and
  A-B/speed/queue policies; a crossfade commit is presented as an automatic transition".

## Decisions - merge-specific corrections

1. **Additive conflict resolution:** both sides of the two conflicts were kept. The mute
   play/preview guards and crossfade hooks coexist in `AdvancedForwardingPlayer`; the crossfade
   wiring sits next to the mute deferral block in `PlaybackService`.
2. **Keep the merged transition handler:** `refreshMediaButtonCustomLayout()` (from the
   checkpoint) is retained alongside the crossfade `transitionReason`; only the reason variable
   changed.
3. **No new fade/stop path:** sleep-timer fade and stop keep using
   `PlaybackFadeController`/`pauseWithoutFade`; crossfade only cancels before them.
4. **Defaults:** crossfade stays off; gapless-album suppression defaults on; all earlier
   playback behavior is unchanged for users who do not enable crossfade.
5. **Untouched lanes:** no changes to docs, versions, tests, translated strings, metadata,
   `.orchestra/` or files outside the integration worktree.
