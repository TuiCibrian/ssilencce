# ADR-004: Single-Module Gradle Build with Centralized Version Catalog

## Status

Accepted

## Context

The codebase grew from a legacy player app (CyanogenMod-Eleven lineage) with all code in one
module. Modernizing to a multi-module structure (feature modules, `:core`, `:data`) would have
required significant refactoring of package-private dependencies, Koin modules, and R-class
references across ~300 files. The project also wants deterministic, fast builds with a single
place for dependency versions.

## Decision

- Keep a single Gradle module `:app` (`settings.gradle.kts` includes only `:app`;
  `rootProject.name = "ssilencce"`).
- Centralize all plugin and library versions in `gradle/libs.versions.toml` and consume them via
  typed accessors and bundles (`kotlinx`, `lifecycle`, `media3`, `navigation`, `glance`, `koin`,
  `coil`, `ktor`, `markwon`).
- Apply shared plugins at the root `build.gradle.kts` with `apply false` (AGP, SafeArgs, Compose
  compiler, KSP, AboutLibraries) and re-alias them in the module.
- Enforce the toolchain: `jvmToolchain(21)`, AGP 9.3.1, Kotlin 2.3.21, KSP 2.3.6,
  compileSdk 37 / minSdk 26 / targetSdk 36.
- Build features: `viewBinding`, `compose`, `buildConfig`, `resValues`.
- Enable `lint { abortOnError = true }` (lint errors fail the build), release minification via
  R8 (`isMinifyEnabled` + `isShrinkResources`) with `proguard-rules.pro`, and Gradle caches
  (configuration cache, parallel builds).
- Disable packaging of dependency info (`dependenciesInfo` excluded from APK/bundle).

## Consequences

- Single place to bump versions; builds are reproducible across developers and CI.
- No modular boundaries exist, so architectural enforcement relies on package conventions
  (`core/`, `data/`, `playback/`, `coil/`, `ui/`) documented in AGENTS.md/CODE_MAP.md.
- Compilation is project-wide: any change recompiles the whole app, which will slow down the
  build as the codebase grows.
- A future split into modules will need to plan dependency direction explicitly
  (Koin `MainModule` currently wires everything together).
