# Folders tab: folder management and library maintenance

## Why

The Folders tab can only browse and blacklist folders. Users cannot inspect a folder,
create or rename folders, move/copy songs between folders, or hide individual files. Rows
that point at files deleted outside the app (playlists, play counts, history) also linger.

## What Changes

- Keep the existing linear/hierarchical toggle and expose the same preference in
  Settings > Library > Folders.
- Add a **Folder info** dialog (path, songs, sub-folders, total duration, last modified).
- Add **Exclude folder** confirmation in the Folders tab and detail screen, and a
  **Hide songs** action that writes the existing blacklist.
- Add **Create folder** and **Rename folder** actions (direct file API when writable,
  SAF fallback through persisted tree URIs; MediaStore rescan afterwards).
- Add **Move to folder** / **Copy to folder** for selected songs (stream copy to a
  directly writable destination or SAF `DocumentFile`; MediaStore rescan; source removal
  through file/SAF/MediaStore deletion with a delete-request fallback on Android 11+).
- Add a **Remove missing songs** maintenance action in Settings > Advanced that cleans
  playlist entries, play counts, play events and history rows for files that no longer
  exist.

No Room schema change: only new DAO queries and repository methods.
