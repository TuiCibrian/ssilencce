# BATCH3 — Static Shortcut Target Fix: Standards & Security Review

**Verdict: PASS** — no blocking findings.

- **Review date:** 2026-10-29
- **Scope (source delta):**
  - `app/src/main/res/xml/shortcuts.xml` (modified)
  - `app/src/debug/res/xml/shortcuts.xml` (new)
- **Mode:** read-only source + built-artifact review. No device/emulator run, no build executed by this review.
- **Candidate artifact:** `app/build/outputs/apk/github/debug/ssilencce-1.4.0-beta.3.debug-github-universal.apk` — SHA-256 `382DDEADBFE148D6F0368A86A86D7B7C237CF897595F6E397CD4798FD489E741`, 53,381,588 B. **Hash and size match the handoff exactly.**
- **Handoff build:** PASS 2m39s (accepted; corroborated by the on-disk artifact, its compiled resources, and a fresh lint report).

## 1. Delta audit

| Check | Result | Evidence |
|---|---|---|
| Main-file delta is only the four target classes | PASS | `git diff HEAD` = 4 deletions / 4 insertions, all `targetClass` values. No other attribute changed. |
| Debug file is the only new file in the delta | PASS | `git status --porcelain app/src/debug` → only `app/src/debug/res/xml/shortcuts.xml` (plus pre-existing `values/strings.xml`). |
| IDs, labels, icons, actions, order preserved | PASS | Normalized structural comparison (ignoring `targetPackage` and the debug comment) is an exact match. Order: `last_added`, `top_tracks`, `favorites`, `shuffle_all`. |
| Resource references valid | PASS | All four `@string/app_shortcut_*`, `@string/*_label`, and `@mipmap/ic_launcher_*` resources resolve; AAPT2 compiled them into the candidate. |
| XML hygiene | PASS | Well-formed; UTF-8 without BOM; final newline; no trailing whitespace. |
| `git diff --check` | PASS | Clean (only unrelated CRLF advisory warnings from other dirty files). |
| Scope creep | PASS | Batch3 source delta is exactly the two files. The wider dirty working tree belongs to earlier batches and was not touched by this fix. |

## 2. Variant override correctness

- `applicationIdSuffix = ".debug"` is declared on the `debug` build type (`app/build.gradle.kts:231`), so the installed debug package is `com.ssilencce.debug`; `namespace` is still `com.ssilencce` (`:108`).
- Build-type resources override `main`: the candidate APK's compiled `res/xml/shortcuts.xml` contains `targetPackage="com.ssilencce.debug"` for all four intents. The `<shortcuts>` element reports `line=3`, proving the debug file (with its two-line header comment) was the one compiled, not the main file (`line=2`).
- The stale release APK still present in the build directory resolves `xml/shortcuts` to its shrunk path `res/Eq.xml`, whose intents use `targetPackage="com.ssilencce"` — i.e. the main variant, not the debug override. This confirms the override is correctly scoped to debug.
- No flavor source set provides a competing `res/xml/shortcuts.xml`; only the main and debug files exist. The override therefore applies to `githubDebug`, `fdroidDebug`, and `playstoreDebug` consistently.
- Packaged debug manifest: `package="com.ssilencce.debug"`. Packaged alias: `com.ssilencce.activities.MainActivity`, `targetActivity="com.ssilencce.ui.screen.MainActivity"`, `exported="true"`.
- Component class names resolve against the **namespace**, so the alias remains `com.ssilencce.activities.MainActivity` in debug; the intent's `targetPackage` must instead be the **applicationId**, `com.ssilencce.debug`. The new file applies exactly that split — correct.

## 3. Exported alias and security impact

