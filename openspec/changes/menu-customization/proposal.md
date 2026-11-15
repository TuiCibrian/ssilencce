# Per-entity option-menu customization

## Why

The song, album, artist, genre, playlist and folder option menus always show every
action in a fixed order. Users cannot remove actions they never use or move the ones
they use most to the top, so long-press menus stay noisy and slow to scan.

## What Changes

- Add a stable catalog of configurable actions for six entity menus (song, album,
  artist, genre, playlist, folder) plus a persisted per-entity state (order + hidden
  actions).
- Add a filter layer applied when a menu is inflated: hidden actions are removed and
  the remaining configurable actions are re-inserted in the saved order. Unknown
  (non-catalog) actions and submenus are preserved.
- Apply the filter to every popup inflated through `newPopupMenu` and to the genre
  detail overflow menu (genres have no item popup; their menu is the detail toolbar
  overflow).
- Add a "Menu actions" category to the Appearance settings screen with one row per
  entity; each row opens a Compose dialog with show/hide checkboxes and reorder arrows.
- Defaults keep the current menus and order untouched (no stored preference = all
  actions enabled in XML order).
