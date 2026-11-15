# Tasks: Manual statistics CSV export

> Status 2026-11-01: implementation complete; JDK 21 assemble/lint gate PASS. Device/runtime testing was intentionally not run for this bounded implementation task.

## 1. Statistics data

- [x] 1.1 Add `SongPlayStat` and `Repository.songPlayStats(period)` without changing the existing screen flows.
- [x] 1.2 Implement `RealSmartRepository.songPlayStats` with all-time aggregate counts, period event counts, the 100-row cap, screen ordering and read-only missing-file filtering (never deleting play-count rows).
- [x] 1.3 Reuse `ArtistPlayStat` for Top Artists rather than redefining artist percentage semantics.
- [x] 1.4 Add an explicit OpenSpec read-only export requirement and scenario.

## 2. CSV and SAF export

- [x] 2.1 Add a pure `StatisticsCsvWriter` with exact headers, CRLF, UTF-8-safe content, RFC 4180 text quoting and formula-injection neutralization.
- [x] 2.2 Add `StatisticsCsvExporter` using the Koin `Repository` and supplied `ContentResolver`, with IO query/write, empty detection, cancellation rethrow and partial-document cleanup.
- [x] 2.3 Use `ActivityResultContracts.CreateDocument("text/csv")` with period-based suggested names.

## 3. UI and resources

- [x] 3.1 Add the Export statistics action to the detail-list menu and show it only for Top Tracks/Top Artists.
- [x] 3.2 Wire the fragment lifecycle, empty/failure/success toasts, cancel no-op and pending-document recreation handling.
- [x] 3.3 Add English strings only in `values/strings.xml`; do not add translated resources.

## 4. Documentation and OpenSpec

- [x] 4.1 Add this OpenSpec change and validate it strictly.
- [x] 4.2 Update `FEATURES`, `MISSING_FEATURES` and `CODE_MAP`.

## 5. Verification

- [x] 5.1 Run ktlint format/check on changed Kotlin files.
- [x] 5.2 Run `git diff --check`.
- [x] 5.3 Run `:app:assembleGithubDebug :app:lintGithubDebug` on JDK 21 with `--no-daemon`.
- [x] 5.4 Record the debug APK path, SHA-256 and size.
  - `app/build/outputs/apk/github/debug/ssilencce-1.4.0-beta.3.debug-github-universal.apk`
  - SHA-256 `EED525190F90F1118FC2F0962842B8DD2379095DADBFD7FEC5BDE82E1DF35495`, 53,383,292 bytes
