# Tasks

- [x] Add reusable queue action helpers in `PlayerViewModel` (pause after song, play after
      current, load next and pause) and feedback events.
- [x] Add action ids/strings/menu entries for per-song menus, queue item menus and the Now
      Playing menu.
- [x] Add `RepeatPolicy` (pure mode cycle + player-level pause-on-auto-transition policy) and the
      persisted active-state preference.
- [x] Wire the repeat cycle in `PlaybackService`, including external repeat-mode changes and the
      queue/Now Playing repeat labels/icons.
- [x] Add the "repeat includes load-next-and-pause" playback setting (default off).
- [x] Add named-queue merge policy, repository operation, ViewModel entry points and UI dialog in
      both named-queue surfaces.
- [x] Add named-queue auto-delete policy, preference, settings switch and save feedback.
- [x] Add strings next to related entries in `values/strings.xml`.
- [x] Write `DOCS_DELTA.md` and run the compile gate.
