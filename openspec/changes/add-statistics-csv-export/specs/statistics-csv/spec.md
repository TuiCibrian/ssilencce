# Statistics CSV export

## Purpose

Let users export the currently selected Top Tracks or Top Artists period as a portable CSV
file without granting storage permissions or changing the statistics screen's semantics.

## ADDED Requirements

### Requirement: Manual export is scoped to statistics screens

The system SHALL expose an Export statistics action only for `ContentType.TopTracks` and
`ContentType.TopArtists`, SHALL launch the Storage Access Framework `CreateDocument` contract
with MIME type `text/csv`, and SHALL request no storage permission.

#### Scenario: Top Tracks action
- **WHEN** the user opens the options menu on the Top Tracks screen
- **THEN** the Export statistics action is visible and opens a CSV document picker

#### Scenario: Non-statistics action hidden
- **WHEN** the user opens the options menu on a non-statistics detail list
- **THEN** the Export statistics action is not visible

#### Scenario: Cancel the picker
- **WHEN** the user cancels the CSV document picker
- **THEN** no file is written, no success message is shown, and statistics data is unchanged

### Requirement: Export reflects the selected period and screen ranking

The system SHALL export the currently selected `ListeningPeriod`, SHALL cap the export at the
top 100 rows, and SHALL preserve the same ordering and data semantics as the statistics
screen. All-time Top Tracks SHALL use aggregate `PlayCountEntity` play counts; bounded periods
SHALL use `PlayEventEntity` counts. Top Artists SHALL use the existing `ArtistPlayStat`
recorded-name, play count, percentage and period denominator semantics.

#### Scenario: Period changes
- **WHEN** the user selects This month and exports Top Tracks
- **THEN** the document contains that period's top 100 tracks in screen order with their
  period event play counts

#### Scenario: All-time tracks
- **WHEN** the user exports Top Tracks with All time selected
- **THEN** the document uses aggregate play counts and the same ordering as the screen

#### Scenario: Artist percentages
- **WHEN** the user exports Top Artists
- **THEN** each row contains the recorded artist name, period play count and the existing
  integer percentage derived from the full period denominator

### Requirement: Export is read-only

The statistics export query SHALL be strictly read-only. It SHALL NOT delete `PlayCountEntity`
rows, including rows whose song id is `-1` or whose media file is missing. Missing-file rows
SHALL only be filtered out of the exported rows. Existing screen cleanup paths SHALL remain
unchanged.

#### Scenario: Missing-file row during export
- **WHEN** a play-count row has no current media file
- **THEN** the row is omitted from the CSV and the play-count row remains in Room

#### Scenario: Export does not mutate statistics
- **WHEN** an export is prepared, canceled, or fails before writing
- **THEN** the number of play-count rows is unchanged

### Requirement: CSV bytes and fields follow the export contract

The system SHALL write UTF-8 without a byte-order mark, SHALL use CRLF row endings, and SHALL
emit the headers `Rank,Title,Artist,Album,DurationMs,Plays` for Top Tracks and
`Rank,Artist,Plays,Percentage` for Top Artists. Ranks SHALL be 1-based. Text fields SHALL be
RFC 4180 quoted only when they contain a comma, quote, carriage return or line feed, with
embedded quotes doubled. Numeric columns SHALL remain plain. Text fields whose first
non-space character is `=`, `+`, `-` or `@` SHALL be prefixed with a single apostrophe.

#### Scenario: Formula-like title
- **WHEN** a track title starts with `=SUM(A1:A2)`
- **THEN** the text field is prefixed with an apostrophe and quoted only if also required by
  CSV escaping

#### Scenario: Comma and quote in text
- **WHEN** a title contains a comma or quote
- **THEN** the field is enclosed in double quotes and embedded quotes are doubled

#### Scenario: Numeric values
- **WHEN** rank, duration, plays or percentage values are written
- **THEN** they are plain numeric fields without quoting

### Requirement: Empty, failed and interrupted exports are safe

The system SHALL show a clear toast and SHALL NOT launch the picker when the selected period
has no rows. Query and write work SHALL run off the main thread. The system SHALL rethrow
coroutine cancellation, SHALL catch other failures, SHALL attempt to delete a partially
written selected document, and SHALL report failure without crashing.

#### Scenario: Empty period
- **WHEN** the selected statistics period has no rows
- **THEN** the app shows the existing no-plays toast and does not open the document picker

#### Scenario: Write failure
- **WHEN** the selected document cannot be written completely
- **THEN** the app best-effort deletes it, shows the failure toast, and does not crash or show
  a success message

#### Scenario: Cancellation
- **WHEN** the export coroutine is canceled during writing
- **THEN** cancellation is rethrown after a best-effort partial-document deletion
