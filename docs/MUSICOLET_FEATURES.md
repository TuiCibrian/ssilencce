# Musicolet — Complete Feature Catalog

Reference document for the Musicolet app (package `in.krosbits.musicolet`) based on a full
decompilation of the Play Store APK bundle.

- **Analyzed version:** 6.14 (build 530) — `versionCode 53000`, `versionName "6.14 build530"`
- **SDK:** `minSdk 28` (Android 9), `targetSdk 36`, `compileSdk 36` (Android 16)
- **Analysis method:** jadx 1.5.5 (GUI plugin + headless CLI dump of the base APK); cross-checked
  against `res/values/strings.xml` (1,299 strings), `res/xml/default_preference.xml`,
  `AndroidManifest.xml`, `assets/change_log.txt` (v0.5.3 → v6.14) and code searches over the
  obfuscated sources.

## App identity & internals

- Package `in.krosbits.musicolet`; app process `:musicolet`; `installLocation="auto"`;
  `requestLegacyExternalStorage` + `preserveLegacyExternalStorage`; `allowBackup=false`.
- **Obfuscated** with R8 (`defpackage.*`, 3,673 classes of ~4,800 total) and protected with
  **PairIP** (native `libpairipcore.so`, `com.pairip.*` classes, encrypted assets, signature
  verification, `Application` subclass `com.pairip.application.Application`).
- **Own native audio engine:** `lib_musicolet.so` + Java `AudioTrack` pipeline (Musicolet
  decoder) with a fallback to the **system decoder**.
- Rooms DB (`androidx.room`), WorkManager, Play Billing 8.3.0, Cast SDK (GMS), ExoPlayer (used
  for Cast/Screencast only), Jaudiotagger (tag I/O), OkHttp, Material 3 / AppCompat legacy views.
- Legacy `original-package com.android.music` (inherited from the ancient Android Music app).

## Playback

- **Local playback** via Android `MediaPlayer` + AudioFocus; foreground service
  `mediaPlayback`; legacy `MediaSession` (compat) for system/third-party controls.
- **Musicolet decoder** (native): high sample-rate decoding, extra formats (WMA, ALAC, APE,
  DSF, DFF, AIFF…); option to switch to System decoder; decoder info; multi-threaded decode
  (developer option).
- **Gapless playback** (experimental toggle, default on).
- **Crossfade** (PRO).
- **Fade-in on play** / **fade-out on pause** (0–4000 ms each, configurable).
- **A-B repeat**; per-track **remember last position**; play/pause fade effects toggle.
- **Play speed + pitch** control (up to 4 decimal places), **per-song play speed**; simple or
  advanced mode; "FF/Rewind only for long audio (audiobook/podcast)" option.
- **Sleep timer**: HH:MM, stop after N songs, stop after this song, gradually fade out the last
  song, timer history (1-tap reuse), reset; "stop only music if UI is foreground".
- **Mute handling**: don't play when muted, pause when muted, resume when unmuted.
- **Respect audio focus** with 4 levels (incl. ignore focus, ask, resume after call).
- **Previous-button behavior** (restart / play previous), next/prev override to FF/Rewind
  (headsets, remotes), FF/Rewind durations.
- "Load next song and pause", "Pause after this song", "Play after current song".
- Session survival: `TaskSaviour` service (`stopWithTask=false`) restores playback after the
  app process is killed; "start playback when app starts".
- **Preview** a song without adding it to any queue.
- **Pause/resume on BT & USB disconnect/connect** (incl. "only resume if paused by disconnect",
  resume on wired/BT connect, "prevent unwanted autoplay" + strict mode).

## Queues

- **Multiple numbered queues** (3+), active-queue concept, per-queue rename/remove/clear/save
  (also "save as playlist"), duplicate-name handling (**replace / merge / keep both**), queue
  counter showing current position + total, scroll-to-current, max queue limit with auto-delete
  of oldest.
- End-of-queue behaviors: **resume next/previous queue from its last position**, **re-shuffle
  the queue**, jump to next/first queue.
