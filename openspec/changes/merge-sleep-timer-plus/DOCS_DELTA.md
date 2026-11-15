# Documentation delta - merge sleep-timer-plus

Apply `openspec/changes/sleep-timer-plus/DOCS_DELTA.md` together with the integration
corrections below. The corrections describe the merged code; where both disagree, this file
wins.

## docs/FEATURES.md - section 11 "Utilities"

Replace the **Sleep timer** bullet with:

```markdown
- **Sleep timer** - AlarmManager-based countdown or exact HH:MM end time, stop after N songs
  (the current song counts as the first; only tracks that play to their end are counted),
  one-tap stop after this song, fade-out and finish-last-song (time modes), last five
  time-based choices as one-tap history chips with a clear action, and an optional
  "Stop only music if app UI is in foreground" policy that pauses only playback when the app
  is visible and also stops the media service when the timer ends in the background.
```

The sleep-timer fade-out uses the same `PlaybackFadeController` ramp as play/pause fades;
there is no separate volume animator.

## docs/MISSING_FEATURES.md

- Appendix A, **Sleep timer** bullet: **delete** it (the reduced-form gap is closed).
- Part 2 §1 **Sleep timer**: replace the ssilencce line and the "See Part 1 §1" line with:

```markdown
- ssilencce: countdown, exact HH:MM end time, stop after N songs, stop after this song,
  fade-out, finish-last-song, timer history with 1-tap reuse/clear, "stop only music if UI
  is foreground". No remaining behavioral gap; a gradual fade whose length the user chooses
  is available for time modes (song modes stop at the track boundary).
```

## docs/CODE_MAP.md

Update `playback/SleepTimer.kt`:

- Purpose: "Schedule playback stop after a configurable time or number of songs."
- Responsibilities: add "song-count mode (`setSongCount`, `onSongCompleted`, `onQueueEnded`)",
  "stop-at-end-armed notifications (`addStopAtEndListener`) so the service can combine the
  sleep-timer stop with the queue stop position", and "listener dispatch on the main looper".
- Inputs: add "songs count".
- Outputs: add "SleepTimerWaitingFor.SongCount" and "stop-at-end armed state".
- Notes: append "Song-count timers are process-local; they do not survive process death. The
  final song is stopped through the player's pause-at-end-of-media-items flag, the same path
  used by the queue's stop-after-this-track action."

Update `ui/screen/sleeptimer/SleepTimerScreen.kt`:

- Line: "Compose sleep timer bottom sheet: duration/end-at/songs modes, options, countdown,
  recent timers, start/stop."
- Responsibilities: add "Songs mode slider (1..20) and one-tap Stop after this song", "Recent
  timer chips + Clear", "foreground-only stop switch", "SongCount waiting text".
- Outputs: `startTimer(mode, hour, minute, songCount)`, `startStopAfterThisSong`,
  `reuseHistory`, `clearHistory`, `setStopMusicOnlyInForeground`.
- Dependencies: add `SleepTimerHistoryEntry`, `R.plurals.sleep_timer_songs_remaining`,
  `Preferences.sleepTimerStopMusicOnlyInForeground`.

Update `ui/screen/sleeptimer/SleepTimerViewModel.kt`:

- Responsibilities: add "song-count persistence/start", "timer history persistence (max 5,
  deduplicated)", "foreground-only pref write".
- Inputs: add song count and `SleepTimerHistoryEntry`.
- Outputs: add history in `SleepTimerUiState`, reuse/clear entry points.
- Notes: append "History records only Duration/EndAt starts; song starts are excluded. The
  foreground-only pref lives in the main SharedPreferences (key
  `sleep_timer_stop_music_only_in_foreground`) so PlaybackService can read it synchronously."

Add a new entry next to the other sleeptimer files:

```markdown
- `ui/screen/sleeptimer/SleepTimerHistory.kt` - Pure persisted timer-history model and codec.
  - **Purpose:** Keep the last five time-based timer choices without a new database.
  - **Responsibilities:** `Duration`/`EndAt` entries, encode/decode (`d:45`, `e:22:30`,
    `|`-separated), deduplicated most-recent-first insertion, max five, validation.
  - **Inputs:** Raw DataStore string; entries.
  - **Outputs:** Parsed entry list; encoded string.
  - **Dependencies:** None (pure Kotlin).
  - **Consumed by:** SleepTimerViewModel.
  - **Related files:** SleepTimerViewModel.kt, SleepTimerScreen.kt.
  - **Notes:** Durations are bounded to 5..180 minutes to match the sheet slider; invalid rows
    are dropped rather than failing the whole history.
```

Add a new entry under `util/`:

