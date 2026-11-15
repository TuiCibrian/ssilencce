# Documentation delta for ab-repeat-speed

Apply these to the docs after merging. Do not copy this file itself into the docs.

## docs/FEATURES.md

### §1 Playback & Audio — add after "Playback modes"

- **A-B repeat** — The player menu cycles Set A → Set B → Clear for the current track; playback loops A→B until cleared and any song change clears the loop. A small chip over the player cover shows the A / A-B markers and clears the loop on tap. The playback service schedules one wake-up at B (no position polling) and accounts for playback speed.

### §1 Playback & Audio — add after "Audio processing"

- **Per-song speed and pitch** — An optional per-song override persisted in Room (`SongPlaybackParamEntity`, DB v9) is applied on every media item transition and falls back to the global tempo when no row exists. The speed and pitch section of the sound settings sheet offers "Apply to this song only" and "Reset song", and shows the stored per-song values.

### §1 Playback & Audio — update the State persistence bullet

- Change `(Room v8)` to `(Room v9)`.

### §2 Equalizer & Sound — update the Sound settings sheet bullet

- **Sound settings sheet** — Balance, tempo and volume controls reachable from the player, plus per-song "Apply to this song only" / "Reset song" actions; ReplayGain mode/preamp and the calculation action live in the Equalizer screen.

### §3 Lyrics — add after "Lyrics display"

- **A-B repeat from lyrics** — Long-pressing a synced lyric line opens Repeat this line / Set A here / Set B here / Clear A-B repeat; "Set A here" then "Set B here" on a later line defines an arbitrary range, and the range is shown by the player A-B chip.

### §11 Utilities — update the Backup/restore bullet

- Change "ZIP backups of settings, playlists (M3U), favorites, play counts, named queues, per-file resume positions, lyrics cache and custom artist images" to also include "per-song speed and pitch".
- Change "a picker over all eight content types" to "a picker over all nine content types".
- Note the new optional restore section: per-song playback parameters are stored in `playback/song_playback_params.json`, entries with invalid song ids or out-of-range speed/pitch values are ignored, and other sections are unaffected.

## docs/MISSING_FEATURES.md

### Part 1 §1 Playback — remove these bullets

- `- **A-B repeat** loop within a track.`
- `- **Per-song play speed** — speed/pitch per individual song (ssilencce has global tempo only).`

### Part 1 §7 Tags, lyrics & album art — remove this bullet

- `- **A-B repeat on selected lyric lines**.`

### Appendix §5 Playback speed & pitch — replace the ssilencce line

- Old: `- ssilencce: global tempo (speed/pitch) control only.`
- New: `- ssilencce: global tempo (speed/pitch) plus an optional per-song override persisted in Room and selectable from the sound settings sheet; the remaining gap is Musicolet's 4-decimal precision and simple/advanced editor modes.`

## docs/CODE_MAP.md

Add entries (in the matching package section, alphabetical where the file already follows that order):

- `data/local/room/SongPlaybackParamEntity.kt`
  - **Purpose:** Room entity for optional per-song speed/pitch overrides.
  - **Responsibilities:** Store `song_id` (PK), `speed` and `pitch`; absence means "use global tempo".
  - **Inputs:** Values written from `PlayerViewModel` / restore and read by `PlaybackService`.
  - **Outputs:** Rows used on media item transition.
  - **Dependencies:** Room annotations.
  - **Consumed by:** `SongPlaybackParamDao`, `PlaybackService`, `PlayerViewModel`, `BackupHelper`.
  - **Related files:** `core/SsilencceDatabase.kt` (v9 + `MIGRATION_8_9`), `util/backup/BackupEntryModels.kt`.
  - **Notes:** Migration creates the table only; existing songs remain on the global tempo.

- `data/local/room/SongPlaybackParamDao.kt`
  - **Purpose:** DAO for per-song playback parameters.
  - **Responsibilities:** Point lookup, full export, upsert (single/all), delete one and clear.
  - **Inputs:** `songId`, `SongPlaybackParamEntity` lists.
  - **Outputs:** Stored overrides.
  - **Dependencies:** Room.
  - **Consumed by:** `PlaybackService` (transition apply/commands), `PlayerViewModel` (state/write), `BackupHelper` (backup/restore).
  - **Related files:** `SongPlaybackParamEntity.kt`, `MainModule.kt` room module.
  - **Notes:** No Flow; callers are lifecycle-owned coroutines.