- Queue tab: album-art show/hide, swipe-right-to-remove (toggle), drag-reorder (incl. dragging
  multiple selected songs), quick search box, confirm before removing queues.
- Playback history per song + **clear history** for one/many songs.

## Shuffle & repeat

- Shuffle modes: off / on / **randomize**; behaviors ("shuffle all songs of each queue",
  "shuffle all songs of that queue", "only shuffle below the current song"), shuffle queue
  visible & reorderable, "shuffle and then…" in multi-select, "shuffle and play".
- **Advance Shuffle**: shuffles albums/artists/album-artists/composers/genres/folders while
  keeping songs inside them in order (sorting rules per entity).
- Repeat: off / repeat queue / repeat song / load-next-and-pause.

## Audio effects & equalizer

- **Musicolet equalizer** (own engine) or System equalizer; **3 / 5 / 10 / 15 / 31 bands**
  (band count configurable, requires Musicolet decoder); **preamp**; **per-output-device EQ
  profiles** (speaker / headset / bluetooth) and **per-Bluetooth-device EQ**; **per-screencast
  device EQ** (PRO); EQ presets with save/delete.
- **Bass boost**, **Loudness enhancer** (volume boost), **audio channel balance**,
  **stereo/mono output**, **surround sound**, channel-balance warning, **headroom** handling.
- **Reverb effects** (experimental): room level, room HF level, decay time, decay HF ratio,
  reflection level/delay, reverb level/delay, diffusion.
- **ReplayGain** (PRO): normalization modes (album/track gain), **read & calculate RG tags**,
  RG preamp, non-RG preamp, use embedded RG tags, skip-or-reload RG, "convert RG to constant
  volume".

## Library & browsing

- Tabs (arrange/hide, position top/bottom, back-button tab history): Albums, Artists,
  Album-Artists, Composers, Genres, Folders, Playlists, Queues, Most Played, Recently Added,
  Recently Played, **Not Played**, Search.
- Album tab: view modes (grid/list, thumbnails), grid size, sort, **sub-categorize by artist /
  album-artist / composer**, shuffle albums/artists.
- Artist tab: choose Artists / Album-Artists / Composers, sort, sub-categorize by albums, sort
  albums by year.
- Genre tab: sort/search, sub-categorize by albums/artists/album-artists/composers.
- **Sorting**: title, filename, track number, album, artist, duration, year, date added, date
  last played, date modified, most/least played, path — asc/desc; **ignore keywords (A/An/The…)**
  with whitelist; **alphanumeric sorting** (experimental, per-tag).
- **Album-merge strategy** (which fields decide album equality), **disc number** support,
  multi-artist / multi-album-artist / multi-composer / multi-genre tags with custom separators
  (PRO), "use album-artist if artist missing".
- **Quick search box at the bottom of every list** (songs, queues, folders…), global search
  with history and system search integration (`MEDIA_PLAY_FROM_SEARCH`).
- Extra info per list (song count, total duration, date updated…); filename-vs-title display.
- **Favorites** (per-song, multi-select add/remove), **Most Played** statistics
  (this week / month / 12 months / year / all time; artist stats with play percentage; manual
  **CSV export**; **auto CSV export** weekly/monthly/yearly — PRO), "Not played in past N days".

## Scanning & folder management

- Welcome/setup wizard: pick storage (SAF, per-volume access), choose folders to scan
  (all folders or specified folders), **main music folder**, tutorials for storage
  permission / SAF steps.
- **Included / excluded folders**, **exclude by duration**, exclude directly from the Folders
  tab; hidden-folder & `.nomedia` scanning options; **scan video files as audio**.
- **Force rescan**, scan on app start, scanner choice (Musicolet scanner / System scanner),
  auto rescan on media mount/unmount (`MediaMountedReceiver`), library DB updater service.
- Folder tab: **linear or hierarchical structure**, folder info (creation time, songs,
  sub-folders), rename folders, exclude/remove.
- **Ghost mode**: hidden folders whose songs stay out of the library, searchable via
  GhostSearch (also reachable on the lock screen).
