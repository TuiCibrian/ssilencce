# Proposal: Maximum song-duration filter (AA-03)

## Why

The library currently supports a minimum song duration but not a maximum. Musicolet exposes a
maximum-duration exclusion; the gap is documented in `MISSING_FEATURES` as “Exclude by duration”.
Users need a library-only way to hide very long tracks without removing them from saved playlists,
favorites or the active queue.

## What Changes

- Add `maximum_song_duration`, default **0 = off/unlimited**, stored in seconds.
- Add a `SeekBarPreference` after the minimum-duration row: 0..7200 s in 60-second steps.
- Add `duration_filter_range_invalid` and reject min/max combinations where both are enabled and
  max < min.
- Add a pure `SongDurationFilterPolicy` and apply **both** inclusive duration predicates only in
  `SongRepository.makeSongCursor`, the same path where minimum currently applies.
- Add an explicit `skipDurationFilter` guard and pass it from queue resolution, ID/path/file-provider
  resolution, the device-playlist sorted-cursor path and the unknown-genre list so none is
  transitively filtered.
- Keep device/Room playlists and favorites unfiltered.
- Do not refactor `GenreRepository`; document that genre detail lists, genre search and genre counts
  still bypass the shared duration cursor as a separate residual.
- No schema, dependency, permission or translated-string changes; main English strings only.

## Impact

- Modified: `Preferences.kt`, `preferences_screen_library.xml`, `strings.xml`,
  `SongRepository.kt`, `PreferencesScreenFragment.kt`, `FEATURES`, `MISSING_FEATURES`, `CODE_MAP`.
- New: `data/local/SongDurationFilterPolicy.kt`, OpenSpec change, probes.
- Unchanged: queue resolution, playlists, favorites, named queues, schema/dependencies and all
  translated resources.
