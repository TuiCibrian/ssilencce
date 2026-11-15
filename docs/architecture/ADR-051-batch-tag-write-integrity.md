# ADR-051: Batch Tag-Write Integrity and Current-File Recovery

## Status

Partially implemented (2026-11-14). `openspec/changes/add-batch-tag-editor` and
`openspec/changes/tag-editor-finish` implement a bounded subset: a session journal copies each
file's original bytes to `cacheDir/tag-write-backup/<uuid>/` before the destructive open (2 GiB
per file, 4 GiB per session, 7-day stale pruning), a failed open/save/read-back restores the
current file, and the editor offers a session undo for successfully written files. Not
implemented: the `noBackupFilesDir` binding, SHA-256/sidecar validation, fail-closed pre-open
identity checks, the 512 MiB free-space reserve, `MISMATCH`/`KEPT_CHANGED` rollback, and
`WRITE_STARTED`/`RECOVERY_NEEDED` process-death recovery. No device/runtime evidence exists.

## Context

AA-07's batch tag editor and the guarded legacy album/artist editors write metadata in place
through `MetadataWriter` and TagLib. A successful read-back verifies requested fields, but a
failure after the destructive writer opens can leave the current file partially changed. Stop is
cooperative at the next file boundary and is not rollback: completed files remain changed and
remaining files are cancelled. Process death can leave an unknown result. The original AA-07
requirements for session Undo, batch covers, single-entity routing and the runtime format matrix
remain separate and open.

The design proposal `7B1B7FB7F8907AAD476E9D83ECDD017D59DB33CAFD13BFE712E640834C288ABB`
concretizes a bounded current-file recovery mechanism. This ADR records that direction for future
work; it does not approve or implement it.

## Decision

- Before opening a destructive `"rw"` descriptor for a file, preserve that file's full original
  bytes in an app-private backup under
  `noBackupFilesDir/tag-write-backup/<batch-token>/`, with a verified sidecar recording schema,
  token, ordinal, media ID, URI, size, SHA-256 and original `date_modified` when available.
- Validate the backup (size, hash, URI/media identity, sidecar state and current target metadata)
  immediately before the writer opens. Any mismatch SHALL fail closed without opening the writer.
- Restore only the current file on an in-file exception, failed save after partial mutation, or
  read-back `MISMATCH`/`KEPT_CHANGED`. A verified restore deletes the backup; a failed restore
  retains it, reports failure and stops the batch.
- Do not roll back files already committed. Stop at a file boundary remains non-rollback and
  leaves completed files changed. Session Undo is a separate original AA-07 requirement, not
  implied by this recovery path.
- Do not auto-overwrite on process death or power loss. A `WRITE_STARTED` record becomes
  `RECOVERY_NEEDED`; a user/C-owned recovery decision is required because the file may already
  contain a valid completed write.
- Bound the mechanism: existing 500-file selection, 2 GiB per-file backup, 4 GiB backup directory,
  at most two orphan/recovery tokens, seven-day retention and a 512 MiB free-space reserve. A file
  over a cap fails before any destructive write.
- Add no dependency, Room schema, permission, version change or translated string. The backup
  directory binding and any new English status labels are C-owned. No content-URI atomicity is
  claimed; restore is a verified stream overwrite with post-restore hash verification.
- Session undo intentionally restores only hard failures; `Mismatch`/`KeptChanged` stay reported as
  `Partial` with undo available, because rolling them back would discard fields that did persist.

## Consequences

- The current file gains fail-closed backup validation and current-file recovery; previously
  completed files remain changed after a later failure or Stop.
- The mechanism is not a user-visible undo history and does not close AA-07's session Undo,
  covers, routing or runtime format-matrix gaps.
- Process-death recovery needs a separate UI/decision path; silent auto-restore is forbidden.
- Backup storage is transient (one current file plus orphan/recovery records), but clear-data,
  uninstall, device loss or storage failure still lose it.
- Implementation requires a separate exact-source lease, C/M review, build/lint receipt and
  host/runtime evidence. Until then this ADR remains proposed only.

## Related

- ADR-007 (MediaStore as source of truth), ADR-024 (TagLib metadata), ADR-031 (scoped storage and
  `noBackupFilesDir` precedent).
- `openspec/changes/add-batch-tag-editor/` and the AA-07 integrity proposal
  `7B1B7FB7...`; the frozen safety v2 source/receipt remain historical and unchanged.
