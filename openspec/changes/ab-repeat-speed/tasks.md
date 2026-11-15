# Tasks

- [x] A-B repeat state in `PlaybackService` (start/end, handler scheduled at B, cleared on song change)
- [x] Custom commands `SET_AB_REPEAT`, `SET_SONG_PLAYBACK_PARAMS`, `RESET_SONG_PLAYBACK_PARAMS`
- [x] Player menu action with A → B → clear cycle and toast feedback
- [x] Player A/B indicator chip (Compose overlay on the album cover)
- [x] Long-press actions in synced lyrics (repeat line / set A / set B / clear)
- [x] `SongPlaybackParamEntity` + DAO, DB version 9, migration 8→9
- [x] Per-song speed/pitch applied on media item transition with global fallback
- [x] "Apply to this song only" / "Reset song" in the speed and pitch settings
- [x] Backup/restore optional section for per-song playback parameters
- [x] English strings only (main `values/strings.xml`)
- [x] Compile gate green
