# ADR-023: Built-in Updater via GitHub Releases

## Status

Accepted

## Context

The github flavor needs an in-app update mechanism (check for new releases, notify, download
and open the installer). The release pipeline publishes per-ABI/per-flavor APKs (ADR-005), so
the updater must pick the right asset, compare versions, and hand off the download to the
system download manager for reliability (downloads survive process death).

## Decision

- Query GitHub releases through `GitHubService` (Ktor, ADR-020) using the flavor-provided
  `GITHUB_API_URL` from `public.properties` (currently pointing at upstream
  `mardous/BoomingMusic` — fork debt, see ADR-001); support an optional auth token for private
  rate-limit headroom.
- Resolve the newest downloadable release (`GitHubRelease.latestRelease`): stable release
  first, then paginated prereleases filtered by `publishedAt`, with fallback to stable when no
  newer APK exists.
- Match assets per ABI with `Build.SUPPORTED_ABIS` priority and universal-APK fallback;
  filter by flavor and compare versions with `g00fy2` `versioncompare`.
- Download via `DownloadManager.Request` (system service, visible notification, resume
  support) and track the download; offer "ignore this release" persisted in preferences.
- Drive state with `UpdateViewModel` + `UpdateSearchResult` (Idle/Searching/Completed/Failed)
  with dedupe (`shouldStartNewSearchFor`) and persist `lastUpdateSearch`/`lastUpdateId` in
  SharedPreferences.
- Render the changelog as markdown in `UpdateDialog` and launch the install with the
  FileProvider URI.
- Gate the feature per flavor: only `github` sets `enable_builtin_updater=true` (ADR-003).

## Consequences

- Users on the GitHub channel get reliable, system-managed APK downloads and installs.
- Version comparison and ABI matching are centralized, so the release naming scheme
  (ADR-005) and the updater can never drift silently.
- The updater depends on GitHub API availability and rate limits; the dedupe/ignore logic
  keeps it polite.
- The fork must re-point `GITHUB_API_URL` to its own repository before publishing, otherwise
  users are offered upstream-signed APKs that fail installation.
