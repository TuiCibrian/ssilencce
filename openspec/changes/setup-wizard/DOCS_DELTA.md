# Documentation delta for setup-wizard

Apply these to the docs after merging. Do not copy this file itself into the docs.

## docs/FEATURES.md

### §4 Library & Organization — add after the "Library filtering" bullet

- **First-run setup wizard** — A Compose `SetupWizardActivity` is shown once after the permission
  screen on a new installation (upgrades never see it automatically). It offers a welcome step, a
  folder step that edits the existing whitelist/blacklist tables and the `start_directory` main
  music folder with a Compose storage browser, a scanning step with the optional auto-rescan
  switch, and a finish step with a library rescan. Skip leaves the folder lists untouched. It is
  re-openable from Settings > Library > Setup wizard.
- **Library rescan** — Rescan library asks `MediaScannerConnection` to refresh the chosen folders
  (the whitelist when non-empty, otherwise the mounted storage roots, always including the main
  music folder). It is available from the wizard and from Settings > Library, serializes concurrent
  scans and has a 120 s completion guard. When the optional auto-rescan switch is on, a
  process-scoped, 2.5 s-debounced `ACTION_MEDIA_MOUNTED` receiver schedules the same rescan; it is
  permission-guarded and off by default.

### §4 Library & Organization — update the "Library filtering" bullet

- Append: "The setup wizard writes the same whitelist/blacklist tables and force-enables the
  corresponding switches when it saves a non-empty list."

### §11 Utilities — update the "Permission onboarding" bullet

- Replace with: "**Permission onboarding and first-run setup** — `PermissionsActivity` with guided
  permission requests (storage/audio, images, Bluetooth, exact alarms), followed by the
  `SetupWizardActivity` first-run library setup on new installations."

## docs/MISSING_FEATURES.md

### §6 Scanning & folder management — replace these bullets

- Old: `- **Welcome/setup wizard** — pick storage (SAF, per-volume access), choose folders to scan, set a **main music folder**, tutorials.`
- New: `- **Welcome/setup wizard** — ssilencce now has a first-run wizard with folder include/exclude, a main music folder and rescan; SAF per-volume access and in-wizard tutorials remain missing.`

- Old: `- **Scan options** — hidden folders, `.nomedia` handling, **scan video files as audio**.`
- New: `- **Scan options** — scanning video files as audio remains missing. Hidden-folder and `.nomedia` options are not offered because the MediaStore-based scanner cannot override Android's media index (documented in the wizard).`

- Old: `- **Rescan controls** — force rescan, scan on app start, scanner choice (Musicolet vs System), auto rescan on media mount/unmount.`
- New: `- **Rescan controls** — force rescan and auto rescan on media mount are implemented; scan on app start, scanner choice (Musicolet vs System) and auto rescan on unmount remain missing.`

## docs/CODE_MAP.md

Add these entries (in the matching package sections, alphabetical where the section is alphabetical):

- `data/local/LibraryRescan.kt`
  - **Purpose:** Request a MediaStore refresh for the user's chosen library folders.
  - **Responsibilities:** Resolve the scan scope (whitelist when non-empty/valid, otherwise storage
    roots, plus the main music folder), call `MediaScannerConnection.scanFile`, serialize concurrent
    scans, provide a debounced background variant and a permission check.
  - **Inputs:** Context, optional include-folder/main-folder overrides, preferences and
    `InclExclDao` whitelist paths.
  - **Outputs:** Android media scan requests; requested-path count.
  - **Dependencies:** MediaScannerConnection, StorageUtil, Preferences, InclExclDao, coroutines.
  - **Consumed by:** SetupWizardViewModel, LibraryPreferencesFragment actions,
    MediaMountRescanReceiver.
  - **Related files:** `util/Preferences.kt`, `data/local/room/InclExclDao.kt`,
    `ui/screen/setup/SetupWizardViewModel.kt`.
  - **Notes:** The `/` filesystem root is never scanned; a scan has a 120 s completion guard and no
    polling.

- `data/local/MediaMountRescanReceiver.kt`
  - **Purpose:** Schedule a debounced library rescan when a storage volume mounts.
  - **Responsibilities:** Filter `ACTION_MEDIA_MOUNTED`, require the auto-rescan preference and the
    audio read permission, then hand off to `LibraryRescan.scheduleDebounced`.
  - **Inputs:** Mount broadcast, Preferences, permission state.
  - **Outputs:** A debounced rescan request.
  - **Dependencies:** LibraryRescan, Preferences.
  - **Consumed by:** `App` (dynamic registration; process-scoped).
  - **Related files:** `data/local/LibraryRescan.kt`, `App.kt`.
  - **Notes:** Not registered in the manifest, so a disabled option never starts the app on a mount
    event; the feature runs while the app process is alive.

