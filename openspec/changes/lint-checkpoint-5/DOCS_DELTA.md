# Documentation delta - lint-checkpoint-5

None. Lint-only API guard with no behavior, feature or architecture change.

## Decisions

- Used a real inline `Build.VERSION.SDK_INT` guard in `requestWriteAccess` instead of an API
  contract annotation or a lint suppression: both callers already route API 26-29 to the direct
  write path, and the guard makes the API 30 requirement explicit to lint and to readers.
