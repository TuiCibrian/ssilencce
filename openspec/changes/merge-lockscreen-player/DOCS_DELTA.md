# Documentation delta - merge lockscreen-player

Apply `openspec/changes/lockscreen-player/DOCS_DELTA.md` together with the integration
corrections below. The corrections describe the merged code; where both disagree, this file
wins.

## Merge-specific corrections

- **Logical active player:** `LockScreenPlaybackClock` and the dynamic `ACTION_SCREEN_OFF`
  receiver are driven by `activePlayer` (the Cast `sessionPlayer` facade when a receiver owns
  playback, otherwise the local `AdvancedForwardingPlayer`). The DOCS_DELTA bullet in
  `playback/PlaybackService.kt` should read "active player (local or Cast)" instead of only
  `player`.
- **Receiver lifecycle:** registered only while `lock_screen_player_enabled` is true and the
  active player is `isPlaying || playWhenReady`; it is refreshed on play/pause, on the feature
  preference change and on service start, and unregistered on pause and `onDestroy`. No
  polling, alarm or extra wake lock exists.
- **Additive manifest/style resolution:** `MiniPlayerActivity` and its VIEW filters coexist with
  `LockScreenPlayerActivity`; `Theme.Ssilencce.Dialog` and `Theme.Ssilencce.LockScreen` both
  remain. Menu-customization preference classes are preserved alongside the new lock-screen
  wallpaper model.
- **Cast/crossfade interaction:** the lock-screen callbacks are additive; Cast audio-focus
  suppression, the Cast-guarded end-of-media queue reset, `ResumeAutoplayPolicy`, sleep-timer
  fade/stop, queue stop/repeat, shuffle and A-B/per-song-speed behavior are unchanged. Local
  crossfade remains suppressed/cancelled while Cast owns playback.
- **Defaults:** `lock_screen_player_enabled` stays false; the feature is visible only after the
  user opts in, and the settings preview bypasses trigger conditions.

## docs/CODE_MAP.md - additional note

Update `playback/PlaybackService.kt`:

- Notes: add "the screen-off receiver and `LockScreenPlaybackClock` follow the logical active
  player, so they keep working during Cast playback and stop when the active player is paused".

Update `ui/screen/lockscreen/LockScreenTrigger.kt`:

- Notes: add "`LockScreenLaunchPolicy` reads the feature/enable/skip preferences; the receiver is
  dynamic and service-owned, never manifest-registered".

## Decisions - merge-specific corrections

1. **Additive conflicts:** both activities, both themes and both import groups were kept; no
   previous checkpoint feature was dropped.
2. **Active-player clock:** the lock-screen trigger reflects Cast playback through
   `sessionPlayer` while retaining local behavior after disconnect; `playWhenReady` remains the
   clock source so buffering is not treated as a pause.
3. **No new ADR:** the worker DOCS_DELTA's "no new ADR" decision stands; ADR-052 (crossfade) and
   ADR-053 (Cast) remain the latest architecture records.
4. **Untouched lanes:** no tests, commits/history, translated strings, project docs, versions or
   edits outside `_integration`.
