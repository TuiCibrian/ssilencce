# Tasks: Sleep timer plus

## 1. Song-based stops

- [x] 1.1 Extend `SleepTimer` with a song-count mode (`setSongCount`, `onSongCompleted`,
      `onQueueEnded`) that reuses the pending-quit player stop.
- [x] 1.2 Count only natural (automatic) media-item transitions in `PlaybackService`; clear the
      timer when the queue ends.
- [x] 1.3 Add a **Songs** mode to the Compose sheet with a 1..20 count slider and a one-tap
      **Stop after this song** action.
- [x] 1.4 Show song-count progress and the "waiting for the current track" state in the running
      UI.

## 2. Timer history

- [x] 2.1 Add a pure `SleepTimerHistoryEntry` model + codec (duration minutes / end-at HH:mm,
      deduplicated, max five).
- [x] 2.2 Persist history in the existing `sleep_timer` DataStore and surface it in the UI.
- [x] 2.3 Render one-tap reuse chips and a clear action; reuse starts the timer immediately.

## 3. Foreground-only stop preference

- [x] 3.1 Add the `sleep_timer_stop_music_only_in_foreground` preference (default off) with a
      getter/setter in `Preferences` and a playback-settings switch.
- [x] 3.2 Add `AppVisibility` and register it from `App`.
- [x] 3.3 On timer end, pause only when the UI is visible; when enabled and the UI is not
      visible, pause and stop the media service.
- [x] 3.4 Expose the same switch inside the sleep-timer sheet.

## 4. Verification and notes

- [x] 4.1 Compile gate green (`:app:compileGithubDebugKotlin`, BUILD SUCCESSFUL).
- [x] 4.2 Self-review the diff (no debug code, no unrelated edits).
- [x] 4.3 Write `DOCS_DELTA.md` with the exact doc changes and decisions.
