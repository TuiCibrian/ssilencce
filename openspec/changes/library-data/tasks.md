# Tasks

- [x] Read `disc_number` from MediaStore (API 30+) into the `Song` model.
- [x] Sort album songs by disc then track and expose the disc on subclasses/mappers.
- [x] Show "Disc N" section headers in album detail for multi-disc albums.
- [x] Add the default-off "Use album artist when artist is missing" preference and settings row.
- [x] Resolve missing/unknown song artists through the album-artist tag in display, search and
      artist grouping (including playback lookup for the synthetic artist group).
- [x] Add "Date last played", "Most played" and "Least played" sort keys and menu items.
- [x] Cache play statistics in memory and use them for the new sort keys.
- [x] Store the last 20 global search queries in preferences.
- [x] Show removable search-history chips and a clear-all action when the query is empty.
- [x] Apply the duration filter to genre detail song lists (including search).
- [x] Apply the duration filter to genre song counts and the unknown-genre list.
- [x] Run the compile gate and self-review the diff.
