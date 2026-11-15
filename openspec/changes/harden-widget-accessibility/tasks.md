# Tasks: Harden widget accessibility and preserve appearance (AA-10)

This is a tracking bundle. Checked source/integration items are evidence only; no checked item is
an AA-10 PASS or completion claim.

## 1. Original requirements and scope

- [x] 1.1 Record the original AA-10 contract from acceptance packet
  `4E33245638361E493C621112CF2823BF0AFDD3CD4F040808F1ED8222A23A9899` and ADR-050.
- [x] 1.2 Cover Wallpaper, per-instance settings/config/delete, transparency, empty state,
  provider/config, 48x48 dp media controls and dark-theme contrast.
- [x] 1.3 Record that the 0.35/0.30 alpha floors are implementation qualifiers, not a contrast
  guarantee or a waiver of the original contract.
- [x] 1.4 Record that no AA-10 PASS, APK, runtime or archive is claimed; integration is source-only.

## 2. Source (live-integrated V4)

- [x] 2.1 Move media-control clickable modifiers to outer 48x48 dp containers without removing
  actions or changing layout families.
- [x] 2.2 Source every dark theme color, including `onTertiaryContainer`, from `darkColorScheme`.
- [x] 2.3 Replace constant Classic/Simplified/Medium/Full height thresholds with font-scale-aware
  required heights and compact/Narrow fallbacks.
- [x] 2.4 Share Large/ExtraLarge layout constants with the model: 64 dp play target and 4 dp
  ExtraLarge info inset.
- [x] 2.5 Preserve Wallpaper, per-instance settings/config/delete, transparency, empty state,
  provider/config and all actions.
- [x] 2.6 Live V4 hashes: `SsilencceGlanceWidget.kt` `7E1DD643...`, `FullWidget.kt`
  `56C1B35F...`; `CardWidget.kt` `54DD4427...`, `WidgetComponent.kt` `C7E5A94F...`,
  `WidgetExtension.kt` `A2BB5A2D...`; all five match the reviewed set.

## 3. Source review

- [x] 3.1 C v4 Spec `04601B2C...` PASS-qualified (source only).
- [x] 3.2 K `F89E7C29...` source-qualified.
- [x] 3.3 Record the 1.2 line-height factor and 1 dp margin as model assumptions; do not call
  font fit measured.
- [x] 3.4 No further source change after C/K review; the live 5-file set is the exact reviewed V4
  set (5/5 hashes match).

## 4. Live integration

- [x] 4.1 M granted an exact 5-file V4 live-copy gate after the C/K source qualification.
- [x] 4.2 Actual live-copy receipt `803160B5...`; evidence `182859FD...`.
- [x] 4.3 All five live hashes match the reviewed V4 set; before/after copies preserved.
- [x] 4.4 No Gradle/plugin/hook/device action; exactly 5 source files changed.

## 5. Build and static gates

- [x] 5.1 JDK 21 `:app:assembleGithubDebug` — actual `66CC81DD...` (53,520,224 B) for the unchanged widget V4 five-file set (FullWidget `5A2A0C95...` plus the other four) covered by source freeze `8569D3DF...`/manifest `B7100456...`; native 0.
- [x] 5.2 JDK 21 `:app:lintGithubDebug` — 0 lint errors on that build.
- [x] 5.3 Record the exact APK hash `66CC81DD9AB71553A6605A8DFD7334A9969B27B1CC07D94C568A7176E563A3E1`; no APK PASS before this task.
  - Scope note: 5.1-5.3 cover only the unchanged widget V4 five-file set and the other files in the `8569D3DF...`/`B7100456...` freeze. The three current patches (CoverProvider `79A78FB4...`, SmartRepository `FC218112...`, arrays `CC9676F6...`) are not in `66CC` and are separately qualified; this is not an all-tree built claim.
- [x] 5.4 `openspec validate harden-widget-accessibility --strict`: valid.

## 6. Runtime gates

- [ ] 6.1 Measured `AccessibilityNodeInfo` bounds >=48 dp at 220x48/70/140/180/240/260/300,
  240x140/148/176, 320x240/254.6/300/305.8 and 360x240/254.6/300/305.8, at 1.0/1.5/2.0 font scale.
- [ ] 6.2 Launcher rounding at exact-fit gates: Medium 240 width, Large/XL 320 width, XL 300
  height, compact 48 height, Simplified 73 height, Full 294 height and 254.6/305.8 boundaries.
- [ ] 6.3 Actual text layout/line height/font padding at 1.0/1.5/2.0 and host font-scale updates.
- [ ] 6.4 TalkBack focus, announcement and activation for every control; no overlap/clipping at
  220 dp width.
- [ ] 6.5 Dark no-artwork contrast and the transparency matrix at 0/50/100%; alpha floors are not
  a contrast PASS.
- [ ] 6.6 Wallpaper derivation/fallback on API 26, live wallpaper and provider failure.
- [ ] 6.7 Per-instance independence, reboot persistence, reconfigure and delete/re-add cleanup in
  all three widgets.
- [ ] 6.8 Card and Full empty-state pages; Responsive 3A-1 historical evidence is not a substitute.
- [ ] 6.9 API 26/27 `reconfigurable` behavior.
- [ ] 6.10 Confirm every layout family and action remains; no feature removal.

## 7. Documentation and closure

- [x] 7.1 Create this original-requirement-based OpenSpec bundle.
- [x] 7.2 Record the source-integrated state in `docs/FEATURES.md`/`docs/MISSING_FEATURES.md`;
  keep the unbuilt/runtime-pending and no-PASS wording.
- [x] 7.3 `docs/CODE_MAP.md` requires no state edit; its widget entries remain source descriptions.
- [ ] 7.4 Archive AA-10 only after 5.1-5.3 and 6.1-6.10 are complete; no archive now.
