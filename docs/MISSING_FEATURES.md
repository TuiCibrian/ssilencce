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

- **Previous-button behavior** — implemented as restart-after-5-seconds / always-previous and
  applied to UI, notification and headset; the dedicated Next/Previous-override-to-FF/Rewind
  mapping remains missing.
- **Session survival beyond force-stop** — ssilencce now opts into restoring an active session after a system service/process restart; Android force-stop and an OS kill that never restarts the service remain outside app control.
- **Extra format playback (APE, WMA)** — AIFF/AIFC, DSF and DSDIFF (DFF) now play through custom Media3 extractors, with an opt-in supplementary indexer for files MediaStore misses and container/codec/decoder/bit-depth rows in song info. APE and WMA still do not play: the packaged Jellyfin FFmpeg 1.9.0+1 build enables only flac/alac/pcm_mulaw/pcm_alaw/mp3/aac/ac3/eac3/dca/mlp/truehd (verified: no `ff_ape_decoder`/`ff_wmav1_decoder`/`ff_wmav2_decoder`/`ff_wmapro_decoder` symbols), and Media3's JNI bridge does not pass the `bit_rate`/`block_align`/`bits_per_coded_sample` those decoders need. `.ape` is therefore not indexed either. Still missing vs Musicolet: system-decoder switch and multi-threaded decode.

## 2. Queues

No remaining feature gaps; see Part 2 §13 for the queue-screen comparison.

## 3. Shuffle & repeat

No remaining feature gaps.

## 4. Equalizer & sound

- **Surround sound** effect — the existing Android `Virtualizer` effect already provides stereo widening; no separate surround switch is needed.
- **Channel-balance warning** — headroom handling is implemented; the balance warning is not.

## 5. Library & browsing

- **Sub-categorization** - albums by artist/album-artist, artists by albums, and genre album-artist/composer subcategories; album composer filtering and genre Songs/Albums/Artists subcategories are available.
- **Album-merge strategy** — user-configurable fields that decide album equality.
- **Multi-artist / multi-album-artist / multi-composer / multi-genre tags** with custom separators (PRO).
- **Third-line list info** — date modified and other extra fields only append to the existing secondary text; compact album/artist song rows and Home song suggestions have no secondary text view, and a configurable multi-field list picker is not available.

## 6. Scanning & folder management

- **Welcome/setup wizard** — ssilencce now has a first-run wizard with folder include/exclude, a main music folder and rescan; SAF per-volume access and in-wizard tutorials remain missing.
- **Scan options** — scanning video files as audio remains missing. Hidden-folder and `.nomedia` options are not offered because the MediaStore-based scanner cannot override Android's media index (documented in the wizard).
- **Rescan controls** — force rescan and auto rescan on media mount are implemented; scan on app start, scanner choice (Musicolet vs System) and auto rescan on unmount remain missing.

## 7. Tags, lyrics & album art

- **Multi-song (batch) tag editing** across all fields and formats (ssilencce has a multi-select text-field and cover batch editor with Keep/Set/Clear, per-file values, per-field read-back, in-place backup/restore and session undo; the album/artist single-entity actions route to the same editor; the full runtime format matrix across ID3v1/v2, MP4, FLAC/Vorbis, OGG/Opus, WAV, AIFF, WMA, DFF and DSF remains unproven).
- **Hide website info from tags** (current song / everywhere).

## 8. File operations

- **Mini Player popup** when opening files from other apps via VIEW intents.

## 9. Now Playing screen

- **Seekbar helper buttons** — cancel seeking.

## 10. Quick access

No remaining feature gaps.

## 11. Notifications

- Android 13 and later may ignore custom notification layouts and show the system media
  controls instead; the custom styles apply only where the platform honors custom media
  RemoteViews.

## 12. Cast & Screencast (PRO)

