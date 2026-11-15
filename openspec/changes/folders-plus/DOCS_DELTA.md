# Docs delta — folders-plus

Apply the bullets below to the listed docs. Do not regenerate the docs wholesale.

## Already implemented (skip / no code needed)

- The linear vs hierarchical folder structure toggle already existed:
  `Preferences.hierarchyFolderView`, the checkable `R.id.action_hierarchy_view` item in
  `menu_folders.xml`, and `LibraryViewModel.navigateToPath(..., hierarchyView = ...)`.
  This change only exposes the same preference as a switch in Settings > Library > Folders
  and refreshes the checked menu state when the fragment resumes.

## docs/FEATURES.md

### Update (existing bullet, line ~54)

Replace the sentence fragment that says missing-file filtering is read-only:

```
- **Non-destructive statistics reads** — Play-count and history reads filter missing/deleted files without deleting rows; explicit `Repository.deleteSong`/`deleteSongs`, `clearPlayCount`, `clearSongHistory` and the **Remove missing songs** maintenance action (Settings > Advanced) remain the only cleanup paths. ...
```

The rest of that bullet stays unchanged. The maintenance action deletes playlist entries,
play counts, play events and history rows for songs whose files no longer exist; the
startup `deleteMissingContent()` cleanup (playlists only) is unchanged.

### Add after the "Media collections" bullet (line ~46)

```
- **Folder management** — The Folders tab keeps the linear/hierarchical toggle (also in Settings > Library > Folders). Folder actions include a **Folder info** dialog (path, direct and total song counts, sub-folders, total duration, total size, created and last-modified dates), **Create folder**, **Rename folder**, **Exclude folder** (confirmation, writes the existing blacklist) and **Hide from library** for songs (writes the blacklist; reversible in Settings > Library > Blacklist). Creating and renaming write directly when the destination is writable and otherwise through persisted SAF trees; a MediaStore rescan follows every filesystem change. Song/file menus and multi-select add **Move to folder** and **Copy to folder**: files are streamed to a writable folder or a SAF `DocumentFile`, names are uniquified, and moves delete the source through SAF, a direct file delete or MediaStore. Android 10 uses the recoverable delete intent; Android 11+ asks for confirmation with `MediaStore.createDeleteRequest`, and declining the request keeps the copies. Missing source files are skipped and reported.
```

### Add near the backup/restore bullet (line ~115)

```
- **Library maintenance** — Settings > Advanced > Maintenance > **Remove missing songs** scans MediaStore for songs whose files no longer exist, reports the count, and after confirmation removes their playlist entries, play counts, play events and history rows in one pass. It never deletes files or MediaStore rows.
```

## docs/MISSING_FEATURES.md

Remove these bullets (all implemented now):

- line 72: `- **Linear or hierarchical folder structure** toggle in the Folders tab.`
- line 73: `- **Folder info** (creation time, songs, sub-folders) and **rename folders**.`
- line 74: `- **Exclude directly from the Folders tab** (ssilencce configures whitelist/blacklist in settings).`
- line 76: `- **Remove missing songs / hidden songs** from the library.`
- line 93: `- **Move / copy songs** between folders.`
- line 94: `- **Rename folders** and **create folders**.`

Leave the section heading `## 6. Scanning & folder management` and the remaining bullets
(welcome/setup wizard, scan options, ghost mode) intact.

## docs/CODE_MAP.md

Add entries (same Purpose / Responsibilities / Inputs / Outputs / Dependencies / Consumed
by / Related files / Notes format as the rest of the file):

- `core/model/filesystem/FolderInfo.kt` — Immutable folder-info snapshot: path, direct and total
  song counts, sub-folder count, total duration/size, created and last-modified seconds.
- `util/FolderInfoProvider.kt` — Builds `FolderInfo` from `Repository.folderByPath`,
  `filesInPath` and `songsByFolder(includeSubfolders = true)`; resolves folder timestamps with
  `java.io` / `java.nio.file` under `runCatching`.
- `util/FolderFileOperations.kt` — Suspend folder mutations: create/rename (direct or SAF
  `DocumentFile`), move/copy of songs (source through `ContentResolver` with file fallback;
  destination through `FileOutputStream` or SAF `createFile`; unique names; source deletion
  via SAF, direct delete or MediaStore with `RecoverableSecurityException` /
  `createDeleteRequest` hand-off), and MediaStore rescans. Consumed by `FolderNameDialog`
  and `FolderTransferDialog`.
- `ui/screen/library/folders/FolderInfoDialog.kt` — Compose `DialogFragment` showing
  `FolderInfo` with a loading state; injected `Repository`.
- `ui/screen/library/folders/FolderNameDialog.kt` — Compose create/rename prompt; performs the
  operation with `FolderFileOperations`, handles the SAF grant (`SAFDialog`) retry, refreshes
  `ReloadType.Folders`, and asks `FolderDetailFragment` to pop when its folder path became stale.
