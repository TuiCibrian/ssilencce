# Documentation delta - merge extra-formats

Apply `openspec/changes/extra-formats/DOCS_DELTA.md` together with the integration
corrections below. The corrections describe the merged code; where both disagree, this file
wins.

## Merge-specific corrections

- **Ghost filtering:** the supplementary scanner results are filtered through the ghost-folder
  policy in `SongRepository.songs()` and `songs(query)`. Normal library lists, counts, shuffle
  queues and Android Auto browse therefore never see extra-format songs stored in ghost
  folders. `ghostSongs(query)` explicitly appends matching extra-format ghost songs, and
  `song(songId)`, `songByMediaItem` and `songByFilePath` keep explicit lookup access (needed for
  playback and GhostSearch).
- **Path semantics:** ghost membership is matched by normalized path prefix
  (`folder` or `folder/`), mirroring the existing MediaStore `GhostMode.exclusionSelection()`.
  The path filter applies while ghost folders are configured, matching the current SQL behavior.
- **Crossfade parity:** the worker patch only replaced the primary `PlaybackService` extractor
  factory. The merge also wraps `CrossfadePlayer`'s extractor factory in
  `ExtraExtractorsFactory`, so the pre-buffered secondary player supports AIFF/DSD during a
  crossfade.
- **Id lookup:** negative extra-format ids resolve through `ExtraFormatLibrary`; MediaStore id
  lookups retain `includeGhost = true` so GhostSearch and playback resolution still work.

## docs/CODE_MAP.md - additional note

Update `playback/crossfade/CrossfadePlayer.kt`:

- Dependencies: add `playback/extractor/ExtraExtractorsFactory.kt`.
- Notes: add "uses the same extra-format extractor stack as the primary player".

Update `data/local/repository/SongRepository.kt`:

- Notes: add "extra-format scanner results honor ghost-folder filtering in normal list/search
  paths, while `ghostSongs`, id lookup and media-item/file-path resolution keep explicit access".

## Decisions - merge-specific corrections

1. **Ghost consistency:** filtering follows the same folder-prefix rule as the MediaStore
   ghost selection, rather than `GhostMode.filterSongs()`'s enabled-only check, so the
   supplementary library cannot leak hidden files while MediaStore hides them.
2. **Explicit access:** GhostSearch and playback lookups bypass folder filtering by design and
   only include extra-format songs when `extra_formats_scan_enabled` is on.
3. **Crossfade inheritance:** the secondary player builds its own extra extractor stack; no
   shared mutable factory is passed, preserving the stateful-extractor contract.
4. **Untouched lanes:** no tests, commits/history, translated strings, project docs, versions or
   edits outside `_integration`.
