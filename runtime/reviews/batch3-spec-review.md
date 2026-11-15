# BATCH3 — Static Shortcut Fix: Spec Review

**Verdict: PASS** — no blocking spec-conformance findings.

- **Review date:** 2026-10-29
- **Mode:** read-only source and APK inspection. No device/emulator run, no build executed.
- **Scope (source delta):**
  - `app/src/main/res/xml/shortcuts.xml` (modified: four `targetClass` values)
  - `app/src/debug/res/xml/shortcuts.xml` (new: debug variant override)
- **Candidate artifact:** `app/build/outputs/apk/github/debug/ssilencce-1.4.0-beta.3.debug-github-universal.apk`
  - Size: **53,381,588 B** (exact match)
  - SHA-256: `382DDEADBFE148D6F0368A86A86D7B7C237CF897595F6E397CD4798FD489E741` (recomputed locally, exact match)
  - Package: `com.ssilencce.debug`; versionName `1.4.0-beta.3.debug`; minSdk 26 / targetSdk 36
- **Spec baseline:** no static-shortcut requirement exists under `openspec/specs/`. The only shortcut requirement is `openspec/specs/library-ux/spec.md` §"Specific entities can be added as launcher shortcuts" (dynamic per-entity shortcuts). Conformance is therefore assessed against the batch acceptance criteria and the manifest contract; no spec delta is required.

## 1. Acceptance checklist

| # | Criterion | Result | Evidence |
|---|---|---|---|
| 1 | Four static shortcuts retain IDs `last_added`, `top_tracks`, `favorites`, `shuffle_all` | PASS | Normalized comparison of both XML files: 4/4 identical IDs. Packaged debug `res/xml/shortcuts.xml`: 4/4. |
| 2 | Shortcut order preserved | PASS | `last_added` → `top_tracks` → `favorites` → `shuffle_all` in main source, debug source, and packaged debug APK. |
| 3 | Short and long labels preserved and resolvable | PASS | Attribute references unchanged. AAPT2 resolves them to `Last added`/`Last Added`, `Top Tracks`/`Top Tracks`, `Favorites`/`Favorites`, `Shuffle all`/`Shuffle`. |
| 4 | Icons preserved and resolvable | PASS | `@mipmap/ic_launcher_last_added`, `ic_launcher_top`, `ic_launcher_fav`, `ic_launcher_shuffle_all` unchanged; all four exist in `mipmap-anydpi-v26/` and are compiled into the candidate. |
| 5 | Actions preserved | PASS | `com.ssilencce.shortcut.LAST_ADDED`, `.TOP_TRACKS`, `.FAVORITES`, `.SHUFFLE` in all four entries; no action renamed. |
| 6 | `MainActivity` handles `LAST_ADDED`/`TOP_TRACKS`/`FAVORITES`/`SHUFFLE` | PASS | `handlePlaybackIntent` when-branches (`MainActivity.kt:254-273`) call `playMediaId(MediaIDs.LAST_ADDED)`, `playMediaId(MediaIDs.TOP_TRACKS)`, `playMediaId(MediaIDs.FAVORITES, true)`, `playMediaId(MediaIDs.SONGS, true)`; each branch clears the intent. Constants at `:395-398`. |
| 7 | Intent target is the exported alias | PASS | Alias `.activities.MainActivity` is `exported="true"` (`AndroidManifest.xml:86-90`); concrete `.ui.screen.MainActivity` is not exported. Both XML files use `targetClass="com.ssilencce.activities.MainActivity"`. |
| 8 | Release target package `com.ssilencce` | PASS (source) | Main XML: `targetPackage="com.ssilencce"` on all four. Compiled release artifact not rebuilt in this batch (see R1). |
| 9 | Debug target package `com.ssilencce.debug` | PASS | Debug XML: `targetPackage="com.ssilencce.debug"` on all four; `applicationIdSuffix = ".debug"` (`app/build.gradle.kts:231`); packaged debug APK confirms. |
| 10 | Static-shortcut spec exists that would require an update | PASS (none found) | `openspec/specs/**` contains only the library-ux entity-shortcut requirement; no static-shortcut spec. No spec/doc update required. |
| 11 | No unintended shortcut delta | PASS | `git diff HEAD` for the main file shows four one-line `targetClass` changes only. The debug file is a normalized duplicate whose only payload difference is `targetPackage` (plus its header comment). |

## 2. Evidence details

### 2.1 Source-level conformance

- `app/src/main/res/xml/shortcuts.xml:3-46` — four `<shortcut>` entries in the required order, with unchanged `enabled`, `icon`, `shortcutId`, `shortcutLongLabel`, `shortcutShortLabel`, `action`, and `targetPackage`; only `targetClass` changed from `com.ssilencce.ui.screen.MainActivity` to `com.ssilencce.activities.MainActivity`.
- `app/src/debug/res/xml/shortcuts.xml:4-47` — same four entries, same attributes, `targetPackage="com.ssilencce.debug"`, `targetClass="com.ssilencce.activities.MainActivity"`.
- Normalized XML comparison (ignoring `targetPackage` and comments) returned **IDENTICAL** for the four-shortcut payload, with order `last_added, top_tracks, favorites, shuffle_all` in both files.

