# ADR-020: Shared Ktor/OkHttp Networking with Response Caching

## Status

Accepted

## Context

The app talks to many external APIs (Deezer, Last.fm, ListenBrainz, MusicBrainz, GitHub,
iTunes/Lyrically, BetterLyrics, LRCLib) for covers, metadata, scrobbling, lyrics and updates.
Without shared HTTP infrastructure, each client would duplicate connection handling, timeouts,
caching and offline behavior. Battery and data usage matter on a music player.

## Decision

- Use Ktor 3.5.1 as the HTTP client layer (`ktor-client-core` + OkHttp engine +
  `ktor-serialization-kotlinx-json` + `ContentNegotiation` + `ContentEncoding`), configured
  once in `KtorClient` and provided via Koin.
- Build the underlying OkHttp client with a shared 10 MB disk cache and two interceptors:
  - a cache-control interceptor applying a 4-day max-age and 28-day max-stale policy
    restricted to Deezer/Last.fm URLs (stable metadata),
  - an offline interceptor gating requests on `NetworkFeature.isOnline` (wifi/cellular policy).
- Identify the app on every service with a single `USER_AGENT` (`"ssilencce/<version>"`,
  `util/Constants.kt`); MusicBrainz requires it by policy.
- Prefer per-service timeouts for interactive lyrics endpoints (5 s connect / 10 s socket /
  15 s request in the lyrics providers) over global settings.
- Gate network-dependent features through `NetworkFeature` (images, lyrics APIs, Last.fm,
  ListenBrainz, updater) with per-feature availability checks and the only-wifi preference.

## Consequences

- One place to tune cache/offline behavior; metadata lookups (covers, wiki) are cheap on
  repeat visits and work offline when previously fetched.
- The 10 MB OkHttp cache plus Coil's own caches bound disk usage; cache eviction is LRU.
- The OkHttp engine brings the ecosystem's HTTP robustness (redirects, gzip, TLS) to Ktor.
- Feature gating lives in one model, so offline-first behavior is consistent across lyrics,
  covers and scrobbling rather than per-screen.