- The target is the existing `<activity-alias android:name=".activities.MainActivity" android:exported="true">` declared in `AndroidManifest.xml:86-90`. The concrete `com.ssilencce.ui.screen.MainActivity` has no `android:exported` and no intent filters, i.e. it is **not** exported. The previous static-shortcut target (the concrete activity) therefore could not be started by a launcher on behalf of the user; the alias is the correct, already-exported launch surface.
- **No new exported component, permission, intent filter, or IPC surface is introduced.** The alias was already the launcher entry point and was already exported; the fix only points the shortcut intents at it.
- The four static shortcut intents carry no extras and no sensitive data. `MainActivity` consumes the custom actions (`APP_SHORTCUT_*`) and clears the intent (`setIntent(Intent())`) after handling.
- Dynamic entity shortcuts already use the same alias with `ComponentName(context.packageName, LAUNCHER_ACTIVITY_ALIAS)` (`ShortcutHelper.kt:36-55`), so static and dynamic shortcuts are now consistent. Folder-path extras are resolved through `repository.songsByFolder` (MediaStore query), not by opening arbitrary files.
- Side-by-side install behavior is improved: a debug shortcut can no longer inadvertently launch the release app (or fail when only debug is installed). Debug artifacts remain debug-signed and non-distributable, independent of this change.

## 4. Behavior, metadata, and scope

- Intended behavior change: the four static shortcuts now resolve to an exported component and can actually launch; debug shortcuts resolve inside `com.ssilencce.debug`.
- No version, manifest, permission, icon, label, action, ID, or ordering changes.
- No documentation changes were required for this delta: `docs/CODE_MAP.md` describes `res/xml/` generically and does not enumerate per-file shortcut entries; no user-facing feature set changed.
- No unrelated refactoring or cleanup was folded into the delta.

## 5. Independent verification evidence

- **Candidate APK:** hash/size match; AAPT2 dump of `AndroidManifest.xml` and `res/xml/shortcuts.xml` shows the expected package, alias, exported flag, and all four intent targets.
- **Lint:** `app/build/reports/lint-results-githubDebug.txt` was regenerated at 11:00 (after the 10:54 source edits): **0 errors, 1,256 warnings**; no `shortcuts.xml`/shortcut-target findings. Lint's `abortOnError` gate is therefore satisfied for this variant.
- **Whitespace:** `git diff --check` clean for the tracked file; the untracked debug file was checked separately (no trailing whitespace, final newline present).

## Findings

| ID | Severity | Finding | Disposition |
|---|---|---|---|
| F1 | Low (maintainability) | The debug file duplicates the full shortcut list; only `targetPackage` differs. A future edit to IDs/labels/icons/actions in `main` must be mirrored manually or debug will silently drift. The debug file has an explanatory comment; the main file does not. | Non-blocking. Recommend adding a short comment in the main XML pointing to the debug override and, optionally, a cheap normalized-file consistency check in CI. |
| F2 | Info | Line endings differ: main file uses CRLF, the new debug file uses LF. `.gitattributes` (`* text=auto`) will normalize on commit; no functional or build impact. | No action required. |
| F3 | Info | The debug resource is currently untracked (`??`). `git diff --check` does not inspect untracked files. | Ensure `app/src/debug/res/xml/shortcuts.xml` is staged with the change. |
| F4 | Info (verification gap) | Only `githubDebug` was built/linted in this batch. The release variant was not rebuilt after the `targetClass` change; the stale release APK confirms source-set separation and `targetPackage=com.ssilencce`, but not the new release `targetClass`. | Source inspection shows the main XML applies to all non-debug variants; run a release assemble/lint before shipping a release artifact. |
| F5 | Info (pre-existing) | `docs/FEATURES.md:104` calls the four built-in shortcuts "dynamic"; they are static XML shortcuts. Not introduced by this batch. | Optional docs cleanup, out of scope. |
| F6 | Info (runtime) | No device run was performed (per instruction). Long-press launch on a launcher, behavior with both release and debug installed, and refresh of already-pinned shortcuts remain device-verifiable checks. | Residual verification only; APK-level resolution (package, class, exported flag, compiled XML) is confirmed. |

## Sign-off

**PASS.** The two-file delta is minimal, correctly scoped, preserves every shortcut attribute, and fixes the actual defect: the old intent targeted an unexported activity, while the new intent targets the exported compatibility alias with the correct variant-specific `applicationId`. Variant override behavior is confirmed in the candidate APK. No security regression; no metadata or behavior changes beyond the intended shortcut launch path.
