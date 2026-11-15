# ADR-062: Folder Mutations Across Scoped Storage

## Status

Accepted.

## Context

Folder management needs to create/rename folders and move/copy audio files on API 26–36, where
direct file writes stop working on scoped storage. MediaStore alone cannot create arbitrary
folders, and SAF trees may not cover every volume. Deletion of files owned by other apps
additionally requires user consent on Android 10+.

## Decision

Folder mutations are performed by `util/FolderFileOperations`. Writes first use direct `java.io`
when the destination is writable and otherwise require a persisted SAF tree resolved through
`findSAFDocumentFile`. Move is implemented as copy-then-delete; source deletion tries SAF, then a
direct file delete, then `ContentResolver.delete`, and hands recoverable/`createDeleteRequest`
consents to the dialog. Every operation ends with a MediaStore rescan. The folder info/maintenance
UI is Compose.

## Consequences

- No storage permission beyond the existing media permissions is added.
- Operations work across API 26–36 within the granted trees, and never silently copy without
  telling the user when originals could not be removed.
- Long transfers are owned by the dialog; a configuration change during a transfer cancels it,
  potentially leaving copies (no partial file is left at the destination because failed writes are
  deleted).
- Hide/Exclude reuse the existing `InclExclEntity` blacklist; no new table or schema version is
  introduced.

## Related

- ADR-007 (MediaStore source of truth), ADR-031 (scoped storage and SAF), ADR-044 (library actions).
- `openspec/changes/folders-plus/`.
