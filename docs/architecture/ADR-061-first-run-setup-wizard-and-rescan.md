# ADR-061: First-Run Setup Wizard and Process-Scoped Rescan

## Status

Accepted.

## Context

ssilencce starts on a permission screen and then opens the full library, which is built from
Android's MediaStore index. A new user has no guided way to limit the library to chosen folders,
choose the starting folder for the Folders tab, or refresh the media index after copying files.
The only folder controls are the whitelist/blacklist switches in Library settings. MediaStore
also hides dot-directories and directories containing `.nomedia`, so some scan options commonly
found in local-file scanners cannot be honored.

## Decision

- A dedicated Compose `SetupWizardActivity` is shown once after the permission flow on a new
  installation, and can be reopened from Settings > Library. It is a separate activity, not a
  navigation destination, so the main graph and sliding-panel shell are unchanged. MainActivity
  routes to it while `setup_wizard_completed` is false; finishing or skipping marks it complete.
- The wizard edits the existing whitelist/blacklist tables and `start_directory` preference. Empty
  include lists keep the whole-device scope; non-empty lists force-enable their switch. The main
  folder is a start folder and a scan target, not an implicit library restriction.
- `LibraryRescan` requests MediaScannerConnection scans for the whitelist when non-empty, otherwise
  the mounted storage roots, plus the main folder. Scans are serialized and time-bounded. A
  process-scoped, dynamically registered `ACTION_MEDIA_MOUNTED` receiver (off by default,
  permission-guarded, 2.5 s debounce) triggers the same rescan while the app process is alive; it
  is not a manifest receiver so a disabled option does not start the app on mount events.
- `.nomedia`/hidden-folder toggles are not implemented; the wizard explains that MediaStore owns
  that indexing behavior.

## Consequences

- Upgrading users keep the existing first-run flow because the completion flag defaults to true
  and only the empty-preferences install path sets it to false.
- Folder scope and the folder-tab start point now have a discoverable first-run surface and share
  persistence with the existing settings dialogs.
- Auto rescan is best-effort while the process is alive; MediaStore still indexes media in the
  background for the app to read on next launch.
- Any future local (non-MediaStore) scanner can add hidden/`.nomedia` options behind this wizard
  step without changing the persisted folder model.

## Related

- ADR-007 (MediaStore source of truth), ADR-027 (navigation and shell), ADR-031 (scoped storage),
  ADR-032 (permissions/onboarding).
- `openspec/changes/setup-wizard/`.
