# Feature Inventory

Complete inventory of user-facing features available in the ssilencce app (fork of Booming Music).

Related: `docs/CODE_MAP.md` (per-file code analysis).

## 1. Playback & Audio

- **Media3/ExoPlayer engine** — `PlaybackService` (MediaLibraryService) owns the player, MediaSession, equalizer, notifications, Bluetooth handling and widgets.
- **State persistence** — Queue, position and shuffle order are persisted in Room/SharedPreferences and restored on app start (`PersistentStorage`).
- **Playback modes** — Repeat, shuffle (normal), play on startup (never / last position / first song), clear queue on completion.
- **Smart shuffle modes** (`ShuffleManager`) — Pure random, most played, favorites, forgotten songs, recently added, combined, by-group shuffle; album/artist shuffle strategies; per-open shuffle behavior (on/off/remember).
- **Queue behavior** — Configurable tap-on-song action (play single song / play whole list / play next / queue at end) and queue-clearing behavior (remove all vs. keep current).
- **Controls** — Rewind with back button, configurable seek interval (5–60 s), mini-player swipe-to-skip, extra controls, adaptive/circle/animated buttons.
- **Audio processing** (`renderer` + `processor`) — Per-channel balance, ReplayGain (album/track mode, preamp, peak limiting on 16/24-bit PCM) injected into `DefaultAudioSink`; ALAC hardware-decoder workaround for broken high-bitrate M4A.
- **Software decoding fallback** — Jellyfin FFmpeg decoder (`media3-ffmpeg-decoder`) auto-loaded by the Media3 extension renderer for codecs MediaCodec cannot handle (e.g. ALAC, and rare codecs inside containers Media3 demuxes); Media3 1.10.1 provides no demuxers for ASF/WMA, APE, DSF/DFF or AIFF, so those containers are not playable.
- **Audio device integration** (`AudioOutputObserver`) — Resume on headphone/Bluetooth connect, pause on disconnect, pause at zero volume, system media output switcher (incl. MIUI/Samsung OEM intents), Xiaomi Mi Play detection, bit-perfect state.
- **Audio focus** — Optional ignore-audio-focus mode.
- **Advanced** — MP3 index seeking, stop when closed from recents, play option always visible / whole list, play all songs when searching.

## 2. Equalizer & Sound

- **Equalizer** with two engines: classic `android.media.audiofx.Equalizer` and `DynamicsProcessing` (API 28+, multiband compressor/limiter).
- **Effects** — Bass boost, virtualizer, loudness, compressor, limiter.
- **Balance, tempo (speed/pitch), system volume** controls.
- **EQ profiles** — Named profiles with per-output-device associations, import/export (JSON and AutoEq TXT format), persistence in DataStore.
- **ReplayGain** — Album/track/off modes with preamp, reading standard, iTunes `----:com.apple.iTunes:` and Opus R128 tags.
- **Sound settings sheet** — Balance, tempo, replay gain and volume controls reachable from the player.

## 3. Lyrics

- **Synced and plain lyrics** — LRC/enhanced-LRC and TTML (Apple Music) parsing with translations, transliterations and actor assignment (duets M/F).
- **Sources** — Embedded tags, lyrics files (`.lrc`/`.ttml`), embedded via LRC files, and online providers: **Lyrically** (iTunes-based, Jaro-Winkler scoring), **BetterLyrics**, **LRCLib** (close-duration search); cached in Room (`LyricsEntity`).
- **Syllable mode & karaoke** — Word-by-word highlighting (`SpannedWordText`), karaoke-style text fill, progressive coloring, blur/shadow text effects, animated backgrounds.
- **Lyrics display** — Full-screen lyrics view and lyrics over the cover, auto-scroll, active line centering, horizontal centering, RTL support, custom font import, line spacing, font sizes and bold per mode (synced/plain × player/fullscreen).
- **Lyrics editor** — Edit text with preview, instrumental detection, online search, save to lyrics/LRC files.
- **Instrumental detection** — By title keywords (configurable identifiers) or short exact lyrics.
- **Advanced** — Ignore blank lines, preferred file format (TTML/LRC), force UTF-8, resume-on-seek, line-crossfade settings.

## 4. Library & Organization

- **Media collections** — Songs, albums, artists (with album-artist support), genres, release years, folders (filesystem explorer with SAF), playlists.
- **Home screen** — Greeting header, last added, top tracks, history, shuffle button and suggestions.
- **Content types** — Favorites, history (configurable cutoff interval), last added (configurable interval), not-recently-played, top tracks, top albums/artists, never played.
- **Play counts** — Per-song play/skip counters and time played (incremented at 50 % of duration), top-100 flows.
- **Sorting** — Locale-aware collator, article-ignoring sort (The/A/El…), per-entity sort keys with persistence.
- **Views** — List/grid modes, 5 grid view types (normal, card, colored card, circle, image), configurable grid size, larger header image, fast scroller.
- **Library filtering** — Whitelist/blacklist folders (`InclExclDao`), minimum song duration, minimum songs per artist/album.
- **Covers** — Embedded/folder art (cover.jpg etc.), MediaStore art, ignore-MediaStore-covers option, use-folder-art option, online artist images and album covers (Deezer), preferred image size.
- **File operations** — Batch delete with MediaStore trash requests (recoverable) and SAF fallback, delete to trash option.

