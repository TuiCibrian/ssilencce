# Tasks

- [x] `LyricsScreen`: resolve the clipboard label (`R.string.lyrics`) with
      `stringResource` during composition instead of `LocalContext.current.getString` in the
      copy callback.
- [x] `LyricsScreen`: resolve the share subject fallback (`R.string.lyrics`) and the chooser
      title (`R.string.action_share`) with `stringResource` during composition, then capture
      them in the share callback.
- [x] Compile gate and `:app:lintGithubDebug` green (no `LocalContextGetResourceValueCall`
      remaining).
