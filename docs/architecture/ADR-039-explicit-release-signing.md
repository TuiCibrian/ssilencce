# ADR-039: Explicit Release Signing (No Silent Debug Fallback)

## Status

Accepted on 2026-10-16. Supersedes the silent debug-keystore fallback part of ADR-005 (release delivery).

## Context

Release signing read `keystore.properties` and, when the file was missing, silently fell back to the Android debug keystore so that local and CI builds kept working. ADR-005 already recorded this as a latent footgun: a release artifact built without the file looked normal but was debug-signed and could not be updated over a properly signed install. Silent substitution of the signing identity is unacceptable for anything that might be distributed.

## Decision

- When `keystore.properties` exists, release signing uses it as before (`keyAlias`, `keyPassword`, `storePassword`, `storeFile`), and a configured `storeFile` that does not exist fails configuration with a `GradleException` instead of being ignored.
- Without `keystore.properties`, release packaging tasks are refused: a `gradle.taskGraph.whenReady` guard rejects tasks whose resolved name starts with `assemble`, `bundle` or `install` and ends with `Release` (this also catches aggregated/abbreviated invocations).
- A mechanical backstop supports the guard: the release build type is wired to a `releaseUnconfigured` signing config that points at the (missing) root `keystore.properties`, so any packaging path that still runs `validateSigning` fails rather than producing an unsigned/debug-signed artifact.
- The only way to build a release artifact without configured release signing is an explicit local opt-in: `-PallowDebugSigning=true` (Gradle property) or `SSILENCCE_ALLOW_DEBUG_SIGNING=true` (environment, exact value). The opt-in logs a warning that the artifacts are NON-PRODUCTION, signed with the Android debug certificate, and must not be distributed.
- Debug builds, lint, IDE/sync and configuration remain usable without a keystore.
- CI tag releases are unaffected: `.github/workflows/release.yml` recreates `keystore.properties` from repository secrets (`SIGNING_PROPERTIES`, `SIGNING_KEY`) before building, so configured signing is always present there.

## Consequences

- Forgetting `keystore.properties` can no longer produce a distribution-candidate release quietly; the build fails with an actionable message (create the file or pass the opt-in).
- Local release testing is still possible, but it is an explicit, loudly warned decision rather than a default.
- CI release behaviour is unchanged as long as the signing secrets stay configured; removing them would now fail the release workflow instead of yielding debug-signed artifacts.
- This supersedes the silent-fallback decision bullet of ADR-005; the rest of ADR-005 (ABI splits, CI naming, metadata stripping) still stands.
