# Tasks: Most Played periods and artist statistics

> Status 2026-10-21: H1/H2 recorded-name attribution, full reactive totals and stable identities pass 47 actual-SQL and 39 extracted Kotlin/Flow checks. Current-APK emulator verification of the revised H semantics has not been rerun.

## 1. Persistence

- [x] 1.1 Add `PlayEventEntity` and DAO queries for play counts by song and by artist over a cutoff.
- [x] 1.2 Add the Room v7 to v8 migration and register the DAO in `MainModule`.
- [x] 1.3 Record one event per counted play in the same transaction as the aggregate increment.

## 2. Repository and ViewModel

- [x] 2.1 Add period-aware top-song and artist-stat queries to `SmartRepository`/`Repository`.
- [x] 2.2 Expose period results and artist percentages through `LibraryViewModel`.
- [x] 2.3 Add a period enum with this week, this month, last 12 months, this year and all time.

## 3. UI and documentation

- [x] 3.1 Add a period selector to Top Tracks and reload real data for the selection.
- [x] 3.2 Show artist play counts and percentages on Top Artists for the selected period.
- [x] 3.3 Explain that period data begins when events start being recorded; all-time includes aggregate history.
- [x] 3.4 Add English strings, update CODE_MAP, FEATURES, MISSING_FEATURES and ADR-018.

## 4. Verification

- [x] 4.1 Verify migration, event insertion and period filtering with a real database/DAO probe.
- [ ] 4.2 Verify period selection and percentage rendering on the emulator.
- [x] 4.3 Run ktlint, `:app:assembleGithubDebug` and `:app:lintGithubDebug` on JDK 21.