- `ui/screen/player/AbRepeatState.kt`
  - **Purpose:** UI contract for in-track A-B repeat.
  - **Responsibilities:** Hold the active `startMs..endMs` range, model the A→B→clear menu result and define the 500 ms minimum range.
  - **Inputs:** Current player position/duration from `PlayerViewModel`.
  - **Outputs:** State for the player menu, chip and lyrics sheet.
  - **Dependencies:** None (plain Kotlin).
  - **Consumed by:** `PlayerViewModel`, `AbsPlayerFragment`, `AbRepeatChip`, `LyricsScreen`.
  - **Related files:** `playback/PlaybackService.kt` (authoritative loop).
  - **Notes:** Only the service owns the live loop; this model mirrors it for UI.

- `ui/component/compose/player/AbRepeatChip.kt`
  - **Purpose:** Small player overlay showing A / A-B markers.
  - **Responsibilities:** Render `A-B start - end` or `A start · set B` and clear the loop on tap, with an accessibility click label.
  - **Inputs:** `AbRepeatState?`, staged A `Long?`, clear callback.
  - **Outputs:** Player cover overlay.
  - **Dependencies:** Compose, `asReadableDuration()`.
  - **Consumed by:** `CoverPagerFragment` (hosted in `fragment_player_album_cover.xml`).
  - **Related files:** `AbRepeatState.kt`, `PlayerViewModel`.
  - **Notes:** Drawn above cover and cover-lyrics in all player styles, including Aurora's cover host.

- `ui/component/compose/lyrics/AbRepeatLineSheet.kt`
  - **Purpose:** Long-press menu for A-B repeat on synced lyric lines.
  - **Responsibilities:** Offer Repeat this line / Set A here / Set B here / Clear A-B repeat, disabling Set B until A exists or when the line ends before A.
  - **Inputs:** Line text, active/started flags, callbacks.
  - **Outputs:** Material bottom sheet.
  - **Dependencies:** Compose Material3.
  - **Consumed by:** `LyricsScreen` synced branch (full and cover lyrics).
  - **Related files:** `LyricsView.kt` (long-press callback), `PlayerViewModel`.
  - **Notes:** Offsets lyric timestamps by `SyncedLyrics.offset` before sending to the player.

- `ui/screen/sound/SongTempoOverrideControls.kt`
  - **Purpose:** Per-song tempo actions below the global speed/pitch controls.
  - **Responsibilities:** Show the current song and stored override values, offer "Apply to this song only" and "Reset song", and explain when playback is unavailable.
  - **Inputs:** Song title, override speed/pitch, apply/reset callbacks.
  - **Outputs:** Compose controls inside the sound settings sheet.
  - **Dependencies:** Compose Material3.
  - **Consumed by:** `SoundSettingsScreen`.
  - **Related files:** `PlayerViewModel` (`songPlaybackParams`), `SongPlaybackParamEntity`.
  - **Notes:** Apply pins the current global tempo values; reset deletes the row and reapplies the global tempo.

Update existing entries:

- `core/SsilencceDatabase.kt` — version 9, register `SongPlaybackParamEntity`, `songPlaybackParamDao()` and `MIGRATION_8_9` (creates the `song_playback_params` table). Change the entry responsibility line from "11 entities ... 9 DAOs and 7 migrations (1_2 through 7_8)" to "12 entities (plus the PlaylistWithSongs relation), 10 DAOs and 8 migrations (1_2 through 8_9)".
- `playback/PlaybackService.kt` — owns the A-B loop (`SET_AB_REPEAT`/`GET_AB_REPEAT`) with one handler wake-up at B (speed-aware, cleared on media item transition) and per-song speed/pitch application on transition (`SET_SONG_PLAYBACK_PARAMS`/`RESET_SONG_PLAYBACK_PARAMS`), falling back to the global tempo.
- `playback/Playback.kt` — new commands/extras listed above.
- `ui/screen/player/PlayerViewModel.kt` — mirrors A-B state and staged A, restores it on controller connection, persists/reads per-song overrides and exposes them to settings.
- `ui/component/base/AbsPlayerFragment.kt` — player overflow action for the A→B→clear cycle with toast feedback.
- `ui/screen/player/cover/CoverPagerFragment.kt` — hosts the A-B chip ComposeView.
- `ui/component/compose/lyrics/LyricsView.kt` — optional `onLineLongPress` callback (combined click) without changing tap-to-seek.
- `ui/screen/lyrics/LyricsScreen.kt` — shows the A-B line sheet in the synced branch.
- `ui/screen/sound/SoundSettingsScreen.kt` / `SoundSettingsFragment.kt` — per-song controls; the fragment injects the activity-scoped `PlayerViewModel`.
- `util/BackupHelper.kt` / `util/backup/BackupArchivePolicy.kt` / `util/backup/BackupEntryModels.kt` / `util/RestoreSummary.kt` — backup, restore, archive path and `BackupContent.PlaybackParams` for per-song parameters.
- `MainModule.kt` — DB migration 8→9, DAO factory and the extra `PlayerViewModel` dependency.

