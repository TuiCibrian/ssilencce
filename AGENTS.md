# ssilencce Agent Guidelines

## Project Information
- **Project Name:** ssilencce
- **Base Project:** Fork of Booming Music (upstream: `https://github.com/mardous/BoomingMusic`)
- **Application ID:** `com.ssilencce` (changed from upstream; see Fork Notes)
- **Build Command:** `./gradlew :app:assembleGithubDebug`

## App Description
ssilencce is a modern, feature-rich music player for Android, designed to provide a high-quality audio experience with advanced playback features and a customizable interface.

## Tech Stack
- **Language:** Kotlin
- **UI Framework:** Jetpack Compose (Material 3) with legacy View support
- **Media Playback:** Media3 (ExoPlayer)
- **Dependency Injection:** Koin
- **Local Database:** Room
- **Networking:** Ktor
- **Image Loading:** Coil
- **Navigation:** Jetpack Navigation
- **Concurrency:** Kotlin Coroutines & Flow
- **Data Serialization:** Kotlinx Serialization
- **Metadata Handling:** TagLib & Jaudiotagger

## Build & Verification

- **Build:** `./gradlew :app:assembleGithubDebug` — requires **JDK 21** (`jvmToolchain(21)`); single-module project (`:app`); AGP 9.3.1, Kotlin 2.3.21, compileSdk 37 / minSdk 26 / targetSdk 36.
- **Lint:** `./gradlew :app:lintGithubDebug` — `lint { abortOnError = true }`, lint errors fail the build.
- **Code quality hooks:** `.opencode/plugin/check.ts` auto-formats each edited `*.kt`/`*.kts` (via `scripts/ktlint.ps1`, project ktlint 1.8.0) and runs `:app:lintGithubDebug` in the background. Run `/codecheck` after each change for the full skill-based review pass (security, performance, accessibility, code-review) plus the lint gate.
- **Tests:** No test source sets are configured; do not add test configurations (rule 10).
- **Signing:** Release reads `keystore.properties` (keyAlias/keyPassword/storePassword/storeFile); a configured `storeFile` must exist or the build fails. Without it, Release packaging tasks (`assemble*Release`, `bundle*Release`, `install*Release`) are refused unless the explicit local opt-in `-PallowDebugSigning=true` (or `SSILENCCE_ALLOW_DEBUG_SIGNING=true`) is passed; opt-in artifacts are debug-signed, NON-PRODUCTION and must not be distributed. Debug, lint and IDE sync work without a keystore (ADR-039).
- **Last.fm keys:** Injected via `LASTFM_API_KEY`/`LASTFM_SECRET` from `local.properties` or environment; only for `github`/`playstore` flavors (`fdroid` gets none).
- **Flavor source sets:** `github` and `fdroid` add `src/shared/java`; `playstore` uses its own stub (`PaxsenixSearchHelper` always returns null, no Lyrically API access).
- **Versioning:** `versionCode`/`versionName` derive from the `Version` classes in `app/build.gradle.kts`; `check(versionCode == currentVersionCode)` fails the build if they diverge. Never change the version (rule 8).
- **CI:** `RELEASE_TYPE=CI` disables ABI splits and appends the git hash to the version name; APK names follow `ssilencce-<version>-<flavor>-<abi>.apk`.

## Fork Notes

