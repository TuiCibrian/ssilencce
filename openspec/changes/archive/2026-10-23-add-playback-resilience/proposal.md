# Proposal: Playback resilience and focus controls

## Why

The documented gaps list playback features that affect long-form listening and real-world
interruption handling: per-track resume for audiobooks/podcasts, configurable audio focus
(including ask-before-taking and resume-after-call), a Quick Settings playback tile, and
session survival after process death. The existing Media3 session and persistence layer provide
the foundations, but the user-facing capabilities are missing.

## What Changes

- Add an opt-in per-track remembered position for long-form tracks, stored in Room, applied when
  a track starts and cleared when it completes.
- Add a Quick Settings playback tile with correct active/inactive/unavailable state and a
  lifecycle-bounded MediaController connection.
- Add an audio-focus mode setting: normal, ignore, ask before taking focus, and resume after
  call; integrate with the existing Media3 focus handling and pause/resume lifecycle.
- Add opt-in session recovery: persist whether playback was active and resume the restored queue
  after a service/process restart, without claiming to bypass force-stop or OS-level kills.

## Impact

- Affected code: `PlaybackService`, `PersistentStorage`, `Advanced`/`Playback` preferences,
  new tile service, new audio-focus controller, new `TrackPosition` entity/DAO and Room migration,
  `MainModule`, `AndroidManifest`.
- Docs: CODE_MAP, FEATURES, MISSING_FEATURES, ADR-011/012 amendments and a new playback
  resilience ADR.
- New strings in `values/strings.xml` only; app version unchanged.
