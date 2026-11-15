# Proposal: Now Playing extras

## Why

The Now Playing screen is the main interaction surface while music plays. It currently
lacks several convenience controls: keeping the screen awake, a full-size artwork viewer,
sharing a player screenshot, direct time seeking, thumb-only seek-bar interaction and a
user-configurable quick-access row.

## What Changes

- Add a `Keep screen on` preference for the expanded Now Playing panel (off by default).
- Extend the existing album-cover tap action preference with `Open fullscreen art`; add
  a corresponding overflow-menu action. Default behaviour is unchanged.
- Add a full-screen Compose album-art viewer with pinch-to-zoom, save-to-gallery
  (`MediaStore` images insert) and set-as-wallpaper (home, lock screen or both).
- Add a `Share Now Playing` action that renders the player view to a bitmap, optionally
  crops it to a square for social apps, writes it to the FileProvider cache and opens a
  share sheet.
- Add a `Seek to...` dialog accepting `mm:ss` input, reachable from the elapsed-time label
  and the player overflow menu.
- Add a `Touch only on thumb` preference for seek bars; implement it for the legacy
  `MusicSlider` wrapper and the Compose slider.
- Add user-selectable quick-access shortcuts (lyrics, equalizer, sleep timer, tempo,
  queue, tag editor, add to playlist), shown as a row in Now Playing, with a settings
  dialog that supports enabling, disabling and reordering. Empty by default.

## Impact

- Affected code: `Preferences`, now-playing settings XML, player base fragments, Aurora
  player screen, new artwork/share/seek components, new quick-access model and row.
- Affected resources: main English strings, `provider_paths.xml`, manifest wallpaper
  permission.
- No database schema, dependency, test source set or version changes.
