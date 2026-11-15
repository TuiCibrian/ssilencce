# Proposal: User-editable sort ignore-keywords

## Why

`MISSING_FEATURES.md` still lists the custom ignore-keyword whitelist: sorting only understands the
fixed per-language articles in `normalizeForSorting`. Users cannot add leading words such as “Los”
or “The Best” without changing the built-in article lists.

## What Changes

- Add a user-editable list of leading keywords used by sort normalization.
- Apply the list only while the existing **“Ignore articles when sorting”** switch is on. No
  dedicated toggle is added; the keyword preference row is disabled while that switch is off.
- Keep built-in article handling unchanged and apply custom keywords in addition to it.
- Persist the list as a `StringSet` in the existing `SharedPreferences`; default is empty.
- Add a Compose settings dialog to view, add and remove keywords, with count summary and explicit
  validation for blank, duplicate, too-long and full-list input.
- Use one pure `IgnoreKeywordPolicy`/`SortNormalizer` in the shared sorting path. Search and
  displayed metadata remain untouched.
- Do not change Room, dependencies, versioning or translated strings.

## Impact

- New: `core/sort/IgnoreKeywordPolicy.kt`,
  `ui/component/preferences/dialog/IgnoreKeywordsPreferenceDialog.kt`.
- Modified: `SortCollator.kt`, `SortMode.kt`, `MediaExt.kt`, `Preferences.kt`,
  `PreferencesScreenFragment.kt`, `preferences_screen_library.xml`, `values/strings.xml`, docs.
- Default empty list preserves the current behavior exactly.
