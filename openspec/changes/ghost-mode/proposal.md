# Ghost mode

## Why

Some listeners keep private audio (voice notes, language lessons, sensitive albums) on the same
device as their music. The app currently has a whole-folder blacklist, but once a folder is
blacklisted it is also unreachable from the app. Users need a reversible privacy filter that
hides selected folders from every normal library surface while keeping those songs reachable
from a deliberate, optionally PIN-protected screen.

## What Changes

- Add **ghost folders**: user-chosen directory trees stored as a JSON list in preferences and
  excluded, by path prefix, from the shared MediaStore song cursor. Every library list, count,
  shuffle source, widget source and Android Auto browse branch that is built from songs therefore
  honors the filter without per-screen logic.
- Room-backed lists (playlists, favorites, play counts, history, artist statistics) apply the same
  path filter when they map stored entities back to songs.
- Add a **Ghost search** screen in the main navigation graph that lists and searches only songs
  inside ghost folders and can play them.
- Entry points: long-press the search field on the search screen; a hidden **Advanced > Privacy >
  Ghost mode** settings item; and an optional `Ghost search` media-notification button.
- Add **Ghost mode settings** (Advanced > Privacy): master switch, ghost folder picker/list,
  optional 4–12 digit PIN, and a shortcut to Ghost search.
- Ghost folders can also be added from the folder context menus (**Add to ghost folders**).
- Add the `GhostSearch` notification button and the `OPEN_GHOST_SEARCH` session command.
- No Room schema change, no new permission, no version change. The default master switch is off,
  so existing users see no behavior change.
