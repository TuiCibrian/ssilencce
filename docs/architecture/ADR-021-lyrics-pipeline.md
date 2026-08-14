# ADR-021: Unified Lyrics Pipeline

## Status

Accepted

## Context

Lyrics arrive from many origins — embedded ID3/MP4 tags, local `.lrc` files, remote providers
(Lyrically/Apple Music, BetterLyrics, LRCLib) and user edits — in two shape families (plain
text and time-synced LRC/TTML), with optional translations, transliterations, syllable
timings, background vocals and instrumental tracks. Rendering all of that consistently needs a
single intermediate model plus per-origin parsers, caching and download orchestration.

## Decision

- Adopt a unified domain model `SyncedLyrics` (`data/model/lyrics/`) with lines, per-syllable
  words, translations, transliterations, actor metadata (`LyricsActor`: v1/v2/v3/M/F with
  background variants) and a validated timeline; plain lyrics are represented as a degenerate
  synced form.
- Parse with two dedicated parsers behind the `LyricsParser` interface:
  - `LrcLyricsParser`: standard + enhanced LRC (multiple timestamps, word-sync, `[offset:]`,
    translations) via a two-pass node model (`LrcNode`),
  - `TtmlLyricsParser`: Apple Music-style TTML via `XmlPullParser` building a node tree
    (`TtmlNodeTree`) with agents, accompaniments (translation/transliteration) and clock/offset
    time expressions.
- Orchestrate downloads in `LyricsDownloadService`: iterate `LyricsApi` implementations
  (Lyrically, BetterLyrics, LRCLib) in order, gated by `NetworkFeature` per provider, merge
  results via `RawLyrics.Remote.accept`, and stop once both plain and synced content are
  available.
- Provider strategies:
  - Lyrically: iTunes search via `PaxsenixSearchHelper` (flavor-swapped, ADR-003) + Jaro-Winkler
    title scoring + duration, converting syllable/line JSON to LRC,
  - BetterLyrics: TTML endpoint with strict timeouts,
  - LRCLib: search with ±2 s duration matching, plain-lyrics fallback and instrumental flag.
- Cache results in Room (`LyricsEntity`: text, provider, instrumental flag) and in-memory via
  an `LruCache`; detect instrumental tracks heuristically (`InstrumentalDetector`: title
  keywords or very short exact lyrics).
- Detect file encoding with juniversalchardet `UniversalDetector` unless UTF-8 is forced.
- Persist edits/saves through `MetadataWriter` (embedded tags) or MediaStore write requests;
  expose file-based lyrics as read-only sources.
- Render with a single Compose `LyricsView` (karaoke/syllable highlight, translations,
  actor alignment) driven by `LyricsViewState` computed from playback position
  (ADR-028/029).

## Consequences

- Any origin can feed any renderer: the UI never switches on source format, only on the
  unified model.
- TTML (Apple Music) support is a differentiator for synced lyrics quality.
- Multi-provider fallback improves hit rate but increases code surface per provider; providers
  are behind a common interface so adding one is additive.
- Lyrics cache is app-local (Room), so it survives song deletion inconsistently if not pruned;
  backups (ADR-031) include lyrics JSON.
