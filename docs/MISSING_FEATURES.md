# Musicolet vs ssilencce — Feature Gaps & Superiorities

Complete comparison of user-facing features between Musicolet (see `docs/MUSICOLET_FEATURES.md`)
and ssilencce (see `docs/FEATURES.md`).

- **Part 1 — Features missing from ssilencce:** present in Musicolet, absent from ssilencce.
- **Part 2 — Shared features, better in Musicolet:** present in both apps, but Musicolet's
  implementation is more capable; the specific absent sub-features are cross-referenced to
  Part 1 instead of being repeated.

# Part 1 — Musicolet Features Missing from ssilencce

Items marked **(partial)** exist in ssilencce in a limited or different form; the gap is
described. Items in the appendix are not applicable to an open-source app.

## 1. Playback

- **Gapless playback** — experimental toggle (default on).
- **Crossfade** between tracks (PRO).
- **Fade-in on play / fade-out on pause** — configurable duration (0–4000 ms each).
- **A-B repeat** loop within a track.
- **Per-song play speed** — speed/pitch per individual song (ssilencce has global tempo only).
- **Mute handling** — don't play when muted, pause when muted, resume when unmuted.
- **Previous-button behavior** — configure as restart / play previous; dedicated next/prev override to FF/Rewind for headsets/remotes and configurable FF/Rewind durations remain missing.
- **Queue control actions** — "Load next song and pause", "Pause after this song", "Play after current song".
- **Session survival beyond force-stop** — ssilencce now opts into restoring an active session after a system service/process restart; Android force-stop and an OS kill that never restarts the service remain outside app control.
- **Preview a song** without adding it to any queue.
- **Resume policies** — "only resume if paused by disconnect", "prevent unwanted autoplay" (strict mode for buggy devices/cars).
- **"FF/Rewind only for long audio (audiobook/podcast)"** option.
- **Extra format playback (WMA, APE, DSF, DFF, AIFF)** — ssilencce bundles the FFmpeg software decoder (`media3-ffmpeg-decoder`, auto-loaded by Media3's extension renderer for unknown codecs), so the codecs exist, but these formats still do not play: Media3 1.10.1 ships no container extractors for ASF/WMA, APE, DSF/DFF or AIFF (the demuxer fails before the decoder is fed), Android's MediaScanner cannot determine their duration (NULL `duration` is then dropped by the minimum-duration filter) and does not index `.ape` files at all. Verified on emulator (API 36) 2026-08-14. Also missing vs Musicolet's own decoder: system-decoder switch, decoder info UI and multi-threaded decode.

## 2. Queues

- **Named-queue merge** - duplicate save/rename now offers Replace existing / Keep both / Rename; merging two saved queues remains deferred.
- **Automatic oldest-queue deletion at the cap** - ssilencce enforces 100 named queues and rejects new saves with an explicit error; it does not delete a queue automatically.

## 3. Shuffle & repeat

- **Randomize mode** (third shuffle state beyond on/off).
- **Shuffle behaviors** — "only shuffle below the current song", "shuffle all songs of each queue".
- **Shuffle queue visible & reorderable**.
- **"Shuffle and then…"** and **"shuffle and play"** from multi-select.
- **Repeat "load-next-and-pause"** mode.

## 4. Equalizer & sound

- **Configurable band count** — 3 / 5 / 10 / 15 / 31 bands (ssilencce engines use a fixed band count).
- **Per-screencast-device EQ** (PRO).
- **Stereo/mono output** switch.
- **Surround sound** effect.
- **Reverb effects** (experimental) — room level/HF, decay time/ratio, reflection level/delay, reverb level/delay, diffusion.
- **"Convert RG to constant volume"** — a non-RG preamp is implemented; conversion is not.
- **Headroom handling** and channel-balance warning.
- **Musicolet EQ vs System equalizer** engine choice.

## 5. Library & browsing

- **Sub-categorization** - albums by artist/album-artist, artists by albums, and genre album-artist/composer subcategories; album composer filtering and genre Songs/Albums/Artists subcategories are available.
- **Play-statistics sort keys** — date last played and most/least played; ssilencce already sorts by filename, path, track number and date modified with numeric-aware collation.
- **Album-merge strategy** — user-configurable fields that decide album equality.
- **Disc number** support.
- **Multi-artist / multi-album-artist / multi-composer / multi-genre tags** with custom separators (PRO).
- **"Use album-artist if artist missing"** fallback.
- **Third-line list info** — date modified and other extra fields only append to the existing secondary text; compact album/artist song rows and Home song suggestions have no secondary text view, and a configurable multi-field list picker is not available.
- **Genre duration-filter parity** - minimum/maximum duration filters apply to the shared song cursor (songs, albums/artists, years, folders, search and their detail lists); genre detail lists, genre search and genre counts still use raw genre-member queries, and the unknown-genre list uses an explicit unfiltered cursor, so all four remain outside the duration filters.

## 6. Scanning & folder management

- **Welcome/setup wizard** — pick storage (SAF, per-volume access), choose folders to scan, set a **main music folder**, tutorials.
- **Scan options** — hidden folders, `.nomedia` handling, **scan video files as audio**.
- **Rescan controls** — force rescan, scan on app start, scanner choice (Musicolet vs System), auto rescan on media mount/unmount.
- **Linear or hierarchical folder structure** toggle in the Folders tab.
- **Folder info** (creation time, songs, sub-folders) and **rename folders**.
- **Exclude directly from the Folders tab** (ssilencce configures whitelist/blacklist in settings).
- **Ghost mode** — hidden folders kept out of the library but searchable via GhostSearch (also from the lock screen).
- **Remove missing songs / hidden songs** from the library.

## 7. Tags, lyrics & album art

- **Multi-song (batch) tag editing** across all fields and formats (ssilencce now has a multi-select text-field editor with Keep/Set/Clear and per-file values and per-field read-back; the legacy album/artist editors now aggregate every file, write only changed fields and confirm destructive writes; batch covers, session undo, in-place write backup/restore, single-entity routing and the full runtime format matrix remain missing).
- **Create/edit synchronized LRC in-app** (ssilencce's editor has no sync/timing editing).
- **A-B repeat on selected lyric lines**.
- **Select/copy/share lyric lines**.
- **Lyrics source selection per song** (embedded or any `.lrc` file).
- **Bookmarks/Notes per song** — stored as shareable text files, importable/editable by other apps, quick bookmark button in Now Playing.
- **Hide website info from tags** (current song / everywhere).
- **Album-art save to gallery** (fullscreen zoom viewer).
- **Set album art as lock-screen wallpaper** (own lock screen).
- **Disable album-art loading everywhere** (ssilencce only ignores MediaStore covers).

## 8. File operations

- **Move / copy songs** between folders.
- **Rename folders** and **create folders**.
- **Share screenshot of Now Playing** (with Instagram-fit crop option).
- **Audio cutter / trimmer** — trim start/end or extract a portion as a new file.
- **Set as ringtone** (built on the cutter).
- **Mini Player popup** when opening files from other apps via VIEW intents.

## 9. Now Playing screen

- **Seekbar helper buttons** — seek-to precise position, cancel seeking, touch-only-on-thumb.
- **FF/Rewind button visibility** — always / only for long audio.
- **Keep screen on**.
- **Custom quick access shortcuts**.
- **Album-art tap action** customization.

## 10. Musicolet Lock Screen & screen saver

- **Custom full-screen player over the system lock screen** (`showWhenLocked`) — playback controls, queue and lyrics views, swipe-up to dismiss, landscape support, gestures, custom wallpaper, launch shortcuts (artist/folder/genre/playlist), skip when paused >1 min or device unlocked, OEM lock-screen permission guides. (Entirely absent from ssilencce.)

## 11. Quick access

- **MIUI-specific launcher-shortcut permission guide** — dynamic album/artist/folder/playlist shortcuts and the Quick Settings tile are implemented; the OEM-specific permission walkthrough is still absent.

## 12. Notifications

- **Notification styles** — small / big simple / big advance.
- **Upcoming songs** in the expanded notification.
- **Replace close button with "add to playlists"**.
- **Favorite button** in the notification.
- **Always-expanded** option and expandable pre-Android 11.
- **Broadcast album art to system lock screen** (toggle).
- Configurable **compact/expanded button arrangement** and background color.

## 13. Cast & Screencast (PRO)

- **Chromecast** — Cast SDK, decode-on-device option, companion "cast plugin" app.
- **Screencast** — Chromecast and Miracast / Wireless Display without mirroring; EQ, ReplayGain and crossfade keep working during screencast. (Entirely absent from ssilencce.)

## 14. Headset, Bluetooth & USB

- **Dedicated Next/Previous override to FF/Rewind** and per-device filtering remain missing; configurable single/double/triple/long-press mapping, optional 4+ Seek forward, 250–600 ms delay and the OEM/AVRCP disclaimer are implemented.

## 15. Backup & restore

- **Share the backup ZIP directly from the app** - no remaining gap for manual backups: a successful manual backup opens the "Backup saved" sheet and can share the archive. Scheduled archives remain in the user-chosen SAF folder without auto-share; that is intended behavior, not a missing feature.

## 16. Export / import

- Device-playlist import conflict handling (Keep both, Replace existing, Rename imported) is implemented; no remaining gap. Backup playlist restore intentionally keeps its separate non-interactive merge-into-existing policy instead of showing this dialog.

## 17. Interface & theming

- **Per-entity option/menu customization** — main menu, quick access, song/folder/album/artist/genre/playlist option lists.
- **Show/hide unimportant settings** (SmartPrefCategory).
- **Accent color** picker (ssilencce derives accent from theme modes/cover).

## 18. OEM support & battery guides

- **Guides per OEM** (MIUI, EMUI/Huawei, Samsung, OPPO, Realme, Vivo, ASUS) for battery optimization / app-killing, **pin in recents**, auto-start, disable optimization.
- **Background-playback opt-in** (Android 11+), unrestricted battery access (Android 15+), WRITE_SETTINGS for status bar.

## Appendix A — Partial overlaps (Musicolet feature exists in reduced form)

- **Sleep timer** — Musicolet adds stop-after-N-songs, timer history with 1-tap reuse, "stop only music if UI is foreground" (ssilencce has countdown, exact HH:MM end time, fade-out, quit-after-end).
- **Advance Shuffle** — album/artist plus album-artist/composer most-played strategies are available; composer strategies require Android 11/API 30+ (matching the Composers category), while album-artist strategies work on all supported versions. Per-entity within-group sorting rules are fixed rather than user-configurable.
- **ReplayGain** — both can calculate tags; Musicolet also converts to constant volume. ssilencce source implements standard/WAV-ID3v2 (explicit RIFF `ID3 ` chunk)/iTunes/Opus tag calculation and writes album gain only for complete albums; the current AA-06 checkpoint is not yet independently accepted, and final review, frozen build and device WAV/M4A gates remain pending. It does not convert to constant volume.
- **Tag editor** — Musicolet batch-edits any field across formats; ssilencce now has a multi-select text-field editor with Keep/Set/Clear and per-file/per-field read-back; the legacy album/artist editors now aggregate every file, write only changed fields and confirm destructive writes; batch covers, session undo, in-place write backup/restore, single-entity routing and the full runtime format matrix remain missing.
- **Album art change/remove** — both support it; Musicolet adds fullscreen zoom, save to gallery, lock-screen wallpaper.
- **Backup** - both ZIP-based and both restore selectively (ssilencce's picker preselects nothing); automatic scheduled backup, structural verification, direct sharing of a manual backup from the post-backup sheet and selective restore of favorites, play counts, named queues and per-file resume positions are now implemented (ADR-043, ADR-048). No remaining backup/restore gap.
- **Widgets** - both have 3 widgets; ssilencce now has per-widget System/AlbumArt/Wallpaper themes, transparency with heuristic alpha floors, empty-state layouts and configure/reconfigure. AA-10 V4 source is live-integrated and independently reviewed; it remains unbuilt and runtime-pending. Measured bounds, TalkBack, contrast, wallpaper fallback, per-instance/delete-re-add, empty-state, API 26/27, exact-fit rounding and transparency runtime gates remain open. No AA-10 PASS.
- **Last.fm/ListenBrainz scrobbling** — ssilencce-only (not listed as gap).
- **Updater** — present in both (Musicolet via changelog checker, ssilencce via GitHub releases); no gap.

## Appendix B — Not applicable to ssilencce

- **PRO / IAP** — one-time purchase and subscriptions (Billing 8.3.0); ssilencce is free and open source.
- **Cast plugin companion app** detection.
- **"Our other apps" / Facebook / X / Instagram / Reddit / Telegram / YouTube / WhatsApp links** — store-identity features.
- **PairIP protection / obfuscation** — closed-source packaging concerns.

## Notes (Part 1)

- Source of gaps: `docs/FEATURES.md` (ssilencce, v-current) vs `docs/MUSICOLET_FEATURES.md` (Musicolet 6.14 build 530).
- "PRO" markers indicate features that are paid in Musicolet; they still count as gaps for ssilencce.
- ssilencce-only features (Media3 engine, smart shuffle modes, lyrics syllable/karaoke mode, AutoEq import, online lyrics providers, sync'd lyrics via TTML, Android Auto with media IDs, crash handler with reports, etc.) are intentionally not listed here.

# Part 2 — Shared Features, Better in Musicolet

Features that both apps have, but where Musicolet's implementation is more capable.
"Better" is judged strictly on documented capabilities, not subjective quality. Where a
specific absent sub-feature was already listed in Part 1, it is referenced instead of repeated.

## 1. Sleep timer

- Musicolet: stop after N songs, stop after this song, gradual fade-out, timer history with
  1-tap reuse, reset, "stop only music if UI is foreground".
- ssilencce: countdown, exact HH:MM end time, fade-out, quit-after-end.
- See Part 1 §1 (timer history, stop-after-N).

## 2. Audio focus handling

- Musicolet: 4 levels — normal, ignore focus, ask before taking focus, resume after call.
- ssilencce: all four modes are wired through one controller (ADR-042, ADR-045), with a restorable request-bound Ask dialog; repeated play preserves consent, explicit/no-op pause and confirm/cancel/mode/release invalidate it. The remaining Musicolet-specific gap is strict autoplay policy on buggy car/device stacks.

## 3. Pause/resume on BT & USB

- Musicolet: "only resume if paused by disconnect", "prevent unwanted autoplay" with strict
  mode for buggy devices/cars, resume on wired/BT connect.
- ssilencce: resume on connect, pause on disconnect, pause at zero volume.
- See Part 1 §1 (resume policies, strict mode).

## 4. FF/Rewind & track navigation

- Musicolet: configurable FF/Rewind durations, next/prev button override to FF/Rewind for
  headsets/remotes, FF/Rewind visible only for long audio.
- ssilencce: configurable seek interval (5-60 s), rewind with back button, and configurable headset single/double/triple/long-press mapping with optional 4+ Seek forward; remaining gaps are dedicated Next/Previous override and configurable FF/Rewind durations.
- See Part 1 §1 (override, configurable durations, long-audio-only).

## 5. Playback speed & pitch

- Musicolet: up to 4 decimal places, per-song play speed, simple or advanced mode.
- ssilencce: global tempo (speed/pitch) control only.
- See Part 1 §1 (per-song speed).

## 6. Session survival

- Musicolet: `TaskSaviour` service restores playback after the app process is killed.
- ssilencce: opt-in recovery restores an active queue/position after a system service restart
  (ADR-042); Android force-stop and OS kills that never restart the service cannot be bypassed.

## 7. Equalizer

- Musicolet: own engine or System equalizer, configurable band count (3/5/10/15/31),
  preamp, per-output-device profiles, per-Bluetooth-device EQ, EQ presets save/delete.
- ssilencce: two engines (classic Equalizer + DynamicsProcessing) with fixed band counts,
  bass boost, virtualizer, loudness, compressor, limiter, profiles with broad output-type and
  exact hashed per-device associations, and JSON/AutoEq import-export (Musicolet lacks
  import/export).
- See Part 1 §4 (band count, per-BT device).

## 8. ReplayGain

- Musicolet: read *and calculate* RG tags, album/track normalization, RG preamp + non-RG
  preamp, use-embedded-tags option, skip-or-reload, "convert RG to constant volume",
  headroom handling.
- ssilencce: reads and calculates standard, iTunes and Opus R128 tags; album/track modes,
  with-tag and non-RG preamps, peak limiting on 16/24-bit PCM. Calculation writes verified tags
  and writes album gain only when the complete album is selected (ADR-049).
- Remaining: "convert RG to constant volume", use-embedded-tags toggle, skip-or-reload and
  user-facing headroom control. See Part 1 §4 (constant-volume conversion).

## 9. Shuffle & repeat modes

- Musicolet: on/off/randomize, shuffle behaviors (per-queue, below-current-song), shuffle
  queue visible & reorderable, "shuffle and then…", "shuffle and play", repeat
  load-next-and-pause.
- ssilencce: repeat/shuffle on/off plus unique smart shuffle modes (most played, favorites,
  forgotten, recently added, by-group, album/artist strategies).
- See Part 1 §3 (randomize, behaviors, shuffle queue).

## 10. Sorting

- Musicolet: 14+ keys (title, filename, track number, album, artist, duration, year, date
  added, date last played, date modified, most/least played, path), ignore-keyword whitelist,
  experimental alphanumeric sorting.
- ssilencce: locale-aware numeric collator (covers natural/alphanumeric ordering), article-ignoring (The/A/El…) with a user-editable keyword list, per-entity sort keys including filename, path, track number and date modified.
- See Part 1 §5 (date last played, most/least played).

## 11. Play statistics

- Musicolet: Most Played with periods (week/month/12 months/year/all time), artist stats
  with play percentage, manual and automatic CSV export.
- ssilencce: period-aware Top Tracks/Top Artists with percentages, manual current-period CSV
  export (ADR-040) and optional automatic weekly/monthly/yearly export of the latest completed
  period; no remaining statistics CSV gap.

## 12. Playback history

- Musicolet: per-song playback history with clear-history for one/many songs.
- ssilencce: history tab with configurable cutoff and individual/multi-select removal; the
  deletion gap is closed.

## 13. Queue screen

- Musicolet: multiple queues with counter (position + total), scroll-to-current,
  swipe-right-to-remove, drag-reorder incl. multiple selected songs, album-art show/hide,
  quick search box.
- ssilencce: named queues, queue-local search, highlighted current song, position/total
  counter, scroll-to-current, swipe-right-to-remove with Undo, single-item drag reordering and
  key-based multi-select block drag (ADR-041); queue filtering keeps original indices for actions.
  Selection/drag are disabled during search or lock, and the full queue/current/stop positions are
  persisted after a block move.

## 14. Android Auto

- Musicolet: browses folders, album-artists, composers and playlists; FF/Rewind, favorite,
  current queue, shuffle controls; exported ContentProvider serving album art.
- ssilencce: hierarchical browsing via media IDs and automotive descriptor.
- See Part 1 §5 (browse types).

## 15. Media notifications

- Musicolet: small / big simple / big advance styles, configurable compact/expanded button
  arrangement, upcoming songs, favorite button, replace-close-with-add-to-playlist,
  always-expanded, broadcast art to system lock screen.
- ssilencce: standard media notification from `PlaybackService`.
- See Part 1 §12 (styles, upcoming songs, buttons).

## 16. Widgets

- Musicolet: 3 widgets (4×1, 4×1 Advanced, 4×3), horizontal resize, transparency, per-widget
  themes ("match system", "adapt from album art", "adapt from wallpaper"), per-widget
  configuration, empty-state layouts.
- ssilencce: 3 Glance widgets (responsive, card, full) with per-widget System/AlbumArt/Wallpaper
  themes, 0-100% transparency (0.35 surface / 0.30 scrim floors), empty-state layouts,
  configure/reconfigure and existing dynamic colors/layout style/third-line/corner radius.

## 17. Tag editor

- Musicolet: multi-song batch editing of every field across all formats (ID3v1/v2, MP4,
  FLAC/Vorbis, OGG/Opus, WAV, AIFF, WMA, DFF, DSF), batch artwork replace/delete.
- ssilencce: multi-select text-field batch editor (17 fields, Keep/Set/Clear, common/Varies
  aggregation, per-file values and per-file/per-field read-back), full single-song editing, guarded album/artist editors,
  covers via Photo Picker, MusicBrainz search (Musicolet lacks MusicBrainz integration).
- Remaining: batch artwork replace/delete, single-entity routing to the batch editor
  (the aggregation/dirty-map guard and destructive confirmation are applied; the single-entity action is still a legacy editor), the
  runtime format matrix, session undo/rollback and in-place write backup/restore.
- See Part 1 §7 (full batch).

## 18. Backup & restore

- Musicolet: single ZIP with choose-what-to-restore, automatic backup with custom folder,
  share the ZIP, verification, restore warnings.
- ssilencce: selective ZIP backup/restore with structural verification and warnings
  (empty/non-backup/unrestorable), including settings, playlists, favorites, play counts, named
  queues, per-file resume positions, lyrics and artist images; opt-in scheduled backups to a SAF
  folder and direct sharing of a manual backup from the post-backup sheet are also implemented
  (ADR-043, ADR-048). Scheduled archives remain in the user-chosen folder without auto-share;
  this is intended behavior, not a gap.

## 19. Playlist import/export

- Musicolet: M3U/M3U8/PLS export and import with conflict handling (replace/keep/rename).
- ssilencce: M3U/M3U8/PLS export and device-playlist import with Keep both, Replace existing and
  Rename imported; creates/replacements are transactional. Backup playlist restore keeps a
  separate non-interactive merge policy. No remaining conflict-handling gap for device import.

## 20. Album art handling

- Musicolet: change/remove per song or batch, `folder.jpg`/`cover.jpg`/`<album>.jpg`
  fallback, fullscreen zoom viewer, save to gallery, set as lock-screen wallpaper, disable
  loading everywhere.
- ssilencce: embedded/folder/MediaStore art with online covers (Deezer) and artist images
  (Musicolet lacks online covers), ignore-MediaStore option.
- See Part 1 §7 (zoom viewer, gallery, wallpaper, disable-loading).

## 21. App shortcuts

- Musicolet: play/search/shuffle plus launcher shortcuts for specific albums, artists,
  folders and playlists, ask-before-adding option.
- ssilencce: four built-in dynamic shortcuts plus per-entity album/artist/folder/playlist
  shortcuts with confirmation (ADR-044); the remaining gap is the MIUI-specific permission
  guide.

## 22. Search integration

- Musicolet: quick search box at the bottom of every list, search history, system search
  with `MEDIA_PLAY_FROM_SEARCH`.
- ssilencce: inline quick search on all seven top-level lists, the queue, the generic
  detail/statistics list and the song sections of album, artist, genre, year, folder and playlist
  detail (playlist filtering is drag-safe); settings search; global search with voice input,
  debounce, grouped results, filters, scoped searches and `searchable.xml`. Search history is
  still absent.

## Notes (Part 2)

- Where ssilencce clearly wins or matches (lyrics with syllable/karaoke mode and online
  providers, Material You theming, scrobbling, smart shuffle, crash handler, EQ import/export),
  the feature is excluded from this list or the advantage is called out inline.
- "Better" is a capability comparison from the two reference documents, not an opinion on
  implementation quality.
