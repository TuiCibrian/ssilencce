# ADR-024: Metadata Read/Write with TagLib and Jaudiotagger

## Status

Accepted

## Context

The app displays and edits rich audio metadata (tags, embedded covers, bitrate/sample
rate/channel mode) in per-entity tag editors (song/album/artist). Android's MediaStore only
exposes a subset of tags and cannot write arbitrary fields; the tag editors need direct file
access with a robust library, plus a source for standardized genre lists and audio header
details.

## Decision

- Use kyant0 `TagLib` (`io.github.kyant0:taglib`, the Android port of the C++ TagLib) as the
  primary reader/writer:
  - `MetadataReader` exposes tags/property maps, embedded pictures, bitrate, sample rate and
    channel mode (with genre normalization), using duplicated detached file descriptors and
    swallowing all throwables for resilience,
  - `MetadataWriter` batch-writes property maps and covers for an `EditTarget`
    (Song/Artist/Album), filters ReplayGain tags to avoid duplicates, regenerates the
    album-art thumbnail, and triggers a `MediaScannerConnection` scan afterwards.
- Use Jaudiotagger (`com.github.Adonai:jaudiotagger`) only where it complements TagLib:
  - standardized `GenreTypes` lists in the editors,
  - audio header details (bitrate/sample rate/channel mode) for the info screen
    (`InfoViewModel`/`ChannelMode`).
- Share one editor scaffold `AbsTagEditorActivity` across the three editors with:
  - Photo Picker for cover selection and `MediaStore.createWriteRequest` (API 29+) with a
    pre-R direct-write fallback,
  - cover sources: download (Deezer, ADR-025), pick, web search, restore/remove,
  - Deezer/MusicBrainz autofill (song editor only) mapping recordings/credits into tag fields,
  - custom artist images via `CustomArtistImageManager` (2048 px JPEG, MediaStore Artists
    notify, SharedPreferences signature tracking).
- Keep `MetadataField` (Album, Genre, Year, Bitrate, ...) as the configurable extra-info
  surface for the player/widgets, reading through `MetadataReader`.

## Consequences

- Full-fidelity tag editing (any field TagLib supports) on real files, not a MediaStore
  subset.
- Writes are user-visible to other apps after the media scan; heavy libraries pay a scan
  cost per batch edit.
- Two metadata libraries coexist with clear boundaries (TagLib = file tags/covers,
  Jaudiotagger = genre taxonomy + audio headers), avoiding duplicate maintenance.
- TagLib's native library is per-ABI, reinforcing the ABI-split strategy (ADR-005).
