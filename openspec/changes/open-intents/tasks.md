# Tasks

- [x] Add `MiniPlayerActivity` with a Compose dialog UI (play/pause, seek, close, open in app).
- [x] Route content/file audio `ACTION_VIEW` intents to the mini player; keep other VIEW targets unchanged.
- [x] Resolve external URIs to library songs and fall back to tag metadata for unknown files.
- [x] Add the song-menu Preview action and play it through a local ExoPlayer without queueing.
- [x] Pause main playback while the popup is open and restore it on close unless "open in app" was used.
- [x] Add `resume_only_if_paused_by_disconnect` preference and gate resume-on-connect.
- [x] Add `prevent_unwanted_autoplay` and `prevent_unwanted_autoplay_strict` preferences and external play guard.
- [x] Add `prevent_playback_when_muted` preference and play gate; keep pause/resume on zero volume.
- [x] Add strings and settings XML entries.
- [x] Write DOCS_DELTA.md.
