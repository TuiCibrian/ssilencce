# ADR-059: Extra Audio Container Extractors and Supplementary Indexing

## Status

Accepted.

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
   example NULL-duration rows); let the existing MediaScanner rescan refresh those. Do not change
   the Room schema.

## Consequences

- AIFF/DSF/DFF playback and library discovery work on every supported device without new native
  dependencies.
- DSD playback costs CPU for filtering; the filter runs once per output sample and avoids polling
  or wakelocks.
- APE/WMA files remain unplayable and are documented in the UI and docs; enabling them later needs
  a rebuilt FFmpeg decoder set plus a JNI bridge that passes the codec parameters, or a platform
  codec. `.ape` is not indexed while playback is impossible.
- Extra-format albums/artists are not merged into the MediaStore aggregate screens; their song
  rows are fully playable and persist play counts, favorites and queues through the negative ids.
- The supplementary scanner is opt-in and off by default; its cache is excluded from ghost
  folders and refreshed on manual rescan, toggle change or after six hours.

## Related

- ADR-007 (MediaStore source of truth), ADR-017 (FFmpeg ALAC decoding), ADR-024 (TagLib metadata).
- `openspec/changes/extra-formats/`, `openspec/changes/merge-extra-formats/`.
