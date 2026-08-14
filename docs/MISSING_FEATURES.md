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
- **Per-track remember last position** — resume each song from where it stopped.
- **Per-song play speed** — speed/pitch per individual song (ssilencce has global tempo only).
- **Mute handling** — don't play when muted, pause when muted, resume when unmuted.
- **Previous-button behavior** — configure as restart / play previous; next/prev override to FF/Rewind (headsets, remotes); configurable FF/Rewind durations.
- **Queue control actions** — "Load next song and pause", "Pause after this song", "Play after current song".
- **Session survival** — `TaskSaviour` service restores playback after the app process is killed.
- **Preview a song** without adding it to any queue.
- **Resume policies** — "only resume if paused by disconnect", "prevent unwanted autoplay" (strict mode for buggy devices/cars).
- **Audio focus level "ask"** — 4 focus levels including ask-before-taking focus and resume-after-call (ssilencce only has an on/off ignore-focus toggle).
- **"FF/Rewind only for long audio (audiobook/podcast)"** option.
- **Extra format playback (WMA, APE, DSF, DFF, AIFF)** — ssilencce bundles the FFmpeg software decoder (`media3-ffmpeg-decoder`, auto-loaded by Media3's extension renderer for unknown codecs), so the codecs exist, but these formats still do not play: Media3 1.10.1 ships no container extractors for ASF/WMA, APE, DSF/DFF or AIFF (the demuxer fails before the decoder is fed), Android's MediaScanner cannot determine their duration (NULL `duration` is then dropped by the minimum-duration filter) and does not index `.ape` files at all. Verified on emulator (API 36) 2026-08-14. Also missing vs Musicolet's own decoder: system-decoder switch, decoder info UI and multi-threaded decode.

## 2. Queues

- **Multiple numbered queues (3+)** with active-queue concept (ssilencce has a single queue).
- **End-of-queue behaviors** — resume next/previous queue from its last position, re-shuffle the queue, jump to next/first queue.
- **Save queue as playlist**; duplicate-name handling (**replace / merge / keep both**).
- **Queue counter** (current position + total) and scroll-to-current.
- **Max queue limit** with auto-delete of oldest.
- **Swipe-right-to-remove** in the queue tab (toggle).
- **Confirm before removing queues**.
- **Clear playback history** for one/many songs.

## 3. Shuffle & repeat

- **Randomize mode** (third shuffle state beyond on/off).
- **Shuffle behaviors** — "only shuffle below the current song", "shuffle all songs of each queue".
- **Shuffle queue visible & reorderable**.
- **"Shuffle and then…"** and **"shuffle and play"** from multi-select.
- **Advance Shuffle** for album-artists and composers (ssilencce covers albums/artists/genres/folders only).
- **Repeat "load-next-and-pause"** mode.

## 4. Equalizer & sound

- **Configurable band count** — 3 / 5 / 10 / 15 / 31 bands (ssilencce engines use a fixed band count).
- **Per-Bluetooth-device EQ** — separate profile per paired BT device.
- **Per-screencast-device EQ** (PRO).
- **Stereo/mono output** switch.
- **Surround sound** effect.
- **Reverb effects** (experimental) — room level/HF, decay time/ratio, reflection level/delay, reverb level/delay, diffusion.
- **ReplayGain calculation** — read *and calculate* RG tags (ssilencce only reads them).
- **Non-RG preamp** and **"convert RG to constant volume"**.
- **Headroom handling** and channel-balance warning.
- **Musicolet EQ vs System equalizer** engine choice.

## 5. Library & browsing

- **Composers tab** and **Album-Artists tab** (ssilencce has album-artist support inside Artists only).
- **Queues tab** in the library.
- **Sub-categorization** — albums by artist/album-artist/composer, artists by albums, albums sorted by year, genre sub-categorization.
- **Extra sort keys** — filename, track number, path, date modified, date last played, most/least played, alphanumeric sorting.
- **Ignore-keyword whitelist** (custom list beyond A/An/The).
- **Album-merge strategy** — user-configurable fields that decide album equality.
- **Disc number** support.
- **Multi-artist / multi-album-artist / multi-composer / multi-genre tags** with custom separators (PRO).
- **"Use album-artist if artist missing"** fallback.
- **Quick search box at the bottom of every list** (songs, queues, folders…).
- **Filename-vs-title display** toggle and **extra list info** (date updated, etc.).
- **Most Played periods** — this week / month / 12 months / year / all time; artist stats with play percentage.
- **CSV export** of most-played songs/artists — manual and automatic (weekly/monthly/yearly, PRO).
- **"Not played in past N days"** filter.

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

- **Multi-song (batch) tag editing** across all fields and formats (ssilencce has batch save but limited scope).
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

- **Quick Settings tile** with active state.
- **Add album/artist/folder/playlist shortcut to launcher** (with ask-before-adding option and MIUI guide; ssilencce has only the 4 built-in app shortcuts).

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

- **Earphone double/triple press configuration**, 4+ presses = fast-forward, allowed-delay tuning, disclaimer.

## 15. Backup & restore

- **Choose what to restore** (settings, playlists, favorites, play counts, queues, per-file settings).
- **Automatic backup** with custom folder.
- Backup **verification** and restore prompt/warnings. (ssilencce has a simple ZIP backup/restore.)

## 16. Export / import

- **M3U8 / PLS export** (ssilencce exports `.m3u` only).
- **Conflict handling** on playlist import (replace / keep / rename).
- **CSV export** of statistics.

## 17. Interface & theming

- **Settings search**.
- **Per-entity option/menu customization** — main menu, quick access, song/folder/album/artist/genre/playlist option lists.
- **Show/hide unimportant settings** (SmartPrefCategory).
- **Accent color** picker (ssilencce derives accent from theme modes/cover).

## 18. OEM support & battery guides

- **Guides per OEM** (MIUI, EMUI/Huawei, Samsung, OPPO, Realme, Vivo, ASUS) for battery optimization / app-killing, **pin in recents**, auto-start, disable optimization.
- **Background-playback opt-in** (Android 11+), unrestricted battery access (Android 15+), WRITE_SETTINGS for status bar.

## Appendix A — Partial overlaps (Musicolet feature exists in reduced form)

- **Sleep timer** — Musicolet adds HH:MM form, stop-after-N-songs, timer history with 1-tap reuse, "stop only music if UI is foreground" (ssilencce has countdown, fade-out, quit-after-end).
- **Queue drag-reorder** — Musicolet adds dragging of multiple selected songs (ssilencce drags single items).
- **Exclude by duration** — Musicolet filters by maximum; ssilencce filters by minimum duration.
- **EQ per output device** — Musicolet splits speaker/headset/BT; ssilencce has one per-output-device association.
- **Advance Shuffle** — ssilencce has album/artist shuffle strategies but not composer/album-artist or all sorting rules.
- **ReplayGain** — Musicolet can calculate tags and convert to constant volume; ssilencce only reads them.
- **Tag editor** — Musicolet batch-edits any field across formats; ssilencce's batch save is limited.
- **Album art change/remove** — both support it; Musicolet adds fullscreen zoom, save to gallery, lock-screen wallpaper.
- **Backup** — both ZIP-based; Musicolet adds selective restore, automatic backups and verification.
- **Widgets** — both have 3 widgets; Musicolet adds transparency, per-widget themes and empty-state layouts.
- **App shortcuts** — Musicolet adds launcher shortcuts for specific albums/artists/folders/playlists.
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

- Musicolet: HH:MM form, stop after N songs, stop after this song, gradual fade-out, timer
  history with 1-tap reuse, reset, "stop only music if UI is foreground".
- ssilencce: countdown, fade-out, quit-after-end.
- See Part 1 §1 (timer history, stop-after-N, HH:MM).

## 2. Audio focus handling

- Musicolet: 4 levels — normal, ignore focus, ask before taking focus, resume after call.
- ssilencce: optional ignore-audio-focus toggle only.

## 3. Pause/resume on BT & USB

- Musicolet: "only resume if paused by disconnect", "prevent unwanted autoplay" with strict
  mode for buggy devices/cars, resume on wired/BT connect.
- ssilencce: resume on connect, pause on disconnect, pause at zero volume.
- See Part 1 §1 (resume policies, strict mode).

## 4. FF/Rewind & track navigation

- Musicolet: configurable FF/Rewind durations, next/prev button override to FF/Rewind for
  headsets/remotes, FF/Rewind visible only for long audio.
- ssilencce: configurable seek interval (5–60 s), rewind with back button.
- See Part 1 §1 (override, long-audio-only).

## 5. Playback speed & pitch

- Musicolet: up to 4 decimal places, per-song play speed, simple or advanced mode.
- ssilencce: global tempo (speed/pitch) control only.
- See Part 1 §1 (per-song speed).

## 6. Session survival

- Musicolet: `TaskSaviour` service restores playback after the app process is killed;
  "start playback when app starts".
- ssilencce: queue/position restored on app start.
- See Part 1 §1 (survival after process kill).

## 7. Equalizer

- Musicolet: own engine or System equalizer, configurable band count (3/5/10/15/31),
  preamp, per-output-device profiles, per-Bluetooth-device EQ, EQ presets save/delete.
- ssilencce: two engines (classic Equalizer + DynamicsProcessing) with fixed band counts,
  bass boost, virtualizer, loudness, compressor, limiter, profiles with per-output-device
  associations and JSON/AutoEq import-export (Musicolet lacks import/export).
- See Part 1 §4 (band count, per-BT device).

## 8. ReplayGain

- Musicolet: read *and calculate* RG tags, album/track normalization, RG preamp + non-RG
  preamp, use-embedded-tags option, skip-or-reload, "convert RG to constant volume",
  headroom handling.
- ssilencce: read-only (standard, iTunes and Opus R128 tags), album/track modes, preamp,
  peak limiting on 16/24-bit PCM.
- See Part 1 §4 (calculation, non-RG preamp, constant-volume conversion).

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
- ssilencce: locale-aware collator, article-ignoring (The/A/El…), per-entity sort keys.
- See Part 1 §5 (extra keys, whitelist, alphanumeric).

## 11. Play statistics

- Musicolet: Most Played with periods (week/month/12 months/year/all time), artist stats
  with play percentage, manual and automatic CSV export.
- ssilencce: per-song play/skip counters, time played, top-100 flows, top albums/artists.
- See Part 1 §5 (periods, CSV).

## 12. Playback history

- Musicolet: per-song playback history with clear-history for one/many songs.
- ssilencce: history tab with configurable cutoff interval, no per-song clearing.
- See Part 1 §2 (clear history).

## 13. Queue screen

- Musicolet: multiple queues with counter (position + total), scroll-to-current,
  swipe-right-to-remove, drag-reorder incl. multiple selected songs, album-art show/hide,
  quick search box.
- ssilencce: Compose queue UI with highlighted current song, search, drag reordering and
  filters.
- See Part 1 §2 (multi-queue, counter, swipe-remove).

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
- ssilencce: 3 Glance widgets (responsive, card, full) with dynamic colors, layout style,
  third-line content, corner radius.
- See Part 1 Appendix A (themes, transparency, empty states).

## 17. Tag editor

- Musicolet: multi-song batch editing of every field across all formats (ID3v1/v2, MP4,
  FLAC/Vorbis, OGG/Opus, WAV, AIFF, WMA, DFF, DSF), batch artwork replace/delete.
- ssilencce: full field editing with batch save (limited), covers via Photo Picker,
  MusicBrainz search (Musicolet lacks MusicBrainz integration).
- See Part 1 §7 (full batch).

## 18. Backup & restore

- Musicolet: single ZIP with choose-what-to-restore, automatic backup with custom folder,
  share the ZIP, verification, restore warnings.
- ssilencce: ZIP backups of playlists, settings, lyrics cache and custom artist images.
- See Part 1 §15 (selective restore, automatic backup, verification).

## 19. Playlist import/export

- Musicolet: M3U/M3U8/PLS export and import with conflict handling (replace/keep/rename).
- ssilencce: `.m3u` export, device playlist import.
- See Part 1 §16 (M3U8/PLS, conflicts).

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
- ssilencce: 4 built-in dynamic shortcuts (last added, top tracks, favorites, shuffle all).
- See Part 1 §11 (per-entity shortcuts).

## 22. Search integration

- Musicolet: quick search box at the bottom of every list, search history, system search
  with `MEDIA_PLAY_FROM_SEARCH`.
- ssilencce: global search with voice input, debounce, grouped results, filters, scoped
  searches, `searchable.xml`.
- See Part 1 §5 (per-list quick search).

## Notes (Part 2)

- Where ssilencce clearly wins or matches (lyrics with syllable/karaoke mode and online
  providers, Material You theming, scrobbling, smart shuffle, crash handler, EQ import/export),
  the feature is excluded from this list or the advantage is called out inline.
- "Better" is a capability comparison from the two reference documents, not an opinion on
  implementation quality.