- **Chromecast** — implemented in the `github`/`playstore` flavors (see `docs/FEATURES.md` §8); `fdroid` stays GMS-free and has no Cast support. Still missing vs Musicolet: the **decode-on-device** option (streaming the processed PCM/WAV signal) and the companion "cast plugin" app.
- **Screencast** — implemented for external presentation displays (HDMI, Miracast/wireless display, Chromecast-as-display routes exposed by the OS): the Now Playing screen is shown with `android.app.Presentation` without mirroring, and audio stays in the local playback pipeline so EQ, ReplayGain and crossfade keep working. Remaining gap vs Musicolet: EQ profiles keyed to a screencast display as such; the equalizer follows the actual audio output route (Cast/remote MediaRouter associations are supported).

## 13. Headset, Bluetooth & USB

- **Dedicated Next/Previous override to FF/Rewind** and per-device filtering remain missing; configurable single/double/triple/long-press mapping, optional 4+ Seek forward, 250–600 ms delay and the OEM/AVRCP disclaimer are implemented.

## 14. Backup & restore

- **Share the backup ZIP directly from the app** - no remaining gap for manual backups: a successful manual backup opens the "Backup saved" sheet and can share the archive. Scheduled archives remain in the user-chosen SAF folder without auto-share; that is intended behavior, not a missing feature.

## 15. Export / import

- Device-playlist import conflict handling (Keep both, Replace existing, Rename imported) is implemented; no remaining gap. Backup playlist restore intentionally keeps its separate non-interactive merge-into-existing policy instead of showing this dialog.

## 16. Interface & theming

- **Main menu customization** — configurable entries for the primary navigation/drawer (quick access and the song/album/artist/genre/playlist/folder option lists are implemented).

## 17. OEM support & battery guides

- **Background-playback opt-in** (Android 11+), unrestricted battery access (Android 15+), WRITE_SETTINGS for status bar.

## Appendix A — Partial overlaps (Musicolet feature exists in reduced form)