- Remove missing songs / hidden songs from the library.

## Tags, lyrics & album art

- **Tag editor** (Jaudiotagger): ID3v1/v2, MP4/M4A, FLAC/Vorbis, OGG/Opus, WAV, AIFF,
  ASF/WMA, DFF, DSF; title/artist/album/album-artist/composer/genre/year/track/disc/lyrics/
  comment/rating; **multi-song (batch) editing**, artwork replace/delete (batch too).
- **Lyrics**: embedded + `.lrc`; per-song source selection (embedded or any LRC file);
  create/edit **synchronized LRC** in-app; A-B repeat on selected lyric lines; select/copy/
  share lyric lines; smooth scrolling; appearance styles; prefer-embedded-or-LRC setting.
- **Bookmarks/Notes** per song: stored as **shareable text files**, importable/editable by
  other apps; quick bookmark button in Now Playing.
- **Hide website info from tags** (current song / everywhere).
- **Album art**: change/remove (per song or batch), `folder.jpg`/`cover.jpg`/`<album>.jpg`
  fallback, fullscreen zoom viewer + **save to gallery**, set as **lock-screen wallpaper**
  (own lock screen), disable album-art loading everywhere.

## File operations

- **Move / copy** songs between folders, **rename folders**, create folders, delete files
  (with write-permission checks per volume), share songs/files, "share screenshot" of Now
  Playing (with Instagram-fit crop option).
- **Audio cutter / trimmer**: trim start/end or extract a portion as a new file; **set as
  ringtone** (uses the cutter).
- Play files from other apps (file managers) via VIEW intents → **Mini Player popup**;
  supported external mime types include audio/video, FLAC, APE, WV, DFF/DSF, Opus, M3U.

## Now Playing screen

- Blurred background (level 0–3), album-art-as-background; gestures (swipe up/down song
  change); show/hide artist/album name, icon, center-aligned info, scrolling text; FF/Rewind
  buttons visibility (always / only long audio); seekbar helper buttons (**seek-to precise
  position**, cancel seeking, touch-only-on-thumb); keep screen on; album-art tap action;
  custom **quick access** shortcuts; lyrics screen with sync + bookmarks.

## Musicolet Lock Screen & screen saver

- Custom full-screen player shown over the system lock screen (`showWhenLocked`): playback
  controls, queue and lyrics views, swipe-up to dismiss, landscape support, gestures,
  keep screen on, custom wallpaper, launch shortcuts (artist/folder/genre/playlist), skip when
  paused >1 min or device unlocked (configurable), OEM lock-screen permission guides (MIUI).

## Widgets & quick access

- **3 home-screen widgets**: 4×1, 4×1 Advanced, 4×3 (queue list); horizontally resizable,
  transparency, **themes** ("match system", "adapt from album art", "adapt from wallpaper");
  per-widget configuration; empty-state layouts.
- **Quick Settings tile** (`QsService`) with active state.
- **App shortcuts** (Android 7.1+): play, search, shuffle; "add album/artist/folder/playlist
  shortcut to launcher" (with MIUI guide); ask-before-adding option.

## Notifications

- Media notification styles: small / big simple / **big advance**; compact + expanded layouts
  with configurable button arrangement; album art (with size presets), background color,
  colorize container; **upcoming songs** in expanded notification; replace close button with
  "add to playlists"; favorite button; notification channels; expandable (pre-Android 11);
  always-expanded option; broadcast album art to system lock screen (toggle).

## Android Auto

- `MediaBrowserServiceImpl` (exported) + `AAProvider` (exported ContentProvider with album
  art) + `automotive_app_desc.xml`; browse folders, album-artists, composers, playlists;
  controls: FF/Rewind, favorite, current queue, shuffle; car notification small icon.

## Cast & Screencast (PRO)

