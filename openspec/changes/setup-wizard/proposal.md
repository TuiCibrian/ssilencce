# Proposal: First-run setup wizard and library rescan

## Why

A new installation starts with the whole device indexed and no guidance about scan scope. MediaStore
already indexes most audio, but users have no single place to choose the folders that should form
their library, set the folder-tab starting point, or ask Android to rescan after copying files. The
only folder controls today are the whitelist/blacklist switches buried in Library settings.

## What Changes

- Add a Compose `SetupWizardActivity` shown once after the permission screen on a new installation.
  It is also re-openable from Settings → Library.
- The wizard guides the user through: welcome, music folders, scanning options and a finish step.
- The folder step edits the existing whitelist and blacklist Room tables (include/exclude folders)
  and the existing `start_directory` preference ("main music folder"), using a Compose folder
  browser over the mounted storage volumes. Empty include list keeps today's "whole device" scope.
- The scanning step documents an Android/MediaStore limitation: hidden folders and folders that
  contain a `.nomedia` file are not indexed by MediaStore, so the scanner cannot honor an
  "ignore .nomedia" or "include hidden folders" toggle. It offers the real option, auto rescan when
  a storage volume is mounted (default off, keeping current behavior).
- Add a `Rescan library` action that asks `MediaScannerConnection` to rescan the chosen folders
  (whitelist when present, otherwise storage roots, always including the main folder). It is
  available from the wizard finish step and from Settings → Library.
- Add a debounced, process-scoped `ACTION_MEDIA_MOUNTED` receiver that schedules the same rescan
  when the auto-rescan option is enabled; a disabled option never starts the app for a mount event.
- Persist a `setup_wizard_completed` flag (default `true`; new installations get `false`) so
  upgrades never see the wizard automatically, and a `auto_rescan_on_media_mount` flag (default
  `false`).

## Impact

- New: setup screen/ViewModel/activity, Compose folder picker, `LibraryRescan`,
  `MediaMountRescanReceiver`, OpenSpec change.
- Modified: `App`, `MainActivity`, `MainModule`, preferences, library settings XML/fragment, the
  Android manifest and English strings.
- No Room schema change, no new dependency and no version change.
- Out of scope: a local (non-MediaStore) scanner, changing whitelist/blacklist query semantics, and
  backup/restore of the new preferences beyond the existing preference backup.
