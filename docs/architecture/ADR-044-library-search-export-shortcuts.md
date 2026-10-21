# ADR-044: Library Search, Export Formats and Entity Shortcuts

## Status

Accepted on 2026-10-20.

## Context

The library had global search but no settings search or inline list filtering, playlist export
wrote only M3U, and launcher shortcuts covered only four built-in actions. These are
user-facing capabilities that need a consistent ownership boundary with the existing View and
Compose surfaces.

## Decision

- **History deletion** uses the existing history DAO and song ids. History actions are scoped to
  the history screen; queues, play counts and media files are untouched.
- **Settings search** builds an index from the public preference XML resources and shows grouped
  results in a Compose sheet. Selecting a result navigates to the section and requests a scroll
  to the key when it exists. No private `PreferenceInflater` access.
- **Inline quick search** is hosted by the shared list fragment and implemented per top-level
  list against already loaded data. It filters in place, preserves selection/sort behavior and
  clears back to the full list.
- **Playlist export** treats the format as a writer concern: M3U/M3U8 share the extended-M3U
  body with UTF-8 output and the appropriate extension, PLS writes numbered entries. The UI
  chooses a format once per export action. Empty playlists are refused for user-facing export and
  leave no file or MediaStore row. A repeated export replaces the same-name/path target instead
  of creating a suffixed duplicate. The pre-Q writer stages beside the target. MediaStore uses
  a fresh pending URI through `MediaStore.Files` with the format MIME, completes/closes/publishes
  it, then verifies backup/final renames before deleting the captured old URI. Pre-commit failure cleans only the new URI and attempts to
  restore the old name; provider refusal retains old bytes at the backup name. Post-commit
  cleanup failure may leave a backup. No atomic-swap, cross-process-lock or power-loss guarantee
  is claimed. Backups keep legitimate empty playlists as valid `#EXTM3U` entries.
- **Entity shortcuts** are dynamic `ShortcutInfoCompat` entries with an entity type and id/path.
  A confirmation dialog is the ask-before-adding step. `MainActivity` resolves the entity to its
  songs and reports missing targets; playlists remove their shortcut when deleted.

## Consequences

- Search and shortcuts use public platform APIs and remain compatible with the hybrid UI
  (ADR-026).
- Settings search navigates to a section rather than deep-linking every preference; scrolling is
  best-effort.
- Export formats share one writer, so a future format is a small addition.
- Launcher capacity is controlled by the launcher; failure is reported rather than hidden.
