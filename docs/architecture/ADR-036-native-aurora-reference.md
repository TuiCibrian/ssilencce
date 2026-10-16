# ADR-036: Native, Optional Aurora Reference Appearance

## Status

Accepted on 2026-10-03. Partially supersedes ADR-028, ADR-029 and ADR-035 for the explicitly selected Aurora appearance. The restored interface remains available. The chrome navigation surface was refined on 2026-10-09 by ADR-037 (category strip retired, floating header actions, grouped drawer).

## Context

After the restoration in ADR-035, the collaborator approved a new native implementation of `ssilencce-auroras.html`, the black-and-white, two-dimensional ribbon reference. The reverted Aurora proposals and the navy brand specification are not its implementation source.

ADR-026 permits progressive Compose interoperability. ADR-027 retains Navigation 2.9/SafeArgs and the sliding panel. ADR-028 defines seven saved player styles and cover capabilities; ADR-029 defines retained appearance and player-color preferences. Replacing these behavioral components wholesale would risk playback, selection, gestures and settings compatibility.

## Decision

- Add independent `aurora_appearance` and `aurora_ribbon_motion` preferences. Initialize Aurora only for an empty default preference store; preserve existing theme, category, font and player settings. A fresh Aurora installation uses the system font.
- Supply matching XML and Compose monochrome schemes. Aurora uses white player accents; saved cover-derived color schemes remain stored and return when Aurora is disabled. Do not present an ineffective color selector as active.
- Embed Compose ribbon, primary/secondary navigation, category tabs, drawer, home actions and player chrome in the existing activity and Fragment hosts. Keep the original shell and seven View-based players for other appearances.
- Keep one active queue. In Aurora, replace the graph's queue dialog destination with a full-screen Fragment using the same `QueueScreen` and real song actions. Other appearances retain the queue dialog.
- Adapt the seven existing `NowPlayingScreen` values without adding an eighth. Retain the cover Fragment, ViewPager, transformers, image settings, cover gestures and supported cover lyrics through `AndroidView`; reuse inherited action routing and `PlayerViewModel` commands.
- Style existing library adapters, contextual selection, search, metadata and preferences conditionally rather than duplicating their data or permission logic. Existing Compose supporting screens use the shared monochrome/glass surfaces.
- Represent multiple queues, queue search, custom tags, composers, manual lyrics timing, lyrics QR sharing, crossfade, mono output and song-count timers as unavailable. Preserve real engine-dependent EQ bands, queue stop-after-song, voice search, history and playlist creation.
- Render a pre-softened, low-resolution ribbon warped in two cached, bounded bitmap meshes instead of separate horizontal strip draws. Read its motion phase during drawing, not composition; align updates to the frame clock and stop outside the resumed/visible state, with zero system animation scale, in battery saver or with the motion switch off. Do not blur foreground text or controls.
- Keep actual Android system bars, keyboard and at least 48 dp control targets. Retain the mini-player where the app needs it, even when the reference hides it.

## Consequences

- Aurora is reversible without preference resets, engine/database changes, new dependencies or application version changes.
- Two player renderers and both theme stacks require verification. The native shared cover and action implementation minimizes behavioral duplication.
- Reference fidelity is constrained by real data, retained functionality, accessibility and the platform's insets; prototype simulations are never treated as implemented features.
- Build/lint, runtime screenshots, compatibility checks and release-mode profiling are separate acceptance gates. Pending checks remain recorded in `openspec/changes/implement-native-aurora-reference/tasks.md`; successful compilation alone does not establish visual or functional completion.
