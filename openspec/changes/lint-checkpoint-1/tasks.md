# Tasks

- [x] `GaplessGateAudioSink`: mark `setOffloadMode`/`setOffloadDelayPadding` overrides with
      `@RequiresApi(29)` to match the Media3 `AudioSink` contract (callers already guard API 29).
- [x] `LyricsScreen`: resolve A-B repeat toast messages with `stringResource` during
      composition instead of `LocalContext.current.getString` in click callbacks.
- [x] Compile gate and `:app:lintGithubDebug` green.
