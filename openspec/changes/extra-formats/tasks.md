# Tasks — extra-formats

## Playback
- [x] Custom `ExtraExtractorsFactory` registered in `PlaybackService` ahead of the defaults
- [x] `AiffExtractor` (AIFF/AIFC, PCM + float conversion, seeking, duration)
- [x] `DsdExtractor` (DSF + DFF, LSB/MSB bit order, block/planar layouts, 2-stage decimation to 88.2 kHz PCM16, block-aligned seeking)
- [x] DST-compressed DSDIFF rejected with a clear parser error
- [x] Runtime probe of the bundled FFmpeg decoders (`ape`, `wmav1/2`, `wmapro`) + platform WMA decoder

## APE / WMA (brief allows skip when the bundled FFmpeg lacks decoders)
- [x] Verify the packaged FFmpeg decoder set and record exact evidence
- [x] Document that APE/WMA playback is unavailable and why; song info reports the limitation

## Indexing
- [x] Persisted `ExtraFormatLibrary` cache (JSON) + process-wide song-id → URI resolver
- [x] `ExtraFormatScanner` walking allowed folders (whitelist or MediaStore folders + Music), reading TagLib tags/duration, applying library duration filters
- [x] `Song.uri` resolves synthetic ids; `SongRepository` merges extra songs in `songs()`, `songs(query)`, `song(id)`, `songByFilePath`, `songsByMediaItems`, `songsByUri`
- [x] `LibraryRescan.scan()` and app start refresh the cache
- [x] `extra_formats_scan` preference + library settings switch + scan-on-enable

## Song info
- [x] `PlaybackDecoderRegistry` fed by an `AnalyticsListener` in `PlaybackService`
- [x] Container / codec / decoder / bit-depth rows in the file section
- [x] English strings for the new labels and unsupported-format messages

## Verification
- [x] Compile gate green (`.orchestra/build-gate.ps1`)
- [x] Self-review diff, no debug code or unrelated edits
- [x] `DOCS_DELTA.md` with FEATURES / MISSING_FEATURES / CODE_MAP exact bullets and decisions
- [x] `STATUS` = DONE
