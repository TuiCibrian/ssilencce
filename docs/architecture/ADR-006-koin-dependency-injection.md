# ADR-006: Koin as Dependency Injection Framework

## Status

Accepted

## Context

The app has a large object graph: Room database + DAOs, MediaStore repositories, Ktor clients,
playback components (ExoPlayer processors, equalizer manager, sleep timer), image managers and
ViewModels. The legacy codebase relied on manual singletons and `KoinComponent` lookups
scattered through models and utilities. The team needed a lightweight, Kotlin-first DI container
that works with both legacy Views and Compose.

## Decision

- Use Koin 4.2.2 (`koin-core`, `koin-android`, `koin-androidx-compose`) as the DI framework,
  started from `App.startKoin(appModules)`.
- Organize definitions in `MainModule.kt` into layered modules:
  - `networkModule`: Ktor/OkHttp clients, remote services (Deezer, Last.fm, ListenBrainz,
    MusicBrainz, GitHub, lyrics providers).
  - `roomModule`: `SsilencceDatabase` (v6, migrations 1→6) and all DAOs.
  - `mainModule`/`dataModule`: repositories, playback components, media writers, image managers.
  - `viewModule`: ViewModels via the Koin `viewModel` DSL.
- Bind interfaces to `Real*` implementations (`RealSongRepository`, `RealPlaylistRepository`,
  `RealLyricsRepository`, `NetworkRepositoryImpl`, ...) so consumers depend on contracts.
- Scope: `single` for services/repositories/playback singletons (shared processors, see
  ADR-014), `factory` for per-use components, `viewModel` for UI state holders.
- Keep `KoinComponent`/`inject()` available for non-framework classes that need the graph
  without constructor injection (e.g. `Preferences`, `Playlist`, `GitHubRelease`, adapters).

## Consequences

- Fast startup, no code generation, and a single place (`MainModule`) to audit the graph.
- ViewModels are obtainable from both legacy Fragments (`getViewModel`) and Compose
  (`koinViewModel`) with activity-scoped sharing where needed.
- `KoinComponent` lookups inside models and utilities make dependencies less explicit; they
  are accepted for pragmatic legacy reasons but new code should prefer constructor injection.
- Switching to another DI framework (e.g. Hilt) would require touching every definition site
  and the legacy `KoinComponent` usages — not planned.
