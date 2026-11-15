# Design: Maximum song-duration filter (AA-03)

## Context

`SongRepository.makeSongCursor` already adds an inclusive minimum-duration SQL predicate and then
composes album/whitelist selections. All repository-backed song queries that use this cursor
inherit those filters; queue resolution (`songsByMediaItems`), playlists and favorites use separate
paths and do not. `GenreRepository` also uses raw genre-member queries, so minimum duration is
already outside genre detail/count/search. This change extends the existing cursor, not the
architecture.

## Goals / Non-Goals

**Goals:**

- Maximum-duration preference default off/unlimited, persisted in seconds.
- Inclusive maximum predicate combined with the existing minimum predicate.
- Settings UI with a clear off state and invalid-range validation.
- Probes for policy, query composition and scope.
- Document the genre residual and intentional queue/playlist/favorite exemptions.

**Non-Goals:**

- Refactoring `GenreRepository` or applying filters to genre detail/count/search.
- Filtering queue resolution, playlists, favorites or named-queue snapshots.
- New Room schema, dependency, permission or translated strings.
- Changing the existing minimum-duration semantics.

## Decisions

### Preference and UI

`maximum_song_duration` is an `Int` in default SharedPreferences, default 0. A second
`SeekBarPreference` follows the minimum row:

- 0..7200 seconds, 60-second step, 0 = no limit,
- value shown, existing bottom-widget layout.

`LibraryPreferencesFragment` validates both sliders:

- max > 0 and max < minimum → reject and show `duration_filter_range_invalid`;
- minimum > max when max > 0 → reject with the same message;
- max = 0 accepts any minimum.

Rejection prevents an accidentally empty library. Changes apply on the next library query; list
screens already reload on resume.

### Query policy

`SongDurationFilterPolicy` is Android-free and returns inclusive fragments:

- min > 0: `DURATION >= min * 1000`
- max > 0: `DURATION <= max * 1000`
- 0/negative bounds contribute nothing.

`makeSongCursor` applies the fragments in minimum-then-maximum order, then the existing
whitelist/blacklist selections. Both bounds compose with `AND`; `max=0` is behaviorally identical to
today. NULL durations are excluded by an active maximum, as they already are by an active minimum.

Both cursor overloads accept `skipDurationFilter: Boolean = false`. The resolution paths pass
`true`: queue resolution (`songsByMediaItems`), `songByMediaItem`, `songByFilePath`, `song(songId)`,
file-provider URI resolution, the device-playlist `SortedCursorUtil` query and
`GenreRepository.getSongsWithNoGenre`. This explicit call-site guard prevents the duration range
from being applied transitively through their shared cursor use. Delete cleanup resolves through
`song(songId)` and is therefore covered.

### Scope

Only the shared song cursor is filtered:

- filtered: songs, albums/artists and their details, years, folders, home/search song lists and
  album/artist/year/folder details;
- unfiltered: queue resolution, `songByMediaItem`, `songByFilePath`, `song(songId)`, file-provider
  URI resolution, the device-playlist sorted-cursor path and `getSongsWithNoGenre`, all via
  `skipDurationFilter = true`; plus device/Room playlists and favorites;
- residual: genre detail lists, genre search and genre counts still use raw genre-member queries.

Raw genre queries are intentionally not changed in this scope. `MISSING_FEATURES` records the
residual explicitly.

## Risks / Trade-offs

- A maximum below the minimum would yield no songs; UI validation prevents it.
- Albums/artists/folders can disappear when all their songs fall outside the range; this matches
  aggregation semantics.
- Open album/artist detail screens may remain stale after a settings change, as with the existing
  minimum value; list screens reload on resume.
- `songById`/`songByFilePath` use the filtered cursor, so filtered-out long files may fail to
  resolve through those paths (existing minimum behavior).
- Genre detail/count/search remain inconsistent with the library by design until a separate
  refactor.

## Verification

- Pure `SongDurationFilterPolicy` probe: off, maximum-only, min+max, inclusive boundaries, invalid
  ranges, defensive values and no Android imports.
- Static source probe: preference/XML/default, both predicates in `makeSongCursor`, AND composition,
  validation listeners, queue/playlist/favorite exclusions, main-only strings and no schema/deps.
- ktlint, `git diff --check`, OpenSpec strict.
- Build/device runtime deferred until the QA lease is granted; no emulator starts from this change.
