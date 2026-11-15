# ab-repeat-speed

## Why

Listeners who practice, transcribe or study a track need to loop a section (A-B repeat) and some
tracks need a different speed or pitch than the global tempo. Today the player only has a global
tempo and no way to loop inside a track.

## What Changes

- Add in-track A-B repeat owned by the playback service: the player menu sets A, then B, playback
  loops A→B until cleared (cleared automatically on song change). A small chip on the player shows
  the current A / A-B markers.
- Add A-B repeat actions to synced lyrics: long-press a line to repeat it, or set A and B on two
  lines to define a range.
- Add per-song speed/pitch overrides persisted in a new `SongPlaybackParamEntity` Room table
  (DB v9, migration 8→9). The override is applied on media item transition and falls back to the
  global tempo. The existing speed & pitch section in sound settings gains "Apply to this song
  only" and "Reset song".
- Include the table in backup/restore as an optional section (`playback/song_playback_params.json`).
