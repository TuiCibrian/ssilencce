# Crossfade Between Tracks

## Why

Musicolet offers a configurable crossfade between tracks (0-12 s). ssilencce currently
only fades on play/pause and relies on the gapless engine for continuous albums. Users
who want a DJ-like overlap between unrelated tracks have no option. The feature is listed
as missing in `docs/MISSING_FEATURES.md`.

## What Changes

- New playback settings: crossfade duration (0-12 s, default 0 = off), optional crossfade
  on manual skip (default off) and automatic crossfade opt-out for gapless albums
  (default on).
- A dual-player crossfade engine: a secondary ExoPlayer pre-buffers the next queue item,
  both players run complementary equal-power volume ramps, and the main player commits the
  queue transition before the outgoing track ends. The secondary renderer stack mirrors the
  main one (balance, mono, ReplayGain, same audio session) so EQ/ReplayGain/balance keep
  working during the overlap.
- The media session, queue state, persistence, widgets, notifications, scrobbling and the
  sleep timer keep using the single main player; the secondary player is invisible to them.
- A crossfade commit is reported to the playback service as an automatic track transition
  so play counts and scrobbles match normal queue advancement.
- Crossfade is suppressed for consecutive tracks of the same album when gapless playback is
  enabled, for repeat-one, A-B repeat, stop-after-song, sleep-timer stop-at-end and
  repeat-load-next-and-pause.
- Settings UI in the playback preferences screen and in the Aurora Sound sheet (the latter
  no longer shows crossfade as unavailable).
