## Context

See proposal.md for motivation. ADR-035 restored legacy players on the working branch; ADR-026 allows Compose interoperability, ADR-027 fixes Navigation/SafeArgs, ADR-028 defines seven styles and cover gestures, and ADR-029 defines saved theme choices. The baseline Kotlin compilation succeeds. The 19 reference views include simulated and unsupported capabilities.

## Goals / Non-Goals

**Goals:** Native, reference-based components with one shared appearance model, actual navigation and retained functionality. Integrate Compose into the existing hosts instead of rewriting data or playback.

**Non-Goals:** An HTML/WebView implementation, sample library data, new playback features, new dependencies, preference resets, version changes or reapplication of the reverted proposals.

## Decisions

- Use a new appearance key independent of saved general theme, Material You and player choices. Detect an empty store before preference defaults are populated; upgrades default to the retained appearance.
- Add a matching XML theme and Compose scheme. Retain the original palettes as alternatives rather than globally recoloring them.
- Render one low-cost shared white ribbon with draw-phase state reads and bounded motion; use lifecycle, system animation and battery-saver gates. Glass means translucent fills over a soft shared backdrop, not foreground blur.
- Keep Navigation 2.9/SafeArgs, the sliding sheet, existing library adapters/CAB and existing cover pager/transformers. New shell and player chrome use Compose. Reuse inherited player action routing rather than duplicating file/dialog logic.
- Adapt all seven variants through the existing style enum and feature flags. Compose interoperability retains cover gestures, image handling and cover lyrics.
- Theme/style legacy library and preference components conditionally, avoiding a risky loss of sorting, selection, SAF, metadata, profile and settings controls during a big-bang rewrite.
- Reserve multiple queues, queue search, custom tags, composer browsing, QR sharing, crossfade, mono, song-count timer and manual lyrics synchronization as unavailable; engine-dependent EQ controls retain their existing capability checks.

## Risks / Trade-offs

- GPU/battery cost → one background renderer, low refresh rate, no screen-wide runtime blur, no off-screen animation and a user motion switch.
- Hybrid integration regressions → retain original implementations for non-Aurora themes and verify shared margins, Fragment state and seven styles.
- Prototype omissions → retain working mini-player/actions even where HTML hides them; use actual Android keyboard and system bars.
- Device unavailable → build and lint are mandatory; screenshot comparison and release profiling remain explicitly pending until a device is available.

## Migration Plan

Apply incrementally to the restored working tree without changing Git history. Aurora can be disabled in Appearance to return to the retained interface. Maintain a new ADR and file/feature documentation; do not archive until the acceptance checks are complete.
