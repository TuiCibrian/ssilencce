# ADR-001: Fork Identity

## Status

Accepted

## Context

ssilencce is a fork of Booming Music (`https://github.com/mardous/BoomingMusic`). The upstream
application uses its own application ID, so the fork needed to decide how to remain installable
alongside the upstream app and how to identify itself in the ecosystem. The application ID is
permanent once published on a store (e.g. Play Store). The AndroidManifest also declares an
`activity-alias` (`.activities.MainActivity`) to preserve icon-pack and CyanogenMod-Eleven
compatibility inherited from the upstream lineage.

## Decision

- Fork Booming Music and keep a close relationship with upstream (regular merges are expected).
- Change `applicationId` and `namespace` from the upstream value to `com.ssilencce` in
  `app/build.gradle.kts` (`defaultConfig`).
- Treat `com.ssilencce` as permanent; never change it after any store publication.
- Keep the `activity-alias` `com.ssilencce.activities.MainActivity` pointing at
  `.ui.screen.MainActivity` so launchers, icon packs, and `MUSIC_PLAYER`/`AUDIO_PLAYER`
  intent filters keep working.
- Keep upstream-branded resources (app name, launcher icon, store listings) out of the fork's
  code; branding lives in `metadata/` and is managed by the release process.
- Before publishing the fork, point the built-in updater at the fork repository instead of the
  upstream one (see ADR-023).

## Consequences

- The fork can be installed side by side with Booming Music.
- Renaming `applicationId` requires updating all `com.ssilencce.R` imports and manifest component
  references; both were migrated during the rebranding commits.
- The updater still pointed at upstream `mardous/BoomingMusic` at the time of writing; installing
  an upstream APK over the fork fails signature checks. This is known debt tracked in AGENTS.md.
- Keeping close to upstream reduces long-term maintenance cost for media playback and library
  features, but any divergence must be rebased carefully.
