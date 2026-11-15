# ADR-048: Selective Backup/Restore Types

## Status

Accepted on 2026-11-03. Extends the ZIP archive contract in ADR-043 and ADR-047.

## Context

The backup archive covered settings, non-favorites playlists, the lyrics cache and custom artist
images. Favorites were an implicit playlist entry, and play counts, named queues and per-file
resume positions were not backed up at all. The Musicolet comparison therefore kept a real gap:
"choose what to restore" promised categories that ssilencce could not restore.

The new data lives in Room, so a raw file copy or a settings-only extension cannot restore it
safely. The archive must remain readable by older builds and must remain strictly selective: an
old archive that stores Favorites under `Playlists/` cannot silently change meaning.

## Decision

- Keep the existing `.bmgbak` ZIP, structural limits, path-safety rules and selective-restore
  outcomes. Add four categories without changing the Room schema or adding dependencies.
- **Favorites:** write a dedicated `Favorites/favorites.m3u` entry. New archives exclude the
  localized Favorites playlist from the generic `Playlists/` set so selecting Playlists and
  selecting Favorites are independent. Restore resolves the current localized Favorites playlist,
  creates it only when the archive contains favorites to add, and merges missing songs without
  duplicating existing entries. Older archives that keep Favorites under `Playlists/` keep their
  legacy behavior: selecting Playlists restores them, and selecting only Favorites does not.
- **Play counts:** write `statistics/play_counts.json` as serializable song snapshots with
  `timePlayed`, `playCount` and `skipCount`. Restore merges each counter with `max(current,
  backup)`, preserves current song metadata when the row already exists and bulk-upserts new rows.
  A restore can never lower a statistic.
- **Named queues:** write `queues/named_queues.json` with id, name, timestamps, resume index and
  position, repeat/shuffle flags and ordered media ids. Restore preserves the backup id when it is
  usable, replaces the same id transactionally, enforces `QueueNamePolicy.MAX_NAMED_QUEUES` for
  new ids, and resolves name collisions with the existing unique-name suffix rule. Blank ids and
  entries above the cap are skipped and reported as failures; a replacement does not consume a
  second slot. Restore does not bind a queue as active; that remains a settings decision.
- **Per-file settings:** write `positions/track_positions.json` for `TrackPositionEntity`.
  Restore inserts missing media ids and replaces an existing row only when the backup `updated_at`
  is strictly newer. Older or equal positions are skipped without being reported as errors.
- Empty JSON lists are omitted at creation and treated as valid no-ops on restore. A malformed
  selected JSON entry is logged and recorded as a selected-entry failure without aborting later
  archive entries; ZIP structure and bounded-stream size failures still abort the whole restore.
  A named-queue batch may report both an import and a failure, producing an honest `Partial`
  result.
- Extend the existing selective-restore picker with Favorites, Play counts, Queues and Per-file
  settings. No separate screen is required; the picker already enumerates `BackupContent`.
  Main English strings only; no translated string changes.
- Keep the pure archive/merge decisions in `BackupArchivePolicy` so they can be probed on the
  JVM without Android or a device.

## Consequences

- Users can move statistics, named queues and resume positions between installs/devices through
  the same single ZIP.
- New backups no longer duplicate favorites under `Playlists/`. Users who restore only Playlists
  from a new archive do not implicitly restore Favorites, while old archives retain their legacy
  behavior.
- Play-count restore is additive/monotonic. It cannot repair a corrupted local count downward.
- Restoring a queue with an id that already exists replaces that queue and its items; queue
  collisions beyond the cap are explicit failures rather than silent drops.
- Track-position restore prefers local newer state. A backup cannot rewind a resume position.
- The archive schema is now larger, but older builds simply ignore the unknown paths. No database
  migration, dependency or translated string is introduced.
- Runtime verification must cover old and new archives, each category independently, malformed
  per-entry JSON, queue-cap collisions, name collisions and local-newer positions. Until that
  device pass is complete, the feature is source/probe verified only.