- **Chromecast**: Cast SDK (CastOptionsProvider, media notifications, ReconnectionService),
  decode-on-device option (chromecast decoder / TV's EQ / dynamic codecs), companion
  "cast plugin" app detection, cast disable toggle.
- **Screencast** (v6.11+): cast without mirroring (device shows now-playing info), works with
  **Chromecast and Miracast / Wireless Display** (Smart TVs, speakers, PCs); EQ, ReplayGain
  and crossfade keep working during screencast; separate EQ per screencast device; hide
  mirroring option; `MediaRouter2`-based routing.

## Headset, Bluetooth & USB

- Earphone button actions: **double/triple press configurable**, 4+ presses = fast-forward,
  allowed-delay tuning, disclaimer.
- Auto pause on BT/USB disconnect; resume on BT/wired connect; prevent unwanted autoplay on
  buggy devices/cars (strict option); audio focus level 4 (ignore) for calls.

## Backup & restore

- **Backup as a single ZIP** (settings, playlists, favorites, play counts/most-played,
  queues, per-file settings), choose what to restore, restore from any backup/device,
  automatic backup with custom folder, share the ZIP, verification, restore prompt/warnings
  (no song files included).

## Export / import

- **M3U/M3U8/PLS export** (playlists; folder/filename chooser) and **import** of external
  playlists (M3U, M3U8, PLS), conflict handling (replace/keep/rename).
- **CSV export** of most-played songs/artists (manual; automatic weekly/monthly/yearly — PRO).

## Themes & interface

- Themes: light / dark / black (AMOLED); **day-night auto**, follow system theme; accent
  color; light status bar; language selector (40+ locales); tabs position top/bottom;
  arrange/hide tabs; settings menu with **search**; per-entity **option/menu customization**
  (main menu, quick access, song/folder/album/artist/genre/playlist option lists); "show/hide
  unimportant settings" (SmartPrefCategory).

## OEM support & battery guides

- Guides for battery optimization / app-killing per OEM (MIUI, EMUI/Huawei, Samsung, OPPO,
  Realme, Vivo, ASUS), "pin in recents", auto-start, disable optimization ("Fix CC optimize"
  activity), background-playback opt-in (Android 11+), unrestricted battery access (Android
  15+), WRITE_SETTINGS for status bar, SCHEDULE_EXACT_ALARM for the sleep timer.

## Support, purchases & about

- **PRO (IAP)**: one-time purchase and monthly/yearly subscription (Billing 8.3.0, manage
  subscription), PRO features: crossfade, Chromecast, screencast, multi-artist/multi-genre
  tag splitting, Most-Played long periods, ReplayGain normalization, automatic CSV export.
- FAQ/help with answers, about screen (credits, licenses, translators, privacy policy),
  feedback/rate/support links (Facebook, X/Twitter, Instagram, Reddit, Telegram, YouTube,
  WhatsApp), update checker, "our other apps".

## Key Android manifests facts

- **Permissions:** READ/WRITE_EXTERNAL_STORAGE (≤ API 32), READ_MEDIA_AUDIO/IMAGES/VIDEO
  (API 33+), POST_NOTIFICATIONS, MODIFY_AUDIO_SETTINGS, BLUETOOTH/BLUETOOTH_CONNECT,
  FOREGROUND_SERVICE + FOREGROUND_SERVICE_MEDIA_PLAYBACK, SCHEDULE_EXACT_ALARM,
  WRITE_SETTINGS, WAKE_LOCK, INSTALL_SHORTCUT, USE_FULL_SCREEN_INTENT, EXPAND_STATUS_BAR,
  BILLING, custom `ALLOW_CAST_AUDIO`.
- **Queries:** `in.krosbits.castplugin`, Chromecast app, DocumentsUI, SAF actions, billing
  services.
- **Multi-process:** main `:musicolet`, `:rs` (restart activity), lock screen task affinity,
  mini player task affinity.

## Analysis notes

- Feature list verified against decompiled code, resources, preferences and the official
  changelog (v0.5.3 → v6.14). A few entries (marked PRO, or described in the changelog only)
  rely on the changelog + code evidence combined; nothing in this document is speculative.
- Source of truth: Play Store APK bundle (`in.krosbits.musicolet.apk` + config splits),
  decompiled with jadx 1.5.5 (headless dump under a local temp dir, not part of this repo).
