# ADR-060: Ghost Mode Filters the Shared Media Query Layer

## Status

Accepted.

## Context

Private folders must disappear from every library surface without duplicating filtering logic in
each screen, and without a Room schema change.

## Decision

- Store ghost folder paths as a JSON list in preferences and apply a `DATA NOT LIKE` prefix
  exclusion inside `RealSongRepository.makeSongCursor`. Room-backed playlist/stat queries reapply
  the same path filter when mapping stored entities.
- Direct ID, file-path and media-item lookups bypass the filter so playback and queue restoration
  keep working. A separate `ghostSongs()` query uses an inclusion selection for Ghost search.
- The optional PIN is a preference-backed hash and a per-process unlock flag, not encryption.
- Folder selection from a folder menu or the settings picker auto-enables ghost mode; the master
  switch can still temporarily reveal everything.

## Consequences

- All MediaStore-derived lists, counts, search, shuffle sources, widgets and Android Auto browsing
  honor the filter automatically.
- Any future query that bypasses `makeSongCursor` must apply `GhostMode` explicitly.
- Ghost songs already present in an active playback queue remain playable until removed, because
  the queue is playback state rather than a library list.
- The lock screen never lists ghost songs; its optional button only deep-links to the guarded
  Ghost search destination.
- Widget now-playing metadata is replaced with a hidden-track label, while the notification keeps
  normal metadata so lock-screen playback controls stay usable.

## Related

- ADR-007 (MediaStore source of truth), ADR-027 (navigation and shell), ADR-044 (search/shortcuts).
- `openspec/changes/ghost-mode/`, `openspec/changes/integration-compatibility/`.
