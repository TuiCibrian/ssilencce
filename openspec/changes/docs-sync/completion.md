# docs-sync completion

DONE

## Scope

Applied every `openspec/changes/*/DOCS_DELTA.md` against the final integrated code and reconciled
overlapping drafts (crossfade/Cast/screencast numbering, notification-plus/notification-styles,
mono/crossfade/song-count availability, album-art/disable-loading, lyrics long-press ownership,
sleep-timer/playback fade paths, extra-format/ghost filtering). Obsolete unavailable-feature
claims from earlier drafts were not restored.

No code behavior, build files, versions, README, contributor data, translations or
`docs/MUSICOLET_FEATURES.md` were changed. No commits or history changes were made.

## Changed docs

- `AGENTS.md` — Repository Code Map brought to Room v9/12 entities/10 DAOs, Cast, crossfade,
  notification, extractor, ghost, lock-screen, mini-player, notes, setup, folder, queue, shuffle,
  tempo and UI additions; ADR range updated to ADR-065.
- `docs/FEATURES.md` — all 28 feature briefs reflected across playback, sound, lyrics, library,
  search, Now Playing, network, system UI, metadata and utilities; backup content count now ten
  types.
- `docs/MISSING_FEATURES.md` — implemented gaps removed/reworded; lock-screen section renumbered
  and downstream section numbers corrected; precise APE/WMA, custom-notification Android 13+,
  Cast decode-on-device and per-screencast-EQ limitations retained.
- `docs/CODE_MAP.md` — database entry updated to v9/migration 8→9; all new source files
  documented in the existing per-file format; integration updates recorded for modified files and
  resource/new-screen entries.
- `docs/AURORA_REFERENCE.md` — manual lyrics timing, crossfade, mono, song-count timer and named
  queues removed from unavailable claims; equalizer/lock-screen/sleep-timer capability rows
  updated.
- `docs/agents/domain.md` — actual count is now 65 ADRs (ADR-001 through ADR-065); ADR-051
  partially-implemented status recorded.

## ADRs

Created `ADR-052` through `ADR-065`; existing ADR-001 through ADR-051 preserved.

- ADR-052 dual-player crossfade
- ADR-053 flavor-scoped Chromecast
- ADR-054 presentation-based screencast
- ADR-055 coordinated playback volume and gapless gate
- ADR-056 three-state shuffle and randomize
- ADR-057 external-audio mini player and connect/mute policies
- ADR-058 in-track A-B repeat and per-song tempo
- ADR-059 extra audio container extractors
- ADR-060 ghost-mode library filter
- ADR-061 first-run setup wizard and process-scoped rescan
- ADR-062 folder mutations across scoped storage
- ADR-063 entity menu customization filter
- ADR-064 custom media notification provider
- ADR-065 custom notification styles

ADR-019 received its integrated sleep-timer amendment; ADR-051 status/decision updated to the
partially implemented journal/rollback/undo design.

## Cleanup

Deleted all 42 `STATUS` files under `openspec/changes/` (feature, lint, build, merge and fix
statuses), including auxiliary merge lanes. No replacement STATUS was created for docs-sync.
