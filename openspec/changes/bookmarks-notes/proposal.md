# Song notes & bookmarks

## Why

ssilencce has no place to keep listener annotations about a track. Users need a fast way to mark
an interesting moment while listening, and a durable, user-owned place for per-song notes.

## What Changes

- Store per-song notes and timestamped bookmarks as plain UTF-8 `.txt` files in the app-specific
  external files directory (`Android/data/<package>/files/notes`), named `<song id> - <title>.txt`.
  The format is human-readable and safe to hand-edit.
- Add a one-tap **Add bookmark** action to Now Playing (configurable quick-access action, player
  overflow menu, Aurora header) that records the current position.
- Add a **Notes & bookmarks** screen per song: notes editor with explicit save, bookmark list
  (tap a bookmark to seek), add/edit/delete bookmark, share note text, and import text from a file.
- Add a song-menu entry that opens the same screen.
- Include the notes folder in backups and restore it as a selectable backup category.
- No new preference key: quick access uses the existing configurable row, and the notes folder needs
  no permission. Default behavior is unchanged for users who never pick the new action.
