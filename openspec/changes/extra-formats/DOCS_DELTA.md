# DOCS_DELTA — extra-formats

Apply these exact changes to the project docs. Do not edit `docs/MUSICOLET_FEATURES.md`.

## Decisions

1. **Scanner is opt-in, default off.** New preference key `extra_formats_scan`
   (`Preferences.extraFormatsScanEnabled`, default `false`) preserves current library behaviour for
   existing installs, as required by the worker protocol. Enabling it scans the configured
   whitelist folders, or (when no whitelist is configured) the MediaStore song folders plus
   `Environment.DIRECTORY_MUSIC`. Results are cached in `filesDir/extra_formats/library.json` and
   reused until the cache is 6 hours old, a manual rescan runs, or the switch is toggled. On
   Android 11+ directory enumeration obeys the app's media read grants; if the OS hides a folder,
   listing that folder in the whitelist is the reliable way to include it.
2. **DSD output is 88.2 kHz PCM16.** DSF and DSDIFF 1-bit streams are decimated with a two-stage
   filter (boxcar cascade to 352.8 kHz, 64-tap Blackman-windowed sinc FIR by 4) so DSD64/128/256
   (2.8224/5.6448/11.2896 MHz) all yield 88.2 kHz. DST-compressed DSDIFF is rejected with
   `ParserException.createForUnsupportedContainerFeature("DST-compressed DSDIFF is not supported")`.
3. **AIFF/AIFC scope.** PCM 8/16/24/32-bit big-endian (`NONE`/`twos`), little-endian (`sowt`),
   unsigned 8-bit (`raw `), 32-bit float (`fl32`/`FL32`) and 64-bit float (`fl64`/`FL64`, converted
   to float32) are supported. Integer 24-bit and 32-bit AIFF are reduced to 16-bit because Android
   below API 31 has no packed 24/32-bit AudioTrack encoding; the sink guarantees 16-bit output on
   every supported device. IMA4, µ-law and A-law AIFC files are rejected with an explicit
   unsupported-container error. Sample bytes are rewritten to the little-endian/unsigned PCM
   encodings `DefaultAudioSink` accepts.
4. **APE and WMA playback are skipped, not stubbed.** The packaged
   `org.jellyfin.media3:media3-ffmpeg-decoder:1.9.0+1` is built by
   `jellyfin-androidx-media/build.sh` with
   `ENABLED_DECODERS=(flac alac pcm_mulaw pcm_alaw mp3 aac ac3 eac3 dca mlp truehd)`.
   Inspection of the shipped `arm64-v8a/libffmpegJNI.so` finds `ff_flac_decoder`,
   `ff_alac_decoder`, `ff_mp3_decoder`, `ff_aac_decoder`, `ff_dca_decoder` and
   `ff_truehd_decoder`, but **no** `ff_ape_decoder`, `ff_wmav1_decoder`, `ff_wmav2_decoder` or
   `ff_wmapro_decoder`. In addition, Media3's `ffmpeg_jni.cc` `createContext()` does not pass
   `bit_rate`, `block_align` or `bits_per_coded_sample`, which the FFmpeg APE/WMA decoders require.
   Therefore no APE/WMA extractors or renderers were added: they would always fail. Instead
   `BundledFfmpegProbe` checks the private `FfmpegLibrary.ffmpegHasDecoder` at runtime and the song
   info screen reports exactly why APE/WMA is unavailable (`decoder_ape_unavailable`,
   `decoder_wma_unavailable`). The scanner excludes APE/WMA unless a platform `audio/x-ms-wma`
   decoder or a future FFmpeg build makes them playable, so the library never shows a song that
   cannot be played.
5. **No `.ape` indexing while playback is impossible.** The brief's indexing item named `.ape`, but
   adding unplayable entries would be worse than hiding them. `.aif/.aiff/.aifc/.dsf/.dff` are
   indexed. If a future FFmpeg build includes `ape`, `ExtraFormatSupport.isPlayable` starts
   admitting it and the scanner picks it up automatically.
6. **NULL-duration MediaStore rows are not duplicated.** Rows that MediaStore knows but reports with
   duration 0 are refreshed by the existing `LibraryRescan`/`MediaScannerConnection` path; the
   supplementary scanner only adds files MediaStore never indexed. Duplicating a known path under a
   synthetic id would split play counts and favorites.
7. **Synthetic ids and URIs.** Extra-format songs get deterministic negative ids
   (`ExtraFormatTrackIds`, FNV-1a over `path:<absolute path>`), so Room play counts/history/queue
   entries work unchanged. `ExtraFormatUriStore` maps those ids to `file://` URIs; `Song.uri` checks
   it before building a MediaStore content URI. Albums/artists of extra songs get separate negative
   synthetic ids: they appear correctly in song lists but are not merged into the MediaStore
   album/artist aggregate screens.