- `ui/screen/setup/SetupWizardActivity.kt`
  - **Purpose:** Compose host for the first-run library setup.
  - **Responsibilities:** Render `SetupWizardScreen`, route back/skip through the wizard steps,
    finish into `MainActivity` on a first run, and simply finish when opened from Settings.
  - **Inputs:** `EXTRA_FROM_SETTINGS`, `SetupWizardViewModel` state.
  - **Outputs:** Persisted setup choices; MainActivity launch.
  - **Dependencies:** AbsThemeActivity, Compose, Koin ViewModel, SsilencceTheme.
  - **Consumed by:** `MainActivity` (first-run gate), Library settings (Setup wizard preference),
    AndroidManifest.
  - **Related files:** SetupWizardScreen.kt, SetupWizardViewModel.kt, MainActivity.kt.
  - **Notes:** Exported false; the first-run route is gated by `setup_wizard_completed`.

- `ui/screen/setup/SetupWizardScreen.kt`
  - **Purpose:** Four-step Compose UI (welcome, folders, scanning, finish) for library setup.
  - **Responsibilities:** Show folder include/exclude lists and the main folder, open the Compose
    folder picker, host the auto-rescan switch, explain MediaStore scan limits, show the rescan
    action/result and drive Back/Next/Finish, with an accessibility heading/role pass.
  - **Inputs:** `SetupWizardUiState`, callbacks.
  - **Outputs:** State changes and picker selections.
  - **Dependencies:** Compose Material3, SsilencceTheme, SetupFolderPicker.
  - **Consumed by:** SetupWizardActivity.
  - **Related files:** SetupFolderPicker.kt, SetupWizardViewModel.kt.
  - **Notes:** The `.nomedia`/hidden-folder limitation is explained instead of offering toggles the
    MediaStore scanner cannot honor.

- `ui/screen/setup/SetupWizardViewModel.kt`
  - **Purpose:** Hold and persist the setup wizard choices.
  - **Responsibilities:** Load the whitelist/blacklist and main folder, seed the default blacklist
    once, add/remove include/exclude folders, track the step and auto-rescan option, run the rescan,
    persist folder tables/preferences and expose save progress/completion/failure.
  - **Inputs:** Application, Repository (default blacklist seeding), InclExclDao.
  - **Outputs:** `StateFlow<SetupWizardUiState>`; Room/preference writes.
  - **Dependencies:** LibraryRescan, Preferences, FileUtil, coroutines, Koin ViewModel.
  - **Consumed by:** SetupWizardActivity/SetupWizardScreen.
  - **Related files:** SetupWizardScreen.kt, SetupFolderPicker.kt, `data/local/LibraryRescan.kt`,
    MainModule.kt.
  - **Notes:** Saving replaces the whitelist/blacklist rows and enables a list's switch when it is
    non-empty; an empty include list keeps the existing "whole device" scope.

- `ui/screen/setup/SetupFolderPicker.kt`
  - **Purpose:** Compose folder browser for the setup wizard.
  - **Responsibilities:** List mounted storage volumes, navigate read-only directories (hiding
    dot-folders), go up, and return the selected absolute path; loads directories off the main
    thread.
  - **Inputs:** Dialog title, optional start path, dismiss/select callbacks.
  - **Outputs:** Selected folder path.
  - **Dependencies:** Compose Material3, StorageUtil, sortedByName.
  - **Consumed by:** SetupWizardScreen.
  - **Related files:** `ui/dialogs/library/FolderChooserDialog.kt` (legacy equivalent),
    SetupWizardScreen.kt.
  - **Notes:** Mirrors the legacy chooser's volume-list start and uses `Dialog` with the app theme.

Update these existing entries:

- `util/Preferences.kt` — add `auto_rescan_on_media_mount` (default false),
  `setup_wizard_completed` (default true; set false only by the new-install path in `App`), plus the
  `library_rescan`/`setup_wizard` settings-action keys and the writable `whitelistEnabled` accessor.
- `App.kt` — sets `setup_wizard_completed=false` only for a new installation and registers the
  process-scoped `MediaMountRescanReceiver`.
- `ui/screen/MainActivity.kt` — after the permission redirect, routes to `SetupWizardActivity` while
  `setup_wizard_completed` is false and finishes, so the wizard sits between permissions and the
  library.
- `ui/screen/settings/PreferencesScreenFragment.kt` — Library maintenance preferences: Rescan
  library (confirmation + background `LibraryRescan`), Auto rescan when media is mounted, and Setup
  wizard (launches `SetupWizardActivity` with `EXTRA_FROM_SETTINGS`).
- `MainModule.kt` — registers `SetupWizardViewModel`.
- `AndroidManifest.xml` — adds the non-exported `SetupWizardActivity`.

## AGENTS.md

Repository Code Map updates:

- In the `data/local/` bullet, add: "`LibraryRescan` (chosen-folder MediaScanner rescan with
  process-scoped debounce) and `MediaMountRescanReceiver`".
- In the `ui/screen/` bullet, add: "`setup/` — first-run Compose library setup wizard (folders,
  main folder, auto-rescan, rescan) reachable again from Library settings".
- In the App entry points bullet, change "`MainActivity.kt` (NavHost, playback intents, shortcuts)"
  to also mention "first-run setup-wizard gate".