## AGENTS.md

Repository Code Map updates:

- In `core/` bullet, change "Room database (v8) with 11 entities, 9 DAOs and migrations through 7→8" to "Room database (v9) with 12 entities, 10 DAOs and migrations through 8→9".
- In `playback/` bullet, add: "A-B repeat (service-owned, single B wake-up) and per-song speed/pitch overrides applied on media item transition".
- In `ui/` bullet, mention the A-B chip (`AbRepeatChip`), the lyrics long-press sheet (`AbRepeatLineSheet`) and `SongTempoOverrideControls`.
- In `data/local/room/` bullet, add `song_playback_params` to the entity list (12 entities, 10 DAOs).
- In `util/` bullet, mention per-song playback parameters in BackupHelper.
- Keep the existing rule text unchanged.

## docs/agents/domain.md

- If the ADR below is accepted, increment the ADR count to 52 and add the title to the ADR list/reference.

## Decisions

- **"Apply to this song only" pins the current tempo values.** The sound sheet sliders still edit the
  global tempo first; the new action stores the current speed and effective pitch for the playing
  song. It does not roll the global tempo back to a pre-edit value, because that value is not
  otherwise tracked and would surprise users who also intended the global change. "Reset song"
  deletes the row and immediately reapplies the global tempo.
- **Stored pitch is the effective pitch.** When the global fixed-pitch lock is on, the stored pitch
  equals the speed, so a per-song override keeps the same audible result even if the lock later
  changes.
- **A-B repeat lives in the playback service.** The UI only mirrors it; the loop keeps running when
  the player sheet is closed, is cleared on every media item transition and is recovered by the UI
  through `GET_AB_REPEAT` after controller reconnection.
- **Minimum A-B range is 500 ms.** Shorter selections are treated as an accidental second tap and
  are rejected with a toast.
- **Backup section is optional on restore only.** Backups always include the section when rows
  exist; the restore picker exposes it as its own content type, and invalid/out-of-range rows are
  skipped without failing the rest of the archive.

## New ADR (optional, if the docs agent treats new persistence and service-owned playback state as architectural)

`docs/architecture/ADR-052-in-track-ab-repeat-and-per-song-tempo.md`

```
Status: Accepted

Context
The player already exposes a global tempo (speed/pitch) held in the Equalizer DataStore and
applied by PlaybackService. Listeners also need to loop part of a track, and some tracks need a
different tempo than the rest of the library. Both features must survive UI recreation and must
not add polling to the playback path.

Decision
- A-B repeat is owned by PlaybackService. The UI sends SET_AB_REPEAT with absolute track
  positions; the service validates/clamps the range, seeks into it when needed and schedules a
  single Handler wake-up at B (delay divided by the current playback speed). Seeks, play-state
  changes and playback-parameter changes reschedule the wake-up. A media item transition clears
  the loop. GET_AB_REPEAT lets a recreated UI recover the active range. The commands reuse the
  same-UID app-command pattern defined in ADR-045.
- Per-song speed/pitch overrides live in a new Room table (SongPlaybackParamEntity, DB v9,
  migration 8->9). On every media item transition the service applies the row when present and
  otherwise applies the global tempo. Commands persist/delete the row and apply it immediately to
  the current item. The global tempo collector only writes playback parameters while no per-song
  override is active.

Consequences
- A-B repeat and per-song tempo continue while the player UI is not visible, and neither depends
  on UI polling.
- The global slots still show the global tempo while a per-song override is playing; the sound
  sheet states the song's stored values and offers Reset song.
- Backup/restore gains an optional JSON section for the new table.
- PlaybackParameters changes made outside this bookkeeping (future features) must respect the
  per-song override guard.
```