8. **Song info decoder source.** Custom containers show the extractor/decimator as the decoder
   (`Ssilencce AIFF extractor`, `Ssilencce DSD decimator (88.2 kHz PCM)`). Standard Media3
   playback shows the actual decoder reported by `AnalyticsListener.onAudioDecoderInitialized`
   for the current song, falling back to `System MediaCodec` for songs that have not been played.
9. **No Room schema change** was needed; the cache is a JSON file and the negative ids reuse the
   existing Long columns.

## docs/FEATURES.md

- Replace the existing playback bullet at line 20:

  Old: `- **Software decoding fallback** — Jellyfin FFmpeg decoder ... Media3 1.10.1 provides no demuxers for ASF/WMA, APE, DSF/DFF or AIFF, so those containers are not playable.`

  New:
  `- **Software decoding fallback** — Jellyfin FFmpeg decoder (`media3-ffmpeg-decoder`) auto-loaded by the Media3 extension renderer for codecs MediaCodec cannot handle (e.g. ALAC). Custom Media3 extractors add AIFF/AIFC and DSF/DFF playback: AIFF is converted to the sink's little-endian PCM encodings, DSF/DFF 1-bit DSD is decimated to 88.2 kHz PCM16 (DST-compressed DSDIFF is rejected). APE and WMA remain unplayable: the packaged FFmpeg build enables only flac/alac/pcm_mulaw/pcm_alaw/mp3/aac/ac3/eac3/dca/mlp/truehd (no ape/wmav1/wmav2/wmapro), and the song info screen reports that limitation.`

- Add under the Library/scanning feature notes (same bullet style):

  `- **Supplementary extra-format indexing** — Opt-in "Index extra audio formats" library setting (default off). Scans the whitelist folders (or MediaStore song folders plus the public Music folder) for `.aif/.aiff/.aifc/.dsf/.dff` files MediaStore does not index, reads tags/duration with TagLib (header fallback for DSD/AIFF), applies the library duration range and blacklist, and caches results in `filesDir/extra_formats/library.json`. Cached songs appear in song lists/search, play from `file://` URIs, and survive queue/favorite/history round trips through stable negative ids. The cache is refreshed on a manual rescan, when the switch is toggled, or after six hours at startup. APE/WMA are excluded while the bundled FFmpeg cannot decode them.`

- Replace the Song info bullets (currently line 121) with:

  `- **Song info** — Bitrate, format, sample rate, channel mode, bit depth, container, codec and the decoder actually used (from Media3 `AnalyticsListener`, or the custom AIFF/DSD extractor description), a metadata completeness check with refresh, and an explicit unsupported-decoder explanation for APE/WMA.`

## docs/MISSING_FEATURES.md

- Replace the bullet at Part 1 line 30 (currently starting `- **Extra format playback (WMA, APE, DSF, DFF, AIFF)** …`) with:

  `- **Extra format playback (APE, WMA)** — AIFF/AIFC, DSF and DSDIFF (DFF) now play through custom Media3 extractors, with an opt-in supplementary indexer for files MediaStore misses and container/codec/decoder/bit-depth rows in song info. APE and WMA still do not play: the packaged Jellyfin FFmpeg 1.9.0+1 build enables only flac/alac/pcm_mulaw/pcm_alaw/mp3/aac/ac3/eac3/dca/mlp/truehd (verified: no `ff_ape_decoder`/`ff_wmav1_decoder`/`ff_wmav2_decoder`/`ff_wmapro_decoder` symbols), and Media3's JNI bridge does not pass the `bit_rate`/`block_align`/`bits_per_coded_sample` those decoders need. `.ape` is therefore not indexed either. Still missing vs Musicolet: system-decoder switch and multi-threaded decode.`

- Remove the now-stale claim in the same line that MediaScanner null-duration rows are dropped by the duration filter (the existing MediaScanner rescan path remains the fix for standard-format rows; the new scanner covers extra-format files).

## docs/CODE_MAP.md

Add entries (use the existing Purpose / Responsibilities / Inputs / Outputs / Dependencies / Consumed by / Related files / Notes format):