- This repository is **ssilencce**, a fork of Booming Music; `applicationId` and `namespace` are now `com.ssilencce`.
- Changing the `applicationId` lets the app install alongside the upstream app, but the ID is permanent once published on a store (e.g. Play Store). Changing the `namespace` additionally requires updating all `com.ssilencce.R` imports and manifest component references.
- **Built-in updater (github flavor):** `GitHubService.kt` defaults and `public.properties` now target the fork (`TuiCibrian/ssilencce`; `DOWNLOAD_URL` points at the fork's releases), so the old upstream `mardous/BoomingMusic` warning is stale — keep both pointed at the fork so the updater never offers mismatched (differently signed) APKs.
- Use your own release keystore; do not touch `metadata/`, README, or contributor info without explicit request (rule 9).

## Repository Code Map

Package-level map (condensed). Full per-file analysis: see `docs/CODE_MAP.md`.

All paths are relative to `app/src/main/java/com/ssilencce/` unless stated otherwise.

### Root & Build Configuration
- `app/build.gradle.kts` — Flavors (github/fdroid/playstore), semantic `Version` classes, signing, ABI splits, Last.fm key injection, CI APK naming with git hash, WorkManager runtime for scheduled backups/statistics CSV, per-flavor Chromecast source sets (`src/cast` for github/playstore, `src/nocast` for fdroid) and `media3-cast` dependencies (same media3 version).
- `build.gradle.kts`, `settings.gradle.kts`, `gradle/libs.versions.toml` — Plugin declarations, repository/module setup, version catalog.
- `public.properties` — Per-flavor public config (download/donation URLs, lyrics API endpoints).
- `app/src/main/AndroidManifest.xml` — Permissions (`SET_WALLPAPER`, `WRITE_SETTINGS`), activities (`AudioCutterActivity`, `MiniPlayerActivity` with audio VIEW filters, `SetupWizardActivity`, `LockScreenPlayerActivity`), `PlaybackService` (MediaLibraryService), `PlaybackTileService` (Quick Settings), widget receivers, FileProvider/CoverProvider, Android Auto.
- `metadata/`, `assets/`, `.github/workflows/` — Store listings (37 locales), store badges, CI/release pipelines.

### core/ — Base, models & widgets
- `core/SsilencceDatabase.kt` — Room database (v9) with 12 entities, 10 DAOs and migrations through 8→9.
- `core/appwidgets/` — 3 Glance widgets (SsilencceGlanceWidget/CardWidget/FullWidget) + receivers + serialized `PlaybackState` in DataStore, per-widget System/AlbumArt/Wallpaper settings and config (`WidgetSettings`, `WallpaperThemeProvider`, `WidgetConfigActivity`).
- `core/audio/` — `AudioOutputObserver` (audio device/volume/mute/bit-perfect flows), `AutoEqTxtParser`.
- `core/model/` — Domain models: action/ (incl. QuickAccessAction), audiodevice/ (incl. cast/remote MediaRouter route type), equalizer/ (AutoEq, ISO band tables/resampler, system-equalizer hand-off, reverb), filesystem/ (FolderInfo), list/, lockscreen/, lyrics/, menu/ (entity menu catalog), player/ (notification button/style), shuffle/ (three-state model and behavior), sort/, tag/ (batch cover), task/, theme/, plus ArtistNamePolicy, CategoryInfo, ComposerNamePolicy, GenreSubcategory, MediaEvent, MediaItemExtras, NotPlayedDaysPolicy, PaletteColor, GridViewType.
- `core/palette/PaletteProcessor.kt` — Cover color extraction (QuantizerCelebi + Palette).
- `core/sort/` — Locale-aware sorting (SortCollator, SortMode, SongPlayStatisticsCache).

### data/ — Data, repositories & network
- `data/model/` — Song (disc number, resolved artist, MIME)/Album/Artist (fallback name/stable id)/Genre/GenreContent/Playlist/Folder/ReleaseYear, ListeningPeriod/ArtistPlayStat, ExtraFormatUriStore, lyrics/ (incl. source selection), notes/ (SongNotes + codec), network/, replaygain/, search/ models.
- `data/local/` - MediaStore layer (MediaQueryDispatcher, MediaStoreObserver/Writer), MetadataReader/Writer (TagLib, batch cover/rollback), BatchWriteJournal, TagWriteVerifier, ReplayGainTagExtractor + `replaygain/`; lyrics parsers (LRC + TTML), InstrumentalDetector, lyrics source store; `notes/` — file-backed per-song notes and bookmarks (`notes/<song id> - <title>.txt`), atomic writes and backup file listing; `extra/` — opt-in AIFF/DSD supplementary index and header probe; `audio/clip/` — lossless audio range export (`AudioClipExporter`, `Mp3RawTrimmer`), clip naming/format models and `RingtoneClipStore`; `LibraryRescan` (chosen-folder MediaScanner rescan with process-scoped debounce) and `MediaMountRescanReceiver`.
- `data/local/room/` — 12 entities + 10 DAOs (playlists, queue, play counts, history, lyrics cache, include/exclude, play events, track positions, named queues, song_playback_params) and the PlaylistWithSongs relation; `SongPlayStatistics` projection.
- `data/local/repository/` — `Repository` interface + implementations (Song/Album/Artist/Genre/Playlist/Search/Smart/Special/Lyrics/Network) + `NamedQueueRepository`, `NamedQueueMaintenancePolicy` (append-merge/LRU eviction) and `MissingSongsCleanup`; song/playlist/history/genre queries apply the ghost filter centrally and merge extra-format songs.
- `data/local/search/` — Search filters (Smart/Basic/LastAdded) + mappers.
- `data/remote/` — KtorClient (10 MB cache), Deezer, GitHub (updater), Last.fm, ListenBrainz, MusicBrainz, lyrics APIs (Lyrically/BetterLyrics/LRCLib).
- `src/shared/` vs `src/playstore/` — `PaxsenixSearchHelper`: real iTunes Search client (github/fdroid) vs null stub (playstore).

### playback/ — Playback engine
- `playback/PlaybackService.kt` — Core: MediaLibraryService owning the active local or Cast session player, MediaSession, equalizer, notifications, Bluetooth, widgets, audio focus, sleep timer, crossfade, A-B repeat and session recovery.
- `playback/` resilience — `AudioFocusController` (Normal/Ignore/Ask/ResumeAfterCall, pending-ask cleanup, ducking), `PlaybackTileService` (Quick Settings), ordered non-blocking `TrackPositionWriter`, bounded `SESSION_RECOVERY`, `ResumeAutoplayPolicy`, `PlaybackVolumeController` + `PlaybackFadeController` (fade-in/out and duck/equalizer/crossfade volume composition), song-count/stop-after-song sleep timer with persisted recent timers and a foreground-only stop policy, dynamic screen-off trigger (only while the active player is playing) feeding the lock-screen player, `RepeatPolicy` (off/all/one/load-next-and-pause), `ShuffleCyclePolicy`, `ShuffleStateHolder`.
- Queue/persistence: AdvancedForwardingPlayer (explicit playback-intent gate, mute/fade/crossfade hooks), ImprovedShuffleOrder, `RandomizeShuffleOrder`, PersistentStorage + application-owned PlaybackStateWriter (ordered active/named snapshots and restore barrier), ProgressObserver, SleepTimer, Playback(+Extension).
- `playback/crossfade/` — `CrossfadeController` (dual-player equal-power crossfade, manual-skip interception, main-player commit), `CrossfadePlayer` (secondary ExoPlayer mirroring renderers and audio session, no wake lock/focus) and `GaplessAlbumDetector`.
- `playback/cast/` — Flavor-agnostic `CastPlayback` contract (main) plus the real `CastPlayer` + Cast SDK session + embedded LAN HTTP server implementation (`src/cast`, github/playstore) and the GMS-free no-op provider (`src/nocast`, fdroid).
- `playback/notification/` — custom Media3 system provider (configurable buttons, ghost masking), lock-screen artwork policy and opt-in RemoteViews notification styles.
- `playback/equalizer/` — EqualizerManager + engines (BasicEQEngine, DynamicsProcessingEngine) with 3/5/10/15/31-band resampling, system EQ hand-off and session-bound environmental reverb controller.
- `playback/processor/` + `playback/renderer/` — Balance, runtime stereo/mono downmix and ReplayGain processors injected into DefaultAudioSink; gapless gate; ALAC codec workaround.
- `playback/extractor/` — AIFF/DSD extra extractors, DSD decimator, FFmpeg capability probe and extra-format support.
- `playback/library/` + `playback/shuffle/` — MediaItem resolution (incl. AAOS, folders, composers and current queue), Auto media buttons and smart shuffle modes.

### coil/ — Image loading
- Fetchers: AudioCoverFetcher, ArtistImageFetcher, AutoGeneratedImageFetcher (mosaics), PlaylistImageFetcher (all respect disable-album-art).
- CoverProvider (content:// URIs), CustomArtistImageManager, CustomPlaylistImageManager, KeyerStore/MapperStore, bitmap loader for notifications.

### util/ and extensions/
- `util/Preferences.kt` — Central typed access to all app settings (incl. audio-focus mode, per-track resume, session recovery, active named queue, transport/fade/crossfade, notification layout/style, ghost mode, lock screen, extra-format scan and shuffle behavior).
- BackupHelper (ZIP backup/restore, per-song playback params, song notes `notes/*.txt`, strict playlist-write failures, valid empty-playlist entries, structured `createBackupFile` results, RestoreSummary), `util/backup/` (WorkManager scheduled backups), MusicUtil (batch delete), CryptoUtil (Keystore AES), StorageUtil, ShortcutHelper (dynamic entity shortcuts), m3u/ (staged M3U/M3U8/PLS export), cursor/, oem/ (MIUI/Samsung output switcher, `PowerManagementGuide`, `LockScreenPermissionGuide`), `AppVisibility`, `GhostMode`, `FolderInfoProvider`, `FolderFileOperations`.
- `extensions/` — Activity, Fragment, Context, files/ (File, SAF incl. ordered path resolution/display names), media/, navigation/, resources/ (Color/Drawable/View), utilities/.

### App entry points
- `App.kt` — Application: Koin startup, default preferences, crash handler, custom Coil ImageLoader, new-install wizard flag, process-scoped media-mount rescan receiver and AppVisibility registration.
- `MainModule.kt` — All Koin DI modules (services, database v9, repositories, playback, ViewModels, audio clip export/ringtone store, screencast manager, setup wizard).
- `MediaControllerOwner.kt` — Lifecycle-aware MediaController for the PlaybackService MediaSession.

### ui/ — Components, adapters & theme
- `ui/adapters/` — Adapter per entity (song/album/artist/genre/playlist/year/file/search/home) with multi-select (CAB).
- `ui/component/base/` — Base activities/fragments (AbsSlidingMusicPanelActivity, AbsRecyclerViewFragment with quick-search host, AbsPlayerFragment, AbsTagEditorActivity...) and the QuickSearchable contract.
- `ui/component/compose/` — Reusable composables, incl. lyrics views (LyricsView, KaraokeStyleText, LyricsSelectionBar), QuickAccessRow, AbRepeatChip, menus, sliders, preferences, QuickSearchBar.
- `ui/component/views/` — Legacy views (MusicSlider with thumb-only mode, MorphicIconButton, SquigglyProgress...).
- `ui/component/compose/aurora/` — Optional Aurora ribbon, six-destination navigation, grouped vector-mark drawer (Library/Discover/Sound & system), home actions and explicit unavailable states; `ui/theme/AuroraDesign.kt`, `AuroraLegacyStyling.kt` and `AuroraHeaderScroll.kt` bridge monochrome styling and the floating header treatment to both UI stacks (ADR-036, ADR-037).
- `ui/component/transform/` — 10 ViewPager PageTransformers (carousel, depth, flips, stack...).
- `ui/component/preferences/` — Custom preferences + Compose preference dialogs (menu customization, notification layout/background, quick access).
- `ui/dialogs/` — Playlist, delete-songs, SAF, folder-chooser, multi-check dialogs.
- `ui/theme/` — SsilencceTheme (custom accent), static M3 palettes, typography, Monet dynamic schemes.

### ui/screen/ — Screens
- `MainActivity.kt` (NavHost, playback intents, shortcuts, first-run setup-wizard gate) + `PermissionsActivity.kt`.
- library/ — Home + list/detail fragments per entity (albums, artists, genres, playlists, years, folders, songs) + search + `LibraryViewModel`; seven top-level lists implement inline quick search; album list filters by composer on API 30+, multi-disc album detail, genre detail offers Songs/Albums/Artists subcategories, and Not Recently Played exposes an arbitrary 1–3650-day threshold with history-interval fallback; folders/ has info/create/rename/exclude/move/copy/hide dialogs.
- player/ — `PlayerViewModel`, gestures controller, cover pager + 7 player styles, each with controls fragment; named-queue CRUD/merge, A-B repeat, per-song tempo, seek helpers, artwork/share actions, screencast (`player/screencast/` Presentation-based external display), cast/screencast overflow and audio-focus session commands.
- player/aurora/ — Compose Aurora chrome for the same seven saved styles, reusing the native cover pager, gestures, ViewModel and inherited song actions; non-Aurora players remain available.
- lyrics/ (select-lines multi-select/copy/share, per-song source picker, manual LRC sync), queue/ (named queues, merge/eviction, queue-local search, configurable Queues category browser), settings/ (settings search, scheduled-backup/statistics sheets, notification style/background, menu customization, quick access, lock screen, ghost mode, setup wizard, rescan, remove missing songs, battery guide, accent color), equalizer/ (system card, reverb card, ReplayGain extras), replaygain/, tageditor/ (single-song editor, LegacyTagAggregator, and the multi-select + single-entity album/artist Compose batch editor with per-file values, cover replace/delete, cooperative stop, in-place backup/restore and session undo; the legacy album/artist editors remain declared but are no longer reachable from menus), sleeptimer/ (song count, history, foreground-only policy), scrobbling/, sound/ (mono, crossfade, per-song tempo), update/, about/, info/, other/ (mini-player, shuffle modes, detail lists with period stats/history removal), error/ (crash handler).
- audiocutter/ (Compose trimmer, preview and system-sound actions), ghost/ (Ghost search), lockscreen/ (keyguard-overlay player), miniplayer/ (dialog external-audio/preview player), notes/ (notes editor/bookmark list), setup/ (first-run library setup).

### res/ (resources)
- drawable/ (~180 XML icons, AVDs, shadows), layout/ (~100, incl. layout-land/ and the two custom notification layouts), values/ (strings + ~40 locales, colors, dimens, styles), navigation/ (2 graphs incl. notes/ghost/lock-screen destinations), menu/ (23, incl. A-B/seek/share/screencast/cast and shuffle-selection entries), xml/ (preferences incl. battery/lock-screen, widgets info, shortcuts, automotive), font/anim/raw/.

## Agent Skills

Preconfigured skills are installed under `.agents/skills/` (managed by the `skills` CLI, tracked in `skills-lock.json`) and `.opencode/skills/` (OpenSpec workflow). **AGENTS.md overrides every skill**: where a skill conflicts with these guidelines, the rules here win.

### Installed skills catalog

- **Android architecture & Gradle** (new-silvermoon): `android-architecture`, `android-data-layer`, `android-viewmodel`, `android-coroutines`, `android-gradle-logic`, `android-retrofit`, `coil-compose`, `compose-ui`, `android-accessibility`, `compose-performance-audit`, `gradle-build-performance`, `xml-to-compose-migration`
- **Engineering** (mattpocock): `code-review`
- **Security** (semgrep / getsentry): `code-security`, `gha-security-review`
- **Docs** (github/awesome-copilot): `documentation-writer`
- **Utilities** (kept from earlier set): `find-docs`, `find-skills`, `android-kotlin-core`, `android-testing-unit`
- **OpenSpec** (fission-ai): `.opencode/skills/`: `openspec-apply-change`, `openspec-archive-change`, `openspec-explore`, `openspec-propose`, `openspec-sync-specs`, `openspec-update-change`; `.agents/skills/`: `openspec-new-change`, `openspec-continue-change`, `openspec-onboard`, `openspec-verify-change`

Load a skill with the `skill` tool when a task matches its description; do not read `SKILL.md` files by hand unless verifying setup.

### Stack exceptions (generic-pattern advice does not apply)

Generic Android skills assume Hilt + Retrofit; this repo deliberately differs (ADRs are the source of truth). When a skill proposes a pattern that contradicts the stack, the stack wins:

- **Dependency Injection:** Koin, not Hilt (ADR-006). Do not generate Hilt `@Module`/`@HiltViewModel`/`@AndroidEntryPoint` code; Koin modules live in `MainModule.kt`.
- **Networking:** Ktor, not Retrofit idioms (ADR-020). Do not generate Retrofit `interface Api { @GET ... }` services; Ktor services live under `data/remote/`.
- **Navigation:** Jetpack Navigation 2.9.8 + safeargs (ADR-027), not `navigation3`.
- **Layering:** Clean-architecture advice is a guide, not a mandate — single-module app with a pragmatic repository layer (ADR-010).

### Issue tracker

Changes and specs are tracked with OpenSpec at `openspec/changes/`. See `docs/agents/issue-tracker.md`.

### Domain docs

Single-context; ADRs under `docs/architecture/` (ADR-001 through ADR-065) plus feature docs: `docs/FEATURES.md` (what
exists), `docs/MISSING_FEATURES.md` (gaps vs Musicolet) and `docs/MUSICOLET_FEATURES.md`
(reference snapshot of the other app). See `docs/agents/domain.md` and the
[Documentation Maintenance](#documentation-maintenance) section.

## Documentation Maintenance

Keep the project docs in sync with the code. When a change is significant enough that a
future reader of the docs would be misled, update the relevant doc in the same working
set as the code change:

- **`docs/CODE_MAP.md`** — add an entry for each new source file and update entries whose
  file's responsibilities change (renames, package moves, new core logic). Keep the
  existing per-file format (Purpose / Responsibilities / Inputs / Outputs / Dependencies /
  Consumed by / Related files / Notes).
- **`docs/FEATURES.md`** — add new user-facing features (settings, screens, behaviors) to
  the matching numbered section, in the same bullet style.
- **`docs/MISSING_FEATURES.md`** — when an implemented feature was listed as missing,
  remove it from Part 1 / Part 2 / Appendix A as applicable; if only partially
  implemented, reword the bullet to describe the remaining gap. Never edit
  `docs/MUSICOLET_FEATURES.md` (pinned reference snapshot of another app).
- **`docs/architecture/ADR-*.md`** — create a new ADR (next free number, template
  Status/Context/Decision/Consequences) when the change affects an architectural
  decision: stack choices, layering, data flow, persistence, playback engine,
  navigation, theming, packaging. Read and cite the relevant ADRs first (see
  `docs/agents/domain.md`); mark superseded ADRs accordingly.
- **`docs/agents/domain.md`** — keep the ADR count and doc references up to date.
- **`AGENTS.md`** — keep the condensed Repository Code Map and this section in sync.

Trivial changes (variable renames, formatting, refactors with no behavior change) do not
require doc updates. When in doubt, consult the human collaborator (rule 7).

## Rules for AI Agents

1. **Git Integrity:** The agent must not execute commands that compromise the Git branch or alter history unless specifically demanded by the human user. In such cases, the user assumes responsibility for reviewing the changes.
2. **Commits and Pushing:** The agent must not perform commits or push changes unless explicitly instructed to do so.
3. **Localization Strings:** When modifying or adding strings, only use the main strings file (located in the `values` folder). The agent is prohibited from directly modifying strings files for other languages.
4. **Code Comments:** Add comments only for non-obvious or complex code. Avoid commenting simple lines to keep the code readable and non-redundant.
5. **Human Collaboration:** Always follow the rules and guidelines provided by the human collaborator.
6. **Performance and Battery Life:** Prioritize performance and battery consumption in all changes. Always consider the impact on the user experience. The agent must inform the human collaborator if a requested change might compromise these aspects.
7. **No Assumptions:** If in doubt, the agent must always consult the human collaborator instead of making assumptions about the code's logic or functionality.
8. **Version Number:** It is strictly forbidden for the agent to alter the application's version number. This task is reserved for the primary development team.
9. **Contributors and Collaborators:** The agent must not modify any information about project contributors or collaborators.
10. **Test Configuration:** Do not attempt to add test configurations unless they are already present in the project or specifically requested by the human user.
11. **Language:** All code, comments, and documentation produced by the agent must be in English.
12. **UI Development:** New UI changes or additions must prioritize Jetpack Compose. The project is currently migrating from the legacy View/XML system to Compose.
13. **Documentation Maintenance:** Significant changes must keep the project docs in sync (see [Documentation Maintenance](#documentation-maintenance)): new or significantly edited source files → `docs/CODE_MAP.md`; new user-facing features → `docs/FEATURES.md`; implemented gaps → remove from `docs/MISSING_FEATURES.md`; architectural decisions → new ADR under `docs/architecture/`.

<!-- context7 -->
Use the `ctx7` CLI to fetch current documentation whenever the user asks about a library, framework, SDK, API, CLI tool, or cloud service — even well-known ones like React, Next.js, Prisma, Express, Tailwind, Django, or Spring Boot. This includes API syntax, configuration, version migration, library-specific debugging, setup instructions, and CLI tool usage. Use even when you think you know the answer — your training data may not reflect recent changes. Prefer this over web search for library docs.

Do not use for: refactoring, writing scripts from scratch, debugging business logic, code review, or general programming concepts.

## Steps

1. Resolve library: `npx ctx7@latest library <name> "<what to look up>"` — use the official library name with proper punctuation (e.g., "Next.js" not "nextjs", "Customer.io" not "customerio", "Three.js" not "threejs")
2. Pick the best match (ID format: `/org/project`) by: exact name match, description relevance, code snippet count, source reputation (High/Medium preferred), and benchmark score (higher is better). If results don't look right, try alternate names or queries (e.g., "next.js" not "nextjs", or rephrase the question)
3. Fetch docs: `npx ctx7@latest docs <libraryId> "<what to look up>"` — run a separate `docs` command per distinct concept if the question spans multiple topics, unless it's about how they interact
4. Answer using the fetched documentation

You MUST call `library` first to get a valid ID unless the user provides one directly in `/org/project` format. Be specific about what to look up in the library's documentation — specific and detailed queries return better results than vague single words, but keep each query to a single concept unless the question is about how concepts interact; combined multi-topic queries dilute ranking and return shallow results for each topic. Do not run more than 3 commands per question. Do not include sensitive information (API keys, passwords, credentials) in queries.

For version-specific docs, use `/org/project/version` from the `library` output (e.g., `/vercel/next.js/v14.3.0`).

If a command fails with a quota error, inform the user and suggest `npx ctx7@latest login` or setting `CONTEXT7_API_KEY` env var for higher limits. Do not silently fall back to training data.
<!-- context7 -->
