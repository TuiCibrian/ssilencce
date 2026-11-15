# Tasks

- [x] `AudioCutterScreen`: resolve the export, tone, write-settings and storage failure
      messages with `LocalResources.current` instead of `LocalContext.current.getString`;
      keep the long-lived event collector on the latest `Resources` via
      `rememberUpdatedState` so locale changes are reflected.
- [x] `SongNotesFragment`: resolve the bookmark-added toast with `LocalResources.current`
      and refresh it in the event collector via `rememberUpdatedState`.
- [x] Compile gate green (`:app:compileGithubDebugKotlin`).
- [x] `:app:lintGithubDebug` green (all twelve `LocalContextGetResourceValueCall` errors
      gone; report now 0 errors).
