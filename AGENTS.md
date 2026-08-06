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
- **Tests:** No test source sets are configured; do not add test configurations (rule 12).
- **Signing:** Release reads `keystore.properties` (keyAlias/keyPassword/storePassword/storeFile); if missing it silently falls back to the debug keystore.
- **Last.fm keys:** Injected via `LASTFM_API_KEY`/`LASTFM_SECRET` from `local.properties` or environment; only for `github`/`playstore` flavors (`fdroid` gets none).
- **Flavor source sets:** `github` and `fdroid` add `src/shared/java`; `playstore` uses its own stub (`PaxsenixSearchHelper` always returns null, no Lyrically API access).
- **Versioning:** `versionCode`/`versionName` derive from the `Version` classes in `app/build.gradle.kts`; `check(versionCode == currentVersionCode)` fails the build if they diverge. Never change the version (rule 10).
- **CI:** `RELEASE_TYPE=CI` disables ABI splits and appends the git hash to the version name; APK names follow `ssilencce-<version>-<flavor>-<abi>.apk`.

## Fork Notes

- This repository is **ssilencce**, a fork of Booming Music; `applicationId` and `namespace` are now `com.ssilencce`.
- Changing the `applicationId` lets the app install alongside the upstream app, but the ID is permanent once published on a store (e.g. Play Store). Changing the `namespace` additionally requires updating all `com.ssilencce.R` imports and manifest component references.
- **Built-in updater (github flavor):** `public.properties` → `github.GITHUB_API_URL` points at the upstream repo `mardous/BoomingMusic`. Before publishing the fork, point it at the fork repository; otherwise the updater offers upstream APKs signed with a different key (install failure).
- Use your own release keystore; do not touch `metadata/`, README, or contributor info without explicit request (rules 1, 11).

## Repository Code Map

Package-level map (condensed). Full per-file analysis: see `docs/CODE_MAP.md`.

All paths are relative to `app/src/main/java/com/ssilencce/` unless stated otherwise.

### Root & Build Configuration
- `app/build.gradle.kts` — Flavors (github/fdroid/playstore), semantic `Version` classes, signing, ABI splits, Last.fm key injection, CI APK naming with git hash.
- `build.gradle.kts`, `settings.gradle.kts`, `gradle/libs.versions.toml` — Plugin declarations, repository/module setup, version catalog.
- `public.properties` — Per-flavor public config (download/donation URLs, lyrics API endpoints).
- `app/src/main/AndroidManifest.xml` — Permissions, activities, `PlaybackService` (MediaLibraryService), widget receivers, FileProvider/CoverProvider, Android Auto.
- `metadata/`, `assets/`, `.github/workflows/` — Store listings (37 locales), store badges, CI/release pipelines.

### core/ — Base, models & widgets
- `core/SsilencceDatabase.kt` — Room database (v6) with entities and migrations.
- `core/appwidgets/` — 3 Glance widgets (SsilencceGlanceWidget/CardWidget/FullWidget) + receivers + serialized `PlaybackState` in DataStore.
- `core/audio/` — `AudioOutputObserver` (audio device/volume/bit-perfect flows), `AutoEqTxtParser`.
- `core/model/` — Domain models: action/, audiodevice/, equalizer/ (incl. AutoEq), filesystem/, lyrics/, player/, shuffle/, sort/, task/, theme/, plus CategoryInfo, MediaEvent, PaletteColor, GridViewType.
- `core/palette/PaletteProcessor.kt` — Cover color extraction (QuantizerCelebi + Palette).
- `core/sort/` — Locale-aware sorting (SortCollator, SortMode).

### data/ — Data, repositories & network
- `data/model/` — Song/Album/Artist/Genre/Playlist/Folder/ReleaseYear + lyrics/, network/, replaygain/, search/ models.
- `data/local/` — MediaStore layer (MediaQueryDispatcher, MediaStoreObserver/Writer), MetadataReader/Writer (TagLib), ReplayGainTagExtractor; lyrics parsers (LRC + TTML) and InstrumentalDetector.
- `data/local/room/` — 8 entities + DAOs (playlists, queue, play counts, history, lyrics cache, include/exclude).
- `data/local/repository/` — `Repository` interface + implementations (Song/Album/Artist/Genre/Playlist/Search/Smart/Special/Lyrics/Network).
- `data/local/search/` — Search filters (Smart/Basic/LastAdded) + mappers.
- `data/remote/` — KtorClient (10 MB cache), Deezer, GitHub (updater), Last.fm, ListenBrainz, MusicBrainz, lyrics APIs (Lyrically/BetterLyrics/LRCLib).
- `src/shared/` vs `src/playstore/` — `PaxsenixSearchHelper`: real iTunes Search client (github/fdroid) vs null stub (playstore).

### playback/ — Playback engine
- `playback/PlaybackService.kt` — Core: MediaLibraryService owning ExoPlayer, MediaSession, equalizer, notifications, Bluetooth, widgets.
- Queue/persistence: AdvancedForwardingPlayer, ImprovedShuffleOrder, PersistentStorage, ProgressObserver, SleepTimer, Playback(+Extension).
- `playback/equalizer/` — EqualizerManager + engines (BasicEQEngine, DynamicsProcessingEngine).
- `playback/processor/` + `playback/renderer/` — Balance/ReplayGain audio processors injected into DefaultAudioSink; ALAC codec workaround.
- `playback/library/` + `playback/shuffle/` — MediaItem resolution (incl. AAOS), smart shuffle modes.

