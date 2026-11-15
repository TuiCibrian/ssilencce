# Playback transport improvements

## Why

Playback lacks user control over transport ergonomics: pause/play transitions are abrupt,
the previous control is fixed at a 5 second rewind threshold, fast-forward and rewind
share one duration, seek gestures are always available even on short tracks, and gapless
trimming cannot be disabled.

## What Changes

- Add configurable fade-in on play and fade-out on pause (0–4000 ms, default 0 = off),
  coordinated with sleep-timer fade-out and audio-focus ducking through one volume owner.
- Replace the legacy `rewind_with_back` switch with a "Previous button" choice
  (restart current song after 5 s / always previous), migrating the old value.
- Split the single seek interval into independent fast-forward and rewind durations,
  falling back to the legacy `seek_interval` value when the new keys are absent.
- Add a minimum-duration gate (off / 10 / 20 / 30 / 60 minutes) that disables
  fast-forward and rewind for short tracks; headset seek actions fall back to next/previous.
- Add a gapless playback toggle (default on) that disables encoder-delay/padding trimming
  when turned off.
