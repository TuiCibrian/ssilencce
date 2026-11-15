# library-data

## Why

The library layer is missing several long-standing data capabilities: multi-disc albums are
flattened into a single track list, songs with missing artist tags are not recoverable from
their album-artist tag, song lists cannot be ordered by play statistics, global search has no
recall of previous queries, and genre lists/counts ignore the configured song-duration filter.

## What Changes

- Read the MediaStore disc number into `Song`, order album songs by disc then track, and show
  per-disc section headers in album detail when an album spans more than one disc.
- Add a default-off preference that resolves a missing/unknown song artist from its album-artist
  tag for display, search and artist grouping.
- Add "Date last played", "Most played" and "Least played" song sort keys backed by a cached,
  in-memory join of the play-count statistics (no per-row queries).
- Keep the last 20 global search queries in preferences and show them as removable chips (with a
  clear-all action) while the query field is empty.
- Make genre detail lists, genre search, genre song counts and the unknown-genre list honor the
  minimum/maximum song duration filters.
