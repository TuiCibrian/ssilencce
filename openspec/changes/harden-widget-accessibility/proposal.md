# Proposal: Harden widget accessibility and preserve appearance (AA-10)

## Why

AA-10 covers the three Glance widgets. The original contract combines per-widget appearance
(`System`, `AlbumArt`, `Wallpaper`), per-instance settings, transparency, a shared empty state,
configure/reconfigure and accessible media controls. The acceptance packet `4E332456...` records
two hard blockers in the current source: media-control click targets below 48x48 dp and the dark
theme reading `onTertiaryContainer` from the light scheme. Later source review also found that
Large/ExtraLarge required-height functions used a 48 dp play target while the layouts use 64 dp,
and omitted the ExtraLarge 4 dp info inset, so the 2.0 font-scale fallbacks were wrong.

This change is a tracking bundle for the original AA-10 requirements and all acceptance gates. It
does not approve, integrate or complete the candidate source.

## What Changes

- Record the original AA-10 contract: per-widget `System`/`AlbumArt`/`Wallpaper` appearance,
  per-instance settings and configure/reconfigure, 0-100 transparency with heuristic floors, the
  shared empty state, provider/config preservation and 48x48 dp media controls.
- Require every media control's clickable node to be at least 48x48 dp without removing or hiding
  actions or changing the layout family.
- Require the dark theme's `onTertiaryContainer` (and every dark color) to come from the dark
  scheme.
- Require responsive selection at the declared widget sizes and 1.0/1.5/2.0 font scale, using
  compact/Narrow fallbacks instead of clipping. The source model uses a line-height factor and a
  selection margin; those are assumptions, not runtime proof (see design).
- Preserve the Wallpaper, per-instance, transparency, empty-state, provider/config and
  all-actions behavior exactly.
- Define the mandatory runtime gates: measured `AccessibilityNodeInfo` bounds, TalkBack
  focus/announcement/activation, dark and transparency contrast, wallpaper fallback,
  per-instance/reboot/reconfigure/delete-re-add, Card/Full empty pages, API 26/27 reconfigure,
  exact-fit launcher rounding and actual text metrics.

## Status and non-claims

- The exact reviewed V4 5-file set is live-integrated: receipt
  `803160B553E34166F280A9A644F3B52143E671827986BB89C0881CEFE4CD770E`, evidence
  `182859FD3CE4F91C0B44BC1AE679C60ED009982EE8C626CFBFE4CF3919D41924`. All five live hashes
  match the reviewed values; before/after copies and the five-only source delta are preserved.
- C `04601B2C34FEDC3DEDFF3F5C954812A35B18E311124738C8B9E788784ADA1A40` and K
  `F89E7C294075FDA10CA9AA3E74D122BB84F36D83F6AA4325AC8395EBB0161739` are **source-qualified**.
  The source is integrated but **unbuilt and runtime-pending**: no Gradle/build/APK, measured
  bounds, TalkBack, contrast, wallpaper, per-instance, empty-state, API 26/27, exact-fit rounding
  or transparency PASS is claimed.
- The v3 report `AA7203B2...` remains frozen; its Large/XL conclusion is superseded only for those
  cases.
- No AA-10 PASS, build, APK, runtime proof, archive or completion is claimed.
- Original normative input: packet `4E33245638361E493C621112CF2823BF0AFDD3CD4F040808F1ED8222A23A9899`
  and ADR-050.

## Impact

- Live source (reviewed V4): `SsilencceGlanceWidget.kt` `7E1DD643...`, `FullWidget.kt`
  `56C1B35F...`, `CardWidget.kt` `54DD4427...`, `WidgetComponent.kt` `C7E5A94F...`,
  `WidgetExtension.kt` `A2BB5A2D...`.
- Preserved source: `WidgetSettings.kt`, `WallpaperThemeProvider.kt`, `WidgetConfigActivity.kt`,
  the three provider XMLs and `AndroidManifest.xml`.
- Docs/OpenSpec only in this bundle; no dependency, Room schema, permission, version or translated
  string change.
