# ADR-005: Release Delivery (Signing, ABI Splits, CI Naming)

## Status

Accepted. Partially superseded by [ADR-039](ADR-039-explicit-release-signing.md) (release signing).

## Context

The app bundles native decoders (media3-ffmpeg) and TagLib, so a single fat APK is large.
GitHub releases want per-ABI APKs while store uploads want bundles. Signing keys must never be
committed, and CI builds need distinguishable version names. AGP 8.9+ also broke ABI splits when
building bundles ("Sequence contains more than one matching element", issuetracker 402800800).

## Decision

- Enable ABI splits for `arm64-v8a`, `armeabi-v7a`, `x86_64`, `x86` with `isUniversalApk = true`,
  and **disable** splits when building a bundle or when `RELEASE_TYPE=CI` to work around the
  AGP 8.9+ issue.
- APK naming: `ssilencce-<versionName>-<flavor>-<abi>.apk` (`universal` when unfiltered).
- CI builds (`RELEASE_TYPE=CI`): disable splits and append the short git hash to the version
  name (`<version>.<gitHash>`); expose `BuildConfig.IS_CI_BUILD`.
- Release signing reads `keystore.properties` (`keyAlias`, `keyPassword`, `storePassword`,
  `storeFile`); there is no silent debug fallback — Release packaging tasks are refused without
  it unless the explicit local opt-in `-PallowDebugSigning=true` is passed (ADR-039).
- Strip metadata from binaries (`META-INF/*` exclusions) and exclude dependency info.

## Consequences

- GitHub releases and CI artifacts are small per-ABI APKs; universal APK remains available as a
  fallback for sideloaders.
- Missing `keystore.properties` refuses Release packaging; the only debug-signed path is the
  explicit, NON-PRODUCTION opt-in (ADR-039), replacing the earlier silent fallback.
- The ABI split / bundle interplay is guarded at configuration time; changing the CI workflow
  requires keeping `RELEASE_TYPE=CI` semantics in sync.
- CI version names are unique per commit, which the built-in updater (ADR-023) can use for
  experimental channels.