- `playback/extractor/ExtraExtractorsFactory.kt` — Prepends `AiffExtractor`/`DsdExtractor` to `DefaultExtractorsFactory` for `DefaultMediaSourceFactory`; creates fresh extractor instances per call.
- `playback/extractor/AiffExtractor.kt` — AIFF/AIFC parser and PCM converter (PCM 8/16/24/32, `sowt`, `raw `, float32/float64), linear PCM seek map, sample writer.
- `playback/extractor/DsdExtractor.kt` — DSF and DSDIFF parser, block/planar layouts, LSB/MSB bit order, DST rejection, block-aligned seek map, 88.2 kHz PCM16 output.
- `playback/extractor/DsdChannelDecoder.kt` — Two-stage DSD decimator (boxcar cascade + 64-tap FIR), per-channel state, reusable by DSF/DFF.
- `playback/extractor/ExtractorSupport.kt` — Shared big/little-endian readers, 80-bit extended float, `LinearPcmSeekMap`, `PcmOutputWriter`, extra MIME constants.
- `playback/extractor/ExtraFormatSupport.kt` — Extra-format kinds, container/codec/decoder descriptors and playability (scanner extension gate).
- `playback/extractor/BundledFfmpegProbe.kt` — Reflection probe of `FfmpegLibrary.ffmpegHasDecoder`/version and platform MediaCodec lookup.
- `data/local/extra/ExtraFormatLibrary.kt` — JSON cache, `ExtraFormatTrack` → `Song`, deterministic negative ids, `ExtraFormatUriStore` population.
- `data/local/extra/ExtraFormatScanner.kt` — Opt-in IO scan of allowed folders, six-hour freshness, blacklist/duration filters, cache refresh.
- `data/local/extra/ExtraFormatFileProbe.kt` — Header-only AIFF/DSF/DFF duration fallback when TagLib cannot report it.
- `data/model/ExtraFormatUriStore.kt` — Process-wide negative-id → `file://` resolver used by `Song.uri`.
- `playback/PlaybackDecoderRegistry.kt` — Last audio format/decoder snapshot for the current media id, fed by `PlaybackService`'s `AnalyticsListener`.

Update existing entries:

- `playback/PlaybackService.kt` — Media source factory now uses `ExtraExtractorsFactory`; registers an `AnalyticsListener` for decoder/container reporting.
- `data/local/repository/SongRepository.kt` — Merges extra-format songs in all song queries and resolves negative ids/paths/MediaItems.
- `data/model/Song.kt` — `uri` consults `ExtraFormatUriStore`; `toMediaItem` carries the Song tag.
- `data/local/MetadataReader.kt` — Exposes `durationMs()`, `sampleRateHz()`, `channelCount()`.
- `ui/screen/info/InfoViewModel.kt` / `InfoResult.kt` / `SongDetailFragment.kt` — Container/codec/decoder/bit-depth info and unsupported-format explanation.
- `util/Preferences.kt` — `extra_formats_scan` preference.
- `data/local/LibraryRescan.kt` — Refreshes the supplementary index alongside MediaScanner.
- `App.kt` — Schedules the supplementary scan when the feature is enabled and the cache is stale.
- `ui/screen/settings/PreferencesScreenFragment.kt` — Scan-on-enable/disable and library reload.
- `res/xml/preferences_screen_library.xml` — Extra-format indexing switch.

## ADR-052 (new)

Create `docs/architecture/ADR-052-extra-audio-container-extractors.md`:

```markdown
# ADR-052: Extra audio container extractors and supplementary indexing

## Status
Accepted

## Context
MediaStore and Media3 1.10.1 do not cover the complete set of audio containers users keep in
music libraries. AIFF/AIFC and DSF/DFF are common in mastering/DSD collections, but Media3 has no
extractor for them, so playback fails before any decoder is considered. MediaStore also does not
index DSF/DFF at all and misses some AIFF files. The packaged Jellyfin FFmpeg extension is a
decoder-only library with a fixed decoder set; it cannot demux containers itself.

## Decision
1. Add a custom `ExtractorsFactory` in front of `DefaultExtractorsFactory`. The extractors are
   responsible for parsing the container and, for PCM/DSD formats, converting samples to PCM:
   AIFF/AIFC is rewritten to the sink's little-endian/float PCM encodings, and DSF/DSDIFF DSD is
   decimated to 88.2 kHz PCM16 with a two-stage filter. DST-compressed DSDIFF is rejected.
2. Keep APE/WMA out of the playback path unless the bundled FFmpeg build exposes their decoders.
   The current build does not, and Media3's JNI bridge does not supply the fields those decoders
   need; a runtime probe reports the limitation in song info rather than shipping an extractor that
   can never decode.
3. Index extra-format files with an opt-in, cached supplementary scanner keyed by deterministic
   negative song ids and `file://` URIs. Do not duplicate MediaStore rows that already exist (for
   example NULL-duration rows); let the existing MediaScanner rescan refresh those. Do not change the
   Room schema.

## Consequences
- AIFF/DSF/DFF playback and library discovery work on every supported device without new native
  dependencies.
- DSD playback costs CPU for filtering; the filter runs once per output sample and avoids polling or
  wakelocks.
- APE/WMA files remain unplayable and are documented in the UI and docs; enabling them later needs a
  rebuilt FFmpeg decoder set plus a JNI bridge that passes the codec parameters, or a platform codec.
- Extra-format albums/artists are not merged into the MediaStore aggregate screens; their song rows
  are fully playable and persist play counts, favorites and queues through the negative ids.
```
