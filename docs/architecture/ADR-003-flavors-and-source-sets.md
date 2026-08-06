# ADR-003: Distribution Flavors and Shared Source Sets

## Status

Accepted

## Context

The app is distributed through three channels with different policy constraints: GitHub releases
(experimental, network features enabled), F-Droid (no network features by default, no built-in
updater) and the Play Store (no updater, no Lyrically provider access). Some features (Last.fm
API keys, Lyrically iTunes-backed lyrics search) are not available or not allowed on every
channel.

## Decision

- Define three product flavors on the `version` dimension in `app/build.gradle.kts`:
  - `github`: network features enabled by default, built-in updater enabled, Lyrically provider
    enabled.
  - `fdroid`: network features disabled by default, updater disabled, Lyrically provider enabled.
  - `playstore`: network features disabled by default, updater disabled, Lyrically provider
    disabled.
- Encode those switches as `resValue` booleans (`network_features_enabled_by_default`,
  `enable_builtin_updater`, `enable_lyrically_provider`) consumed via `NetworkFeature`.
- Share implementation between `github` and `fdroid` by adding `src/shared/java` to both source
  sets; `playstore` keeps its own stub:
  - `PaxsenixSearchHelper`: real iTunes Search client (Ktor) in `src/shared`, a stub always
    returning `null` in `src/playstore` — the flavor source-set swap is the mechanism that keeps
    the Lyrically provider from reaching the iTunes API on Play builds.
- Inject Last.fm API key/secret via `LASTFM_API_KEY`/`LASTFM_SECRET` from `local.properties` or
  environment into `BuildConfig` for `github` and `playstore` only; `fdroid` gets none, and
  `enable_lastfm_integration` resValue gates the feature at runtime.
- Per-flavor public configuration (download URL, donation link, GitHub API URL, Lyrically API
  URL) comes from `public.properties` with optional `flavor.`-prefixed overrides, injected as
  `BuildConfig` fields.

## Consequences

- One codebase covers three distribution policies without runtime branching for the provider
  itself; policy differences are visible and auditable in the build file.
- The flavor-swapped `PaxsenixSearchHelper` keeps the Lyrically provider off the Play Store
  while preserving the identical interface (`HttpClient`, `songTitle`, `artistName`).
- F-Droid builds lose the built-in updater and default-off network features, which is aligned
  with F-Droid policy.
- Every new network-dependent feature must decide its per-flavor default, adding a small
  surface area to the build configuration.
