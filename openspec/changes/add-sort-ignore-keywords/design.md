# Design: User-editable sort ignore-keywords

## Context

Sorting currently normalizes title/artist/album names through `normalizeForSorting`, which strips a
leading built-in article from a fixed per-language map (en/es/fr/de/it/pt/nl). `SortMode` and
`SortCollator` call this normalization from comparators; `asSectionName` uses it for the
FastScroller section letter. Search and displayed titles use raw fields.

## Goals / Non-Goals

**Goals:**

- Let users add leading keywords ignored by sort comparisons in addition to built-in articles.
- Preserve the existing article toggle as the only gate; default behavior stays unchanged.
- Persist without a database migration; keep the list bounded and safe for regex construction.
- Make the matching/normalization logic pure and probeable.

**Non-Goals:**

- A dedicated custom-keyword toggle.
- Applying custom keywords to search, displayed titles or metadata.
- Locale-specific translations of the new English UI strings.
- Arbitrary regular expressions; keywords are literal leading phrases.

## Decisions

### Existing toggle gate

Custom keywords are an extension of “Ignore articles when sorting”. When the switch is off, the
custom list is stored but not used and the settings row is disabled through
`app:dependency="ignore_articles_when_sorting"`. No new switch is added.

### Storage and limits

`Preferences.sortingIgnoreKeywords` is a `Set<String>` backed by the existing SharedPreferences
`StringSet` API (`sorting_ignore_keywords`). The setter and getter sanitize through
`IgnoreKeywordPolicy`: trim, collapse whitespace, drop blanks, deduplicate case-insensitively and
cap at 50 keywords of 40 characters each. The UI displays the set alphabetically; matching order
is determined by the normalizer, not the set order. No schema or dependency changes.

### Shared normalizer

`IgnoreKeywordPolicy` owns the built-in article map and a pure `SortNormalizer`:

- combines built-in articles for the locale with sanitized custom keywords;
- deduplicates case-insensitively and prefers longer phrases first;
- escapes every keyword with `Regex.escape`, then matches one leading keyword followed by
  whitespace, case-insensitively;
- returns the input unchanged when article ignoring is off or no keyword applies.

`SortMode` creates one normalizer per `sorted()` call and reuses it for all comparators;
`SortCollator.sortedByMediaName` does the same per call. `asSectionName` reads the same keyword set
through `SortMode.ignoreKeywords`. This avoids rebuilding the regex on every comparison and keeps
custom keywords out of search/display paths.

### Compose preference dialog

`IgnoreKeywordsPreferenceDialog` is a `DialogFragment` hosting a Compose surface. It shows the
current list, an add field and per-keyword remove actions, persists immediately, and reports
blank/duplicate/too-long/full errors. The settings row summary shows the keyword count. The row is
a `CustomDialogPreference`, like the existing category/headset dialogs, and is routed from
`PreferencesScreenFragment.onDisplayPreferenceDialog`.

## Risks / Trade-offs

- A custom phrase longer than a built-in article is preferred by length, so “The Best” removes the
  whole phrase instead of only “The”. This matches the user’s explicit keyword.
- Regex construction is bounded by the 50 × 40 limits and cached per sort operation.
- Built-in article behavior remains the default when the list is empty and the switch is off.

## Verification

- Pure JVM probe over `IgnoreKeywordPolicy`/`SortNormalizer` (built-in articles, custom keywords,
  escape handling, multi-word keywords, disabled gate, sanitization, add/remove errors and sorted
  output).
- Static source probe for Preferences, settings XML/dialog routing, SortMode/SortCollator/MediaExt
  wiring, search/display isolation and absence of schema/dependency/translated-string changes.
- ktlint, `git diff --check`, `openspec validate add-sort-ignore-keywords --strict`.
- Gradle assemble/lint and runtime UI checks are deferred until a build lease and emulator
  authorization.
