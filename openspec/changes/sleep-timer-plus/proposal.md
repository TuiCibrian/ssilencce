# Proposal: Sleep timer plus

## Why

The sleep timer only understands wall-clock deadlines. Users who want to fall asleep after a
fixed number of tracks (or after the track that is playing right now) have to use the queue
menu, and every timer has to be rebuilt from scratch because no recent choices are kept.
Musicolet also offers "stop only music if app UI is in foreground"; ssilencce has no such
guard, so a background timer can leave a media session and foreground service alive even though
nobody is going to resume playback.

## What Changes

- Add a **Songs** sleep-timer mode that stops playback after N songs, counting the song that is
  playing when the timer starts as the first one. The last song is stopped by arming the same
  player `pauseAtEndOfMediaItems` stop path that the queue's existing "Stop after this track"
  action uses, so no extra alarm is scheduled.
- Add a one-tap **Stop after this song** action to the timer sheet (equivalent to N = 1, same
  stop path).
- Persist the **last five** time-based timer choices (durations and end-at wall-clock times),
  expose them as one-tap reuse chips in the sheet, and add a clear action.
- Add the **Stop only music if app UI is in foreground** preference. When enabled and the timer
  ends while the app UI is not visible, playback pauses and the media service is stopped (a full
  quit); when the UI is visible only playback pauses. When disabled (default) behavior is
  unchanged: playback pauses and the session stays alive.
- Track UI visibility through a small `ActivityLifecycleCallbacks` helper registered by `App`.

## Impact

- Affected code: `playback/SleepTimer`, `playback/PlaybackService`, `App`, new
  `util/AppVisibility`, `util/Preferences`, `ui/screen/sleeptimer/` (screen, view model, new
  history model), `res/xml/preferences_screen_playback.xml`, main English strings.
- No Room schema, dependency, version, translated-resource or test source-set changes.
- Documentation updates are listed in `DOCS_DELTA.md` (the orchestra protocol forbids editing
  the shared docs directly).