### coil/ — Image loading
- Fetchers: AudioCoverFetcher, ArtistImageFetcher, AutoGeneratedImageFetcher (mosaics), PlaylistImageFetcher.
- CoverProvider (content:// URIs), CustomArtistImageManager, CustomPlaylistImageManager, KeyerStore/MapperStore, bitmap loader for notifications.

### util/ and extensions/
- `util/Preferences.kt` — Central typed access to all app settings.
- BackupHelper (ZIP backup), MusicUtil (batch delete), CryptoUtil (Keystore AES), StorageUtil, m3u/ (M3U export), cursor/, oem/ (MIUI/Samsung output switcher).
- `extensions/` — Activity, Fragment, Context, files/ (File, SAF), media/, navigation/, resources/ (Color/Drawable/View), utilities/.

### App entry points
- `App.kt` — Application: Koin startup, default preferences, crash handler, custom Coil ImageLoader.
- `MainModule.kt` — All Koin DI modules (services, database, repositories, playback, ViewModels).
- `MediaControllerOwner.kt` — Lifecycle-aware MediaController for the PlaybackService MediaSession.

### ui/ — Components, adapters & theme
- `ui/adapters/` — Adapter per entity (song/album/artist/genre/playlist/year/file/search/home) with multi-select (CAB).
- `ui/component/base/` — Base activities/fragments (AbsSlidingMusicPanelActivity, AbsRecyclerViewFragment, AbsPlayerFragment, AbsTagEditorActivity...).
- `ui/component/compose/` — Reusable composables, incl. lyrics views (LyricsView, KaraokeStyleText), menus, sliders, preferences.
- `ui/component/views/` — Legacy views (MusicSlider, MorphicIconButton, SquigglyProgress...).
- `ui/component/transform/` — 10 ViewPager PageTransformers (carousel, depth, flips, stack...).
- `ui/component/preferences/` — Custom preferences + Compose preference dialogs.
- `ui/dialogs/` — Playlist, delete-songs, SAF, folder-chooser, multi-check dialogs.
- `ui/theme/` — SsilencceTheme, static M3 palettes, typography, Monet dynamic schemes.

### ui/screen/ — Screens
- `MainActivity.kt` (NavHost, playback intents, shortcuts) + `PermissionsActivity.kt`.
- library/ — Home + list/detail fragments per entity (albums, artists, genres, playlists, years, folders, songs) + search + `LibraryViewModel`.
- player/ — `PlayerViewModel`, gestures controller, cover pager + 7 player styles (Default, FullCover, Gradient, Plain, M3, Expressive, Peek), each with controls fragment.
- lyrics/, queue/, settings/, equalizer/, tageditor/, sleeptimer/, scrobbling/, sound/, update/, about/, info/, other/ (mini-player, shuffle modes), error/ (crash handler).

### res/ (resources)
- drawable/ (~170 XML icons, AVDs, shadows), layout/ (~100, incl. layout-land/), values/ (strings + ~40 locales, colors, dimens, styles), navigation/ (2 graphs), menu/ (~25), xml/ (preferences, widgets info, shortcuts, automotive), font/anim/raw/.

## Rules for AI Agents

1. **Documentation and Workflows:** The agent must not modify documentation files like `README.md` or GitHub templates/workflows unless explicitly requested.
2. **Git Integrity:** The agent must not execute commands that compromise the Git branch or alter history unless specifically demanded by the human user. In such cases, the user assumes responsibility for reviewing the changes.
3. **Commits and Pushing:** The agent must not perform commits or push changes unless explicitly instructed to do so.
4. **Localization Strings:** When modifying or adding strings, only use the main strings file (located in the `values` folder). The agent is prohibited from directly modifying strings files for other languages.
5. **Commit Message Format:** Commits must follow the format: `type(scope): short description`. 
   - *Example:* `feat(player): add blur radius setting`
6. **Code Comments:** Add comments only for non-obvious or complex code. Avoid commenting simple lines to keep the code readable and non-redundant.
7. **Human Collaboration:** Always follow the rules and guidelines provided by the human collaborator.
8. **Performance and Battery Life:** Prioritize performance and battery consumption in all changes. Always consider the impact on the user experience. The agent must inform the human collaborator if a requested change might compromise these aspects.
9. **No Assumptions:** If in doubt, the agent must always consult the human collaborator instead of making assumptions about the code's logic or functionality.
10. **Version Number:** It is strictly forbidden for the agent to alter the application's version number. This task is reserved for the primary development team.
11. **Contributors and Collaborators:** The agent must not modify any information about project contributors or collaborators.
12. **Test Configuration:** Do not attempt to add test configurations unless they are already present in the project or specifically requested by the human user.
13. **Language:** All code, comments, and documentation produced by the agent must be in English.
14. **UI Development:** New UI changes or additions must prioritize Jetpack Compose. The project is currently migrating from the legacy View/XML system to Compose.

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