```markdown
- `util/AppVisibility.kt` - Process-wide "is any app activity started" tracker.
  - **Purpose:** Let playback policies distinguish a visible app from a backgrounded one.
  - **Responsibilities:** Register `ActivityLifecycleCallbacks`, maintain a started-activity
    count, expose a volatile `isUiVisible` flag; all callbacks are no-ops except
    started/stopped.
  - **Inputs:** `Application` registration.
  - **Outputs:** `isUiVisible` boolean.
  - **Dependencies:** Android Application/Activity lifecycle.
  - **Consumed by:** PlaybackService (sleep-timer foreground-only stop).
  - **Related files:** App.kt, playback/PlaybackService.kt, playback/SleepTimer.kt.
  - **Notes:** Callbacks run on the main thread; the flag is safe to read from any thread.
```

Update `App.kt`:

- Responsibilities: add "register AppVisibility".
- Related files: add `util/AppVisibility.kt`.

Update `util/Preferences.kt`:

- Responsibilities/notes: add the `sleep_timer_stop_music_only_in_foreground` boolean
  (default false) and the getter/setter `sleepTimerStopMusicOnlyInForeground`.

Update the `res/xml/preferences_screen_playback.xml` notes paragraph:

- Append: "Advanced also hosts the sleep-timer foreground-only stop switch
  (`sleep_timer_stop_music_only_in_foreground`, default false)."

Update `playback/PlaybackService.kt` notes:

- Add: "Tracks a song-count sleep timer on automatic media-item transitions, resets it when
  the queue ends, combines the sleep-timer and queue stop-at-end contributions into
  `pauseAtEndOfMediaItems`, and (when enabled) stops the media service after a background
  sleep-timer end. Sleep-timer fade-out reuses `PlaybackFadeController` and
  `pauseWithoutFade()`; no separate volume animator exists."

## AGENTS.md

Update the `playback/` resilience bullet (Repository Code Map) to read:

```markdown
- `playback/` resilience - `AudioFocusController` (Normal/Ignore/Ask/ResumeAfterCall, pending-ask cleanup, ducking), `PlaybackTileService` (Quick Settings), ordered non-blocking `TrackPositionWriter`, bounded `SESSION_RECOVERY`, song-count/stop-after-song sleep timer with persisted recent timers and a foreground-only stop policy.
```

No other AGENTS.md change is needed.

## docs/agents/domain.md

- No ADR count change (no new ADR file). ADR-019 is amended in place (text below).

## ADR-019-sleep-timer.md (amendment)

Append to **Decision**:

```markdown
- Song-count timers are evaluated on automatic media-item transitions only; the currently
  playing track counts as the first song. The final track is stopped through the player's
  `pauseAtEndOfMediaItems` flag, combined with the queue stop position so canceling either
  source does not clear the other.
- The last five time-based choices (durations and end-at times) are persisted in the existing
  `sleep_timer` DataStore and can be restarted with one tap or cleared.
- An opt-in `sleep_timer_stop_music_only_in_foreground` policy keeps playback paused but alive
  when the timer ends while an activity is visible; when no activity is visible it also calls
  `pauseAllPlayersAndStopSelf()` so a sleeping user is not left with a media service.
```

Append to **Consequences**:

```markdown
- Song-count timers do not survive process death (unlike AlarmManager deadlines), and the
  history is best-effort (invalid rows are ignored).
- UI visibility is tracked by `AppVisibility`; the policy reads the main SharedPreferences key
  synchronously from the service.
- Sleep-timer fade-out shares `PlaybackFadeController`/`PlaybackVolumeController` with the
  play/pause fades, so equalizer volume, audio-focus ducking and sleep-timer fade-out compose
  instead of overwriting each other.
```

## Decisions - merge-specific corrections

1. **No second fade path:** the worker patch reintroduced `ValueAnimator`-based
   `launchMusicFadeOut`/`cancelSleepTimerFadeOut`/`restorePlayerVolume` and wrote
   `player.volume` directly. The merge deletes those helpers and adapts the sleep timer to
   `fadeController.fadeOut(durationMs) { player.pauseWithoutFade(); ...quit policy }`.
2. **Pause-at-end composition:** the queue stop position and the sleep-timer stop-at-end state
   are separate fields (`queueStopsAtEndOfCurrentItem`, `sleepTimerStopsAtEndOfCurrentItem`);
   `updatePauseAtEndOfMediaItems()` writes their OR into
   `player.exoPlayer.pauseAtEndOfMediaItems`. Canceling either source no longer clears the
   other.
3. **Repeat-load-next-pause preserved:** `RepeatPolicy` still pauses and rewinds the loaded
   item on `MEDIA_ITEM_TRANSITION_REASON_AUTO`; the same automatic transition also calls
   `sleepTimer.onSongCompleted()`. Manual skips/seeks do not count toward the song limit and
   do not trigger the repeat pause.
4. **App.kt conflict:** additive imports; `AppVisibility` was added without dropping the
   existing `Preferences` import.
5. **Defaults:** the foreground-only preference defaults to false and all existing timer
   behavior (duration/end-at, fade, finish last song) is unchanged.
6. **Untouched lanes:** no changes were made to the lyrics tools, artwork store, A-B/per-song
   tempo, queue actions or library data integrations, nor to docs, versions, tests or
   non-English strings.
