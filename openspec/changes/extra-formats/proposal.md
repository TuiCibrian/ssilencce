# Extra audio formats (AIFF, DSF/DFF) playback + supplementary indexing + decoder info

## Why

MediaStore indexes only a subset of Android audio containers, and Media3 ships no extractor for
AIFF/AIFC, DSF or DSDIFF (DFF). As a result, DSD libraries and many AIFF files are invisible or
unplayable even though the audio data is plain PCM or 1-bit DSD that can be converted on the fly.
The brief also asked for APE/WMA support through the bundled FFmpeg decoder; the packaged
`org.jellyfin.media3:media3-ffmpeg-decoder:1.9.0+1` only enables flac/alac/pcm_mulaw/pcm_alaw/
mp3/aac/ac3/eac3/dca/mlp/truehd, so CD-quality playback of APE/WMA is not possible without a
different native build. The feature therefore probes that build at runtime and reports the exact
limitation instead of shipping dead extractors.

## What Changes

- Add a custom Media3 `ExtractorsFactory` placed in front of `DefaultExtractorsFactory`.
- Add an `AiffExtractor` for AIFF/AIFC (PCM 8/16/24/32-bit big-endian, `sowt` little-endian,
  32/64-bit float; rewrites samples to the PCM encoding understood by `DefaultAudioSink`).
- Add a `DsdExtractor` for DSF and DSDIFF (DFF) with a two-stage decimation filter producing
  88.2 kHz 16-bit PCM (DSD64/128/256); DST-compressed DSDIFF is rejected with a clear error.
- Add a supplementary library scanner for `.aif/.aiff/.aifc/.dsf/.dff` files that MediaStore misses,
  reading tags/duration with TagLib and merging them into every song list. It is opt-in
  (`extra_formats_scan`) and caches results so song queries stay database-only.
- Teach `Song`/`SongRepository` to resolve non-MediaStore songs by a negative synthetic id and a
  `file://` URI, including queue/persistence/URI lookups.
- Add a runtime probe of the bundled FFmpeg decoders and expose container/codec/decoder details in
  the song info screen, including the actual decoder selected by ExoPlayer via `AnalyticsListener`.

## Impact

- New files under `playback/extractor/` and `data/local/extra/`.
- Affected: `PlaybackService`, `Song`, `SongRepository`, `InfoViewModel`, `InfoResult`,
  `SongDetailFragment`, `Preferences`, library settings XML, `LibraryRescan`, `App`.
