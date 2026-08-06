# ADR-002: Semantic Versioning Enforced at Build Time

## Status

Accepted

## Context

The project ships to multiple stores (GitHub releases, F-Droid, Play Store) and the built-in
updater (ADR-023) compares versions. `versionCode` must always increase and be consistent with
`versionName`. Manual maintenance of two unrelated numbers (`versionCode` int + `versionName`
string) has historically caused drift and broken update checks.

## Decision

- Model versions as sealed Kotlin classes in `app/build.gradle.kts`:
  `Version.Alpha`, `Version.Beta`, `Version.RC`, `Version.Stable`.
- Derive `versionName` and `versionCode` from the single `Version` instance:
  - `versionName` = `major.minor.patch` with `-type.build` suffix for pre-releases.
  - `versionCode` = `major*1_000_000 + minor*100_000 + patch*10_000 + typeOffset*100 + build`
    where type offsets are Alpha=0, Beta=1, RC=2, Stable=3.
- Enforce consistency at configuration time: `check(versionCode == currentVersionCode)` fails
  the build if `versionCode` and the derived code diverge.
- Reserve version changes for the core team (AGENTS.md rule 8); agents and contributors must
  never bump the version.
- Current version at the time of writing: `Version.Beta(1, 4, 0, 3)` → `1400103`.

## Consequences

- A single source of truth for the version; drift between `versionCode` and `versionName` is
  impossible by construction.
- Pre-release ordering (alpha < beta < rc < stable) is encoded in the numeric code, which is
  compatible with Play Store monotonicity requirements and the updater's comparisons.
- Version bumps are compile-time audited (`check`), but the formula makes version numbers
  large and tied to the sealed class hierarchy; adding new release types requires changing
  the formula and constants in one place.
