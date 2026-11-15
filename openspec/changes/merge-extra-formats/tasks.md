# Tasks - merge extra-formats

- [x] Applied `extra-formats.patch` with `git apply --3way` on top of the compile-green 25-feature checkpoint; only `SongRepository.kt` needed manual conflict resolution.
- [x] `SongRepository.song(songId)`: kept the MediaStore lookup with `includeGhost = true` (GhostSearch/general id lookup) and added the negative-id `ExtraFormatLibrary.songById` branch.
- [x] `SongRepository.songByMediaItem`: kept the shared `song(songId)` path and preserved `includeGhost = true` for non-numeric media ids.
- [x] Ghost filtering for supplementary scanner results: `songs()` and `songs(query)` now filter extra-format songs inside `GhostMode.folderPaths` before merging with MediaStore results, so normal library, counts, shuffle queues and Auto browse exclude them.
- [x] GhostSearch keeps explicit access: `ghostSongs(query)` appends matching extra-format songs whose paths are inside the ghost folders (only when extra-format scanning is enabled).
- [x] Path matching mirrors the existing MediaStore SQL (`folder` or `folder/` prefix, slash-normalized, independent of the master toggle while folders are configured).
- [x] Crossfade secondary player: `CrossfadePlayer` now wraps its `DefaultExtractorsFactory` in `ExtraExtractorsFactory`, so AIFF/DSD work during pre-buffered crossfade playback as well as on the primary player.
- [x] Verified the primary `PlaybackService` extractor factory, analytics decoder registry, App startup scan scheduling, settings/preferences and info-screen additions from the patch remain intact.
- [x] Preserved all previous features (Cast guards, crossfade, lock-screen player, ResumeAutoplayPolicy/mute gates, sleep/queue/shuffle/A-B/per-song-speed, EQ band/system engine).
- [x] No versions, tests, translations, project docs, commits/history or edits outside `_integration`.
- [x] Compile gate green (`powershell -NoProfile -ExecutionPolicy Bypass -File .orchestra\build-gate.ps1`, exit 0).
- [x] Single self-review: no conflict markers, no unrelated edits.
- [x] Integration adjustments documented in `DOCS_DELTA.md`.
