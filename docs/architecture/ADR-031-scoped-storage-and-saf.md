# ADR-031: Scoped Storage and Storage Access Framework Strategy

## Status

Accepted

## Context

The app reads the MediaStore library (ADR-007), writes playlist exports, deletes songs,
backs up data and exports EQ profiles — all constrained by Android's scoped storage (API 29+)
and runtime permissions. Deleting files across API levels requires three different mechanisms
(MediaStore delete/trash requests, SAF document deletion, legacy direct file delete), and
user-selected folder trees need persisted permissions.

## Decision

- Centralize writes in `MediaStoreWriter` (API 29+): insert `ContentValues`, stream content,
  `notifyChange`, with a plain-file fallback on pre-Q devices and cleanup of inserted rows on
  failure; used for M3U exports and EQ profile exports.
- Delete songs through `DeleteSongsDialog` + `MusicUtil`: on Android 11+ use
  `MediaStore.createDeleteRequest`/`createTrashRequest` (respecting the trash preference),
  with SAF-document deletion as fallback; pre-R use batched direct deletes
  (`_ID IN (...)` chunks under 100k entries to dodge SQLite limits) with progress callbacks.
- Persist SAF tree permissions: `saveTreeUri`/`getWritablePersistedUris`/`findSAFDocument`
  (`SAFExt`) so folder trees chosen once (e.g. for deletion access) keep working across
  sessions; declare `requestLegacyExternalStorage` for API 29 devices.
- Export playlists as M3U via `M3UWriter` (EXTINF lines, sorted by song key): MediaStore
  `.m3u` entries on API 29+, plain files in the app playlists directory before that.
- Backup/restore with `BackupHelper`: a `.bmgbak` ZIP containing playlists as M3U, settings
  XML, lyrics JSON and custom artist images, restored through SAF pickers; system backups are
  described by `backup_descriptor` + `data_extraction_rules`.
- Keep the FileProvider for sharing reports/APKs and the `CoverProvider` (ADR-025) for
  stable media URIs.

## Consequences

- Each API level gets its least-privilege path; there is no legacy blanket storage
  permission (minSdk 26 reads via `READ_EXTERNAL_STORAGE`, 33+ via `READ_MEDIA_AUDIO/IMAGES`).
- Delete flows are inherently API-versioned and permission-dialog-heavy; the SAF dialog
  explains why a folder tree is needed before deletions on scoped storage.
- Backups are portable (ZIP + M3U), but play counts/history are not backed up (ADR-018).
- The pre-Q fallback paths are legacy debt that must remain until minSdk rises above 28.
