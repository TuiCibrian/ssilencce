# Tasks

- [x] Add `media3-cast` to the version catalog at the existing media3 version.
- [x] Add `src/cast` (github + playstore) and `src/nocast` (fdroid) source sets.
- [x] Add cast flavor manifest with the Cast options provider.
- [x] Add `CastPlayback` API and `CastTrack` descriptor in the main source set.
- [x] Implement `CastPlayer` + Cast SDK session handling (github/playstore).
- [x] Implement a no-op `CastPlaybackProvider` for fdroid.
- [x] Implement the LAN HTTP server (audio with range requests + artwork).
- [x] Add the `action_cast` menu entry and Cast icon.
- [x] Wire queue/position transfer between ExoPlayer and CastPlayer through
      `MediaSession.setPlayer`.
- [x] Route cast queue additions (`onAddMediaItems`/`onSetMediaItems`) through the
      HTTP rewriter.
- [x] Route transport, repeat/shuffle and favorite commands to the active player.
- [x] Keep widgets, media button layout and sleep timer aware of the active player.
- [x] Add user-facing strings (English only).
- [x] Write DOCS_DELTA.md for the docs agent.
- [x] Compile gate (`:app:compileGithubDebugKotlin`).
