# Docs delta — library-data

Apply these changes from the docs agent. Do not copy this file into the final docs.

## Decisions

- Disc numbers are read from `MediaStore.MediaColumns.DISC_NUMBER` (Android 11 / API 30+). On older
  releases `discNumber` stays `0`, so those devices keep the previous flat track order and show no
  disc headers. Reading one TagLib file per row was rejected for scan/battery cost.
- Disc headers only appear when the album detail is sorted by Track (the default). Other sort keys
  keep a single flat list, and songs without a disc tag are grouped under "Disc 1".
- "Most played" sorts by descending play count by default (the sort direction toggle inverts it);
  "Least played" is ascending; "Date last played" is ascending (oldest/never first) and the toggle
  reverses it.
- Play statistics are joined from `PlayCountEntity` in one query and cached for 5 seconds; no
  per-song query is made.
- The search history stores at most 20 deduplicated queries in SharedPreferences. A query is stored
  when the user submits it (Enter) or opens a result; typing alone does not create history entries.
- The album-artist fallback is default-off. When enabled, missing/`<unknown>` song artists resolve
  to the album artist for display, notification/Media3 metadata, search and artist grouping.
  Fallback artist groups use a synthetic stable ID; `ArtistRepository.artist(id)` maps it back to
  the album-artist detail query so Android Auto playback still works.
- The search-history UI reuses Material chips in the existing XML search screen (no Compose screen
  was created for this small addition).

## docs/FEATURES.md

In §4 Library & Organization:

- Replace the **Sorting** bullet with:
  `- **Sorting** — Locale-aware numeric collator (natural number order), article-ignoring sort (The/A/El…) with a user-editable keyword list, per-entity sort keys with persistence, and filename, path, track-number, date-modified, date-last-played, most-played and least-played keys on the all-songs, folder-song, genre-song and year-song lists. Play-statistics keys join the cached play-count table in memory (5 s TTL).`
- Replace the **Library filtering** bullet with:
  `- **Library filtering** - Whitelist/blacklist folders (InclExclDao), minimum and maximum song duration (maximum 0 = unlimited; library queries only), minimum songs per artist/album. Duration bounds are library-browsing only: queue resolution, ID/path/file-provider resolution, delete cleanup, saved playlists and favorites are not duration-filtered. Genre detail lists, genre search, genre song counts and the unknown-genre list use the same duration bounds as the other library lists.`
- Add after the **Genre subcategories** bullet:
  `- **Multi-disc albums** — Album detail groups songs by disc and shows a "Disc N" section header when the album spans more than one disc and the list is sorted by Track. Songs without a disc tag are treated as disc 1.` 
- Add after the **Composer and album-artist tabs** bullet:
  `- **Album-artist fallback** — Default-off "Use album artist when artist is missing" preference: songs whose artist tag is empty or unknown resolve to the album artist for list/player/notification display, global search and artist grouping; fallback artist groups open the album-artist detail and remain playable from Android Auto.`

In §6 Search:

- Replace the **Global search** bullet with:
  `- **Global search** — Text field, voice input, 300 ms debounce, grouped results (albums, artists, songs, playlists, genres), and the last 20 submitted queries shown as removable chips (plus Clear history) while the query field is empty.`

## docs/MISSING_FEATURES.md

- Remove from Part 1 §5 (Library & browsing):
  - `- **Play-statistics sort keys** — …`
  - `- **Disc number** support.`
  - `- **"Use album-artist if artist missing"** fallback.`
  - `- **Genre duration-filter parity** - …`
- In Appendix A, update the sort-key comparison bullet: ssilencce now also exposes date-last-played,
  most-played and least-played song sort keys, so only Musicolet-specific/experimental options
  remain missing.

## docs/CODE_MAP.md

Add entries:

- `core/model/ArtistNamePolicy.kt` — Pure fallback policy for missing/`<unknown>` artist names;
  returns the album-artist tag when enabled. Consumed by `Song` display extensions, navigation and
  `RealArtistRepository`.
- `core/sort/SongPlayStatisticsCache.kt` — 5-second in-memory snapshot of `PlayCountDao`
  statistics used by the play-statistics sort keys; one joined query per snapshot, no per-row
  queries.
- `data/local/room/SongPlayStatistics.kt` — Query projection (`id`, `playCount`, `timePlayed`) for
  song-play-statistics sorting. Not a Room entity; no schema/version change.

Update entries:

- `data/model/Song.kt` — Add `discNumber` (MediaStore `disc_number`, API 30+) to the model, copy
  constructor, equality/hash/toString and Media3 `artist` metadata (resolved artist name).
- `data/model/Artist.kt` — Add `nameOverride` for fallback artist groups and
  `albumArtistFallbackId(name)` for synthetic stable IDs.
- `data/model/ExpandedSong.kt`, `data/model/QueueSong.kt` — Carry `discNumber` through the
  subclass/queue snapshots.
- `data/local/repository/SongRepository.kt` — Project and parse `MediaColumns.DISC_NUMBER` on
  API 30+; global song search includes `ALBUM_ARTIST` when the album-artist fallback is enabled.
- `data/local/repository/AlbumRepository.kt` — Opening-song selection follows disc-then-track order.
- `data/local/repository/ArtistRepository.kt` — Album-artist fallback grouping/display, search by
  album artist, synthetic-ID lookup for playback, and a concurrent fallback-name map.
- `data/local/repository/GenreRepository.kt` — Genre song lists/search/counts and the unknown-genre
  list honor the min/max duration filter; the genre cover lookup stays unfiltered.
- `data/local/room/PlayCountDao.kt` — Add the `allPlayStatistics()` projection query.
- `core/sort/SortMode.kt` — Disc-then-track album ordering, new play-statistics sort keys, and the
  stats cache hook.
- `core/model/sort/SortKey.kt`, `core/model/sort/SortItem.kt` — New `DateLastPlayed`, `MostPlayed`
  and `LeastPlayed` keys/menu items.
- `extensions/media/DataExt.kt` — `Song.resolvedArtistName()` applies the fallback before display
  and unknown-artist checks.
- `extensions/navigation/NavigationExt.kt` — Artist navigation prefers the album artist when the
  fallback applies.
- `util/Preferences.kt` — `useAlbumArtistIfArtistMissing` and the bounded, deduplicated
  `searchHistory` helpers.
- `ui/screen/library/albums/AlbumDetailFragment.kt` — Builds one song adapter per disc plus
  section headers; quick search and compact-view toggling update all disc groups.
- `ui/screen/library/search/SearchFragment.kt` — Renders removable search-history chips, commit on
  submit/result click and the empty-state interplay.
- `res/layout/fragment_search.xml` — Search-history chip scroller and Clear history button.

## AGENTS.md

In the condensed Repository Code Map:

- `core/model/` — add `ArtistNamePolicy` (album-artist fallback).
- `core/sort/` — add `SongPlayStatisticsCache` (cached play-stat sort data).
- `data/local/room/` — add `SongPlayStatistics` query projection (no schema change).
- `data/model/` — note `Song.discNumber` and `Artist.nameOverride`/fallback IDs.

## docs/agents/domain.md

No change: no new ADR, doc count unchanged.

## ADR

No new ADR required (no stack/layering/persistence/playback-engine/navigation decision changed).
