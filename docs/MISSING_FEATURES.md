# Musicolet Features Missing from ssilencce

Complete list of user-facing features present in Musicolet (see `docs/MUSICOLET_FEATURES.md`)
but absent from ssilencce (see `docs/FEATURES.md`). Items marked **(partial)** exist in
ssilencce in a limited or different form; the gap is described. Items in the appendix are
not applicable to an open-source app.

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
- **Own native decoder** with extra formats (WMA, APE, DSF, DFF, AIFF), system-decoder switch and multi-threaded decode (ssilencce uses Media3/ExoPlayer only).

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

## Notes

- Source of gaps: `docs/FEATURES.md` (ssilencce, v-current) vs `docs/MUSICOLET_FEATURES.md` (Musicolet 6.14 build 530).
- "PRO" markers indicate features that are paid in Musicolet; they still count as gaps for ssilencce.
- ssilencce-only features (Media3 engine, smart shuffle modes, lyrics syllable/karaoke mode, AutoEq import, online lyrics providers, sync'd lyrics via TTML, Android Auto with media IDs, crash handler with reports, etc.) are intentionally not listed here.
