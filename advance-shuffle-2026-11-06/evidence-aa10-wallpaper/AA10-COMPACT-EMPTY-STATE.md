# AA-10 compact empty-state fix (post-8DB source)

Defect from AA10 runtime on 8DB35: Responsive at 3x1 rendered the shared empty state with the
48dp icon, 16dp padding and two default-size text lines, so “Nothing playing” was clipped and
“Open ssilencce” was not visible.

## Source fix

`WidgetComponent.kt` now reads `LocalSize.current.height`:

- **Below 160dp (compact):** no icon; 14sp bold title + 12sp action; 6dp padding; 2dp gap;
  one line each. Both texts remain visible without clipping.
- **160dp and above (normal):** unchanged 48dp icon + title + action + 16dp padding layout.
- The whole empty-state surface remains one clickable target to `MainActivity`; both text
  nodes remain accessible. No new string, permission or dependency.

Threshold is 160dp to keep the normal 128dp content stack safe under font scaling, while
Card/Full (3x3/4x4) keep the normal layout and 3x1 uses compact.

## Probe

`advance-shuffle-2026-11-06/tools/probe_aa10_widgets.py`

- **74/74 PASS** (previous 70/70).
- New checks: adaptive `LocalSize`/160dp selection, compact title/action text sizes and
  one-line bounds, normal 48dp/16dp layout preservation, and pure threshold cases
  (`96`/`159` compact, `160`/`240` normal).
- Result JSON: `advance-shuffle-2026-11-06/evidence-aa10-wallpaper/probe_aa10_widgets.json`.

## Validation

- ktlint 1.8.0 format + check on `WidgetComponent.kt`: **exit 0**.
- No build and no device run in this phase.
- Next combined build waits for H AA-07 source and M AA-06 gate evidence.
- AA-05/AA-06/AA-07 source files untouched.