- In the `App.kt` bullet, add "new-install wizard flag and process-scoped media-mount rescan
  receiver".
- Keep the existing rules text and version numbers unchanged.

## docs/agents/domain.md

- If the ADR below is accepted, increment the ADR count to 52 and add the title to the ADR list.

## Decisions

- **The wizard is a separate Compose activity, not a navigation destination.** It sits after
  `PermissionsActivity` via a `MainActivity` gate (both first launch and any launch with pending
  permissions), and the same activity is opened directly from Settings. This keeps the main nav
  graph, deep links and the sliding-panel shell untouched.
- **Only new installations see it automatically.** `setup_wizard_completed` defaults to `true` and
  `App` writes `false` only when the default SharedPreferences are empty on first run. Upgrading
  users therefore keep their current flow. Finishing or skipping sets it to `true`; re-opening from
  Settings works regardless.
- **The main music folder reuses `start_directory`.** It is the Folders-tab start folder and is
  always part of the rescan scope, but it is not added to the whitelist automatically. Include
  folders remain the only library-scope restriction, so choosing a main folder does not hide music
  stored elsewhere. "Use default" resets it to `FileUtil.getDefaultStartDirectory()`.
- **Empty include list keeps the existing whole-device behavior.** Saving a non-empty include or
  exclude list force-enables the matching switch so the chosen folder scope takes effect. Adding a
  folder to one list removes it from the other, matching the single-path primary key of
  `InclExclEntity`.
- **Skip/back during the first run marks the wizard complete without writing folder changes.** Once
  the user is in the app, it is not shown again; Settings can reopen it. The wizard seeds the
  default alarm/notification/ringtone blacklist once, like `LibraryViewModel` would on its first
  load, so the summary is accurate.
- **`.nomedia` and hidden-folder toggles are not offered.** The library is based on MediaStore,
  which does not index hidden directories or directories containing `.nomedia`; the app cannot
  override that from a read-only MediaStore query. The scanning step and the docs state the
  limitation instead of shipping toggles that cannot work.
- **Auto rescan defaults to off and is process-scoped.** `MediaMountRescanReceiver` is registered
  dynamically in `App` with `RECEIVER_NOT_EXPORTED`, requires the audio read permission, and
  debounces mount bursts for 2.5 s. Deliberately not a manifest receiver: the default-off feature
  should not start the app on every mount event. The practical limitation is that it runs while the
  app process is alive; Android's own MediaStore indexing still covers the rest.
- **Rescan scope and safety.** Whitelist folders when non-empty/valid, otherwise mounted storage
  roots, plus the main folder; `/` is excluded; concurrent scans are serialized with a mutex; a
  120 s timeout prevents an indefinite coroutine. No polling, wakelocks or main-thread IO.
- **No Room schema change.** The wizard writes existing whitelist/blacklist rows; no DB version,
  migration, dependency or app-version change was made.

## New ADR (next free number; 052 if still free)

`docs/architecture/ADR-052-first-run-setup-wizard-and-process-scoped-rescan.md`

```
Status: Accepted

Context
ssilencce starts on a permission screen and then opens the full library, which is built from
Android's MediaStore index. A new user has no guided way to limit the library to chosen folders,
choose the starting folder for the Folders tab, or refresh the media index after copying files.
The only folder controls are the whitelist/blacklist switches in Library settings. MediaStore
also hides dot-directories and directories containing .nomedia, so some scan options commonly
found in local-file scanners cannot be honored.

Decision
- A dedicated Compose SetupWizardActivity is shown once after the permission flow on a new
  installation, and can be reopened from Settings > Library. It is a separate activity, not a
  navigation destination, so the main graph and sliding-panel shell are unchanged. MainActivity
  routes to it while setup_wizard_completed is false; finishing or skipping marks it complete.
- The wizard edits the existing whitelist/blacklist tables and start_directory preference. Empty
  include lists keep the whole-device scope; non-empty lists force-enable their switch. The main
  folder is a start folder and a scan target, not an implicit library restriction.
- LibraryRescan requests MediaScannerConnection scans for the whitelist when non-empty, otherwise
  the mounted storage roots, plus the main folder. Scans are serialized and time-bounded. A
  process-scoped, dynamically registered ACTION_MEDIA_MOUNTED receiver (off by default,
  permission-guarded, 2.5 s debounce) triggers the same rescan while the app process is alive;
  it is not a manifest receiver so a disabled option does not start the app on mount events.
- .nomedia/hidden-folder toggles are not implemented; the wizard explains that MediaStore owns
  that indexing behavior.

Consequences
- Upgrading users keep the existing first-run flow because the completion flag defaults to true
  and only the empty-preferences install path sets it to false.
- Folder scope and the folder-tab start point now have a discoverable first-run surface and share
  persistence with the existing settings dialogs.
- Auto rescan is best-effort while the process is alive; MediaStore still indexes media in the
  background for the app to read on next launch.
- Any future local (non-MediaStore) scanner can add hidden/.nomedia options behind this wizard
  step without changing the persisted folder model.
```
