# ADR-035: Restore the pre-Aurora UI

## Status

Accepted on 2026-10-03. Supersedes ADR-033 and ADR-034. Partially superseded by ADR-036 for the newly authorized, optional native Aurora appearance; the restoration remains the non-Aurora baseline.

## Context

The collaborator requested restoration of the application to its state before any aesthetic
changes on `feature/rebrand-ui`. The reference is commit
`43862e38c5cb308cd388c871214a4b6d1385a6bc`, the branch's common ancestor with `develop`,
immediately before the Aurora Glass implementation.

The branch introduced Aurora Glass in `0715febb`, expanded it in `a7b71263`, and replaced the
legacy player in `075b90d7`. Restoring colors alone would leave the new player structure,
resources and interactions in place rather than restore the requested application state.

## Decision

- Restore the application's source and resources to the reference commit, including the
  original static palettes, Material You default on Android 12+, shell, library, mini-player,
  search, settings, queue, lyrics and all seven player styles.
- Restore the View/XML player fragments, cover pager, transitions, gesture controller,
  custom controls, layouts and associated model/ProGuard definitions. Remove the Aurora
  components and Compose player introduced by this branch.
- Retain the hybrid UI policy in ADR-026, the player behavior in ADR-028 and the color
  model in ADR-029. Existing Compose screens remain Compose; this is a rollback, not a
  restriction on future Compose development.
- Preserve non-visual work, including the emulator autostart support in
  `scripts/mcp-emulator.ps1`. Do not change the application version, dependencies,
  playback engine, repositories, database, manifest, signing or translations.
- Restore `AGENTS.md`, `docs/CODE_MAP.md` and `docs/FEATURES.md` to describe the recovered
  implementation. Keep ADR-033, ADR-034 and the Aurora OpenSpec proposals as historical
  records, marked as superseded or reverted rather than current implementation targets.
- Back up the local `AuroraBackground.kt` changes outside the repository before removing
  that component. Leave the branch and Git history intact; do not commit or push.
- Do not clear application data or rewrite saved preferences. The removed Aurora animation
  setting is ignored; saved theme and player choices continue to use their original keys.

## Consequences

- The application returns to its pre-Aurora appearance and player implementation, while
  the earlier functional, security and accessibility improvements remain in place.
- The restored legacy player and Compose screens again coexist as described by ADR-026.
  Future new UI work still follows the repository's Compose-first rule.
- The Aurora OpenSpec proposals are no longer implementation targets. Resuming that design
  requires a new explicit request and reconciliation with this decision.
- Build and lint verification must include the recovered source files and layouts, including
  files deleted by the former Compose migration and now restored in the working tree.
