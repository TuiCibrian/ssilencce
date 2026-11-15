# Design: Selective backup/restore types (AA-09)

## Archive layout

| Category | Path | Format |
| --- | --- | --- |
| Favorites | `Favorites/favorites.m3u` | M3U |
| Play counts | `statistics/play_counts.json` | JSON list |
| Named queues | `queues/named_queues.json` | JSON list |
| Per-file settings | `positions/track_positions.json` | JSON list |

Existing paths remain `Playlists/`, `prefs/`, `lyrics/` and `artistImages/`. New backups write
favorites only under `Favorites/`; the generic playlist writer skips the playlist whose name equals
the current `R.string.favorites_label`. The archive path, not the localized display name, is the
restore contract.

## Favorites

- Backup writes the favorites M3U even when the playlist is empty (a valid `#EXTM3U` header), but
  omits it when no Favorites playlist exists.
- Restore parses the M3U with the same absolute-path resolution as playlists, resolves the current
  localized Favorites playlist, creates it only when there are songs to add, and inserts only songs
  not already present.
- Legacy archives still contain Favorites under `Playlists/`; selecting Playlists keeps restoring
  them. Selecting only Favorites on such an archive restores nothing, because the archive has no
  Favorites category.

## Play counts

- JSON entries carry the full song snapshot plus `timePlayed`, `playCount` and `skipCount`.
- Existing rows keep their current metadata and take `max(current, backup)` per counter. New rows
  use the backup snapshot. The DAO bulk-upserts the merged list.
- A replay cannot lower a statistic; this is deliberate and documented.

## Named queues

- JSON entries carry id, name, createdAt/updatedAt, resume index/position, repeat/shuffle flags and
  ordered media ids.
- Restore plans each entry with `BackupArchivePolicy.planNamedQueueRestore`: blank ids are skipped;
  a new id is rejected at `QueueNamePolicy.MAX_NAMED_QUEUES`; a replacement id does not consume a
  slot; names are normalized and made unique against queues with other ids.
- `NamedQueueDao.saveQueue` replaces the queue and items transactionally. The active queue binding
  remains a settings decision; it is not changed here.
- Per-entry DB failures and cap skips set the failed flag. A batch with both successful and failed
  entries reports an import plus a failure, which produces `Partial`.

## Per-file positions

- JSON entries carry mediaId, positionMs and updatedAt.
- Restore reads all current positions, inserts missing media ids and replaces only when the backup
  `updatedAt` is strictly newer. Equal or older positions are skipped silently.
- No song-file lookup is performed: media ids are the playback identity.

## Failure and limit behavior

- Empty JSON lists are omitted at creation and return `NoContent` on restore.
- JSON parsing uses `Json { ignoreUnknownKeys = true }`; malformed selected entries are logged and
  return `Failed`, which calls `RestoreSummary.recordFailure()`.
- The bounded entry stream still enforces 10 MiB for JSON entries, 100 MiB for other entries,
  100 MiB total and 1,000 entries. A size/zip failure throws before or during `readText()` and
  aborts the restore, so it is not mistaken for a per-entry format failure.
- Existing selective-restore outcomes are unchanged: no selection, no recognized content, nothing
  imported, partial and success keep their current messages.

## Scope

- No Room entity/DAO schema change: only two DAO query methods were added to existing DAOs.
- No dependency, permission or translated-string change; only `values/strings.xml` gains the four
  English labels and the updated share disclosure.
- The existing `BackupContent` picker enumerates the new categories, so no new screen is needed.
- Pure rules live in `BackupArchivePolicy`; probes compile it with `RestoreSummary` and
  `QueueNamePolicy` on the JVM.
