# Proposal: Most Played periods and artist statistics

## Why

The current Top Tracks and Top Artists views are all-time only. The documented gap asks for
week/month/12-month/year/all-time periods and artist play percentages. `PlayCountEntity` stores
only a total count and the latest play timestamp, so it cannot answer period questions.

## What Changes

- Record each counted play as a row in a new `PlayEventEntity` with song id, artist name and
  timestamp, written alongside the existing play-count increment.
- Add DAO/repository queries for top songs and top artists over a cutoff, with play counts and
  percentages.
- Add a period selector to the Top Tracks screen and show artist play statistics with
  percentages on the Top Artists screen.
- Document honestly that period data starts when this version records events; pre-existing
  aggregate play counts are only available for all-time.

## Impact

- Affected code: new Room entity/DAO, `SsilencceDatabase` migration, `MainModule`,
  `SmartRepository`/`Repository`, `LibraryViewModel`, `DetailListFragment`,
  `ArtistAdapter` or a new stats adapter, menus/layouts, `strings.xml`.
- Docs: CODE_MAP, FEATURES, MISSING_FEATURES, ADR-018 amendment.
