# Notification plus

## Why

The Media3 notification currently exposes only previous/play/next plus repeat and
shuffle, with no way to reach Favorites, playlists, or transport extras. Users cannot
choose which actions appear in the collapsed/expanded notification, and album art is
always broadcast to the system lock screen.

## What Changes

- Add a stateful Favorite action (heart filled/unfilled) to the notification buttons.
- Add Close and Add-to-playlist actions; a setting can replace the Close slot with
  Add to playlist.
- Add a compact/expanded notification button editor (previous, next, play/pause,
  favorite, FF, RW, close, shuffle, repeat) with the current layout as default.
- Add a "Broadcast album art to system lock screen" toggle (default on); when off,
  artwork is omitted from session metadata.
- Document that the system media style cannot render an upcoming-songs list, so that
  item is intentionally skipped.
