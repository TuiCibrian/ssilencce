# ADR-058: In-Track A-B Repeat and Per-Song Tempo

## Status

Accepted.

## Context

The player already exposes a global tempo (speed/pitch) held in the Equalizer DataStore and
applied by PlaybackService. Listeners also need to loop part of a track, and some tracks need a
different tempo than the rest of the library. Both features must survive UI recreation and must
not add polling to the playback path.

## Decision

- A-B repeat is owned by PlaybackService. The UI sends `SET_AB_REPEAT` with absolute track
  positions; the service validates/clamps the range, seeks into it when needed and schedules a
  single Handler wake-up at B (delay divided by the current playback speed). Seeks, play-state
  changes and playback-parameter changes reschedule the wake-up. A media item transition clears
  the loop. `GET_AB_REPEAT` lets a recreated UI recover the active range. The commands reuse the
  same-UID app-command pattern defined in ADR-045.
- Per-song speed/pitch overrides live in a new Room table (`SongPlaybackParamEntity`, DB v9,
  migration 8→9). On every media item transition the service applies the row when present and
  otherwise applies the global tempo. Commands persist/delete the row and apply it immediately to
  the current item. The global tempo collector only writes playback parameters while no per-song
  override is active.

## Consequences

- A-B repeat and per-song tempo continue while the player UI is not visible, and neither depends
  on UI polling.
- The global slots still show the global tempo while a per-song override is playing; the sound
  sheet states the song's stored values and offers Reset song.
- Backup/restore gains an optional JSON section for the new table.
- PlaybackParameters changes made outside this bookkeeping (future features) must respect the
  per-song override guard.
- Existing songs remain on the global tempo after the migration.

## Related

- ADR-014 (audio processing pipeline), ADR-015 (equalizer engines), ADR-045 (app commands),
  ADR-048 (selective backup types).
- `openspec/changes/ab-repeat-speed/`.
