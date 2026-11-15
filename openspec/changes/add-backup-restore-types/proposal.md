# Proposal: Selective backup/restore types (AA-09)

## Why

The backup archive covers settings, non-favorites playlists, the lyrics cache and custom artist
images. Favorites are only an implicit playlist entry, and play counts, named queues and per-file
resume positions are not backed up. The Musicolet comparison still lists
"choose what to restore (settings, playlists, favorites, play counts, queues, per-file settings)"
as a gap because choosing those categories is not possible.

The missing data lives in Room. Adding it must preserve old ZIP archives, selective-restore
semantics, bounded structural verification and honest failure reporting.

## What Changes

- Add a dedicated `Favorites/favorites.m3u` entry and a Favorites restore category. New backups
  exclude the localized Favorites playlist from the generic `Playlists/` set. Old archives that
  keep Favorites under `Playlists/` still restore them when Playlists is selected.
- Add `statistics/play_counts.json` for complete play-count snapshots. Restore merges
  play/skip/time values by maximum so statistics never decrease.
- Add `queues/named_queues.json` for named queue snapshots. Restore preserves or replaces ids,
  enforces the 100-queue cap, resolves name collisions and reports capped entries as failures.
- Add `positions/track_positions.json` for per-file resume positions. Restore inserts missing
  positions and keeps the newer `updated_at`.
- Extend the existing selective-restore picker from four to eight categories.
- Omit empty JSON lists at creation and treat them as valid no-ops. Record malformed selected JSON
  entries as per-entry failures without aborting later entries; ZIP size/structure failures still
  abort.
- Keep the pure archive/merge rules in `BackupArchivePolicy` for JVM probing.
- No Room schema, dependency, permission or translated-string change. New UI, if any, remains
  Compose; this change reuses the existing picker.

## Impact

- Modified: `util/BackupHelper.kt`, `util/RestoreSummary.kt`, `data/local/room/PlayCountDao.kt`,
  `data/local/room/TrackPositionDao.kt`, `res/values/strings.xml`, `docs/FEATURES.md`,
  `docs/MISSING_FEATURES.md`, `docs/CODE_MAP.md`, `docs/agents/domain.md`.
- New: `util/backup/BackupArchivePolicy.kt`, `util/backup/BackupEntryModels.kt`, ADR-048, this
  OpenSpec change and probes.
- Unchanged: Room schema/migrations, build files, translated strings, playback transport and the
  existing playlist/settings/lyrics/artist-image restore paths.
