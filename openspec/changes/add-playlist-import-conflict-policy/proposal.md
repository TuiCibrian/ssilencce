# Proposal: Playlist import conflict policy

## Why

Device-playlist import currently has three related problems:

- The song picker computes the checked subset but passes the original playlist to the import
  call, so unchecked songs are imported anyway.
- An exact name collision is auto-suffixed silently and the success message reports the source
  name instead of the stored name.
- Playlist creation and song insertion are separate calls, so a failure can leave an orphan
  playlist, and check-then-create can race with another import.

Backup restore already has a different, non-interactive merge policy. The interactive import
path should not inherit or silently change that behavior.

## What Changes

- Import only the selected subset, deduplicated by song ID with first occurrence preserved.
- When the source name does not exactly match an existing playlist, import directly and
  transactionally.
- When an exact name conflict exists, show a Compose conflict dialog with:
  - **Keep both** as the default, creating the next free `"Name 1"`..`"Name 100"` suffix while
    leaving the existing playlist untouched. Keep both is disabled when suffixes are exhausted.
  - **Replace existing** for a single exact, non-Favorites target; replace its contents in one
    transaction while preserving its ID, name, description and custom cover.
  - **Rename imported** to a trimmed, non-blank, non-duplicate name; create a new playlist and
    leave the existing one untouched.
  - Cancel/back/dismiss with no mutation and no success message.
- Add a pure `PlaylistImportNameResolver` for suffix generation and rename validation.
- Add DAO transactions for create+insert and replace-contents, and serialize name resolution
  and mutation with a ViewModel `Mutex`.
- Extend `ImportResult` with the actual stored name, outcome and imported song count; success
  messages use the actual name.
- Keep backup restore on its separate, deterministic non-interactive merge-into-existing path.
  It is documented and is not routed through the conflict dialog.
- Add only main English strings and Compose UI. No schema migration, unique-name index,
  dependencies, test source sets, version change, README/metadata/contributor edits or
  translated strings.

## Impact

- Affected code: playlist import dialog, new Compose conflict dialog, `LibraryViewModel`,
  `LibraryResult`, `PlaylistDao`, `PlaylistRepository`, `Repository`, new name resolver.
- Affected resources: `values/strings.xml`.
- Affected docs: `FEATURES`, `MISSING_FEATURES`, `CODE_MAP`; OpenSpec change only.
- Backup restore behavior remains unchanged and is documented as a separate policy.