### 2.2 Packaged candidate artifact

AAPT2 inspection of the candidate APK confirms the compiled resources, not just the source:

- `package: name='com.ssilencce.debug'`.
- Compiled `res/xml/shortcuts.xml` contains exactly four shortcuts in the same order with:
  - IDs `last_added`, `top_tracks`, `favorites`, `shuffle_all`.
  - Actions `com.ssilencce.shortcut.LAST_ADDED`, `.TOP_TRACKS`, `.FAVORITES`, `.SHUFFLE`.
  - `targetPackage="com.ssilencce.debug"` and `targetClass="com.ssilencce.activities.MainActivity"` on every intent.
- The compiled manifest contains `activity-alias` `com.ssilencce.activities.MainActivity` with `targetActivity="com.ssilencce.ui.screen.MainActivity"` and `exported="true"`.
- The alias carries `meta-data android.app.shortcuts` resolving to `xml/shortcuts` (`0x7f170016`), which is the compiled debug override.
- Resource resolution confirms the labels (`Last added`/`Last Added`, `Top Tracks`/`Top Tracks`, `Favorites`/`Favorites`, `Shuffle all`/`Shuffle`) and the four adaptive shortcut icons.
- The concrete `com.ssilencce.ui.screen.MainActivity` has no `exported` attribute and no intent filters in the packaged manifest, so it is not a valid launcher target.

### 2.3 Handler path (cold and warm launch)

- `onConnected` (`MainActivity.kt:77-81`) calls `handlePlaybackIntent(intent, true)` after `AbsSlidingMusicPanelActivity` publishes the controller (`AbsSlidingMusicPanelActivity.kt:282-284`). A cold shortcut launch therefore waits for the `MediaController` before handling, and the unconsumed intent remains available.
- `onNewIntent` (`MainActivity.kt:245-248`) handles a shortcut delivered to an existing activity; the activity is `singleTop` (`AndroidManifest.xml:60`), so a warm shortcut launch reuses it.
- Each handled action calls `setIntent(Intent())` (`:257, :262, :267, :272`), preventing replay after configuration changes.
- `playMediaId` (`PlayerViewModel.kt:510-526`) applies `shuffleMode` before setting the media item; `FAVORITES` and `SHUFFLE` pass `true`, while `LAST_ADDED` and `TOP_TRACKS` use the default `false`. The `SHUFFLE` shortcut maps to all songs shuffled, matching the `shuffle_all` shortcut ID/label.

### 2.4 Spec and documentation impact

- No static-shortcut spec exists under `openspec/specs/`; the only related requirement covers dynamic per-entity shortcuts. The archived `2026-10-23-add-library-ux` design mentions the static `shortcuts.xml` as existing infrastructure, but it is historical and does not define these four entries.
- Existing docs already describe the exported alias contract: `docs/CODE_MAP.md:5382` (ShortcutHelper targets `.activities.MainActivity`, not the non-exported `.ui.screen.MainActivity`) and `docs/architecture/ADR-001-fork-identity.md` (alias compatibility).
- The delta is a defect fix (pointing launchers at an exported component and correcting the debug package), not a user-facing feature change. **No spec or documentation update is required.**

## 3. Residuals

| ID | Severity | Residual | Disposition |
|---|---|---|---|
| R1 | Low (artifact hygiene) | The release APKs still in `app/build/outputs/apk/github/release/` are from 2026-10-16 and still package `targetClass="com.ssilencce.ui.screen.MainActivity"` (confirmed by AAPT2 on the universal release APK). Shipping one of those stale files would reproduce the original defect. | Not a failure of the candidate. Rebuild the release variant from the fixed source before distributing any release artifact. |
| R2 | Low (verification gap) | No device/emulator run was performed. Launcher long-press display, cold and warm launch of each shortcut, pinned-shortcut refresh after reinstall/update, and routing with both release and debug installed remain unexercised at runtime. | APK-level resolution (package, class, exported flag, compiled XML, labels/icons) is confirmed; runtime behavior is left to the device gate. |
| R3 | Low (maintainability) | The debug file duplicates the full shortcut list; future edits to IDs/labels/icons/actions in `main` must be mirrored or debug will drift. The debug file has an explanatory comment; the main file does not. | Non-blocking. Optional consistency check or a pointer comment in the main XML. |
| R4 | Low (delivery) | `app/src/debug/res/xml/shortcuts.xml` is currently untracked (`??` in `git status`). | Stage it together with the main XML change so the debug override is not lost. |
| R5 | Info (pre-existing docs wording) | `docs/FEATURES.md:104` calls the four built-in shortcuts "dynamic"; they are static XML shortcuts. This wording predates the batch. | Optional docs cleanup; not required for this change. |

## 4. Sign-off

**PASS.** Both changed XML files preserve the four static shortcuts' IDs, labels, icons, actions, and order. The main variant targets `com.ssilencce` and the debug variant targets `com.ssilencce.debug`, both against the exported `com.ssilencce.activities.MainActivity` alias. `MainActivity` handles all four actions on cold and warm launch. No static-shortcut spec exists, so no spec or documentation update is required. The candidate APK hash and size match the handoff exactly.

**Handoff:** back to `subdirector-next`, without `--wait`; this review does not block on further runtime checks.