## 5. Playlists

- **CRUD** — Create, rename, edit (name, description, custom cover via Photo Picker), delete playlists; favorites playlist.
- **Playlist management** — Drag & drop reordering, internal search, song counts, multi-select.
- **Import/export** — Import device playlists (M3U and others), export as `.m3u` (MediaStore on Android 10+).
- **Add-to-playlist** — Compose bottom sheet with search and multi-select.

## 6. Search

- **Global search** — Text field, voice input, 300 ms debounce, grouped results (albums, artists, songs, playlists, genres).
- **Search filters** — Smart, basic and last-added filters; scoped searches within genre, playlist, year and folder.
- **System integration** — Android searchable config, search action, hold-tab-to-search.

## 7. Now Playing Screen

- **7 player styles** — Default, FullCover, Gradient, Plain, Material 3, Expressive, Peek — each with dedicated controls fragment.
- **Gestures** — Configurable single-tap, double-tap (left/right/center), long-press on cover; swipe on cover, swipe anywhere, swipe-up for queue, swipe-down to dismiss.
- **Page transitions** — Carousel effect plus 10 PageTransformers (depth, 3D flips, hinge, cascading, parallax, vertical stack, zoom-out…).
- **Color schemes** — 5 modes (App theme, simple color, vibrant color, Material You, blur) with palette extraction from the cover.
- **Display options** — Small image, corner radius, player blur intensity, squiggly seek bar, scrolling text, remaining-time preference, album-artist name, album title, next song preview, extra metadata fields (bitrate, genre, year…) with draggable configuration.
- **Buttons** — Circle play button, adaptive controls, animated controls, extra controls, button style (Normal/M3/Expressive).
- **Queue screen** — Compose UI with highlighted current song, search, drag reordering and filters.

## 8. Network & Integrations

- **Scrobbling** — Last.fm/Libre.fm (username+password, MD5-signed session) and ListenBrainz (token): scrobble and now-playing toggles, per-service login state.
- **Last.fm** — Artist/album info, biography/wiki display, error-code mapping.
- **Deezer** — Track/album/artist search used for covers and artist images (best-image selection).
- **MusicBrainz** — Recording search/details used by the tag editor; Cover Art Archive URLs.
- **Built-in updater** — GitHub releases (stable/prerelease), weekly/daily/manual search modes, experimental updates toggle, markdown changelog dialog, ABI/flavor-aware APK download.
- **Network features** — Master switch, Wi-Fi-only mode, online artist images/album covers, preferred image size, 10 MB HTTP cache with offline interceptor.

## 9. UI, Themes & System

- **Themes** — Light/dark/system toggle, Material You (Monet dynamic color with palette styles and system contrast), pure black theme, custom font (Google Sans Flex).
- **App language** — ~40 locale translations, per-app language override.
- **Library UI** — Configurable visible categories (max 5, draggable), tab titles mode, remember last page, appbar mode (compact/simple/collapsing), rotation lock.
- **Widgets** — 3 Glance widgets (responsive, card, full) with cover, title, artist, progress and controls; dynamic colors, widget layout style, third-line content, corner radius.
- **Android Auto** — Hierarchical media browsing via media IDs and Automotive app descriptor.
- **App shortcuts** — 4 dynamic shortcuts: last added, top tracks, favorites, shuffle all.
- **System search** — `searchable.xml` integration.
- **Crash handler** — Error activity saving `.log` reports with copy/share/report/restart actions.
- **Backup/restore** — ZIP backups of playlists (M3U), settings, lyrics cache and custom artist images.

## 10. Metadata & Info

- **Tag editors** — Song, album and artist editors using TagLib/Jaudiotagger: full field editing, covers via Photo Picker, lyrics, MusicBrainz search dialog, batch save with MediaStoreWriter, album-artist/conductor fields.
- **Song info** — Bitrate, format, sample rate, channel mode, metadata completeness check with refresh.
- **Play statistics** — Plays/skips counts, last played date, top tracks per album/artist.
- **Web search** — Song web search dialog (Google, Last.fm, Wikipedia, YouTube) with localized URLs.

## 11. Utilities

- **Sleep timer** — AlarmManager-based, countdown, fade-out, quit-after-end option.
- **Sharing** — Share songs/albums via intents.
- **Storage** — Storage volume enumeration, SAF persisted permissions, folder chooser dialog.
- **OEM helpers** — Xiaomi Mi Play detection, MIUI/Samsung media output switcher.
- **Permission onboarding** — `PermissionsActivity` with guided permission requests (storage/audio, images, Bluetooth, exact alarms).

## Notes

- Flavor differences: the updater and Lyrically provider exist only in the `github` flavor; `fdroid` has no updater; `playstore` uses a stub for the Lyrically API.
- Feature availability checks (`NetworkFeature`) gate images, lyrics APIs, scrobbling and the updater per build flavor.