- **Advance Shuffle** — album/artist plus album-artist/composer most-played strategies are available; composer strategies require Android 11/API 30+ (matching the Composers category), while album-artist strategies work on all supported versions. Per-entity within-group sorting rules are fixed rather than user-configurable.
- **ReplayGain** — both can calculate tags and both expose use-embedded-tags, clipping/headroom and constant-volume controls. ssilencce source implements standard/WAV-ID3v2 (explicit RIFF `ID3 ` chunk)/iTunes/Opus tag calculation and writes album gain only for complete albums; the current AA-06 checkpoint is not yet independently accepted, and final review, frozen build and device WAV/M4A gates remain pending.
- **Tag editor** — Musicolet batch-edits any field across formats; ssilencce has a multi-select text-field and cover batch editor with Keep/Set/Clear, per-file/per-field read-back, in-place backup/restore, session undo and single-entity routing; the full runtime format matrix remains unproven.
- **Album art change/remove** — both support it; both also offer a fullscreen zoom viewer, save to gallery and wallpaper targets (ssilencce: home, lock screen or both).
- **Backup** - both ZIP-based and both restore selectively (ssilencce's picker preselects nothing); automatic scheduled backup, structural verification, direct sharing of a manual backup from the post-backup sheet and selective restore of favorites, play counts, named queues, per-file resume positions, song notes and per-song speed/pitch are now implemented (ADR-043, ADR-048). No remaining backup/restore gap.
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
- ssilencce: countdown, exact HH:MM end time, stop after N songs, stop after this song,
  fade-out, finish-last-song, timer history with 1-tap reuse/clear, "stop only music if UI
  is foreground". No remaining behavioral gap; a gradual fade whose length the user chooses
  is available for time modes (song modes stop at the track boundary).

## 2. Audio focus handling

- Musicolet: 4 levels — normal, ignore focus, ask before taking focus, resume after call.
- ssilencce: all four modes are wired through one controller (ADR-042, ADR-045), with a
  restorable request-bound Ask dialog; repeated play preserves consent, explicit/no-op pause
  and confirm/cancel/mode/release invalidate it. Strict autoplay policy for buggy car/device
  stacks is now implemented as an optional Bluetooth/car guard (standard or strict).

## 3. Pause/resume on BT & USB

- Musicolet: "only resume if paused by disconnect", "prevent unwanted autoplay" with strict
  mode for buggy devices/cars, resume on wired/BT connect.
- ssilencce: resume on connect, pause on disconnect, resume only if paused by disconnect,
  pause at zero volume with resume at any non-zero step, don't start playback when muted, and
  an unwanted-autoplay guard (5-second connect window or strict background blocking).

## 4. FF/Rewind & track navigation

- Musicolet: configurable FF/Rewind durations, next/prev button override to FF/Rewind for
  headsets/remotes, FF/Rewind visible only for long audio.
- ssilencce: independent fast-forward/rewind durations, long-audio-only gating with
  next/previous fallback for headset seek commands, previous-button restart/always-previous
  behavior, and configurable headset single/double/triple/long-press mapping with optional
  4+ Seek forward; the remaining gap is dedicated Next/Previous override to FF/Rewind.

## 5. Playback speed & pitch

- Musicolet: up to 4 decimal places, per-song play speed, simple or advanced mode.
- ssilencce: global tempo (speed/pitch) plus an optional per-song override persisted in Room
  and selectable from the sound settings sheet; the remaining gap is Musicolet's 4-decimal
  precision and simple/advanced editor modes.

## 6. Session survival

- Musicolet: `TaskSaviour` service restores playback after the app process is killed.
- ssilencce: opt-in recovery restores an active queue/position after a system service restart
  (ADR-042); Android force-stop and OS kills that never restart the service cannot be bypassed.

## 7. Equalizer

- Musicolet: own engine or System equalizer, configurable band count (3/5/10/15/31),
  preamp, per-output-device profiles, per-Bluetooth-device EQ, EQ presets save/delete.
- ssilencce: three engine choices (classic Equalizer with device-fixed bands, DynamicsProcessing with 3/5/10/15/31 ISO bands and curve resampling, System/OEM panel hand-off), bass boost, virtualizer, loudness, compressor, limiter, profiles with broad output-type and exact hashed per-device associations, and JSON/AutoEq import-export (Musicolet lacks import/export).
- See Part 1 §4 (per-BT device).

## 8. ReplayGain

- Musicolet: read *and calculate* RG tags, album/track normalization, RG preamp + non-RG
  preamp, use-embedded-tags option, skip-or-reload, "convert RG to constant volume",
  headroom handling.
- ssilencce: reads and calculates standard, iTunes and Opus R128 tags; album/track modes,
  with-tag and non-RG preamps, peak limiting on 16/24-bit PCM. Calculation writes verified tags
  and writes album gain only when the complete album is selected (ADR-049). ssilencce now also
  exposes use-embedded-tags, prevent-clipping, headroom and constant-volume controls.
- Remaining: skip-or-reload; the use-embedded-tags, clipping/headroom and constant-volume controls now exist.

## 9. Shuffle & repeat modes

- Musicolet: on/off/randomize, shuffle behaviors (per-queue, below-current-song), shuffle
  queue visible & reorderable, "shuffle and then…", "shuffle and play", repeat
  load-next-and-pause.
- ssilencce: matches on/off/randomize, per-queue and below-current shuffle behaviors, a visible
  and reorderable shuffled queue, multi-select shuffle actions and repeat load-next-and-pause;
  the remaining difference is the broader smart-shuffle strategy set ssilencce already has.

## 10. Sorting

- Musicolet: 14+ keys (title, filename, track number, album, artist, duration, year, date
  added, date last played, date modified, most/least played, path), ignore-keyword whitelist,
  experimental alphanumeric sorting.
- ssilencce: locale-aware numeric collator (covers natural/alphanumeric ordering), article-ignoring (The/A/El…) with a user-editable keyword list, per-entity sort keys including filename, path, track number, date modified, date last played and most/least played; only Musicolet-specific/experimental sort options remain missing.

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
- ssilencce: hierarchical browsing via media IDs and automotive descriptor, including
  folders (flat or hierarchical), album artists, composers (API 30+), playlists and the
  current playback queue; Auto buttons cover FF/Rewind, favorite, shuffle and repeat.
- Remaining gap: no exported album-art ContentProvider for external clients.

## 15. Media notifications

- Musicolet: small / big simple / big advance styles, configurable compact/expanded button
  arrangement, upcoming songs, favorite button, replace-close-with-add-to-playlist,
  always-expanded, broadcast art to system lock screen.
- ssilencce: custom Media3 notification provider with configurable compact/expanded buttons
  (previous, next, play/pause, favorite, FF, RW, close, shuffle, repeat), a favorite toggle,
  Close/Add-to-playlist replacement and an optional lock-screen artwork broadcast; opt-in
  Small / Big simple / Big with upcoming songs custom RemoteViews styles add a seek bar,
  always-expanded and theme / cover-palette / custom notification backgrounds. Android 13+
  may use system media controls instead of the custom layouts. Custom notification sizes/styles,
  notification background colors and upcoming songs remain missing from the system style; the
  system media style cannot render an upcoming list.

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
- ssilencce: multi-select text-field and cover batch editor (17 fields, Keep/Set/Clear, common/Varies
  aggregation, per-file values and per-file/per-field read-back), full single-song editing,
  album/artist single-entity actions routed to the same batch editor,
  covers via Photo Picker, MusicBrainz search (Musicolet lacks MusicBrainz integration).
- Remaining: the runtime format matrix across every container/codec combination (the editor, batch
  cover replace/delete, session undo/rollback and in-place write backup/restore are implemented at
  source; no device PASS is claimed).
- See Part 1 §7 (full batch).

## 18. Backup & restore

- Musicolet: single ZIP with choose-what-to-restore, automatic backup with custom folder,
  share the ZIP, verification, restore warnings.
- ssilencce: selective ZIP backup/restore with structural verification and warnings
  (empty/non-backup/unrestorable), including settings, playlists, favorites, play counts, named
  queues, per-file resume positions, per-song speed/pitch, lyrics, song notes and artist images;
  opt-in scheduled backups to a SAF folder and direct sharing of a manual backup from the
  post-backup sheet are also implemented (ADR-043, ADR-048). Scheduled archives remain in the
  user-chosen folder without auto-share; this is intended behavior, not a gap.

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
  (Musicolet lacks online covers), ignore-MediaStore option, fullscreen zoom viewer, save to gallery
  and home/lock/both wallpaper targets.
- Album-art loading can be disabled in Library > Advanced; no remaining Part 1 §7 gap.

## 21. App shortcuts

- Musicolet: play/search/shuffle plus launcher shortcuts for specific albums, artists,
  folders and playlists, ask-before-adding option.
- ssilencce: four built-in dynamic shortcuts plus per-entity album/artist/folder/playlist
  shortcuts with confirmation (ADR-044); the MIUI-specific permission guide is implemented in the
  battery & background guide.

## 22. Search integration

- Musicolet: quick search box at the bottom of every list, search history, system search
  with `MEDIA_PLAY_FROM_SEARCH`.
- ssilencce: inline quick search on all seven top-level lists, the queue, the generic
  detail/statistics list and the song sections of album, artist, genre, year, folder and playlist
  detail (playlist filtering is drag-safe); settings search; global search with voice input,
  debounce, grouped results, filters, scoped searches, remembered submitted queries (last 20,
  removable chips) and `searchable.xml`.

## Notes (Part 2)

- Where ssilencce clearly wins or matches (lyrics with syllable/karaoke mode and online
  providers, Material You theming, scrobbling, smart shuffle, crash handler, EQ import/export),
  the feature is excluded from this list or the advantage is called out inline.
- "Better" is a capability comparison from the two reference documents, not an opinion on
  implementation quality.