- `ui/screen/library/folders/FolderTransferDialog.kt` — Compose folder picker with search,
  progress and result summary for move/copy; owns the Android 10/11+ source-deletion
  confirmation via `StartIntentSenderForResult`.
- `ui/screen/library/folders/HideSongsDialog.kt` — Compose confirmation that inserts song file
  paths into `InclExclDao` as `BLACKLIST`, toasts the count (or the blacklist-disabled hint)
  and refreshes folder/song lists.
- `ui/screen/library/folders/ExcludeFolderDialog.kt` — Compose confirmation that inserts a
  folder path into the blacklist and refreshes the library.
- `ui/screen/library/folders/FolderMediaRefresh.kt` — `notifyMediaStoreChanged(context)` helper
  used after blacklist changes.
- `ui/screen/settings/RemoveMissingSongsDialog.kt` — Scan/confirm/progress/result Compose
  dialog for `Repository.findMissingSongs` / `Repository.removeMissingSongs`.
- `data/local/repository/MissingSongsCleanup.kt` — `MissingSongsCleanupResult` row counts.

Update existing entries:

- `extensions/files/SAFExt.kt` — add `Context.findSAFDocumentFile(path)` and the ordered
  `DocumentFile.resolvePath(segments)` resolver (unlike the existing unordered `findDocument`).
- `ui/screen/library/folders/FoldersListFragment.kt` — folder info/rename/exclude actions,
  create-folder and current-folder actions with mode-aware visibility, menu state refresh on
  resume.
- `ui/screen/library/folders/FolderDetailFragment.kt` — folder info/rename/exclude menu,
  `onMediaContentChanged` reload and `onFolderRenamed` pop.
- `ui/component/menu/MenuItemClickExt.kt` — move/copy/hide handling for single and multi-select
  song menus.
- `data/local/repository/Repository.kt` — `findMissingSongs()` and `removeMissingSongs()`.
- `data/local/repository/PlaylistRepository.kt` — `countSongsInAllPlaylists()`.
- `data/local/repository/SmartRepository.kt` — `deleteSongsInPlayEvents()`,
  `countSongsInPlayEvents()`, `countSongsInHistory()`.
- `data/local/room/PlaylistDao.kt`, `HistoryDao.kt`, `PlayEventDao.kt` — count/delete queries
  used by the maintenance action.
- `util/Preferences.kt` — `REMOVE_MISSING_SONGS` preference key.
- `ui/screen/settings/PreferencesScreenFragment.kt` — opens `RemoveMissingSongsDialog` from
  Settings > Advanced.

## AGENTS.md (Repository Code Map)

Update the `core/model/` line to mention `filesystem/FolderInfo`; the `util/` line to add
`FolderInfoProvider` and `FolderFileOperations`; the `data/local/repository/` line to add
`MissingSongsCleanup`; the `data/local/room/` line to note the maintenance count queries; the
`settings/` line to add `RemoveMissingSongsDialog`; and the `folders` entry under library/
screens to mention info/create/rename/exclude/move/copy dialogs.

## ADR candidate (number to be assigned by the docs agent; ADR-051 is the current last)

Status: Accepted

Context: Folder management needs to create/rename folders and move/copy audio files on API
26–36, where direct file writes stop working on scoped storage. MediaStore alone cannot
create arbitrary folders, and SAF trees may not cover every volume. Deletion of files owned
by other apps additionally requires user consent on Android 10+.

Decision: Folder mutations are performed by `util/FolderFileOperations`. Writes first use
direct `java.io` when the destination is writable and otherwise require a persisted SAF tree
resolved through `findSAFDocumentFile`. Move is implemented as copy-then-delete; source
deletion tries SAF, then a direct file delete, then `ContentResolver.delete`, and hands
recoverable/`createDeleteRequest` consents to the dialog. Every operation ends with a
MediaStore rescan. The folder info/maintenance UI is Compose.

Consequences: No storage permission beyond the existing media permissions is added.
Operations work across API 26–36 within the granted trees, and never silently copy without
telling the user when originals could not be removed. Long transfers are owned by the
dialog; a configuration change during a transfer cancels it, potentially leaving copies
(no partial file is left at the destination because failed writes are deleted).

## Decisions / notes

- Folder structure toggle already existed; only the Settings switch and menu-state refresh are new.
- Hide/Exclude reuse `InclExclEntity` (blacklist) exactly as the settings dialog does; no new table.
- The automatic playlist-only missing-content cleanup on `LibraryViewModel` startup is preserved.
- Play event cleanups are added to the maintenance action and counted together with play
  counts in the result text to keep the localized format small.
- Move/copy menu items are available from the shared song menus, so any screen that flattens
  albums/artists/years/playlists to songs supports them; the actual transfer always runs in
  `FolderTransferDialog` with no per-screen state.
- No Room schema/version change; only DAO queries and repository methods were added.
