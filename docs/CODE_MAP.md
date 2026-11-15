# Repository Code Map

Full per-file analysis of the ssilencce fork (Booming Music). Referenced from `AGENTS.md`.

All paths are relative to `app/src/main/java/com/ssilencce/` unless stated otherwise.

### Root & Build Configuration

- `build.gradle.kts` — Top-level build file declaring shared plugins (AGP, SafeArgs, Compose compiler, KSP, AboutLibraries).
  - **Purpose:** Root Gradle build script for the ssilencce project.
  - **Responsibilities:** Declares shared plugins with `apply false`; no per-module configuration.
  - **Inputs:** Plugin aliases from `gradle/libs.versions.toml`.
  - **Outputs:** AGP, SafeArgs, Compose compiler, KSP, AboutLibraries plugins available to modules.
  - **Dependencies:** Gradle 9.5 wrapper, version catalog.
  - **Consumed by:** `app/build.gradle.kts`, which re-alias the same plugins.
  - **Related files:** `settings.gradle.kts`, `gradle/libs.versions.toml`, `gradle.properties`.
  - **Notes:** Single-module project; all plugin versions centralized.


- `settings.gradle.kts` — Repositories (google, mavenCentral, jitpack) and `:app` module inclusion.
  - **Purpose:** Configures Gradle repository resolution and project layout.
  - **Responsibilities:** `pluginManagement` repos, `FAIL_ON_PROJECT_REPOS`, `rootProject.name = "ssilencce"`, `include(":app")`.
  - **Inputs:** Plugin and dependency resolution settings.
  - **Outputs:** Resolved repositories; registered `:app` module.
  - **Dependencies:** google(), mavenCentral(), gradlePluginPortal(), jitpack.io.
  - **Consumed by:** Gradle build for all tasks.
  - **Related files:** `build.gradle.kts`, `gradle/libs.versions.toml`.
  - **Notes:** JitPack hosts m3color, coil-transformations, advrecyclerview, jaudiotagger.


- `app/build.gradle.kts` — Module build: 3 flavors (github/fdroid/playstore), semantic version classes (Alpha/Beta/RC/Stable), signing, ABI splits, Last.fm key injection, CI APK naming with git hash, per-flavor Cast source sets/dependencies.
  - **Purpose:** Android application module configuration for `com.ssilencce`.
  - **Responsibilities:** Flavors, sealed `Version` classes, release-signing guard (no silent debug fallback, ADR-039), ABI splits, BuildConfig/ResValue injection, lint, dependencies.
  - **Inputs:** `keystore.properties`, `local.properties`, `public.properties`, `RELEASE_TYPE` env, git hash, flavor names.
  - **Outputs:** `ssilencce-<version>-<flavor>-<abi>.apk`; BuildConfig `IS_CI_BUILD`, `LASTFM_API_KEY`, `LASTFM_SECRET`, flavor props.
  - **Dependencies:** AGP 9.3.1, KSP, Kotlin 2.3.21, `jvmToolchain(21)`, version catalog bundles.
  - **Consumed by:** Gradle wrapper, `.github/workflows/android.yml` and `release.yml`.
  - **Related files:** `gradle/libs.versions.toml`, `public.properties`, `AndroidManifest.xml`.
  - **Notes:** `check(versionCode == currentVersionCode)`; current `Version.Beta(1,4,0,3)` maps to 1400103; release packaging tasks are refused without `keystore.properties` unless `-PallowDebugSigning=true`/`SSILENCCE_ALLOW_DEBUG_SIGNING=true` is passed, and a configured `storeFile` must exist; `github`/`fdroid` add `src/shared/java`.


- `gradle/libs.versions.toml` — Version catalog for all dependencies (Media3, Compose, Koin, Ktor, Coil, Room, TagLib, etc.).
  - **Purpose:** Central version catalog for plugins and libraries.
  - **Responsibilities:** `[versions]`, `[libraries]`, `[bundles]`, `[plugins]` sections.
  - **Inputs:** Coordinates: AGP 9.3.1, Kotlin 2.3.21, Media3 1.10.1, Room 2.8.4, Koin 4.2.2.
  - **Outputs:** Typed accessors used by both Gradle scripts.
  - **Dependencies:** Google, Maven Central, JitPack repositories.
  - **Consumed by:** `build.gradle.kts`, `app/build.gradle.kts`.
  - **Related files:** `gradle.properties`, `settings.gradle.kts`.
  - **Notes:** Bundles: kotlinx, lifecycle, media3, navigation, glance, koin, coil, ktor, markwon. `media3-cast` reuses the existing `media3` version for github/playstore.


- `gradle.properties` — Gradle and wrapper configuration.
  - **Purpose:** Project-wide Gradle and Android build settings.
  - **Responsibilities:** Enables caching, parallel builds, configuration cache; sets JVM args and AndroidX.
  - **Inputs:** Gradle feature flags and daemon memory settings.
  - **Outputs:** Build performance and memory configuration.
  - **Dependencies:** Gradle 9.5 daemon.
  - **Consumed by:** Every Gradle invocation.
  - **Related files:** `gradle/wrapper/gradle-wrapper.properties`.
  - **Notes:** `kotlin.code.style=official`; `r8.strictFullModeForKeepRules=false`.


- `public.properties` — Per-flavor public config (download/donation URLs, lyrics API endpoints).
  - **Purpose:** Holds per-flavor public configuration values.
  - **Responsibilities:** `DOWNLOAD_URL`, `DONATION_LINK`, `GITHUB_API_URL`, `LYRICALLY_API_URL` defaults and overrides.
  - **Inputs:** Flavor-prefixed keys (`fdroid.`, `playstore.`); parsed by `loadFlavorProperties`.
  - **Outputs:** BuildConfig string fields per variant.
  - **Dependencies:** `app/build.gradle.kts` property loading.
  - **Consumed by:** Built-in updater, lyrics provider, about/donation screens.
  - **Related files:** `app/build.gradle.kts`.
  - **Notes:** Fork `DOWNLOAD_URL` and the updater defaults target the fork (`TuiCibrian/ssilencce`).


- `app/src/main/AndroidManifest.xml` — Permissions (`SET_WALLPAPER`, `WRITE_SETTINGS`), activities (`AudioCutterActivity`, `MiniPlayerActivity` with audio VIEW filters, `SetupWizardActivity`, `LockScreenPlayerActivity`), PlaybackService (MediaLibraryService), widget receivers, FileProvider, CoverProvider, Android Auto, searchable config.
  - **Purpose:** Declares app components, permissions, and capabilities.
  - **Responsibilities:** Media/Bluetooth/notification permissions; activities; `PlaybackService`; widget receivers; providers.
  - **Inputs:** `.App` application class; `@xml/searchable`, `@xml/shortcuts`, `@xml/automotive_app_desc` resources.
  - **Outputs:** Component registration for the Android system.
  - **Dependencies:** `PlaybackService`, `SsilencceGlanceWidgetReceiver`, `CardWidgetReceiver`, `FullWidgetReceiver`, `CoverProvider`.
  - **Consumed by:** OS launcher, Media3 session clients, Android Auto, icon packs.
  - **Related files:** `app/build.gradle.kts` (`applicationId`), `res/xml` configs.
  - **Notes:** `activity-alias .activities.MainActivity` preserves icon-pack and CyanogenMod-Eleven compatibility.


- `metadata/` — Store listings per locale (37 languages).
  - **Purpose:** Localized store listing content.
  - **Responsibilities:** `short_description.txt`, `full_description.txt`, changelogs, icon per locale.
  - **Inputs:** 37 locale directories (ar-SA through zh-TW).
  - **Outputs:** Play Store/F-Droid listing material.
  - **Dependencies:** —
  - **Consumed by:** Store publishing pipelines.
  - **Related files:** `assets/`, `.github/workflows/release.yml`.
  - **Notes:** `en-US/changelogs` keyed by versionCode (1030300...1300300).


- `assets/` — Store badges (F-Droid, Play Store, GitHub, etc.).
  - **Purpose:** Download/store badge images.
  - **Responsibilities:** Six `badge-*.png` files: fdroid, github, izzyondroid, obtainium, openapk, playstore.
  - **Inputs:** —
  - **Outputs:** Badge graphics for README and listings.
  - **Dependencies:** —
  - **Consumed by:** README documentation and store pages.
  - **Related files:** `metadata/`.
  - **Notes:** No build-time packaging; purely documentation assets.


- `.github/workflows/` — CI (`android.yml`) and release (`release.yml`) pipelines; issue/PR templates.
  - **Purpose:** GitHub automation and contribution templates.
  - **Responsibilities:** CI build, release pipeline, `FUNDING.yml`, issue and PR templates.
  - **Inputs:** GitHub triggers, `RELEASE_TYPE=CI`, secrets, `LASTFM_API_KEY`.
  - **Outputs:** CI-built APKs and releases.
  - **Dependencies:** Gradle wrapper, keystore, signing configuration.
  - **Consumed by:** GitHub repository automation.
  - **Related files:** `app/build.gradle.kts` CI naming logic.
  - **Notes:** Includes `bug_report.yaml`, `feature_request.yaml`, `pull_request_template.md`.


- `gradle/wrapper/gradle-wrapper.properties` — Gradle and wrapper configuration.
  - **Purpose:** Pins the Gradle wrapper distribution version.
  - **Responsibilities:** Sets `distributionUrl` to Gradle 9.5.0-bin, network timeout, URL validation.
  - **Inputs:** Wrapper configuration properties.
  - **Outputs:** Deterministic Gradle version across builds.
  - **Dependencies:** services.gradle.org distribution download.
  - **Consumed by:** `gradlew`/`gradlew.bat` scripts.
  - **Related files:** `gradle.properties`.
  - **Notes:** Compatible with AGP 9.3.1 requirement.


### core/

- `core/SsilencceDatabase.kt` - Room database (v9) with all entities and migrations.
  - **Purpose:** Room database v9 for persistent app data.
  - **Responsibilities:** Declares 12 entities (plus the PlaylistWithSongs relation), 10 DAOs and 8 migrations (1_2 through 8_9).
  - **Inputs:** Entity class declarations, migration SQL statements.
  - **Outputs:** DAO instances, migrated SQLite schema.
  - **Dependencies:** androidx.room (Database, RoomDatabase, Migration), data/local/room package.
  - **Consumed by:** MainModule.kt (Room.databaseBuilder, DAO singletons), repository implementations.
  - **Related files:** PlaylistEntity, SongEntity, HistoryEntity, PlayCountEntity, QueueEntity, InclExclEntity, LyricsEntity, PlayEventEntity, TrackPositionEntity, NamedQueueEntity, NamedQueueItemEntity, SongPlaybackParamEntity.
  - **Notes:** exportSchema=false; 3_4 creates CanvasEntity, dropped again in 4_5; 5_6 recreates LyricsEntity; 6_7 recreates QueueEntity with `order` as primary key (ADR-038); 7_8 adds PlayEventEntity, TrackPositionEntity and the two named-queue tables additively (ADR-040, ADR-041, ADR-042); 8_9 creates the `song_playback_params` table additively (ADR-058).


- `core/appwidgets/SsilencceGlanceWidget.kt` — Responsive Glance widget with 5 layouts showing cover, title, artist and controls.
  - **Purpose:** Responsive main Glance home-screen widget.
  - **Responsibilities:** 5 layouts (Classic, Simplified, Medium, Large, ExtraLarge), playback/shuffle/favorite controls.
  - **Inputs:** PlaybackState from DataStore, LocalSize, WidgetTheme colors.
  - **Outputs:** Composed Glance UI, media-key Actions to PlaybackService.
  - **Dependencies:** Glance (GlanceAppWidget, SizeMode.Exact), PlaybackState, PlaybackStateDefinition, WidgetComponent.
  - **Consumed by:** SsilencceGlanceWidgetReceiver; AndroidManifest receiver.
  - **Related files:** CardWidget, FullWidget, WidgetComponent, WidgetExtension.
  - **Notes:** Click opens MainActivity; small layout chooses Simplified vs Classic per state flag; reads per-widget settings, resolves the Wallpaper fallback and renders WidgetEmptyState when title/artwork are empty.


- `core/appwidgets/SsilencceGlanceWidgetReceiver.kt` — Receiver registering SsilencceGlanceWidget.
  - **Purpose:** Register SsilencceGlanceWidget with the system.
  - **Responsibilities:** Exposes glanceAppWidget instance.
  - **Inputs:** —.
  - **Outputs:** Widget instance to Glance framework.
  - **Dependencies:** GlanceAppWidgetReceiver, SsilencceGlanceWidget.
  - **Consumed by:** AndroidManifest.xml receiver entry.
  - **Related files:** SsilencceGlanceWidget.
  - **Notes:** Minimal 8-line receiver subclass.


- `core/appwidgets/CardWidget.kt` — Card-sized widget with framed cover and bottom title/controls bar.
  - **Purpose:** Card-sized widget with framed cover and bottom controls.
  - **Responsibilities:** Padded cover frame, bottom surface bar with prev/play/next.
  - **Inputs:** PlaybackState, PlaybackStateDefinition, WidgetTheme.
  - **Outputs:** Composed Glance UI, service control intents.
  - **Dependencies:** Glance, WidgetComponent (AlbumArtGlance, ControlIconGlance), PlaybackStateDefinition.
  - **Consumed by:** CardWidgetReceiver.
  - **Related files:** FullWidget, SsilencceGlanceWidget, WidgetComponent.
  - **Notes:** SizeMode.Exact; whole widget clickable to MainActivity; per-widget theme/transparency with Wallpaper fallback and shared empty state.


- `core/appwidgets/CardWidgetReceiver.kt` — Receiver registering CardWidget.
  - **Purpose:** Register CardWidget with the system.
  - **Responsibilities:** Provides glanceAppWidget instance.
  - **Inputs:** —.
  - **Outputs:** CardWidget instance.
  - **Dependencies:** GlanceAppWidgetReceiver, CardWidget.
  - **Consumed by:** AndroidManifest.xml receiver entry.
  - **Related files:** CardWidget.
  - **Notes:** Minimal receiver subclass.


- `core/appwidgets/FullWidget.kt` — Full-size widget with cover, gradient scrim, progress bar and controls.
  - **Purpose:** Full-size widget with cover, gradient scrim, controls.
  - **Responsibilities:** createGradientScrim bitmap, shuffle/prev/play/next/repeat/favorite controls, responsive width.
  - **Inputs:** PlaybackState, currentSize, WidgetTheme colors, Context.
  - **Outputs:** Glance UI, scrim bitmap, service action intents.
  - **Dependencies:** Glance, WidgetComponent, PlaybackState, Media3 Player repeat modes, isDark.
  - **Consumed by:** FullWidgetReceiver.
  - **Related files:** SsilencceGlanceWidget, CardWidget, WidgetExtension.
  - **Notes:** Per-widget theme/transparency with Wallpaper fallback and shared empty state; scrim uses the 0.30 alpha floor; showAllActions when width ? 220dp.


- `core/appwidgets/FullWidgetReceiver.kt` — Receiver registering FullWidget.
  - **Purpose:** Register FullWidget with the system.
  - **Responsibilities:** Provides glanceAppWidget instance.
  - **Inputs:** —.
  - **Outputs:** FullWidget instance.
  - **Dependencies:** GlanceAppWidgetReceiver, FullWidget.
  - **Consumed by:** AndroidManifest.xml receiver entry.
  - **Related files:** FullWidget.
  - **Notes:** Minimal receiver subclass with GPL header.


- `core/appwidgets/WidgetComponent.kt` — Reusable Glance composables (album art, control icons, circular action button).
  - **Purpose:** Shared Glance composables used by all widgets.
  - **Responsibilities:** AlbumArtGlance decode + placeholder, WidgetEmptyState (Nothing playing, Open ssilencce, MainActivity click; icon+title+action at >=160dp, compact title/action below 160dp), ControlIconGlance, CircularControlIconGlance, ShapeableControlIconGlance.
  - **Inputs:** PlaybackState, drawable resIds, Dp sizes, ColorProviders, GlanceModifier, LocalSize for empty-state height.
  - **Outputs:** Reusable widget UI building blocks.
  - **Dependencies:** Glance (Image, ImageProvider, ColorFilter), BitmapFactory, PlaybackState.
  - **Consumed by:** SsilencceGlanceWidget, CardWidget, FullWidget.
  - **Related files:** PlaybackState, WidgetExtension.
  - **Notes:** Tinted music-note placeholder fallback; logs decode errors; compact empty state drops the icon below 160dp so title and open action stay visible without clipping.


- `core/appwidgets/WidgetExtension.kt` — Widget helpers: Dp→px, theme building with dynamic colors, service intents.
  - **Purpose:** Widget helpers for sizing, theming and service intents.
  - **Responsibilities:** Dp?px, WidgetTheme from dynamicColorSchemes, per-widget getColors with System/AlbumArt/Wallpaper selection and alpha floors, getScrimColor, 4 service Actions.
  - **Inputs:** Context, source color, PaletteStyle, media key codes.
  - **Outputs:** ColorProviders, playbackAction/toggleShuffleAction/cycleRepeatAction/toggleFavoriteAction.
  - **Dependencies:** Glance, dynamicColorSchemes (ui/theme/Monet), PlaybackService.
  - **Consumed by:** SsilencceGlanceWidget, CardWidget, FullWidget, PlaybackService.
  - **Related files:** PlaybackState (WidgetTheme), WallpaperThemeProvider, PlaybackService.
  - **Notes:** PaletteStyle.Fidelity, contrast 0.75; surface alpha floor 0.35 and scrim floor 0.30 are heuristic, not guaranteed contrast; System transparency wraps current Glance colors; Wallpaper falls back to album art then system when derivation is unavailable.


- `core/appwidgets/state/PlaybackState.kt` — Serializable playback state for widgets plus light/dark WidgetTheme.
  - **Purpose:** Serializable playback snapshot for widget rendering.
  - **Responsibilities:** Title/artist/info/artwork bytes, flags, repeat mode, custom equals/hashCode/toString.
  - **Inputs:** Written by PlaybackService.buildPlaybackState.
  - **Outputs:** PlaybackState.empty, JSON-serializable state, WidgetTheme.
  - **Dependencies:** kotlinx.serialization, Media3 Player repeat constants.
  - **Consumed by:** PlaybackService, three widgets, PlaybackStateDefinition, WidgetComponent.
  - **Related files:** PlaybackStateDefinition, WidgetExtension.
  - **Notes:** WidgetTheme holds 18 light/dark ARGB colors; artworkData contentEquals-compared.


- `core/appwidgets/state/PlaybackStateDefinition.kt` — GlanceStateDefinition persisting PlaybackState in a DataStore JSON file.
  - **Purpose:** GlanceStateDefinition persisting PlaybackState via DataStore.
  - **Responsibilities:** DataStore delegate, lenient JSON serializer, file location resolution.
  - **Inputs:** Context, fileKey, PlaybackState writes/reads.
  - **Outputs:** playback_state_v1.json in filesDir/datastore.
  - **Dependencies:** androidx.datastore, kotlinx.serialization Json, GlanceStateDefinition.
  - **Consumed by:** SsilencceGlanceWidget, CardWidget, FullWidget, PlaybackService.updateGlanceWidgets.
  - **Related files:** PlaybackState.
  - **Notes:** Throws CorruptionException on serialization/IO errors; default empty state.

- `core/appwidgets/state/WidgetSettings.kt` - Per-widget appearance settings keyed by appWidgetId in Preferences DataStore.
  - **Purpose:** Persist widget theme mode, transparency and cached wallpaper color independently per placed widget.
  - **Responsibilities:** System/AlbumArt/Wallpaper mode mapping, 0-100 clamp, heuristic 0.35 surface and 0.30 scrim alpha floors, wallpaper color cache key, empty-state predicate, load/save/delete keyed by appWidgetId.
  - **Inputs:** Context, appWidgetId, WidgetSettings values.
  - **Outputs:** widget_settings Preferences DataStore entries (theme, transparency, wallpaper color) and pure policy values.
  - **Dependencies:** androidx.datastore.preferences, kotlinx.coroutines Flow.
  - **Consumed by:** SsilencceGlanceWidget, CardWidget, FullWidget, WidgetConfigActivity.
  - **Related files:** WidgetExtension.kt, WallpaperThemeProvider.kt, WidgetConfigActivity.kt.
  - **Notes:** No Room/dependency/version change; keys are widget_theme_<id>, widget_transparency_<id> and widget_wallpaper_color_<id>; all are removed by each widget's onDelete.


- `core/appwidgets/WallpaperThemeProvider.kt` - Wallpaper-derived widget palette with per-widget cache.
  - **Purpose:** Derive and cache a WidgetTheme source color from the current home-screen wallpaper.
  - **Responsibilities:** On API 27+ read the public WallpaperManager.getWallpaperColors(FLAG_SYSTEM) primary/secondary/tertiary color; on API 26 or when colors are unavailable return null. Build the Fidelity WidgetTheme, persist the ARGB color through WidgetSettingsStore, and fall back safely on any non-cancellation failure.
  - **Inputs:** Context, appWidgetId, cached wallpaperColor.
  - **Outputs:** WidgetTheme? (null when wallpaper colors are unavailable) and the widget_wallpaper_color_<id> DataStore entry.
  - **Dependencies:** WallpaperManager, WallpaperColors, WidgetSettingsStore, WidgetTheme/dynamicColorSchemes.
  - **Consumed by:** SsilencceGlanceWidget, CardWidget, FullWidget, WidgetConfigActivity.
  - **Related files:** WidgetSettings.kt, WidgetExtension.kt, WidgetConfigActivity.kt.
  - **Notes:** No restricted wallpaper permission, dependency or Room change; derivation runs off the main thread, uses only public WallpaperColors, rethrows CancellationException and logs other failures.


- `core/audio/AudioOutputObserver.kt` — BroadcastReceiver exposing Flows for current audio device, system volume and bit-perfect state.
  - **Purpose:** Observe audio output device, volume and bit-perfect state.
  - **Responsibilities:** BroadcastReceiver + AudioDeviceCallback + MediaRouter callback, StateFlows, opaque output identity, USB bit-perfect config (API 34+).
  - **Inputs:** AudioManager, MediaRouter route, volume intents, playback sample rate/channels.
  - **Outputs:** audioDevice (with optional hashed deviceKey), systemVolumeState, bitPerfectState, availableBitPerfectDevices Flows.
  - **Dependencies:** AudioManager, MediaRouter, AudioDevice/AudioDeviceType/AudioDeviceKeyPolicy, BitPerfectState, VolumeState, SystemMediaControlResolver.
  - **Consumed by:** MainModule, PlaybackService, EqualizerManager, EqualizerViewModel.
  - **Related files:** core/model/audiodevice/*, util/oem/SystemMediaControlResolver.
  - **Notes:** setPreferredMixerAttributes USB-only; headset/volume actions; export receiver. Route selection/changes trigger a device refresh; product-like devices get an opaque key from route id/address/product fallback.


- `core/audio/AutoEqTxtParser.kt` — Parser for AutoEq text files (`GraphicEQ: ...`) producing an AutoEqProfile.
  - **Purpose:** Parse AutoEq text files into AutoEqProfile.
  - **Responsibilities:** Find GraphicEQ line, split frequency/gain pairs, derive preset name from Uri.
  - **Inputs:** Context, content Uri, InputStream via ContentResolver.
  - **Outputs:** AutoEqProfile (name, points, preamp) or null.
  - **Dependencies:** AutoEqProfile/AutoEqPoint models, OpenableColumns.
  - **Consumed by:** EqualizerViewModel (AutoEq TXT import).
  - **Related files:** core/model/equalizer/autoeq/AutoEqProfile, EqualizerScreen.
  - **Notes:** Skips malformed bands; fallback name "AutoEq Preset"; strips .txt suffix.


- `core/model/CategoryInfo.kt` — Parcelable navigation category mapping (Home, Songs, Albums, Artists, Album artists, Composers, Queues...) with visibility (max 5).
  - **Purpose:** Model visible library navigation categories and their ordering.
  - **Responsibilities:** Define eleven Category entries with nav-id, title, icon; track visibility; cap at five visible; expose `Composers.isAvailable` on API 30+.
  - **Inputs:** User reorder/visibility from Preferences.libraryCategories.
  - **Outputs:** Visible category list driving MainActivity navigation.
  - **Dependencies:** android.os.Build, R.id, R.string, R.drawable, kotlinx Parcelable/Serializable.
  - **Consumed by:** Preferences, CategoryInfoAdapter, CategoriesPreferenceDialog, MainActivity, LibraryProvider, AuroraNavigation.
  - **Related files:** ui/adapters/preference/CategoryInfoAdapter.kt, ui/component/preferences/dialog/CategoriesPreferenceDialog.kt.
  - **Notes:** MAX_VISIBLE_CATEGORIES enforced by adapter check logic; serialized to SharedPreferences JSON. AlbumArtists/Composers/Queues are appended after the original eight; Queues is last and starts invisible because the default five-tab list fills first; unavailable Composers is filtered before the list reaches navigation. AuroraNavigation filters Queues from the drawer because Aurora already has a fixed Queue destination.


- `core/model/ComposerNamePolicy.kt` — Pure composer-name normalization, matching and stable IDs.
  - **Purpose:** Group MediaStore composer strings case-insensitively without Android dependencies.
  - **Responsibilities:** Trim/collapse whitespace, normalize keys, case-insensitive matches and stable synthetic IDs.
  - **Inputs:** Raw composer names.
  - **Outputs:** Display name, comparison key, stable id.
  - **Dependencies:** java.util.Locale only.
  - **Consumed by:** RealArtistRepository; compiled probe.
  - **Related files:** data/model/Artist.kt, data/local/repository/ArtistRepository.kt.
  - **Notes:** Blank/null names return null/-1; multiple composers are treated as one literal value.


- `core/model/GenreSubcategory.kt` — Pure genre detail subcategory enum (Songs, Albums, Artists).
  - **Purpose:** Identify the visible genre detail collection without Android dependencies.
  - **Responsibilities:** Enumerate Songs/Albums/Artists and restore a saved enum name with Songs as fallback.
  - **Inputs:** Saved-state name.
  - **Outputs:** GenreSubcategory.
  - **Dependencies:** None.
  - **Consumed by:** GenreDetailFragment; compiled probe.
  - **Related files:** data/model/GenreContent.kt, ui/screen/library/genres/GenreDetailFragment.kt.
  - **Notes:** Unknown or null saved names fall back to Songs.


- `core/model/Cutoff.kt` — (description, interval ms) pair for library cutoff periods (e.g. recently added).
  - **Purpose:** Pair a human-readable cutoff label with its millisecond interval.
  - **Responsibilities:** Carry description string and interval for history/last-added windows.
  - **Inputs:** Interval from Preferences cutoff settings.
  - **Outputs:** Cutoff consumed by history queries.
  - **Dependencies:** None.
  - **Consumed by:** Preferences (getLastAddedCutoff, getHistoryCutoff).
  - **Related files:** core/sort/SortMode.kt, data/local/room/HistoryDao.kt, extensions/utilities/CalendarExt.kt.
  - **Notes:** Pure data holder, no logic.


- `core/model/GridViewType.kt` — Grid view type enum (Normal, Card, ColoredCard, Circle, Image).
  - **Purpose:** Enumerate list/grid presentation styles per library screen.
  - **Responsibilities:** Map each type to menu item id, layout resource, margin; resolve margin by layout.
  - **Inputs:** Layout resource id.
  - **Outputs:** Grid margin or layout for adapter.
  - **Dependencies:** R.id, R.layout, R.
  - **Consumed by:** AbsRecyclerViewCustomGridSizeFragment, SongListFragment, ArtistListFragment, YearsListFragment, GenresListFragment.
  - **Related files:** ui/component/base/AbsRecyclerViewCustomGridSizeFragment.kt, ui/adapters/song/SongAdapter.kt.
  - **Notes:** getMarginForLayout used by recycler span calculations.


- `core/model/LibraryMargin.kt` — Computes total library margin from base margin, extra spacing and window insets.
  - **Purpose:** Aggregate bottom margin for library screens (mini-player/FAB).
  - **Responsibilities:** Combine margin, additionalSpace, bottomInsets and optional navigationMargin; expose legacy totalMargin, totalMarginWithNavigation and getWithSpace.
  - **Inputs:** Base margin, optional space, inset values and shell-owned navigation clearance.
  - **Outputs:** Total pixel margin for layout padding.
  - **Dependencies:** None.
  - **Consumed by:** LibraryViewModel (fabMargin, miniPlayerMargin), LyricsScreen, EqualizerScreen.
  - **Related files:** ui/screen/library/LibraryViewModel.kt, ui/component/base/AbsRecyclerViewFragment.kt.
  - **Notes:** Insets and navigation can be included independently; legacy list hosts retain their separate navigation margins.


- `core/model/MediaEvent.kt` — Media event enum (content/favorite changed, playback restored/started).
  - **Purpose:** Signal media-collection and playback lifecycle changes.
  - **Responsibilities:** Define four event kinds for ViewModel/UI notification.
  - **Inputs:** — (pure enum).
  - **Outputs:** Events submitted to PlayerViewModel.
  - **Dependencies:** None.
  - **Consumed by:** MainActivity, AbsMainActivityFragment, PlayerViewModel.
  - **Related files:** ui/screen/player/PlayerViewModel.kt, ui/component/base/AbsMainActivityFragment.kt.
  - **Notes:** submitEvent routes to LiveData/Flow consumers.


- `core/model/NotPlayedDaysPolicy.kt` — Pure rules for the Not Recently Played threshold.
  - **Purpose:** Keep the custom N-day window and its default-history fallback testable without Android dependencies.
  - **Responsibilities:** Define the 0 = use-history sentinel, clamp/validate 1–3650 days, parse dialog input and compute cutoff millis.
  - **Inputs:** Stored day count, input text, current time, history cutoff millis.
  - **Outputs:** Normalized days, parsed days, cutoff millis, effective cutoff millis.
  - **Dependencies:** None.
  - **Consumed by:** Preferences, NotPlayedDaysDialogFragment; compiled probe.
  - **Related files:** util/Preferences.kt, data/local/repository/SmartRepository.kt, ui/dialogs/NotPlayedDaysDialog.kt.
  - **Notes:** `normalizeDays(0)` keeps the history interval; positive values clamp to 3650; fixed 24-hour arithmetic is intentionally separate from calendar-preset cutoffs.


- `core/model/PaletteColor.kt` — Extracted cover colors (background, primary, text colors) with fallback error color.
  - **Purpose:** Hold color set extracted from album artwork.
  - **Responsibilities:** Store four ARGB colors; build errorColor fallback from PlaceholderDrawable.
  - **Inputs:** Bitmap-derived colors via PaletteProcessor.
  - **Outputs:** PaletteColor for adapters and player theming.
  - **Dependencies:** PlaceholderDrawable, resolveColor, withAlpha extensions.
  - **Consumed by:** PaletteProcessor, AdapterExt, PlayerColorScheme, PlaybackService.
  - **Related files:** core/palette/PaletteProcessor.kt, ui/adapters/MediaEntryViewHolder (via AdapterExt).
  - **Notes:** errorColor used when extraction fails.


- `core/model/WebSearchEngine.kt` — Search engine enum (Google, Last.fm, Wikipedia, YouTube) with localized URLs.
  - **Purpose:** Build web-search URLs for songs.
  - **Responsibilities:** Define four engines; encode query; localize Last.fm/Wikipedia by locale.
  - **Inputs:** Query string, Locale.
  - **Outputs:** Encoded search URL.
  - **Dependencies:** R.string, URLEncoder.
  - **Consumed by:** WebSearchDialog, DataExt (Song.searchQuery).
  - **Related files:** ui/dialogs/WebSearchDialog.kt, extensions/media/DataExt.kt.
  - **Notes:** Falls back to raw query when encoding fails.


- `core/model/about/AboutItemData.kt` — DTO for "About" screen items (icon, title, summary, markdown, onClick).
  - **Purpose:** Model a single row of the About screen.
  - **Responsibilities:** Carry composable icon, title, summary, optional markdown content, click handler.
  - **Inputs:** Composable icon and click lambda.
  - **Outputs:** Row data for AboutScreen lists.
  - **Dependencies:** androidx.compose.runtime.Composable.
  - **Consumed by:** AboutScreen (AboutItemData groups).
  - **Related files:** ui/screen/about/AboutScreen.kt, ui/screen/about/AboutFragment.kt.
  - **Notes:** markdown enables license/detail dialogs.


- `core/model/about/Contribution.kt` — Serializable contributor model loaded from a JSON asset.
  - **Purpose:** Represent a translator/contributor entry.
  - **Responsibilities:** Parse JSON fields; resolve asset image URLs; loadContributions from assets.
  - **Inputs:** JSON asset name (e.g. translators.json).
  - **Outputs:** List of contributions for AboutScreen.
  - **Dependencies:** kotlinx.serialization.Json, readStringFromAsset.
  - **Consumed by:** AboutScreen.
  - **Related files:** ui/screen/about/AboutScreen.kt, assets/translators.json.
  - **Notes:** Returns empty list on parse failure; ignoreUnknownKeys.


- `core/model/action/FolderAction.kt` — Folder action enum (Play, ShufflePlay, QueueNext, Queue, AddToPlaylist, Delete).
  - **Purpose:** Enumerate recursive folder context actions.
  - **Responsibilities:** Map actions to menu ids and preference values; isPresent helper.
  - **Inputs:** Menu item id.
  - **Outputs:** Boolean membership check for selected actions.
  - **Dependencies:** R.id.
  - **Consumed by:** Preferences (recursiveFolderActions), FoldersListFragment.
  - **Related files:** ui/screen/library/folders/FoldersListFragment.kt, util/Preferences.kt.
  - **Notes:** Persisted as string set of preferenceValue.


- `core/model/action/NowPlayingAction.kt` — Now Playing action enum (Lyrics, AddToPlaylist, SleepTimer, SeekForward, etc.).
  - **Purpose:** Catalog all player quick actions with icon/title.
  - **Responsibilities:** Define sixteen actions; attach drawable and string resources.
  - **Inputs:** — (pure enum).
  - **Outputs:** Actions bound to views and gesture preferences.
  - **Dependencies:** R.drawable, R.string.
  - **Consumed by:** AbsPlayerFragment, AbsPlayerControlsFragment, player style fragments, ActionOnCoverPreferenceDialog, Preferences.
  - **Related files:** ui/component/base/AbsPlayerFragment.kt, ui/component/preferences/dialog/ActionOnCoverPreferenceDialog.kt.
  - **Notes:** Nothing acts as placeholder for unset gestures.



- `core/model/action/HeadsetButtonAction.kt` - Transport-only headset action enum.
  - **Purpose:** Restrict headset mappings to safe playback/seek operations.
  - **Responsibilities:** Define PlayPause, Next, Previous, SeekForward, SeekBackward and Nothing.
  - **Inputs:** - (pure enum).
  - **Outputs:** Persisted action names and service actions.
  - **Dependencies:** None.
  - **Consumed by:** HeadsetButtonConfig/Policy, PlaybackService, Preferences, HeadsetButtonMappingDialog.
  - **Related files:** playback/HeadsetButtonPolicy.kt, ui/component/preferences/dialog/HeadsetButtonMappingDialog.kt, util/Preferences.kt.
  - **Notes:** Deliberately separate from NowPlayingAction; no destructive or UI-opening actions.

- `core/model/action/QueueClearingBehavior.kt` — Enum for queue-clearing behavior (remove all vs. keep current).
  - **Purpose:** Choose what clear-queue removes.
  - **Responsibilities:** Define two behaviors with icon, title, summary resources.
  - **Inputs:** — (pure enum).
  - **Outputs:** Behavior passed to PlayerViewModel.clearQueue.
  - **Dependencies:** R.drawable, R.string.
  - **Consumed by:** Preferences, PlayerViewModel.clearQueue, ClearQueueActionPreferenceDialog.
  - **Related files:** ui/component/preferences/dialog/ClearQueueActionPreferenceDialog.kt, ui/screen/player/PlayerViewModel.kt.
  - **Notes:** Stored by ordinal preference.


- `core/model/action/SongClickBehavior.kt` — Enum for tap-on-song behavior (play single/whole list, queue next/end).
  - **Purpose:** Configure what tapping a song does.
  - **Responsibilities:** Define four behaviors with resources and isAbleToPlay flag.
  - **Inputs:** — (pure enum).
  - **Outputs:** Behavior consumed by queue-opening logic.
  - **Dependencies:** R.drawable, R.string.
  - **Consumed by:** Preferences, SongAdapter, FileAdapter, SearchViewModel, PlayerViewModel, SongClickActionPreferenceDialog.
  - **Related files:** ui/component/preferences/dialog/SongClickActionPreferenceDialog.kt, ui/adapters/song/SongAdapter.kt.
  - **Notes:** isAbleToPlay distinguishes playback from queueing.


- `core/model/audiodevice/AudioDevice.kt` - Output device model resolving displayable name by type and optional opaque identity.
  - **Purpose:** Represent the current audio output device.
  - **Responsibilities:** Resolve device name from product name, manufacturer/model, or type label; expose optional deviceKey.
  - **Inputs:** AudioDeviceType, product name, optional hashed device key.
  - **Outputs:** Display name string, opaque key.
  - **Dependencies:** Context, Build.
  - **Consumed by:** AudioOutputObserver, EqualizerManager, EqualizerScreen, SoundSettingsScreen.
  - **Related files:** core/audio/AudioOutputObserver.kt, playback/equalizer/EqualizerManager.kt, AudioDeviceKeyPolicy.kt.
  - **Notes:** UnknownDevice constant for unset state; key is null for type-only devices.

- `core/model/audiodevice/AudioDeviceKeyPolicy.kt` - Opaque identity hashing for output devices.
  - **Purpose:** Persist exact output identity without storing raw Bluetooth addresses/product names.
  - **Responsibilities:** SHA-256 hash of `type:identity`, type-prefixed hex key, null for blank identity.
  - **Inputs:** Type name, raw OS identity.
  - **Outputs:** Opaque key or null.
  - **Dependencies:** Java MessageDigest, Kotlin standard library.
  - **Consumed by:** AudioOutputObserver, compiled association probe.
  - **Related files:** AudioDevice.kt, EqProfile.kt, EqualizerManager.kt.
  - **Notes:** Raw identity is never stored in profiles or displayed.


- `core/model/audiodevice/AudioDeviceType.kt` — Device type enum (BluetoothA2DP, USB, HDMI, Speaker...) with mappings from AudioDeviceInfo/MediaRouter.
  - **Purpose:** Classify audio output hardware.
  - **Responsibilities:** Map Android device types to enum; extension functions for AudioDeviceInfo/MediaRouter.
  - **Inputs:** AudioDeviceInfo or MediaRouter.RouteInfo.
  - **Outputs:** Matching AudioDeviceType or Unknown.
  - **Dependencies:** AudioDeviceInfo, MediaRouter, R.
  - **Consumed by:** AudioDevice, AudioOutputObserver, EqualizerManager.
  - **Related files:** core/audio/AudioOutputObserver.kt, playback/equalizer/EqualizerManager.kt.
  - **Notes:** isThisDeviceOutput covers BuiltinSpeaker/Unknown.


- `core/model/audiodevice/BitPerfectState.kt` — Sealed bit-perfect state (Inactive/Active) with readable encoding/sample-rate labels.
  - **Purpose:** Model bit-perfect audio session status.
  - **Responsibilities:** Track active/inactive plus fixed volume; format encoding and sample-rate labels.
  - **Inputs:** Device name, sample rate, channel count, AudioFormat encoding.
  - **Outputs:** Human-readable state labels.
  - **Dependencies:** AudioFormat, Locale.
  - **Consumed by:** AudioOutputObserver, SoundSettingsScreen, EqualizerManager.
  - **Related files:** core/audio/AudioOutputObserver.kt, ui/screen/sound/SoundSettingsScreen.kt.
  - **Notes:** Active labels like "PCM 24-bit" and "48 kHz".


- `core/model/equalizer/EqBand.kt` — Immutable EQ band (index, value, range, frequency) with readable frequency.
  - **Purpose:** Represent one graphic-EQ band.
  - **Responsibilities:** Hold band index, level, range, frequency; format readable frequency.
  - **Inputs:** Band properties from engine capabilities.
  - **Outputs:** EqBand list for slider UI.
  - **Dependencies:** Compose @Immutable, Locale.
  - **Consumed by:** EqBandCapabilities.getBands, EqualizerScreen.
  - **Related files:** core/model/equalizer/EqBandCapabilities.kt, ui/screen/equalizer/EqualizerScreen.kt.
  - **Notes:** FREQUENCY_FACTOR = 1000 for kHz conversion.


- `core/model/equalizer/EqBandCapabilities.kt` — EQ engine capabilities (dB range, band configs, max bands) building EqBand lists.
  - **Purpose:** Describe engine-supported band layouts.
  - **Responsibilities:** Hold band range/configurations; filter by pro mode; build EqBand lists from EqProfile.
  - **Inputs:** EqProfile, band count, pro-mode flag.
  - **Outputs:** Available band counts, frequencies, EqBand list.
  - **Dependencies:** Compose @Immutable, EqBand, EqProfile.
  - **Consumed by:** BasicEQEngine, DynamicsProcessingEngine, EqualizerManager.
  - **Related files:** playback/equalizer/engine/BasicEQEngine.kt, playback/equalizer/engine/DynamicsProcessingEngine.kt.
  - **Notes:** BandConfiguration compares int arrays by content.


- `core/model/equalizer/EqEngineMode.kt` — EQ engine enum (Basic with Android UUID, DynamicsProcessing custom UUID) with SDK auto-selection.
  - **Purpose:** Select the underlying EQ implementation.
  - **Responsibilities:** Define engine UUIDs, titles, band counts, pro support; Auto selection by SDK.
  - **Inputs:** — (enum + SDK check).
  - **Outputs:** Engine mode for EqualizerManager.
  - **Dependencies:** Build, UUID, R.
  - **Consumed by:** EqualizerManager, EqualizerViewModel, EqualizerScreen, EqState.
  - **Related files:** playback/equalizer/EqualizerManager.kt, playback/equalizer/engine/EQEngine.kt.
  - **Notes:** DynamicsProcessing requires API 28+.


- `core/model/equalizer/EqProfile.kt` - Serializable EQ profile (name, band levels, type/device associations, custom/AutoEq).
  - **Purpose:** Persist a named EQ preset.
  - **Responsibilities:** Store levels, broad type associations and optional exact device keys; validate; localized name/description; custom equals/hashCode.
  - **Inputs:** Name, FloatArray levels, device associations, associated device keys.
  - **Outputs:** JSON-serializable profile for export/import; missing `device_keys` defaults empty.
  - **Dependencies:** kotlinx.serialization, AudioDeviceType, R.
  - **Consumed by:** EqualizerManager, EqualizerViewModel, EqualizerScreen.
  - **Related files:** playback/equalizer/EqualizerManager.kt, ui/screen/equalizer/EqualizerViewModel.kt.
  - **Notes:** CUSTOM_PRESET_NAME renders localized "Custom". Old profiles/exports remain compatible.

- `core/model/equalizer/EqOutputAssociationPolicy.kt` - Pure exact-device/type association resolver.
  - **Purpose:** Keep output-profile matching and overlap normalization deterministic and JVM-probeable.
  - **Responsibilities:** Exact device key wins, type fallback, no-match null; overlap removal for save/edit.
  - **Inputs:** Profile name + type-name/device-key sets, current type/key.
  - **Outputs:** Matching profile name or null; normalized association.
  - **Dependencies:** Kotlin standard library only.
  - **Consumed by:** EqualizerManager, compiled association probe.
  - **Related files:** EqProfile.kt, EqualizerManager.kt, AudioDevice.kt.
  - **Notes:** Android-free; no match means the current profile is preserved.


- `core/model/equalizer/EqSession.kt` — EQ session identity (Internal/External, id, active).
  - **Purpose:** Identify which audio session the EQ applies to.
  - **Responsibilities:** Carry session type, id, active flag.
  - **Inputs:** — (data holder).
  - **Outputs:** Session state for EqualizerManager.
  - **Dependencies:** None.
  - **Consumed by:** EqualizerManager (setSession).
  - **Related files:** playback/equalizer/EqualizerManager.kt, playback/equalizer/engine/EQEngine.kt.
  - **Notes:** Internal uses NO_SESSION_ID when disabled.


- `core/model/equalizer/EqState.kt` — Immutable EQ processing states (support, bands, mode, pro mode) plus bass/virtualizer/limiter/compressor states.
  - **Purpose:** Immutable snapshot of all EQ effect states.
  - **Responsibilities:** Define EqState plus BassBoost/Virtualizer/LoudnessGain/Limiter/Compressor states; disable reasons.
  - **Inputs:** Engine-derived support/ranges.
  - **Outputs:** UI state flows for EqualizerScreen.
  - **Dependencies:** Compose @Immutable, EqEngineMode, R.
  - **Consumed by:** EqualizerManager, EqualizerViewModel, EqualizerScreen.
  - **Related files:** playback/equalizer/EqualizerManager.kt, ui/screen/equalizer/EqualizerViewModel.kt.
  - **Notes:** Unspecified constants for default values.


- `core/model/equalizer/SoundState.kt` — Immutable Balance, Tempo (speed/pitch), system volume and ReplayGain states.
  - **Purpose:** Snapshot non-EQ sound processing states.
  - **Responsibilities:** Compute left/right from balance; actual pitch; volume percent; expose ReplayGain modes.
  - **Inputs:** Raw effect parameters.
  - **Outputs:** Derived values for sliders and processors.
  - **Dependencies:** Compose @Immutable, ReplayGainMode.
  - **Consumed by:** EqualizerManager, AudioOutputObserver, EqualizerScreen, PlaybackService.
  - **Related files:** playback/equalizer/EqualizerManager.kt, playback/processor/ReplayGainAudioProcessor.kt.
  - **Notes:** ReplayGainState.availableModes drives UI.


- `core/model/equalizer/autoeq/AutoEqProfile.kt` — AutoEq profile (frequency/gain points + preamp) with logarithmic gain interpolation.
  - **Purpose:** Convert AutoEq frequency/gain curves to EQ band levels.
  - **Responsibilities:** Store AutoEqPoint list and preamp; interpolate gains logarithmically at target frequencies.
  - **Inputs:** Target frequencies, valid gain range.
  - **Outputs:** FloatArray of band gains.
  - **Dependencies:** kotlinx.serialization, kotlin.math.log10.
  - **Consumed by:** AutoEqTxtParser, EqualizerViewModel, EqualizerManager, EqualizerScreen.
  - **Related files:** core/audio/AutoEqTxtParser.kt, ui/screen/equalizer/EqualizerViewModel.kt.
  - **Notes:** Clamps interpolated gain plus preamp.


- `core/model/filesystem/FileSystemItem.kt` — Common interface for filesystem items (id, name, path, dates).
  - **Purpose:** Unify files, folders, and storage volumes.
  - **Responsibilities:** Expose fileId (hash-based), name, path, added/modified dates.
  - **Inputs:** — (interface contract).
  - **Outputs:** Implemented by Song, Folder, StorageDevice, GoUpFileSystemItem.
  - **Dependencies:** None.
  - **Consumed by:** FileAdapter, SpecialRepository, FileSortMode.
  - **Related files:** data/model/Song.kt, data/model/Folder.kt, core/sort/SortMode.kt.
  - **Notes:** fileId derived from path hash for stable identity.


- `core/model/filesystem/FileSystemQuery.kt` — Filesystem query (name, path, children, storage root) with ".." up-navigation.
  - **Purpose:** Represent one level of folder navigation.
  - **Responsibilities:** Sort children, prepend GoUp item, detect flat/storage-root views; isNavigablePath.
  - **Inputs:** Path, children list, sort mode.
  - **Outputs:** Navigable child list.
  - **Dependencies:** StorageUtil, FileSortMode, kotlinx Parcelable.
  - **Consumed by:** SpecialRepository, Repository, FoldersListFragment.
  - **Related files:** data/local/repository/SpecialRepository.kt, ui/screen/library/folders/FoldersListFragment.kt.
  - **Notes:** GoUpFileSystemItem has GO_UP_ID -2.


- `core/model/filesystem/StorageDevice.kt` — Parcelable storage volume wrapper implementing FileSystemItem.
  - **Purpose:** Simplify android.os.storage.StorageVolume.
  - **Responsibilities:** Wrap path/name/icon; provide File; value equality.
  - **Inputs:** Storage volume path and description.
  - **Outputs:** StorageDevice list for explorer.
  - **Dependencies:** kotlinx Parcelable, java.io.File.
  - **Consumed by:** StorageUtil, FileAdapter, FolderChooserDialog, SpecialRepository.
  - **Related files:** util/StorageUtil.kt, ui/adapters/FileAdapter.kt.
  - **Notes:** Dates ignored on parcel (-1).


- `core/model/lyrics/LyricsViewSettings.kt` — Lyrics view settings (player/full mode, centering, karaoke, effects) with preference keys.
  - **Purpose:** Immutable settings for the lyrics UI.
  - **Responsibilities:** Hold mode, alignment, karaoke/syllable flags, effects, text styles, spacing; define preference keys.
  - **Inputs:** User preferences from LyricsViewModel.
  - **Outputs:** Settings consumed by LyricsScreen/LyricsView.
  - **Dependencies:** Compose @Immutable, TextStyle.
  - **Consumed by:** LyricsViewModel, LyricsScreen, LyricsView.
  - **Related files:** ui/screen/lyrics/LyricsViewModel.kt, ui/component/compose/lyrics/LyricsView.kt.
  - **Notes:** Key constants match SharedPreferences names.


- `core/model/lyrics/LyricsViewState.kt` — Synced-lyrics playback state computing current line/word/background and crossfade.
  - **Purpose:** Track active lyric elements during playback.
  - **Responsibilities:** Update position with offset; find line, word, background indices; detect crossfade jumps.
  - **Inputs:** Playback position millis, SyncedLyrics.
  - **Outputs:** Current indices and shouldCrossfade for UI.
  - **Dependencies:** Compose @Stable state, SyncedLyrics.
  - **Consumed by:** LyricsScreen, LyricsView, KaraokeStyleText.
  - **Related files:** ui/screen/lyrics/LyricsScreen.kt, ui/component/compose/lyrics/KaraokeStyleText.kt.
  - **Notes:** Position offset applied from lyrics.offset.


- `core/model/player/MetadataField.kt` — Selectable metadata field (Album, Genre, Year, Bitrate...) reading via MetadataReader.
  - **Purpose:** Configure player/widget extra info lines.
  - **Responsibilities:** Enumerate tag fields; getMetadataValue reads TagLib metadata via MetadataReader.
  - **Inputs:** Song, enabled MetadataField list.
  - **Outputs:** Joined info string or null.
  - **Dependencies:** MetadataReader, Song, kotlinx Parcelable/Serializable.
  - **Consumed by:** Preferences, ExtraInfoAdapter, ExtraInfoPreferenceDialog, PlaybackService.
  - **Related files:** data/local/MetadataReader.kt, ui/adapters/preference/ExtraInfoAdapter.kt.
  - **Notes:** Format/Bitrate/SampleRate derive from audio header.


- `core/model/player/PlayerColorScheme.kt` — Player color scheme with 5 modes (AppTheme, SimpleColor, VibrantColor, MaterialYou, Blur).
  - **Purpose:** Produce cohesive player UI colors.
  - **Responsibilities:** Build schemes from theme, PaletteColor, m3color Monet; blur token; validity tokens.
  - **Inputs:** Context, PaletteColor, mode.
  - **Outputs:** PlayerColorScheme for PlayerTheme.
  - **Dependencies:** m3color Hct/SchemeContent, Preferences, ColorUtils, extensions.
  - **Consumed by:** PlayerViewModel, ui/theme/Theme.kt (PlayerTheme), NowPlayingScreen.
  - **Related files:** ui/theme/Theme.kt, ui/screen/player/PlayerViewModel.kt, extensions/resources/ColorExt.kt.
  - **Notes:** autoColorScheme dispatches per mode; check(mode==colorScheme.mode).


- `core/model/player/PlayerTintTarget.kt` — Re-tint target (view + old/new colors and classification) with builders.
  - **Purpose:** Describe a view needing color re-tint.
  - **Responsibilities:** Hold View, colors, classification flags; extension builders surfaceTintTarget/iconButtonTintTarget/tintTarget.
  - **Inputs:** View, old/new colors.
  - **Outputs:** PlayerTintTarget for animation.
  - **Dependencies:** android.view.View, Color.
  - **Consumed by:** PlayerViewModel, AbsPlayerFragment.
  - **Related files:** ui/screen/player/PlayerViewModel.kt, ui/component/base/AbsPlayerFragment.kt.
  - **Notes:** isForeground/isSurface/isIcon categorize targets.


- `core/model/player/PlayerTransition.kt` — ViewPager transition enum with PageTransformer factory.
  - **Purpose:** Choose cover-pager page transition.
  - **Responsibilities:** Map nine transitions to PageTransformer factories; Simple maps null transformer.
  - **Inputs:** — (enum).
  - **Outputs:** Pair<PageTransformer?, Boolean> for ViewPager setup.
  - **Dependencies:** ViewPager, transform package, R.
  - **Consumed by:** Preferences, NowPlayingScreenPreferenceDialog, AbsSlidingMusicPanelActivity.
  - **Related files:** ui/component/transform/ (9 transformers), ui/component/preferences/dialog/NowPlayingScreenPreferenceDialog.kt.
  - **Notes:** Parallax takes page id; Boolean signals overlay use.


- `core/model/player/ProgressState.kt` — Immutable progress state (position/duration) with remaining times and readable strings.
  - **Purpose:** Snapshot playback progress for UI.
  - **Responsibilities:** Compute remaining time, mayUpdateUI, readable duration strings.
  - **Inputs:** Progress and total millis.
  - **Outputs:** Progress strings for sliders/text.
  - **Dependencies:** Compose @Immutable, asReadableDuration.
  - **Consumed by:** PlayerViewModel, MiniPlayerFragment, widgets.
  - **Related files:** ui/screen/player/PlayerViewModel.kt, extensions/media/MediaExt.kt.
  - **Notes:** Unspecified for initial state.


- `core/model/navigation/AuroraStartDestinationPolicy.kt` — Pure Aurora start/last-page rules for the Queues category.
  - **Purpose:** Keep `nav_queues` out of Aurora start/resume while leaving classic navigation unchanged.
  - **Responsibilities:** Reject the Queues category as an Aurora top-level/last-page destination; choose the first visible non-Queues category with Home fallback.
  - **Inputs:** Destination/category ids, visible category ids, queues id, fallback id.
  - **Outputs:** Allowed destination id / first allowed category id.
  - **Dependencies:** None (pure Kotlin).
  - **Consumed by:** ui/screen/MainActivity.kt.
  - **Related files:** openspec/changes/add-queues-library-category/specs/queues-category/spec.md.
  - **Notes:** Aurora retains its fixed `nav_queue`; classic keeps the Queues category.


- `core/model/shuffle/ShuffleGroupKeyPolicy.kt` — Pure grouping keys for album-artist/composer most-played shuffle.
  - **Purpose:** Keep the new most-played group keys deterministic and JVM-probeable.
  - **Responsibilities:** Normalize album-artist and composer names; fall back to the song artist for a blank album artist; use one unknown group for missing composers.
  - **Inputs:** Song.
  - **Outputs:** Stable group-key strings.
  - **Dependencies:** Song (pure Kotlin only).
  - **Consumed by:** playback/shuffle/ShuffleManager.kt.
  - **Related files:** core/model/shuffle/SpecialShuffleMode.kt, playback/shuffle/ShuffleManager.kt.
  - **Notes:** Existing MostPlayedArtists/Albums keys are unchanged.


- `core/model/shuffle/ShuffleModeAvailabilityPolicy.kt` - Pure platform gate for composer-dependent shuffle modes.
  - **Purpose:** Keep composer-dependent modes out of the shuffle UI and manager below Android 11/API 30.
  - **Responsibilities:** SDK predicate (`COMPOSER_MIN_SDK = 30`) and a generic availability filter; album-artist modes remain available.
  - **Inputs:** `composerDependent` flag, SDK int, items and predicate.
  - **Outputs:** Availability boolean / filtered list.
  - **Dependencies:** None (pure Kotlin).
  - **Consumed by:** `ui/screen/other/ShuffleModeBottomSheet.kt`, `playback/shuffle/ShuffleManager.kt`.
  - **Related files:** `core/model/shuffle/SpecialShuffleMode.kt`, `core/model/shuffle/ShuffleGroupKeyPolicy.kt`.
  - **Notes:** `COMPOSER_MIN_SDK = 30` matches `CategoryInfo.Category.Composers`; a stale composer mode falls back to a plain shuffle.


- `core/model/shuffle/GroupShuffleMode.kt` — Group shuffle mode enum (ByGroup, BySong, FullRandom).
  - **Purpose:** Choose how album/artist shuffle groups.
  - **Responsibilities:** Define three grouping strategies.
  - **Inputs:** — (pure enum).
  - **Outputs:** Mode for ShuffleManager.groupShuffle.
  - **Dependencies:** None.
  - **Consumed by:** Preferences, ShuffleManager.
  - **Related files:** playback/shuffle/ShuffleManager.kt, util/Preferences.kt.
  - **Notes:** Stored per album/artist shuffle preference.


- `core/model/shuffle/OpenShuffleMode.kt` — Enum for shuffle-on-open behavior (On, Off, Remember).
  - **Purpose:** Decide shuffle state when opening a queue.
  - **Responsibilities:** Define three behaviors.
  - **Inputs:** — (pure enum).
  - **Outputs:** Mode passed to PlayerViewModel.openQueue.
  - **Dependencies:** None.
  - **Consumed by:** AlbumAdapter, MenuItemClickExt, MainActivity, PlayerViewModel.
  - **Related files:** ui/screen/player/PlayerViewModel.kt, ui/component/menu/MenuItemClickExt.kt.
  - **Notes:** Remember retains previous shuffle state.


- `core/model/shuffle/ShuffleOperationState.kt` — Special shuffle operation state (mode + Idle/InProgress).
  - **Purpose:** Track in-flight special shuffle operations.
  - **Responsibilities:** Hold mode and status; isIdle when both unset/idle.
  - **Inputs:** SpecialShuffleMode, Status.
  - **Outputs:** State for shuffle UI feedback.
  - **Dependencies:** SpecialShuffleMode.
  - **Consumed by:** ShuffleManager, PlayerViewModel, ShuffleModeBottomSheet.
  - **Related files:** playback/shuffle/ShuffleManager.kt, ui/screen/other/ShuffleModeBottomSheet.kt.
  - **Notes:** Status enum Idle/InProgress.


- `core/model/shuffle/SpecialShuffleMode.kt` — Special shuffle mode enum (PureRandom, MostPlayed, Favorites, ForgottenSongs, RecentlyAdded, Combined...).
  - **Purpose:** Enumerate smart shuffle strategies.
  - **Responsibilities:** Define ten modes with icon/title/description resources, including MostPlayedAlbumArtists and MostPlayedComposers.
  - **Inputs:** — (pure enum).
  - **Outputs:** Mode for ShuffleManager.applySmartShuffle.
  - **Dependencies:** R.drawable, R.string.
  - **Consumed by:** ShuffleManager, ShuffleModeBottomSheet, ShuffleModeFragment.
  - **Related files:** playback/shuffle/ShuffleManager.kt, ui/screen/other/ShuffleModeBottomSheet.kt.
  - **Notes:** Includes MostPlayedArtists/MostPlayedAlbums plus MostPlayedAlbumArtists/MostPlayedComposers; the Compose bottom sheet filters composer-dependent entries below API 30 and keeps album-artist mode.


- `core/model/sort/SortItem.kt` — Sort menu items (KeySortItem with all keys, DescendingItem).
  - **Purpose:** Model sort menu entries.
  - **Responsibilities:** Define KeySortItem objects per SortKey (including Path) plus DescendingItem; group ids.
  - **Inputs:** — (menu model).
  - **Outputs:** SortItem lists built into SortMode menus.
  - **Dependencies:** R.id, R.string, SortKey.
  - **Consumed by:** SortMode (all subclasses), SimpleSongAdapter.
  - **Related files:** core/sort/SortMode.kt, ui/adapters/song/SimpleSongAdapter.kt.
  - **Notes:** Group 0 keys, group 1 descending.


- `core/model/sort/SortKey.kt` — Sort key enum with persistent preference values.
  - **Purpose:** Stable identifiers for sortable fields.
  - **Responsibilities:** Map keys to SharedPreferences string values.
  - **Inputs:** — (pure enum).
  - **Outputs:** SortKey for persistence and ordering.
  - **Dependencies:** None.
  - **Consumed by:** SortMode (getSortKey, sorting), Preferences.
  - **Related files:** core/sort/SortMode.kt, util/Preferences.kt.
  - **Notes:** Values like "az_key" kept for backward compatibility; `path_key` orders by the full file path.


- `core/model/list/SongListDisplay.kt` — Pure song-list title mode and date-suffix display policy.
  - **Purpose:** Keep list title/date display rules independent of Android and of sorting.
  - **Responsibilities:** Resolve FollowSort/Title/FileName titles; append a date-only suffix to existing secondary text.
  - **Inputs:** Song, active SortKey, SongTitleMode, base text, formatted date.
  - **Outputs:** Display title string; combined secondary text.
  - **Dependencies:** Song, SortKey (pure Kotlin only).
  - **Consumed by:** ui/adapters/song/SongAdapter.kt, util/Preferences.kt.
  - **Related files:** core/model/sort/SortKey.kt, util/Preferences.kt, ui/adapters/song/SongAdapter.kt.
  - **Notes:** FollowSort preserves the historical filename-on-File-name behavior; the date suffix is only used where a secondary text view exists.


- `core/model/task/Event.kt` — Single-consumable event wrapper (getContentIfNotConsumed/peekContent) for UI.
  - **Purpose:** One-shot UI events avoiding replay.
  - **Responsibilities:** Guard content consumption; peek without consuming.
  - **Inputs:** Arbitrary event content.
  - **Outputs:** Content once, then null.
  - **Dependencies:** None.
  - **Consumed by:** ObserveAsEvent, ViewModels emitting events.
  - **Related files:** ui/component/compose/ObserveAsEvent.kt.
  - **Notes:** Classic Android single-event pattern.


- `core/model/task/Result.kt` — Sealed generic network result type (Success/Error/Loading).
  - **Purpose:** Model async operation outcomes.
  - **Responsibilities:** Define Success with data, Error with exception, Loading object.
  - **Inputs:** — (type wrapper).
  - **Outputs:** Result<T> for ViewModel state.
  - **Dependencies:** None.
  - **Consumed by:** NetworkRepository, repositories, ViewModels.
  - **Related files:** data/local/repository/NetworkRepository.kt.
  - **Notes:** Error carries Exception only.


- `core/model/tag/TagEditField.kt` — The 17 text fields exposed by the batch tag editor with their `MetadataReader` keys, labels and input kinds.
  - **Purpose:** One source of truth for batch field identity and UI labels.
  - **Responsibilities:** Map TITLE/ALBUM/.../COMMENT to existing `MetadataReader` constants; retain CONDUCTOR->PRODUCER and PUBLISHER->COPYRIGHT; no cover field.
  - **Inputs:** None.
  - **Outputs:** Enum values consumed by TagEditPlan, TagWriteVerifier, BatchTagEditorViewModel/Screen.
  - **Dependencies:** MetadataReader constants, Android string resources.
  - **Consumed by:** BatchTagEditorViewModel, BatchTagEditorScreen, TagWriteVerifier.
  - **Related files:** TagEditPlan.kt, TagEditReport.kt, MetadataReader.kt.
  - **Notes:** Legacy field semantics are intentionally retained; remapping is a separate change.

- `core/model/tag/TagEditPlan.kt` — Explicit Keep/Set/Clear batch operations converted to a property override map.
  - **Purpose:** Express per-field intent without blank-means-clear.
  - **Responsibilities:** Default Keep; validate non-blank Set; emit only Set/Clear overrides, trimming Set values; expose changed fields. Also defines `BatchTagEditPlan` with sparse per-song overrides, where a per-song Keep overrides a common Set.
  - **Inputs:** Map<TagEditField, FieldOperation>; BatchTagEditPlan adds a per-song override map.
  - **Outputs:** Property overrides for MetadataWriter, validity and changed-field metadata; effective per-song plans.
  - **Dependencies:** TagEditField.
  - **Consumed by:** BatchTagEditorViewModel, MetadataWriter.writeBatch, TagWriteVerifier.
  - **Related files:** TagEditField.kt, TagEditReport.kt, MetadataWriter.kt.
  - **Notes:** Keep never reaches the writer override, so current file values survive. Per-song overrides are sparse and validated before write.

- `core/model/tag/TagEditReport.kt` — Per-file/per-field batch write result models.
  - **Purpose:** Carry honest read-back results to the UI.
  - **Responsibilities:** FieldWriteStatus (Kept/Written/Cleared/Unsupported/Mismatch/KeptChanged/NotAttempted), FileWriteStatus (Succeeded/Partial/Failed/Skipped), file reports and summary counts.
  - **Inputs:** Results from TagWriteVerifier and MetadataWriter.
  - **Outputs:** BatchWriteReport consumed by BatchTagEditorViewModel/Screen.
  - **Dependencies:** None.
  - **Consumed by:** BatchTagEditorViewModel, BatchTagEditorScreen, MetadataWriter.
  - **Related files:** TagEditPlan.kt, TagWriteVerifier.kt.
  - **Notes:** A batch is full success only when every requested field verifies and no file is skipped/partial.


- `core/model/theme/AppTheme.kt` — App theme (id, style resource, dynamic colors, seed color) built from preferences.
  - **Purpose:** Resolve the current app theme configuration.
  - **Responsibilities:** Define Light/Dark/Black/FollowSystem modes; createAppTheme from Preferences and dynamic-color availability.
  - **Inputs:** Context, Preferences.
  - **Outputs:** AppTheme for activity theming.
  - **Dependencies:** GeneralTheme, Preferences, DynamicColors, ContextThemeWrapper.
  - **Consumed by:** ContextExt.createAppTheme, AbsThemeActivity, ui/theme/Theme.kt.
  - **Related files:** extensions/ContextExt.kt, ui/component/base/AbsThemeActivity.kt.
  - **Notes:** isBlackTheme checks GeneralTheme.BLACK.


- `core/model/theme/ColorSchemes.kt` — Light/dark Material 3 ColorScheme pair.
  - **Purpose:** Bundle Compose color schemes.
  - **Responsibilities:** Hold light and dark ColorScheme instances.
  - **Inputs:** — (data holder).
  - **Outputs:** ColorSchemes for theming and widgets.
  - **Dependencies:** androidx.compose.material3.ColorScheme.
  - **Consumed by:** ui/theme/Monet.kt (dynamicColorSchemes), WidgetExtension.
  - **Related files:** ui/theme/Monet.kt, core/appwidgets/WidgetExtension.kt.
  - **Notes:** Minimal container class.


- `core/model/theme/NowPlayingButtonStyle.kt` — Player button style enum (Normal, Material3, Expressive) with drawables.
  - **Purpose:** Choose player transport button icons.
  - **Responsibilities:** Map styles to play/pause/skip drawables.
  - **Inputs:** — (pure enum).
  - **Outputs:** Button drawables for controls.
  - **Dependencies:** R.drawable.
  - **Consumed by:** NowPlayingScreen (buttonStyle), player control fragments.
  - **Related files:** core/model/theme/NowPlayingScreen.kt, ui/component/base/AbsPlayerControlsFragment.kt.
  - **Notes:** Expressive mixes Normal and M3 icons.


- `core/model/theme/NowPlayingScreen.kt` — Now Playing screen style enum (Default, FullCover, Gradient, Plain, M3, Expressive, Peek).
  - **Purpose:** Describe the seven player layouts.
  - **Responsibilities:** Map styles to layout/button style, supported color schemes, transitions, feature flags.
  - **Inputs:** — (enum metadata).
  - **Outputs:** Style configuration for player and preferences.
  - **Dependencies:** R, PlayerColorSchemeMode, PlayerTransition, NowPlayingButtonStyle.
  - **Consumed by:** Preferences, AbsSlidingMusicPanelActivity, NowPlayingScreenPreferenceDialog.
  - **Related files:** ui/component/base/AbsSlidingMusicPanelActivity.kt, ui/component/preferences/dialog/NowPlayingScreenPreferenceDialog.kt.
  - **Notes:** Feature flags gate carousel, corner radius, cover lyrics.

- `core/palette/PaletteProcessor.kt` — Extracts PaletteColor from bitmaps (QuantizerCelebi/Score + Palette) with white/black filters and contrast adjustments.
  - **Purpose:** Extract PaletteColor from cover bitmaps.
  - **Responsibilities:** Celebi+Score vibrant, Palette region sampling (background/foreground), white/black filters, contrast-guaranteed text colors.
  - **Inputs:** Bitmap, Context, colorAccuracy flag.
  - **Outputs:** PaletteColor or errorColor fallback.
  - **Dependencies:** QuantizerCelebi, Score (m3color), androidx Palette, NotificationColorUtil, isColorLight.
  - **Consumed by:** AdapterExt, PlaybackService, ImageFragment.
  - **Related files:** core/model/PaletteColor, util/color/NotificationColorUtil.
  - **Notes:** Dispatchers.Default; scaled bitmap recycled; accuracy 64 vs 16 px; contrast 4.5 minimum.


- `core/sort/IgnoreKeywordPolicy.kt` — Pure leading-keyword policy and `SortNormalizer` for sort comparisons.
  - **Purpose:** Combine locale articles with user-editable keywords without Android dependencies.
  - **Responsibilities:** Article map, keyword sanitization, case-insensitive add/remove validation, length/count limits, escaped leading-keyword regex, normalized sort strings.
  - **Inputs:** Article toggle, locale language, custom keyword collection, raw keyword/input list.
  - **Outputs:** `SortNormalizer`, sanitized lists, `IgnoreKeywordAddResult`, normalized strings.
  - **Dependencies:** Kotlin/JVM only (`Regex`, `Locale`).
  - **Consumed by:** SortCollator, SortMode, MediaExt, Preferences, IgnoreKeywordsPreferenceDialog; compiled probe.
  - **Related files:** core/sort/SortMode.kt, core/sort/SortCollator.kt, extensions/media/MediaExt.kt, util/Preferences.kt.
  - **Notes:** Only applies when article ignoring is enabled; regex is built once per normalizer and requires whitespace after a literal leading keyword. Limits are 40 characters and 50 keywords.


- `core/sort/SortCollator.kt` — Cached locale Collator and name-based sorting helpers (article/custom-keyword aware).
  - **Purpose:** Provide cached locale Collators and name sorting helpers.
  - **Responsibilities:** Frozen cached collators (PRIMARY, numeric), sortedByName, sortedByMediaName with one sort normalizer per call.
  - **Inputs:** Locale, Iterable plus name selector.
  - **Outputs:** Collator instances, sorted lists.
  - **Dependencies:** android.icu.text.Collator, IgnoreKeywordPolicy, Preferences.
  - **Consumed by:** SortMode, SpecialRepository, ArtistDetailViewModel.
  - **Related files:** core/sort/IgnoreKeywordPolicy.kt, core/sort/SortMode, extensions/media/DataExt.
  - **Notes:** ConcurrentHashMap cache; custom keywords apply only while the article switch is on.


- `core/sort/SortMode.kt` — Sealed sort-mode base: persists key/descending, builds sort menus; subclasses per entity.
  - **Purpose:** Sealed base persisting sort key/descending and building menus.
  - **Responsibilities:** SharedPreferences persistence, sort menu creation/selection, per-entity sorted() implementations (including filename, path, track and date-modified keys), one custom-keyword normalizer per sort call.
  - **Inputs:** SharedPreferences, Menu/MenuItem, entity collections.
  - **Outputs:** Sorted lists, checked menu state.
  - **Dependencies:** Koin (KoinComponent), Preferences, SortKey/SortItem, sortingCollator, IgnoreKeywordPolicy, PlaylistWithSongs.
  - **Consumed by:** Library fragments (SongListFragment, AlbumListFragment, ArtistListFragment, GenresListFragment, YearsListFragment, PlaylistListFragment, FoldersListFragment, GenreDetailFragment, FolderDetailFragment, AlbumDetailFragment, ArtistDetailFragment, YearDetailFragment).
  - **Related files:** SortCollator, IgnoreKeywordPolicy, SortKey, SortItem.
  - **Notes:** Preference keys "<id>_sort_order"/"<id>_descending"; SongSortMode.Dynamic variant. AllSongs and FolderSongs expose Track/Path; AllFiles exposes Path for folders and songs; locale collator numeric collation remains unchanged. `normalizer()` snapshots the article toggle and custom keywords before building comparators, and `ignoreKeywords` feeds the section-letter helper. Genre detail uses dedicated GenreSongs/GenreAlbums/GenreArtists modes so its sort choices stay screen-local.

### data/

- `data/LyricsParser.kt` — Lyrics parser interface (String/Reader → SyncedLyrics with track duration).
  - **Purpose:** Contract for parsing lyrics text into SyncedLyrics with track duration.
  - **Responsibilities:** Parse String/Reader, detect supported formats via handles().
  - **Inputs:** Lyrics text/Reader, trackLength, ignoreBlankLines, LyricsFile.
  - **Outputs:** SyncedLyrics? (nullable on parse failure).
  - **Dependencies:** SyncedLyrics, LyricsFile.
  - **Consumed by:** LrcLyricsParser, TtmlLyricsParser, LyricsRepository (lyricsParsers list).
  - **Related files:** data/model/lyrics/SyncedLyrics.kt, LrcNode.kt, TtmlNodeTree.kt.
  - **Notes:** Blank input short-circuits to null.


- `data/SearchFilter.kt` — Parcelable search filter interface (name, compatible modes, suspend results).
  - **Purpose:** Parcelable contract for scoped library search filters.
  - **Responsibilities:** Expose display name, compatible FilterMode list, suspend getResults.
  - **Inputs:** SearchQuery.FilterMode, query string.
  - **Outputs:** List<Any> results (songs/albums/artists).
  - **Dependencies:** SearchQuery, Parcelable.
  - **Consumed by:** BasicSearchFilter, LastAddedSearchFilter, SmartSearchFilter, SearchRepository, SearchFragment.
  - **Related files:** SearchFilterMapper.kt, data/model/search/SearchQuery.kt.
  - **Notes:** Implementations parcelable for navigation arguments.


- `data/SongProvider.kt` — Minimal interface exposing a Song list.
  - **Purpose:** Minimal interface exposing a Song list.
  - **Responsibilities:** Declare the songs property only.
  - **Inputs:** —.
  - **Outputs:** List<Song>.
  - **Dependencies:** Song.
  - **Consumed by:** Album, Artist, Folder, ReleaseYear, ShuffleManager.shuffleByProvider.
  - **Related files:** data/model/Song.kt and all model implementors.
  - **Notes:** Marker for shuffle and mosaic generation code.


- `data/model/Song.kt` - Main song model (parcelable, FileSystemItem) with uri helpers, disc number, resolved artist name, MIME-derived type and Song.emptySong.
  - **Purpose:** Central parcelable song model implementing FileSystemItem.
  - **Responsibilities:** MediaStore fields, volume-aware uri, toMediaItem(), equality, emptySong, nullable composer metadata for library grouping.
  - **Inputs:** MediaStore cursor data, volumeName, composerName (API 30+).
  - **Outputs:** Song instances, MediaItem, content URIs.
  - **Dependencies:** CoverProvider, getAudioContentUri, FileSystemItem, MediaItem.
  - **Consumed by:** SongRepository, PlaybackExtension.toMediaItems, PlayerViewModel, SongMapper, adapters.
  - **Related files:** ExpandedSong.kt, QueueSong.kt, Album/Artist/Folder/ReleaseYear.
  - **Notes:** Open class subclassed by ExpandedSong and QueueSong. Composer is not part of equality/hashCode because Room-backed songs do not persist it; it defaults to null outside MediaStore R+ queries.


- `data/model/Album.kt` — Album with songs (SongProvider), name, duration, isSingle, cover and Album.empty.
  - **Purpose:** Album grouping songs, deriving name/artist from first song.
  - **Responsibilities:** songCount/duration/dateAdded/isSingle, albumCover, safeGetFirstSong.
  - **Inputs:** id, artistName, albumArtistName, year, firstSongIndex, songs.
  - **Outputs:** Album with computed metadata and cover URI.
  - **Dependencies:** SongProvider, Song, asAlbumCoverUri.
  - **Consumed by:** AlbumRepository.splitIntoAlbums, AlbumDetailViewModel/Fragment, InfoViewModel, PlayInfoFragment.
  - **Related files:** Artist.kt, Song.kt, extensions/media/DataExt.kt.
  - **Notes:** UNKNOWN_ALBUM_DISPLAY_NAME constant; equality by id and songs.


- `data/model/Artist.kt` — Artist with albums (SongProvider), counts, duration, album-artist and composer support.
  - **Purpose:** Artist/album-artist/composer grouping albums with sort and counts.
  - **Responsibilities:** Name resolution, albumCount/songCount/duration, sortedAlbums/sortedSongs, safeGetFirstAlbum, `composerName`/`isComposer`.
  - **Inputs:** id, albums, filterSingles, isAlbumArtist, composerName.
  - **Outputs:** Artist with aggregated metrics.
  - **Dependencies:** Album, SongProvider, SongSortMode.ArtistSongs.
  - **Consumed by:** ArtistRepository, ArtistDetailViewModel/Fragment, TagEditorViewModel, PlayInfoFragment, ArtistAdapter.
  - **Related files:** Album.kt, core/model/ComposerNamePolicy.kt, core/sort/SongSortMode, VARIOUS_ARTISTS constants.
  - **Notes:** Secondary constructor derives id from first album's artistId; composer artists use a synthetic stable id and composerName takes precedence in `name`.


- `data/model/ChannelMode.kt` — Channel mode enum (Mono...Surround 13.1) resolved by channel count.
  - **Purpose:** Enum mapping channel counts to localized labels.
  - **Responsibilities:** fromChannels() lookup, expose channels and labelRes.
  - **Inputs:** Channel count integer.
  - **Outputs:** ChannelMode? or localized string resource.
  - **Dependencies:** R.string channel resources.
  - **Consumed by:** MetadataReader.channelMode(), InfoViewModel/AudioHeaderInfo.
  - **Related files:** data/local/MetadataReader.kt, ui/screen/info/InfoResult.kt.
  - **Notes:** No entries for 9 or 11 channel counts.


- `data/model/ContentType.kt` — Library content type enum (TopArtists, History, Favorites, NotRecentlyPlayed...) with flags.
  - **Purpose:** Enum of library content categories with behavior flags.
  - **Responsibilities:** Flag helpers: isObservableContent, isHistoryContent, isFavoriteContent, isRecentContent, isSearchableContent.
  - **Inputs:** Enum constant selection.
  - **Outputs:** Flag booleans and titleRes.
  - **Dependencies:** R.string titles.
  - **Consumed by:** Repository suggestions, SmartRepository, LibraryViewModel, HomeFragment, DetailListFragment, LastAddedSearchFilter.
  - **Related files:** Suggestion.kt, HomeResult.kt, CategoryInfo.
  - **Notes:** Drives home suggestions and detail-list navigation.


- `data/model/ExpandedSong.kt` — Song subclass with playCount, skipCount, lastPlayedAt and isFavorite.
  - **Purpose:** Song extended with play statistics and favorite flag.
  - **Responsibilities:** Add playCount/skipCount/lastPlayedAt/isFavorite; copy constructor from Song.
  - **Inputs:** Song plus play-count fields.
  - **Outputs:** ExpandedSong instances.
  - **Dependencies:** Song, PlayCountEntity data.
  - **Consumed by:** ShuffleManager (combined shuffle), SmartRepository.
  - **Related files:** Song.kt, PlayCountEntity.kt, SongMapper.kt.
  - **Notes:** Parcelable; used for weighted shuffle scoring.


- `data/model/Folder.kt` — Music folder with musicFiles (FileSystemItem, SongProvider), name, counts and dates.
  - **Purpose:** Filesystem folder containing music files.
  - **Responsibilities:** fileName/dates derivation, songs filter, songCount, file wrapper.
  - **Inputs:** filePath, musicFiles list.
  - **Outputs:** Folder implementing FileSystemItem contract.
  - **Dependencies:** FileSystemItem, SongProvider, File.
  - **Consumed by:** SpecialRepository, FoldersListFragment, FolderDetailFragment/ViewModel, FileAdapter, SearchFilterMapper.
  - **Related files:** FileSystemItem.kt, Song.kt, FolderDetailFragment.kt.
  - **Notes:** songs derived via filterIsInstance<Song>().


- `data/model/Genre.kt` — Parcelable genre (id, name, songCount) with EmptyGenre.
  - **Purpose:** Parcelable genre with id, name, songCount.
  - **Responsibilities:** Hold genre data; EmptyGenre sentinel.
  - **Inputs:** id, name, songCount.
  - **Outputs:** Genre instances.
  - **Dependencies:** Parcelable.
  - **Consumed by:** GenreRepository (EmptyGenre default), GenreDetailViewModel, GenreAdapter, GenresListFragment.
  - **Related files:** GenreRepository.kt, data/local/search/BasicSearchFilter.
  - **Notes:** Songs resolved separately via MediaStore.Genres.


- `data/model/GenreContent.kt` — Genre song snapshot plus derived albums and artists.
  - **Purpose:** Carry the full genre song set and the album/artist groupings used by genre subcategories.
  - **Responsibilities:** Hold songs, albums and artists from one genre query.
  - **Inputs:** Repository.genreContent().
  - **Outputs:** GenreContent for GenreDetailViewModel.
  - **Dependencies:** Song, Album, Artist.
  - **Consumed by:** GenreDetailViewModel, GenreDetailFragment.
  - **Related files:** Genre.kt, Repository.kt, ArtistRepository.kt.
  - **Notes:** Immutable value object; no persistence or schema change.


- `data/model/Playlist.kt` — Device playlist (id, name) resolving songs via repository, with EmptyPlaylist.
  - **Purpose:** Device (MediaStore) playlist resolving songs via repository.
  - **Responsibilities:** description(context), getSongs(), EmptyPlaylist; Koin lookup of PlaylistRepository.
  - **Inputs:** Cursor or id/name.
  - **Outputs:** Playlist, song list, localized description.
  - **Dependencies:** PlaylistRepository, MediaStore, KoinComponent, asNumberOfSongs.
  - **Consumed by:** PlaylistRepository (takeOrDefault), LibraryViewModel, PlaylistListFragment, PlaylistDetailFragment.
  - **Related files:** PlaylistEntity.kt, PlaylistWithSongs.kt.
  - **Notes:** Uses Koin get() inside model for lazy song resolution.


- `data/model/QueuePosition.kt` — Queue position mapping between timeline indices and visible positions.
  - **Purpose:** Maps queue timeline indices to visible positions.
  - **Responsibilities:** previous/next, setCurrentIndex, getPositionForIndex, getIndexForPosition, Undefined.
  - **Inputs:** Current index, indicesInTimeline array.
  - **Outputs:** Position mappings; C.INDEX_UNSET for missing.
  - **Dependencies:** androidx.media3.common.C.
  - **Consumed by:** PlayerViewModel (_positionFlow), QueueScreen.
  - **Related files:** QueueSong.kt, AdvancedForwardingPlayer.kt, PersistentStorage.kt.
  - **Notes:** Undefined sentinel for empty queue.


- `data/model/QueueSong.kt` — Song subclass with a key (Pair<Long,Int>) for queue persistence.
  - **Purpose:** Song subclass keyed for queue persistence.
  - **Responsibilities:** Carry key (queueId, position); copy constructor from Song.
  - **Inputs:** key, Song fields, volumeName.
  - **Outputs:** QueueSong instances.
  - **Dependencies:** Song.
  - **Consumed by:** PlayerViewModel (_queueFlow), QueueScreen, PersistentStorage, QueueEntity.
  - **Related files:** Song.kt, QueueEntity.kt, QueueDao.kt.
  - **Notes:** Key mirrors the (mediaId, order) pair written into a QueueEntity row.


- `data/model/ReleaseYear.kt` — Release year with its songs (SongProvider), parcelable.
  - **Purpose:** Release year grouping its songs.
  - **Responsibilities:** name/songCount, safeGetFirstSong, Empty sentinel.
  - **Inputs:** year, songs.
  - **Outputs:** ReleaseYear instances.
  - **Dependencies:** SongProvider, Parcelable.
  - **Consumed by:** SpecialRepository (releaseYears), YearsListFragment, YearDetailViewModel/Fragment.
  - **Related files:** Song.kt, SpecialRepository.kt.
  - **Notes:** Empty sentinel is ReleaseYear(-1, emptyList()).


- `data/model/Suggestion.kt` — Content suggestion (ContentType + item list).
  - **Purpose:** Home content suggestion pairing ContentType with items.
  - **Responsibilities:** Hold type and item list.
  - **Inputs:** ContentType, List<Any>.
  - **Outputs:** Suggestion instances.
  - **Dependencies:** ContentType.
  - **Consumed by:** Repository (builds suggestions), LibraryViewModel, HomeFragment, HomeAdapter.
  - **Related files:** ContentType.kt, HomeResult.kt, HomeAdapter.kt.
  - **Notes:** Items typed only at render time via type.


- `data/model/lyrics/LyricsActor.kt` — Lyrics actor enum (v1/v2/v3, duet, M/F) with background variants.
  - **Purpose:** Actor enum for multi-person lyrics lines.
  - **Responsibilities:** asBackground conversion, getActorFromValue lookup.
  - **Inputs:** Actor value strings (v1/v2/v3/D/M/F).
  - **Outputs:** LyricsActor with isBackground flag.
  - **Dependencies:** —.
  - **Consumed by:** TtmlNodeTree, LrcLyricsParser, SyncedLyrics lines/words.
  - **Related files:** SyncedLyrics.kt, TtmlAgent.kt.
  - **Notes:** Background variants share the same value.


- `data/model/lyrics/LyricsFile.kt` — Lyrics file (path + TTML/LRC format), parcelable and serializable.
  - **Purpose:** Lyrics file path plus TTML/LRC format.
  - **Responsibilities:** Carry path/format; parcelable and serializable.
  - **Inputs:** path, Format.
  - **Outputs:** LyricsFile instances.
  - **Dependencies:** Parcelable, kotlinx.serialization.
  - **Consumed by:** LyricsRepository.fileLyrics (RawLyrics.File), LyricsParser.handles(file).
  - **Related files:** RawLyrics.kt, LyricsRepository.kt.
  - **Notes:** Format enum values used for extension matching.


- `data/model/lyrics/LyricsSource.kt` — Lyrics source (Embedded/Downloaded/File) and mode (Plain/Synced) enums.
  - **Purpose:** Source and mode enums for lyrics provenance.
  - **Responsibilities:** Map Embedded/Downloaded/File and Plain/Synced to string resources.
  - **Inputs:** Enum constants.
  - **Outputs:** titleRes labels.
  - **Dependencies:** R.string.
  - **Consumed by:** LyricsRepository (cache keys), LyricsViewModel, LyricsEditorScreen.
  - **Related files:** RawLyrics.kt, LyricsEditorScreen.kt.
  - **Notes:** Mode separates plain vs synced rendering.


- `data/model/lyrics/RawLyrics.kt` — Sealed raw lyrics (Embedded, Remote plain/synced/instrumental, Stored).
  - **Purpose:** Sealed unparsed lyrics variants from all origins.
  - **Responsibilities:** Embedded/Remote/File/Stored/Edited variants; accept() merging; prepareToStore().
  - **Inputs:** Raw lyrics text, provider, LyricsFile, instrumental flag.
  - **Outputs:** RawLyrics subtypes for parsing and caching.
  - **Dependencies:** Parcelable, kotlinx.serialization, LyricsFile.
  - **Consumed by:** LyricsRepository, LyricsDownloadService, LyricsApi implementations, LyricsViewModel.
  - **Related files:** SyncedLyrics.kt, LyricsFile.kt, LyricsEntity.kt.
  - **Notes:** Remote.Content pairs provider with lyrics text.


- `data/model/lyrics/SyncedLyrics.kt` — Synced lyrics model: lines with times, word syllables, background content, translation and transliteration.
  - **Purpose:** Structured synced lyrics with lines, words, translations.
  - **Responsibilities:** Validate timings, Line/Word/TextContent helpers, EmptyContent, MIN_OFFSET_TIME.
  - **Inputs:** Parsed lines from LRC/TTML parsers.
  - **Outputs:** SyncedLyrics consumed by UI rendering.
  - **Dependencies:** Compose @Immutable, LyricsActor.
  - **Consumed by:** LyricsRepository, LyricsView, KaraokeStyleText, SpannedWordText, LyricsViewState, LyricsScreen.
  - **Related files:** LyricsParser.kt, LrcNode.kt, TtmlNodeTree.kt.
  - **Notes:** Word-sync via syllables with background vocals.


- `data/model/network/LoginParams.kt` — Sealed login params (Last.fm user/password, ListenBrainz token).
  - **Purpose:** Sealed credentials for scrobbling login.
  - **Responsibilities:** Wrap Last.fm username/password and ListenBrainz token as data map.
  - **Inputs:** Credential values.
  - **Outputs:** LoginParams with map access via operator get.
  - **Dependencies:** —.
  - **Consumed by:** NetworkRepository.loginToService, Repository.loginToService.
  - **Related files:** LoginState.kt, ScrobblingService.kt.
  - **Notes:** Map keyed by API parameter names.


- `data/model/network/LoginState.kt` — Sealed login state (Empty/LoggingIn/LoggedIn/Failure) with helpers.
  - **Purpose:** Sealed login UI state machine.
  - **Responsibilities:** Empty/LoggingIn/LoggedIn/Failure plus canLogIn, isLoading, isFailure extensions.
  - **Inputs:** Login attempts from NetworkRepository.
  - **Outputs:** State flows for scrobbling screens.
  - **Dependencies:** —.
  - **Consumed by:** NetworkRepository (MutableStateFlow), Repository, scrobbling UI.
  - **Related files:** LoginParams.kt, ScrobblingService.kt.
  - **Notes:** LoggedIn carries username and profile url.


- `data/model/network/NetworkFeature.kt` — Enableable network features (Images, Lyrics APIs, Last.fm, ListenBrainz, Updater) with availability checks.
  - **Purpose:** Enableable network capabilities with preference and connectivity checks.
  - **Responsibilities:** isEnabled/isAvailable, isOnline wifi/cellular gate, updater interval logic, preference keys.
  - **Inputs:** SharedPreferences, resource booleans, ConnectivityManager.
  - **Outputs:** Feature availability decisions.
  - **Dependencies:** Preferences, Koin, ConnectivityManager, R.bool flags.
  - **Consumed by:** LyricsDownloadService, LyricsApi implementations, PlaybackService, AudioCoverFetcher, ArtistImageFetcher, MainActivity, KtorClient.
  - **Related files:** util/Preferences.kt, util/UpdateSearchMode.
  - **Notes:** Only-wifi setting gates cellular usage.


- `data/model/network/ScrobblingResult.kt` — Sealed scrobbling result (Success with songId / Failure with message).
  - **Purpose:** Sealed result of scrobble/now-playing submissions.
  - **Responsibilities:** Success(songId) or Failure(message) variants.
  - **Inputs:** API responses from NetworkRepository.
  - **Outputs:** Result objects to callers.
  - **Dependencies:** —.
  - **Consumed by:** NetworkRepository (toScrobblingResult), Repository.scrobble/updateNowPlaying.
  - **Related files:** ScrobblingService.kt, LastFmResult.kt.
  - **Notes:** Failure message optional localized string.


- `data/model/network/ScrobblingService.kt` — Scrobbling service enum (Lastfm, ListenBrainz).
  - **Purpose:** Enum identifying scrobbling backend.
  - **Responsibilities:** Lastfm and ListenBrainz constants.
  - **Inputs:** —.
  - **Outputs:** Service selector for login/scrobble dispatch.
  - **Dependencies:** —.
  - **Consumed by:** NetworkRepository, Repository, scrobbling settings UI.
  - **Related files:** LoginParams.kt, LoginState.kt, ScrobblingResult.kt.
  - **Notes:** Matches LastFmService/ListenBrainzService clients.


- `data/model/network/lastfm/LastFmResult.kt` — LastFmFailure enum mapping Last.fm error codes to resource messages.
  - **Purpose:** Maps Last.fm API error codes to user messages.
  - **Responsibilities:** fromCode() classification into Auth/InvalidCredentials/InvalidParams/ServiceOffline/RateLimit/Unknown.
  - **Inputs:** Error code (Int or String).
  - **Outputs:** LastFmFailure with messageRes.
  - **Dependencies:** R.string error resources.
  - **Consumed by:** NetworkRepository (login and scrobble failures).
  - **Related files:** data/remote/lastfm/model/LastFmError.kt, LastFmService.kt.
  - **Notes:** Unknown fallback for unmatched codes.


- `data/model/replaygain/ReplayGainMode.kt` - ReplayGain mode enum (Album/Track/Off) and presence-aware ReplayGain data class (nullable gains/peaks).
  - **Purpose:** ReplayGain mode enum and gain/peak data class.
  - **Responsibilities:** Album/Track/Off modes with isOn; ReplayGain gains/peaks with Empty default.
  - **Inputs:** Mode selection, tag-extracted values.
  - **Outputs:** Gain values for audio processing.
  - **Dependencies:** —.
  - **Consumed by:** EqualizerManager, ReplayGainAudioProcessor, ReplayGainTagExtractor, EqualizerScreen, SoundState.
  - **Related files:** SoundState.kt, ReplayGainTagExtractor.kt, EqualizerManager.kt.
  - **Notes:** Empty default has neutral gain and peak 1f.


- `data/model/search/FilterSelection.kt` — Parcelable search filter selection (mode, column, SQL selection and args).
  - **Purpose:** Parcelable SQL-backed filter selection.
  - **Responsibilities:** Hold mode, column, selection, arguments for MediaStore queries.
  - **Inputs:** FilterMode, MediaStore column, SQL fragment, args.
  - **Outputs:** FilterSelection consumed by SmartSearchFilter.
  - **Dependencies:** SearchQuery.FilterMode, Parcelable.
  - **Consumed by:** SmartSearchFilter, SearchFilterMapper.
  - **Related files:** SearchQuery.kt, SmartSearchFilter.kt.
  - **Notes:** Internal visibility; built by mappers.


- `data/model/search/SearchQuery.kt` — Search query (filter mode, text, timestamp) with FilterMode enum.
  - **Purpose:** Search query state with filter mode and text.
  - **Responsibilities:** Hold filterMode/searched/timestamp; FilterMode enum with button ids.
  - **Inputs:** Search text, selected FilterMode.
  - **Outputs:** SearchQuery for SearchRepository.search.
  - **Dependencies:** R.id mode buttons.
  - **Consumed by:** SearchViewModel (debounced flow), SearchFragment, SearchRepository, AbsSlidingMusicPanelActivity.
  - **Related files:** FilterSelection.kt, SearchFilter.kt.
  - **Notes:** Timestamp used for query ordering/debounce.


- `data/mapper/SearchFilterMapper.kt` — Extensions creating SearchFilters (Smart/Basic/LastAdded) for album/artist/folder/genre/year.
  - **Purpose:** Builds scoped SearchFilters for entities.
  - **Responsibilities:** Album/Artist SmartSearchFilter with FilterSelection; Folder/Genre/Year/Playlist BasicSearchFilter; lastAddedSearchFilter.
  - **Inputs:** Context, model instances.
  - **Outputs:** SearchFilter parcelable for search navigation.
  - **Dependencies:** SmartSearchFilter, BasicSearchFilter, LastAddedSearchFilter, FilterSelection.
  - **Consumed by:** AlbumDetailFragment, ArtistDetailFragment, GenreDetailFragment, PlaylistDetailFragment, FolderDetailFragment, YearDetailFragment, DetailListFragment.
  - **Related files:** SearchFilter.kt, data/local/search/*, SearchFragment.
  - **Notes:** Album-artist vs artist SQL differs by flag.


- `data/mapper/SongMapper.kt` — Mappings between Song/SongEntity/HistoryEntity/PlayCountEntity.
  - **Purpose:** Conversions between Song and Room entities.
  - **Responsibilities:** Song<->SongEntity, Song->HistoryEntity/PlayCountEntity, PlayCountEntity/HistoryEntity->Song.
  - **Inputs:** Song, playlist id, timePlayed, counters.
  - **Outputs:** Room entities or Song instances.
  - **Dependencies:** SongEntity, HistoryEntity, PlayCountEntity, PlaylistEntity.
  - **Consumed by:** PlaylistRepository, SmartRepository, PlayCountDao, LibraryViewModel, PlaylistDetailFragment, BackupHelper, InfoViewModel, MapperStore, LibraryProvider, PlayerViewModel.
  - **Related files:** data/local/room/* entities, Song.kt.
  - **Notes:** Entities duplicate song fields as snapshots.

- `data/local/SongDurationFilterPolicy.kt` - Pure minimum/maximum song-duration filter rules.
  - **Purpose:** Keep duration bounds and SQL predicate formatting deterministic and JVM-probeable.
  - **Responsibilities:** `isValidRange`; inclusive minimum/maximum selection fragments (`DURATION >= min*1000`, `DURATION <= max*1000`); 0/negative bound contributes no predicate; 0..7200 s/60 s step constants.
  - **Inputs:** Duration column name, minimum seconds, maximum seconds.
  - **Outputs:** Ordered selection-fragment strings or an empty list.
  - **Dependencies:** Kotlin standard library only.
  - **Consumed by:** RealSongRepository.makeSongCursor; compiled policy probe.
  - **Related files:** SongRepository.kt, util/Preferences.kt.
  - **Notes:** Android-free. Applied through the shared song cursor unless the caller sets `skipDurationFilter`; queue resolution and the unknown-genre list skip it, while genre detail/count/search, playlists and favorites remain outside this policy.


- `data/local/MediaQueryDispatcher.kt` - Fluent MediaStore query builder dispatching cursors via ContentResolver.
  - **Purpose:** Build and dispatch MediaStore audio queries fluently.
  - **Responsibilities:** Chain projection/selection/sort; AND-combine selections; execute ContentResolver.query safely.
  - **Inputs:** Uri (audio content), projection, selection, arguments, sort order.
  - **Outputs:** Cursor or null on IllegalArgumentException.
  - **Dependencies:** ContentResolver (Koin), RealSongRepository.getAudioContentUri.
  - **Consumed by:** RealSongRepository, RealArtistRepository, RealSmartRepository, SmartSearchFilter.
  - **Related files:** SongRepository.kt, SmartRepository.kt, SmartSearchFilter.kt.
  - **Notes:** Worker-thread only; default uri is external audio media.


- `data/local/MediaStoreObserver.kt` — ContentObserver batching MediaStore changes (500 ms) and triggering refresh.
  - **Purpose:** Debounce MediaStore audio changes into a single refresh callback.
  - **Responsibilities:** Batch changes 500 ms via Handler; register/unregister external and internal audio URIs.
  - **Inputs:** uiHandler, onChanged callback, Context.
  - **Outputs:** Invokes onChanged once after quiet 500 ms.
  - **Dependencies:** ContentResolver, MediaStore, Handler.
  - **Consumed by:** PlaybackService.
  - **Related files:** PlaybackService.kt.
  - **Notes:** Runnable pattern removes pending callbacks on each change.


- `data/local/MediaStoreWriter.kt` — Writes files (images/playlists) to MediaStore on Android 10+ with Request/Result.
  - **Purpose:** Write images and playlists into scoped MediaStore storage.
  - **Responsibilities:** UUID pending-row staging; complete/close/publish output; checked name/path/volume lookup; backup/final renames with pre-commit restoration; post-commit old-URI cleanup; Request/Result and plain-file helpers.
  - **Inputs:** contentUri, Request (name/path/mime), streamConsumer, optional File.
  - **Outputs:** Result code (SUCCESS/NO_SCOPED_STORAGE/ERROR) with destination Uri.
  - **Dependencies:** ContentResolver, MediaStore, FileUtil.PLAYLISTS_DIRECTORY_NAME.
  - **Consumed by:** M3UWriter, EqualizerViewModel; registered in MainModule.
  - **Related files:** M3UWriter.kt, EqualizerViewModel.kt, MainModule.kt.
  - **Notes:** API 29+; same-instance exports are serialized. Q+ playlist callers pass `MediaStore.Files` with the format's concrete MIME, avoiding legacy `Audio.Playlists` publication name rewriting; this writer itself remains collection-agnostic. Old bytes are never opened for output or pre-deleted. Pre-commit errors remove only the new URI and attempt name restoration; provider refusal can retain old bytes under a backup name. After verified commit, cleanup failure leaves the new exact-name output successful and may leave a backup. No public-API atomic swap, cross-process locking or power-loss guarantee (ADR-044).


- `data/local/MetadataReader.kt` - Reads audio metadata with TagLib (properties, tags, images, bitrate, sample rate, channel mode), plus duration/sample-rate/channel helpers used by extra-format info.
  - **Purpose:** Read audio file metadata and properties via TagLib.
  - **Responsibilities:** Expose tags/propertyMap, pictures, bitrate, sample rate, channel mode, genre normalization.
  - **Inputs:** Uri, readPictures flag.
  - **Outputs:** Metadata accessors (all/get/first/merge/frontCover), Bitmap via Picture.toBitmap.
  - **Dependencies:** TagLib (com.kyant.taglib), ContentResolver, Jaudiotagger GenreTypes.
  - **Consumed by:** InfoViewModel, TagEditorViewModel, MetadataField, ReplayGainTagExtractor, LyricsRepository.
  - **Related files:** MetadataWriter.kt, ReplayGainTagExtractor.kt, MetadataField.kt.
  - **Notes:** Catches all throwables; dup()-detached file descriptors.


- `data/local/MetadataWriter.kt` - Writes metadata and covers via TagLib, including the batch plan path, batch cover writes, current-file journal rollback and MediaScanner refresh.
  - **Purpose:** Write metadata properties/covers and apply a multi-file batch plan with read-back reporting.
  - **Responsibilities:** Build EditTarget; write propertyMap/pictures via TagLib; preserve canonical ReplayGain tags or replace them from an explicit map; album-art thumb; MediaScanner scan; legacy `write()` uses an explicit `kotlin.Result<WriteResultInternal>` and the shared `scanFiles()` accounting so null-URI scan failures increment `failed` and reduce `scanned`; `writeBatch` applies Keep/Set/Clear per file without cover changes, then reads back and reports per-file/per-field status.
  - **Inputs:** EditTarget (Song/Artist/Album), propertyMap, optional replayGainProperties map, picture bitmap, deletion flag; writeBatch accepts a content list, BatchTagEditPlan, progress callback and cooperative stop callback.
  - **Outputs:** WriteResult (contents, failed, scanned counts) for legacy writes; BatchWriteReport for batch writes.
  - **Dependencies:** TagLib, ContentResolver, MediaScannerConnection, ReplayGainTags, insertAlbumArt/deleteAlbumArt extensions, TagEditPlan/TagEditReport, TagWriteVerifier.
  - **Consumed by:** TagEditorViewModel, LyricsRepository.saveLyrics, BatchTagEditorViewModel.
  - **Related files:** MetadataReader.kt, ReplayGainTags.kt, EditTarget (same file), TagWriteVerifier.kt, TagEditorViewModel.kt.
  - **Notes:** TagLib savePropertyMap replaces the tag map; canonicalizes existing ReplayGain aliases instead of filtering them out, so ordinary edits preserve one value per field. Batch writes never create/delete covers and use a fresh read-back; legacy `write` behavior is preserved. Runs on IO dispatcher. Validation: v2 source `6531A98D...` reviewed PASS-qualified (`4B88AD41...`) and live-integrated as the exact 10-file set (`D49D1A89...`); no app build or runtime PASS.

- `data/local/TagWriteVerifier.kt` — Pure read-back comparison for batch tag writes.
  - **Purpose:** Turn TagLib's boolean save into per-field truth.
  - **Responsibilities:** Compare before/after property maps; classify Set as Written/Unsupported/Mismatch, Clear as Cleared/Mismatch, Keep as Kept/KeptChanged; derive file status.
  - **Inputs:** TagEditPlan, before/after property maps.
  - **Outputs:** List<FieldWriteResult> and FileWriteStatus.
  - **Dependencies:** TagEditPlan/TagEditField/TagEditReport.
  - **Consumed by:** MetadataWriter.writeBatch.
  - **Related files:** TagEditReport.kt, TagEditPlan.kt, MetadataWriter.kt.
  - **Notes:** Normalizes by trimming and joining multi-values with "; "; no case folding. Format normalization can surface as Mismatch/KeptChanged.


- `data/local/ReplayGainTagExtractor.kt` - Extracts ReplayGain tags (standard, iTunes `----:com.apple.iTunes:`, Opus R128) with LruCache per Uri.
  - **Purpose:** Extract ReplayGain values from audio tag variants.
  - **Responsibilities:** Read the TagLib property map, delegate parsing/alias resolution to ReplayGainTagResolver, cache per Uri (LruCache 128), invalidate after calculation writes.
  - **Inputs:** Song.
  - **Outputs:** ReplayGain (nullable album/track gains and peaks).
  - **Dependencies:** MetadataReader, ReplayGain model, ReplayGainTagResolver, LruCache.
  - **Consumed by:** PlaybackService, DataExt.kt, ReplayGainScanner, App.kt (clearCache).
  - **Related files:** MetadataReader.kt, ReplayGainMode.kt, ReplayGainTags.kt, ReplayGainTagResolver.kt, PlaybackService.kt.
  - **Notes:** Missing/malformed values stay null so a measured 0.00 dB is not treated as absent; alias priority is Standard > iTunes > R128.

- `data/local/replaygain/ReplayGainTagResolver.kt` - Pure ReplayGain tag parser and alias resolver.
  - **Purpose:** Convert raw TagLib property maps into nullable ReplayGain values with deterministic alias priority.
  - **Responsibilities:** Parse standard/iTunes gains and peaks, convert R128 Q7.8 gains, skip blank/malformed/non-finite values, and choose the highest-priority valid alias per field.
  - **Inputs:** Map<String, Array<String>> from MetadataReader.
  - **Outputs:** ReplayGain.
  - **Dependencies:** ReplayGainTags, ReplayGain model, kotlin stdlib.
  - **Consumed by:** ReplayGainTagExtractor; pure tags probe.
  - **Related files:** ReplayGainTagExtractor.kt, ReplayGainTags.kt, ReplayGainMode.kt.
  - **Notes:** Standard > iTunes > R128; a malformed higher-priority alias does not hide a valid lower-priority one; 0.00 dB and zero peak remain present.

- `data/local/replaygain/ReplayGainTags.kt` — Canonical ReplayGain property names and format-specific maps.
  - **Purpose:** Keep standard, MP4 freeform, Opus R128 and alias canonicalization consistent.
  - **Responsibilities:** Detect ReplayGain keys by suffix; choose one canonical value per field; format gain/peak/R128 strings; expose `mp4Properties` (standard names that TagLib maps to iTunes freeform atoms) and `opusProperties`.
  - **Inputs:** TagLib property maps; measured values.
  - **Outputs:** Canonical property maps and formatted strings.
  - **Dependencies:** java.util.Locale, kotlin.math.round.
  - **Consumed by:** ReplayGainTagExtractor, ReplayGainTagWriter, MetadataWriter.
  - **Related files:** ReplayGainTagWriter.kt, MetadataWriter.kt.
  - **Notes:** Standard key wins over iTunes and R128 when aliases coexist; aliasPriority is shared with the resolver and writer verifier. MP4 writes use standard names because TagLib silently drops prefixed freeform keys while returning success.

- `data/local/replaygain/ReplayGainAnalyzer.kt` — Streaming BS.1770-4/EBU R128 loudness analyzer and pure math.
  - **Purpose:** Measure integrated loudness and linear peak from interleaved float PCM.
  - **Responsibilities:** Rate-aware K-weighting; 400 ms blocks with 100 ms hop; absolute/relative gating; track/album gain; peak limiting/clamping.
  - **Inputs:** Interleaved FloatArray frames, sample rate, mono/stereo channel count.
  - **Outputs:** ReplayGainAnalysis (block energies, peak); ReplayGainMath.LUFS/gain helpers.
  - **Dependencies:** kotlin.math only (Android-free).
  - **Consumed by:** ReplayGainScanner; pure probes.
  - **Related files:** ReplayGainScanner.kt, ReplayGainTagWriter.kt.

- `data/local/replaygain/ReplayGainDecoder.kt` — MediaExtractor/MediaCodec PCM decoder.
  - **Purpose:** Decode one MediaStore audio item to interleaved float PCM without the playback pipeline.
  - **Responsibilities:** Select audio track; configure/start codec; handle output-format change, 8/16-bit and float PCM, EOS, idle limits, resource release and cancellation; count decoded frames and reject a materially short decode or partial frame against the declared duration after subtracting `KEY_ENCODER_DELAY`/`KEY_ENCODER_PADDING`.
  - **Inputs:** Context, Song.
  - **Outputs:** ReplayGainAudioFormat; sample callbacks.
  - **Dependencies:** MediaExtractor, MediaCodec, kotlinx.coroutines.
  - **Consumed by:** ReplayGainScanner.
  - **Related files:** ReplayGainScanner.kt, ReplayGainAnalyzer.kt.
  - **Notes:** Mono/stereo only; unsupported codecs/encodings, truncated files and corrupt frames fail per file. AAC/MP4 encoder priming is trimmed by MediaCodec and is not counted as missing audio.

- `data/local/replaygain/ReplayGainScanner.kt` — Album-grouped scan orchestration.
  - **Purpose:** Measure selected songs and write track/album ReplayGain.
  - **Responsibilities:** De-duplicate songs; group by album; measure sequentially with cancellation; write album gain only for complete, fully measured albums; snapshot previous album tags and roll back on a write failure; report progress/failures.
  - **Inputs:** Repository, decoder, tag writer, selected songs, progress callback.
  - **Outputs:** ReplayGainScanResult (written/failed/cancelled/failures).
  - **Dependencies:** Repository, ReplayGainDecoder, ReplayGainAnalyzer, ReplayGainTagWriter, ReplayGainTagExtractor, kotlinx.coroutines.
  - **Consumed by:** ReplayGainScanViewModel.
  - **Related files:** ReplayGainScanViewModel.kt, ReplayGainTagWriter.kt.
  - **Notes:** Partial selections never receive fabricated album tags; a complete-album write failure suppresses new album values, restores already-tagged tracks and continues remaining tracks with track tags only; cancellation never completes album tags from a partial set. Per-file failures are logged under the `ReplayGainScan` tag.

- `data/local/replaygain/WavId3v2TagCodec.kt` - Explicit RIFF WAV ID3v2 ReplayGain codec.
  - **Purpose:** Persist WAV ReplayGain tags without relying on TagLib's non-persisting WAV save path.
  - **Responsibilities:** Parse RIFF chunks; read/replace the `ID3 ` chunk; preserve `fmt `/`data` and non-ReplayGain ID3 frames; remove/replace RG `TXXX` frames; update the RIFF size; shift later chunks when the tag size changes; stage/restore a backup on failure; apply version-aware ID3 frame flags; read back RG values.
  - **Inputs:** Read/write FileChannel pair, RG values, removeTrack/removeAlbum flags, optional backup file/observer.
  - **Outputs:** WriteResult; Map<String, Array<String>> read-back.
  - **Dependencies:** java.io File streams, java.nio FileChannel/ByteBuffer, ReplayGainTags (Android-free).
  - **Consumed by:** ReplayGainTagWriter WAV write/snapshot/restore.
  - **Related files:** ReplayGainTagWriter.kt, ReplayGainTags.kt.
  - **Notes:** Handles lowercase `id3 ` chunks, mid-file ID3 before `fmt `/`data`, exactly one `TXXX` per field, and verifies through a fresh descriptor in the writer. v2.3 compression/encryption/grouping and v2.4 grouping/unsync/data-length flags fail closed; backup restore covers injected mid-write failure; recovery uses the app-private `noBackupFilesDir` and remains subject to power-loss/partial-backup limits. The current AA-06 checkpoint is source-unvalidated; final independent review, frozen build and device WAV/M4A gates remain pending.

- `data/local/replaygain/ReplayGainTagWriter.kt` — Format-aware ReplayGain writer and read-back verifier.
  - **Purpose:** Persist calculated values safely without duplicate aliases.
  - **Responsibilities:** Open rw descriptor; read current map; choose standard/WAV-ID3v2/MP4-freeform/Opus keys; preserve non-RG tags; replace calculated fields; save non-WAV through TagLib and WAV through WavId3v2TagCodec with an app-private persistent backup under `noBackupFilesDir`; reopen with a fresh read descriptor and verify one alias per field; fail closed when the container cannot persist them; notify MediaStore; snapshot and restore album-only aliases for rollback.
  - **Inputs:** Song, ReplayGainAudioFormat, track gain/peak, optional album gain/peak.
  - **Outputs:** ReplayGainWriteResult.
  - **Dependencies:** TagLib, ContentResolver, ReplayGainTags, WavId3v2TagCodec.
  - **Consumed by:** ReplayGainScanner.
  - **Related files:** ReplayGainTags.kt, WavId3v2TagCodec.kt, MetadataWriter.kt.
  - **Notes:** Fresh-descriptor verification prevents a false success when TagLib does not persist tags. WAV uses `WavId3v2TagCodec` because TagLib's WAV save returns true without persisting in the app process; MP4 uses standard properties that TagLib stores as `----:com.apple.iTunes:REPLAYGAIN_*` freeform atoms. Verification matches by RG suffix and requires exactly one alias. Numeric tolerance 0.001; R128 integers compared exactly. restoreAlbumTags removes album aliases, preserves canonical track tags/non-RG metadata and verifies the restored album state. The WAV branch stages an app-private persistent backup under `noBackupFilesDir` and restores original bytes on any failure; recovery is per-song on the next write, with no global startup scan, so stale backups for songs never rewritten can persist, and legacy cacheDir backups are not migrated or swept. The current AA-06 checkpoint is source-unvalidated; final independent review, frozen build and device WAV/M4A gates remain pending.


- `data/local/lyrics/InstrumentalDetector.kt` — Detects instrumental songs by title keywords or short exact lyrics.
  - **Purpose:** Heuristically detect instrumental tracks.
  - **Responsibilities:** Match title keywords; match short exact lyrics against identifiers.
  - **Inputs:** identifiers set, markByTitle, maxLength, title, lyrics.
  - **Outputs:** Boolean instrumental flag.
  - **Dependencies:** None.
  - **Consumed by:** LyricsViewModel.
  - **Related files:** LyricsViewModel.kt, LyricsRepository.kt.
  - **Notes:** Configuration built from preferences in LyricsViewModel.


- `data/local/lyrics/lrc/LrcLyricsParser.kt` — LRC/enhanced-LRC parser: multiple timestamps, word-sync, offset, translations.
  - **Purpose:** Parse standard and enhanced LRC lyric text.
  - **Responsibilities:** Handle timestamps, attributes, word-sync, background vocals, translations; produce SyncedLyrics.
  - **Inputs:** Reader, trackLength, ignoreBlankLines; LyricsFile for format check.
  - **Outputs:** SyncedLyrics or null.
  - **Dependencies:** LyricsParser interface, LrcNode, SyncedLyrics, LyricsActor.
  - **Consumed by:** RealLyricsRepository.
  - **Related files:** LrcNode.kt, TtmlLyricsParser.kt, LyricsParser.kt.
  - **Notes:** Two-pass: raw nodes then duration/translation resolution.


- `data/local/lyrics/lrc/LrcNode.kt` — Intermediate LRC parser node with children and conversion to SyncedLyrics.
  - **Purpose:** Intermediate node model for LRC parsing.
  - **Responsibilities:** Hold line/word timings, actors, background; add children; convert to SyncedLyrics.
  - **Inputs:** start, text, bgText, rawLine, actor.
  - **Outputs:** SyncedLyrics.Line, TextContent, Word.
  - **Dependencies:** SyncedLyrics, LyricsActor.
  - **Consumed by:** LrcLyricsParser.
  - **Related files:** LrcLyricsParser.kt.
  - **Notes:** INVALID_DURATION (-1) signals missing end time.


- `data/local/lyrics/ttml/TtmlAccompaniment.kt` — TTML accompaniment (translation/transliteration) state handling.
  - **Purpose:** Track translation/transliteration content during TTML parsing.
  - **Responsibilities:** Prepare/set/finish/close per-key texts; collect words and background content.
  - **Inputs:** Type (Translation lang / Transliteration lang), key, content, TtmlNode words.
  - **Outputs:** AccompanistText (content, backgroundContent, syllables).
  - **Dependencies:** TtmlNode, collapseSpaces/isWhitespace extensions.
  - **Consumed by:** TtmlNodeTree.
  - **Related files:** TtmlNodeTree.kt, TtmlLyricsParser.kt.
  - **Notes:** TtmlTranslation supports in-line variant.


- `data/local/lyrics/ttml/TtmlAgent.kt` — TTML agent (person/group/other) assigning actors to lines.
  - **Purpose:** Model TTML agent (performer) definitions.
  - **Responsibilities:** Map agent id to type (person/group/other) from XML.
  - **Inputs:** id, type string.
  - **Outputs:** TtmlAgent data class instance.
  - **Dependencies:** None.
  - **Consumed by:** TtmlNodeTree, TtmlNode.
  - **Related files:** TtmlNodeTree.kt, TtmlLyricsParser.kt.
  - **Notes:** Unknown types fall back to Other.


- `data/local/lyrics/ttml/TtmlLyricsParser.kt` — TTML (Apple Music) parser with XmlPullParser building the node tree.
  - **Purpose:** Parse Apple Music-style TTML lyrics via XmlPullParser.
  - **Responsibilities:** Validate document; build TtmlNodeTree; parse time expressions; handle spans/agents.
  - **Inputs:** Reader, trackLength, ignoreBlankLines; LyricsFile format check.
  - **Outputs:** SyncedLyrics or null.
  - **Dependencies:** LyricsParser, XmlPullParser, TtmlNodeTree, TtmlNode, TtmlAccompaniment.
  - **Consumed by:** RealLyricsRepository.
  - **Related files:** TtmlNode.kt, TtmlNodeTree.kt, TtmlAccompaniment.kt, TtmlAgent.kt.
  - **Notes:** Supports clock-time and offset-time expressions.


- `data/local/lyrics/ttml/TtmlNode.kt` — TTML tree node with timings, text, background, agents and open/closed states.
  - **Purpose:** Single node in the TTML document tree.
  - **Responsibilities:** Store type/timings/text/agent/background; add children; close recursively.
  - **Inputs:** type, begin/end/dur, key, agent; text via setText.
  - **Outputs:** Child nodes, text, closed state; builder helpers.
  - **Dependencies:** TtmlAgent, isWhitespace extension.
  - **Consumed by:** TtmlLyricsParser, TtmlNodeTree.
  - **Related files:** TtmlNodeTree.kt, TtmlLyricsParser.kt.
  - **Notes:** Node types: body, section, line, word.


- `data/local/lyrics/ttml/TtmlNodeTree.kt` — Hierarchical TTML tree managing node open/close and final SyncedLyrics conversion.
  - **Purpose:** Manage hierarchical TTML tree and convert to SyncedLyrics.
  - **Responsibilities:** Open/close nodes, agents, accompaniments; resolve timings; map agents to LyricsActor.
  - **Inputs:** TtmlNodes from parser, trackLength.
  - **Outputs:** SyncedLyrics with words, translations, transliterations.
  - **Dependencies:** TtmlNode, TtmlAgent, TtmlAccompaniment, SyncedLyrics, LyricsActor.
  - **Consumed by:** TtmlLyricsParser.
  - **Related files:** TtmlLyricsParser.kt, TtmlNode.kt.
  - **Notes:** Picks translation matching the system locale.


- `data/local/room/SongEntity.kt` — Room entity for playlist songs (unique index playlist_creator_id+id).
  - **Purpose:** Room entity snapshot of a song inside a playlist.
  - **Responsibilities:** Persist full song fields; unique playlist_creator_id+id index; parcelable.
  - **Inputs:** Song data via toSongEntity mapping.
  - **Outputs:** SongEntity rows.
  - **Dependencies:** Room (@Entity, @Index), Parcelize.
  - **Consumed by:** PlaylistDao, RealPlaylistRepository, LibraryViewModel, PlaylistDetailViewModel, M3UWriter, SongMapper.
  - **Related files:** PlaylistDao.kt, PlaylistWithSongs.kt, SongMapper.kt.
  - **Notes:** song_key is auto-generated primary key.


- `data/local/room/QueueEntity.kt` — Room entity for a saved queue item keyed by queue position.
  - **Purpose:** Room entity for a persisted queue item.
  - **Responsibilities:** Store media id and its order position; allow the same media id on several rows (duplicates).
  - **Inputs:** id (mediaId), order (primary key).
  - **Outputs:** QueueEntity row.
  - **Dependencies:** Room.
  - **Consumed by:** QueueDao, PersistentStorage.
  - **Related files:** QueueDao.kt, PersistentStorage.kt.
  - **Notes:** Primary key is `order`, not the media id, so repeated songs survive persistence (ADR-038).


- `data/local/room/QueueDao.kt` — Queue DAO: save, list ordered and transactional replace-all.
  - **Purpose:** Persist and restore the playback queue.
  - **Responsibilities:** Replace the queue transactionally (clear + save); save; list ordered.
  - **Inputs:** List<QueueEntity>.
  - **Outputs:** Ordered QueueEntity list.
  - **Dependencies:** Room (@Dao, @Transaction).
  - **Consumed by:** PersistentStorage.
  - **Related files:** QueueEntity.kt, PersistentStorage.kt.
  - **Notes:** `replaceQueue` clears and re-inserts inside one transaction; the old `removeNotIn` prune was removed (full rewrite keeps rows in sync).


- `data/local/room/PlaylistWithSongs.kt` — Room @Embedded+@Relation between playlist and its songs.
  - **Purpose:** Room relation combining playlist and its songs.
  - **Responsibilities:** Embed PlaylistEntity; relate SongEntity by playlist_creator_id; songCount helper.
  - **Inputs:** PlaylistEntity, songs.
  - **Outputs:** Combined parcelable relation; Empty companion.
  - **Dependencies:** Room (@Embedded, @Relation), Parcelize.
  - **Consumed by:** PlaylistDao, RealPlaylistRepository, PlaylistDetailViewModel, Repository.
  - **Related files:** PlaylistEntity.kt, SongEntity.kt, PlaylistDao.kt.
  - **Notes:** Also exposed as LiveData by PlaylistDao.


- `data/local/room/PlaylistEntity.kt` — Room playlist entity (name, custom cover, description).
  - **Purpose:** Room playlist metadata entity.
  - **Responsibilities:** Persist name, custom cover uri, description.
  - **Inputs:** PlaylistEntity fields.
  - **Outputs:** PlaylistEntity row; Empty companion.
  - **Dependencies:** Room, Parcelize.
  - **Consumed by:** PlaylistDao, RealPlaylistRepository, BackupHelper.
  - **Related files:** PlaylistWithSongs.kt, PlaylistDao.kt.
  - **Notes:** playlist_id is auto-generated.


- `data/local/room/PlaylistDao.kt` - Playlists/songs DAO: CRUD, relations as LiveData/Flow, searches, cascade delete.
  - **Purpose:** Room DAO for playlists and their songs.
  - **Responsibilities:** CRUD playlists/songs; exact-name/count and by-ID lookups; relation queries LiveData/Flow; searches; cascade deletes; favorites.
  - **Inputs:** PlaylistEntity/SongEntity, ids, names.
  - **Outputs:** Entities, PlaylistWithSongs, LiveData/Flow lists.
  - **Dependencies:** Room, LiveData, Flow.
  - **Consumed by:** RealPlaylistRepository, SsilencceDatabase.
  - **Related files:** PlaylistEntity.kt, SongEntity.kt, PlaylistWithSongs.kt.
  - **Notes:** `playlist(name)` is an exact case-sensitive match. `playlistById` and `countPlaylistsByName` support import revalidation; interactive create+insert and replace-contents transactions live in RealPlaylistRepository, not as schema changes.


- `data/local/room/PlayCountEntity.kt` — Room play-count entity (playCount, skipCount, timePlayed) with song snapshot.
  - **Purpose:** Room entity tracking per-song playback statistics.
  - **Responsibilities:** Persist song snapshot plus timePlayed/playCount/skipCount.
  - **Inputs:** Song data via toPlayCount mapping.
  - **Outputs:** PlayCountEntity row.
  - **Dependencies:** Room.
  - **Consumed by:** PlayCountDao, SongMapper, InfoResult.
  - **Related files:** PlayCountDao.kt, SongMapper.kt, InfoResult.kt.
  - **Notes:** id is the MediaStore song id.


- `data/local/room/PlayCountDao.kt` - Play-count DAO: upsert, transactional play/skip increments, top 100 in Flow, stable-id artist aggregates, backup snapshots.
  - **Purpose:** Room DAO for play/skip counting, all-time artist aggregates and backup/restore snapshots.
  - **Responsibilities:** Upsert; bulk upsert; read all counts for backup; transactional increments; top-100 list/Flow queries; all-time artist-ID counts with unbounded positive-play total in the same query snapshot; delete/clear.
  - **Inputs:** Song, PlayCountEntity lists, timePlayed, song ids.
  - **Outputs:** PlayCountEntity lists and Flows; ArtistIdPlayCount lists/Flows.
  - **Dependencies:** Room (@Dao, @Upsert, @Transaction, Flow), SongMapper.toPlayCount.
  - **Consumed by:** RealSmartRepository; LibraryViewModel stats flows; BackupHelper (allPlayCounts/upsertSongsInPlayCount).
  - **Related files:** PlayCountEntity.kt, PlayEventDao.kt, SmartRepository.kt, data/model/ArtistPlayStat.kt.
  - **Notes:** Only the outer artist list is limited to 100; its denominator includes all positive aggregate plays. Same-display-name artists retain separate aggregate IDs. All-time preserves pre-event counts (ADR-040).


- `data/local/room/LyricsEntity.kt` — Room cached-lyrics entity (song id, lyrics, provider, isInstrumental), serializable for backups.
  - **Purpose:** Room entity caching lyrics per song and carrying it through backup/restore.
  - **Responsibilities:** Store lyrics text, provider, instrumental flag; expose a kotlinx serializer.
  - **Inputs:** id, lyrics, provider, instrumental.
  - **Outputs:** LyricsEntity row; `LyricsEntity$$serializer` for the backup JSON round-trip.
  - **Dependencies:** Room, kotlinx.serialization (@Serializable).
  - **Consumed by:** LyricsDao, RealLyricsRepository, BackupHelper.
  - **Related files:** LyricsDao.kt, LyricsRepository.kt.
  - **Notes:** Instrumental songs stored without lyrics text; without @Serializable the lyrics cache could not be encoded/decoded and silently broke backup/restore.


- `data/local/room/LyricsDao.kt` — Lyrics DAO: upsert, deletion by song/total, get individual/all.
  - **Purpose:** Room DAO for cached lyrics.
  - **Responsibilities:** Upsert single/batch; delete by song or all; get individual/all.
  - **Inputs:** LyricsEntity, song ids.
  - **Outputs:** LyricsEntity or list.
  - **Dependencies:** Room (@Dao, @Upsert).
  - **Consumed by:** RealLyricsRepository, BackupHelper.
  - **Related files:** LyricsEntity.kt, LyricsRepository.kt.
  - **Notes:** No Flow/LiveData; suspend queries only.


- `data/local/room/InclExclEntity.kt` — Room include/exclude path entity (path + type).
  - **Purpose:** Room entity for library include/exclude paths.
  - **Responsibilities:** Store folder path and type (whitelist/blacklist).
  - **Inputs:** path, type int.
  - **Outputs:** InclExclEntity row.
  - **Dependencies:** Room.
  - **Consumed by:** InclExclDao, RealSongRepository, LibraryViewModel, BlacklistWhitelistDialog.
  - **Related files:** InclExclDao.kt, SongRepository.kt.
  - **Notes:** Path is the primary key.


- `data/local/room/InclExclDao.kt` — Whitelist/blacklist folder DAO (insert/delete/clear/get).
  - **Purpose:** Room DAO for whitelist/blacklist folders.
  - **Responsibilities:** Insert single/batch, delete, clear per type, query typed lists.
  - **Inputs:** InclExclEntity, type constants.
  - **Outputs:** Path lists by type.
  - **Dependencies:** Room.
  - **Consumed by:** RealSongRepository, LibraryViewModel, BlacklistWhitelistDialog, PreferencesScreenFragment.
  - **Related files:** InclExclEntity.kt, SongRepository.kt.
  - **Notes:** WHITELIST=0, BLACKLIST=1 constants.


- `data/local/room/HistoryEntity.kt` — Room playback-history entity (song snapshot + timePlayed).
  - **Purpose:** Room entity for playback history entries.
  - **Responsibilities:** Persist song snapshot plus timePlayed timestamp.
  - **Inputs:** Song via toHistoryEntity mapping.
  - **Outputs:** HistoryEntity row.
  - **Dependencies:** Room.
  - **Consumed by:** HistoryDao, SongMapper.
  - **Related files:** HistoryDao.kt, SongMapper.kt.
  - **Notes:** id is the MediaStore song id.


- `data/local/room/HistoryDao.kt` — History DAO: upsert, deletions, played/not-played songs with cutoffs, limit 100.
  - **Purpose:** Room DAO for playback history.
  - **Responsibilities:** Upsert; deletions; played/not-played id queries with cutoffs; limit 100; clear.
  - **Inputs:** HistoryEntity, cutoff, limit, song ids.
  - **Outputs:** HistoryEntity lists, Flows, id lists.
  - **Dependencies:** Room (@Dao, @Upsert), Flow.
  - **Consumed by:** RealSmartRepository.
  - **Related files:** HistoryEntity.kt, SmartRepository.kt.
  - **Notes:** Cutoff drives "not recently played" logic.


- `data/local/search/BasicSearchFilter.kt` — Context search filter (folder/genre/year/playlist) with generic parcelable argument.
  - **Purpose:** Scoped search filter for folder/genre/year/playlist contexts.
  - **Responsibilities:** Dispatch query to scoped searches; optionally convert songs to albums; parcelable argument.
  - **Inputs:** Name, Argument (value+type), FilterMode, query.
  - **Outputs:** Song or Album lists.
  - **Dependencies:** SearchRepository, RealAlbumRepository, Koin injection.
  - **Consumed by:** SearchFilterMapper, SearchFragment, SearchViewModel.
  - **Related files:** SearchFilterMapper.kt, SearchRepository.kt.
  - **Notes:** Argument types YEAR/GENRE/PLAYLIST/FOLDER.


- `data/local/search/LastAddedSearchFilter.kt` — Search filter over recently added content via SmartRepository.
  - **Purpose:** Search restricted to recently added content.
  - **Responsibilities:** Map mode to ContentType; query SmartRepository; split into albums/artists.
  - **Inputs:** Name, FilterMode, query.
  - **Outputs:** Songs, Albums or Artists.
  - **Dependencies:** SmartRepository, RealAlbumRepository, RealArtistRepository.
  - **Consumed by:** SearchFilterMapper, SearchFragment.
  - **Related files:** SearchFilterMapper.kt, SmartRepository.kt.
  - **Notes:** Compatible with Songs/Albums/Artists modes.


- `data/local/search/SmartSearchFilter.kt` — Configurable filter building MediaStore SQL by FilterSelection mode.
  - **Purpose:** Configurable MediaStore SQL search filter.
  - **Responsibilities:** Combine FilterSelection SQL with LIKE query; project base columns; convert to albums.
  - **Inputs:** Name, projection, selections, mode, query.
  - **Outputs:** Songs or Albums.
  - **Dependencies:** MediaQueryDispatcher, RealSongRepository, RealAlbumRepository.
  - **Consumed by:** SearchFilterMapper, SearchViewModel.
  - **Related files:** FilterSelection.kt, SearchFilterMapper.kt.
  - **Notes:** Parcelable with Koin-injected repository.

- `data/local/repository/Repository.kt` - Main Repository interface: songs/albums/artists/genres/years/folders/playlists, searches, favorites, scrobbling and play statistics.
  - **Purpose:** Single facade over all local and network data.
  - **Responsibilities:** Delegate to sub-repositories; delete cleanup; home suggestions; search; scrobbling; period/all-time play stats (suspend + Flow); one-shot song play stats with counts for CSV export; artist/album-artist/composer collections; genre content grouping.
  - **Inputs:** Queries, ids, uris, MediaItems, SearchFilter, ListeningPeriod.
  - **Outputs:** Songs, albums, artists, album artists, composers, genre content, playlists, suggestions, scrobble results, ArtistPlayStat and SongPlayStat rows.
  - **Dependencies:** RealSongRepository, RealAlbumRepository, RealArtistRepository, RealGenreRepository, RealSmartRepository, RealSpecialRepository, RealPlaylistRepository, RealSearchRepository, NetworkRepositoryImpl.
  - **Consumed by:** LibraryViewModel, PlayerViewModel, PlaybackService, LibraryProvider.
  - **Related files:** All repository implementations, MainModule.kt, NamedQueueRepository.kt.
  - **Notes:** MAX_ITEMS_PER_CHUNK (900) batching. Statistics expose suspend methods for one-shot reads and Flow methods for live period/all-time updates (ADR-040); named queues are a separate repository injected into PlayerViewModel (ADR-041). `genreContent()` performs one `songsByGenre()` query and reuses album/artist grouping, with no schema change.


- `data/local/repository/SongRepository.kt` - Song queries from MediaStore (cursors, uris, MediaItems, blacklist) + RealSongRepository, including ghost filtering, extra-format merging and album-artist fallback search.
  - **Purpose:** MediaStore song queries and cursor-to-Song mapping.
  - **Responsibilities:** Build song cursors; minimum/maximum duration selection via SongDurationFilterPolicy; explicit `skipDurationFilter` for queue/unknown-genre paths; blacklist/whitelist selection; Uri/MediaItem resolution; blacklist init; API 30+ composer projection and mapping.
  - **Inputs:** Selections, uris, MediaItems, file paths.
  - **Outputs:** Song lists, Songs (including nullable composer), missing MediaItems.
  - **Dependencies:** MediaQueryDispatcher, SongDurationFilterPolicy, InclExclDao, Preferences, SongSortMode, MediaStore.MediaColumns.COMPOSER on R+.
  - **Consumed by:** RealRepository and all sibling repositories.
  - **Related files:** MediaQueryDispatcher.kt, SongDurationFilterPolicy.kt, InclExclDao.kt, AlbumRepository.kt.
  - **Notes:** BASE_SELECTION filters IS_MUSIC and non-empty titles. `makeSongCursor` applies inclusive min/max duration predicates unless `skipDurationFilter` is true. Resolution paths pass true: `songsByMediaItems`, `songByMediaItem`, `songByFilePath`, `song(songId)`, file-provider URI resolution and the device-playlist `SortedCursorUtil` query; `GenreRepository.getSongsWithNoGenre` also passes true. Device/Room playlists/favorites and raw genre queries bypass the filters.


- `data/local/repository/AlbumRepository.kt` — Albums from MediaStore grouping songs (splitIntoAlbums) + RealAlbumRepository.
  - **Purpose:** Album aggregation from MediaStore songs.
  - **Responsibilities:** Group songs into Albums; sort; similar albums; min-song filtering; expose splitIntoAlbums on the interface with default sorted/sort-mode parameters.
  - **Inputs:** Queries, album ids, Songs.
  - **Outputs:** Album lists and single Album.
  - **Dependencies:** RealSongRepository, Preferences, AlbumSortMode, SongSortMode.
  - **Consumed by:** RealRepository (including genreContent), RealSearchRepository, SmartSearchFilter, LastAddedSearchFilter.
  - **Related files:** SongRepository.kt, ArtistRepository.kt.
  - **Notes:** splitIntoAlbums is part of AlbumRepository so RealRepository can group through the interface; RealAlbumRepository overrides it without redeclaring defaults, and the interface defaults remain available to concrete callers.


- `data/local/repository/ArtistRepository.kt` — Artists/album-artists/composers grouped into albums + RealArtistRepository.
  - **Purpose:** Artist, album-artist and composer aggregation.
  - **Responsibilities:** Group albums by artist; Various Artists special case; similar album artists via genre; API 30+ composer list/detail grouping by `Song.composerName`; group an arbitrary song snapshot into artists with `artistsForSongs()` for genre subcategories.
  - **Inputs:** Queries, artist ids, names, composer names, song snapshots.
  - **Outputs:** Artist lists, single Artist (including composers), artists for a song snapshot.
  - **Dependencies:** RealSongRepository, RealAlbumRepository, MediaQueryDispatcher, Preferences, ComposerNamePolicy, MediaStore.MediaColumns.COMPOSER on R+.
  - **Consumed by:** RealRepository (including genreContent), RealSearchRepository, LastAddedSearchFilter, LibraryViewModel.
  - **Related files:** AlbumRepository.kt, SongRepository.kt, core/model/ComposerNamePolicy.kt.
  - **Notes:** splitIntoAlbumArtists shared with SmartRepository; composers return empty below API 30 and apply minimum-song filtering only to the list, not detail lookup. `artistsForSongs()` reuses `splitIntoAlbums(..., sorted = false)` and the minimum-song preference.


- `data/local/repository/GenreRepository.kt` — Genres and their songs via MediaStore.Genres + RealGenreRepository.
  - **Purpose:** Genre queries via MediaStore.Genres.
  - **Responsibilities:** List genres with counts; genre songs incl. no-genre set; per-genre search.
  - **Inputs:** Genre ids, queries, Song.
  - **Outputs:** Genre lists, Song lists.
  - **Dependencies:** ContentResolver, RealSongRepository, GenreSortMode, SongSortMode.
  - **Consumed by:** RealRepository, RealSearchRepository, BasicSearchFilter.
  - **Related files:** SongRepository.kt, SearchRepository.kt.
  - **Notes:** genreId -1 represents songs without genre; that set uses `makeSongCursor(..., skipDurationFilter = true)` so it is not duration-filtered, matching the raw genre-member queries. Genre detail/search/count remain outside the duration filters.


- `data/local/repository/LyricsRepository.kt` - Lyrics parsing (file/embedded/stored/remote), per-song source selection, user-picked SAF document, encoding detection, LruCache, saving.
  - **Purpose:** Central lyrics acquisition and persistence.
  - **Responsibilities:** Parse file/embedded/stored/remote lyrics; encoding detection; LruCache; save/delete.
  - **Inputs:** Song, RawLyrics, preferences, download requests.
  - **Outputs:** SyncedLyrics, RawLyrics, save result, writable Uris.
  - **Dependencies:** LrcLyricsParser, TtmlLyricsParser, MetadataReader, MetadataWriter, LyricsDao, LyricsDownloadService, UniversalDetector.
  - **Consumed by:** LyricsViewModel; registered in MainModule.
  - **Related files:** LyricsEntity.kt, LyricsDao.kt, MetadataReader.kt.
  - **Notes:** Charset detection unless forced UTF-8.


- `data/local/repository/NetworkRepository.kt` — Last.fm/ListenBrainz login and scrobbling, artist/album info, Deezer and MusicBrainz lookups.
  - **Purpose:** Scrobbling and online metadata lookups.
  - **Responsibilities:** Last.fm/ListenBrainz login/logout/scrobble; artist/album info; Deezer and MusicBrainz queries.
  - **Inputs:** Credentials, tokens, Song, timestamps, search terms.
  - **Outputs:** LoginState Flows, ScrobblingResult, artist/album/recording models.
  - **Dependencies:** LastFmService, ListenBrainzService, DeezerService, MusicBrainzService, CryptoUtil, SharedPreferences.
  - **Consumed by:** RealRepository, scrobbling screens; registered in MainModule.
  - **Related files:** data/remote services, CryptoUtil.kt.
  - **Notes:** Sessions encrypted via Keystore and Base64-stored.


- `data/local/repository/PlaylistRepository.kt` - Device and Room playlists (create, rename, delete, favorites, songs).
  - **Purpose:** Device and Room playlist management.
  - **Responsibilities:** List device playlists/songs; Room CRUD; favorites playlist logic; bulk delete chunks; transactional create-with-songs and replace-contents for interactive import.
  - **Inputs:** Playlist ids, PlaylistEntity, Song lists.
  - **Outputs:** Playlists, PlaylistWithSongs, favorite state, transactional import results.
  - **Dependencies:** PlaylistDao, SsilencceDatabase (`withTransaction`), RealSongRepository, SortedCursorUtil, SongMapper.
  - **Consumed by:** RealRepository, LibraryViewModel.
  - **Related files:** PlaylistDao.kt, SongEntity.kt, SongMapper.kt, PlaylistImportNameResolver.kt.
  - **Notes:** Favorites is a Room playlist named via R.string.favorites_label. `createPlaylistWithSongsIfNameFree` checks the exact name and creates+inserts in one transaction; `replacePlaylistContentsIfEligible` revalidates ID/name/single-match/Favorites and replaces only song rows, preserving metadata. No unique-name index or migration.


- `data/local/repository/PlaylistImportNameResolver.kt` - Pure exact-name/Keep-both/Rename rules for interactive playlist import.
  - **Purpose:** Keep conflict naming deterministic without Android dependencies.
  - **Responsibilities:** `resolveName` returns the source name or the first free `source N` for N=1..100, else null; `validateRename` trims and classifies blank/duplicate/valid; exposes MAX_SUFFIX.
  - **Inputs:** Source name or rename text plus existing playlist names.
  - **Outputs:** Resolved name, null on suffix exhaustion, RenameValidation.
  - **Dependencies:** Kotlin standard library only.
  - **Consumed by:** LibraryViewModel; compiled probe.
  - **Related files:** LibraryResult.kt, LibraryViewModel.kt, PlaylistRepository.kt.
  - **Notes:** Names are exact/case-sensitive, matching the DAO's equality. Rename input is trimmed; source names are kept as provided.


- `data/local/repository/SearchRepository.kt` — Global search with grouped filters and scoped searches (genre/playlist/year/folder).
  - **Purpose:** Global and scoped search orchestration.
  - **Responsibilities:** Run filters or grouped mode searches; scoped genre/playlist/year/folder song searches.
  - **Inputs:** Context, SearchQuery, SearchFilter.
  - **Outputs:** Titled result lists (headers plus items).
  - **Dependencies:** RealAlbumRepository, RealSongRepository, RealArtistRepository, RealPlaylistRepository, GenreRepository, SpecialRepository.
  - **Consumed by:** RealRepository, SearchViewModel, BasicSearchFilter.
  - **Related files:** SearchQuery.kt, SearchFilter.kt, BasicSearchFilter.kt.
  - **Notes:** addTitled inserts localized headers between groups.


- `data/local/repository/SmartRepository.kt` - Smart content (top albums/artists, recent, never played), play counts, period statistics and history.
  - **Purpose:** Computed "smart" content and playback stats.
  - **Responsibilities:** Top/recent albums-artists; recent songs by cutoff; not-recently-played with the configurable custom-day threshold; play counts and history; period/all-time top songs and artist percentages via PlayEventDao/PlayCountDao; explicit start/end event ranges for automatic CSV; one-shot SongPlayStat with counts; Flow and suspend variants.
  - **Inputs:** ContentType, queries, cutoffs from Preferences, ListeningPeriod, start/end millis for bounded automatic periods.
  - **Outputs:** Song/Album/Artist lists, Flows, counts, ArtistPlayStat and SongPlayStat rows.
  - **Dependencies:** RealSongRepository, RealAlbumRepository, RealArtistRepository, HistoryDao, PlayCountDao, PlayEventDao, MediaQueryDispatcher.
  - **Consumed by:** RealRepository, LastAddedSearchFilter, HomeFragment, LibraryViewModel, StatisticsCsvExporter.
  - **Related files:** HistoryDao.kt, PlayCountDao.kt, PlayEventDao.kt, ListeningPeriod.kt, ArtistPlayStat.kt, SongPlayStat.kt, Repository.kt.
  - **Notes:** `fromPlayCountToSongs` and `fromHistoryToSongs` filter missing/deleted files read-only and never delete play-count or history rows; `songPlayStats` likewise preserves the screen's all-time aggregate/period-event ordering and 100-row cap while filtering missing files from returned counts. Explicit cleanup remains `Repository.deleteSong`/`deleteSongs` (which call the chunked history/play-count deletes) and `clearPlayCount`/`clearSongHistory`; no read path deletes. The 100-row `PLAY_STATS_LIMIT`/`HISTORY_LIMIT` is still applied before the missing-file filter, so a sparse window can return fewer than 100 and omit valid rows beyond the window; permanent missing-row cleanup is explicit only. Source `FC218112...` is implemented but not built into the current `66CC` APK; no runtime/feature PASS. Range overloads use `topSongsBetween`/`topArtistsBetween` with a start-inclusive, end-exclusive event window and the same recorded-artist percentage denominator; automatic CSV uses them without changing `ListeningPeriod`. Artist stats retain recorded-name/aggregate-ID snapshots even when current media is missing. One bound-parameter query resolves optional current artists; ambiguous/exact-case-mismatched results do not supply actions. Percentages use same-query full totals, including denominator-only Flow emissions outside the top 100. Resolution stays off Main and propagates cancellation; all-time retains legacy aggregate counts (ADR-040).


- `data/local/repository/SpecialRepository.kt` — Release years and filesystem folders/files with navigation.
  - **Purpose:** Release years and filesystem folder browsing.
  - **Responsibilities:** Group songs by year; build FileSystemQuery trees; folder navigation and searches.
  - **Inputs:** Years, paths, queries, recursive flag.
  - **Outputs:** ReleaseYears, Folders, FileSystemQuery.
  - **Dependencies:** RealSongRepository, StorageUtil, sortingCollator, YearSortMode.
  - **Consumed by:** RealRepository, RealSearchRepository, BasicSearchFilter, FoldersListFragment.
  - **Related files:** SongRepository.kt, FileSystemQuery.kt, Folder.kt.
  - **Notes:** musicFilesInPath recurses lazily per subfolder.


- `data/remote/KtorClient.kt` — Ktor/OkHttp HttpClient with 10 MB cache, cache-control and offline interceptors, JSON negotiation.
  - **Purpose:** Shared HTTP client factory for all remote services.
  - **Responsibilities:** Build OkHttpClient with three interceptors; create Ktor HttpClient with ContentNegotiation and ContentEncoding.
  - **Inputs:** Context (cache dir, package name); preconfigured OkHttpClient for jsonHttpClient.
  - **Outputs:** OkHttpClient (provideOkHttp), HttpClient (jsonHttpClient).
  - **Dependencies:** OkHttp Cache/CacheControl/Interceptor, Ktor OkHttp engine, kotlinx Json, NetworkFeature.isOnline.
  - **Consumed by:** MainModule (Koin), DeezerService, LastFmService, ListenBrainzService, MusicBrainzService, LyricsDownloadService, GitHubService.
  - **Related files:** MainModule, NetworkFeature, NetworkRepository.
  - **Notes:** 10 MB okhttp-cache; 4-day max-age, 28-day max-stale only for Deezer/Last.fm URLs.


- `data/remote/deezer/DeezerService.kt` — Deezer client: track/album/artist search.
  - **Purpose:** Query Deezer API for cover-art metadata.
  - **Responsibilities:** GET /search, /search/album, /search/artist with URL-encoded query parameters.
  - **Inputs:** artistName, title, name, limit, index; injected HttpClient.
  - **Outputs:** DeezerTrack, DeezerAlbum, DeezerArtist deserialized bodies.
  - **Dependencies:** Ktor HttpClient get/body, encodeURLParameter, Deezer models.
  - **Consumed by:** NetworkRepository (deezerTrack/deezerAlbum/deezerArtist), MainModule.
  - **Related files:** DeezerAlbum, DeezerArtist, DeezerTrack, Repository.
  - **Notes:** Responses cached 4 days by KtorClient cache interceptor.


- `data/remote/deezer/model/DeezerAlbum.kt` — Deezer album response model with best-image selection (Jaro-Winkler).
  - **Purpose:** Model album search results and select matching cover art.
  - **Responsibilities:** Parse album data list; score titles with Jaro-Winkler; pick image by requested size.
  - **Inputs:** Serialized JSON; requestedName, requestedImageSize (ImageSize).
  - **Outputs:** imageUrl or getBestImage URL String?.
  - **Dependencies:** kotlinx serialization, DeezerArtist.JW_SIMILARITY, normalize extension, ImageSize.
  - **Consumed by:** DeezerService, NetworkRepository, AudioCoverFetcher, TagEditorViewModel, AlbumTagEditorActivity.
  - **Related files:** DeezerArtist, DeezerTrack, AudioCoverFetcher.
  - **Notes:** 0.90 threshold; filters blank and "/images/cover//" placeholder URLs.


- `data/remote/deezer/model/DeezerArtist.kt` — Deezer artist model with getBestImage (0.90 similarity threshold).
  - **Purpose:** Model artist search results and match artist images.
  - **Responsibilities:** Hold result list and total; score with JaroWinklerSimilarity; select picture by size.
  - **Inputs:** JSON response; requestedName, requestedImageSize.
  - **Outputs:** Pair<Boolean, String?> matched flag plus image URL.
  - **Dependencies:** JaroWinklerSimilarity, normalize, ImageSize, kotlinx serialization.
  - **Consumed by:** DeezerService, NetworkRepository, ArtistImageFetcher (paged pagination).
  - **Related files:** DeezerAlbum (shares JW_SIMILARITY), ArtistImageFetcher.
  - **Notes:** Companion JW_SIMILARITY reused by DeezerAlbum for scoring.


- `data/remote/deezer/model/DeezerTrack.kt` — Deezer track model with album artwork by size.
  - **Purpose:** Model track search results exposing album cover art.
  - **Responsibilities:** Expose first track's album covers; select image by requested size.
  - **Inputs:** JSON response; requestedImageSize.
  - **Outputs:** imageUrl or getBestImage URL String?.
  - **Dependencies:** kotlinx serialization, ImageSize.
  - **Consumed by:** DeezerService, NetworkRepository, AudioCoverFetcher, TagEditorViewModel, SongTagEditorActivity.
  - **Related files:** DeezerAlbum, DeezerArtist, AudioCoverFetcher.
  - **Notes:** Filters "/images/artist//" placeholder URLs.


- `data/remote/github/GitHubService.kt` — GitHub releases query (stable/prerelease, optional token) with latest downloadable selection.
  - **Purpose:** Resolve newest downloadable GitHub release for updater.
  - **Responsibilities:** Fetch latest stable release; fetch paginated releases; filter prereleases by publishedAt.
  - **Inputs:** user, repo (default TuiCibrian/ssilencce), authToken, allowExperimental.
  - **Outputs:** GitHubRelease via latestRelease().
  - **Dependencies:** Ktor HttpClient, BuildConfig.GITHUB_API_URL, GitHubRelease.
  - **Consumed by:** UpdateViewModel, MainModule.
  - **Related files:** GitHubRelease, UpdateViewModel, UpdateDialog, UpdateSearchResult.
  - **Notes:** Falls back to stable when newer APK exists or no prerelease.


- `data/remote/github/model/GitHubRelease.kt` — GitHub release model: APK asset per ABI/flavor, version comparison, DownloadManager request.
  - **Purpose:** Model release payload and drive APK download logic.
  - **Responsibilities:** Detect APK assets by flavor/ABI; compare versions; build DownloadManager.Request; ignore-release flag.
  - **Inputs:** JSON release; Context; SharedPreferences via KoinComponent.
  - **Outputs:** hasApk, isNewer, getDownloadSize, getDownloadRequest, publishedAt.
  - **Dependencies:** kotlinx serialization, Parcelize, g00fy2 Version, KoinComponent, DownloadManager, BuildConfig.FLAVOR.
  - **Consumed by:** GitHubService, UpdateViewModel, UpdateDialog, UpdateSearchResult.
  - **Related files:** GitHubService, UpdateViewModel.
  - **Notes:** ABI priority matching via Build.SUPPORTED_ABIS with universal-APK fallback.


- `data/remote/lastfm/LastFmService.kt` — Last.fm client: album/artist info, signed mobile session (MD5), scrobble and now playing.
  - **Purpose:** Talk to Last.fm REST API for info and scrobbling.
  - **Responsibilities:** GET album/artist/user info; POST auth.getMobileSession, track.scrobble, track.updateNowPlaying with api_sig.
  - **Inputs:** names, language, cacheControl, username/password, session key sk, timestamp.
  - **Outputs:** LastFmAlbum, LastFmArtist, LastFmUserResponse, LastFmError or session/scrobble/nowplaying responses.
  - **Dependencies:** Ktor HttpClient get/submitForm, kotlinx Json, encodeMd5, BuildConfig.LASTFM_API_KEY/SECRET.
  - **Consumed by:** NetworkRepository, MainModule.
  - **Related files:** LastFmAlbum, LastFmArtist, LastFmError, LastFmSession, LastFmResponse, LastFmUser.
  - **Notes:** api_sig = MD5(sorted key-value pairs + secret); error detected via "\"error\"" in body.


- `data/remote/lastfm/model/LastFmAlbum.kt` — album.getInfo response model (wiki).
  - **Purpose:** Model album.getInfo response.
  - **Responsibilities:** Expose album wiki content.
  - **Inputs:** JSON from LastFmService.albumInfo.
  - **Outputs:** LastFmAlbum with nullable Album/Wiki.
  - **Dependencies:** kotlinx serialization.
  - **Consumed by:** LastFmService, NetworkRepository, Repository.
  - **Related files:** LastFmService, LastFmArtist.
  - **Notes:** Nested Album and Wiki classes.


- `data/remote/lastfm/model/LastFmArtist.kt` — artist.getInfo response model (bio).
  - **Purpose:** Model artist.getInfo response.
  - **Responsibilities:** Expose artist bio content.
  - **Inputs:** JSON from LastFmService.artistInfo.
  - **Outputs:** LastFmArtist with nullable Artist/Bio.
  - **Dependencies:** kotlinx serialization.
  - **Consumed by:** LastFmService, NetworkRepository, ArtistDetailViewModel, ArtistDetailFragment.
  - **Related files:** LastFmService, LastFmAlbum.
  - **Notes:** Nested Artist and Bio classes.


- `data/remote/lastfm/model/LastFmError.kt` — Last.fm API error model (message + code).
  - **Purpose:** Represent Last.fm API errors.
  - **Responsibilities:** Carry error message and numeric code.
  - **Inputs:** JSON error payload.
  - **Outputs:** LastFmError decoded by LastFmService or NetworkRepository.
  - **Dependencies:** kotlinx serialization.
  - **Consumed by:** LastFmService, NetworkRepository (toScrobblingResult).
  - **Related files:** LastFmService, NetworkRepository.
  - **Notes:** Selected when response text contains "\"error\"".


- `data/remote/lastfm/model/LastFmResponse.kt` — Scrobble/now-playing response models with ignoredMessage.
  - **Purpose:** Model scrobble and now-playing API responses.
  - **Responsibilities:** Parse accepted/ignored counts, corrected fields, ignoredMessage; ScrobbleListSerializer normalization.
  - **Inputs:** JSON from LastFmService lastfmPost calls.
  - **Outputs:** NowPlayingResponse, ScrobbleResponse.
  - **Dependencies:** kotlinx serialization, JsonTransformingSerializer, ListSerializer.
  - **Consumed by:** LastFmService, NetworkRepository.
  - **Related files:** LastFmService, LastFmError.
  - **Notes:** ScrobbleListSerializer wraps single scrobble object into a list.


- `data/remote/lastfm/model/LastFmSession.kt` — Last.fm session response (name, key, subscriber).
  - **Purpose:** Model auth.getMobileSession response.
  - **Responsibilities:** Hold session name, key, subscriber flag.
  - **Inputs:** JSON from createSession.
  - **Outputs:** LastFmSessionResponse wrapping LastFmSession.
  - **Dependencies:** kotlinx serialization.
  - **Consumed by:** LastFmService, NetworkRepository (LastFmSessionInfo persistence).
  - **Related files:** LastFmService, NetworkRepository.
  - **Notes:** Session key encrypted before storage by NetworkRepository.


- `data/remote/lastfm/model/LastFmUser.kt` — Last.fm user model (name, realname, url).
  - **Purpose:** Model user.getInfo response.
  - **Responsibilities:** Hold username, realname, profile URL.
  - **Inputs:** JSON from userInfo.
  - **Outputs:** LastFmUserResponse wrapping LastFmUser.
  - **Dependencies:** kotlinx serialization.
  - **Consumed by:** LastFmService, NetworkRepository (setLastfmSessionInfo).
  - **Related files:** LastFmService, NetworkRepository.
  - **Notes:** —.


- `data/remote/listenbrainz/ListenBrainzService.kt` — ListenBrainz client: token validation and listen submission.
  - **Purpose:** Client for ListenBrainz API.
  - **Responsibilities:** Validate token via GET; POST submit-listens with Authorization header.
  - **Inputs:** token, ListenBrainzSubmission.
  - **Outputs:** ListenBrainzTokenValidationResponse, ListenBrainzResponse.
  - **Dependencies:** Ktor HttpClient get/post/setBody, Constants.USER_AGENT.
  - **Consumed by:** NetworkRepository, MainModule.
  - **Related files:** ListenBrainzModels, NetworkRepository.
  - **Notes:** Base URL https://api.listenbrainz.org/1/.


- `data/remote/listenbrainz/model/ListenBrainzModels.kt` — ListenBrainz models (submission, listen, track metadata, validation responses).
  - **Purpose:** Serialization DTOs for ListenBrainz API.
  - **Responsibilities:** Define submission, listen, track metadata, additional info, status and validation responses.
  - **Inputs:** JSON request and response bodies.
  - **Outputs:** Typed ListenBrainz DTOs.
  - **Dependencies:** kotlinx serialization.
  - **Consumed by:** ListenBrainzService, NetworkRepository.
  - **Related files:** ListenBrainzService, NetworkRepository.
  - **Notes:** additional_info records media player name and version.


- `data/remote/lyrics/LyricsDownloadService.kt` — Orchestrates lyrics downloads from Lyrically/BetterLyrics/LRCLib per network features.
  - **Purpose:** Coordinate remote lyrics retrieval across providers.
  - **Responsibilities:** Iterate LyricsApi list by network feature; merge via RawLyrics.Remote.accept; clean title regexes.
  - **Inputs:** Song, title, artist, fromUser flag.
  - **Outputs:** RawLyrics.Remote (plain/synced/instrumental).
  - **Dependencies:** LyricallyApi, BetterLyricsApi, LrcLibApi, NetworkFeature, extractMainArtistName.
  - **Consumed by:** LyricsRepository, MainModule.
  - **Related files:** LyricsApi, RawLyrics, LyricsRepository.
  - **Notes:** Title cleanup patterns taken from Metrolist; stops once result.hasBoth.


- `data/remote/lyrics/api/LyricsApi.kt` — Common lyrics API interface (name, network feature, downloadLyrics).
  - **Purpose:** Contract implemented by all lyrics providers.
  - **Responsibilities:** Expose name, NetworkFeature.Lyrics gate, suspend downloadLyrics.
  - **Inputs:** Song, title, artist.
  - **Outputs:** RawLyrics.Remote? or null.
  - **Dependencies:** RawLyrics, NetworkFeature.
  - **Consumed by:** LyricallyApi, BetterLyricsApi, LrcLibApi, LyricsDownloadService.
  - **Related files:** LyricsDownloadService, RawLyrics.
  - **Notes:** —.


- `data/remote/lyrics/api/betterlyrics/BetterLyricsApi.kt` — BetterLyrics API (TTML) with timeouts.
  - **Purpose:** Fetch synced TTML lyrics from BetterLyrics endpoint.
  - **Responsibilities:** GET /getLyrics with s/a/d/al params; return synced content only on 200 with non-empty TTML.
  - **Inputs:** Song, cleaned title, artist.
  - **Outputs:** RawLyrics.Remote synced TTML or null.
  - **Dependencies:** Ktor HttpClient timeout plugin, NetworkFeature.Lyrics.BetterLyrics.
  - **Consumed by:** LyricsDownloadService.
  - **Related files:** LyricsApi, LyricsResponse (BetterLyricsResponse).
  - **Notes:** Timeouts 5s connect, 10s socket, 15s request.


- `data/remote/lyrics/api/lrclib/LrcLibApi.kt` — LRCLib API: search with close duration and plain-lyrics fallback.
  - **Purpose:** Fetch lyrics from LRCLib search API.
  - **Responsibilities:** Search by artist/title/album; match duration within 2 seconds; plain-lyrics fallback; instrumental flag.
  - **Inputs:** Song, cleaned title, artist.
  - **Outputs:** RawLyrics.Remote with plain, synced, instrumental.
  - **Dependencies:** Ktor HttpClient, USER_AGENT, timeout plugin, NetworkFeature.Lyrics.LRCLib.
  - **Consumed by:** LyricsDownloadService.
  - **Related files:** LyricsApi, LyricsResponse (LRCLibResponse).
  - **Notes:** Same 5/10/15-second timeout profile as BetterLyricsApi.


- `data/remote/lyrics/api/lyrically/LyricallyApi.kt` — Lyrically API: iTunes search with Jaro-Winkler scoring and TTML/LRC/ELRC parsing.
  - **Purpose:** Fetch synced lyrics via Lyrically API track IDs.
  - **Responsibilities:** iTunes search via PaxsenixSearchHelper; score top 5 results by Jaro-Winkler plus duration; parse TTML/LRC/ELRC to LRC.
  - **Inputs:** Song, cleaned title, artist.
  - **Outputs:** RawLyrics.Remote? with synced/plain content.
  - **Dependencies:** PaxsenixSearchHelper, JaroWinklerSimilarity, BuildConfig.LYRICALLY_API_URL, timeout plugin.
  - **Consumed by:** LyricsDownloadService.
  - **Related files:** SearchResponse, LyricsResponse, PaxsenixSearchHelper.
  - **Notes:** Based on Metrolist and SongSync; Syllable/Line types converted to LRC timestamps.


- `data/remote/lyrics/model/LyricsResponse.kt` — Lyrics response models (LRCLib, BetterLyrics, Lyrically).
  - **Purpose:** DTOs for the three lyrics providers.
  - **Responsibilities:** Model LRCLib search entries, BetterLyrics TTML, Lyrically line/syllable content.
  - **Inputs:** JSON from lyrics endpoints.
  - **Outputs:** Typed response objects for parsers.
  - **Dependencies:** kotlinx serialization.
  - **Consumed by:** LrcLibApi, BetterLyricsApi, LyricallyApi.
  - **Related files:** LrcLibApi, BetterLyricsApi, LyricallyApi.
  - **Notes:** Lyrically content supports background lines and multi-person turns.


- `data/remote/lyrics/model/SearchResponse.kt` — iTunes search response model (resultCount, trackId, duration).
  - **Purpose:** Model iTunes search API response.
  - **Responsibilities:** Hold resultCount and results with id, name, artist, album, durationInMillis.
  - **Inputs:** JSON from iTunes search endpoint.
  - **Outputs:** ITunesSearchResponse / ITunesSearchResult.
  - **Dependencies:** kotlinx serialization.
  - **Consumed by:** PaxsenixSearchHelper, LyricallyApi.
  - **Related files:** LyricallyApi, PaxsenixSearchHelper.
  - **Notes:** durationInMillis nullable for scoring.


- `data/remote/musicbrainz/MusicBrainzExt.kt` — Derives Cover Art Archive URL from a release.
  - **Purpose:** Extension to build cover artwork URLs.
  - **Responsibilities:** Compose coverartarchive.org/release/{id}/front from MusicBrainzRelease.
  - **Inputs:** MusicBrainzRelease.
  - **Outputs:** artworkUrl String.
  - **Dependencies:** COVER_ARCHIVE_URL, MusicBrainzRelease.
  - **Consumed by:** TagEditorViewModel (firstRelease?.artworkUrl).
  - **Related files:** MusicBrainzService, MusicBrainzModels.
  - **Notes:** —.


- `data/remote/musicbrainz/MusicBrainzService.kt` — MusicBrainz client: recording search and details with inc=.
  - **Purpose:** Query MusicBrainz web service for recording metadata.
  - **Responsibilities:** searchRecording with Lucene query; getRecordingDetails with inc= relations/releases/genres.
  - **Inputs:** title, artist, recording id.
  - **Outputs:** MusicBrainzSearchResponse, MusicBrainzRecording.
  - **Dependencies:** Ktor HttpClient, Constants.USER_AGENT.
  - **Consumed by:** NetworkRepository, MainModule.
  - **Related files:** MusicBrainzModels, MusicBrainzExt.
  - **Notes:** Exposes BASE_URL and COVER_ARCHIVE_URL constants.


- `data/remote/musicbrainz/model/MusicBrainzModels.kt` — MusicBrainz models (search response, recording, relations, works, genres, artist credits).
  - **Purpose:** Serialization DTOs for MusicBrainz API.
  - **Responsibilities:** Define search response, recording, relations, works, genres, credits, releases, media, tracks.
  - **Inputs:** JSON responses.
  - **Outputs:** Typed MusicBrainz DTOs.
  - **Dependencies:** kotlinx serialization.
  - **Consumed by:** MusicBrainzService, NetworkRepository, TagEditorViewModel, SongTagEditorActivity.
  - **Related files:** MusicBrainzService, MusicBrainzExt.
  - **Notes:** —.


- `src/shared/.../lyrically/PaxsenixSearchHelper.kt` — (shared variant) iTunes Search API client via Ktor used by the Lyrically provider.
  - **Purpose:** Real iTunes Search client backing LyricallyApi.
  - **Responsibilities:** GET itunes.apple.com/search; decode ITunesSearchResponse with lenient Json; browser User-Agent.
  - **Inputs:** songTitle, artistName.
  - **Outputs:** ITunesSearchResponse? or null on decode failure.
  - **Dependencies:** Ktor HttpClient, kotlinx Json, ITunesSearchResponse.
  - **Consumed by:** LyricallyApi.
  - **Related files:** LyricallyApi, SearchResponse, playstore stub.
  - **Notes:** limit 10, media music, entity song; Chrome desktop User-Agent.


- `src/playstore/.../lyrically/PaxsenixSearchHelper.kt` — (playstore variant) stub always returning null (no API access).
  - **Purpose:** Play-store stub disabling iTunes search.
  - **Responsibilities:** Provide getAppleMusicSearchResponse always returning null.
  - **Inputs:** songTitle, artistName (unused), injected HttpClient.
  - **Outputs:** null.
  - **Dependencies:** HttpClient, ITunesSearchResponse.
  - **Consumed by:** LyricallyApi.
  - **Related files:** LyricallyApi, shared PaxsenixSearchHelper variant.
  - **Notes:** Matches shared variant signature for flavor source-set swapping.

### playback/

- `playback/PlaybackService.kt` - MediaLibraryService owning the active local or Cast session player, equalizer, notifications, Bluetooth, widgets, audio focus, per-track resume, sleep timer, crossfade, A-B repeat and bounded session recovery (playback core).
  - **Purpose:** Central service hosting ExoPlayer, MediaLibrarySession, notifications and widget state.
  - **Responsibilities:** ExoPlayer/MediaSession setup, library browsing callbacks, custom commands, scrobbling, play counts, widget updates, receivers; AudioFocusController ownership; remembered track-position observer; serialized TrackPositionWriter enqueueing; seek-generation/manual-seek protection; session-recovery autoplay; named-queue switch command; explicit queue re-shuffle command using a fresh seeded order and existing saveState; queue block-move command with repeated single-item moves and stop/current remapping. Headset button mapping runs through HeadsetButtonController: counted single/double/triple/4+, optional one-shot Seek forward, bounded delay and best-effort long-press; headset preference changes reset the callback/count.
  - **Inputs:** MediaSession commands, SharedPreferences, MediaStoreObserver, AudioOutputObserver, Bluetooth/headset broadcasts.
  - **Outputs:** PlaybackState for Glance widgets, notifications, custom commands/events, persisted state, scrobbles, focus-ask event.
  - **Dependencies:** AdvancedForwardingPlayer, EqualizerManager, SleepTimer, AudioOutputObserver, AudioFocusController, TrackPositionDao, TrackPositionWriter, Repository, LibraryProvider, SsilencceRenderersFactory, processors. HeadsetButtonController, HeadsetButtonPolicy, HeadsetButtonAction.
  - **Consumed by:** MediaControllerOwner, WidgetExtension, PlaybackTileService, MainActivity (custom events), AndroidManifest service.
  - **Related files:** Playback.kt, MediaIDs.kt, PersistentStorage.kt, AudioFocusController.kt, TrackPositionDao.kt, TrackPositionWriter.kt, PlaybackTileService.kt, MainModule.kt. HeadsetButtonPolicy.kt, core/model/action/HeadsetButtonAction.kt, util/Preferences.kt.
  - **Notes:** Player runs on dedicated HandlerThread "Ssilencce-ExoPlayer"; foreground service timeout set. ExoPlayer's built-in focus handling is disabled because AudioFocusController owns it. Per-track writes are throttled for tracks ≥10 minutes and serialized through TrackPositionWriter; seek generations reject stale bookmark lookups. Pending Ask consent is request-bound, preserved by repeated play and synchronously cleared by explicit/no-op pause. Restore, saved-queue switch and focus state/consent commands require the app UID at advertisement and dispatch. Session recovery is opt-in via SESSION_RECOVERY and one-shot per process. PersistentStorage finishes its ordered queue snapshot before player release. A named-queue switch awaits a successful outgoing snapshot before binding/replacing the target; failure or an unstable timeline after bounded retries aborts it without mutating the departing binding/timeline (ADR-041, ADR-042, ADR-046, ADR-045). Only HEADSETHOOK and MEDIA_PLAY_PAUSE are counted; dedicated Next/Previous/FF/Rewind fall through to Media3. The headset callback owns its pending Runnable; updateEqualizerSessionState no longer cancels it, while preference changes and onDestroy do. Defaults preserve the legacy 1/2/3 behavior, 4+ off and 300 ms. The re-shuffle handler is app-UID-only, rejects shuffle-off/empty with ERROR_INVALID_STATE, starts the new ImprovedShuffleOrder at the current index and makes PersistentStorage.saveState() its only persistence call; it does not toggle shuffle or touch the timeline/repeat, named item list/order or active binding. The standard saveState path may refresh the active named queue's resume metadata (index/position/repeat/shuffle/updated_at) through PlaybackStateWriter/NamedQueueDao, which is existing ADR-041/045 behavior rather than a re-shuffle-specific snapshot write. The app-UID block-move handler validates parallel from/to arrays, applies repeated `moveMediaItem`, captures current/position/stop, remaps stop and current through the simulated old-to-new order, and persists the final full queue with `saveState(true)`; it does not touch shuffle/repeat or the named-queue binding.



- `playback/HeadsetButtonPolicy.kt` - Pure headset gesture policy and controller.
  - **Purpose:** Keep count-to-action, delay, immediate-dispatch and long-press state deterministic and Android-free.
  - **Responsibilities:** HeadsetButtonConfig; normalizeDelay (250/300/400/500/600, default 300); actionForCount; shouldDispatchImmediately (triple immediate when 4+ off, fourth when 4+ on); effectiveDelayMs (600 ms guard for a distinct long action); HeadsetButtonController press count/long latch and Dispatch/Schedule/Consumed decisions.
  - **Inputs:** HeadsetButtonAction config, count, configured delay, repeat signal.
  - **Outputs:** HeadsetButtonDecision for the service; action-per-count.
  - **Dependencies:** Kotlin standard library and HeadsetButtonAction only.
  - **Consumed by:** PlaybackService; compiled policy/controller probe.
  - **Related files:** core/model/action/HeadsetButtonAction.kt, PlaybackService.kt, util/Preferences.kt.
  - **Notes:** Default long press is Play/Pause (same as single), so hold remains a single press. Distinct long action raises the dispatch floor to 600 ms. No Android imports; no destructive/UI actions.

- `playback/AdvancedForwardingPlayer.kt` — ForwardingPlayer wrapper adding sequential timeline handling, upcoming-range realignment, fade/crossfade and mute play gates.
  - **Purpose:** Wrap ExoPlayer with upcoming-range queue semantics.
  - **Responsibilities:** Track listeners, mark upcoming MediaItems via extras, realign/clear ranges on discontinuities and moves; route explicit play/pause/setPlayWhenReady intent through the focus consent gate; intercept manual next/previous for crossfade, request pause fades, route the previous-button preference through `seekToPreviousMediaItem`, and gate/defer play while muted.
  - **Inputs:** Wrapped Player, addMediaItem(s)/moveMediaItem calls, sequentialTimelineEnabled flag, crossfade/fade/mute callbacks.
  - **Outputs:** MediaItems tagged `__is_upcoming__`/`__normal_index__`; adjusted queue mutations; crossfade/fade hooks.
  - **Dependencies:** ForwardingPlayer, ExoPlayer, MediaItem, KoinComponent.
  - **Consumed by:** PlaybackService, PersistentStorage.
  - **Related files:** PlaybackExtension.kt, PlaybackService.kt.
  - **Notes:** QUEUE_NEXT_MODE preference drives sequential timeline; internal listener on position discontinuity. Focus-internal pauses bypass the intent gate through the wrapped ExoPlayer (ADR-045).


- `playback/ImprovedShuffleOrder.kt` — Custom Media3 ShuffleOrder with random seed, atomic index and kotlinx-serialization persistence.
  - **Purpose:** Deterministic shuffle order with JSON persistence.
  - **Responsibilities:** Shuffled IntArray + inverse index, cloneAndInsert/Remove/Move/Clear, playerIndex AtomicInt, SerializedOrder, and `SerializedOrder.remap` for compacting a saved permutation onto a shrunk restored queue.
  - **Inputs:** firstIndex, length, randomSeed; ShuffleOrder mutation operations.
  - **Outputs:** ShuffleOrder instances; serialized JSON strings.
  - **Dependencies:** ShuffleOrder, Random, AtomicInt, kotlinx-serialization Json.
  - **Consumed by:** PlaybackService, PersistentStorage.
  - **Related files:** PlaybackService.kt, PersistentStorage.kt.
  - **Notes:** @UnstableApi; persisted via "shuffle_order" SharedPreferences key. `remap(savedToRestored)` filters the saved permutation by survivor positions, renumbers them, and returns null unless both the payload and mapping are valid permutations.


- `playback/QueueBatchMovePolicy.kt` - Pure block-move planner for queue multi-selection.
  - **Purpose:** Compute a correct timeline block move independently of Compose/Media3.
  - **Responsibilities:** Validate at least two selected timeline indices, reject selected/invalid targets, insert the block before/after the target, preserve relative order, compute repeated single-item moves from the correct end, return final timeline keys and an old-to-new index map.
  - **Inputs:** Timeline keys (`QueueSong.key`), selected timeline indices, target timeline index and drop direction.
  - **Outputs:** Ordered `Move(from, to)` list, final timeline keys, old-to-new index map, block range.
  - **Dependencies:** Kotlin standard library only.
  - **Consumed by:** PlayerViewModel block-move plan; compiled policy probe.
  - **Related files:** PlayerViewModel.kt, QueueScreen.kt, QueuePosition.kt, PlaybackService.kt.
  - **Notes:** Android-free. Exhaustively probed over permutations, subsets, targets and both drop directions; duplicate media IDs are represented by occurrence-aware keys.


- `playback/QueueReshufflePolicy.kt` - Pure availability rule for the explicit queue re-shuffle.
  - **Purpose:** Keep shuffle-off/empty-queue decisions deterministic and JVM-probeable.
  - **Responsibilities:** `canReshuffle(shuffleEnabled, mediaItemCount)` is true only when shuffle is already on and the queue has at least one item.
  - **Inputs:** Shuffle-enabled flag and media-item count.
  - **Outputs:** Boolean availability.
  - **Dependencies:** Kotlin standard library only.
  - **Consumed by:** PlaybackService command handler, PlayerViewModel, QueueScreen overflow menus, compiled policy probe.
  - **Related files:** PlaybackService.kt, PlayerViewModel.kt, QueueScreen.kt, ImprovedShuffleOrder.kt.
  - **Notes:** No Android imports; never enables shuffle implicitly.


- `playback/PersistentStorage.kt` - Persists queue, position, tri-state shuffle order, WAS_PLAYING and active named-queue state in Room/SharedPreferences and restores asynchronously; applies the notification artwork policy to restored items.
  - **Purpose:** Restore and save playback state across sessions.
  - **Responsibilities:** restoreState via QueueDao/Repository, debounced saveState, waitForRestoration/waitForMediaItems, RestorationState machine, occurrence-aware saved-to-restored mapping, shuffle-order remap/validation/fallback, active named-queue item/resume-state updates.
  - **Inputs:** AdvancedForwardingPlayer state, QueueDao, NamedQueueDao, Repository, "playback_state" SharedPreferences.
  - **Outputs:** MediaItemsWithStartPosition + ShuffleOrder; QueueEntity rows, named-queue rows and prefs.
  - **Dependencies:** QueueDao, NamedQueueDao, PlaybackStateWriter, Repository, AdvancedForwardingPlayer, SerializedOrder, CoroutineScope.
  - **Consumed by:** PlaybackService, playback resumption flow.
  - **Related files:** ImprovedShuffleOrder.kt, QueueEntity.kt, NamedQueueDao.kt, Repository.kt.
  - **Notes:** 500 ms debounce; skips saves while Restoring. Playlist intent survives cancellation: pause enqueues it immediately, and finish captures a final full snapshot before service-scope cancellation. All snapshots are captured on Main and written FIFO through PlaybackStateWriter; restoration awaits earlier writes. Occurrence-aware remapping preserves repeated ids and surviving shuffle positions; missing-current-item fallback resets position to 0. Invalid non-empty shuffle payloads are cleared and disable shuffle without emptying the restored queue; valid empty order preserves the setting. Named playlist saves atomically replace items and resume state; ordinary saves update resume metadata only. Binding/expected-media guards prevent old-timeline attribution; a committed-target marker keeps immediate post-switch edits attributed while pre-commit departing timelines stay excluded (ADR-012, ADR-041, ADR-045, ADR-046).


- `playback/Playback.kt` - Constants for custom MediaSession commands/events (shuffle/toggle/re-shuffle, seek forward/back, queue block move, repeat, favorite, playback restore, audio-focus ask/confirm/cancel, named-queue switch, stop position, A-B repeat, per-song params, ghost search).
  - **Purpose:** Single source of custom command/event names.
  - **Responsibilities:** Defines playback/session custom commands and event constants, including audio-focus and named-queue flow.
  - **Inputs:** —.
  - **Outputs:** Identifiers used in SessionCommand and broadcastCustomCommand.
  - **Dependencies:** None.
  - **Consumed by:** PlaybackService, PlayerViewModel, MainActivity, QueueScreen.
  - **Related files:** PlaybackService.kt, PlayerViewModel.kt, MainActivity.kt.
  - **Notes:** Includes SET_UNSHUFFLED_ORDER, SET_STOP_POSITION, SWITCH_NAMED_QUEUE, RESHUFFLE_QUEUE, MOVE_QUEUE_BLOCK (+ from/to extras) and token-bound CONFIRM/CANCEL/GET_AUDIO_FOCUS_STATE. Restore, named-queue switch, re-shuffle, block move and focus state/consent are advertised and dispatched only to the app UID (ADR-045).


- `playback/PlaybackExtension.kt` — Extensions for MediaSession/Player/MediaItem: queue extraction, MediaItem↔Song conversion, extras.
  - **Purpose:** Reusable Media3 conversions and helpers.
  - **Responsibilities:** getQueueItems timeline traversal, mediaItems property, toMediaItems, withExtras, song extension, isRemoteController.
  - **Inputs:** Player, MediaSession, List<Song>, MediaItem.
  - **Outputs:** QueueItem pairs, converted MediaItems, Bundle extras, Song.
  - **Dependencies:** Media3 Player/MediaSession/Timeline, Song model.
  - **Consumed by:** PlayerViewModel, LibraryProvider, PersistentStorage.
  - **Related files:** AdvancedForwardingPlayer.kt, PlaybackService.kt.
  - **Notes:** Song carried in MediaItem.localConfiguration.tag.


- `playback/ProgressObserver.kt` — Main-looper handler invoking a callback every 500 ms to observe playback progress.
  - **Purpose:** Periodic UI progress polling.
  - **Responsibilities:** start/stop lifecycle, self-reposting runnable, configurable interval.
  - **Inputs:** ProgressCallback, intervalMs (default 500).
  - **Outputs:** Repeated callback invocations on main looper.
  - **Dependencies:** Handler, Looper.
  - **Consumed by:** PlayerViewModel (intervalMs = 100).
  - **Related files:** PlayerViewModel.kt.
  - **Notes:** Simple handler; no coroutines.


- `playback/SleepTimer.kt` - AlarmManager-based sleep timer with countdown, song-count mode, fade-out, recent history and pending-quit option, exposing StateFlows.
  - **Purpose:** Schedule playback stop after configurable time.
  - **Responsibilities:** Exact alarm set/cancel, OnAlarmListener dispatch, pendingQuit consumption, CountDownTimer updater, StateFlows.
  - **Inputs:** millisInFuture, allowPendingQuit, fadeOut, fadeDuration; finish listeners.
  - **Outputs:** SleepParams callbacks, SleepTimerWaitingFor states.
  - **Dependencies:** AlarmManager, CountDownTimer, StateFlow, SleepTimerWaitingFor.
  - **Consumed by:** PlaybackService, SleepTimerViewModel, MainModule.
  - **Related files:** SleepTimerScreen.kt, SleepTimerViewModel.kt.
  - **Notes:** ELAPSED_REALTIME_WAKEUP; Android 12+ canScheduleExactAlarms permission.


- `playback/SleepTimerEndTime.kt` - Pure local wall-clock end-time resolver.
  - **Purpose:** Convert a selected 24-hour `HH:mm` into the next strictly future delay.
  - **Responsibilities:** Resolve today's target, roll to tomorrow when equal/past, account for seconds and midnight, format `HH:mm`, default next full hour; use `ZonedDateTime.of` zone rules.
  - **Inputs:** `ZonedDateTime now`, hour, minute.
  - **Outputs:** delay milliseconds, `HH:mm` string, next-full-hour pair.
  - **Dependencies:** java.time only.
  - **Consumed by:** SleepTimerViewModel; compiled helper probe.
  - **Related files:** SleepTimerViewModel.kt, SleepTimer.kt.
  - **Notes:** DST gaps resolve forward and overlaps use the earlier offset by java.time rules; no clock mutation. If the resolved instant is not after now, the next local date is used.


- `playback/equalizer/EqualizerManager.kt` — Central manager applying EQ/balance/replay-gain/bass effects per output device, persisted in DataStore.
  - **Purpose:** Orchestrate all audio-effect state and engine application.
  - **Responsibilities:** eqDataStore persistence, state flows, engine lifecycle, profile CRUD/import, AutoEq, exact/type device selection and overlap normalization.
  - **Inputs:** AudioOutputObserver, audioSessionId, DataStore prefs, EqProfile/AutoEqProfile.
  - **Outputs:** Engine/processor config, StateFlows for UI, AudioEffect session broadcasts.
  - **Dependencies:** EQEngine, BasicEQEngine, DynamicsProcessingEngine, processors, AudioOutputObserver, EqOutputAssociationPolicy, AudioEffect.
  - **Consumed by:** PlaybackService, EqualizerViewModel.
  - **Related files:** EQEngine.kt, EqualizerFragment.kt, MainModule.kt.
  - **Notes:** DataStore "equalizer"; EQ disabled under bit-perfect/audio offload; pro mode gates compressor/limiter. Exact key wins over type; no match preserves the current profile. AudioDevice/profiles/ready state are combined; redundant profile application is skipped; add/edit remove overlapping type/key claims.


- `playback/equalizer/engine/EQEngine.kt` — Abstract base wrapping audiofx effects (BassBoost/Virtualizer/LoudnessEnhancer) with safe operations.
  - **Purpose:** Shared audiofx effect ownership and safe wrappers.
  - **Responsibilities:** Creates BassBoost/Virtualizer/LoudnessEnhancer; set*State methods; release; execOpCatching synchronized guards.
  - **Inputs:** sessionId, BassBoostState, VirtualizerState, LoudnessGainState.
  - **Outputs:** Applied effect states; abstract EQ operations for subclasses.
  - **Dependencies:** android.media.audiofx BassBoost/Virtualizer/LoudnessEnhancer.
  - **Consumed by:** EqualizerManager, subclasses.
  - **Related files:** BasicEQEngine.kt, DynamicsProcessingEngine.kt.
  - **Notes:** All audiofx operations guarded, null on failure.


- `playback/equalizer/engine/BasicEQEngine.kt` — EQEngine implementation based on the classic android.media.audiofx.Equalizer.
  - **Purpose:** Legacy Equalizer-based engine.
  - **Responsibilities:** Band capabilities, setEnabled, setProfile/setBandGain in millibels, release.
  - **Inputs:** sessionId, EqProfile, band gains.
  - **Outputs:** EqBandCapabilities, applied band levels.
  - **Dependencies:** android.media.audiofx.Equalizer, EQEngine.
  - **Consumed by:** EqualizerManager.
  - **Related files:** EQEngine.kt, DynamicsProcessingEngine.kt.
  - **Notes:** No MBC/limiter; setBandCount and setInputGain unsupported.


- `playback/equalizer/engine/DynamicsProcessingEngine.kt` — EQEngine using DynamicsProcessing (API 28+) with multiband compressor/limiter.
  - **Purpose:** Modern engine with MBC and limiter.
  - **Responsibilities:** Config builder, band capabilities (5/10/15/32), preEq profile/gain, MBC/limiter application, band-count re-creation.
  - **Inputs:** sessionId, bandCount, EqProfile, CompressorState, LimiterState.
  - **Outputs:** Applied DynamicsProcessing config, EqBandCapabilities.
  - **Dependencies:** android.media.audiofx.DynamicsProcessing (API 28+), EQEngine.
  - **Consumed by:** EqualizerManager (DynamicsProcessing mode).
  - **Related files:** EQEngine.kt, BasicEQEngine.kt.
  - **Notes:** @RequiresApi(P); two channels; VARIANT_FAVOR_FREQUENCY_RESOLUTION; ISO band frequencies.


- `playback/library/LibraryProvider.kt` — Resolves MediaItems for playback (including AAOS paths, folders, composers, current queue and search results) via the repository.
  - **Purpose:** Bridge repository data into MediaLibrary browsing/playback.
  - **Responsibilities:** getMediaItemsForPlayback, getMediaItemsForAAOSPlayback, getChildren/getItem/search; folder flat/hierarchical listings; composer and current-queue resolution; always-available Auto roots.
  - **Inputs:** MediaItem lists, mediaId paths, search query, current queue snapshot, Repository.
  - **Outputs:** Resolved/playable MediaItems, browsable children, searchResult.
  - **Dependencies:** Repository, MediaIDs, MediaPathCodec, CoverProvider URIs, Preferences.
  - **Consumed by:** PlaybackService.
  - **Related files:** MediaIDs.kt, MediaPathCodec.kt, PlaybackService.kt.
  - **Notes:** AAOS paths handle Android Auto controllers; internal SEARCH prefix. Folder ids embed URL-safe Base64 paths; composers require API 30+.


- `playback/library/MediaIDs.kt` — Constants/helpers for hierarchical media ID paths (ROOT, SONGS, ALBUMS, FOLDERS, COMPOSERS, CURRENT_QUEUE...).
  - **Purpose:** Canonical media ID path scheme.
  - **Responsibilities:** Category constants; getPathId/splitPath/isPath helpers with ":" separator.
  - **Inputs:** parentId, mediaId (Long/String), path strings.
  - **Outputs:** Path IDs and split segments.
  - **Dependencies:** None.
  - **Consumed by:** LibraryProvider, PlaybackService, MainActivity.
  - **Related files:** LibraryProvider.kt, MediaPathCodec.kt.
  - **Notes:** RECENT_SONGS uses "HISTORY"; LAST_ADDED/FAVORITES for shortcuts. FOLDERS, COMPOSERS and CURRENT_QUEUE are Auto browsing roots.


- `playback/processor/BalanceAudioProcessor.kt` — Media3 BaseAudioProcessor applying per-channel gain to 16-bit PCM.
  - **Purpose:** Stereo balance adjustment in audio pipeline.
  - **Responsibilities:** setBalance(left,right), PCM-16 validation, per-channel gain with clamping; mono averages gains.
  - **Inputs:** PCM ByteBuffer, leftGain/rightGain.
  - **Outputs:** Gain-adjusted PCM ByteBuffer.
  - **Dependencies:** BaseAudioProcessor, C.ENCODING_PCM_16BIT.
  - **Consumed by:** SsilencceRenderersFactory, EqualizerManager, PlaybackService.
  - **Related files:** SsilencceRenderersFactory.kt, ReplayGainAudioProcessor.kt.
  - **Notes:** Single shared instance; synchronized setBalance.


- `playback/processor/ByteUtils.kt` — Helpers for reading/writing signed 24-bit integers from/to ByteBuffer.
  - **Purpose:** Little-endian 24-bit PCM access.
  - **Responsibilities:** getInt24/putInt24, Int24 min/max constants.
  - **Inputs:** ByteBuffer, Int sample.
  - **Outputs:** 24-bit ints, written bytes.
  - **Dependencies:** java.nio.ByteBuffer.
  - **Consumed by:** ReplayGainAudioProcessor.
  - **Related files:** ReplayGainAudioProcessor.kt.
  - **Notes:** Pure utility object.


- `playback/processor/ReplayGainAudioProcessor.kt` - Media3 BaseAudioProcessor applying presence-aware ReplayGain (album/track mode, with/without-tag preamp, peak limiting and gain clamp) to 16/24-bit PCM.
  - **Purpose:** Normalize loudness via ReplayGain tags.
  - **Responsibilities:** Gain computation (album/track, preamp, peak limit), queueInput scaling 16/24-bit, passthrough at zero gain.
  - **Inputs:** ReplayGain (ReplayGainTagExtractor), mode, preAmpGain, PCM buffers.
  - **Outputs:** Scaled PCM ByteBuffer.
  - **Dependencies:** BaseAudioProcessor, ReplayGain/ReplayGainMode, ByteUtils.
  - **Consumed by:** SsilencceRenderersFactory, EqualizerManager, PlaybackService.
  - **Related files:** SsilencceRenderersFactory.kt, ByteUtils.kt, ReplayGainTagExtractor.kt.
  - **Notes:** currentGain set on media item transition; dB→linear via 10^(gain/20); peak limiting is delegated to ReplayGainMath.limitByPeak and applies to any finite positive peak, including >1.


- `playback/renderer/AlacWorkaroundCodecSelector.kt` — MediaCodecSelector filtering hardware ALAC decoders to avoid broken high-bitrate M4A playback.
  - **Purpose:** Force stable software ALAC decoding.
  - **Responsibilities:** Delegates to DEFAULT selector; filters hardwareAccelerated infos for AUDIO_ALAC.
  - **Inputs:** mimeType, requiresSecureDecoder, requiresTunnelingDecoder.
  - **Outputs:** List<MediaCodecInfo> (software ALAC only).
  - **Dependencies:** MediaCodecSelector.DEFAULT, MimeTypes, MediaCodecInfo.
  - **Consumed by:** PlaybackService (ExoPlayer).
  - **Related files:** SsilencceRenderersFactory.kt.
  - **Notes:** Falls back to extension decoders (FFmpeg) if none.


- `playback/renderer/SsilencceRenderersFactory.kt` - DefaultRenderersFactory subclass injecting balance, mono and ReplayGain processors into DefaultAudioSink and wrapping it in the gapless gate.
  - **Purpose:** Wire custom audio processors into playback.
  - **Responsibilities:** buildAudioSink with both processors, float-output/playback-params passthrough.
  - **Inputs:** Context, BalanceAudioProcessor, ReplayGainAudioProcessor.
  - **Outputs:** DefaultAudioSink with processor array.
  - **Dependencies:** DefaultRenderersFactory, DefaultAudioSink.
  - **Consumed by:** PlaybackService (ExoPlayer builder).
  - **Related files:** BalanceAudioProcessor.kt, ReplayGainAudioProcessor.kt.
  - **Notes:** Same MainModule-injected processor instances app-wide.


- `playback/shuffle/ShuffleManager.kt` — Implements smart/weighted shuffle modes (pure random, most played, favorites, forgotten, recent, combined, by-group) plus album-artist/composer most-played strategies.
  - **Purpose:** Generate smart shuffle orders from play stats.
  - **Responsibilities:** Weighted shuffle modes via ln weights, combined scoring, expandSongsBatch enrichment, shuffleByProvider group modes, and album-artist/composer most-played weighting through ShuffleGroupKeyPolicy.
  - **Inputs:** List<Song>, SpecialShuffleMode, providers, GroupShuffleMode, SongSortMode, Repository stats.
  - **Outputs:** Shuffled List<Song>.
  - **Dependencies:** Repository, ExpandedSong, ShuffleGroupKeyPolicy, SpecialShuffleMode, GroupShuffleMode, SongSortMode, SongProvider.
  - **Consumed by:** PlayerViewModel.
  - **Related files:** PlayerViewModel.kt, core model shuffle modes.
  - **Notes:** Scores ln(r)/weight sorted descending; falls back to shuffled on failure. Album-artist/composer weights use ln(summed group play count + 1) with the same 1.0 base weight as the existing artist/album modes.

### coil/

- `coil/CoilBitmapLoader.kt` — Media3 BitmapLoader backed by Coil's SingletonImageLoader for notification bitmaps.
  - **Purpose:** Decode Media3 notification artwork into bitmaps.
  - **Responsibilities:** Checks bitmap mime types, decodes bytes, loads Uris asynchronously via ListenableFuture.
  - **Inputs:** ByteArray, Uri, mimeType.
  - **Outputs:** ListenableFuture<Bitmap> through CallbackToFutureAdapter.
  - **Dependencies:** Media3 BitmapLoader, coil3 SingletonImageLoader, CachePolicy, ContextCompat.
  - **Consumed by:** PlaybackService, wrapped in media3 CacheBitmapLoader.
  - **Related files:** PlaybackService.kt, App.kt (newImageLoader).
  - **Notes:** 1024 px max, memory cache disabled, request disposed on cancellation.


- `coil/CoilExtension.kt` — Coil extensions and default placeholder drawables for loading song/album/artist/playlist images.
  - **Purpose:** Simplify legacy ImageView image loading with typed helpers.
  - **Responsibilities:** Defines default drawable constants, placeholder/error builder, per-entity load functions.
  - **Inputs:** Song, Album, Artist, PlaylistWithSongs, ImageView.
  - **Outputs:** Coil ImageRequest enqueued on ImageView.
  - **Dependencies:** coil3 load, R.drawable resources, getPlaceholderDrawable.
  - **Consumed by:** MiniPlayerFragment, AlbumDetailFragment, ArtistDetailFragment, PlaylistAdapter, SearchAdapter.
  - **Related files:** ui/component/views getPlaceholderDrawable, MapperStore.
  - **Notes:** Placeholder doubles as error drawable.


- `coil/CoverProvider.kt` — ContentProvider exposing content:// URIs resolving cached covers for albums/artists/playlists/genres.
  - **Purpose:** Serve cached cover images to external consumers via content URIs.
  - **Responsibilities:** UriMatcher routing, Repository entity lookup, Coil rendering, JPEG caching, fallback art.
  - **Inputs:** content://com.ssilencce.cover URIs with entity id.
  - **Outputs:** ParcelFileDescriptor for cached cover or fallback JPEG.
  - **Dependencies:** Repository, SingletonImageLoader, Koin, UriMatcher, BitmapFactory.
  - **Consumed by:** LibraryProvider, Song.kt, PreferencesScreenFragment.
  - **Related files:** LibraryProvider.kt, Song.kt, model/AutoGeneratedImage.kt.
  - **Notes:** 7-day cache expiry, 1024 px, genre covers auto-generated. Album-artist cache names are
    `4_<lowercase SHA-256 hex>.jpg`; numeric routes keep `1_123.jpg`/`2_456.jpg`; `containedCacheFile`
    canonicalizes `covers` and requires parent equality. This is boundary hardening for the raw
    album-artist path, not a proven arbitrary read/write or security PASS; an actual escape needs a
    pre-existing `covers/4_<first-component>` directory this code never creates. Legacy raw-name
    entries cold-miss and are not migrated; `clearCache` deletes direct children only. Source
    `79A78FB4...` is implemented but not built into the current `66CC` APK; no runtime PASS.
    `getType` returns `image/*` for every UriMatcher-recognized cover path and `null` for unknown
    URIs, so album/artist/playlist/genre MIME consumers do not throw.


- `coil/CustomArtistImageManager.kt` — Saves chosen artist images to file, registers in SharedPreferences and notifies MediaStore.
  - **Purpose:** Persist user-chosen custom artist images.
  - **Responsibilities:** Checks, gets, sets, removes custom files; tracks flags/signatures in SharedPreferences.
  - **Inputs:** Artist, ArtistImage, Uri.
  - **Outputs:** Boolean success, File path, signature string.
  - **Dependencies:** SingletonImageLoader, FileUtil, SharedPreferences, MediaStore Artists.
  - **Consumed by:** ArtistImageFetcher, ArtistImageKeyer, TagEditorViewModel.
  - **Related files:** ArtistTagEditorActivity, MainModule, FileUtil.
  - **Notes:** 2048 px JPEG, notifies Artists.EXTERNAL_CONTENT_URI on changes.


- `coil/CustomPlaylistImageManager.kt` — Creates/deletes custom playlist covers (reduced JPGs in the app files dir).
  - **Purpose:** Manage custom playlist cover image files.
  - **Responsibilities:** Decodes picked Uri via Coil, writes reduced 1080 px JPEG, deletes files.
  - **Inputs:** selectedUri String, imageUri.
  - **Outputs:** New file Uri or null, deletion Boolean.
  - **Dependencies:** SingletonImageLoader, FileUtil, toJPG extension.
  - **Consumed by:** LibraryViewModel, MainModule.
  - **Related files:** LibraryViewModel.kt, FileUtil.kt.
  - **Notes:** Filename from System.currentTimeMillis, memory cache disabled.


- `coil/fetcher/ArtistImageFetcher.kt` — Coil fetcher resolving artist images from custom files, Deezer or MediaStore art.
  - **Purpose:** Fetch artist artwork for Coil requests.
  - **Responsibilities:** Prefers custom file, pages through Deezer artist search, falls back to MediaStore stream.
  - **Inputs:** ArtistImage, Options, preferred image size.
  - **Outputs:** SourceFetchResult or delegated fetcher result.
  - **Dependencies:** CustomArtistImageManager, Repository.deezerArtist, ContentResolver, ImageLoader components.
  - **Consumed by:** App.kt ImageLoader components.
  - **Related files:** ArtistImageKeyer, ArtistMapper, Repository.
  - **Notes:** Max 20 results, skips unknown names when network disabled.


- `coil/fetcher/AudioCoverFetcher.kt` — Coil fetcher loading embedded/folder art (via TagLib) or MediaStore, falling back to Deezer.
  - **Purpose:** Fetch song/album cover artwork.
  - **Responsibilities:** Reads folder art/TagLib front cover or MediaStore album art, falls back to Deezer album/track.
  - **Inputs:** AudioCover, Options, preferred image size.
  - **Outputs:** SourceFetchResult or null.
  - **Dependencies:** TagLib, AudioCoverUtils, Repository.deezerAlbum/deezerTrack, asAlbumCoverUri.
  - **Consumed by:** App.kt ImageLoader components.
  - **Related files:** AudioCoverKeyer, SongMapper, AlbumMapper, AudioCoverUtils.
  - **Notes:** Deezer only when network enabled and artist known.


- `coil/fetcher/AutoGeneratedImageFetcher.kt` — Coil fetcher generating genre/year images by combining album covers.
  - **Purpose:** Produce mosaic images for genres and years.
  - **Responsibilities:** Loads songs by genre/year via Repository, delegates mosaic construction.
  - **Inputs:** AutoGeneratedImage, Options.
  - **Outputs:** ImageFetchResult or null.
  - **Dependencies:** Repository.songsByGenre, Repository.songsByYear, AutoGeneratedImageUtil.
  - **Consumed by:** App.kt ImageLoader components.
  - **Related files:** AutoGeneratedImageKeyer, GenreMapper, YearMapper.
  - **Notes:** Returns null for id -1; dataSource DISK.


- `coil/fetcher/PlaylistImageFetcher.kt` — Coil fetcher loading the custom playlist cover or an auto-generated mosaic.
  - **Purpose:** Fetch playlist cover image.
  - **Responsibilities:** Loads custom cover via nested Coil request, falls back to auto-generated mosaic.
  - **Inputs:** PlaylistImage, Size.
  - **Outputs:** ImageFetchResult or null.
  - **Dependencies:** SingletonImageLoader, AutoGeneratedImageUtil, PlaylistEntity.customCoverUri.
  - **Consumed by:** App.kt ImageLoader components.
  - **Related files:** PlaylistImageKeyer, PlaylistMapper, AutoGeneratedImageUtil.
  - **Notes:** Returns null for PlaylistEntity.Empty; custom cover failure falls back.


- `coil/model/ArtistImage.kt` — Request data model (id, coverUri, name) for artist images.
  - **Purpose:** Coil request payload for artist artwork.
  - **Responsibilities:** Holds id/coverUri/name, computes isNameUnknown flag.
  - **Inputs:** Long id, Uri, String name.
  - **Outputs:** toString debug representation.
  - **Dependencies:** isArtistNameUnknown extension.
  - **Consumed by:** ArtistMapper, ArtistImageFetcher, ArtistImageKeyer.
  - **Related files:** ArtistImageFetcher.kt, MapperStore.kt.
  - **Notes:** —.


- `coil/model/AudioCover.kt` — Request data model (albumId, uri, path, artist/album/title, flags) for song/album covers.
  - **Purpose:** Coil request payload for song/album covers.
  - **Responsibilities:** Bundles metadata and MediaStore/TagLib behavior flags.
  - **Inputs:** albumId, uri, path, names, lastModified, three boolean flags.
  - **Outputs:** toString debug representation.
  - **Dependencies:** —.
  - **Consumed by:** AudioCoverFetcher, AudioCoverKeyer, Song/Album/File mappers.
  - **Related files:** MapperStore.kt, AudioCoverFetcher.kt.
  - **Notes:** isAlbum, isIgnoreMediaStore, isUseFolderArt drive fetcher paths.


- `coil/model/AutoGeneratedImage.kt` — Request model (type, id, songCount) for a mosaic image of a genre or year.
  - **Purpose:** Coil request payload for generated mosaic images.
  - **Responsibilities:** Identifies genre/year type and id for fetching.
  - **Inputs:** type, id, songCount.
  - **Outputs:** toString debug representation.
  - **Dependencies:** —.
  - **Consumed by:** GenreMapper, YearMapper, AutoGeneratedImageFetcher, CoverProvider.
  - **Related files:** AutoGeneratedImageFetcher.kt, AutoGeneratedImageKeyer.
  - **Notes:** Constants TYPE_GENRE=1, TYPE_YEAR=2.


- `coil/model/PlaylistImage.kt` — Request model (playlistEntity, songs) for a playlist cover.
  - **Purpose:** Coil request payload for playlist covers.
  - **Responsibilities:** Bundles PlaylistEntity and songs for custom/mosaic rendering.
  - **Inputs:** PlaylistEntity, List<Song>.
  - **Outputs:** toString debug representation.
  - **Dependencies:** PlaylistEntity, Song.
  - **Consumed by:** PlaylistMapper, PlaylistImageFetcher, PlaylistImageKeyer.
  - **Related files:** PlaylistMapper.kt, PlaylistImageFetcher.kt.
  - **Notes:** —.


- `coil/store/KeyerStore.kt` — Coil Keyer implementations producing stable cache keys for the image models.
  - **Purpose:** Provide deterministic cache keys per image model.
  - **Responsibilities:** Builds keys for AudioCover, ArtistImage, PlaylistImage, AutoGeneratedImage.
  - **Inputs:** Model data plus Options.
  - **Outputs:** String cache keys.
  - **Dependencies:** CustomArtistImageManager (ArtistImageKeyer).
  - **Consumed by:** App.kt ImageLoader components registration.
  - **Related files:** MapperStore.kt, App.kt.
  - **Notes:** Artist key includes signature; playlist key includes song count.


- `coil/store/MapperStore.kt` — Coil Mapper implementations converting Song/Album/Artist/Playlist/Genre/Year/FileSystemItem to image models.
  - **Purpose:** Convert domain entities into Coil request models.
  - **Responsibilities:** Maps seven entity types honoring IGNORE_MEDIA_STORE/USE_FOLDER_ART prefs.
  - **Inputs:** Song, Album, Artist, PlaylistWithSongs, Genre, ReleaseYear, FileSystemItem.
  - **Outputs:** AudioCover, ArtistImage, PlaylistImage, AutoGeneratedImage.
  - **Dependencies:** SharedPreferences, toSongs, albumArtistName, safeGetFirstSong/Album.
  - **Consumed by:** App.kt ImageLoader components.
  - **Related files:** KeyerStore.kt, model/ classes.
  - **Notes:** AlbumMapper/FileMapper source data from first song.


- `coil/util/AudioCoverUtils.kt` — Locates fallback folder art files (cover.jpg, folder.png...) next to the audio file.
  - **Purpose:** Find folder art files near audio files.
  - **Responsibilities:** Probes nine fallback filenames in parent directory when folder art enabled.
  - **Inputs:** Audio file path, useFolderImage flag.
  - **Outputs:** InputStream or null.
  - **Dependencies:** java.io.File.
  - **Consumed by:** AudioCoverFetcher.
  - **Related files:** AudioCoverFetcher.kt.
  - **Notes:** Throws IOException; no network involved.


- `coil/util/AutoGeneratedImageUtil.kt` — Builds a mosaic bitmap from the album covers of a song list.
  - **Purpose:** Produce auto-generated cover images for playlists/genres/years.
  - **Responsibilities:** Loads up to 9 distinct album covers, joins via MergedImageUtils, wraps as Image.
  - **Inputs:** Context, List<Song>.
  - **Outputs:** Coil Image or null.
  - **Dependencies:** SingletonImageLoader, asAlbumCoverUri, RoundedCornersTransformation, MergedImageUtils.
  - **Consumed by:** AutoGeneratedImageFetcher, PlaylistImageFetcher.
  - **Related files:** MergedImageUtils.kt, fetcher/ classes.
  - **Notes:** Single song yields plain cover; memory key playlist-preview.


- `coil/util/MergedImageUtils.kt` — Disposes and merges up to 9 cover bitmaps into a rotated 1600 px collage.
  - **Purpose:** Merge album cover bitmaps into a decorative collage.
  - **Responsibilities:** Arranges duplicates for 1-8 covers, draws 3x3 grid, rotates 9 degrees, crops.
  - **Inputs:** List<Bitmap> up to 9 items.
  - **Outputs:** Single cropped 1600 px Bitmap.
  - **Dependencies:** android.graphics Bitmap/Canvas/Matrix, core-ktx createBitmap/scale.
  - **Consumed by:** AutoGeneratedImageUtil.
  - **Related files:** AutoGeneratedImageUtil.kt.
  - **Notes:** Recycles intermediate bitmaps; internal object.

### util/ and extensions/

- `util/BackupHelper.kt` - Creates/restores ZIP backups with settings, playlists, favorites, play counts, named queues, per-file positions, per-song playback params, lyrics, song notes and custom artist images; returns strict creation results, preserves empty playlist snapshots and drives restore accounting.
  - **Purpose:** ZIP backup and restore of app data (settings, playlists, favorites, play counts, named queues, per-file resume positions, lyrics, artist images).
  - **Responsibilities:** Zip non-favorites playlist M3Us (including a valid `#EXTM3U` entry for legitimate empty playlists), a dedicated favorites M3U, settings XML, lyrics JSON (`LyricsEntity` list), custom artist images, play-count JSON, named-queue JSON and track-position JSON; exclude favorites from the generic playlist set; propagate unexpected playlist write failures; restore each entry type selected via `BackupContent`; enforce entry/size limits while streaming; expose `createBackupFile`/`BackupCreationResult` for the scheduled worker and the manual share flow; retain the legacy `createBackup` toast wrapper without a current caller; drive `RestoreSummary`/`RestoreOutcome`; clean cached playlist/favorites dirs. Playlist restore uses a separate non-interactive merge-into-existing policy: an exact-name existing playlist receives the backup's resolvable songs without the device-import conflict dialog or suffix/Replace/Rename choices. Favorites restore merges missing songs into the current localized Favorites playlist and does not duplicate existing entries. Play counts merge by max play/skip/time values. Named queues preserve or replace ids, enforce the 100-queue cap and resolve name collisions. Per-file positions keep the newer `updated_at`. Settings restore parses the untrusted XML into a typed map before mutation, rejects DTD/unknown/trailing content, and applies a whole-map clear/put/commit through the app's cached default `SharedPreferences` so the live singleton cannot write stale values back. A successful settings import returns `true` to trigger Settings UI recreation.
  - **Inputs:** Context, backup Uri, `BackupContent` selection, injected `Repository`, `LyricsDao`, `PlayCountDao`, `TrackPositionDao` and `NamedQueueDao`.
  - **Outputs:** `.bmgbak` ZIP via `ZipOutputStream`; restored playlists, favorites, play counts, queues, positions, prefs, lyrics, images; `BackupCreationResult`; legacy toast messages; restore summary.
  - **Dependencies:** Repository, LyricsDao, PlayCountDao, TrackPositionDao, NamedQueueDao, BackupArchivePolicy, BackupEntryModels, M3UWriter, M3UConstants, PlaylistExportFormat, FileUtil, RestoreSummary, kotlinx Json, `zipOutputStream` extension, KoinComponent.
  - **Consumed by:** `PreferencesScreenFragment` (createBackupFile/restoreBackup, BACKUP_EXTENSION), `ScheduledBackupWorker`, `RestoreSummary` consumers.
  - **Related files:** `util/backup/BackupArchivePolicy.kt`, `util/backup/BackupEntryModels.kt`, `util/m3u/M3UWriter.kt`, `util/m3u/M3UConstants.kt`, `util/RestoreSummary.kt`, `util/backup/ScheduledBackupWorker.kt`, `util/FileUtil.kt`, `data/local/room/LyricsDao.kt`, `data/local/room/PlayCountDao.kt`, `data/local/room/TrackPositionDao.kt`, `data/local/room/NamedQueueDao.kt`, `data/mapper/SongMapper.kt`.
  - **Notes:** Backup extension "bmgbak"; zip paths `Playlists/`, `Favorites/`, `statistics/`, `queues/`, `positions/`, `prefs/`, `lyrics/`, `artistImages/`; `createBackup` catches failures (rethrows `CancellationException`, logs with `Log.e`, toasts `backup_failed`); `createBackupFile` does not toast and returns Success/Failure so the worker can record status. Playlist write failures are no longer swallowed: a missing/failed file aborts the backup with `BackupCreationResult.Failure`; empty playlists are emitted as valid `#EXTM3U` entries so restore can recreate their entities. ZIP entries are written as direct bytes (no writer, so the archive is never closed early) with `closeEntry()` per entry and per-entry cancellation checks; restore streams through `BoundedEntryInputStream`, which counts actual bytes (10 MB for lyrics and AA-09 JSON entries, 100 MB per other entry, 100 MB total, 1000-entry cap), rejects unsafe entry file names via `requireSafeFileName()` and copies with `copyToWithCancellation()` (8 KB chunks); restore helpers are suspend; unselected/unrecognized entries are drained through the same counter (`drain()`), so skipped entries still count towards the limits and stay cancellable; entry classification requires a path-segment boundary (`hasPrefix`), directory entries are skipped, and lyrics decoding tolerates unknown JSON fields (`Json { ignoreUnknownKeys = true }`). Settings XML is parsed with `XmlPullParserFactory`, accepts only string/int/long/float/boolean/string-set values, rejects plain DOCTYPE/DOCDECL/DTD, partial/trailing content and oversized entries (1 MiB entry, 1,000 keys, 256-char keys, 64 KiB values, 4,096 set members), and commits through `PreferenceManager.getDefaultSharedPreferences` on IO; malformed or oversized settings entries return false and later entries continue, while archive-level limits still abort. Live-editor serialization means restored prefs need semantic whole-map comparison, not byte-identical XML. Commit failure attempts a best-effort live rollback; restored settings followed by a later archive error remain committed but the outer restore reports failure. See ADR-047. Manual sharing stages a successful SAF archive in `BackupShareStaging` outside this object; `restorePlaylists` remains the separate non-interactive merge policy and is not edited by the share flow. AA-09 adds `Favorites/` (new backups exclude favorites from `Playlists/`, while old archives still restore them when Playlists is selected), `statistics/play_counts.json`, `queues/named_queues.json` and `positions/track_positions.json`. Empty JSON lists are valid no-ops; a malformed selected JSON entry is logged and counted as a selected-entry failure without aborting later entries, while bounded-stream size/zip failures still abort. Queue cap/name failures surface as a partial result. See ADR-048.


- `util/Constants.kt` — App constants: support email, GitHub/community/legal URLs, user agent.
  - **Purpose:** Central constants for links, email and user agent.
  - **Responsibilities:** Define support email, GitHub/community/legal URLs, BuildConfig-driven download/donation links, `USER_AGENT`.
  - **Inputs:** `BuildConfig.DOWNLOAD_URL`, `BuildConfig.DONATION_LINK`, `BuildConfig.VERSION_NAME`.
  - **Outputs:** String constants used across app.
  - **Dependencies:** BuildConfig only.
  - **Consumed by:** AboutScreen, ErrorActivity, LastFmService, ListenBrainzService, MusicBrainzService, LyricallyApi, LrcLibApi.
  - **Related files:** `app/build.gradle.kts` (flavor injection), `ui/screen/about/AboutScreen.kt`, `data/remote/*`.
  - **Notes:** `USER_AGENT` = "ssilencce/<version>"; URLs point to upstream BoomingMusic.


- `util/CryptoUtil.kt` — MD5 encoding and AES-GCM encryption/decryption of sensitive strings using the Android Keystore.
  - **Purpose:** Secure encryption of sensitive strings (Last.fm/ListenBrainz secrets) via Android Keystore.
  - **Responsibilities:** `encodeMd5`; generate `SsilencceCryptoKey` in AndroidKeyStore; encrypt prepending random IV; decrypt with `GCMParameterSpec(128, iv)`.
  - **Inputs:** Plaintext strings; Base64 of IV+cyphertext for decryption.
  - **Outputs:** Base64 `IV || ciphertext`; decrypted plaintext.
  - **Dependencies:** `KeyGenParameterSpec`, `KeyGenerator`, Cipher AES/GCM/NoPadding, `MessageDigest` MD5.
  - **Consumed by:** `NetworkRepository` (session keys, ListenBrainz token), `LastFmService` (encodeMd5 signatures).
  - **Related files:** `data/local/repository/NetworkRepository.kt`, `data/remote/lastfm/LastFmService.kt`.
  - **Notes:** Key alias "SsilencceCryptoKey"; fixed 12-byte GCM IV.


- `util/FileUtil.kt` — App directories (playlists, custom images, thumbs) and storage helpers.
  - **Purpose:** Resolve app-owned and external storage directories.
  - **Responsibilities:** Provide playlists, custom artist/playlist image, thumbs dirs; `getDefaultStartDirectory` falling back Music → storage → root.
  - **Inputs:** Context via Koin `get`, optional dirType.
  - **Outputs:** Created `File` directories; default start directory.
  - **Dependencies:** Environment, KoinComponent, Context.
  - **Consumed by:** BackupHelper, M3UWriter, CustomArtistImageManager, CustomPlaylistImageManager, MediaStoreWriter, Preferences, MediaExt.
  - **Related files:** `util/BackupHelper.kt`, `util/m3u/M3UWriter.kt`, `coil/CustomPlaylistImageManager.kt`, `extensions/media/MediaExt.kt`.
  - **Notes:** `PLAYLISTS_DIRECTORY_NAME` = "Playlists"; Thumbs stored in externalCacheDir.


- `util/MusicUtil.kt` — Batch song deletion from MediaStore with playlist/cache/SAF cleanup and progress reporting.
  - **Purpose:** Batch-delete songs from MediaStore with progress callbacks.
  - **Responsibilities:** Batch `_ID IN (...)` queries (100k limit); `repository.deleteSong` per track; MediaStore delete; SAF delete pre-Q; `notifyChange`; progress/completion callbacks.
  - **Inputs:** Context, `List<Song>`, `ProgressCallback`, `CompletionCallback`.
  - **Outputs:** Deleted count; per-song progress; refreshed MediaStore.
  - **Dependencies:** Repository, MediaStore, `deleteUsingSAF`, `hasQ`/`onUI`, KoinComponent.
  - **Consumed by:** `DeleteSongsDialog` (deleteTracks).
  - **Related files:** `ui/dialogs/songs/DeleteSongsDialog.kt`, `extensions/files/SAFExt.kt`, `data/local/repository/Repository.kt`.
  - **Notes:** Batching avoids SQLite 10^6-byte query limit; SecurityException swallowed per batch.


- `util/Preferences.kt` - Central typed SharedPreferences access for all app settings, including audio-focus mode, per-track resume, session recovery, active named-queue binding, song duration bounds, sort ignore keywords, crossfade/fade, transport keys, notification layout/style, ghost mode, lock screen, extra-format scan, shuffle behavior and per-song tempo.
  - **Purpose:** Single typed access point for all app settings and preference keys.
  - **Responsibilities:** Typed getters/setters (theme, player, queue, shuffle, cutoffs, not-played custom-day threshold, audio focus, per-track resume, session recovery, active named queue, minimum/maximum song duration, song title mode, list date-modified switch, folder actions, updates, independent Aurora appearance/motion, custom sort keywords); listener registration; enum helpers; preference keys. Headset button mapping keys/getters/setters with code defaults, delay normalization and reset-to-defaults.
  - **Inputs:** Injected SharedPreferences, model enums (NowPlayingAction, SongClickBehavior, AudioFocusMode...), `Cutoff`/`CategoryInfo`/`NotPlayedDaysPolicy`, string resources. HeadsetButtonAction, HeadsetButtonPolicy, IgnoreKeywordPolicy.
  - **Outputs:** Typed settings values; SharedPreferences writes via `edit`; constant keys.
  - **Dependencies:** SharedPreferences, AppCompatDelegate, CategoryInfo, Cutoff, NotPlayedDaysPolicy, FileUtil, Calendar/JSON helpers. HeadsetButtonPolicy, HeadsetButtonAction, IgnoreKeywordPolicy.
  - **Consumed by:** PlaybackService, AudioFocusController, MainActivity, AbsThemeActivity, AbsPlayerFragment, EqualizerViewModel, ViewExt, NavigationExt, nearly all UI.
  - **Related files:** `App.kt` (defaults), `extensions/SharedPreferencesExt.kt`, `extensions/utilities/Utilities.kt`, `extensions/utilities/CalendarExt.kt`, `util/AudioFocusMode.kt`, `core/sort/IgnoreKeywordPolicy.kt`.
  - **Notes:** `LASTFM_LOGIN`/`LISTENBRAINZ_LOGIN` keys; lists serialized via kotlinx JSON; enums by name or ordinal. Audio-focus mode falls back to Ignore when the legacy IGNORE_AUDIO_FOCUS flag is set. Aurora does not overwrite saved alternative theme/player choices. Headset keys fall back to PlayPause/Next/Previous/PlayPause, 4+ off and 300 ms on existing installs; delay values outside 250/300/400/500/600 normalize to 300. `sorting_ignore_keywords` is a sanitized string set (default empty); it is read only while the article switch is on. `maximum_song_duration` defaults to 0 (unlimited); minimum defaults to 15 seconds. `not_played_days` defaults to 0, which resolves through the history interval; positive values produce an N-day cutoff and a localized plural description. `song_title_mode` defaults to FollowSort; `show_list_date_modified` defaults to false. `libraryCategories` filters unavailable Composers and appends new AlbumArtists/Composers/Queues entries invisible, preserving existing stored order and the original first five tabs.


- `util/AuroraPreferences.kt` — Independent Aurora appearance and ribbon-motion preference keys.
  - **Purpose:** Keep the optional appearance separate from saved theme and player-style keys.
  - **Responsibilities:** Define AURORA_APPEARANCE and AURORA_MOTION.
  - **Inputs:** None.
  - **Outputs:** Stable SharedPreferences key constants.
  - **Dependencies:** None.
  - **Consumed by:** App, Preferences, appearance settings, AuroraRibbon.
  - **Related files:** `util/Preferences.kt`, `App.kt`, `res/xml/preferences_screen_appearance.xml`.
  - **Notes:** Defaults are initialized only for a new empty preference store (ADR-036).


- `util/SongPlayCountHelper.kt` — Times playback per song to decide when to increment the counter (50% of duration).
  - **Purpose:** Track play time to bump play count at half the song duration.
  - **Responsibilities:** Reset/start stopwatch on song change; pause/resume on play state; `shouldBumpPlayCount` compares elapsed vs 50% duration.
  - **Inputs:** `Song` (duration), isPlaying flags.
  - **Outputs:** Boolean play-count decision.
  - **Dependencies:** StopWatch, `Song.emptySong`.
  - **Consumed by:** `PlaybackService` (songPlayCountHelper).
  - **Related files:** `util/StopWatch.kt`, `playback/PlaybackService.kt`, `data/local/room/PlayCountDao.kt`.
  - **Notes:** TAG constant for logging.


- `util/StopWatch.kt` — Simple thread-safe stopwatch (start/pause/reset/elapsed).
  - **Purpose:** Thread-safe millisecond stopwatch for playback timing.
  - **Responsibilities:** start/pause accumulating `previousElapsedTime`; reset; `elapsedTime` getter; toString.
  - **Inputs:** Start times via `System.currentTimeMillis`.
  - **Outputs:** Elapsed milliseconds.
  - **Dependencies:** None beyond java.util Locale.
  - **Consumed by:** `SongPlayCountHelper`.
  - **Related files:** `util/SongPlayCountHelper.kt`.
  - **Notes:** Authored by Karim Abou Zeid (kabouzeid), legacy upstream code.


- `util/StorageUtil.kt` — Enumerates mounted storage volumes and resolves the storage device of a directory.
  - **Purpose:** List mounted storage volumes and map directories to `StorageDevice`.
  - **Responsibilities:** `refreshStorageVolumes` via StorageManager; description/icon selection; `getStorageDevice` by canonical path; `getPathCompat` reflection pre-R.
  - **Inputs:** Context via Koin, `StorageVolume` list, directory `File`.
  - **Outputs:** `List<StorageDevice>`; matching device or null.
  - **Dependencies:** StorageManager, StorageVolume, Environment, `hasR`, KoinComponent.
  - **Consumed by:** LibraryViewModel, FolderChooserDialog, SpecialRepository, FileSystemQuery, FileExt.
  - **Related files:** `core/model/filesystem/StorageDevice.kt`, `extensions/files/FileExt.kt`, `ui/dialogs/library/FolderChooserDialog.kt`.
  - **Notes:** Only MEDIA_MOUNTED volumes; private `getPath()` reflection below API 30.


- `util/SwipeAndDragHelper.java` — ItemTouchHelper callback enabling drag reordering of RecyclerView items.
  - **Purpose:** RecyclerView drag-reorder callback for reorderable lists.
  - **Responsibilities:** UP/DOWN drag flags; `onMove` reporting via `ActionCompletionContract`; long-press drag disabled; swipe alpha fade.
  - **Inputs:** `ActionCompletionContract` listener.
  - **Outputs:** `onViewMoved(oldPosition, newPosition)` callbacks.
  - **Dependencies:** androidx `ItemTouchHelper`, `RecyclerView`.
  - **Consumed by:** `DraggableItemAdapter` (touchHelper).
  - **Related files:** `ui/adapters/DraggableItemAdapter.kt`, `ui/adapters/preference/ExtraInfoAdapter.kt`, `ui/adapters/preference/CategoryInfoAdapter.kt`.
  - **Notes:** Legacy Java port; no swipe movement flags (0).


- `util/color/NotificationColorUtil.java` — AOSP legacy helper for notification colors with guaranteed contrast.
  - **Purpose:** Contrast-guaranteeing color math for palette text readability.
  - **Responsibilities:** `findContrastColor` (LAB) / `findContrastColorAgainstDark` (HSL); `changeColorLightness`; luminance/contrast/text-contrast checks; embedded `ColorUtilsFromCompat`.
  - **Inputs:** `@ColorInt` colors, minRatio, findFg flag.
  - **Outputs:** Adjusted contrast-safe colors; luminance/contrast doubles.
  - **Dependencies:** android.graphics.Color only (self-contained).
  - **Consumed by:** `PaletteProcessor` (text color derivation).
  - **Related files:** `core/palette/PaletteProcessor.kt`.
  - **Notes:** Apache-2.0 AOSP framework copy.


- `util/cursor/SortedCursorUtil.kt` — Builds a SortedLongCursor selecting MediaStore song ids in a given order.
  - **Purpose:** Wrap a song cursor ordered by an external id order.
  - **Responsibilities:** Build `_ID IN (...)` selection from cursor ids; query `RealSongRepository.makeSongCursor`; wrap in `SortedLongCursor` with `AudioColumns._ID`.
  - **Inputs:** Cursor, idColumn index.
  - **Outputs:** `SortedLongCursor` or null.
  - **Dependencies:** RealSongRepository, SortedLongCursor, KoinComponent.
  - **Consumed by:** `PlaylistRepository` (playlist song ordering).
  - **Related files:** `util/cursor/SortedLongCursor.java`, `data/local/repository/PlaylistRepository.kt`.
  - **Notes:** `order` array preserves the desired external ordering. The playlist-id resolution query passes `skipDurationFilter = true`, so saved playlist membership is unaffected by library duration bounds.


- `util/cursor/SortedLongCursor.java` — AbstractCursor presenting an underlying cursor reordered by ids (CyanogenMod legacy).
  - **Purpose:** Present underlying cursor rows in requested id order.
  - **Responsibilities:** Build position mapping via `LongSparseArray`; track missing ids; `onMove` to mapped position; typed accessors delegate.
  - **Inputs:** Wrapped Cursor, `long[] order`, id column name.
  - **Outputs:** Reordered cursor rows; `getMissingIds()`.
  - **Dependencies:** `AbstractCursor`, androidx `LongSparseArray`.
  - **Consumed by:** `SortedCursorUtil`.
  - **Related files:** `util/cursor/SortedCursorUtil.kt`.
  - **Notes:** CyanogenMod Apache-2.0 legacy.


- `util/m3u/M3UConstants.kt` — M3U format constants (extension, header, entry markers).
  - **Purpose:** Constants for the M3U playlist file format.
  - **Responsibilities:** Define `EXTENSION` "m3u", `HEADER` "#EXTM3U", `ENTRY` "#EXTINF:", `DURATION_SEPARATOR` ",".
  - **Inputs:** —
  - **Outputs:** Format constants.
  - **Dependencies:** None.
  - **Consumed by:** `M3UWriter`.
  - **Related files:** `util/m3u/M3UWriter.kt`.
  - **Notes:** Minimal object, no logic.


- `util/m3u/M3UWriter.kt` - Exports playlists as M3U, M3U8 or PLS (plain file pre-Q, MediaStore on Q+) with staged replacement.
  - **Purpose:** Export Room playlists to M3U/M3U8/PLS files without destroying an existing export on failure.
  - **Responsibilities:** Export single/multiple with toasts; on Q+ use `MediaStore.Files` plus `MediaStoreWriter` with the selected format MIME, `FileUtil.playlistsDirectory` pre-Q; stage directory writes to a sibling temp, refuse empty playlists, and replace the target only after a complete UTF-8 write; `writeImpl` sorted by songPrimaryKey; extended-M3U body for M3U/M3U8 and numbered PLS entries.
  - **Inputs:** Context, `PlaylistWithSongs` (SongEntity list), `PlaylistExportFormat`.
  - **Outputs:** `.m3u`/`.m3u8`/`.pls` files in MediaStore or app dir; boolean success or `IOException`.
  - **Dependencies:** MediaStoreWriter, FileUtil, M3UConstants, PlaylistExportFormat, `hasQ`, `toSongs` mapper, KoinComponent, java.nio.file.Files.
  - **Consumed by:** BackupHelper (writeToDirectory for non-empty playlists), MenuItemClickExt (single/batch export).
  - **Related files:** `util/m3u/M3UConstants.kt`, `util/m3u/PlaylistExportFormat.kt`, `util/BackupHelper.kt`, `data/local/MediaStoreWriter.kt`, `data/local/room/PlaylistWithSongs.kt`.
  - **Notes:** EXTINF line format "duration,artist - title"; PLS writes numbered entries and `Version=2`; all formats use UTF-8. Directory exports use a hidden sibling and `Files.move(REPLACE_EXISTING)`. The Q+ `MediaStore.Files` route avoids the legacy provider appending `.m3u` during `Audio.Playlists` publication; MediaStore stages complete published content, then checks backup/final renames before old cleanup. Pre-commit failure preserves old bytes but provider-refused restoration may leave the backup name; post-commit cleanup may leave an extra backup. No atomic-swap/power-loss claim (ADR-044).


- `util/oem/MiPlayAudioSupport.kt` — Detects the Xiaomi Mi Play service availability on Chinese MIUI builds.
  - **Purpose:** Detect Xiaomi Mi Play (妙播) service support on MIUI.
  - **Responsibilities:** Check `com.milink.service` CoreService, `MiuiAudioPlaybackRecorder` class, international build flag, SystemUI intent resolution, foreground whitelist.
  - **Inputs:** Context.
  - **Outputs:** Boolean `supportMiPlay`.
  - **Dependencies:** PackageManager, Intent resolution, reflection on `miui.os.Build`.
  - **Consumed by:** `SystemMediaControlResolver`.
  - **Related files:** `util/oem/SystemMediaControlResolver.kt`.
  - **Notes:** Derived from Moriafly Media Kit (LGPL); Chinese builds only.


- `util/oem/SystemMediaControlResolver.kt` — Opens the system media output switcher with OEM-specific intents or native dialogs.
  - **Purpose:** Open system media output switcher across OEMs and Android versions.
  - **Responsibilities:** Try MiPlay/OneUI activities; fallback SystemUI `LAUNCH_MEDIA_OUTPUT_DIALOG` (12–13), settings panel (11/older), `MediaRouter2.showSystemOutputSwitcher` (14+).
  - **Inputs:** Context.
  - **Outputs:** Started activity/dialog; boolean success.
  - **Dependencies:** MiPlayAudioSupport, MediaRouter2 (API 34), SystemProperties reflection.
  - **Consumed by:** `AudioOutputObserver` (openMediaOutputSwitcher).
  - **Related files:** `util/oem/MiPlayAudioSupport.kt`, `core/audio/AudioOutputObserver.kt`.
  - **Notes:** OneUI branch currently disabled; system-app check via FLAG_SYSTEM.


- `extensions/ActivityExt.kt` — Activity/Intent extensions: hide keyboard, keep screen on, typed extras, root view.
  - **Purpose:** Activity and Intent convenience extensions.
  - **Responsibilities:** `hideSoftKeyboard`; `keepScreenOn` flags; typed lazy `extra`/`extraNotNull`; `rootView` accessor.
  - **Inputs:** Activity/Intent, extra keys, defaults.
  - **Outputs:** Side effects; lazy extra values; root View.
  - **Dependencies:** InputMethodManager, WindowManager, Intent extras.
  - **Consumed by:** MainActivity, AbsSlidingMusicPanelActivity, AbsThemeActivity, player fragments.
  - **Related files:** `extensions/FragmentExt.kt`, `ui/screen/MainActivity.kt`.
  - **Notes:** Extras read via deprecated `intent.extras.get`.


- `extensions/AdapterExt.kt` — RecyclerView adapter helpers: image loading with palette colors in MediaEntryViewHolder.
  - **Purpose:** Palette-tinted Coil image loading for `MediaEntryViewHolder`.
  - **Responsibilities:** `loadPaletteImage` with `PaletteProcessor` on success; `setColor` styling title/text/menu/play; checkable `isActivated`; adapter isEmpty checks.
  - **Inputs:** ImageRequest data, placeholderRes, builder lambda.
  - **Outputs:** Loaded images; palette-colored view states.
  - **Dependencies:** Coil3, PaletteProcessor, PaletteColor, MediaEntryViewHolder, lifecycleScope.
  - **Consumed by:** SongAdapter, AlbumAdapter, ArtistAdapter, GenreAdapter, YearAdapter.
  - **Related files:** `ui/component/base/MediaEntryViewHolder.kt`, `core/palette/PaletteProcessor.kt`.
  - **Notes:** Error fallback uses `PaletteColor.errorColor`.


- `extensions/AndroidExt.kt` — SDK version checks (hasPie/hasQ/...) and PackageManager/HtmlCompat helpers.
  - **Purpose:** SDK-level feature checks and package helpers.
  - **Responsibilities:** `hasPie`..`hasU` predicates; `PackageManager.packageInfo`/`resolveActivity` flags-aware; `CharSequence.toHtml`.
  - **Inputs:** Build.VERSION.SDK_INT, Context, Intent.
  - **Outputs:** Boolean checks; PackageInfo/ResolveInfo; HTML string.
  - **Dependencies:** Build, PackageManager, HtmlCompat.
  - **Consumed by:** PlaybackService, MusicUtil, M3UWriter, MediaStoreWriter, App.kt and many others.
  - **Related files:** — (pervasive across app).
  - **Notes:** `hasT`/`hasU` map TIRAMISU/UPSIDE_DOWN_CAKE.


- `extensions/BluetoothExt.kt` — Checks whether a Bluetooth A2DP device is connected (API-aware).
  - **Purpose:** A2DP connection state checks, API-aware.
  - **Responsibilities:** `isBluetoothA2dpConnected`/`Disconnected` via profile state (S+) or deprecated `AudioManager.isBluetoothA2dpOn`; BLUETOOTH_CONNECT permission gating.
  - **Inputs:** Context.
  - **Outputs:** Boolean connected/disconnected.
  - **Dependencies:** BluetoothManager, `BluetoothProfile.A2DP`, AudioManager, `hasS`.
  - **Consumed by:** `PlaybackService` (resume/pause on connect).
  - **Related files:** `playback/PlaybackService.kt`, `util/Preferences.kt` (RESUME_ON_BLUETOOTH_CONNECT).
  - **Notes:** Pre-S fallback uses deprecated API.


- `extensions/ContextExt.kt` — Context/Resources extensions: dp conversion, night mode, tablet/landscape detection, URL opening, toasts.
  - **Purpose:** Context/Resources convenience extensions.
  - **Responsibilities:** dp/dip/intRes conversion; landscape/tablet/night detection; `fileProviderAuthority`; openUrl/webSearch/tryStartActivity; toasts; onUI; power-save; theme/shape/plurals helpers.
  - **Inputs:** Context/Fragment/Resources, dimen/attr/plurals ids, URLs.
  - **Outputs:** Converted values; started activities; shown toasts; resolved colors.
  - **Dependencies:** MaterialColors, ShapeAppearanceModel, AppTheme, ktor `encodeURLParameter`, UiModeManager.
  - **Consumed by:** Broadly across UI, FileExt, PreferencesScreenFragment, AbsThemeActivity.
  - **Related files:** `extensions/resources/ColorExt.kt`, `extensions/files/FileExt.kt`, `core/model/theme/AppTheme.kt`.
  - **Notes:** `createAppTheme` bridges Preferences to AppTheme.


- `extensions/FragmentExt.kt` — Fragment/AlertDialog helpers: dialog builders, default grid columns, argument bundles, transitions.
  - **Purpose:** Fragment and DialogFragment helpers.
  - **Responsibilities:** AlertDialog create/show with onShow; grid columns; requestActivity/Context/View; typed extras; NavHostFragment lookups; MaterialSharedAxis/Fade transitions; `launchAndRepeatWithViewLifecycle`; balloons.
  - **Inputs:** Fragments, Bundle args, toolbar, ids.
  - **Outputs:** Built dialogs, transitions, scoped coroutines.
  - **Dependencies:** AlertDialog, MaterialSharedAxis, NavHostFragment, Balloon, lifecycleScope.
  - **Consumed by:** Library, player and settings fragments.
  - **Related files:** `extensions/ActivityExt.kt`, `extensions/resources/ViewExt.kt` (createSsilencceBalloon).
  - **Notes:** extra/extraNotNull mirror ActivityExt for arguments.


- `extensions/InsetsExt.kt` — Applies window insets (padding/margins, IME, scrollable content) to legacy Views.
  - **Purpose:** Legacy-View window inset application.
  - **Responsibilities:** `applyWindowInsets` with systemBars/displayCutout/IME mask; padding vs margin; RTL-aware start/end; `Space` resolution; bottom-inset fallback to navigation bar height.
  - **Inputs:** View, WindowInsetsCompat, Space, InsetsConsumer.
  - **Outputs:** Applied padding/margins; consumed-inset tag; consumer callbacks.
  - **Dependencies:** ViewCompat, WindowInsetsCompat, `getNavigationBarHeight`, MarginLayoutParams.
  - **Consumed by:** AbsMainActivityFragment, AbsSlidingMusicPanelActivity, AbsRecyclerViewFragment, AbsTagEditorActivity, detail fragments, PlayInfoFragment, ErrorActivity.
  - **Related files:** `extensions/ContextExt.kt` (getNavigationBarHeight), `ui/component/base/AbsMainActivityFragment.kt`.
  - **Notes:** Tag `R.id.id_inset_consumed` prevents double application.


- `extensions/IntentExt.kt` — Intent builders for sharing songs, opening web URLs, MIME/extra constants.
  - **Purpose:** Intent creation for sharing songs and opening content.
  - **Responsibilities:** `getShareSongIntent`/`getShareSongsIntent` via ShareCompat; `openWeb`; `toChooser`; MIME and extra constants (EXTRA_SONG, EXTRA_SONGS, EXTRA_PLAYLISTS).
  - **Inputs:** Context, Song(s), URL strings.
  - **Outputs:** Chooser Intents.
  - **Dependencies:** ShareCompat, `toUri`, Song.
  - **Consumed by:** MenuItemClickExt, QueueFragment, WebSearchDialog.
  - **Related files:** `ui/component/menu/MenuItemClickExt.kt`, `data/model/Song.kt`.
  - **Notes:** `MIME_TYPE_AUDIO` = "audio/*"; filters `Song.emptySong`.


- `extensions/SharedPreferencesExt.kt` — Observes SharedPreferences keys as Compose state via produceState.
  - **Purpose:** Bridge SharedPreferences to Compose state.
  - **Responsibilities:** `observeKeyAsState` registers listener, re-reads typed value (Boolean/String/Int/Float/Long), unregisters on dispose.
  - **Inputs:** SharedPreferences, key, default value.
  - **Outputs:** Compose `State<T>`.
  - **Dependencies:** Compose `produceState`, OnSharedPreferenceChangeListener.
  - **Consumed by:** `QueueScreen` (LOCKED_QUEUE).
  - **Related files:** `util/Preferences.kt`, `ui/screen/queue/QueueScreen.kt`.
  - **Notes:** Primitive types only.


- `extensions/files/FileExt.kt` — File extensions: formatted names, readable sizes, pretty paths, FileProvider URIs, Jaudiotagger reads, ZipOutputStream.
  - **Purpose:** File and stream utility extensions.
  - **Responsibilities:** Formatted file names/times; readable sizes; pretty paths via StorageUtil; canonical safe path; FileProvider content URIs; Jaudiotagger AudioFile reads; `formatFixed`; `readString`; `zipOutputStream`.
  - **Inputs:** File, Context, InputStream/OutputStream, Long values.
  - **Outputs:** Strings, Uris, AudioFile, ZipOutputStream.
  - **Dependencies:** FileProvider, StorageUtil, Jaudiotagger (AudioFileIO), ZipOutputStream.
  - **Consumed by:** BackupHelper (zipOutputStream), MetadataReader/Writer, CustomArtistImageManager, tag editor screens.
  - **Related files:** `util/StorageUtil.kt`, `extensions/ContextExt.kt` (fileProviderAuthority).
  - **Notes:** `getContentUri` uses "$packageName.provider" authority.


- `extensions/files/SAFExt.kt` - Storage Access Framework helpers: persisted URI permissions, document lookup by path, ordered path resolution, display names, SAF deletion.
  - **Purpose:** SAF permission persistence, document lookup and deletion.
  - **Responsibilities:** `saveTreeUri` persisted permissions; `getWritablePersistedUris`; `isSAFAccessGranted`; `findSAFDocument` path traversal; `deleteUsingSAF` fallback.
  - **Inputs:** Context, tree Uri, file paths, Song lists.
  - **Outputs:** Persisted Uris, document Uris, deletion booleans, failure toasts.
  - **Dependencies:** DocumentFile, DocumentsContract, contentResolver.
  - **Consumed by:** MusicUtil, DeleteSongsDialog, SAFDialog, tag editor save.
  - **Related files:** `util/MusicUtil.kt`, `ui/dialogs/SAFDialog.kt`, `ui/dialogs/songs/DeleteSongsDialog.kt`.
  - **Notes:** `deleteUsingSAF` deletes directly when `File.canWrite()`.


- `extensions/media/ArtistNameExt.kt` — Artist name utilities: main artist extraction from "feat./ft.", unknown/multi detection.
  - **Purpose:** Clean artist names for display and search.
  - **Responsibilities:** `extractMainArtistName` via regex; `displayArtistName` localizes unknown/various; `isVariousArtists`/`isArtistNameUnknown` checks.
  - **Inputs:** String artist names, Context via GlobalContext.
  - **Outputs:** Extracted/display names; booleans.
  - **Dependencies:** `Artist.VARIOUS_ARTISTS_DISPLAY_NAME`, R.string resources, Koin GlobalContext.
  - **Consumed by:** LyricsDownloadService, NetworkRepository, SongAdapter, AbsPlayerFragment, QueueScreen, MiniPlayerFragment.
  - **Related files:** `extensions/media/DataExt.kt`, `data/remote/lyrics/LyricsDownloadService.kt`.
  - **Notes:** Regex covers feat./ft./with, slashes, semicolons and " x ".


- `extensions/media/DataExt.kt` — Display string builders for Album/Artist/Song metadata (info, counts, Spanned titles, web search).
  - **Purpose:** Human-readable metadata strings for library/player UI.
  - **Responsibilities:** Album/artist/song info strings; date-only list modification label; plural counts; Spanned titles with color spans; playlist info/durations; `replayGainStr`; `searchQuery` URLs; favorites check.
  - **Inputs:** Album/Artist/Song/PlaylistEntity, Context, WebSearchEngine.
  - **Outputs:** Formatted strings, CharSequences, query URLs.
  - **Dependencies:** `buildInfoString`, ReplayGainTagExtractor, text color spans, plurals.
  - **Consumed by:** SongAdapter, SimpleSongAdapter, AlbumAdapter, SearchAdapter, AlbumDetailFragment, InfoViewModel.
  - **Related files:** `extensions/media/ArtistNameExt.kt`, `extensions/utilities/Utilities.kt`, `extensions/resources/ColorExt.kt`.
  - **Notes:** Uses `DEFAULT_INFO_DELIMITER` " • ". `listDateModified` formats `Song.dateModified` with the medium date style in the resource locale.


- `extensions/media/MediaExt.kt` — MediaStore helpers: insert/delete/album-art URIs, readable track/duration numbers, thumbnail files.
  - **Purpose:** MediaStore and formatting helpers.
  - **Responsibilities:** Album art thumb file; insertAlbumArt/deleteAlbumArt; iTunes track modulo; song/times plurals; album cover Uris; `asReadableDuration`; section names and article/custom-keyword normalization.
  - **Inputs:** ContentResolver, albumId, Long durations, SortMode.
  - **Outputs:** Uris, Files, formatted strings, section letters.
  - **Dependencies:** MediaStore AlbumColumns/MediaColumns, FileUtil, IgnoreKeywordPolicy, SortMode, plurals.
  - **Consumed by:** MetadataWriter, AlbumDetailFragment, SongAdapter, TagEditor, LibraryViewModel.
  - **Related files:** `util/FileUtil.kt`, `data/local/MetadataWriter.kt`, `core/sort/SortMode.kt`, `core/sort/IgnoreKeywordPolicy.kt`.
  - **Notes:** Section letters call the shared normalizer with the active sort mode’s keyword set; the article map now lives in the pure policy.


- `extensions/navigation/NavigationExt.kt` — Builds navigation argument bundles for detail screens and navigation actions.
  - **Purpose:** SafeArgs bundle builders and navigation helpers.
  - **Responsibilities:** playInfo/detail/songDetail/genreDetail/folderDetail/playlistDetail/albumDetail/artistDetail/search args; `findActivityNavController`; availability-aware `isValidCategory`; `asFragmentExtras` shared elements; composer-name artist detail args.
  - **Inputs:** Album/Artist/Song/Folder/Genre/Playlist/ContentType/SearchFilter, views.
  - **Outputs:** Navigation argument Bundles, NavController, Extras.
  - **Dependencies:** Generated Args classes, NavController, CategoryInfo, Preferences.
  - **Consumed by:** MenuItemClickExt, DetailListFragment, HomeFragment, AlbumListFragment, ArtistDetailFragment, SearchFragment, AbsPlayerFragment, QueueFragment, CoverPagerFragment.
  - **Related files:** res/navigation graphs, `ui/screen/library` detail fragments.
  - **Notes:** Album-artists use id -1 plus name; composers use id -1 plus `composerName`; `Preferences.onlyAlbumArtists` remains respected for normal artist links. `isValidCategory` excludes unavailable Composers on API < 30.


- `extensions/resources/ColorExt.kt` — Color utilities: luminance, darkening, contrast guarantee, desaturation, view/seekbar tinting.
  - **Purpose:** Color math and view tinting helpers.
  - **Responsibilities:** isColorLight/darken/ensureContrastAgainst/desaturate/adjustSaturation; shiftColor/withAlpha; spans and state lists; theme color resolution; `applyColor` for SeekBar/Slider/buttons/FAB/TextView/MorphicIconButton/Toolbar.
  - **Inputs:** Int colors, Context/Fragment, views.
  - **Outputs:** Adjusted colors, ColorStateLists, tinted views.
  - **Dependencies:** ColorUtils, MaterialColors, MorphicIconButton, Slider.
  - **Consumed by:** AdapterExt, ViewExt, player/theme screens.
  - **Related files:** `extensions/resources/ViewExt.kt`, `ui/component/views/MorphicIconButton.kt`.
  - **Notes:** `getPrimaryTextColor` resolves light/dark text resources.


- `extensions/resources/DrawableExt.kt` — Drawable/Bitmap conversions: tinting, drawable→bitmap, resizing, JPG encoding.
  - **Purpose:** Drawable and Bitmap conversion helpers.
  - **Responsibilities:** `getDrawableCompat`; `getTinted` (SRC_IN); `toBitmap`; `getResized` proportional scale; `toJPG` compression.
  - **Inputs:** Drawables, Bitmaps, colors, OutputStream.
  - **Outputs:** Tinted Drawables, Bitmaps, JPG bytes.
  - **Dependencies:** DrawableCompat, `createBitmap`, `scale`.
  - **Consumed by:** CustomPlaylistImageManager, AutoGeneratedImageUtil, tag editor, widget art.
  - **Related files:** `coil/util/AutoGeneratedImageUtil.kt`, `coil/CustomPlaylistImageManager.kt`.
  - **Notes:** `toJPG` default quality 90.


- `extensions/resources/ViewExt.kt` — View helpers: animations, visibility, icon tinting, toolbars, balloons, bottom sheets, keyboard.
  - **Purpose:** Legacy View animation and behavior helpers.
  - **Responsibilities:** show/hide/bounce animations; key reactions; keyboard focus; tint color animations; marquee/markdown text; fast scroller; navigation-bar snapshot show/hide; wavy progress; balloon tooltips; peek-height animation.
  - **Inputs:** Views, colors, menus, lifecycle owners.
  - **Outputs:** Animated/tinted views, FastScroller, Balloon.
  - **Dependencies:** ObjectAnimator/ValueAnimator, Markwon, FastScroller, Balloon, Material components, Preferences.
  - **Consumed by:** AbsPlayerFragment, AbsRecyclerViewFragment, DetailListFragment, PlaylistDetailFragment, UpdateDialog, DetailAdapters.
  - **Related files:** `extensions/resources/ColorExt.kt`, `extensions/ContextExt.kt`.
  - **Notes:** `SSILENCCE_ANIM_TIME` = 350L.


- `extensions/utilities/CalendarExt.kt` — Calendar helpers: elapsed today/week/month/year ms and playlist cutoff calculations.
  - **Purpose:** Calendar elapsed-time math for playlist/history cutoffs.
  - **Responsibilities:** `getElapsedToday/Days/Week/Month/Months/Year`; `getCutoffTimeMillis` mapping PlaylistCutoff; locale-aware date formatting.
  - **Inputs:** Calendar, cutoff string, Context, timeMillis.
  - **Outputs:** Elapsed millis; formatted dates.
  - **Dependencies:** PlaylistCutoff, DateFormat, GregorianCalendar.
  - **Consumed by:** `Preferences` (cutoffs), `SmartRepository` (history cutoffs).
  - **Related files:** `util/Preferences.kt`, `data/local/repository/SmartRepository.kt`.
  - **Notes:** `calendarSingleton` caches a shared Calendar instance.


- `extensions/utilities/CursorExt.kt` — Cursor iteration helpers: null safety, row mapping, safe column reads.
  - **Purpose:** Safe Cursor iteration and reads.
  - **Responsibilities:** `isNullOrEmpty`; `iterateIfValid`; `mapIfValid` with per-row exception logging; `takeOrDefault`; `getBoolean`/`getLongSafe`/`getStringSafe`.
  - **Inputs:** Cursor, column names/indices.
  - **Outputs:** Mapped lists, defaults, typed values.
  - **Dependencies:** android.database.Cursor, Log.
  - **Consumed by:** RealSongRepository, AlbumRepository, ArtistRepository, GenreRepository, SongRepository.
  - **Related files:** `data/local/repository/*`, `data/local/MediaQueryDispatcher.kt`.
  - **Notes:** Safe getters return -1/null when column missing.


- `extensions/utilities/Utilities.kt` — String utilities: normalize, sanitize, collapse spaces, RTL/Arabic detection, enum/JSON helpers.
  - **Purpose:** String, enum and JSON utility extensions.
  - **Responsibilities:** `collapseSpaces`; NFD normalize; `sanitize` filename chars; `capitalize`; Arabic/RTL detection; `buildInfoString`; `toEnum`; lenient `deserialize`/`serialize`.
  - **Inputs:** Strings, enums, JSON strings.
  - **Outputs:** Normalized strings, enum values, parsed objects.
  - **Dependencies:** Normalizer, kotlinx Json.
  - **Consumed by:** Preferences (serialize/deserialize), DataExt, BackupHelper (lyrics JSON), SongRepository.
  - **Related files:** `util/Preferences.kt`, `extensions/media/DataExt.kt`.
  - **Notes:** `DEFAULT_INFO_DELIMITER` " • " defined here; lenient JSON tolerates unknowns.

### App entry points

- `App.kt` — Application class: starts Koin, sets default preferences, crash activity, night mode, custom Coil ImageLoader, cache cleanup.
  - **Purpose:** Application entry point initializing DI, preferences, and image loading.
  - **Responsibilities:** `startKoin(appModules)`, seed default preferences, `CaocConfig` error activity, night mode, Coil ImageLoader, memory trim.
  - **Inputs:** `BuildConfig.DEBUG/FLAVOR/VERSION_NAME`; `SettingsScreen` preference layouts.
  - **Outputs:** Koin graph, `SingletonImageLoader`, crash-handler config, StrictMode in debug.
  - **Dependencies:** Koin, Coil3, customactivityoncrash, `ReplayGainTagExtractor`, coil mappers/fetchers/keyers.
  - **Consumed by:** AndroidManifest application; whole app through DI.
  - **Related files:** `MainModule.kt`, `ErrorActivity`, `MainActivity`.
  - **Notes:** Helpers `isFDroidBuild()`, `isPlayStoreBuild()`, `isExperimentalBuild()` (alpha/beta/rc regex).


- `MainModule.kt` - Koin DI modules: network services, Room database (v9), repositories, playback components, media writers, image managers, ViewModels, audio clip exporter, ringtone store, screencast manager and setup wizard.
  - **Purpose:** Defines the complete Koin dependency graph.
  - **Responsibilities:** `networkModule`, `mainModule`, `roomModule`, `dataModule`, `viewModule` definitions, including PlayEvent/TrackPosition/NamedQueue DAOs, NamedQueueRepository and the batch tag editor ViewModel.
  - **Inputs:** Ktor client, OkHttp, Room DAOs, audio processors, repositories, ViewModels.
  - **Outputs:** `appModules` list with `single`/`factory`/`viewModel` providers.
  - **Dependencies:** Koin, Room (v9 migrations 1→9), Ktor, Media3 processors, Koin viewModel DSL.
  - **Consumed by:** `App.startKoin`.
  - **Related files:** `App.kt`, `SsilencceDatabase`, repository implementations, `NamedQueueRepository.kt`.
  - **Notes:** `Real*` implementations bound to interfaces; `github`/`playstore` Last.fm access via injected services; PlayerViewModel receives NamedQueueRepository. `RealPlaylistRepository` also receives the Room database so interactive playlist create+insert and replace-contents can use `withTransaction`. WorkManager scheduled backups are constructed from `util/backup/` directly and need no Koin binding.


- `MediaControllerOwner.kt` — Lifecycle-aware owner of the MediaController connected to the PlaybackService MediaSession.
  - **Purpose:** Manages the lifecycle of the app's MediaController.
  - **Responsibilities:** Async connect, state machine (Idle/Connected/Connecting/Disconnected), listener forwarding, addPlayerListener.
  - **Inputs:** Context, `MediaController.Listener`, `LifecycleOwner`/`Lifecycle`.
  - **Outputs:** `MediaController`, `isConnected` StateFlow of `Event<Boolean>`.
  - **Dependencies:** Media3 session API, `SessionToken(PlaybackService)`, `DefaultLifecycleObserver`, `AtomicReference`.
  - **Consumed by:** `MainActivity`, `PlayerViewModel`, player listeners.
  - **Related files:** `PlaybackService.kt`, `core/model/task/Event.kt`.
  - **Notes:** Reconnects on start after disconnect; cancels future and releases controller on stop.

### ui/

- `ui/LibraryCallbacks.kt` — Callback interfaces used by library adapters (songs, albums, artists, genres, years, files, playlists, search, home).
  - **Purpose:** Contract between library adapters and hosting fragments for clicks and menu actions.
  - **Responsibilities:** Define ISongCallback, IAlbumCallback, IArtistCallback, IGenreCallback, IYearCallback, IFileCallback, IPlaylistCallback, ISearchCallback, IHomeCallback.
  - **Inputs:** Song, Album, Artist, Genre, ReleaseYear, FileSystemItem, PlaylistWithSongs, Suggestion, MenuItem, shared-element View pairs.
  - **Outputs:** Callback methods dispatched to fragments (clicks, menu item selection, multi-selection).
  - **Dependencies:** RecyclerView, core/data models only; no Android framework beyond MenuItem/View.
  - **Consumed by:** YearAdapter, SearchAdapter, PlaylistAdapter, GenreAdapter, FileAdapter, SongAdapter, ArtistAdapter, AlbumAdapter, HomeAdapter, SearchFragment, library fragments.
  - **Related files:** `ui/UiCallbacks.kt`, `ui/adapters/*`, `ui/screen/library/*` fragments.
  - **Notes:** Home callback creates per-suggestion sub-adapters; shared elements enable fragment transitions.


- `ui/UiCallbacks.kt` — IBackConsumer (back press) and IScrollHelper (scroll to top) interfaces implemented by fragments.
  - **Purpose:** Minimal fragment callback contracts for back handling and scroll-to-top.
  - **Responsibilities:** IBackConsumer.handleBackPress returning Boolean; IScrollHelper.scrollToTop.
  - **Inputs:** — (no data; plain interfaces).
  - **Outputs:** Back-press consumed flag; scroll-to-top side effect.
  - **Dependencies:** None.
  - **Consumed by:** AbsSlidingMusicPanelActivity (IBackConsumer check), AbsRecyclerViewFragment (IScrollHelper), library fragments.
  - **Related files:** `ui/component/base/AbsSlidingMusicPanelActivity.kt`, `ui/component/base/AbsRecyclerViewFragment.kt`.
  - **Notes:** Used with `whichFragment` lookups; back is handled before navController.navigateUp.


- `ui/adapters/YearAdapter.kt` — Multi-select RecyclerView adapter for release years with cover, name, song count and context menu.
  - **Purpose:** Bind ReleaseYear rows in the years list/grid screens.
  - **Responsibilities:** Palette cover loading (DEFAULT_YEAR_IMAGE), title/song-count text, popup menu, multi-select toggle, stable ids.
  - **Inputs:** List<ReleaseYear>, item layout resource, IYearCallback, menu_media_selection.
  - **Outputs:** yearClick/yearMenuItemClick/yearsMenuItemClick callbacks; activated row states.
  - **Dependencies:** AbsMultiSelectAdapter, MediaEntryViewHolder, OnClickMenu, coil DEFAULT_YEAR_IMAGE, loadPaletteImage.
  - **Consumed by:** YearsListFragment, YearDetailFragment (menu actions), years multi-selection.
  - **Related files:** `ui/screen/library/years/YearsListFragment.kt`, `ui/component/base/AbsMultiSelectAdapter.kt`, `ui/component/menu/MenuClick.kt`.
  - **Notes:** Reuses menu_item_artist popup with action_tag_editor removed; item id is year value.


- `ui/adapters/SimpleItemAdapter.kt` — Minimal generic adapter for simple lists (configurable layout and TextView).
  - **Purpose:** Lightweight generic RecyclerView adapter for plain string lists.
  - **Responsibilities:** Inflate configurable layout (default android.R.layout.simple_list_item_1), bind text, click dispatch via Callback.
  - **Inputs:** List<T>, layoutRes, textViewId, Callback<T> bindData/itemClick.
  - **Outputs:** Callback.itemClick; bindData may override default text binding.
  - **Dependencies:** RecyclerView, LayoutInflater, TextView.
  - **Consumed by:** MultiCheckDialog, FolderChooserDialog, BlacklistWhitelistDialog (as SimpleItemAdapter<String>).
  - **Related files:** `ui/dialogs/MultiCheckDialog.kt`, `ui/dialogs/library/FolderChooserDialog.kt`, `ui/dialogs/library/BlacklistWhitelistDialog.kt`.
  - **Notes:** notifyDataSetChanged on items set; not multi-select aware.


- `ui/adapters/SearchAdapter.kt` — Multi-select adapter for search results distinguishing albums, artists, songs, playlists, genres and headers.
  - **Purpose:** Render heterogeneous global-search results in one RecyclerView.
  - **Responsibilities:** View-type dispatch (HEADER/ALBUM/ARTIST/SONG/PLAYLIST/GENRE), Coil image loading, popup menus, song multi-select, and the shared song title/date display policy for SONG rows.
  - **Inputs:** List<Any> of mixed models, ISearchCallback, menu_media_selection.
  - **Outputs:** songClick/songMenuItemClick, album/artist/playlist clicks and menus, genreClick, onMultipleItemAction.
  - **Dependencies:** AbsMultiSelectAdapter, OnClickMenu, coil songImage/albumImage/artistImage/playlistImage, displayArtistName/albumInfo helpers, SongListDisplayPolicy, Preferences.
  - **Consumed by:** SearchFragment.
  - **Related files:** `ui/screen/library/search/SearchFragment.kt`, `ui/LibraryCallbacks.kt`.
  - **Notes:** Only Song rows participate in quick-select; long-press on other rows opens popup menu. Song rows resolve titles with a null sort key through `SongListDisplayPolicy` and append the optional date suffix to their existing secondary text.


- `ui/adapters/PlaylistAdapter.kt` — Multi-select playlist adapter (name, song count, image, transition, selection menu).
  - **Purpose:** Bind PlaylistWithSongs rows in playlist list screens.
  - **Responsibilities:** Playlist cover via coil playlistImage, transition names, popup menu, play button opening PlayerViewModel.openPlaylist, multi-select.
  - **Inputs:** List<PlaylistWithSongs>, item layout, IPlaylistCallback, menu_playlists_selection.
  - **Outputs:** playlistClick/playlistMenuItemClick/playlistsMenuItemClick; openPlaylist via Koin PlayerViewModel.
  - **Dependencies:** AbsMultiSelectAdapter, MediaEntryViewHolder, OnClickMenu, coil playlistImage, PlayerViewModel.
  - **Consumed by:** PlaylistListFragment; menu actions in dialogs.
  - **Related files:** `ui/screen/library/playlists/PlaylistListFragment.kt`, `ui/screen/player/PlayerViewModel.kt`.
  - **Notes:** Stable ids from playlistEntity.playListId; transition name is playlist name.


- `ui/adapters/HomeAdapter.kt` — Home adapter rendering suggestion rows with a horizontal RecyclerView.
  - **Purpose:** Render home-screen suggestion sections (heading + horizontal item row).
  - **Responsibilities:** Bind Suggestion type title, create inner horizontal adapter via callback, open-suggestion button; apply conditional Aurora rail typography (13 sp all-caps headings, 0.06 letter spacing), expose Aurora headings as accessibility headings and use 112 dp child cards with hierarchy styling.
  - **Inputs:** List<Suggestion>, IHomeCallback.
  - **Outputs:** createSuggestionAdapter, suggestionClick callbacks.
  - **Dependencies:** RecyclerView, LinearLayoutManager, MaterialButton, item_suggestion layout.
  - **Consumed by:** HomeFragment.
  - **Related files:** `ui/screen/library/home/HomeFragment.kt`, `data/model/Suggestion.kt`, `core/model/ContentType.kt`.
  - **Notes:** itemViewCacheSize 0; stable ids from suggestion hash.


- `ui/adapters/GenreAdapter.kt` — Genre adapter with palette image, name, song count and FastScroller popup text.
  - **Purpose:** Bind Genre rows in genres list/grid.
  - **Responsibilities:** Palette cover (DEFAULT_GENRE_IMAGE), name/count text, PopupTextProvider for FastScroller, hides play/menu buttons.
  - **Inputs:** List<Genre>, item layout, IGenreCallback.
  - **Outputs:** genreClick callback; popup text via asSectionName.
  - **Dependencies:** MediaEntryViewHolder, loadPaletteImage, PopupTextProvider (fastscroll), KoinComponent.
  - **Consumed by:** GenresListFragment.
  - **Related files:** `ui/screen/library/genres/GenresListFragment.kt`, `ui/component/base/MediaEntryViewHolder.kt`.
  - **Notes:** Not multi-select; unknown genre (-1 id) yields empty popup text.


- `ui/adapters/FileAdapter.kt` — Multi-select adapter for the file explorer (folders, songs, storage devices).
  - **Purpose:** Bind mixed filesystem rows (StorageDevice, Folder, Song) in the folder explorer.
  - **Responsibilities:** View-type dispatch (folder/song/other), icons for devices/folders, Coil covers for songs, sort-aware titles, popup menus, multi-select.
  - **Inputs:** List<FileSystemItem>, FileSortMode, SongClickBehavior, IFileCallback.
  - **Outputs:** fileClick/fileMenuItemClick/filesMenuItemClick; submitList refresh.
  - **Dependencies:** AbsMultiSelectAdapter, MediaEntryViewHolder, OnClickMenu, coil placeholderDrawableRes, SwipeAndDragHelpers none.
  - **Consumed by:** FoldersListFragment.
  - **Related files:** `ui/screen/library/folders/FoldersListFragment.kt`, `core/model/filesystem/FileSystemItem.kt`, `util/SwipeAndDragHelper.java`.
  - **Notes:** Play menu visibility honors songClickBehavior and playOptionAlwaysVisible.


- `ui/adapters/DraggableItemAdapter.kt` — Generic drag & drop adapter for reorderable lists with checkbox and drag handle.
  - **Purpose:** Base for preference dialogs reordering items (categories, extra info).
  - **Responsibilities:** Start ItemTouchHelper drag from drag handle, move items, attach to RecyclerView, bind checkbox/title.
  - **Inputs:** MutableList<T>; preference_dialog_draggable_item layout.
  - **Outputs:** onViewMoved reorder notifications; mutated items list.
  - **Dependencies:** RecyclerView, ItemTouchHelper, SwipeAndDragHelper (ActionCompletionContract).
  - **Consumed by:** ExtraInfoAdapter, CategoryInfoAdapter.
  - **Related files:** `ui/adapters/preference/ExtraInfoAdapter.kt`, `ui/adapters/preference/CategoryInfoAdapter.kt`, `util/SwipeAndDragHelper.java`.
  - **Notes:** Subclasses bind checkbox semantics and row click in onBindViewHolder.


- `ui/adapters/DetailAdapters.kt` — Detail-screen adapters: HeaderAdapter, SectionHeaderAdapter, WikiAdapter, HorizontalListAdapter.
  - **Purpose:** Composite single-item adapters assembling album/artist detail screens.
  - **Responsibilities:** HeaderAdapter binds header layout; SectionHeaderAdapter shows sort button; WikiAdapter renders markdown wiki; HorizontalListAdapter hosts inner horizontal adapter.
  - **Inputs:** ViewBinding inflaters, title strings, onSortClick lambdas, inner RecyclerView.Adapter.
  - **Outputs:** updateTitle/setVisible state changes; single-row item counts with stable ids.
  - **Dependencies:** RecyclerView, LinearLayoutManager, ItemDetail*Binding, setMarkdownText.
  - **Consumed by:** AlbumDetailFragment, ArtistDetailFragment.
  - **Related files:** `ui/screen/library/albums/AlbumDetailFragment.kt`, `ui/screen/library/artists/ArtistDetailFragment.kt`.
  - **Notes:** Wiki text toggles between 4 and unlimited maxLines on click.


- `ui/adapters/song/SongAdapter.kt` - Open base adapter for songs with multi-select, palette image, selection menu, section popup text and optional history-only removal action.
  - **Purpose:** Reusable base song adapter for all song lists.
  - **Responsibilities:** Palette cover loading, title-mode policy plus sort-mode-aware texts, optional date-modified suffix, FastScroller popup, click plays via PlayerViewModel.openSongs, popup menu, multi-select, historyMode visibility for `action_remove_from_history`.
  - **Inputs:** List<Song>, SongSortMode, ISongCallback, item layout, menu_media_selection, historyMode flag.
  - **Outputs:** songsMenuItemClick; openSongs honoring SongClickBehavior/playOptionClickBehavior.
  - **Dependencies:** AbsMultiSelectAdapter, MediaEntryViewHolder, OnClickMenu, PlayerViewModel, Preferences.
  - **Consumed by:** SongListFragment, GenreDetailFragment, YearDetailFragment, FolderDetailFragment, DetailListFragment, HomeFragment.
  - **Related files:** `ui/adapters/song/SimpleSongAdapter.kt`, `ui/adapters/song/PlaylistSongAdapter.kt`, `ui/screen/player/PlayerViewModel.kt`, `ui/screen/other/DetailListFragment.kt`.
  - **Notes:** Open ViewHolder/songMenuRes/onPrepareSongMenu/onPrepareActionModeMenu for subclass customization; hides play button and only exposes history removal when historyMode is true (ADR-044). Path sorting suppresses the title-based FastScroller popup because the list is no longer ordered by title. `SongTitleMode.FollowSort` preserves the filename-on-File-name behavior; the date suffix is appended only where a secondary text view exists.


- `ui/adapters/song/SimpleSongAdapter.kt` — SongAdapter subclass showing track number, duration and info text per sort key.
  - **Purpose:** Compact song rows with track number, duration, and sort-key text.
  - **Responsibilities:** Bind imageText track number, time duration, getSongText per SortKey (Album/Track/Year), and the shared optional date-modified suffix.
  - **Inputs:** FragmentActivity, songs, layoutRes, SongSortMode, ISongCallback.
  - **Outputs:** Rendered rows; inherits SongAdapter click/menu behavior.
  - **Dependencies:** SongAdapter, asReadableTrackNumber/asReadableDuration, buildInfoString.
  - **Consumed by:** AlbumDetailFragment, ArtistDetailFragment.
  - **Related files:** `ui/adapters/song/SongAdapter.kt`, `ui/screen/library/albums/AlbumDetailFragment.kt`, `ui/screen/library/artists/ArtistDetailFragment.kt`.
  - **Notes:** "-" placeholder when track number unknown. The date suffix uses the base `appendListDateModified` helper, so album/artist detail follows the same setting as general lists.


- `ui/adapters/song/PlaylistSongAdapter.kt` — SongAdapter subclass with drag & drop (advrecyclerview) for playlist reordering.
  - **Purpose:** Song rows inside playlist detail with drag reordering and persistence.
  - **Responsibilities:** DraggableItemAdapter contract (start-drag on dragView, move, drop), lock-drag toggle, saveSongs via LibraryViewModel.insertSongs with an explicit song list parameter.
  - **Inputs:** FragmentActivity, songs, layout, isLockDrag, ISongCallback, PlaylistEntity, optional full song list for saving.
  - **Outputs:** Reordered dataSet; Room playlist song persistence; playlist-specific menu.
  - **Dependencies:** advrecyclerview DraggableItemAdapter, SongAdapter, LibraryViewModel, DraggableItemState.
  - **Consumed by:** PlaylistDetailFragment (wrapped by DragDropManager).
  - **Related files:** `ui/screen/library/playlists/PlaylistDetailFragment.kt`, `ui/adapters/song/SongAdapter.kt`.
  - **Notes:** Uses menu_playlist_songs_selection and menu_item_playlist_song menus. `saveSongs(playlistEntity, songs = dataSet)` lets the fragment persist the full order while the adapter displays a filtered subset; never call it with the filtered list.


- `ui/adapters/preference/ExtraInfoAdapter.kt` — Draggable adapter configuring extra metadata fields shown in the player.
  - **Purpose:** Let users enable/reorder MetadataField entries for player/widget extra info.
  - **Responsibilities:** Bind localized field names, toggle isEnabled on row click.
  - **Inputs:** MutableList<MetadataField>.
  - **Outputs:** Mutated MetadataField list persisted by dialog.
  - **Dependencies:** DraggableItemAdapter, MetadataField, core/model MetadataField.
  - **Consumed by:** ExtraInfoPreferenceDialog.
  - **Related files:** `ui/component/preferences/dialog/ExtraInfoPreferenceDialog.kt`, `ui/adapters/DraggableItemAdapter.kt`.
  - **Notes:** Checkbox state reflects MetadataField.isEnabled.


- `ui/adapters/preference/CategoryInfoAdapter.kt` — Draggable adapter choosing visible library categories (max 5, at least 1).
  - **Purpose:** Configure visible/reordered library navigation categories.
  - **Responsibilities:** Bind category titles, enforce MAX_VISIBLE_CATEGORIES and at-least-one rules with toasts.
  - **Inputs:** MutableList<CategoryInfo>.
  - **Outputs:** Mutated CategoryInfo visibility list saved by dialog.
  - **Dependencies:** DraggableItemAdapter, CategoryInfo, showToast.
  - **Consumed by:** CategoriesPreferenceDialog.
  - **Related files:** `ui/component/preferences/dialog/CategoriesPreferenceDialog.kt`, `core/model/CategoryInfo.kt`.
  - **Notes:** Blocks unchecking last visible category; warns over five selections.


- `ui/adapters/artist/ArtistAdapter.kt` - Artist adapter with palette image, composer/album-artist-aware transition names and typed statistics rows.
  - **Purpose:** Bind Artist rows in artist, album-artist and composer lists and similar-artist rows.
  - **Responsibilities:** Artist covers/menus/play/selection; atomic statistics-list submission; typed-key collision-safe stable row IDs; snapshot labels/count/percentage and unavailable-action suppression; hide composer-inapplicable tag-editor/add-home actions.
  - **Inputs:** List<Artist> or complete ArtistPlayStat rows, ArtistSortMode, IArtistCallback.
  - **Outputs:** artistClick/artistMenuItemClick/artistsMenuItemClick; openQueue of sorted songs; stats-aware secondary text.
  - **Dependencies:** AbsMultiSelectAdapter, MediaEntryViewHolder, OnClickMenu, PlayerViewModel, ArtistPlayStat.
  - **Consumed by:** ArtistListFragment, ArtistDetailFragment (similar), DetailListFragment, HomeFragment.
  - **Related files:** `ui/screen/library/artists/ArtistListFragment.kt`, `ui/adapters/album/AlbumAdapter.kt`, `data/model/ArtistPlayStat.kt`.
  - **Notes:** Recorded-name and aggregate-ID keys never collide or overwrite one another. Stable row IDs survive reorder/period changes/temporary absence; actions use only real current Artist IDs. Snapshot-only rows remain visible but cannot navigate/play/select. Album-artist transition names use `Artist.isAlbumArtist` and composer names use `Artist.composerName`, independent of the global onlyAlbumArtists preference (ADR-040).


- `ui/adapters/album/AlbumAdapter.kt` — Open multi-select album adapter with cover, sort text, transition and selection menu.
  - **Purpose:** Reusable album rows for lists, grids, detail sections.
  - **Responsibilities:** Palette cover, transition names, sort-aware secondary text, popup menu, play button opening sorted album queue, FastScroller popup, multi-select.
  - **Inputs:** List<Album>, AlbumSortMode, IAlbumCallback.
  - **Outputs:** albumClick/albumMenuItemClick/albumsMenuItemClick; openQueue with shuffle Off.
  - **Dependencies:** AbsMultiSelectAdapter, MediaEntryViewHolder, OnClickMenu, PlayerViewModel, loadPaletteImage.
  - **Consumed by:** AlbumListFragment, AlbumDetailFragment (similar albums), ArtistDetailFragment, DetailListFragment, HomeFragment.
  - **Related files:** `ui/adapters/album/SimpleAlbumAdapter.kt`, `ui/screen/library/albums/AlbumListFragment.kt`.
  - **Notes:** Open createViewHolder/getAlbumText for subclass customization.


- `ui/adapters/album/SimpleAlbumAdapter.kt` — AlbumAdapter subclass showing the album year as secondary text.
  - **Purpose:** Compact album rows displaying year or unknown-year fallback.
  - **Responsibilities:** Override getAlbumText with year string.
  - **Inputs:** FragmentActivity, albums, itemLayoutRes, IAlbumCallback.
  - **Outputs:** Rendered rows; inherits AlbumAdapter click/menu behavior.
  - **Dependencies:** AlbumAdapter, R.string.unknown_year.
  - **Consumed by:** ArtistDetailFragment (grid + horizontal rows).
  - **Related files:** `ui/adapters/album/AlbumAdapter.kt`, `ui/screen/library/artists/ArtistDetailFragment.kt`.
  - **Notes:** Used inside HorizontalListAdapter for similar/albums sections.


- `ui/component/base/AbsBaseActivity.kt` — Base activity handling storage/notification permissions and music volume stream.
  - **Purpose:** Centralize runtime permission requests and MUSIC volume stream setup.
  - **Responsibilities:** Request storage/notifications per SDK, Snackbar rationale or settings redirect, permission-change callbacks, Bluetooth permission handling.
  - **Inputs:** Target SDK-derived permission arrays; optional denied message.
  - **Outputs:** onHasPermissionsChanged; granted-state tracking.
  - **Dependencies:** AbsThemeActivity, ActivityCompat, Snackbar, AudioManager.
  - **Consumed by:** AbsSlidingMusicPanelActivity, AbsTagEditorActivity, MainActivity, PermissionsActivity.
  - **Related files:** `ui/component/base/AbsThemeActivity.kt`, `ui/screen/permissions/PermissionsActivity.kt`.
  - **Notes:** Constants PERMISSION_REQUEST (100), BLUETOOTH_PERMISSION_REQUEST (101).


- `ui/component/base/AbsThemeActivity.kt` — Base activity applying theme (dynamic colors, black theme, custom font), edge-to-edge, rotation lock.
  - **Purpose:** Apply app theme and window system-bar styling before content.
  - **Responsibilities:** createAppTheme + setTheme, DynamicColors with seed, BlackThemeOverlay/CustomFont overlays, edge-to-edge, light status/nav bars, rotation lock.
  - **Inputs:** Preferences (theme, dynamic colors, rotation lock, custom font).
  - **Outputs:** Themed window with correct system-bar appearance.
  - **Dependencies:** AppCompatActivity, DynamicColors, enableEdgeToEdge, WindowInsetsControllerCompat, Preferences.
  - **Consumed by:** AbsBaseActivity (all activities).
  - **Related files:** `ui/component/base/AbsBaseActivity.kt`, `core/model/theme/AppTheme.kt`.
  - **Notes:** setLightStatusBar/setLightNavigationBar helpers used by player screens.


- `ui/component/base/AbsMainActivityFragment.kt` — MainActivity fragment base exposing PlayerViewModel/LibraryViewModel and reacting to playback events.
  - **Purpose:** Shared base for fragments hosted inside MainActivity.
  - **Responsibilities:** Activity-scoped PlayerViewModel/LibraryViewModel, MenuProvider registration, MediaEvent collection, window insets and bottom margins; conditionally style Aurora hierarchies and install the hamburger drawer entry (`ic_menu_24dp` with the library-drawer description) for library destinations.
  - **Inputs:** MediaEvent flow, preferences (PLAY_ON_STARTUP_MODE, OPEN_ON_PLAY).
  - **Outputs:** onFavoriteContentChanged/onMediaContentChanged hooks; panel expansion.
  - **Dependencies:** Fragment, MenuProvider, Koin activityViewModel, applyWindowInsets, MainActivity.
  - **Consumed by:** AbsRecyclerViewFragment, lyrics/settings/equalizer fragments.
  - **Related files:** `ui/screen/MainActivity.kt`, `ui/screen/library/LibraryViewModel.kt`, `ui/screen/player/PlayerViewModel.kt`, `res/drawable/ic_menu_24dp.xml`.
  - **Notes:** expandPanel triggered when playback starts or restores per preferences. The former configurable category strip is intentionally no longer injected anywhere (user decision recorded in ADR-037); category navigation lives in the drawer.


- `ui/component/base/AbsRecyclerViewFragment.kt` - Base fragment with RecyclerView, toolbar, shuffle button, fast-scroller, mini-player margins and optional inline quick-search bar.
  - **Purpose:** Standard library list scaffolding with toolbar, shuffle FAB, empty view and quick search.
  - **Responsibilities:** Create adapter/layout manager, FastScroller, shuffle click/long-press (ShuffleModeFragment), mini-player/FAB margins, library menu, scroll-to-top; hosts `QuickSearchBar` in a ComposeView when the fragment implements `QuickSearchable`, restores/saves the fragment-owned query, mirrors it in Compose, replays a restored non-blank query, adds bottom padding/offset and forwards queries.
  - **Inputs:** Abstract createAdapter/createLayoutManager, isShuffleVisible, titleRes, QuickSearchable implementation.
  - **Outputs:** Shuffle/scan/equalizer/import actions; empty-state visibility; filtered list callbacks.
  - **Dependencies:** AbsMainActivityFragment, FragmentMainRecyclerBinding, FastScroller, ImportPlaylistDialog, ShuffleModeFragment, NavController, QuickSearchable, QuickSearchBar, SsilencceTheme.
  - **Consumed by:** AbsRecyclerViewCustomGridSizeFragment and all library list fragments.
  - **Related files:** `ui/component/base/AbsRecyclerViewCustomGridSizeFragment.kt`, `ui/component/base/QuickSearchable.kt`, `ui/component/compose/QuickSearchBar.kt`, `ui/component/views/TopAppBarLayout.kt`.
  - **Notes:** Finishes CAB action mode on pause; dismisses shuffle dialog on PlaybackStarted. The quick-search bar only appears for QuickSearchable fragments; list padding and FAB offset are adjusted while it is visible. The query is saved in `onSaveInstanceState` and restored before the bar is composed; a restored query is replayed once so text and filtered list stay aligned (ADR-044).


- `ui/component/base/AbsRecyclerViewCustomGridSizeFragment.kt` - Extends AbsRecyclerViewFragment with list/grid modes, configurable grid size/view type and quick-search-aware bottom padding.
  - **Purpose:** Add list/grid switching, grid-size and view-type menus to library lists.
  - **Responsibilities:** Persist grid size/view type, rebuild adapter/layout on change, item layout per GridViewType, menu setup with max columns, include quick-search padding in recycler padding.
  - **Inputs:** Preferences-backed getSaved*/save* overrides, maxGridSize per orientation.
  - **Outputs:** viewType/gridSize changes triggering invalidateAdapter/invalidateLayoutManager.
  - **Dependencies:** AbsRecyclerViewFragment, GridViewType, R.integer column resources.
  - **Consumed by:** SongListFragment, AlbumListFragment, ArtistListFragment, GenresListFragment, PlaylistListFragment, YearsListFragment, FoldersListFragment.
  - **Related files:** `ui/component/base/AbsRecyclerViewFragment.kt`, `core/model/GridViewType.kt`, `ui/component/base/QuickSearchable.kt`.
  - **Notes:** Grid mode when gridSize exceeds maxGridSizeForList; recycler padding per layout plus the quick-search bottom offset.


- `ui/component/base/AbsPlayerFragment.kt` - Player screen base fragment (toolbar, song menu, animated background, gestures, player colors, dialogs, A-B repeat cycle, quick-access row, web screens, screencast, cast and share/art actions).
  - **Purpose:** Common behavior for all seven now-playing style fragments.
  - **Responsibilities:** PlayerViewModel/LibraryViewModel wiring, gesture handling, color scheme animation, blur background, now-playing menu, quick actions, favorite toggling, navigation helpers.
  - **Inputs:** MediaEvent/currentSong/colorScheme flows, Preferences gestures and cover actions.
  - **Outputs:** onQuickActionEvent dispatch, PlayerColorScheme application, goToAlbum/Artist/Genre navigation.
  - **Dependencies:** PlayerViewModel, PlayerGesturesController, CoverPagerFragment, Coil BlurTransformation, WebSearchDialog, DeleteSongsDialog, AddToPlaylistDialog.
  - **Consumed by:** Default/FullCover/Gradient/Plain/M3/Expressive/Peek player fragments.
  - **Related files:** `ui/screen/player/styles/*`, `ui/component/base/AbsPlayerControlsFragment.kt`, `ui/screen/player/PlayerViewModel.kt`.
  - **Notes:** goToDestination helper hides bottom bar and collapses sheet before navigating.


- `ui/component/base/AbsPlayerControlsFragment.kt` — Player controls base fragment (play/pause, repeat, shuffle, slider, titles) with animations.
  - **Purpose:** Shared playback controls behavior across player styles and mini-player.
  - **Responsibilities:** Collect progress/duration/play-pause/repeat/shuffle flows, update MusicSlider and texts, skip-button hold/tap handling, extra-info long-press dialog, preference change handling.
  - **Inputs:** PlayerViewModel flows, MusicSlider listener, Preferences keys.
  - **Outputs:** Seek commands, play/pause/repeat/shuffle UI updates, marquee toggling.
  - **Dependencies:** MusicSlider, PlayerAnimator, ExtraInfoPreferenceDialog, SkipButtonTouchHandler, MainActivity panel state.
  - **Consumed by:** Default/Gradient/Plain/M3/Expressive/Peek/FullCover controls fragments; SkipButtonTouchHandler by MiniPlayerFragment.
  - **Related files:** `ui/screen/player/styles/*PlayerControlsFragment.kt`, `ui/screen/other/MiniPlayerFragment.kt`, `ui/component/views/MusicSlider.kt`.
  - **Notes:** Requires parent AbsPlayerFragment; prefers remaining time on total-time click. Shared-preference callbacks return early when `view` is null so a queued notification cannot touch destroyed controls (ADR-047).


- `ui/component/base/AbsSlidingMusicPanelActivity.kt` — Base activity with sliding mini-player bottom sheet, bottom nav/rail, player styles, gestures, search.
  - **Purpose:** Main app shell combining library navigation, mini-player sheet and player container.
  - **Responsibilities:** BottomSheetBehavior state management, player style selection, bottom nav/rail tab updates, hold-tab search, back press routing, queue-driven sheet hiding, preference reactions; optional Aurora ribbon/navigation/More scrim/drawer, content visibility/accessibility, background keyboard-focus blocking and shell-owned navigation clearance.
  - **Inputs:** MediaControllerOwner connection, PlayerViewModel queue/currentSong/colorScheme flows, Preferences.
  - **Outputs:** Panel expand/collapse, library margins, tab menu rebuild, player fragment replacement.
  - **Dependencies:** AbsBaseActivity, MediaControllerOwner, MiniPlayerFragment, retained player styles, AuroraPlayerFragment, Aurora Compose components, BottomSheetBehavior, NavigationBarView.
  - **Consumed by:** MainActivity.
  - **Related files:** `ui/screen/MainActivity.kt`, `ui/screen/other/MiniPlayerFragment.kt`, `ui/component/base/AbsBaseActivity.kt`.
  - **Notes:** STATE_HIDDEN clears the queue (RemoveAllSongs); peek height accounts for bottom insets. Peek remains wrap-content; covered content is excluded from focus traversal. Shell destinations save isolated stacks; filtered search replacement discards the old active/saved Search. More preserves destination-owned sheet hiding and is dismissed before nested-settings Back navigation. Shared-preference callbacks return early when the activity is finishing/destroyed (ADR-047).


- `ui/component/base/AbsTagEditorActivity.kt` — Tag editor base activity: image selection (Photo Picker), SAF writing, Coil loading, tag saving.
  - **Purpose:** Shared scaffolding for song/album/artist tag editors.
  - **Responsibilities:** Photo Picker + write-request launchers, image dialog (download/pick/web-search/restore/remove), Coil loading to bitmap, save with MediaStore write request, genre selector; optional view-owned Aurora backdrop and conditional legacy styling.
  - **Inputs:** EditTarget extra, TagEditorViewModel, abstract propertyMap/placeholderDrawable.
  - **Outputs:** viewModel.write results observed; image bitmap stored in ViewModel.
  - **Dependencies:** AbsBaseActivity, TagEditorViewModel, Coil SingletonImageLoader, MediaStore.createWriteRequest, Jaudiotagger GenreTypes.
  - **Consumed by:** SongTagEditorActivity, AlbumTagEditorActivity, ArtistTagEditorActivity.
  - **Related files:** `ui/screen/tageditor/*TagEditorActivity.kt`, `ui/screen/tageditor/TagEditorViewModel.kt`.
  - **Notes:** EXTRA_TARGET parcelable extra; pre-R falls back to direct writeTags.


- `ui/component/base/AbsMultiSelectAdapter.kt` — Abstract adapter implementing multi-select mode with ActionMode (CAB) and select-all.
  - **Purpose:** Reusable contextual action-bar multi-selection for entity adapters.
  - **Responsibilities:** Start/finish ActionMode, toggle/check-all selection, CAB title (name or "x selected"), back-press callback, dispatch onMultipleItemAction.
  - **Inputs:** FragmentActivity, menuRes, getIdentifier/getName overrides.
  - **Outputs:** onMultipleItemAction with selection list; activated item states.
  - **Dependencies:** FragmentActivity, ActionMode, OnBackPressedCallback, RecyclerView.
  - **Consumed by:** SongAdapter, AlbumAdapter, ArtistAdapter, YearAdapter, PlaylistAdapter, SearchAdapter, FileAdapter.
  - **Related files:** `ui/adapters/*`, `ui/component/base/AbsRecyclerViewFragment.kt`.
  - **Notes:** select-all uses menu id action_multi_select_adapter_check_all.


- `ui/component/base/MediaEntryViewHolder.kt` — Base ViewHolder binding common media item views and resolving Koin ViewModels.
  - **Purpose:** Standard view holder for media list rows.
  - **Responsibilities:** Find common row views, click/long-click listeners, Koin getViewModel helper; conditionally apply Aurora media-row styling and minimum menu targets.
  - **Inputs:** Inflated item View.
  - **Outputs:** Click events to subclasses; ViewModel access.
  - **Dependencies:** RecyclerView.ViewHolder, Koin getViewModel, MaterialButton.
  - **Consumed by:** SongAdapter, AlbumAdapter, ArtistAdapter, GenreAdapter, YearAdapter, PlaylistAdapter, FileAdapter.
  - **Related files:** `ui/adapters/*`, `extensions/AdapterExt.kt`.
  - **Notes:** Optional views may be null depending on layout; onLongClick defaults false.


- `ui/component/views/TopAppBarLayout.kt` — Custom AppBarLayout toggling between simple or collapsing mode per preference.
  - **Purpose:** Provide toolbar/app-bar variant chosen by app-bar preference.
  - **Responsibilities:** Inflate Simple or Collapsing appbar bindings, expose toolbar/title, pinWhenScrolled; use the simple floating-header anatomy under Aurora without changing the saved app-bar preference.
  - **Inputs:** AppBarMode preference (COLLAPSING/SIMPLE).
  - **Outputs:** Configured MaterialToolbar and title accessor.
  - **Dependencies:** AppBarLayout, CollapsingAppbarLayoutBinding, SimpleAppbarLayoutBinding, MaterialShapeDrawable.
  - **Consumed by:** fragment_main_recycler layout, SettingsFragment, CollapsibleAppBar compose wrapper.
  - **Related files:** `ui/component/base/AbsRecyclerViewFragment.kt`, `ui/component/compose/CollapsibleAppBar.kt`, `util/Preferences.kt`.
  - **Notes:** Landscape disables fitsSystemWindows in collapsing mode.


- `ui/component/views/SquigglyProgress.kt` — Animated sine-wave progress drawable used by the player seekbar.
  - **Purpose:** Wave-shaped seek progress with smooth amplitude transition.
  - **Responsibilities:** Draw animated sine wave up to progress level, transition region, tint/alpha handling, ValueAnimator-driven height.
  - **Inputs:** Level (0–10000), stroke/amplitude/wave length, animate flag.
  - **Outputs:** Progress bar rendering via Drawable draw.
  - **Dependencies:** Drawable, ValueAnimator, ColorStateList, withAlpha.
  - **Consumed by:** MusicSlider via media_squiggly_progress drawable.
  - **Related files:** `ui/component/views/MusicSlider.kt`, `res/drawable/media_squiggly_progress.xml`.
  - **Notes:** Ported from AOSP media progress; onLevelChange returns animate state.


- `ui/component/views/PlaceholderDrawable.kt` — Placeholder drawable with surface color and centered tinted icon.
  - **Purpose:** Material-themed image placeholder for covers.
  - **Responsibilities:** Draw surfaceContainerHighest rect with centered onSurfaceVariant icon, expose color attrs.
  - **Inputs:** Context, foreground drawable resource.
  - **Outputs:** getPlaceholderDrawable(context, res) factory.
  - **Dependencies:** Drawable, MaterialColors, getDrawableCompat.
  - **Consumed by:** CoilExtension placeholder/error, tag editor activities, FullCoverPlayerFragment, PaletteColor/LyricsScreen color resolution.
  - **Related files:** `coil/CoilExtension.kt`, `core/model/PaletteColor.kt`, `ui/screen/lyrics/LyricsScreen.kt`.
  - **Notes:** BACKGROUND_COLOR/FOREGROUND_COLOR attrs reused for palette extraction.


- `ui/component/views/PermissionView.kt` — XML-configurable FrameLayout showing number, title, description and button for a permission.
  - **Purpose:** Permission card UI for the permissions onboarding screen.
  - **Responsibilities:** Read PermissionView attributes, render number/title/description/button, granted state swaps to check icon + "Granted".
  - **Inputs:** Styled attributes (permissionTitle, permissionDescription, buttonIcon, buttonText, number, granted).
  - **Outputs:** isGranted/setGranted; button click listener.
  - **Dependencies:** FrameLayout, MaterialButton, permission_view layout.
  - **Consumed by:** PermissionsActivity.
  - **Related files:** `ui/screen/permissions/PermissionsActivity.kt`, `res/layout/permission_view.xml`.
  - **Notes:** Inflates permission_view and re-parents content.


- `ui/component/views/MusicSlider.kt` — FrameLayout wrapping a SeekBar (with SquigglyProgress) or Material Slider per preference.
  - **Purpose:** Unified progress widget switching between squiggly SeekBar and Material Slider.
  - **Responsibilities:** Inflate music_squiggly_slider or music_progress_slider, preserve state across swaps, forward progress/touch listeners, apply colors.
  - **Inputs:** Attributes (thumb/track height, squigglyStyle), Preferences.squigglySeekBar.
  - **Outputs:** Listener callbacks onProgressChanged/onStartTrackingTouch/onStopTrackingTouch; isTrackingTouch.
  - **Dependencies:** FrameLayout, SeekBar, Slider, SquigglyProgress, applyColor.
  - **Consumed by:** AbsPlayerControlsFragment and all player controls layouts, MiniPlayerFragment.
  - **Related files:** `ui/component/base/AbsPlayerControlsFragment.kt`, `res/layout/music_squiggly_slider.xml`.
  - **Notes:** setUseSquiggly swaps internal view preserving min/max/progress/color.


- `ui/component/views/MorphicIconButton.kt` — Button animating shape morphosis (square to circle) with rotating icon (based on XMusic).
  - **Purpose:** Decorative animated button for expressive player controls.
  - **Responsibilities:** Morph between MaterialShapes via androidx.graphics.shapes Morph, infinite icon rotation, icon tint, background tint list.
  - **Inputs:** Attributes (icon, tint, morphDuration, rotationDuration, morphShape).
  - **Outputs:** morphToShape and isRotating state; custom onDraw path.
  - **Dependencies:** ValueAnimator, Morph/toPath, MaterialShapes, MaterialColors, AnimatedVectorDrawable.
  - **Consumed by:** Expressive player controls layout, item_detail_header layout.
  - **Related files:** `ui/screen/player/styles/expressivestyle/*`, `extensions/resources/ColorExt.kt`, `extensions/resources/ViewExt.kt`.
  - **Notes:** Throws on setBackground/setBackgroundColor; use setBackgroundTintList.


- `ui/component/views/InfoView.kt` — Simple view with title and text, horizontal/vertical styles via XML attributes.
  - **Purpose:** Reusable labeled-info block for detail screens.
  - **Responsibilities:** Inflate horizontal/vertical info layout per infoStyle attribute, bind title/text with optional colors.
  - **Inputs:** Attributes (infoStyle, infoTitle, infoText, colors).
  - **Outputs:** setTitle/setText updates.
  - **Dependencies:** FrameLayout, TextView, info_item_horizontal/info_item_vertical layouts.
  - **Consumed by:** SongDetailFragment (metadata rows).
  - **Related files:** `ui/screen/info/SongDetailFragment.kt`, `res/layout/info_item_horizontal.xml`.
  - **Notes:** STYLE_HOR = 0, STYLE_VER = 1.


- `ui/component/views/SsilencceShapeableImageView.kt` — Shapeable ImageView forcing square measure and rounded corners.
  - **Purpose:** Square cover images with fully rounded (circle) or rounded corners.
  - **Responsibilities:** Force square onMeasure, corner size from width on layout, updateCornerSize for rounded corners.
  - **Inputs:** Styled attributes from SsilencceShapeableImageView.
  - **Outputs:** Squared, rounded ShapeableImageView.
  - **Dependencies:** ShapeableImageView, ShapeAppearanceModel, CornerFamily.
  - **Consumed by:** item_artist layout (artist covers).
  - **Related files:** `res/layout/item_artist.xml`, `res/values/attrs.xml`.
  - **Notes:** Radius = width/2f gives circular shape.


- `ui/component/views/AnimSlider.kt` — Material Slider animating its value via ObjectAnimator with alpha-colored thumb/track.
  - **Purpose:** Smoothly animated slider for equalizer/settings values.
  - **Responsibilities:** Animate value changes with DecelerateInterpolator, cancel on drag, tint thumb/halo/track with alpha variants.
  - **Inputs:** setValueAnimated(progress), setSliderColor(color), valueFrom/valueTo.
  - **Outputs:** Animated slider value; isDragging flag.
  - **Dependencies:** Slider, ObjectAnimator, toColorStateList, withAlpha.
  - **Consumed by:** Equalizer screen sliders.
  - **Related files:** `ui/screen/equalizer/EqualizerScreen.kt`.
  - **Notes:** Skips animation when target equals current value.


- `ui/component/transform/ZoomOutPageTransformer.kt` — ViewPager PageTransformer with zoom-out effect.
  - **Purpose:** Shrink side pages (zoom-out) during pager swipes.
  - **Responsibilities:** Scale 0.85–1 with margins translation; alpha reset off-screen.
  - **Inputs:** Page position [-1,1].
  - **Outputs:** Transformed page scale/translation/alpha.
  - **Dependencies:** ViewPager.PageTransformer.
  - **Consumed by:** PlayerTransition factory (ZoomOut enum).
  - **Related files:** `core/model/player/PlayerTransition.kt`, `ui/component/transform/CarouselPagerTransformer.kt`.
  - **Notes:** MIN_SCALE 0.85; MIN_ALPHA unused.


- `ui/component/transform/VerticalStackTransformation.kt` — PageTransformer stacking pages vertically with scale and Y offset.
  - **Purpose:** Stack incoming pages with vertical offset and shrink.
  - **Responsibilities:** Scale X shrink, translation X/Y for pages at position >= 0.
  - **Inputs:** Page position.
  - **Outputs:** Scaled/offset page transforms.
  - **Dependencies:** ViewPager.PageTransformer.
  - **Consumed by:** PlayerTransition factory (Stack enum).
  - **Related files:** `core/model/player/PlayerTransition.kt`.
  - **Notes:** Only transforms pages to the right; left pages untouched.


- `ui/component/transform/VerticalFlipTransformation.kt` — PageTransformer rotating pages on the X axis (3D camera).
  - **Purpose:** Vertical 3D flip page transitions.
  - **Responsibilities:** rotationX by position with cameraDistance, visibility culling near center, alpha fade off-screen.
  - **Inputs:** Page position.
  - **Outputs:** rotationX/translationX/alpha/visibility transforms.
  - **Dependencies:** ViewPager.PageTransformer, isInvisible/isVisible.
  - **Consumed by:** PlayerTransition factory (VerticalFlip enum).
  - **Related files:** `core/model/player/PlayerTransition.kt`.
  - **Notes:** Hides pages beyond ±0.5 position.


- `ui/component/transform/SimpleTransformer.kt` — No-op PageTransformer.
  - **Purpose:** Default pager behavior without transforms.
  - **Responsibilities:** No-op transformPage.
  - **Inputs:** Page position (ignored).
  - **Outputs:** —.
  - **Dependencies:** ViewPager.PageTransformer.
  - **Consumed by:** Default pager when no transition selected.
  - **Related files:** `core/model/player/PlayerTransition.kt`.
  - **Notes:** Class named SimplePageTransformer.


- `ui/component/transform/ParallaxPagerTransformer.kt` — PageTransformer moving an inner view with parallax at configurable speed.
  - **Purpose:** Parallax shift of an inner view while swiping.
  - **Responsibilities:** Translate inner view by position * width * speed; reset scale.
  - **Inputs:** View id of parallax child; setSpeed.
  - **Outputs:** parallaxView translationX.
  - **Dependencies:** ViewPager.PageTransformer, findViewById.
  - **Consumed by:** PlayerTransition factory (Parallax enum with speed 0.3).
  - **Related files:** `core/model/player/PlayerTransition.kt`.
  - **Notes:** Only applies within position (-1,1).


- `ui/component/transform/HorizontalFlipTransformation.kt` — PageTransformer rotating pages on the Y axis (3D camera).
  - **Purpose:** Horizontal 3D flip page transitions.
  - **Responsibilities:** rotationY by position with large cameraDistance, visibility culling, alpha fade off-screen.
  - **Inputs:** Page position.
  - **Outputs:** rotationY/translationX/alpha/visibility transforms.
  - **Dependencies:** ViewPager.PageTransformer, isInvisible/isVisible.
  - **Consumed by:** PlayerTransition factory (HorizontalFlip enum).
  - **Related files:** `core/model/player/PlayerTransition.kt`.
  - **Notes:** Mirror of VerticalFlipTransformation on Y axis.


- `ui/component/transform/HingeTransformation.kt` — PageTransformer rotating from the top-left pivot like a hinge, with fade.
  - **Purpose:** Hinge-style page rotation for leaving pages.
  - **Responsibilities:** rotation from top-left pivot with fade-out, hide off-screen pages.
  - **Inputs:** Page position.
  - **Outputs:** rotation/alpha/visibility transforms.
  - **Dependencies:** ViewPager.PageTransformer, isVisible.
  - **Consumed by:** PlayerTransition factory (Hinge enum).
  - **Related files:** `core/model/player/PlayerTransition.kt`.
  - **Notes:** Pivot (0,0); incoming pages stay static.


- `ui/component/transform/DepthTransformation.kt` — Classic depth PageTransformer (incoming page grows and fades in).
  - **Purpose:** Depth effect where incoming page scales up and fades in.
  - **Responsibilities:** Fade/scale incoming page (0.5–1), counteract default slide.
  - **Inputs:** Page position.
  - **Outputs:** alpha/translationX/scale transforms.
  - **Dependencies:** ViewPager.PageTransformer.
  - **Consumed by:** PlayerTransition factory (Depth enum).
  - **Related files:** `core/model/player/PlayerTransition.kt`.
  - **Notes:** MIN_SCALE 0.5.


- `ui/component/transform/CascadingPageTransformer.kt` — Cascading transformer with rotation, offset and decreasing scale of side pages.
  - **Purpose:** Cascade effect rotating/offsetting leaving pages, shrinking side pages.
  - **Responsibilities:** Rotation/translation for negative positions, scale-down with translationY for positive positions.
  - **Inputs:** Page position.
  - **Outputs:** alpha/rotation/scale/translation transforms.
  - **Dependencies:** ViewPager.PageTransformer.
  - **Consumed by:** PlayerTransition factory (Cascading enum).
  - **Related files:** `core/model/player/PlayerTransition.kt`.
  - **Notes:** mScaleOffset 40 px; guards zero-width and non-finite scale.


- `ui/component/transform/CarouselPagerTransformer.kt` — Carousel-style transformer scaling/offsetting pages by distance to center.
  - **Purpose:** Carousel cover-pager effect in the now-playing screen.
  - **Responsibilities:** Compute offset from pager center, scale and translate pages proportionally.
  - **Inputs:** Context (dp2px), page position via view left/scrollX.
  - **Outputs:** scaleX/scaleY/translationX per page.
  - **Dependencies:** ViewPager.PageTransformer, ViewPager.
  - **Consumed by:** CoverPagerFragment (setPageTransformer).
  - **Related files:** `ui/screen/player/cover/CoverPagerFragment.kt`, `core/model/player/PlayerTransition.kt`.
  - **Notes:** maxTranslateOffsetX 180dp; scaleFactor clamped positive.


- `ui/component/menu/MenuClick.kt` — PopupMenu factory and abstract OnClickMenu class showing a popup on click.
  - **Purpose:** Shared popup-menu creation and click handling for adapters.
  - **Responsibilities:** newPopupMenu factory with menu consumer, OnClickMenu wiring onClick → show popup → onMenuItemClick.
  - **Inputs:** Anchor view, menu resource.
  - **Outputs:** Prepared PopupMenu; menu item clicks.
  - **Dependencies:** PopupMenu, Menu.
  - **Consumed by:** SongAdapter, AlbumAdapter, ArtistAdapter, YearAdapter, PlaylistAdapter, SearchAdapter, FileAdapter, AbsPlayerFragment.
  - **Related files:** `ui/component/menu/MenuItemClickExt.kt`, `ui/adapters/*`.
  - **Notes:** Subclasses override popupMenuRes and onPreparePopup.


- `ui/component/menu/MenuItemClickExt.kt` - Extensions resolving menu item actions (play next, add to queue/playlist, share, delete, edit tags, navigate, export formats, entity shortcuts).
  - **Purpose:** Central song/album/artist/playlist menu action handling.
  - **Responsibilities:** onSongMenu/onSongsMenu/onAlbumMenu/onAlbumsMenu/onArtistMenu/onArtistsMenu/onPlaylistMenu/onPlaylistsMenu routing to PlayerViewModel, dialogs, tag editors, navigation, M3U/M3U8/PLS export chooser, and entity shortcut add dialog.
  - **Inputs:** Fragment, MenuItem, entity models.
  - **Outputs:** Queue/play commands, toast feedback, dialog/navigation side effects, export/shortcut actions.
  - **Dependencies:** PlayerViewModel, LibraryViewModel, AddToPlaylistDialog, DeleteSongsDialog, DeletePlaylistDialog, EditPlaylistDialog, M3UWriter, PlaylistExportFormat, ShortcutHelper, NavigationExt args, BatchTagEditorActivity/BatchTagEditorViewModel.
  - **Consumed by:** AbsPlayerFragment, AbsRecyclerViewFragment menus, library fragments, SearchFragment.
  - **Related files:** `ui/component/menu/MenuClick.kt`, `extensions/navigation/NavigationExt.kt`, `util/m3u/M3UWriter.kt`, `util/ShortcutHelper.kt`, `ui/screen/tageditor/BatchTagEditorActivity.kt`.
  - **Notes:** Playlist actions guard PlaylistWithSongs.Empty; list actions flatten songs on IO. Format selection is shared by single and batch export; shortcut confirmation reports launcher capacity failures (ADR-044). Composer artist menus suppress tag-editor and add-to-home actions because neither applies to composer names. The multi-select tag action deduplicates IDs, enforces the 500-song batch limit and launches BatchTagEditorActivity; single album/artist actions still use the legacy editors.


- `ui/component/preferences/ThemePreference.kt` — Preference with a MaterialButtonToggleGroup for light/dark/system theme.
  - **Purpose:** Inline theme selector row in appearance settings.
  - **Responsibilities:** Bind toggle group to GeneralTheme, disable for BLACK theme, notify callback on selection.
  - **Inputs:** Preferences.generalTheme; selector ids for LIGHT/DARK/AUTO.
  - **Outputs:** ThemePreference.Callback.onThemeSelected.
  - **Dependencies:** Preference, MaterialButtonToggleGroup.
  - **Consumed by:** PreferencesScreenFragment (GENERAL_THEME).
  - **Related files:** `ui/screen/settings/PreferencesScreenFragment.kt`, `res/xml/preferences_screen_appearance.xml`.
  - **Notes:** Removes ripple via null item background.


- `ui/component/preferences/SwitchWithButtonPreference.kt` — SwitchPreferenceCompat adding an extra button with listener.
  - **Purpose:** Switch row with secondary action button (whitelist/blacklist manage).
  - **Responsibilities:** Bind extra button from widget frame, expose OnButtonPressedListener.
  - **Inputs:** R.id.button in preference layout.
  - **Outputs:** onButtonPressed callbacks.
  - **Dependencies:** SwitchPreferenceCompat, Button.
  - **Consumed by:** PreferencesScreenFragment (WHITELIST_ENABLED/BLACKLIST_ENABLED).
  - **Related files:** `ui/screen/settings/PreferencesScreenFragment.kt`, `res/xml/preferences_screen_library.xml`.
  - **Notes:** Clears button/listener on detach.


- `ui/component/preferences/ProgressIndicatorPreference.kt` — Preference showing/hiding a CircularProgressIndicator in its widget frame.
  - **Purpose:** Busy indicator for long-running preference actions (update search).
  - **Responsibilities:** Bind android.R.id.progress indicator, show/hide via state flag.
  - **Inputs:** showProgressIndicator/hideProgressIndicator calls.
  - **Outputs:** CircularProgressIndicator visibility.
  - **Dependencies:** Preference, CircularProgressIndicator.
  - **Consumed by:** PreferencesScreenFragment (search_for_update).
  - **Related files:** `ui/screen/settings/PreferencesScreenFragment.kt`, `res/xml/preferences_screen_network.xml`.
  - **Notes:** Used by UpdateViewModel search state.


- `ui/component/preferences/CustomDialogPreference.kt` — Simple DialogPreference without customization.
  - **Purpose:** Plain dialog-backed preference for settings entries.
  - **Responsibilities:** Extends DialogPreference with default styling.
  - **Inputs:** AttributeSet, style attrs.
  - **Outputs:** Dialog-launching preference.
  - **Dependencies:** DialogPreference.
  - **Consumed by:** PreferencesScreenFragment via preferences_screen_playback/appearance/now_playing/network XML.
  - **Related files:** `res/xml/preferences_screen_*.xml`.
  - **Notes:** Dialog fragments launched from PreferencesScreenFragment.onPreferenceDisplayDialog.


- `ui/component/preferences/dialog/SongClickActionPreferenceDialog.kt` — Compose DialogFragment choosing the tap-on-song behavior (SongClickBehavior).
  - **Purpose:** Pick song-click behavior from enum entries.
  - **Responsibilities:** Compose LazyColumn with DialogListItemWithRadio, persist Preferences.songClickAction, scroll to current selection.
  - **Inputs:** Preferences.songClickAction.
  - **Outputs:** Updated SongClickBehavior preference.
  - **Dependencies:** DialogFragment, MaterialAlertDialogBuilder, SsilencceTheme, DialogListItemWithRadio.
  - **Consumed by:** PreferencesScreenFragment (ON_SONG_CLICK_ACTION).
  - **Related files:** `ui/screen/settings/PreferencesScreenFragment.kt`, `core/model/action/SongClickBehavior.kt`.
  - **Notes:** Immediate persistence on selection; close button only.



- `ui/component/preferences/dialog/HeadsetButtonMappingDialog.kt` - Compose Playback -> Headphones mapping dialog.
  - **Purpose:** Configure single/double/triple/long actions, optional 4+ Seek forward and multi-press delay.
  - **Responsibilities:** Four action rows with nested radio picker; whole-row 4+ switch; 250-600 ms FilterChips; OEM/AVRCP/long-press disclaimer; Reset to defaults; Close; immediate persistence through Preferences.
  - **Inputs:** Preferences headset getters/setters, HeadsetButtonAction entries, action title resources.
  - **Outputs:** Persisted headset action/4+/delay values; dialog dismissal.
  - **Dependencies:** MaterialAlertDialogBuilder, ComposeView, SsilencceTheme, DialogListItemWithRadio, HeadsetButtonPolicy.
  - **Consumed by:** PreferencesScreenFragment (HEADSET_BUTTON_MAPPING).
  - **Related files:** util/Preferences.kt, core/model/action/HeadsetButtonAction.kt, res/values/strings.xml.
  - **Notes:** No exclusivity restrictions; gestures may share an action. Reset restores the legacy defaults. Long-press support is best-effort and documented.

- `ui/component/preferences/dialog/IgnoreKeywordsPreferenceDialog.kt` — Compose dialog for user-editable sort ignore keywords.
  - **Purpose:** View, add and remove leading keywords ignored while article-ignore sorting is enabled.
  - **Responsibilities:** Immediate persistence, alphabetical display, add/remove validation messages, count/empty state and Done action.
  - **Inputs:** Preferences.sortingIgnoreKeywords, IgnoreKeywordPolicy limits/add/remove results.
  - **Outputs:** Updated string-set preference; dialog dismissal.
  - **Dependencies:** MaterialAlertDialogBuilder, ComposeView, SsilencceTheme, IgnoreKeywordPolicy, Preferences.
  - **Consumed by:** PreferencesScreenFragment (SORTING_IGNORE_KEYWORDS).
  - **Related files:** util/Preferences.kt, core/sort/IgnoreKeywordPolicy.kt, res/xml/preferences_screen_library.xml, res/values/strings.xml.
  - **Notes:** The row is a dependent CustomDialogPreference disabled while the existing article switch is off; keyword changes are only used by sorting.


- `ui/component/preferences/dialog/SingleSelectionDialog.kt` — Compose-based single-selection PreferenceDialogFragmentCompat with radio buttons.
  - **Purpose:** Compose replacement for ListPreference selection dialogs.
  - **Responsibilities:** Read entries/entryValues, Compose radio list, persist via ListPreference.setValue, state save/restore.
  - **Inputs:** ListPreference key via newInstance.
  - **Outputs:** Updated ListPreference value.
  - **Dependencies:** PreferenceDialogFragmentCompat, ComposeView, DialogListItemWithRadio.
  - **Consumed by:** PreferencesScreenFragment for generic ListPreference dialogs.
  - **Related files:** `ui/screen/settings/PreferencesScreenFragment.kt`, `ui/component/compose/DialogListItem.kt`.
  - **Notes:** Hides dialog buttons; selection immediately closes dialog.


- `ui/component/preferences/dialog/NowPlayingScreenPreferenceDialog.kt` — Dialog with ViewPager of now-playing styles, color scheme and transition spinners.
  - **Purpose:** Configure now-playing screen style, per-style color scheme and transition.
  - **Responsibilities:** ViewPager preview of NowPlayingScreen, spinners for PlayerColorSchemeMode and PlayerTransition, persist on OK.
  - **Inputs:** Preferences.nowPlayingScreen, per-style scheme/transition prefs.
  - **Outputs:** Saved now-playing screen/scheme/transition preferences.
  - **Dependencies:** DialogFragment, ViewPager, ArrayAdapters, PreferenceDialogNowPlayingScreenBinding.
  - **Consumed by:** PreferencesScreenFragment (NOW_PLAYING_SCREEN).
  - **Related files:** `ui/screen/settings/PreferencesScreenFragment.kt`, `core/model/theme/NowPlayingScreen.kt`.
  - **Notes:** Disables spinners when only one option supported.


- `ui/component/preferences/dialog/ExtraInfoPreferenceDialog.kt` — Dialog with draggable RecyclerView (ExtraInfoAdapter) for player/widget extra info.
  - **Purpose:** Reorder/enable extra-info metadata fields for player or widgets.
  - **Responsibilities:** Build ExtraInfoAdapter, attach drag, reset-to-default button, persist to preference key on OK.
  - **Inputs:** Title, preference key, default MetadataField list (nowPlaying or appWidgets factories).
  - **Outputs:** Saved extra-info content preference.
  - **Dependencies:** DialogFragment, ExtraInfoAdapter, DialogRecyclerViewBinding, BundleCompat parcelables.
  - **Consumed by:** PreferencesScreenFragment, AbsPlayerControlsFragment, ExpressivePlayerFragment.
  - **Related files:** `ui/adapters/preference/ExtraInfoAdapter.kt`, `util/Preferences.kt`.
  - **Notes:** Saves state across rotation via SAVED_KEY.


- `ui/component/preferences/dialog/ClearQueueActionPreferenceDialog.kt` — Compose DialogFragment choosing the queue-clearing behavior.
  - **Purpose:** Pick queue-clearing behavior (remove all vs keep current).
  - **Responsibilities:** Compose radio list over QueueClearingBehavior entries, persist Preferences.clearQueueAction.
  - **Inputs:** Preferences.clearQueueAction.
  - **Outputs:** Updated QueueClearingBehavior preference.
  - **Dependencies:** DialogFragment, MaterialAlertDialogBuilder, SsilencceTheme, DialogListItemWithRadio.
  - **Consumed by:** PreferencesScreenFragment (ON_CLEAR_QUEUE_ACTION).
  - **Related files:** `ui/screen/settings/PreferencesScreenFragment.kt`, `core/model/action/QueueClearingBehavior.kt`.
  - **Notes:** Immediate persistence; scrolls to selected entry.


- `ui/component/preferences/dialog/CategoriesPreferenceDialog.kt` — Dialog with draggable list (CategoryInfoAdapter) to enable/reorder library categories.
  - **Purpose:** Configure visible and ordered library navigation categories.
  - **Responsibilities:** Build CategoryInfoAdapter, drag attach, reset-to-default, validate at least one visible before persisting.
  - **Inputs:** Preferences.libraryCategories; saved instance state.
  - **Outputs:** Updated LIBRARY_CATEGORIES preference.
  - **Dependencies:** DialogFragment, CategoryInfoAdapter, DialogRecyclerViewBinding.
  - **Consumed by:** PreferencesScreenFragment (LIBRARY_CATEGORIES).
  - **Related files:** `ui/adapters/preference/CategoryInfoAdapter.kt`, `core/model/CategoryInfo.kt`.
  - **Notes:** updateCategories refuses lists with no visible category.


- `ui/component/preferences/dialog/ActionOnCoverPreferenceDialog.kt` — Compose DialogFragment for the cover-gesture action (tap/double-tap/long-press).
  - **Purpose:** Pick a now-playing action for a cover gesture (single/double/long press).
  - **Responsibilities:** Compose LazyColumn with DialogListItemWithRadio; persist selection; enforce action exclusivity across gestures.
  - **Inputs:** EXTRA_KEY preference name, NowPlayingAction entries, SharedPreferences.
  - **Outputs:** Written COVER_*_ACTION preference; dialog dismissal.
  - **Dependencies:** MaterialAlertDialogBuilder, ComposeView, Preferences, NowPlayingAction, SsilencceTheme.
  - **Consumed by:** PreferencesScreenFragment (onDisplayPreferenceDialog for cover gesture prefs).
  - **Related files:** util/Preferences.kt, core/model/action/NowPlayingAction.kt, ui/component/compose/DialogListItem.kt.
  - **Notes:** Exclusivity map prevents duplicate actions across gestures (except Nothing).


- `ui/component/compose/TitledSurface.kt` — Collapsible card composables (TitledCard/SwitchCard) with title, icon and optional switch.
  - **Purpose:** Grouped settings cards with collapsible content areas.
  - **Responsibilities:** Render Card with clickable title row, icon, expand/collapse via AnimatedVisibility; SwitchCard adds toggle.
  - **Inputs:** title, icon Painter, style, color, collapsible, titleEndContent, content slot; SwitchCard checked/onCheckedChange.
  - **Outputs:** Composable Card with animated content visibility.
  - **Dependencies:** Material3 Card, Switch, Icon, AnimatedVisibility, MaterialTheme.
  - **Consumed by:** EqualizerScreen, SoundSettingsScreen, SongDetailFragment.
  - **Related files:** Dialog.kt, MaterialSwitch.kt, EqualizerScreen.kt.
  - **Notes:** Opts into ExperimentalMaterial3ExpressiveApi; content receives PaddingValues(16dp sides/bottom).


- `ui/component/compose/TipView.kt` — Tip/info card with icon and text.
  - **Purpose:** Show informational tip with icon in a card.
  - **Responsibilities:** Render Card with Row of Icon and bodyMedium Text.
  - **Inputs:** text, icon Painter, containerColor, contentColor, modifier.
  - **Outputs:** Composable Card.
  - **Dependencies:** Material3 Card, contentColorFor, painterResource ic_info_24dp.
  - **Consumed by:** EqualizerScreen, ScrobblingServiceLoginFragment.
  - **Related files:** ShapedText.kt, ErrorView.kt.
  - **Notes:** Default icon info; medium shape, 16dp padding, 16dp spacing.


- `ui/component/compose/SmallHeader.kt` — Compact header with image (MediaImage), title, subtitle and optional trailing content.
  - **Purpose:** Compact header row for detail screens.
  - **Responsibilities:** Render MediaImage (72dp, rounded 16dp), bold title, subtitle, trailing composable slot.
  - **Inputs:** title, subtitle, trailingContent, imageModel, imagePlaceholderIconRes, modifier.
  - **Outputs:** Composable Row header.
  - **Dependencies:** MediaImage, MaterialTheme typography/colors.
  - **Consumed by:** SongDetailFragment, QueueScreen.
  - **Related files:** MediaImage.kt, EmptyView.kt.
  - **Notes:** Title/subtitle ellipsized; default placeholder ic_music_note_24dp.


- `ui/component/compose/ShapedText.kt` — Text label with clipped shape, background color and optional click.
  - **Purpose:** Render chip-like text label with shape.
  - **Responsibilities:** Clip shape, apply container background, optional clickable, single-line centered text.
  - **Inputs:** text, containerColor, color, style, shape, onClick, enabled, modifier.
  - **Outputs:** Composable Box label.
  - **Dependencies:** MaterialTheme shapes, contentColorFor, background/clip.
  - **Consumed by:** SoundSettingsScreen, EqualizerScreen, SongDetailFragment, AboutScreen.
  - **Related files:** TipView.kt, EmptyView.kt.
  - **Notes:** Padding 10x8dp; contentColor defaults from container color.


- `ui/component/compose/EmptyView.kt` — Empty state view with circular icon, title, subtitle and optional button.
  - **Purpose:** Show empty-state placeholder content.
  - **Responsibilities:** Circular icon container, title/subtitle text, optional button slot; EmptyViewDefaults/EmptyViewColors.
  - **Inputs:** icon Painter, title, subtitle, colors, iconSize, button composable, modifier.
  - **Outputs:** Composable Column empty state.
  - **Dependencies:** MaterialTheme colors, CircleShape clip.
  - **Consumed by:** AddToPlaylistDialog, EqualizerScreen.
  - **Related files:** ErrorView.kt, MediaPlaceholder.
  - **Notes:** 32dp padding; icon container padded 32dp; colors customizable via data class.


- `ui/component/compose/DialogListItem.kt` — Dialog list items with radio button or checkbox.
  - **Purpose:** Clickable dialog rows with leading selection control.
  - **Responsibilities:** DialogListItemWithRadio/WithCheckBox wrap shared Row with title, subtitle, padding, click handling.
  - **Inputs:** title, subtitle, isSelected, onClick, contentPadding, modifier.
  - **Outputs:** Composable selectable rows.
  - **Dependencies:** Material3 Checkbox, RadioButton, Text.
  - **Consumed by:** LyricsEditorScreen, EqualizerScreen, SingleSelectionDialog, ClearQueueActionPreferenceDialog, SongClickActionPreferenceDialog, ActionOnCoverPreferenceDialog.
  - **Related files:** DialogButton.kt, Dialog.kt.
  - **Notes:** Leading control renders non-clickable; row handles click.


- `ui/component/compose/DialogButton.kt` — Checkbox row for Compose dialogs.
  - **Purpose:** Full-width toggleable checkbox row.
  - **Responsibilities:** toggleable Row with Checkbox and bodyMedium text.
  - **Inputs:** text, isChecked, onValueChange, modifier.
  - **Outputs:** Composable checkbox row.
  - **Dependencies:** Material3 Checkbox, toggleable semantics.
  - **Consumed by:** Dialog.kt (InputDialog additionalContent).
  - **Related files:** Dialog.kt, DialogListItem.kt.
  - **Notes:** Vertical padding 8dp; 16dp spacing between checkbox and text.


- `ui/component/compose/Dialog.kt` — Reusable Compose dialogs: ConfirmDialog and InputDialog.
  - **Purpose:** Reusable AlertDialog-based confirm and input dialogs.
  - **Responsibilities:** ConfirmDialog message/buttons/icon; InputDialog OutlinedTextField with length validation, optional checkbox or additional content.
  - **Inputs:** onConfirm, onDismiss, message, confirm/dismissButton, title, icon, inputHint, inputPrefill, inputMaxLength.
  - **Outputs:** Material3 AlertDialog composables.
  - **Dependencies:** AlertDialog, OutlinedTextField, rememberTextFieldState, DialogCheckBox, KeyboardOptions.
  - **Consumed by:** EqualizerScreen (ConfirmDialog, InputDialog).
  - **Related files:** DialogButton.kt, DialogListItem.kt.
  - **Notes:** Confirm disabled when blank or over max length; suffix shows character count.


- `ui/component/compose/ButtonGroup.kt` — Connected TonalToggleButton group with resolved icons/texts.
  - **Purpose:** Segmented button group for single-choice selection.
  - **Responsibilities:** Row of connected TonalToggleButton with weighted sizing, RadioButton semantics, icon/text resolvers, leading/middle/trailing shapes.
  - **Inputs:** buttonItems, buttonStateResolver, buttonIconResolver, buttonTextResolver, enabled, onSelected.
  - **Outputs:** Composable connected button group.
  - **Dependencies:** Material3 TonalToggleButton, ButtonGroupDefaults (ExperimentalMaterial3ExpressiveApi).
  - **Consumed by:** LyricsEditorScreen, EqualizerScreen.
  - **Related files:** MenuItem.kt, OverflowMenu.kt.
  - **Notes:** Checked button gets 1.5f weight; connected shapes per index.


- `ui/component/compose/AnimatedEqBars.kt` — Animated EQ bars (Canvas + infinite sine animation) active while playing.
  - **Purpose:** Decorative animated equalizer bars.
  - **Responsibilities:** Canvas draws rounded bars; per-bar sine phase read during drawing; infinite transition exists only during permitted playback animation.
  - **Inputs:** color, isPlaying, animate, barCount, barWidth, gap, min/maxHeightFraction, basePeriodMillis.
  - **Outputs:** Composable Canvas animation.
  - **Dependencies:** rememberInfiniteTransition, animateFloatAsState, LocalDensity, sin.
  - **Consumed by:** QueueScreen, LyricsScreen.
  - **Related files:** EqualizerWaveform.kt, AnimatedBackground.kt.
  - **Notes:** Heights squared-wave shaped; Reverse repeat mode; 220ms activity tween.


- `ui/component/compose/CollapsibleAppBar.kt` — Compose Scaffold with collapsible or simple TopAppBar per app bar preference.
  - **Purpose:** Scaffold wrapper honoring app-bar mode preference.
  - **Responsibilities:** Choose MediumTopAppBar exitUntilCollapsed or TopAppBar enterAlways via Preferences.appBarMode; nestedScroll connection, back icon, snackbarHost, mini-player margin insets.
  - **Inputs:** title, actions, snackbarHost, showNavigationButton, collapsibleAppBar, miniPlayerMargin, onBackClick, content.
  - **Outputs:** Scaffold with TopAppBar.
  - **Dependencies:** TopAppBarLayout.AppBarMode, Preferences, Material3 scroll behaviors.
  - **Consumed by:** EqualizerScreen, AboutScreen.
  - **Related files:** Preferences.kt, TopAppBarLayout.kt.
  - **Notes:** Content insets exclude top and add supplied shell clearance at the bottom. Aurora uses a transparent simple bar; retained expanded typography is unchanged.


- `ui/component/compose/ErrorView.kt` — Error view with icon, text and error container colors.
  - **Purpose:** Display error message panel.
  - **Responsibilities:** Clipped errorContainer background, icon, centered text with contentColor.
  - **Inputs:** text, iconRes, textStyle, textSize, color, contentColor, shape, modifier.
  - **Outputs:** Composable Column error panel.
  - **Dependencies:** MaterialTheme error/onErrorContainer, painterResource.
  - **Consumed by:** SongDetailFragment.
  - **Related files:** EmptyView.kt, TipView.kt.
  - **Notes:** Defaults RoundedCornerShape(12dp), 20sp text; iconRes 0 hides nothing.


- `ui/component/compose/EqualizerWaveform.kt` — Canvas drawing the EQ waveform with smoothed line, fill and 0 dB baseline.
  - **Purpose:** Visualize equalizer band gains as waveform.
  - **Responsibilities:** Map bands to points, cubic-spline smoothing, gradient fill, 0dB baseline line, band dots; disabled alpha.
  - **Inputs:** bands, valueRange, enabled, color, fillColor, strokeWidth, smoothingTension.
  - **Outputs:** Composable Canvas.
  - **Dependencies:** MaterialTheme, Brush.verticalGradient, Path cubicTo.
  - **Consumed by:** EqualizerScreen.
  - **Related files:** AnimatedEqBars.kt, IconifiedSlider.kt.
  - **Notes:** Empty bands short-circuit; fill alpha 0.2 default.


- `ui/component/compose/IconifiedSlider.kt` — Material Slider track drawing an icon along active/inactive track.
  - **Purpose:** Slider track embellished with icon segment.
  - **Responsibilities:** Wrap SliderDefaults.Track, drawWithContent icon on active or inactive side, tinted by tick colors, SliderTokens sizes.
  - **Inputs:** state, icon, disabledIcon, colors, thumbTrackGapSize, trackCornerSize, enabled, iconPadding, iconSize.
  - **Outputs:** Composable slider track.
  - **Dependencies:** SliderDefaults.Track, drawWithContent, SliderTokens, ColorFilter.tint.
  - **Consumed by:** SoundSettingsScreen.
  - **Related files:** NowPlayingSlider.kt, Tokens.kt.
  - **Notes:** Icon drawn only if track segment fits minimum space.


- `ui/component/compose/Legacy.kt` — Surface for bottom sheets with rounded top corners.
  - **Purpose:** Shared bottom-sheet surface wrapper.
  - **Responsibilities:** Material3 Surface with surfaceContainer color and RoundedCornerShape top 16dp.
  - **Inputs:** modifier, content.
  - **Outputs:** Composable Surface.
  - **Dependencies:** Material3 Surface, MaterialTheme.
  - **Consumed by:** AddToPlaylistDialog.
  - **Related files:** AddToPlaylistDialog.kt.
  - **Notes:** Legacy bridge for View-based bottom sheet dialog content.


- `ui/component/compose/ObserveAsEvent.kt` — Collects a Flow as an event respecting STARTED lifecycle.
  - **Purpose:** One-shot Flow collection scoped to lifecycle.
  - **Responsibilities:** LaunchedEffect with repeatOnLifecycle(STARTED) collecting flow into callback.
  - **Inputs:** flow, onEvent.
  - **Outputs:** — (side-effect collection).
  - **Dependencies:** LocalLifecycleOwner, repeatOnLifecycle, kotlinx Flow.
  - **Consumed by:** LyricsEditorScreen (saveEvent, downloadEvent, permissionRequestEvent), QueueScreen (stopAfterPosition).
  - **Related files:** LyricsEditorScreen.kt, QueueScreen.kt.
  - **Notes:** GPL-3.0 header (2026).


- `ui/component/compose/MaterialSwitch.kt` — Switch-style toggle row with icon, title and subtitle over primaryContainer.
  - **Purpose:** Prominent toggle row for settings.
  - **Responsibilities:** primaryContainer background, toggleable row, icon, title/subtitle, Switch with check/close thumb icons.
  - **Inputs:** title, isChecked, onClick, icon, subtitle, enabled, modifier.
  - **Outputs:** Composable switch row.
  - **Dependencies:** Material3 Switch, SwitchDefaults, toggleable, MutableInteractionSource.
  - **Consumed by:** EqualizerScreen.
  - **Related files:** TitledSurface.kt (SwitchCard), PreferenceItem.kt.
  - **Notes:** Thumb shows ic_check/ic_close; extraLarge shape clip.


- `ui/component/compose/MediaImage.kt` — Media image with Coil (AsyncImage) and placeholder on load/error.
  - **Purpose:** Coil-backed media cover image.
  - **Responsibilities:** rememberAsyncImagePainter with size resolver, ContentScale.Crop; placeholder on Loading/Error states.
  - **Inputs:** model, placeholderIcon, contentDescription, modifier.
  - **Outputs:** Composable Image or MediaPlaceholder.
  - **Dependencies:** coil3 AsyncImagePainter/ImageRequest, collectAsState.
  - **Consumed by:** SmallHeader, AddToPlaylistDialog, QueueScreen, LyricsEditorScreen.
  - **Related files:** SmallHeader.kt, coil fetchers (AudioCoverFetcher).
  - **Notes:** MediaPlaceholder uses surfaceContainerHighest + icon.


- `ui/component/compose/decoration/FadingEdge.kt` — fadingEdges modifier applying fade gradients on content edges (RTL aware).
  - **Purpose:** Fade content edges for immersive scroll views.
  - **Responsibilities:** drawWithCache gradient rects with BlendMode.DstIn per edge; FadingEdges data class; RTL-aware start/end.
  - **Inputs:** edges or per-edge Dp lengths, gradientStartAlpha, gradientEndAlpha.
  - **Outputs:** Modifier.
  - **Dependencies:** drawWithCache, Brush gradients, LocalLayoutDirection, BlendMode.
  - **Consumed by:** LyricsView, LyricsScreen.
  - **Related files:** LyricsView.kt, AnimatedBackground.kt.
  - **Notes:** graphicsLayer alpha 0.99f; FadingEdges.None no-op.


- `ui/component/compose/decoration/AnimatedBackground.kt` — animatedGradient modifier drawing a looping animated gradient while playing.
  - **Purpose:** Animated ambient background behind lyrics.
  - **Responsibilities:** Animatable time loop via withFrameMillis; three drifting radial-gradient circles over base color; HSL adjustColorForBackground.
  - **Inputs:** colors, animating.
  - **Outputs:** Modifier drawBehind.
  - **Dependencies:** Animatable, animateColorAsState, Brush.radialGradient, sin/cos.
  - **Consumed by:** LyricsScreen.
  - **Related files:** LyricsScreen.kt, FadingEdge.kt.
  - **Notes:** Saturation clamped 0.15-0.45, lightness 0.18-0.28 for dark glow.


- `ui/component/compose/preferences/PreferenceItem.kt` — Compose preference items (icon, title, summary, switch/value variants).
  - **Purpose:** Compose-based preference rows for settings screens.
  - **Responsibilities:** Horizontal/Vertical/Expandable variants; Switch, Slider, ProgressIndicator, plain icon variants; shared surface/content.
  - **Inputs:** title, summary, iconRes, enabled, onClick/onCheckedChanged/onValueChanged, trailing/bottom content slots.
  - **Outputs:** Composable preference rows.
  - **Dependencies:** Material3 Surface, Switch, Slider, CircularProgressIndicator, AnimatedVisibility.
  - **Consumed by:** Settings/preference Compose screens (generic).
  - **Related files:** MaterialSwitch.kt, SliderTokens.
  - **Notes:** Surface surfaceContainer, RoundedCornerShape(4dp); arrow rotates when expanded.


- `ui/component/compose/lyrics/LyricsView.kt` - Full Compose lyrics view: LazyColumn with synced scrolling, active line highlight, blur/shadow effects, RTL, line selection and long-press A-B callback.
  - **Purpose:** Render synced lyrics with playback-driven effects.
  - **Responsibilities:** Auto-scroll/center active line, line scale/alpha, blur & shadow, karaoke/syllable selection, translations/transliterations, actor alignment, provider footer, BubblesLine; consume `inactiveTextAlpha` for inactive lines, the progressive-gradient brush and the provider credit.
  - **Inputs:** LyricsViewState, LyricsViewSettings, contentPadding, fadingEdges, contentColor, isPowerSaveMode, hasBackgroundEffects, inactiveTextAlpha (default `.4f`; Aurora passes `.92f`), onSeekTo.
  - **Outputs:** Composable LazyColumn.
  - **Dependencies:** LyricsViewState/Settings, SyncedLyrics, KaraokeLineView, SpannedWordText, fadingEdges, BlurEffect.
  - **Consumed by:** LyricsScreen.
  - **Related files:** KaraokeStyleText.kt, SpannedWordText.kt, FadingEdge.kt.
  - **Notes:** BubblesLine from Lotus player; power-save disables advanced effects. The inactive alpha participates in the gradient brush `remember` keys, and the provider credit floors at `.5f` (legacy-neutral).


- `ui/component/compose/lyrics/KaraokeStyleText.kt` — Karaoke-style line rendering measuring syllables and animating text fill by playback time.
  - **Purpose:** Per-syllable karaoke fill rendering.
  - **Responsibilities:** TextMeasurer syllable measurement, greedy word wrapping, static line layout, Canvas drawText with gradient brush fill via saveLayer/DstIn.
  - **Inputs:** selectedLine, shadowEffect, currentMillis, syllables, contentColor, inactiveTextAlpha (default `.4f`), style, align.
  - **Outputs:** Composable Canvas.
  - **Dependencies:** rememberTextMeasurer, BoxWithConstraints, Paint, Brush.horizontalGradient.
  - **Consumed by:** LyricsView (WordSyncedText).
  - **Related files:** LyricsView.kt, SpannedWordText.kt.
  - **Notes:** RTL-aware fill direction; shadow on active line (alpha `.5f` unchanged); the inactive alpha drives the DstIn progress brush.


- `ui/component/compose/lyrics/SpannedWordText.kt` — Word-by-word synced lyrics rendering using AnnotatedString (based on Metrolist).
  - **Purpose:** Word-level highlight for synced lyrics.
  - **Responsibilities:** buildAnnotatedString with per-word SpanStyle color alpha and shadow driven by currentMillis.
  - **Inputs:** selectedLine, shadowEffect, currentMillis, syllables, contentColor, style, align.
  - **Outputs:** Material3 Text composable.
  - **Dependencies:** buildAnnotatedString, SpanStyle, Shadow, Material3 Text.
  - **Consumed by:** LyricsView (WordSyncedText fallback).
  - **Related files:** LyricsView.kt, KaraokeStyleText.kt.
  - **Notes:** Smoothstep easing; based on Metrolist.


- `ui/component/compose/color/ColorComponent.kt` — Color utilities: isDark/darken via HCT (m3color), on-surface colors, gradient extraction from Bitmap.
  - **Purpose:** Color helpers for dynamic theming.
  - **Responsibilities:** luminance isDark, HCT darken(maxTone), onThis contrast colors, Bitmap.extractGradientColors via Palette + Score.
  - **Inputs:** Color, Bitmap, fallbackColorArgb.
  - **Outputs:** Color, List<Color>.
  - **Dependencies:** m3color Hct/Score, androidx Palette, onSurfaceLight/Dark.
  - **Consumed by:** PlayerColorScheme (onThis), FullWidget (isDark), LyricsScreen (extractGradientColors).
  - **Related files:** Monet.kt, Theme.kt.
  - **Notes:** Gradient colors darkened to tone 20.


- `ui/component/compose/menu/MenuItem.kt` — Sealed menu item model (Action, DropDown, Checkable, Divider) with visibility/enabled.
  - **Purpose:** Immutable model for Compose menus.
  - **Responsibilities:** Define Action/DropDown/Checkable Button variants and Divider with enabled/visible flags.
  - **Inputs:** text, icon, dangerous, onClick, isChecked, isSingleSelection, enabled, visible.
  - **Outputs:** MenuItem sealed instances.
  - **Dependencies:** Painter, Immutable annotation.
  - **Consumed by:** OverflowMenu, TopAppBarMenu, LyricsEditorScreen, QueueScreen.
  - **Related files:** OverflowMenu.kt, MenuItemClickExt.kt.
  - **Notes:** Checkable toggles via onCheckedChange(!isChecked).


- `ui/component/compose/menu/OverflowMenu.kt` — Compose overflow menu (DropdownMenu) built from MenuItem with radio/checkbox states.
  - **Purpose:** Top-bar and overflow dropdown menus.
  - **Responsibilities:** TopAppBarMenu splits Actions (IconButton/TextButton) vs dropdown; DropdownMenu with dangerous coloring, check/radio trailing, dividers; MenuDefaults colors.
  - **Inputs:** items, showItemIcons, overflowIcon, colors, state, enabled.
  - **Outputs:** Composable menu row/DropdownMenu.
  - **Dependencies:** Material3 DropdownMenu, DropdownMenuItem, Checkbox, RadioButton, HorizontalDivider.
  - **Consumed by:** LyricsEditorScreen, QueueScreen.
  - **Related files:** MenuItem.kt.
  - **Notes:** RoundedCornerShape(16dp) menu; external MutableState controls expansion.


- `ui/component/compose/player/NowPlayingSlider.kt` — Compose player slider with straight/wavy track and thumb styles.
  - **Purpose:** Stylized playback position slider.
  - **Responsibilities:** Slider with selectable TrackStyle (Straight/Wavy) and ThumbStyle (Straight/Ball); wavy uses LinearWavyProgressIndicator with playing amplitude.
  - **Inputs:** sliderState, isPlaying, trackStyle, thumbStyle, colors, enabled.
  - **Outputs:** Composable Slider.
  - **Dependencies:** Material3 Slider, LinearWavyProgressIndicator, SliderDefaults, animateDpAsState.
  - **Consumed by:** Player UI (now-playing styles).
  - **Related files:** IconifiedSlider.kt, player style screens.
  - **Notes:** Drag expands thumb/track sizes; amplitude 0 when paused.


- `ui/dialogs/WebSearchDialog.kt` — Dialog offering web search engines for a song, opening the browser.
  - **Purpose:** Pick search engine for a song.
  - **Responsibilities:** MaterialAlertDialogBuilder items from WebSearchEngine.entries; launches searchQuery().openWeb().toChooser().
  - **Inputs:** Song via EXTRA_SONG.
  - **Outputs:** Intent chooser to browser.
  - **Dependencies:** WebSearchEngine, Song.searchQuery/openWeb extensions, BundleCompat.
  - **Consumed by:** AbsPlayerFragment.
  - **Related files:** extensions/media/searchQuery, MenuItemClickExt.kt.
  - **Notes:** DialogFragment; create(song) factory.


- `ui/dialogs/SAFDialog.kt` — Dialog requesting SAF access via OpenDocumentTree and notifying the persisted Uri.
  - **Purpose:** Request Storage Access Framework permission.
  - **Responsibilities:** Register OpenDocumentTree launcher, persist tree URI via saveTreeUri, notify SAFResultListener.
  - **Inputs:** SAFResultListener via parent fragment or activity.
  - **Outputs:** Uri? callback onSAFResult.
  - **Dependencies:** ActivityResultContracts.OpenDocumentTree, MaterialAlertDialogBuilder, saveTreeUri extension.
  - **Consumed by:** DeleteSongsDialog.
  - **Related files:** DeleteSongsDialog.kt, files extensions.
  - **Notes:** show() constrained to AppCompatActivity/Fragment implementing SAFResultListener.


- `ui/dialogs/MultiCheckDialog.kt` — Generic multi-select dialog with RecyclerView checkboxes (MultiCheckConfig).
  - **Purpose:** Generic multi-select list dialog.
  - **Responsibilities:** ViewBinding dialog with SimpleItemAdapter + Checkbox toggle, callback-based positive button, MultiCheckConfig/Builder.
  - **Inputs:** MultiCheckConfig (title, positiveText, message, items), MultiCheckCallback2.
  - **Outputs:** Callback with checked positions/items.
  - **Dependencies:** MaterialAlertDialogBuilder, SimpleItemAdapter, DialogMultiCheckBinding.
  - **Consumed by:** PreferencesScreenFragment.
  - **Related files:** SimpleItemAdapter.kt, PreferencesScreenFragment.kt.
  - **Notes:** Open for subclassing; processMultiCheck overridable.


- `ui/dialogs/songs/DeleteSongsDialog.kt` — Deletes songs from the library using MediaStore createDeleteRequest/TrashRequest (with SAF fallback).
  - **Purpose:** Delete songs from device storage.
  - **Responsibilities:** Android R+ MediaStore delete/trash requests via IntentSender; pre-R manual deletion with progress via MusicUtil.deleteTracks; SAF fallback.
  - **Inputs:** List<Song> via EXTRA_SONGS.
  - **Outputs:** LibraryViewModel.deleteSongs, toast results.
  - **Dependencies:** MediaStore, MusicUtil, LibraryViewModel, SAFDialog, Preferences.trashMusicFiles.
  - **Consumed by:** AbsPlayerFragment, QueueFragment, MenuItemClickExt.
  - **Related files:** SAFDialog.kt, MusicUtil.kt, LibraryViewModel.kt.
  - **Notes:** Implements SAFDialog.SAFResultListener; disables buttons during deletion.


- `ui/dialogs/playlists/RemoveFromPlaylistDialog.kt` — Confirmation dialog to remove one or more songs from a playlist.
  - **Purpose:** Confirm removal from playlist.
  - **Responsibilities:** Title/message for single or multiple songs; calls LibraryViewModel.deleteSongsInPlaylist.
  - **Inputs:** List<SongEntity> via EXTRA_SONGS.
  - **Outputs:** ViewModel deletion.
  - **Dependencies:** MaterialAlertDialogBuilder, LibraryViewModel, toHtml extension.
  - **Consumed by:** PlaylistDetailFragment.
  - **Related files:** PlaylistDetailFragment.kt, LibraryViewModel.kt.
  - **Notes:** —.


- `ui/dialogs/playlists/ImportPlaylistDialog.kt` — Lists device playlists (M3U/others) for import with multi-selection and progress.
  - **Purpose:** Import device playlists.
  - **Responsibilities:** Observe getDevicePlaylists, RecyclerView adapter, per-playlist song multi-choice, pass only the checked/deduplicated subset to the ViewModel, show conflict dialog on exact collision, observe result toasts, progress indicator.
  - **Inputs:** —.
  - **Outputs:** Import result toasts, conflict dialog launching.
  - **Dependencies:** LibraryViewModel (getDevicePlaylists, startPlaylistImport, conflict/result LiveData), PlaylistImportConflictDialogFragment, ImportablePlaylistResult, DialogProgressRecyclerViewBinding.
  - **Consumed by:** Playlist screens (import action).
  - **Related files:** LibraryViewModel.kt, ImportablePlaylistResult, PlaylistImportConflictDialog.kt.
  - **Notes:** Dismisses with toast if no importable playlists. The selected-subset bug is fixed here; cancel clears pending conflict state without mutation.


- `ui/dialogs/playlists/PlaylistImportConflictDialog.kt` — Compose conflict resolver hosted by ImportPlaylistDialog.
  - **Purpose:** Let the user Keep both, Replace the single exact non-Favorites playlist, or Rename the incoming playlist.
  - **Responsibilities:** Observe conflict StateFlow; Keep both default/disabled on suffix exhaustion; Replace disabled for Favorites/ambiguous matches; inline blank/duplicate rename validation; disable controls and block back while applying; cancel/back clears state without mutation.
  - **Inputs:** PlaylistImportConflictState, LibraryViewModel actions.
  - **Outputs:** ViewModel apply/cancel calls; no database access of its own.
  - **Dependencies:** Compose Material 3, activityViewModel, SsilencceTheme.
  - **Consumed by:** ImportPlaylistDialog.
  - **Related files:** PlaylistImportConflictDialog.kt, LibraryViewModel.kt, LibraryResult.kt.
  - **Notes:** Compose-only; no XML layout. Keep both remains the default; Replace is never preselected.


- `ui/dialogs/playlists/EditPlaylistDialog.kt` — Edits an existing playlist (name, description, image with Photo Picker).
  - **Purpose:** Edit playlist metadata.
  - **Responsibilities:** Pre-fill name/description/custom cover; PickVisualMedia image selection with Coil load; save via updatePlaylist + forceReload.
  - **Inputs:** PlaylistEntity via EXTRA_PLAYLIST.
  - **Outputs:** LibraryViewModel.updatePlaylist(ReloadType.Playlists).
  - **Dependencies:** ActivityResultContracts.PickVisualMedia, coil3.load, DialogCreatePlaylistBinding, LibraryViewModel.
  - **Consumed by:** MenuItemClickExt.
  - **Related files:** CreatePlaylistDialog.kt, MenuItemClickExt.kt.
  - **Notes:** Favorites playlist name field disabled; validates non-empty name.


- `ui/dialogs/playlists/DeletePlaylistDialog.kt` - Confirmation dialog to delete one or more playlists and remove their dynamic shortcuts.
  - **Purpose:** Confirm playlist deletion.
  - **Responsibilities:** Singular/plural title and message; removes each playlist's entity shortcut; calls LibraryViewModel.deletePlaylists.
  - **Inputs:** List<PlaylistEntity> via EXTRA_PLAYLISTS.
  - **Outputs:** ViewModel deletion; ShortcutHelper removal.
  - **Dependencies:** MaterialAlertDialogBuilder, LibraryViewModel, toHtml, ShortcutHelper, ShortcutEntityType.
  - **Consumed by:** MenuItemClickExt.
  - **Related files:** MenuItemClickExt.kt, PlaylistDetailFragment.kt, util/ShortcutHelper.kt.
  - **Notes:** create() accepts PlaylistWithSongs and maps entities; removal runs for single and multi-selection confirmation (ADR-044).


- `ui/dialogs/playlists/CreatePlaylistDialog.kt` — Creates a new playlist with name, description, cover and optional initial songs.
  - **Purpose:** Create custom playlists.
  - **Responsibilities:** Name/description fields, PickVisualMedia cover, createCustomPlaylist observation, PlaylistCreatedCallback.
  - **Inputs:** List<Song> via EXTRA_SONGS (optional), callback.
  - **Outputs:** LibraryViewModel.createCustomPlaylist result, toasts.
  - **Dependencies:** ActivityResultContracts.PickVisualMedia, coil3.load, DialogCreatePlaylistBinding, LibraryViewModel.
  - **Consumed by:** AddToPlaylistDialog, MenuItemClickExt, QueueFragment.
  - **Related files:** AddToPlaylistDialog.kt, EditPlaylistDialog.kt.
  - **Notes:** Shows inline error for blank name; duplicate playlist toast.


- `ui/dialogs/playlists/AddToPlaylistDialog.kt` — Compose bottom sheet with search and multi-select of playlists for adding songs.
  - **Purpose:** Add songs to one or more playlists.
  - **Responsibilities:** BottomSheetDialog with ComposeView; AddToPlaylistUiState machine (Loading/Empty/Ready/Completed); search TextField, SegmentedListItem selection, floating add card; nested CreatePlaylistDialog.
  - **Inputs:** List<Song> via EXTRA_SONGS.
  - **Outputs:** LibraryViewModel.prepareToAddToPlaylist/addToPlaylists/finishAddingToPlaylists.
  - **Dependencies:** LibraryViewModel, BottomSheetDialog, SsilencceTheme, EmptyView, MediaImage, BottomSheetDialogSurface.
  - **Consumed by:** AbsPlayerFragment, QueueFragment, MenuItemClickExt.
  - **Related files:** CreatePlaylistDialog.kt, Legacy.kt, EmptyView.kt.
  - **Notes:** Expanded bottom sheet; dismisses child create dialog on close.


- `ui/dialogs/library/FolderChooserDialog.kt` — Navigable folder tree dialog for selecting a storage folder.
  - **Purpose:** Pick a storage folder.
  - **Responsibilities:** Permission check, folder navigation (.. up), storage volumes listing via StorageUtil, SimpleItemAdapter, FolderCallback.
  - **Inputs:** FolderCallback; state saved in bundle.
  - **Outputs:** onFolderSelection(dialog, File).
  - **Dependencies:** StorageUtil, SimpleItemAdapter, sortedByName, MaterialAlertDialogBuilder.
  - **Consumed by:** BlacklistWhitelistDialog.
  - **Related files:** BlacklistWhitelistDialog.kt, StorageUtil.kt.
  - **Notes:** Checks READ_MEDIA_AUDIO/READ_EXTERNAL_STORAGE; emulated root shows storage volumes.


- `ui/dialogs/library/BlacklistWhitelistDialog.kt` — Manages blacklisted/whitelisted library folders via InclExclDao and FolderChooserDialog.
  - **Purpose:** Configure included/excluded library folders.
  - **Responsibilities:** List paths with add-first row; FolderChooserDialog callback inserts path; per-item remove confirm; clear-all confirm; InclExclDao CRUD on IO.
  - **Inputs:** listType (BLACKLIST/WHITELIST) via EXTRA_TYPE.
  - **Outputs:** InclExclDao insertPath/deletePath/clearPaths, refreshed adapter.
  - **Dependencies:** InclExclDao, InclExclEntity, FolderChooserDialog, SimpleItemAdapter.
  - **Consumed by:** PreferencesScreenFragment.
  - **Related files:** FolderChooserDialog.kt, InclExclDao.kt.
  - **Notes:** Koin injects DAO; canonical paths stored.

- `ui/theme/Theme.kt` — SsilencceTheme (light/dark/black/dynamic schemes) and PlayerTheme (player color scheme).
  - **Purpose:** Root theme composition.
  - **Responsibilities:** Build colorScheme from dark/black/dynamic preferences; dynamicDark/LightColorScheme on S+; black overrides; conditional Aurora scheme/content color/system typography; PlayerTheme copies player colors.
  - **Inputs:** darkTheme, blackTheme, dynamicColor, playerColorScheme, content.
  - **Outputs:** MaterialTheme wrappers.
  - **Dependencies:** Preferences, PlayerColorScheme, dynamicColorScheme APIs, customTypography.
  - **Consumed by:** App entry screens, AddToPlaylistDialog, LyricsScreen (PlayerTheme).
  - **Related files:** Color.kt, Type.kt, Monet.kt.
  - **Notes:** Black theme uses surfaceContainerBlack resources. Non-Aurora Compose typography remains unchanged; system typography is selected only for Aurora with the custom-font switch off.


- `ui/theme/AuroraDesign.kt` — Monochrome reference tokens and Compose color scheme.
  - **Purpose:** Shared black/white/glass design vocabulary for Aurora.
  - **Responsibilities:** Define foreground, secondary/muted text, glass/border colors and matching Material 3 roles.
  - **Inputs:** None.
  - **Outputs:** Color tokens and darkColorScheme.
  - **Dependencies:** Compose Color, Material 3.
  - **Consumed by:** Theme, Aurora components/player, queue and supporting surfaces.
  - **Related files:** `res/values/aurora.xml`, `ui/theme/AuroraLegacyStyling.kt`.
  - **Notes:** Alternate palettes are retained; no navy brand palette is substituted (ADR-036).


- `ui/theme/AuroraLegacyStyling.kt` — Conditional reference styling for retained View-based screens and rows.
  - **Purpose:** Keep existing adapters/actions while sharing Aurora's visual vocabulary.
  - **Responsibilities:** Transparent toolbars, glass cards/search field, row typography/image corners/menu targets, reusable glass drawable, and delegation of the reference header scroll treatment to `AuroraHeaderScroll`.
  - **Inputs:** Existing View hierarchy or media row; selected appearance.
  - **Outputs:** In-place styling without replacing data, menus or selection listeners.
  - **Dependencies:** Material View components, Preferences.
  - **Consumed by:** AbsMainActivityFragment, MediaEntryViewHolder, preferences, MiniPlayerFragment.
  - **Related files:** `ui/theme/AuroraDesign.kt`, `ui/theme/AuroraHeaderScroll.kt`, `res/values/aurora.xml`.
  - **Notes:** Returns immediately outside Aurora; per-row padding reserves 48 dp for menu controls.


- `ui/theme/AuroraHeaderScroll.kt` — Reference scroll treatment for the Aurora toolbar.
  - **Purpose:** Keep the hamburger/search/overflow actions visible and readable while the page scrolls, matching the golden header behaviour.
  - **Responsibilities:** Pin simple app bars, fade/translate/blur the title with progress `(scrollTop - 6dp) / 28dp`, switch the action icons to floating glass pills (rounded, `#C716161A` with a `#17FFFFFF` stroke plus ripple and a soft elevation), collapse the simple header space through the scrolling-view overlay, and snap states when system animations are disabled. Collapsing-header screens (`R.id.app_bar` with `CollapsingToolbarLayout`) keep their scroll flags and pinned toolbar and receive the same pill treatment.
  - **Inputs:** Fragment root view containing `R.id.appBarLayout` and `R.id.recyclerView` or `R.id.container`.
  - **Outputs:** In-place header/action styling driven by the scroll position; no data or menu changes.
  - **Dependencies:** `TopAppBarLayout`, `AppBarLayout.ScrollingViewBehavior`, Preferences, RenderEffect (API 31+).
  - **Consumed by:** Aurora library screens through `styleAuroraHierarchy`.
  - **Related files:** `ui/theme/AuroraLegacyStyling.kt`, `ui/component/views/TopAppBarLayout.kt`.
  - **Notes:** Reduced motion replaces interpolation with a binary snap (observed once and on change, not per scroll event); blur is skipped below API 31 and throttled to 0.5 px changes; styling is idempotent per scroll view through the `aurora_header_scroll_styled` tag; collapsing-header screens keep their own collapse behaviour and are covered by the pill-only path; the floating pill uses a static translucent fill because View content has no backdrop blur. Target semantics and 48 dp touch targets are preserved.


- `ui/component/compose/aurora/AuroraRibbon.kt` — Faithful native reproduction of the reference aurora background and the shared Aurora motion gates.
  - **Purpose:** Reproduce the reference's layered white S, veil/glow field and dark overlay without runtime foreground blur.
  - **Responsibilities:** Pre-render two per-layer rasters on Default from the reference stroke profiles (main five strokes w 240/180/96/32/3.2, SD 118/88/64/36/18; back three strokes w 190/110/28, SD 110/76/40 with .32 layer opacity) using the CTM-aware BlurMaskFilter conversion; cache paints, elliptical glow gradients (static veil, horizon, top), the eight-stop scrim and the mesh vertex buffer; warp each layer through its own overscan mapping (-8%/-16% main, -10%/-20% back) with the traveling wave (A=40, K=.0105, .55 rad/s, back phase pi) plus the approximated CSS wiggle; observe lifecycle, animation scale, battery saver and the motion preference.
  - **Inputs:** Visibility, modifier, motion preference and platform signals.
  - **Outputs:** Static reduced-motion shape or approximately 30 Hz warped ribbon; motion scale for native player animations.
  - **Dependencies:** Canvas, Android bitmap/blur mask/mesh/radial gradients, coroutines, Lifecycle, ContentObserver, PowerManager, SharedPreferences.
  - **Consumed by:** AbsSlidingMusicPanelActivity, AbsTagEditorActivity, AuroraPlayerScreen, QueueScreen, LyricsScreen.
  - **Related files:** `util/AuroraPreferences.kt`, `ui/theme/AuroraDesign.kt`, reference `ssilencce-auroras.html`.
  - **Notes:** Two ~0.77 MB bounded rasters replace the previous single shared bitmap; paints, gradients and vertices are created off-frame and no per-frame allocations occur. Updates stop when obscured, paused, disabled or power-saving; animation scale zero paints the reference's un-waved shape. Glow blurs are approximated by the gradients' soft ramps and the wiggle phase origin differs cosmetically from the CSS keyframes; emulator frame timing is not evidence of physical-device performance.


- `ui/component/compose/aurora/AuroraNavigation.kt` — Six-destination shell, grouped library drawer and unavailable rows.
  - **Purpose:** Native reference navigation using actual Navigation destinations.
  - **Responsibilities:** Primary/More navigation, accessible selected/disabled states, scrim-dismissable library drawer with the reference groups (Library / Discover / Sound & system), the app-mark header, real category/queue/search/EQ/settings destinations, most-played and recently-added entries, and capability reservations.
  - **Inputs:** Destination, player/More availability, CategoryInfo list and navigation/search/top-tracks/last-added callbacks.
  - **Outputs:** Real destination/search callbacks or explicitly unavailable explanatory rows.
  - **Dependencies:** Compose Foundation/Material 3, CategoryInfo, AuroraDesign, main string/icon resources.
  - **Consumed by:** AbsSlidingMusicPanelActivity, AbsMainActivityFragment, queue/sound/timer screens.
  - **Related files:** `ui/screen/MainActivity.kt`, `ui/theme/AuroraDesign.kt`, `res/drawable/app_logo_splash.xml`.
  - **Notes:** The drawer header draws the app mark with the existing vector `app_logo_splash` in a 46 dp rounded black box (no raster/mipmap). The former category strip is retired app-wide per ADR-037; its contrast token `AuroraInactiveTabColor` is kept unused with an explanatory comment and `AuroraDesign.muted` is unchanged.


- `ui/component/compose/aurora/AuroraHomeActions.kt` — Reference home action rows with real callbacks and library count.
  - **Purpose:** Native 2x2 compact action rows entering existing library actions.
  - **Responsibilities:** Render top-tracks, last-added, history and shuffle rows with glass styling, 38 dp icon boxes, an optional song-count subtitle, a 66 dp minimum height and per-row equal heights so the subtitle tile matches its peer.
  - **Inputs:** Real song-count text and four callbacks.
  - **Outputs:** Existing HomeFragment actions.
  - **Dependencies:** Compose Foundation/Material 3, AuroraDesign, main string/icon resources.
  - **Consumed by:** HomeFragment.
  - **Related files:** `ui/adapters/HomeAdapter.kt`, `ui/screen/library/home/HomeFragment.kt`.
  - **Notes:** No prototype song counts or library fixtures are used at runtime; the shuffle subtitle shows the real library count. Each row stretches to its intrinsic height (`IntrinsicSize.Min` + `fillMaxHeight`) like the reference's CSS grid, so no tile grows beyond its peer.


- `ui/theme/Color.kt` — Static Material 3 color palettes (lightScheme/darkScheme) for the default theme.
  - **Purpose:** Default static M3 color values.
  - **Responsibilities:** Declare ~40 light and dark Color constants (primary, surface containers, error, etc.).
  - **Inputs:** —.
  - **Outputs:** Color constants.
  - **Dependencies:** androidx.compose.ui.graphics.Color.
  - **Consumed by:** Theme.kt, ColorComponent.kt (onSurfaceLight/Dark).
  - **Related files:** Theme.kt, Monet.kt.
  - **Notes:** Primary hue ~indigo (0xFF4D5C92).


- `ui/theme/Type.kt` — Typography: Google Sans Flex font family and customTypography applied to all styles.
  - **Purpose:** App-wide typography.
  - **Responsibilities:** GoogleSansFlex FontFamily from 7 weights; customTypography reusing default styles with that family.
  - **Inputs:** —.
  - **Outputs:** FontFamily, Typography.
  - **Dependencies:** Material3 Typography, Font resources R.font.googlesansflex_*.
  - **Consumed by:** Theme.kt (SsilencceTheme, PlayerTheme).
  - **Related files:** Theme.kt.
  - **Notes:** Covers display/headline/title/body/label roles.


- `ui/theme/Tokens.kt` — Dimension tokens for sliders (corner size, gaps, track heights).
  - **Purpose:** Shared slider dimension tokens.
  - **Responsibilities:** Define SliderTokens: TrackCornerSize 12dp, ThumbTrackGapSize 6dp, Medium/Large track heights.
  - **Inputs:** —.
  - **Outputs:** SliderTokens constants.
  - **Dependencies:** dp.
  - **Consumed by:** IconifiedSlider.kt, SoundSettingsScreen, SleepTimerScreen.
  - **Related files:** IconifiedSlider.kt, NowPlayingSlider.kt.
  - **Notes:** Track heights 32dp/40dp.


- `ui/theme/Monet.kt` — Dynamic (Monet/M3) color scheme generation from a key color with palette styles and system contrast.
  - **Purpose:** Generate dynamic M3 schemes from key color.
  - **Responsibilities:** dynamicColorSchemes/Scheme building via m3color Scheme* classes; PaletteStyle enum; system contrast via UiModeManager.
  - **Inputs:** keyColor, style, contrastLevel, isDark.
  - **Outputs:** ColorScheme, ColorSchemes.
  - **Dependencies:** m3color (Hct, SchemeTonalSpot/Neutral/Vibrant/Expressive/Rainbow/FruitSalad/Monochrome/Fidelity/Content).
  - **Consumed by:** WidgetExtension (dynamicColorSchemes).
  - **Related files:** Theme.kt, Color.kt.
  - **Notes:** getSystemContrast requires API 34.


- `ui/adapters/pager/CustomFragmentStatePagerAdapter.java` — (NEW) AOSP-derived FragmentStatePagerAdapter managing fragment lifecycle states (STARTED/RESUMED) with saved-state restore.
  - **Purpose:** Replace framework FragmentStatePagerAdapter with explicit maxLifecycle control.
  - **Responsibilities:** instantiateItem/destroyItem with FragmentTransaction, save/restore Fragment.SavedState, setMenuVisibility, getFragment accessor.
  - **Inputs:** FragmentManager; subclasses implement getItem(position).
  - **Outputs:** Lifecycle-managed Fragment pages in a ViewPager.
  - **Dependencies:** androidx.viewpager.widget.PagerAdapter, FragmentManager, FragmentTransaction, Lifecycle.
  - **Consumed by:** CoverPagerFragment.
  - **Related files:** `ui/screen/player/cover/CoverPagerFragment.kt`.
  - **Notes:** Handles "Fragment is no longer active" restore errors gracefully; commits with allowStateLoss.


- `ui/component/views/AlbumCoverViewPager.java` — (NEW) ViewPager gating swiping on the album cover by the swipe-on-cover preference.
  - **Purpose:** Allow/disallow horizontal swiping of the now-playing cover pager.
  - **Responsibilities:** Read Preferences.getSwipeOnCover, setAllowSwiping, consume touch events when disabled.
  - **Inputs:** AttributeSet; Preferences swipe-on-cover setting.
  - **Outputs:** Blocked or forwarded touch events.
  - **Dependencies:** androidx.viewpager.widget.ViewPager, Preferences.
  - **Consumed by:** fragment_player_album_cover layout inside CoverPagerFragment.
  - **Related files:** `res/layout/fragment_player_album_cover.xml`, `ui/screen/player/cover/CoverPagerFragment.kt`.
  - **Notes:** Returns true for all touches when swiping disabled.


- `ui/component/views/BaselineGridTextView.java` — (NEW) MaterialTextView aligning text baselines and heights to the 4dp grid with optional maxLinesByHeight.
  - **Purpose:** Grid-aligned text for detail lists, avoiding mid-line clipping.
  - **Responsibilities:** Compute 4dp-multiple line heights, baseline/top and bottom padding alignment, maxLines from exact height, line-height hints.
  - **Inputs:** TextAppearance + view attributes (lineHeightHint, lineHeightMultiplierHint, maxLinesByHeight, fontFamily).
  - **Outputs:** Measured height aligned to 4dp grid; constrained maxLines.
  - **Dependencies:** MaterialTextView, Paint.FontMetrics, TypedValue.
  - **Consumed by:** fragment_detail_list, list_item_view, list_item_view_bottom_widget, list_item_view_with_button, permission_view layouts.
  - **Related files:** `res/layout/fragment_detail_list.xml`, `res/values/attrs.xml`.
  - **Notes:** Port of Google's BaselineGridTextView; FOUR_DIP constant.


- `ui/component/views/WidthFitSquareCardView.java` — (NEW) MaterialCardView forcing a square measure by using its width for height.
  - **Purpose:** Square cover cards in tag-editor image picker.
  - **Responsibilities:** Override onMeasure to pass width spec for height.
  - **Inputs:** Width measure spec.
  - **Outputs:** Square-measured MaterialCardView.
  - **Dependencies:** MaterialCardView.
  - **Consumed by:** activity_tag_editor layout (cover preview).
  - **Related files:** `res/layout/activity_tag_editor.xml`, `ui/component/views/WidthFitSquareLayout.java`.
  - **Notes:** Height always equals width regardless of height spec.


- `ui/component/views/WidthFitSquareLayout.java` — (NEW) FrameLayout forcing square measure (width-based) with optional forceSquare toggle.
  - **Purpose:** Square containers for grid/card item images.
  - **Responsibilities:** Override onMeasure using width spec for height; forceSquare toggle with requestLayout.
  - **Inputs:** Width measure spec; forceSquare flag.
  - **Outputs:** Square-measured FrameLayout.
  - **Dependencies:** FrameLayout.
  - **Consumed by:** item_card, item_album, item_grid, item_grid_circle, item_playlist, item_image_gradient layouts.
  - **Related files:** `res/layout/item_grid.xml`, `res/layout/item_card.xml`, `ui/adapters/album/AlbumAdapter.kt`.
  - **Notes:** When forceSquare false, height spec passes through unchanged.


### ui/screen/

- `ui/screen/MainActivity.kt` — Main activity extending AbsSlidingMusicPanelActivity; NavHost, playback intents, MediaController commands, dynamic/entity shortcuts, audio-focus ask dialog, update search.
  - **Purpose:** App entry activity hosting navigation, playback intents, MediaController custom events.
  - **Responsibilities:** NavHost graph setup, last-page start destination, custom command handling, playback shortcuts, entity-shortcut intent resolution/queue opening, audio-focus ask dialog, bottom-nav visibility, update search, scan dialog.
  - **Inputs:** Intents/actions (shortcuts, media URIs), MediaController custom commands, Preferences.
  - **Outputs:** PlayerViewModel commands (playMediaId, openQueue, restorePlayback, confirm/cancel audio focus), UpdateDialog, toasts.
  - **Dependencies:** AbsSlidingMusicPanelActivity, PlayerViewModel, LibraryViewModel, UpdateViewModel, MediaController.Listener, Playback, MediaIDs, ShortcutHelper.
  - **Consumed by:** Launcher activity (AndroidManifest); AbsRecyclerViewFragment calls scanAllPaths().
  - **Related files:** navigation/graph_main.xml, UpdateDialog, IScrollHelper, util/ShortcutHelper.kt.
  - **Notes:** Aurora start/last-page resolution uses `AuroraStartDestinationPolicy` to exclude `nav_queues`; classic keeps the Queues category and its restore behavior. Custom commands relay MediaEvent to PlayerViewModel; implements MediaController.Listener. `com.ssilencce.shortcut.ENTITY` resolves album/artist/folder/playlist targets and reports unavailable ones; a newer entity shortcut cancels the previous lookup job so only the last intent can publish. Focus-state queries on connection/resume reconcile a stable-tag AudioFocusAskDialog; stale query responses cannot replace a newer focus event (ADR-042, ADR-044, ADR-045). Bottom-nav visibility recognizes the AlbumArtists/Composers destinations; the nav graph adds their list fragments and composer detail argument.


- `ui/screen/about/AboutFragment.kt` — Fragment inflating a ComposeView with AboutScreen, handling back and transitions.
  - **Purpose:** Host AboutScreen in a legacy Fragment container.
  - **Responsibilities:** Create ComposeView, apply SsilencceTheme, wire back navigation, shared-axis transition.
  - **Inputs:** None (no args).
  - **Outputs:** Rendered AboutScreen UI.
  - **Dependencies:** AboutScreen, SsilencceTheme, materialSharedAxis extension, getOnBackPressedDispatcher.
  - **Consumed by:** Settings navigation graph (`nav_about` from PreferencesScreenFragment).
  - **Related files:** AboutScreen.kt, SettingsFragment.kt, MainActivity.kt.
  - **Notes:** Plain Fragment; theme applied manually.


- `ui/screen/about/AboutScreen.kt` — Compose "About" screen: app info, contributors table, licenses with dialog, more contributors.
  - **Purpose:** Display app info, contributors, licenses, support links.
  - **Responsibilities:** Version from PackageManager, Contributor enum list, translators JSON via Contribution.loadContributions, licenses via produceLibraries/AboutLibraries LibrariesContainer, openUrl actions, share invitation.
  - **Inputs:** onBackClick callback, BuildConfig, Constants links.
  - **Outputs:** About UI with ModalBottomSheets for translators/licenses.
  - **Dependencies:** AboutLibraries (mikepenz), Coil AsyncImage, MarkdownText, CollapsibleAppBarScaffold, Constants, App.isPlayStoreBuild.
  - **Consumed by:** AboutFragment.
  - **Related files:** AboutFragment.kt, core/model/about/AboutItemData.kt, Contribution.kt.
  - **Notes:** CI build badge shown when BuildConfig.IS_CI_BUILD; avatar images from assets.


- `ui/screen/equalizer/EqualizerFragment.kt` — Compose fragment orchestrating EqualizerViewModel/LibraryViewModel/PlayerViewModel for EqualizerScreen.
  - **Purpose:** Host the full equalizer UI screen.
  - **Responsibilities:** Koin ViewModels wiring, nav args fromPlayer, navigateUp, expand mini-panel on exit.
  - **Inputs:** EqualizerFragmentArgs (fromPlayer), LibraryViewModel, PlayerViewModel.
  - **Outputs:** EqualizerScreen composition; panel expansion side effect.
  - **Dependencies:** EqualizerViewModel, LibraryViewModel, PlayerViewModel, MainActivity.expandPanel.
  - **Consumed by:** Navigation graph from player/settings destinations.
  - **Related files:** EqualizerScreen.kt, MainActivity.kt, library/LibraryViewModel.kt, player/PlayerViewModel.kt.
  - **Notes:** activityViewModel shared instances.


- `ui/screen/equalizer/EqualizerResult.kt` — EQ profile operation results (deletion, export/import with Uri/mime, requests).
  - **Purpose:** Define result/request event types for EQ profile operations.
  - **Responsibilities:** ProfileOpResult base with success/message/canDismiss; deletion, export, import, AutoEq variants.
  - **Inputs:** None.
  - **Outputs:** Event objects consumed via Channels in EqualizerViewModel.
  - **Dependencies:** EqProfile, AutoEqProfile, Uri, StringRes.
  - **Consumed by:** EqualizerViewModel, EqualizerScreen.
  - **Related files:** EqualizerViewModel.kt, EqualizerScreen.kt.
  - **Notes:** ProfileDeletionResult carries profileName and autoEqProfile flag.


- `ui/screen/equalizer/EqualizerScreen.kt` — Full EQ UI: band sliders, bass boost, virtualizer, compressor, limiter, balance, tempo, replay gain, profile import/export.
  - **Purpose:** Render complete equalizer configuration screen.
  - **Responsibilities:** Collect EqualizerViewModel flows, band waveform, sliders, dialogs (save/edit/delete/import/export/share), system EQ launcher, share via ShareCompat, snackbars; Aurora output-device header, real band-count/profile chips, accessible vertical bands and Bands/Sound pages. Profile saver/editor pass the current output and show the exact-device row.
  - **Inputs:** LibraryViewModel, EqualizerViewModel, onBackClick; file picker Uris; current AudioDevice flow.
  - **Outputs:** UI state changes; profile CRUD (type + device-key associations) and export events.
  - **Dependencies:** EqualizerViewModel, EqualizerWaveform, TopAppBarMenu, ButtonGroup, MediaStoreWriter via VM, EqEngineMode, AudioDevice.
  - **Consumed by:** EqualizerFragment.
  - **Related files:** EqualizerFragment.kt, EqualizerViewModel.kt, EqualizerResult.kt, ui/component/compose/EqualizerWaveform.kt.
  - **Notes:** Band count selector; AutoEq TXT import; pro-mode gates compressor/limiter. Aurora uses actual engine bands and the same commit/profile/effect commands; other appearances retain the original anatomy. “Use with this device” appears only when the current output has an exact key; type rows remain.


- `ui/screen/equalizer/EqualizerViewModel.kt` — Exposes EqualizerManager state, implements profile import/export (JSON/TXT AutoEq), launches system AudioEffect.
  - **Purpose:** Bridge EqualizerManager and AudioOutputObserver state to UI.
  - **Responsibilities:** Expose eqState/bandCapabilities/profiles flows, profile save/edit/delete/import/export including type/device-key associations, AutoEqTxtParser import, system equalizer intent, MediaStoreWriter export.
  - **Inputs:** ContentResolver, EqualizerManager, AudioOutputObserver, MediaStoreWriter.
  - **Outputs:** State flows and Channel events (export/import/save/delete).
  - **Dependencies:** EqualizerManager, AudioOutputObserver, AutoEqTxtParser, MediaStoreWriter, kotlinx.serialization Json, AudioEffect.
  - **Consumed by:** EqualizerScreen, SoundSettingsFragment, SoundSettingsScreen.
  - **Related files:** EqualizerScreen.kt, EqualizerResult.kt, playback/equalizer/EqualizerManager.kt, core/audio/AudioOutputObserver.kt.
  - **Notes:** Share writes JSON to externalCacheDir; volume/tempo/balance/replayGain also exposed. Save/edit forward exact device keys from the current-output row.


- `ui/screen/error/ErrorActivity.kt` — Crash-handler activity saving the report to a .log file, with copy/share/report/restart actions.
  - **Purpose:** Show uncaught crash reports to the user.
  - **Responsibilities:** Read CustomActivityOnCrash config, write Crash_<time>.log, display report, GitHub issue, share file via FileProvider, restart app.
  - **Inputs:** Intent from CustomActivityOnCrash, crash report string.
  - **Outputs:** Log file in filesDir, share/clipboard intents.
  - **Dependencies:** CustomActivityOnCrash, AbsThemeActivity, FileProvider, ActivityErrorBinding.
  - **Consumed by:** App.kt crash handler (CustomActivityOnCrash errorActivity).
  - **Related files:** App.kt, Constants.ISSUE_TRACKER_LINK.
  - **Notes:** Open GitHub copies report to clipboard first.


- `ui/screen/info/InfoResult.kt` — State data classes: PlayInfoResult (play counts), AudioHeaderInfo (format/bitrate), SongInfo (metadata, isMissingMetadata).
  - **Purpose:** Define data holders for song/playback info screens.
  - **Responsibilities:** PlayInfoResult totals, AudioHeaderInfo codec details, SongInfo all metadata fields with isMissingMetadata.
  - **Inputs:** PlayCountEntity, metadata strings.
  - **Outputs:** Immutable state consumed by InfoViewModel.
  - **Dependencies:** PlayCountEntity.
  - **Consumed by:** InfoViewModel, SongDetailFragment, PlayInfoFragment.
  - **Related files:** InfoViewModel.kt, SongDetailFragment.kt.
  - **Notes:** SongInfo.Empty companion default.


- `ui/screen/info/InfoViewModel.kt` — Loads album/artist, computes PlayInfoResult from PlayCountEntity and builds SongInfo via MetadataReader + Jaudiotagger.
  - **Purpose:** Provide play statistics and rich song metadata.
  - **Responsibilities:** loadAlbum/loadArtist via Repository, playInfo aggregation, refreshSongInfo building SongInfo through MetadataReader/AudioHeader.
  - **Inputs:** Repository, Song, Context, song id/name.
  - **Outputs:** LiveData<Album/Artist/PlayInfoResult>, SongInfoUiState StateFlow.
  - **Dependencies:** Repository, MetadataReader, Jaudiotagger AudioHeader, PlayCountEntity, extension formatters.
  - **Consumed by:** PlayInfoFragment, SongDetailFragment.
  - **Related files:** InfoResult.kt, data/local/MetadataReader.kt, data/local/room/PlayCountEntity.kt.
  - **Notes:** Gracefully falls back to song fields when metadata missing.


- `ui/screen/info/PlayInfoFragment.kt` — View fragment showing playback statistics (plays/skips, last date, top tracks) for an album/artist/list.
  - **Purpose:** Legacy View screen for per-album/artist play statistics.
  - **Responsibilities:** Load album/artist via InfoViewModel, show play/skip counts, most-played tracks list, Coil cover images.
  - **Inputs:** PlayInfoFragmentArgs (id, name, isArtist).
  - **Outputs:** FragmentPlayInfoBinding-rendered stats.
  - **Dependencies:** InfoViewModel, AbsMainActivityFragment, albumImage/artistImage, dateStr, asNumberOfTimes.
  - **Consumed by:** Navigation from album/artist menus (NavigationExt.kt).
  - **Related files:** InfoViewModel.kt, InfoResult.kt, NavigationExt.kt.
  - **Notes:** Backs out automatically when entity empty.


- `ui/screen/info/SongDetailFragment.kt` — Compose BottomSheetDialogFragment showing song detail with metadata refresh.
  - **Purpose:** Song info bottom sheet with metadata, play info, file info.
  - **Responsibilities:** Refresh via InfoViewModel.refreshSongInfo, sections rendering, navigation to lyrics/tag editors.
  - **Inputs:** SongDetailFragmentArgs (extraSong), InfoViewModel.
  - **Outputs:** SongInfoUiState-driven Compose sheet.
  - **Dependencies:** InfoViewModel, AbsTagEditorActivity EditTarget, LyricsEditorFragmentArgs, SongTagEditorActivity, TitledCard/SmallHeader.
  - **Consumed by:** QueueScreen details action, NavigationExt songDetailArgs.
  - **Related files:** InfoViewModel.kt, InfoResult.kt, QueueScreen.kt, LyricsEditorFragment.kt.
  - **Notes:** Landscape forces expanded BottomSheet.


- `ui/screen/library/LibraryResult.kt` — Library operation results (HandleIntentResult, AddToPlaylistResult, ImportResult, conflict state).
  - **Purpose:** Data classes modeling outcomes of library operations.
  - **Responsibilities:** HandleIntentResult (handled, songs, position, failed), AddToPlaylistResult progress flags, ImportResult localized messages with actual stored name/outcome/song count, PlaylistImportConflictState with existing matches/counts/replace block and inline error enums.
  - **Inputs:** Song lists, playlist names, PlaylistEntity matches, Context for string resources.
  - **Outputs:** LiveData payloads/StateFlow conflict state consumed by UI and dialogs.
  - **Dependencies:** PlaylistEntity, Song model, string resources.
  - **Consumed by:** LibraryViewModel, ImportPlaylistDialog, PlaylistImportConflictDialog, MainActivity.
  - **Related files:** ui/dialogs/playlists/ImportPlaylistDialog.kt, PlaylistImportConflictDialog.kt, PlaylistImportNameResolver.kt.
  - **Notes:** ImportResult exposes Created/Replaced outcome, actualName and importedSongCount; errors carry no success. Conflict state is transient and never persisted.


- `ui/screen/library/LibraryViewModel.kt` — Central library ViewModel: LiveData of all collections, forced reloads, intents, shuffle, playlist import, loading animations.
  - **Purpose:** Central ViewModel feeding all library screens with collections and operations.
  - **Responsibilities:** LiveData for songs/albums/artists/album artists/composers/playlists/genres/years/filesystem/suggestions and a reloadable Not Recently Played list, forceReload(ReloadType), intent handling, playlist CRUD/import, blacklist, margin animations, scrobbling login.
  - **Inputs:** Repository, InclExclDao, CustomPlaylistImageManager, intents, Preferences, StorageUtil.
  - **Outputs:** Collection and Not Recently Played LiveData, HandleIntentResult/AddToPlaylistResult/ImportResult, PlaylistImportConflictState flow/LiveData, FileSystemQuery, LibraryMargin.
  - **Dependencies:** Repository, PlaylistImportNameResolver, InclExclDao, CustomPlaylistImageManager, Preferences, CategoryInfo, MediaScannerConnection, Mutex.
  - **Consumed by:** Library fragments, MainActivity, AddToPlaylistDialog, CreatePlaylistDialog, ImportPlaylistDialog, PlaylistImportConflictDialog, ShuffleModeBottomSheet.
  - **Related files:** HomeResult (SuggestedResult), ReloadType enum, LibraryResult, playlist dialogs.
  - **Notes:** Init initializes blacklist and deletes missing content; declares ReloadType enum. Pending import state may be lost on process death without DB mutation. Backup restore does not call these methods.


- `ui/screen/library/home/HomeBinding.kt` — Typed wrapper of the home layout views.
  - **Purpose:** Type-safe accessor bundle for fragment_home inflated views.
  - **Responsibilities:** Expose root, container, appBarLayout, toolbar, lastAdded, myTopTracks, shuffleButton, history, recyclerView, progressIndicator, empty.
  - **Inputs:** FragmentHomeBinding instance.
  - **Outputs:** View references consumed by HomeFragment.
  - **Dependencies:** FragmentHomeBinding (ViewBinding).
  - **Consumed by:** HomeFragment.
  - **Related files:** layout/fragment_home.xml.
  - **Notes:** Thin delegate hiding binding class from UI code.


- `ui/screen/library/home/HomeFragment.kt` — Library home screen: greeting header, last added, top tracks, history, shuffle button and suggestions.
  - **Purpose:** Home tab aggregating suggestions and quick library actions.
  - **Responsibilities:** HomeAdapter over SuggestedResult, header title, shuffle/last-added/top-tracks/history clicks, suggestion adapters per ContentType, margins, settings-only menu; Aurora replaces the legacy quick-action strip with compact Compose action rows and adds an always-visible search action routed through the shell's real destination.
  - **Inputs:** LibraryViewModel suggestions/miniPlayerMargin, PlayerViewModel.
  - **Outputs:** Navigation to detail lists/playlist/album/artist, openAndShuffleQueue, forceReload.
  - **Dependencies:** AbsMainActivityFragment, HomeAdapter, AlbumAdapter/ArtistAdapter/SongAdapter, NavigationExt args, onSongMenu extensions.
  - **Consumed by:** NavHost graph (nav_home) via AbsSlidingMusicPanelActivity.
  - **Related files:** HomeBinding, HomeResult, DetailListFragment, AlbumDetailFragment, ArtistDetailFragment.
  - **Notes:** Implements IHomeCallback/IScrollHelper; reloads suggestions on media/favorite changes. Aurora keeps Settings in the overflow and the all-songs observer is registered once, backing the shuffle action.


- `ui/screen/library/home/HomeResult.kt` — SuggestedResult with Idle/Loading/Ready states for home suggestions.
  - **Purpose:** UI state wrapper for home suggestion content.
  - **Responsibilities:** State enum (Ready/Loading/Idle), data list of Suggestion, isLoading helper, Idle companion singleton.
  - **Inputs:** List<Suggestion> from repository.homeSuggestions().
  - **Outputs:** LiveData payload for HomeFragment and HomeAdapter.
  - **Dependencies:** Suggestion model.
  - **Consumed by:** LibraryViewModel, HomeFragment, HomeAdapter.
  - **Related files:** LibraryViewModel, HomeFragment.
  - **Notes:** Stateless immutable data holder.


- `ui/screen/library/albums/AlbumListFragment.kt` — Album list/grid fragment with shuffle, menus and detail navigation.
  - **Purpose:** Browse albums grid with view-type/grid-size persistence.
  - **Responsibilities:** Observe getAlbums(), shuffle via PlayerViewModel.openShuffle, AlbumSortMode.AllAlbums menu, detail navigation, SharedPreferences persistence, and an API 30+ composer filter derived from `Album.songs` with `ComposerNamePolicy`.
  - **Inputs:** LibraryViewModel albums, Preferences, AlbumAdapter, composer names, saved filter state.
  - **Outputs:** Navigation nav_album_detail, forceReload(ReloadType.Albums), filtered album grid.
  - **Dependencies:** AbsRecyclerViewCustomGridSizeFragment, AlbumAdapter, AlbumSortMode, SongSortMode.Dynamic(SortKey.Track), ComposerNamePolicy, ComposeChoiceDialogFragment/showComposeChoice, hasR().
  - **Consumed by:** NavHost graph (nav_albums).
  - **Related files:** AlbumDetailFragment, LibraryViewModel, core/model/ComposerNamePolicy.kt, ui/dialogs/ComposeChoiceDialog.kt.
  - **Notes:** Reloads on resume and onMediaContentChanged. Filter by composer is hidden below API 30 or when no normalized composer names exist; quick search applies after the composer filter and the selected composer is restored from instance state.


- `ui/screen/library/albums/AlbumDetailFragment.kt` — Album detail with cover header, song list, similar albums, Last.fm wiki and container transition.
  - **Purpose:** Album detail screen combining header, songs, similar albums, wiki.
  - **Responsibilities:** ConcatAdapter assembly, sort popups, play/shuffle, search/play-info navigation, MaterialContainerTransform, compact song view and duration toggles, and in-place QuickSearchHost filtering of the Songs section (title/album/display artist) with a `quick_search_empty` section header and filtered header playback.
  - **Inputs:** AlbumDetailFragmentArgs (albumId), AlbumDetailViewModel, LastFmAlbum wiki, saved quick-search query.
  - **Outputs:** Navigation to artist/search/play-info, openQueue/openAndShuffleQueue using the visible songs.
  - **Dependencies:** HeaderAdapter, SimpleSongAdapter, AlbumAdapter, WikiAdapter, HorizontalListAdapter, QuickSearchHost/Policy, Coil albumImage.
  - **Consumed by:** Navigation nav_album_detail.
  - **Related files:** AlbumDetailViewModel, AlbumListFragment, ArtistDetailFragment, QuickSearchSupport.kt.
  - **Notes:** Navigates up on empty album; retries wiki in default language. Quick search filters songs only; album rails/wiki/header metadata stay full. The song header shows `quick_search_empty` only when the full `allSongs` source is non-empty, the query is non-blank and the visible song list is empty. The full song snapshot survives async reloads and query restoration.


- `ui/screen/library/albums/AlbumDetailViewModel.kt` — Loads album by id, similar albums and Last.fm wiki.
  - **Purpose:** Per-album detail loader with albumId constructor parameter.
  - **Responsibilities:** loadAlbumDetail(), getAlbumDetail()/getAlbum(), getSimilarAlbums(), getAlbumWiki() gated by NetworkFeature.Lastfm.
  - **Inputs:** Repository, albumId, Application.
  - **Outputs:** LiveData<Album>, LiveData<List<Album>>, LiveData<LastFmAlbum>.
  - **Dependencies:** Repository, NetworkFeature, AndroidViewModel.
  - **Consumed by:** AlbumDetailFragment via parametersOf(arguments.albumId).
  - **Related files:** AlbumDetailFragment.
  - **Notes:** Wiki emission skipped when feature unavailable.


- `ui/screen/library/artists/ArtistListFragment.kt` — Shared artist/album-artist/composer list/grid fragment with shuffle, sorting and context menus.
  - **Purpose:** Browse artist-like entries with sorting and optional album-artist toggle.
  - **Responsibilities:** Observe the LiveData selected by `ArtistListKind`, shuffle via PlayerViewModel.openShuffle, ArtistSortMode.AllArtists menu, detail navigation, per-kind view/grid persistence; hide the legacy onlyAlbumArtists toggle when Album artists is visible.
  - **Inputs:** LibraryViewModel artists/album artists/composers, Preferences, ArtistAdapter, CategoryInfo.
  - **Outputs:** Navigation nav_artist_detail, forceReload for the matching ReloadType.
  - **Dependencies:** AbsRecyclerViewCustomGridSizeFragment, ArtistAdapter, ArtistSortMode, SongSortMode.Dynamic(SortKey.Name).
  - **Consumed by:** NavHost graph (nav_artists, nav_album_artists, nav_composers) and the two thin subclasses.
  - **Related files:** AlbumArtistListFragment.kt, ComposerListFragment.kt, ArtistDetailFragment, LibraryViewModel.
  - **Notes:** Default view type Circle; Artists keeps its original preference keys, while album artists/composers use separate keys. `ArtistListKind` is defined in this file.


- `ui/screen/library/artists/AlbumArtistListFragment.kt` — Album-artist tab using the shared artist list.
  - **Purpose:** Separate Album artists destination.
  - **Responsibilities:** Select ArtistListKind.AlbumArtists and observe getAlbumArtists().
  - **Inputs:** LibraryViewModel album artists.
  - **Outputs:** Album-artist list/grid and detail navigation.
  - **Dependencies:** ArtistListFragment.
  - **Consumed by:** Navigation nav_album_artists.
  - **Related files:** ArtistListFragment.kt, ArtistDetailFragment.kt.
  - **Notes:** No behavior change to Artists when this category is hidden.


- `ui/screen/library/artists/ComposerListFragment.kt` — Composer tab using the shared artist list.
  - **Purpose:** Separate Composers destination.
  - **Responsibilities:** Select ArtistListKind.Composers and observe getComposers().
  - **Inputs:** LibraryViewModel composers.
  - **Outputs:** Composer list/grid and composer detail navigation.
  - **Dependencies:** ArtistListFragment.
  - **Consumed by:** Navigation nav_composers (available on API 30+).
  - **Related files:** ArtistListFragment.kt, ArtistDetailFragment.kt, ComposerNamePolicy.kt.
  - **Notes:** Category is hidden below API 30; repository returns empty defensively.


- `ui/screen/library/artists/ArtistDetailFragment.kt` — Artist/album-artist/composer detail: header image, albums/songs, optional similar artists and biography.
  - **Purpose:** Artist-like detail combining albums, songs and optional editorial content.
  - **Responsibilities:** ConcatAdapter with GridLayoutManager span lookup, sort popups, play/shuffle, preferences (horizontal albums, ignore singles, compact songs), container transition, and songs-only QuickSearchHost filtering (title/album/display artist) with a `quick_search_empty` song header. Composer mode skips biography/similar artists and hides tag-editor/play-info actions.
  - **Inputs:** ArtistDetailFragmentArgs (artistId, artistName, composerName), ArtistDetailViewModel, LastFmArtist bio, saved quick-search query.
  - **Outputs:** Navigation to album/artist/search/play-info, openQueue/openAndShuffleQueue using the visible songs.
  - **Dependencies:** HeaderAdapter, SimpleAlbumAdapter, SimpleSongAdapter, ArtistAdapter, WikiAdapter, QuickSearchHost/Policy, Coil artistImage.
  - **Consumed by:** Navigation nav_artist_detail.
  - **Related files:** ArtistDetailViewModel, ArtistListFragment, AlbumDetailFragment, QuickSearchSupport.kt.
  - **Notes:** Composer lookup takes precedence over album-artist name/id. Quick search never filters the album grid/rail or similar artists; header playback uses the filtered song list while counts stay full. The song header shows `quick_search_empty` only when the full `allSongs` source is non-empty, the query is non-blank and the visible song list is empty.


- `ui/screen/library/artists/ArtistDetailViewModel.kt` — Loads artist by id/name/composer, similar artists and Last.fm biography.
  - **Purpose:** Per-artist-like detail loader keyed by composer name, album-artist name or id.
  - **Responsibilities:** loadArtistDetail() (composerByName, albumArtistByName or artistById), getSimilarArtists() sorted by media name, getArtistBio() feature-gated.
  - **Inputs:** Repository, artistId, artistName?, composerName?, Application.
  - **Outputs:** LiveData<Artist>, LiveData<List<Artist>>, LiveData<LastFmArtist?>.
  - **Dependencies:** Repository, NetworkFeature, sortedByMediaName extension.
  - **Consumed by:** ArtistDetailFragment via parametersOf(artistId, artistName, composerName).
  - **Related files:** ArtistDetailFragment.
  - **Notes:** Composer name wins over artist name/id; returns Artist.empty when all identifiers are invalid.


- `ui/screen/library/folders/FoldersListFragment.kt` — Filesystem explorer fragment (FileAdapter, IFileCallback) with recursive navigation.
  - **Purpose:** Browse folders/files in hierarchy or flat view.
  - **Responsibilities:** FileAdapter over FileSystemQuery, back navigation via IBackConsumer, song/folder click handling, blacklist/scan/start-directory actions, FileSortMode menu.
  - **Inputs:** LibraryViewModel fileSystem, Preferences (hierarchyFolderView, songClickAction), filesystem items.
  - **Outputs:** navigateToPath, openSongs, folderDetailArgs navigation, blacklistPath, scanPaths.
  - **Dependencies:** AbsRecyclerViewCustomGridSizeFragment, FileAdapter, FileSortMode, IFileCallback, IBackConsumer.
  - **Consumed by:** NavHost graph (nav_folders).
  - **Related files:** FolderDetailFragment, LibraryViewModel.
  - **Notes:** Handles back press to navigate up; grid size persisted.


- `ui/screen/library/folders/FolderDetailFragment.kt` — Folder detail showing the songs of a given path.
  - **Purpose:** List songs of a folder path passed via nav args.
  - **Responsibilities:** SongAdapter (SongSortMode.FolderSongs), play/shuffle buttons, search navigation, sort menu reload, pop back on a genuinely empty source, and in-place QuickSearchHost filtering including the song file name.
  - **Inputs:** FolderDetailFragmentArgs (extraFolderPath), FolderDetailViewModel, miniPlayerMargin, saved quick-search query.
  - **Outputs:** openQueue/openAndShuffleQueue using visible songs, nav_search, loadDetail().
  - **Dependencies:** AbsMainActivityFragment, SongAdapter, SongSortMode.FolderSongs, QuickSearchHost/Policy.
  - **Consumed by:** Navigation nav_folder_detail (from FoldersListFragment).
  - **Related files:** FolderDetailViewModel, FoldersListFragment, QuickSearchSupport.kt.
  - **Notes:** Shares fragment_detail_list layout with genre/year/details. An empty loaded source pops the back stack regardless of the query; a non-empty source with no matches shows `quick_search_empty` and never pops. Full duration/count subtitle stays on the full source.


- `ui/screen/library/folders/FolderDetailViewModel.kt` — Minimal ViewModel loading a Folder by path.
  - **Purpose:** Load a Folder from its filesystem path.
  - **Responsibilities:** getFolder() LiveData, loadDetail() via repository.folderByPath, init auto-load.
  - **Inputs:** Repository, folderPath string.
  - **Outputs:** LiveData<Folder>.
  - **Dependencies:** Repository.
  - **Consumed by:** FolderDetailFragment via parametersOf(arguments.extraFolderPath).
  - **Related files:** FolderDetailFragment.
  - **Notes:** —.


- `ui/screen/library/genres/GenresListFragment.kt` — Genre list/grid fragment with sorting and forced reload on resume.
  - **Purpose:** Browse genres grid.
  - **Responsibilities:** Observe getGenres(), GenreSortMode.AllGenres menu, genre detail navigation, grid/view-type prefs, reload on resume.
  - **Inputs:** LibraryViewModel genres, GenreAdapter.
  - **Outputs:** Navigation nav_genre_detail, forceReload(ReloadType.Genres).
  - **Dependencies:** AbsRecyclerViewCustomGridSizeFragment, GenreAdapter, GenreSortMode.
  - **Consumed by:** NavHost graph (nav_genres).
  - **Related files:** GenreDetailFragment, LibraryViewModel.
  - **Notes:** No shuffle button (isShuffleVisible=false).


- `ui/screen/library/genres/GenreDetailFragment.kt` — Genre detail with Songs/Albums/Artists subcategories.
  - **Purpose:** Browse a genre's songs, albums or artists.
  - **Responsibilities:** Compose View dialog with GenreSubcategory choices, adapter switching between SongAdapter/AlbumAdapter/ArtistAdapter, matching GenreSongs/GenreAlbums/GenreArtists sort modes, play/shuffle on visible songs, album/artist detail navigation, search navigation, navigate up on a genuinely empty genre, and in-place QuickSearchHost filtering.
  - **Inputs:** GenreDetailFragmentArgs (extraGenre), GenreDetailViewModel/GenreContent, miniPlayerMargin, saved quick-search query and saved subcategory.
  - **Outputs:** openQueue/openAndShuffleQueue using visible songs, nav_album_detail/nav_artist_detail, nav_search, loadGenreContent().
  - **Dependencies:** AbsMainActivityFragment, SongAdapter, AlbumAdapter, ArtistAdapter, GenreSubcategory, GenreContent, ComposeChoiceDialogFragment/showComposeChoice, QuickSearchHost/Policy.
  - **Consumed by:** Navigation nav_genre_detail.
  - **Related files:** GenreDetailViewModel, GenresListFragment, SearchFragment, QuickSearchSupport.kt, data/model/GenreContent.kt.
  - **Notes:** Shares fragment_detail_list layout. An empty loaded genre navigates up regardless of the query; a non-empty subcategory with no matches shows `quick_search_empty` and never navigates. Subtitle count/duration stays on the full genre song set. Action modes are finished before switching adapters, and the selected subcategory is restored from instance state.


- `ui/screen/library/genres/GenreDetailViewModel.kt` — Loads the songs, albums and artists of a genre.
  - **Purpose:** Fetch the content snapshots for genre detail subcategories.
  - **Responsibilities:** getContent() LiveData, loadGenreContent() via repository.genreContent(genre.id), init auto-load.
  - **Inputs:** Repository, Genre.
  - **Outputs:** LiveData<GenreContent>.
  - **Dependencies:** Repository, GenreContent.
  - **Consumed by:** GenreDetailFragment via parametersOf(arguments.extraGenre).
  - **Related files:** GenreDetailFragment, data/model/GenreContent.kt.
  - **Notes:** —.


- `ui/screen/library/playlists/PlaylistListFragment.kt` — Playlist list/grid fragment with menus, create/import dialogs and detail navigation.
  - **Purpose:** Browse playlists with creation/import entry points.
  - **Responsibilities:** Observe getPlaylists(), PlaylistSortMode.AllPlaylists menu, CreatePlaylistDialog/ImportPlaylistDialog, detail navigation, grid size prefs.
  - **Inputs:** LibraryViewModel playlists, PlaylistAdapter.
  - **Outputs:** Navigation nav_playlist_detail, dialog shows, forceReload(ReloadType.Playlists).
  - **Dependencies:** AbsRecyclerViewCustomGridSizeFragment, PlaylistAdapter, PlaylistSortMode, playlist dialogs.
  - **Consumed by:** NavHost graph (nav_playlists).
  - **Related files:** PlaylistDetailFragment, CreatePlaylistDialog, ImportPlaylistDialog.
  - **Notes:** No shuffle; view type always Normal.


- `ui/screen/library/playlists/PlaylistDetailFragment.kt` — Playlist detail (incl. favorites) with drag & drop reordering, internal search, menus.
  - **Purpose:** Playlist detail with drag/drop reordering and song management.
  - **Responsibilities:** PlaylistSongAdapter with RecyclerViewDragDropManager, lock toggle, remove-from-playlist dialogs, play/shuffle, header info, exists check, fast scroller, and drag-safe QuickSearchHost filtering.
  - **Inputs:** PlaylistDetailFragmentArgs (playlistId), PlaylistDetailViewModel, Preferences, LibraryViewModel, saved quick-search query.
  - **Outputs:** openQueue/openAndShuffleQueue using the visible songs, nav_search, RemoveFromPlaylistDialog, full-order `saveSongs` on pause.
  - **Dependencies:** AbsMainActivityFragment, PlaylistSongAdapter, advrecyclerview drag/drop, QuickSearchHost/Policy, Coil playlistImage, MaterialContainerTransform.
  - **Consumed by:** Navigation nav_playlist_detail.
  - **Related files:** PlaylistDetailViewModel, PlaylistListFragment, RemoveFromPlaylistDialog, QuickSearchSupport.kt.
  - **Notes:** Delete action hidden for favorites playlist. While a query is non-blank the adapter is filtered and drag is forced off; `onPause` always saves the full `allSongs` order (guarded until the first song emission), never the filtered subset. Clearing restores the full order and `Preferences.lockedPlaylists`.


- `ui/screen/library/playlists/PlaylistDetailViewModel.kt` — Exposes playlist songs, existence and PlaylistWithSongs observables.
  - **Purpose:** Observable access to playlist data.
  - **Responsibilities:** playlistSongsObservable, checkPlaylistExists, playlistWithSongsObservable.
  - **Inputs:** PlaylistRepository, playlistId.
  - **Outputs:** LiveData<List<SongEntity>>, LiveData<Boolean>, LiveData<PlaylistWithSongs>.
  - **Dependencies:** PlaylistRepository.
  - **Consumed by:** PlaylistDetailFragment via parametersOf(arguments.playlistId).
  - **Related files:** PlaylistDetailFragment.
  - **Notes:** Pure pass-through to PlaylistRepository.


- `ui/screen/library/search/SearchFragment.kt` — Search screen with text field, voice input, SearchFilters, grouped results and navigation.
  - **Purpose:** Full-library search UI with filters, modes and voice input.
  - **Responsibilities:** SearchViewModel wiring, SearchAdapter grouped results, mode toggle group, voice search launcher, keyboard popup, queue emission, detail navigation.
  - **Inputs:** SearchViewModel flows, optional nav args (FILTER, MODE, QUERY), libraryViewModel playlists.
  - **Outputs:** openSongs via queueFlow, navigation to album/artist/playlist/genre details, refresh().
  - **Dependencies:** AbsMainActivityFragment, SearchAdapter, SearchFilter, SearchQuery.FilterMode, KeyboardVisibilityEvent.
  - **Consumed by:** NavHost graph (nav_search), launched from Home and all detail fragments.
  - **Related files:** SearchViewModel, LibraryViewModel, SearchAdapter.
  - **Notes:** Debounced results; enter key hides keyboard.


- `ui/screen/library/search/SearchViewModel.kt` — Combines query+filter with 300 ms debounce, executes repository.search and emits the resulting queue.
  - **Purpose:** Orchestrate search state and playback queue emission.
  - **Responsibilities:** combine query+filter with debounce(300), repository.search, songClick start-position computation, refresh() via timestamp.
  - **Inputs:** Repository, Preferences (playAllSongsWhenSearching, songClickAction).
  - **Outputs:** searchFilter/searchResult StateFlows, queueFlow SharedFlow.
  - **Dependencies:** Repository, SearchQuery, SearchFilter, Preferences, indexOfSong extension.
  - **Consumed by:** SearchFragment.
  - **Related files:** SearchFragment.
  - **Notes:** distinctUntilChanged prevents duplicate repository searches.


- `ui/screen/library/songs/SongListFragment.kt` — All-songs list/grid fragment with shuffle, sorting and menus.
  - **Purpose:** Browse all songs.
  - **Responsibilities:** Observe getSongs(), shuffle openAndShuffleQueue, SongSortMode.AllSongs menu, view-type/grid prefs, reload on resume.
  - **Inputs:** LibraryViewModel songs, SongAdapter.
  - **Outputs:** openAndShuffleQueue, forceReload(ReloadType.Songs).
  - **Dependencies:** AbsRecyclerViewCustomGridSizeFragment, SongAdapter, SongSortMode.
  - **Consumed by:** NavHost graph (nav_songs).
  - **Related files:** LibraryViewModel, AbsRecyclerViewFragment (shuffle sheet).
  - **Notes:** —.


- `ui/screen/library/years/YearsListFragment.kt` — Release-year list/grid fragment with sorting and reload on resume.
  - **Purpose:** Browse release years.
  - **Responsibilities:** Observe getYears(), YearSortMode.AllYears menu, year detail navigation via YearDetailFragmentArgs, multi-select song menus, prefs persistence.
  - **Inputs:** LibraryViewModel years, YearAdapter.
  - **Outputs:** Navigation nav_year_detail, songs menus via libraryViewModel.songs(selection).
  - **Dependencies:** AbsRecyclerViewCustomGridSizeFragment, YearAdapter, YearSortMode.
  - **Consumed by:** NavHost graph (nav_years).
  - **Related files:** YearDetailFragment, LibraryViewModel.
  - **Notes:** —.


- `ui/screen/library/years/YearDetailFragment.kt` — Year detail listing its songs.
  - **Purpose:** List songs of a release year.
  - **Responsibilities:** SongAdapter, play/shuffle buttons, search navigation, SongSortMode.YearSongs menu, navigate up on a genuinely empty source, and in-place QuickSearchHost filtering.
  - **Inputs:** YearDetailFragmentArgs (extraYear), YearDetailViewModel, miniPlayerMargin, saved quick-search query.
  - **Outputs:** openQueue/openAndShuffleQueue using visible songs, nav_search, loadDetail().
  - **Dependencies:** AbsMainActivityFragment, SongAdapter, ReleaseYear model, QuickSearchHost/Policy.
  - **Consumed by:** Navigation nav_year_detail.
  - **Related files:** YearDetailViewModel, YearsListFragment, QuickSearchSupport.kt.
  - **Notes:** Reuses menu_genre_detail resource. An empty loaded source navigates up regardless of the query; a non-empty source with no matches shows `quick_search_empty` and never navigates. Empty subtitle metadata stays on the full source.


- `ui/screen/library/years/YearDetailViewModel.kt` — Loads a ReleaseYear by year.
  - **Purpose:** Load ReleaseYear for a year integer.
  - **Responsibilities:** getYear() LiveData, loadDetail() via repository.yearById(year), init auto-load.
  - **Inputs:** Repository, year Int.
  - **Outputs:** LiveData<ReleaseYear>.
  - **Dependencies:** Repository.
  - **Consumed by:** YearDetailFragment via parametersOf(arguments.extraYear).
  - **Related files:** YearDetailFragment.
  - **Notes:** —.


- `ui/screen/lyrics/LyricsFragment.kt` — Compose fragment container of LyricsScreen, navigating to the editor.
  - **Purpose:** Host full-screen lyrics view.
  - **Responsibilities:** View-lifecycle-owned ComposeView with LyricsScreen, navigate to LyricsEditorFragment, expand panel on exit; report opaque lyrics backgrounds to the Aurora ribbon host.
  - **Inputs:** PlayerViewModel (activity-scoped).
  - **Outputs:** LyricsScreen rendering; navigation events.
  - **Dependencies:** PlayerViewModel, LyricsEditorFragmentArgs, MainActivity.expandPanel.
  - **Consumed by:** Navigation graph from player cover actions.
  - **Related files:** LyricsScreen.kt, LyricsEditorFragment.kt.
  - **Notes:** currentFragment check avoids panel expansion over editor.


- `ui/screen/lyrics/LyricsScreen.kt` - Compose lyrics screen: auto-scrolling, blur/background effects, keepScreenOn, auto-advance with playback, line multi-select/copy/share, lyrics-source picker and A-B line sheet.
  - **Purpose:** Render synced/plain lyrics with playback-synced highlighting.
  - **Responsibilities:** LyricsUiState sealed class, smooth playback position interpolation, gradient/blur backgrounds, LyricsView rendering, seek callbacks; conditional transparent centered Aurora presentation, motion-gated indicators and background-occlusion reporting; cover-lyrics expand control labelled `action_show_lyrics`; Aurora lyric legibility (fixed `.72` player/background-effects backplate, explicit white Aurora player foreground, `.92` inactive alpha passed to the view).
  - **Inputs:** LibraryViewModel, LyricsViewModel, PlayerViewModel, Song.
  - **Outputs:** Lyrics UI states (Loading/Empty/Instrumental/Plain/Synced).
  - **Dependencies:** LyricsViewModel, PlayerViewModel, LyricsView, AnimatedEqBars, Coil SingletonImageLoader, extractGradientColors.
  - **Consumed by:** LyricsFragment, CoverLyricsFragment (CoverLyricsScreen).
  - **Related files:** LyricsViewModel.kt, LyricsFragment.kt, player/cover/CoverLyricsFragment.kt, ui/component/compose/lyrics/LyricsView.kt.
  - **Notes:** keepScreenOn and power-save mode respected; rememberSmoothPlaybackPosition public helper. With Aurora motion disabled, loading remains an honest static text state rather than a simulated percentage. Aurora player/background-effects lyrics sit on the fixed black backplate with forced white foreground so a media-derived tint cannot produce dark text; the inactive alpha defaults to `.4f` outside Aurora.


- `ui/screen/lyrics/LyricsViewModel.kt` - Central lyrics ViewModel: loading from repository/network, InstrumentalDetector, view settings, editor state and per-song source selection.
  - **Purpose:** Manage lyrics loading, view settings, editor state.
  - **Responsibilities:** Best-source lyrics resolution, InstrumentalDetector, LyricsViewSettings from preferences, font import, editor load/save/download, preference listener.
  - **Inputs:** SharedPreferences, LyricsRepository, Song.
  - **Outputs:** lyricsUiState, editor state, settings flows, save/download/permission events.
  - **Dependencies:** LyricsRepository, InstrumentalDetector, LyricsViewSettings, NetworkFeature flags.
  - **Consumed by:** LyricsScreen, LyricsEditorScreen, PreferencesScreenFragment.
  - **Related files:** LyricsScreen.kt, LyricsEditorScreen.kt, data/local/repository/LyricsRepository.kt.
  - **Notes:** Imports TTF/OTF fonts validating magic bytes; LRCLib search URL helper.


- `ui/screen/lyrics/LyricsEditorFragment.kt` — Compose fragment showing LyricsEditorScreen for the song from navigation args.
  - **Purpose:** Container for the lyrics editor screen.
  - **Responsibilities:** Read LyricsEditorFragmentArgs song, ComposeView with LyricsEditorScreen, back navigation, transition.
  - **Inputs:** LyricsEditorFragmentArgs (extraSong).
  - **Outputs:** LyricsEditorScreen composition.
  - **Dependencies:** LyricsEditorScreen, SsilencceTheme, materialSharedAxis.
  - **Consumed by:** Navigation from LyricsFragment and SongDetailFragment.
  - **Related files:** LyricsEditorScreen.kt, LyricsFragment.kt, SongDetailFragment.kt.
  - **Notes:** Minimal passthrough fragment.


- `ui/screen/lyrics/LyricsEditorScreen.kt` - Compose lyrics editor: text editing with preview, instrumental detection, online search, saving lyrics/LRC files, Edit/Sync mode and stamped LRC export.
  - **Purpose:** Edit lyrics across Embedded/Downloaded/File sources.
  - **Responsibilities:** TextFieldState per source, MediaStore write permission requests, download/search dialogs, save via viewModel, clipboard/select-all/undo, landscape layouts.
  - **Inputs:** Song, LyricsViewModel, onBackClick.
  - **Outputs:** LyricsEditorResult events; lyrics saved through LyricsRepository.
  - **Dependencies:** LyricsViewModel, LyricsSource, LyricsMode, NetworkFeature, MediaStore.createWriteRequest, TopAppBarMenu.
  - **Consumed by:** LyricsEditorFragment.
  - **Related files:** LyricsEditorFragment.kt, LyricsViewModel.kt, LyricsResult helpers.
  - **Notes:** File source is read-only; SnapshotMapSaver persists edits across config changes. Aurora uses shell-owned navigation clearance and explanatory disabled manual-sync/QR actions.


- `ui/screen/widgets/WidgetConfigActivity.kt` - Compose configure/reconfigure activity for each widget instance.
  - **Purpose:** Let the user choose per-widget theme mode and transparency before/after placement.
  - **Responsibilities:** Read EXTRA_APPWIDGET_ID, load WidgetSettings, System/AlbumArt/Wallpaper radio choices, derive and cache the wallpaper color on save, 0-100 slider, save, update the matching Glance widget, return RESULT_OK/RESULT_CANCELED.
  - **Inputs:** AppWidgetManager extras, WidgetSettingsStore, AppWidgetManager provider info.
  - **Outputs:** Persisted per-widget settings, Glance widget update, activity result.
  - **Dependencies:** ComponentActivity/Compose Material3, WidgetSettingsStore, WallpaperThemeProvider, GlanceAppWidgetManager, three widget classes.
  - **Consumed by:** android:configure entries in the three glance_*_widget_info.xml files; launcher reconfiguration.
  - **Related files:** WidgetSettings.kt, SsilencceGlanceWidget.kt, CardWidget.kt, FullWidget.kt.
  - **Notes:** Wallpaper is derived on save and cached; if derivation fails the widget falls back to album art/system. No permission or dependency added.
- `ui/screen/other/DetailListFragment.kt` - Generic content list fragment (albums/artists/songs per ContentType) with history removal, period statistics and quick content actions.
  - **Purpose:** Reusable screen for home "see all" content lists and history/statistics destinations.
  - **Responsibilities:** ContentType switch loading (top/recent artists/albums, top tracks, history, recent songs, favorites, not-recently-played), per-type adapters, full snapshots with in-place QuickSearchHost filtering, period selector for Top Tracks/Top Artists, Not Recently Played threshold dialog, reactive Room-Flow statistics, subtitle period/threshold labels, history deletion (single + confirmed multi-select), stats-history note/empty message, clear history, search navigation, manual statistics CSV export through SAF.
  - **Inputs:** DetailListFragmentArgs (type), LibraryViewModel flows/LiveData, Preferences cutoffs/custom Not Recently Played days, ListeningPeriod selection, saved quick-search query.
  - **Outputs:** Navigation to album/artist details, nav_search, clearHistory(), deleteSongsFromHistory(), openQueue/openAndShuffleQueue using the visible rows, Not Recently Played reload after a threshold change; prepared CSV document and SAF export result.
  - **Dependencies:** AbsMainActivityFragment, SongAdapter/ArtistAdapter/AlbumAdapter, ContentType, ListeningPeriod, ArtistPlayStat, QuickSearchHost/Policy, NotPlayedDaysDialogFragment, MaterialAlertDialogBuilder, fast scroller, StatisticsCsvExporter.
  - **Consumed by:** Navigation nav_detail_list (from HomeFragment detailArgs).
  - **Related files:** HomeFragment, LibraryViewModel, ContentType, ListeningPeriod, ArtistPlayStat, SongAdapter, QuickSearchSupport.kt, NotPlayedDaysDialog.kt, util/statistics/StatisticsCsvExporter.kt.
  - **Notes:** Shuffle hidden for non-song content types. Selected-period StateFlow drives reactive statistics; complete artist rows include unavailable historical snapshots and denominator-only updates. Top Artists uses `item_grid_circle` because it supplies both title and text views for the percentage; filtering happens on `ArtistPlayStat` before `submitPlayStats` so percentages survive, and header/metadata counts stay full. Toolbar play is enabled from the filtered playable `songList` (artists with resolved songs), not from the stat count. Empty stats keep `no_play_stats_period`; filtered no-match uses `quick_search_empty` and never navigates up; audio/session behavior is outside this fragment (ADR-040, ADR-044). The export action is hidden outside Top Tracks/Top Artists; the selected period is snapshotted before SAF, pending CSV survives recreation, and cancel/failure paths avoid success claims. The Not played period action is visible only for Not Recently Played; its result is persisted and reloads the same LiveData, while the subtitle shows the effective history-interval or custom-day threshold.

- `ui/screen/other/MiniPlayerFragment.kt` — Bottom mini-player: cover, title, play/next/prev buttons (with long-press gestures), progress, panel expansion.
  - **Purpose:** Compact playback controls above the sliding panel.
  - **Responsibilities:** Observe PlayerViewModel flows (currentSong, progress/duration, isPlaying), button styling via NowPlayingButtonStyle, SkipButtonTouchHandler gestures, swipe-to-skip fling, wavy progress.
  - **Inputs:** PlayerViewModel (activityViewModel), Preferences, resources.
  - **Outputs:** seekForward/seekBack, seekToNext/seekToPrevious, togglePlayPause.
  - **Dependencies:** Fragment, SkipButtonTouchHandler, Coil songImage, GestureDetector, ProgressState.
  - **Consumed by:** sliding_music_panel_layout.xml, AbsSlidingMusicPanelActivity (panel expansion, preference callbacks).
  - **Related files:** PlayerViewModel, AbsSlidingMusicPanelActivity.
  - **Notes:** Setup methods invoked by activity preference listeners. Aurora glass/48 dp play target is applied conditionally; root layout parameters are changed only after attachment.


- `ui/screen/other/ShuffleModeBottomSheet.kt` - Compose bottom sheet choosing special shuffle modes over the full library.
  - **Purpose:** Compose UI listing available SpecialShuffleMode options.
  - **Responsibilities:** Observe songs/shuffleOperationState, forceReload Songs when empty, filter composer-dependent modes with ShuffleModeAvailabilityPolicy for the current SDK, equal-height ShuffleModeItem rows, busy/progress states, mode click triggers openSpecialShuffle.
  - **Inputs:** LibraryViewModel, PlayerViewModel, SpecialShuffleMode entries, current SDK int.
  - **Outputs:** playerViewModel.openSpecialShuffle(allSongs, selected available mode).
  - **Dependencies:** Compose Material3, BottomSheetDialogSurface, SpecialShuffleMode, ShuffleModeAvailabilityPolicy, ShuffleOperationState, Build.
  - **Consumed by:** ShuffleModeFragment.
  - **Related files:** ShuffleModeFragment, PlayerViewModel, LibraryViewModel, ShuffleModeAvailabilityPolicy.kt.
  - **Notes:** Filters MostPlayedComposers below Android 11/API 30 while keeping MostPlayedAlbumArtists available; equalizes row heights via onGloballyPositioned; crossfade spinner while shuffling.


- `ui/screen/other/ShuffleModeFragment.kt` — Compose BottomSheetDialogFragment wrapping ShuffleModeBottomSheet with library/player ViewModels.
  - **Purpose:** Dialog wrapper hosting the Compose shuffle-mode sheet.
  - **Responsibilities:** Obtain activity-scoped LibraryViewModel/PlayerViewModel, inflate ComposeView with SsilencceTheme and ShuffleModeBottomSheet.
  - **Inputs:** Koin activityViewModel instances.
  - **Outputs:** Bottom sheet UI shown over library screens.
  - **Dependencies:** BottomSheetDialogFragment, ComposeView, SsilencceTheme.
  - **Consumed by:** AbsRecyclerViewFragment (SHUFFLE_MODE tag).
  - **Related files:** ShuffleModeBottomSheet, AbsRecyclerViewFragment.
  - **Notes:** —.

- `ui/screen/permissions/PermissionsActivity.kt` — Initial permission requests (storage/audio, images, Bluetooth, exact alarms) with PermissionView.
  - **Purpose:** First-run permission onboarding.
  - **Responsibilities:** Version-gated PermissionView visibility/order, requestPermissions, exact-alarm settings intent, finish gating, launch MainActivity.
  - **Inputs:** Manifest permissions, ActivityPermissionBinding.
  - **Outputs:** Permission grants; MainActivity intent.
  - **Dependencies:** PermissionView, AbsBaseActivity, AlarmManager, AppCompat.
  - **Consumed by:** AbsSlidingMusicPanelActivity (launcher).
  - **Related files:** MainActivity.kt, ui/component/views/PermissionView.kt.
  - **Notes:** Back press finishes affinity; rechecks on resume.

- `ui/screen/player/PlayerAnimator.kt` — Abstract AnimatorSet manager for player animations (prepare/start/disable with FastOutSlowIn).
  - **Purpose:** Base class building and running AnimatorSets for player control entrance animations.
  - **Responsibilities:** prepare/start/disable, FastOutSlowIn interpolator, addScaleAnimation helper, subclass hooks.
  - **Inputs:** isEnabled flag, Views from subclass bindings.
  - **Outputs:** prepared/started AnimatorSet, isPrepared state.
  - **Dependencies:** AnimatorSet, ObjectAnimator, FastOutSlowInInterpolator.
  - **Consumed by:** AbsPlayerControlsFragment (playerAnimator).
  - **Related files:** Default/Expressive/FullCover/M3/Peek/Plain PlayerAnimator subclasses.
  - **Notes:** Subclasses override onAddAnimation and onPrepareForAnimation.


- `ui/screen/player/PlayerGesturesController.kt` — Gesture controller (OnTouchListener + GestureDetector) mapping taps/double-taps/long-presses/flings to GestureType.
  - **Purpose:** Translate raw touch events into typed gestures for player surfaces.
  - **Responsibilities:** GestureDetector, tap/double-tap edge split, long-press, fling direction, acceptedGestures filter.
  - **Inputs:** Context, acceptedGestures, Listener, MotionEvents, view Rect.
  - **Outputs:** gestureDetected callbacks for accepted gestures.
  - **Dependencies:** GestureDetector, nested GestureType sealed class.
  - **Consumed by:** AbsPlayerFragment, CoverPagerFragment.
  - **Related files:** GestureType (nested), AbsPlayerFragment gesture handling.
  - **Notes:** Double-tap edges at 0.30/0.70 width thresholds; fling picks dominant axis.


- `ui/screen/player/PlayerViewModel.kt` - Central player ViewModel: MediaController subscription, progress, events, shuffle/queue, named queues, session commands, palette colors.
  - **Purpose:** Bind MediaController playback state to UI flows and dispatch session commands.
  - **Responsibilities:** queue/position/currentSong flows, real queue-ended flow, ProgressObserver polling, Player.Listener events, color scheme generation, queue mutations, dedicated replay/seek restart, timeline-index removal of unavailable MediaItems, single-item removal/insertion for swipe undo, shuffle openers, explicit re-shuffle dispatch with empty/shuffle-off guards and feedback, key-based multi-selection block-move planning through QueuePosition and QueueBatchMovePolicy, named-queue CRUD/switch events with duplicate/limit-aware save/rename feedback, active-only First/Previous/Next named-queue chain via the stable ordered ids, audio-focus confirm/cancel commands.
  - **Inputs:** MediaController via setMediaController, SharedPreferences, Repository, NamedQueueRepository, MediaEvent.
  - **Outputs:** StateFlows (queueFlow, currentSongFlow, colorSchemeFlow, namedQueuesFlow, activeNamedQueueIdFlow, isQueueEndedFlow), mediaEvent, stopAfterPosition, `NamedQueueMessage` events carrying actual names/limit errors, `queueMessage` resource events for re-shuffle and block-move failure feedback.
  - **Dependencies:** MediaController, ProgressObserver, ShuffleManager, Repository, NamedQueueRepository, QueueNamePolicy, QueueEndPolicy, QueueReshufflePolicy, QueueBatchMovePolicy, Preferences, Playback constants.
  - **Consumed by:** AbsPlayerFragment, AbsPlayerControlsFragment, AbsSlidingMusicPanelActivity, MainActivity, QueueScreen.
  - **Related files:** PlaybackService, MediaEvent, QueuePosition, QueueSong, NamedQueueRepository.
  - **Notes:** Koin-injected; debounced queue rebuild (QUEUE_DEBOUNCE=100ms); `openQueue` defers the last request in a single `PendingQueue` slot when the controller is not connected, replays it once from `setMediaController`, and a request sequence guard prevents an older in-flight lookup from overriding a newer request; removes missing MediaItems by `QueueItem.second` timeline indices, grouping consecutive ranges and removing them descending (never by shuffle-list positions). The removal pass captures one `queueTimeline` snapshot before reading the queue, then revalidates timeline identity and `mediaItemCount == windowCount` on Main; a stale pass removes nothing and returns before publishing queue/position flows. Named-queue switching and audio-focus commands are session commands; queue-search actions retain original indices (ADR-041, ADR-042). `restoreRemovedPosition` is used only by the queue swipe Undo and inserts at the current mapping of the original logical index (append if past the end). `isQueueEndedFlow` is true only for `STATE_ENDED` with a non-empty queue and resets on play/seek/timeline/media-item/empty; `replayQueue()` seeks to logical position 0 (first of the current shuffled order) with `seekToDefaultPosition` and starts without changing shuffle/repeat. The named chain requires an active id and non-empty current queue, verifies the active snapshot's media IDs/order/count exactly match the controller timeline, then resolves First/Previous/Next with `NamedQueueChainPolicy` over `orderedQueueIds()` and reuses `switchToNamedQueue` (ADR-046); unrelated/modified/deleted/boundary/empty targets emit messages and no partial switch. `reshuffleQueue()` rejects an empty queue or shuffle off with an explicit message, sends the app-internal re-shuffle command and reports done/invalid/failure; it never enables shuffle. `moveQueueBlock()` rejects fewer than two selected keys and inside-selection drops, resolves every effective position and the target through `QueuePosition.getIndexForPosition`, plans via `QueueBatchMovePolicy`, sends the from/to arrays with `MOVE_QUEUE_BLOCK` and reports failure. Save/Rename emit `NamedQueueMessage` with the actual stored name, limit or name-length error. If Rename replaces the active named queue target, the active named-queue id/marker is cleared after success; renaming the current active queue keeps its id, and the active playback queue is never touched.
`openSpecialShuffle()` rebuilds the weighted queue and starts playback at item 0/position 0; it
does not capture or restore the current item, position or play/pause intent. The non-special
`openShuffle()` path is unchanged.


- `ui/screen/player/QueueEndPolicy.kt` - Pure end-of-queue decisions.
  - **Purpose:** Keep the ended/footer/replay rules deterministic and JVM-probeable.
  - **Responsibilities:** `isEnded` requires `STATE_ENDED` plus a non-empty queue; `shouldShowFooter` requires ended plus non-empty; `replayTimelineIndex` maps logical position 0 to the current effective/shuffled timeline index or falls back to 0.
  - **Inputs:** Playback-state boolean, media item count, queue size, mapped first index.
  - **Outputs:** Ended/footer booleans and replay timeline index.
  - **Dependencies:** Kotlin stdlib only.
  - **Consumed by:** PlayerViewModel, QueueScreen, compiled end-policy probe.
  - **Related files:** PlayerViewModel.kt, QueueScreen.kt.
  - **Notes:** No Android/Media3 dependency; shuffle/repeat are not modified by replay.


- `ui/screen/player/cover/CoverPagerFragment.kt` - ViewPager of covers (carousel effect) with player gestures and palette color capture.
  - **Purpose:** Host the swipable cover pager and propagate palette colors upward.
  - **Responsibilities:** AlbumCoverPagerAdapter, page transformers, PlayerGesturesController, queue/position sync, lyrics overlay toggle, keepScreenOn.
  - **Inputs:** PlayerViewModel flows, Preferences, PaletteColor via ColorReceiver.
  - **Outputs:** onColorChanged/onLyricsVisibilityChange callbacks, playSongAt on page select.
  - **Dependencies:** PlayerViewModel, ImageFragment, CarouselPagerTransformer, CustomFragmentStatePagerAdapter.
  - **Consumed by:** All style player layouts (fragment_default_player, fragment_m3_player, etc.).
  - **Related files:** CoverLyricsFragment, ImageFragment, AbsPlayerFragment, NowPlayingScreen.
  - **Notes:** Lyrics loading gated by NowPlayingScreen.supportsCoverLyrics. `onSharedPreferenceChanged` and `requestColor` return when `_binding` is null so queued whole-map restore notifications cannot dereference a destroyed view (ADR-047).


- `ui/screen/player/cover/page/ImageFragment.kt` — Single cover page fragment loading with Coil, applying theme layouts and processing palette colors.
  - **Purpose:** Display one song's cover with per-style styling and color extraction.
  - **Responsibilities:** Layout selection (carousel/album cover), Coil songImage load, corner radius, PaletteProcessor extraction, ColorReceiver responses.
  - **Inputs:** Song via EXTRA_SONG argument, Preferences, NowPlayingScreen.
  - **Outputs:** onColorReady PaletteColor, rendered bitmap.
  - **Dependencies:** Coil songImage, PaletteProcessor, PaletteColor, Preferences.
  - **Consumed by:** AlbumCoverPagerAdapter (CoverPagerFragment).
  - **Related files:** CoverPagerFragment, AudioCoverFetcher, PaletteColor.
  - **Notes:** memoryCacheKey "nowplaying:song:{id}"; error falls back to errorColor.


- `ui/screen/player/cover/CoverLyricsFragment.kt` — Compose fragment showing lyrics over the cover with expand-to-full-lyrics action.
  - **Purpose:** Render synced lyrics overlay on the cover pager.
  - **Responsibilities:** ComposeView with SsilencceTheme + CoverLyricsScreen, expand navigation, lifecycle handling.
  - **Inputs:** LyricsViewModel, PlayerViewModel (activity-scoped).
  - **Outputs:** onExpandClick navigating to nav_lyrics or collapsing panel.
  - **Dependencies:** CoverLyricsScreen, LyricsViewModel, PlayerViewModel, SsilencceTheme, MainActivity.
  - **Consumed by:** CoverPagerFragment (child in fragment_player_album_cover.xml).
  - **Related files:** LyricsScreen (CoverLyricsScreen), LyricsFragment, CoverPagerFragment.
  - **Notes:** Compose disposed via DisposeOnViewTreeLifecycleDestroyed.


- `ui/screen/player/styles/defaultstyle/DefaultPlayerFragment.kt` — Default style player fragment (toolbar, cover, color scheme tinting, panel management).
  - **Purpose:** Root fragment for the Default now-playing style.
  - **Responsibilities:** toolbar setup, menu inflation, insets, tint targets, controls fragment lookup, queue item visibility.
  - **Inputs:** PlayerColorScheme, Preferences (DISPLAY_NEXT_SONG).
  - **Outputs:** getTintTargets, onQuickActionEvent (SoundSettings).
  - **Dependencies:** AbsPlayerFragment, DefaultPlayerControlsFragment, PlayerTintTarget, Preferences.
  - **Consumed by:** AbsSlidingMusicPanelActivity (NowPlayingScreen.Default).
  - **Related files:** fragment_default_player.xml, DefaultPlayerControlsFragment, AbsPlayerFragment.
  - **Notes:** Navigation icon opens sound settings; hides queue item when next song shown. Shared-preference callbacks return when `_binding` is null, preventing queued restore notifications from touching a destroyed view (ADR-047).


- `ui/screen/player/styles/defaultstyle/DefaultPlayerControlsFragment.kt` — Default style controls: slider, play/next/prev/repeat/shuffle buttons and animations.
  - **Purpose:** Provide playback controls for the Default style.
  - **Responsibilities:** FAB/buttons/slider binding, click handling, skip touch handlers, tint targets, queue info view, DefaultPlayerAnimator.
  - **Inputs:** PlayerViewModel actions, Preferences.
  - **Outputs:** Updated texts/icons, animated controls.
  - **Dependencies:** AbsPlayerControlsFragment, MusicSlider, SkipButtonTouchHandler, PlayerAnimator.
  - **Consumed by:** DefaultPlayerFragment (playbackControlsFragment).
  - **Related files:** DefaultPlayerFragment, PlayerAnimator, AbsPlayerControlsFragment.
  - **Notes:** queueInfo triggers NowPlayingAction.OpenPlayQueue.


- `ui/screen/player/styles/expressivestyle/ExpressivePlayerFragment.kt` — Expressive style fragment: blur background, toolbar, actions popup, color scheme, gestures.
  - **Purpose:** Root fragment for the Expressive now-playing style.
  - **Responsibilities:** Actions setup, repeat/shuffle buttons, flow collection, tint targets, marquee on show/hide, lyrics icon swap.
  - **Inputs:** PlayerViewModel flows (repeatModeFlow, shuffleModeFlow, currentSongFlow, extraInfoFlow).
  - **Outputs:** getTintTargets, onQuickActionEvent, ExtraInfoPreferenceDialog on long-press.
  - **Dependencies:** AbsPlayerFragment, ExpressivePlayerControlsFragment, ExtraInfoPreferenceDialog.
  - **Consumed by:** AbsSlidingMusicPanelActivity (NowPlayingScreen.Expressive).
  - **Related files:** fragment_expressive_player.xml, ExpressivePlayerControlsFragment, AbsPlayerFragment.
  - **Notes:** Menu items hidden in landscape; repeat icon ic_repeat_one.


- `ui/screen/player/styles/expressivestyle/ExpressivePlayerControlsFragment.kt` — Expressive controls with MorphicIconButton, slider and animations.
  - **Purpose:** Provide playback controls for the Expressive style.
  - **Responsibilities:** MusicSlider binding, MorphicIconButton morphing/rotation, skip buttons, tint targets, ExpressivePlayerAnimator slide-in.
  - **Inputs:** PlayerViewModel isPlaying, togglePlayPause.
  - **Outputs:** Morph state, animated controls.
  - **Dependencies:** AbsPlayerControlsFragment, MorphicIconButton, MusicSlider, PlayerAnimator.
  - **Consumed by:** ExpressivePlayerFragment.
  - **Related files:** ExpressivePlayerFragment, MorphicIconButton, avd_play/avd_pause.
  - **Notes:** onSongInfoChanged/onExtraInfoChanged are no-ops (parent handles).


- `ui/screen/player/styles/fullcoverstyle/FullCoverPlayerFragment.kt` — FullCover style: full-screen cover as background with dynamic tinting.
  - **Purpose:** Root fragment for the FullCover now-playing style.
  - **Responsibilities:** Next-song preview, close button, insets, tint targets (masks/labels/close), favorite delegation, DISPLAY_NEXT_SONG listener.
  - **Inputs:** PlayerViewModel.nextSongFlow, Preferences, scheme.
  - **Outputs:** onQuickActionEvent OpenPlayQueue, back navigation.
  - **Dependencies:** AbsPlayerFragment, FullCoverPlayerControlsFragment, Coil songImage.
  - **Consumed by:** AbsSlidingMusicPanelActivity (NowPlayingScreen.FullCover).
  - **Related files:** fragment_full_cover_player.xml, FullCoverPlayerControlsFragment, AbsPlayerFragment.
  - **Notes:** onPrepareViewGestures overridden as no-op.


- `ui/screen/player/styles/fullcoverstyle/FullCoverPlayerControlsFragment.kt` — Overlaid controls for FullCover with animations and actions popup.
  - **Purpose:** Provide overlaid playback controls for FullCover style.
  - **Responsibilities:** FAB, repeat/shuffle, slider, favorite/menu popup, navigation-bar insets, tint targets, FullCoverPlayerAnimator fade-in.
  - **Inputs:** PlayerViewModel actions, Preferences (DISPLAY_NEXT_SONG).
  - **Outputs:** setFavorite state, queue menu visibility.
  - **Dependencies:** AbsPlayerControlsFragment, MusicSlider, PopupMenu, PlayerAnimator.
  - **Consumed by:** FullCoverPlayerFragment.
  - **Related files:** FullCoverPlayerFragment, PlayerAnimator, AbsPlayerControlsFragment.
  - **Notes:** Queue menu hidden when next song displayed.


- `ui/screen/player/styles/gradientstyle/GradientPlayerFragment.kt` — Gradient style combining cover and color gradient derived from the scheme.
  - **Purpose:** Root fragment for the Gradient now-playing style.
  - **Responsibilities:** Bottom action buttons, insets, mask alpha on lyrics visibility, tint targets, favorite delegation.
  - **Inputs:** PlayerViewModel scheme, lyrics visibility AnimatorSet.
  - **Outputs:** getTintTargets, onQuickActionEvent.
  - **Dependencies:** AbsPlayerFragment, GradientPlayerControlsFragment, PlayerTintTarget.
  - **Consumed by:** AbsSlidingMusicPanelActivity (NowPlayingScreen.Gradient).
  - **Related files:** fragment_gradient_player.xml, GradientPlayerControlsFragment, AbsPlayerFragment.
  - **Notes:** Toolbar-less; menu removes queue/lyrics/sound/favorite items.


- `ui/screen/player/styles/gradientstyle/GradientPlayerControlsFragment.kt` — Gradient controls: slider, repeat/shuffle, actions popup.
  - **Purpose:** Provide playback controls for the Gradient style.
  - **Responsibilities:** Slider, repeat/shuffle, FAB, skip handlers, favorite/menu, landscape insets, tint targets.
  - **Inputs:** PlayerViewModel actions.
  - **Outputs:** Updated texts/icons.
  - **Dependencies:** AbsPlayerControlsFragment, MusicSlider, PopupMenu.
  - **Consumed by:** GradientPlayerFragment.
  - **Related files:** GradientPlayerFragment, AbsPlayerControlsFragment.
  - **Notes:** No custom PlayerAnimator (base default null).


- `ui/screen/player/styles/m3style/M3PlayerFragment.kt` — Material 3 style player with toolbar and Material tinting.
  - **Purpose:** Root fragment for the Material 3 now-playing style.
  - **Responsibilities:** Toolbar/back, action buttons (queue, lyrics, sleep timer, playlist, more), insets, tint targets, lyrics icon swap.
  - **Inputs:** PlayerViewModel scheme, lyrics visibility AnimatorSet.
  - **Outputs:** getTintTargets, onQuickActionEvent.
  - **Dependencies:** AbsPlayerFragment, M3PlayerControlsFragment, PlayerTintTarget.
  - **Consumed by:** AbsSlidingMusicPanelActivity (NowPlayingScreen.M3).
  - **Related files:** fragment_m3_player.xml, M3PlayerControlsFragment, AbsPlayerFragment.
  - **Notes:** Favorite state reflected in popup menu.


- `ui/screen/player/styles/m3style/M3PlayerControlsFragment.kt` — M3 controls with play/pause FAB, slider and animations.
  - **Purpose:** Provide playback controls for the M3 style.
  - **Responsibilities:** FAB with m3 icons, repeat/shuffle, slider, titles, skip handlers, tint targets, M3PlayerAnimator scale-in.
  - **Inputs:** PlayerViewModel actions.
  - **Outputs:** Updated texts/icons, animated controls.
  - **Dependencies:** AbsPlayerControlsFragment, MusicSlider, PlayerAnimator.
  - **Consumed by:** M3PlayerFragment.
  - **Related files:** M3PlayerFragment, PlayerAnimator, ic_play_m3_24dp.
  - **Notes:** Uses M3-specific play/pause drawables.


- `ui/screen/player/styles/peekplayerstyle/PeekPlayerFragment.kt` — Peek style: top progress bar, compact toolbar, scheme tinting.
  - **Purpose:** Root fragment for the Peek now-playing style.
  - **Responsibilities:** Toolbar/back, menu show-as-action, insets, title/extra-info flow collection, tint targets.
  - **Inputs:** PlayerViewModel.currentSongFlow, extraInfoFlow, scheme.
  - **Outputs:** getTintTargets.
  - **Dependencies:** AbsPlayerFragment, PeekPlayerControlsFragment, MaterialColors.
  - **Consumed by:** AbsSlidingMusicPanelActivity (NowPlayingScreen.Peek).
  - **Related files:** fragment_peek_player.xml, PeekPlayerControlsFragment, AbsPlayerFragment.
  - **Notes:** AppTheme mode surface uses colorSurfaceContainerLow.


- `ui/screen/player/styles/peekplayerstyle/PeekPlayerControlsFragment.kt` — Peek controls: slider, repeat/shuffle, play/pause and animations.
  - **Purpose:** Provide playback controls for the Peek style.
  - **Responsibilities:** Play/pause, repeat/shuffle, slider, skip handlers, tint targets, PeekPlayerAnimator staggered scale-in.
  - **Inputs:** PlayerViewModel actions.
  - **Outputs:** Updated icons.
  - **Dependencies:** AbsPlayerControlsFragment, MusicSlider, PlayerAnimator.
  - **Consumed by:** PeekPlayerFragment.
  - **Related files:** PeekPlayerFragment, PlayerAnimator, AbsPlayerControlsFragment.
  - **Notes:** No title views; onSongInfoChanged no-op.


- `ui/screen/player/styles/plainstyle/PlainPlayerFragment.kt` — Plain style with simple cover, toolbar and color scheme.
  - **Purpose:** Root fragment for the Plain now-playing style.
  - **Responsibilities:** Toolbar/back, always-visible menu actions, insets, title/artist flow collection, tint targets.
  - **Inputs:** PlayerViewModel.currentSongFlow, scheme.
  - **Outputs:** getTintTargets.
  - **Dependencies:** AbsPlayerFragment, PlainPlayerControlsFragment, PlayerTintTarget.
  - **Consumed by:** AbsSlidingMusicPanelActivity (NowPlayingScreen.Plain).
  - **Related files:** fragment_plain_player.xml, PlainPlayerControlsFragment, AbsPlayerFragment.
  - **Notes:** Toolbar navigation wired only when icon present.


- `ui/screen/player/styles/plainstyle/PlainPlayerControlsFragment.kt` — Plain controls with play/pause FAB, slider and animations.
  - **Purpose:** Provide playback controls for the Plain style.
  - **Responsibilities:** FAB, repeat/shuffle, slider, skip handlers, extra-info view, tint targets, PlainPlayerAnimator spin+scale.
  - **Inputs:** PlayerViewModel actions.
  - **Outputs:** Updated texts/icons, animated controls.
  - **Dependencies:** AbsPlayerControlsFragment, MusicSlider, PlayerAnimator.
  - **Consumed by:** PlainPlayerFragment.
  - **Related files:** PlainPlayerFragment, PlayerAnimator, AbsPlayerControlsFragment.
  - **Notes:** Emphasis color follows VibrantColor scheme mode.

- `ui/screen/queue/QueueFragment.kt` — Retained queue dialog and Aurora full-screen queue host with shared song actions.
  - **Purpose:** Host the same real queue in the appearance-appropriate container.
  - **Responsibilities:** QueueFragment expands/auto-dismisses its dialog; AuroraQueueFragment remains visible when empty; both render QueueScreen and share playlist/delete/share/details routing.
  - **Inputs:** PlayerViewModel queueFlow.
  - **Outputs:** Queue UI; dialog/navigation intents.
  - **Dependencies:** PlayerViewModel, AddToPlaylistDialog, DeleteSongsDialog, getShareSongIntent, songDetailArgs.
  - **Consumed by:** Main navigation graph and mini-player/player queue actions.
  - **Related files:** QueueScreen.kt, other/MiniPlayerFragment.kt, MainActivity.kt.
  - **Notes:** MainActivity replaces only Aurora's nav_queue destination with the full-screen Fragment; non-Aurora retains the dialog. The Aurora host renders QueueScreen's header, so the queue selector opens the queues-manager dialog from the same surface.


- `ui/screen/queue/QueuesFragment.kt` — Configurable top-level Queues library category browser outside Aurora.
  - **Purpose:** Browse the active playing queue and the saved named queues from a library category.
  - **Responsibilities:** Compose list with a playing-queue row that opens the existing queue dialog, saved-queue summaries with counts and the active marker, save-current-queue dialog, rename dialog, delete confirmation, and direct switch; disables saved rows while an async switch runs and opens `nav_queue` only on success; observes `NamedQueueMessage` for toast feedback; applies status-bar/insets and bottom-navigation margins.
  - **Inputs:** PlayerViewModel `queueFlow`, `namedQueuesFlow`, `activeNamedQueueIdFlow`, `namedQueueMessage`.
  - **Outputs:** Navigation to `nav_queue`; `saveCurrentQueueAs`, `renameNamedQueue`, `deleteNamedQueue`, `switchToNamedQueueWithResult`.
  - **Dependencies:** AbsMainActivityFragment, PlayerViewModel, NamedQueueSummary, ObserveAsEvent, SsilencceTheme.
  - **Consumed by:** `graph_main` `nav_queues`, MainActivity top-level routing, CategoryInfo.Queues.
  - **Related files:** QueueScreen.kt, QueueFragment.kt, PlayerViewModel.kt, CategoryInfo.kt, res/navigation/graph_main.xml, AuroraNavigation.kt.
  - **Notes:** The category is appended last and defaults invisible through the existing Preferences.libraryCategories merge. Duplicate save/rename names use the repository keep-both/suffix policy (`QueueNamePolicy.uniqueName`) and report the final name (`named_queue_saved_as`/`named_queue_renamed_as`); blank/over-long/limit errors surface as toasts. The richer Replace/Rename chooser remains in `QueueScreen`. The fragment is top-level outside Aurora only; `AuroraNavigation` filters Queues because Aurora already has a fixed Queue destination, and `AuroraStartDestinationPolicy` rejects `nav_queues` as an Aurora start/last-page target. No DAO, repository, schema or dependency was added.

- `ui/screen/queue/QueueScreen.kt` - Compose real-queue UI with named-queue manager, merge/eviction actions, queue-local search, fourth repeat state and key-based multi-select block drag.
  - **Purpose:** Interactive queue list with playback control, persistent named queues and filtering.
  - **Responsibilities:** Reorderable drag list, shuffle/repeat/lock toggles, up-next summary, locate-current button, per-item menu, stop-after events, auto-scroll to current; guarded right-swipe removal with Snackbar Undo; ended-queue footer with Replay and explicit full Clear; overflow-menu re-shuffle item hidden for an empty queue and disabled while shuffle is off; key-based multi-select mode with long-press/tap, count/select-all/cancel, selected-row handles and effective target highlighting; Aurora header with queue selector, play/pause state, in-queue badge, save control, named-queue rename/delete/remove-others; stable-order named-queue manager with direct switch plus First/Previous/Next controls when an active queue exists; named-queue duplicate choice (Keep both/Replace/Rename) and name-length/count feedback; queue-local search filtering by title/artist/album; one scrolling Aurora surface for header, songs, empty state and footer.
  - **Inputs:** PlayerViewModel flows (queue, named queues, active id, isQueueEndedFlow, `NamedQueueMessage`), SharedPreferences LOCKED_QUEUE, queue query.
  - **Outputs:** PlayerViewModel commands (playSongAt, moveSong, moveQueueBlock, removePosition/restoreRemovedPosition, replayQueue, reshuffleQueue, stopAt, clearQueue with optional behavior, saveCurrentQueueAs with optional replace id, switchToNamedQueue, first/previous/next named queue, rename/delete named queues with optional replace id).
  - **Dependencies:** PlayerViewModel, NamedQueueSummary, QueueNamePolicy, QueueReshufflePolicy, QueueBatchMovePolicy, QueueClearingBehavior, reorderable (sh.calvin), LazyColumnScrollbar, AnimatedEqBars, MediaImage.
  - **Consumed by:** QueueFragment, AuroraQueueFragment.
  - **Related files:** QueueFragment.kt, PlayerViewModel.kt, NamedQueueRepository.kt, QueueNamePolicy.kt.
  - **Notes:** QUEUE_DEBOUNCE delays reorder commits; stop-after toasts observed. Aurora translates list indices past the header and rejects header/footer reorder targets. Queue-local search filters indexed `(originalIndex, song)` pairs, so play/remove/stop/move actions keep the original queue index. Swipe is enabled only when unlocked, query blank and no reorder is active; it removes by original index, only one removal is pending until the Undo Snackbar resolves, and Undo inserts through `restoreRemovedPosition`. Duplicate Save/Rename never touches the active playback queue; Save Replace overwrites a named snapshot only; Rename Replace deletes the selected duplicate and renames the current queue. The manager list and First/Previous/Next chain share the stable createdAt/id order; chain controls appear only with an active queue, the action verifies exact active-snapshot identity, and direct row switching remains. The re-shuffle item appears in both overflow menus only, is hidden for an empty real queue, disabled while shuffle is off, and reports through `queueMessage`; it is absent from the end footer and multi-drag work. Multi-select is keyed by `QueueSong.key`; long-press enters, tap toggles, one row is an intermediate state and drag requires two. Selected effective positions and the drop target are mapped through `QueuePosition`; the selection bar is a LazyColumn item included in `queueContentOffset`, selected rows alone get enabled handles, drop-down inserts after/drop-up before the target, and drops inside the selected effective range are no-ops. Search/lock clear selection; swipe and single drag remain outside selection. Named-queue save/binding ordering is coordinated by PlayerViewModel and PlaybackService (ADR-041).


- `ui/screen/scrobbling/ScrobblingServiceLoginFragment.kt` — Compose bottom sheet for scrobbling login (Last.fm/Libre.fm): user/token or API key form.
  - **Purpose:** Authenticate Last.fm/ListenBrainz scrobbling accounts.
  - **Responsibilities:** LastFmForm (username/password), ListenBrainzForm (token), LoginState crossfade, logout, connected user card.
  - **Inputs:** ScrobblingService extra, LibraryViewModel login state.
  - **Outputs:** logInToService/logoutFromService calls.
  - **Dependencies:** LibraryViewModel, LoginParams, LoginState, BottomSheetDialogSurface.
  - **Consumed by:** PreferencesScreenFragment (LASTFM_LOGIN/LISTENBRAINZ_LOGIN).
  - **Related files:** PreferencesScreenFragment.kt, LibraryViewModel.kt.
  - **Notes:** Logs out failed logins on dispose.


- `ui/screen/settings/SettingsFragment.kt` - Settings container with nested NavHostFragment, toolbar, destination listener and settings search entry point.
  - **Purpose:** Host settings categories in nested navigation.
  - **Responsibilities:** Toolbar title per destination, back-press handling (dismiss Aurora overlays, collapse panel, pop child stack), child NavController listener, inflate `menu_settings` search action, open the Compose settings-search sheet, navigate to the selected SettingsScreen and pop to root when needed.
  - **Inputs:** Layout fragment_settings, child NavHostFragment.
  - **Outputs:** Title updates, navigation pops, SettingsSearchDialog launch/navigation result.
  - **Dependencies:** AbsMainActivityFragment, NavController.OnDestinationChangedListener, BottomSheetBehavior, SettingsSearchDialog, menu_settings.
  - **Consumed by:** Main navigation graph.
  - **Related files:** SettingsScreen.kt, PreferencesScreenFragment.kt, SettingsSearchDialog.kt, SettingsSearchIndex.kt.
  - **Notes:** onBackPressedCallback removed when child stack empty; search scroll target is passed to PreferencesScreenFragment (ADR-044).


- `ui/screen/settings/SettingsScreen.kt` - Enum mapping each settings category to its XML preference layout, navigation action and display title.
  - **Purpose:** Declarative mapping of settings categories.
  - **Responsibilities:** Enumerate Appearance/NowPlaying/Lyrics/Playback/Library/Network/Advanced with layoutRes, navAction and titleRes.
  - **Inputs:** None.
  - **Outputs:** Lookup used by PreferencesScreenFragment navigation and SettingsSearchDialog grouping.
  - **Dependencies:** R.xml.preferences_screen_*, navigation actions, title strings.
  - **Consumed by:** PreferencesScreenFragment (toEnum), SettingsSearchIndex/Dialog, App.kt.
  - **Related files:** PreferencesScreenFragment.kt, SettingsSearchIndex.kt, App.kt.
  - **Notes:** Pure declaration, no logic; titleRes supplies grouped search headers (ADR-044).


- `ui/screen/settings/PreferencesScreenFragment.kt` - PreferenceFragmentCompat with all preference screens, change handlers, audio-focus mode, scheduled-backup row and search scroll targets.
  - **Purpose:** Render and handle all XML preference screens.
  - **Responsibilities:** Bind change listeners (theme, language, font, backup/restore, update search, audio-focus mode), custom dialogs via onDisplayPreferenceDialog, cache clearing, summaries; validate the minimum/maximum song-duration pair in LibraryPreferencesFragment and reject values that would make min > max; handle ARG_SCROLL_TO_PREFERENCE; host the ScheduledBackupPreference entry; after a successful manual backup, stage the SAF archive and show the Compose share sheet, falling back to a saved-but-not-shareable toast when staging is unavailable and the success toast when the sheet cannot be shown. Route HEADSET_BUTTON_MAPPING to the Compose mapping dialog and refresh its summary on open/change. Route SORTING_IGNORE_KEYWORDS to the Compose keyword dialog and refresh its count summary.
  - **Inputs:** SharedPreferences, LibraryViewModel, LyricsViewModel, UpdateViewModel, fragment arguments, manual SAF backup Uri.
  - **Outputs:** Preference changes, BackupHelper create/restore, BackupShareStaging/BackupShareDialogFragment interactions, ScheduledBackupController/Preference interactions, ScrobblingServiceLoginFragment, font import, preference scroll.
  - **Dependencies:** ThemePreference, ProgressIndicatorPreference, SwitchWithButtonPreference, BackupHelper, BackupCreationResult, BackupShareStaging, BackupShareDialogFragment, CoverProvider, AppCompatDelegate, AudioFocusMode, ScheduledBackup. HeadsetButtonMappingDialog, HeadsetButtonAction.titleRes, IgnoreKeywordsPreferenceDialog.
  - **Consumed by:** SettingsFragment nested graph.
  - **Related files:** SettingsFragment.kt, SettingsScreen.kt, util/BackupHelper.kt, util/backup/BackupShareStaging.kt, ui/screen/settings/backup/BackupShareDialogFragment.kt, ui/screen/settings/backup/, res/xml/provider_paths.xml. HeadsetButtonMappingDialog.kt, res/xml/preferences_screen_playback.xml, IgnoreKeywordsPreferenceDialog.kt, res/xml/preferences_screen_library.xml.
  - **Notes:** Seven subclasses per category screen; DynamicColors applied for Material You. Aurora switches recreate the activity, saved theme selectors are disabled, and list clearance comes from LibraryMargin rather than a fixed navigation height. The audio-focus preference maps to AudioFocusMode and clears the legacy IGNORE_AUDIO_FOCUS flag; scheduled backups are opened from Advanced settings (ADR-042, ADR-043, ADR-044). A settings restore recreates the activity only after the live SharedPreferences commit succeeds; the fragment does not substitute activity recreation for updating the process-wide cache. Its change listener returns early when the fragment is not added or its view is gone, so queued restore notifications cannot call `requireActivity()` after teardown (ADR-047). The manual backup callback calls `createBackupFile`, then `BackupShareStaging.stageFromSaf`; a staging failure is not a backup failure. The share sheet is shown only while the fragment is added, its view exists and the child FragmentManager is not state-saved; otherwise the success toast is shown. The headset summary resolves the four action title resources; settings search indexes the new row automatically.


- `ui/screen/sleeptimer/SleepTimerFragment.kt` — Expanded BottomSheetDialogFragment showing SleepTimerBottomSheet with SleepTimerViewModel.
  - **Purpose:** Container for sleep timer bottom sheet.
  - **Responsibilities:** Force expanded state, ComposeView with SleepTimerBottomSheet, Koin viewModel scoping.
  - **Inputs:** SleepTimerViewModel.
  - **Outputs:** SleepTimerBottomSheet composition.
  - **Dependencies:** SleepTimerViewModel, SsilencceTheme, BottomSheetBehavior.
  - **Consumed by:** Player UI sleep timer action.
  - **Related files:** SleepTimerScreen.kt, SleepTimerViewModel.kt.
  - **Notes:** Fragment-scoped viewModel.


- `ui/screen/sleeptimer/SleepTimerScreen.kt` - Compose sleep timer bottom sheet: duration/end-at modes, options, countdown, start/stop.
  - **Purpose:** Configure and control countdown or exact end-time sleep timers.
  - **Responsibilities:** Duration/End at mode chips, 24-hour Compose TimeInput dialog, persisted/default time display, duration slider/presets, finish-music/fade-out switches, animated start/cancel button, disabled controls and `Ends at` countdown while running.
  - **Inputs:** SleepTimerViewModel uiState/events.
  - **Outputs:** setTimerState, startTimer(mode, hour, minute), cancelTimer calls.
  - **Dependencies:** SleepTimerViewModel, SleepTimerMode, SleepTimerEndTime, Material3 TimeInput/FilterChip, haptic feedback.
  - **Consumed by:** SleepTimerFragment.
  - **Related files:** SleepTimerFragment.kt, SleepTimerViewModel.kt, playback/SleepTimerEndTime.kt.
  - **Notes:** Haptics on events; countdown text from state; End at controls remain visible but disabled while running.


- `ui/screen/sleeptimer/SleepTimerViewModel.kt` - Combines SleepTimer state with DataStore preferences and emits events.
  - **Purpose:** Bridge SleepTimer service state with persisted duration/end-at preferences.
  - **Responsibilities:** Combine isRunning/waitingFor with sleep_timer DataStore, default next full hour, resolve end-at delay, persist confirmed mode/hour/minute, start/cancel with exact-alarm permission fallback, timer updater lifecycle.
  - **Inputs:** Application, SleepTimer, SleepTimerMode, end-at hour/minute.
  - **Outputs:** SleepTimerUiState StateFlow, SleepTimerEvent channel, scheduled delay.
  - **Dependencies:** SleepTimer, SleepTimerEndTime, DataStore preferencesDataStore("sleep_timer").
  - **Consumed by:** SleepTimerScreen, SleepTimerFragment.
  - **Related files:** SleepTimerScreen.kt, playback/SleepTimer.kt, playback/SleepTimerEndTime.kt.
  - **Notes:** timer_mode/end_at_hour/end_at_minute are committed only after sleepTimer.set succeeds; duration slider/preset persistence remains unchanged. createTimerUpdater/cancelTimerUpdater managed in init/onCleared.


- `ui/screen/sound/SoundSettingsFragment.kt` — Compose BottomSheetDialogFragment opening SoundSettingsScreen with EqualizerViewModel.
  - **Purpose:** Container for sound settings bottom sheet.
  - **Responsibilities:** Expanded sheet in landscape, ComposeView with SoundSettingsSheet, EqualizerViewModel injection.
  - **Inputs:** EqualizerViewModel.
  - **Outputs:** SoundSettingsSheet composition.
  - **Dependencies:** EqualizerViewModel, BottomSheetDialogSurface.
  - **Consumed by:** Player sound settings action.
  - **Related files:** SoundSettingsScreen.kt, EqualizerViewModel.kt.
  - **Notes:** DisposeOnViewTreeLifecycleDestroyed composition strategy.


- `ui/screen/sound/SoundSettingsScreen.kt` - Compose sound settings bottom sheet: balance, tempo and volume, connected to EQ state.
  - **Purpose:** Quick volume/balance/tempo/advanced audio controls.
  - **Responsibilities:** Volume slider with iconified track, centered balance slider, speed/pitch sliders with presets, audio device card, offload/float/skip-silence switches.
  - **Inputs:** EqualizerViewModel state flows, AudioDevice, BitPerfectState.
  - **Outputs:** EqualizerViewModel setters (setVolume, setBalance, setTempo, showOutputDeviceSelector).
  - **Dependencies:** EqualizerViewModel, IconifiedSliderTrack, ShapedText, TitledCard.
  - **Consumed by:** SoundSettingsFragment.
  - **Related files:** SoundSettingsFragment.kt, EqualizerViewModel.kt, core/model/audiodevice.
  - **Notes:** Audio effects gated when offload/bit-perfect/float active; bit-perfect UI temporarily disabled.


- `ui/screen/replaygain/ReplayGainScanActivity.kt` — Compose ReplayGain calculation activity.
  - **Purpose:** Confirm, run, cancel and report an offline ReplayGain scan.
  - **Responsibilities:** Load selected song IDs, request read/write access, request MediaStore write grant on Android 11+, render confirmation/progress/result including each failed title/reason, keep screen on, cancel on finish.
  - **Inputs:** EXTRA_SONG_IDS long array, ReplayGainScanViewModel state.
  - **Outputs:** Scan start/cancel; finished/error UI with per-file failure details.
  - **Dependencies:** AbsBaseActivity, Compose Material3, MediaStore, Koin ViewModel, ReplayGainScanViewModel.
  - **Consumed by:** MenuItemClickExt (song/album/artist selection).
  - **Related files:** ReplayGainScanViewModel.kt, MenuItemClickExt.kt, MainModule.kt.
  - **Notes:** Write grant is scoped to the selected URIs; reads use READ_MEDIA_AUDIO/READ_EXTERNAL_STORAGE and legacy write below R.

- `ui/screen/replaygain/ReplayGainScanViewModel.kt` — State holder for the ReplayGain scan flow.
  - **Purpose:** Load selected songs and expose scan progress/result without blocking the UI.
  - **Responsibilities:** Resolve song IDs; prepare Ready state; run ReplayGainScanner; publish Loading/Ready/Running/Finished/Empty/Error; cancel and clear jobs.
  - **Inputs:** Repository, ReplayGainScanner, selected IDs.
  - **Outputs:** StateFlow<ReplayGainScanUiState>.
  - **Dependencies:** Repository, ReplayGainScanner, kotlinx.coroutines.
  - **Consumed by:** ReplayGainScanActivity.
  - **Related files:** ReplayGainScanActivity.kt, ReplayGainScanner.kt.
  - **Notes:** Scan remains activity-scoped in this minimal package; process death cancels it. A forced cancellation preserves the last completed/failed counts and reports cancelled with an empty failure-detail list.

- `ui/screen/tageditor/SongTagEditorActivity.kt` — Song tag editor activity with all fields (title, artist, year, track, lyrics...), cover and MusicBrainz search dialog.
  - **Purpose:** Edit full song metadata tags.
  - **Responsibilities:** Bind TagEditorResult to fields, genre selector, MusicBrainz autofill search, Deezer cover download, propertyMap for saving.
  - **Inputs:** EditTarget extra, TagEditorViewModel.
  - **Outputs:** TagEditorResult-driven fields; save via propertyMap.
  - **Dependencies:** AbsTagEditorActivity, TagEditorViewModel, MusicBrainzRecording, Deezer service, MetadataReader keys.
  - **Consumed by:** MenuItemClickExt, AbsPlayerFragment, SongDetailFragment.
  - **Related files:** TagEditorViewModel.kt, AbsTagEditorActivity.kt, ui/component/menu/MenuItemClickExt.kt.
  - **Notes:** MusicBrainz recordings dialog maps credits/releases into tags.


- `ui/screen/tageditor/AlbumTagEditorActivity.kt` — Album tag editor (via AbsTagEditorActivity): aggregates common album values across every album song, shows Varies, writes only dirty fields and confirms destructive totals/disc writes.
  - **Purpose:** Edit album-level tags without treating the first song as the source of truth.
  - **Responsibilities:** LegacyTagAggregator aggregation, album fields binding, Varies hints, default genre, Deezer album cover download, web search, dirty propertyMap, destructive-field confirmation.
  - **Inputs:** EditTarget extra, TagEditorViewModel.
  - **Outputs:** Aggregated field values; save via a dirty-only propertyMap.
  - **Dependencies:** AbsTagEditorActivity, TagEditorViewModel, LegacyTagAggregator, TagEditField, MetadataReader keys.
  - **Consumed by:** MenuItemClickExt (album edit).
  - **Related files:** LegacyTagAggregator.kt, TagEditorViewModel.kt, AbsTagEditorActivity.kt.
  - **Notes:** Still entity-wide (one dirty map broadcast); not routed to the batch editor.


- `ui/screen/tageditor/ArtistTagEditorActivity.kt` — Artist tag editor with artist image (Coil), common-value aggregation, Varies hints and dirty-only writes.
  - **Purpose:** Edit artist-level tags without treating the first song as the source of truth, plus the app-local artist image.
  - **Responsibilities:** LegacyTagAggregator aggregation, Varies hints, artist image picker/search/reset via CustomArtistImageManager, Coil loading with disabled cache, dirty propertyMap, destructive DISC_TOTAL confirmation, no conditional ARTIST clear.
  - **Inputs:** EditTarget extra, TagEditorViewModel.
  - **Outputs:** Aggregated field values; artist image updates; save via a dirty-only propertyMap.
  - **Dependencies:** AbsTagEditorActivity, TagEditorViewModel, LegacyTagAggregator, TagEditField, SingletonImageLoader, CustomArtistImageManager.
  - **Consumed by:** MenuItemClickExt (artist edit).
  - **Related files:** LegacyTagAggregator.kt, TagEditorViewModel.kt, AbsTagEditorActivity.kt.
  - **Notes:** Still entity-wide (one dirty map broadcast); routing and per-file values remain open.

- `ui/screen/tageditor/LegacyTagAggregator.kt` — Read-only common-value aggregation for the legacy album/artist editors.
  - **Purpose:** Prevent first-file-as-source-of-truth in entity-level tag editors.
  - **Responsibilities:** Read every selected URI with `MetadataReader(uri, readPictures = false)`, compute common values and `Varies`, count readable/total files and cooperatively cancel per file.
  - **Inputs:** `List<Uri>` and `List<TagEditField>`.
  - **Outputs:** `AggregatedTagValues(common, varies, readableCount, totalCount)`.
  - **Dependencies:** MetadataReader, TagEditField, Kotlin coroutines.
  - **Consumed by:** AlbumTagEditorActivity, ArtistTagEditorActivity.
  - **Related files:** TagEditField.kt, TagEditorViewModel.kt.
  - **Notes:** Read-only; no cover reads; unreadable files are excluded from the aggregate.


- `ui/screen/tageditor/TagEditorResult.kt` — TagEditorResult (editable fields) and SaveTagsResult (scan/failure counters).
  - **Purpose:** Data holders for tag editing.
  - **Responsibilities:** TagEditorResult nullable metadata fields; SaveTagsResult loading/success/scanned/failed.
  - **Inputs:** Metadata strings.
  - **Outputs:** State consumed by TagEditorViewModel and editor activities.
  - **Dependencies:** None.
  - **Consumed by:** TagEditorViewModel, Song/Album/ArtistTagEditorActivity.
  - **Related files:** TagEditorViewModel.kt.
  - **Notes:** Mirror of MetadataReader keys.


- `ui/screen/tageditor/TagEditorViewModel.kt` — Reads/writes metadata with MetadataWriter (TagLib), handles covers, saves via MediaStoreWriter/repository.
  - **Purpose:** Orchestrate metadata read/write and cover handling.
  - **Responsibilities:** loadContent/loadArtwork via MetadataReader, write via MetadataWriter, artist image set/reset, Deezer album/track, MusicBrainz tag mapping.
  - **Inputs:** Repository, CustomArtistImageManager, EditTarget.
  - **Outputs:** LiveData TagEditorResult/artwork/SaveTagsResult.
  - **Dependencies:** MetadataWriter, MetadataReader, CustomArtistImageManager, Repository (deezer/musicbrainz).
  - **Consumed by:** SongTagEditorActivity, AlbumTagEditorActivity, ArtistTagEditorActivity.
  - **Related files:** TagEditorResult.kt, data/local/MetadataWriter.kt, AbsTagEditorActivity.kt.
  - **Notes:** uris derived from target.contents. Legacy writes go through `MetadataWriter.write()`, whose v2 result typing and shared scan accounting make save counters truthful; no app build or runtime PASS.


- `ui/screen/tageditor/BatchTagEditorActivity.kt` — Compose host for the multi-song batch tag editor.
  - **Purpose:** Launch the batch editor from selected song IDs and gate writes behind read/write permissions.
  - **Responsibilities:** Resolve `EXTRA_SONG_IDS`; request read permissions; request `MediaStore.createWriteRequest` on R+ only on save; cancel preparation on back; block back while writing; reset the permission flag; host `BatchTagEditorScreen` with per-file/edit/stop callbacks.
  - **Inputs:** Intent with distinct song IDs, Koin BatchTagEditorViewModel.
  - **Outputs:** Compose UI and write-request results.
  - **Dependencies:** AbsBaseActivity, SsilencceTheme, BatchTagEditorViewModel/Screen, MediaStore.
  - **Consumed by:** MenuItemClickExt (`List<Song>.onSongsMenu`).
  - **Related files:** BatchTagEditorViewModel.kt, BatchTagEditorScreen.kt, MainModule.kt, AndroidManifest.xml.
  - **Notes:** No new permission; stop takes effect at the next file boundary; back is blocked while writing; process death remains a residual.

- `ui/screen/tageditor/BatchTagEditorViewModel.kt` — State holder for multi-song tag aggregation and batch writes.
  - **Purpose:** Read every selected file, aggregate Common/Varies/Absent, hold Keep/Set/Clear intent, and publish progress/report.
  - **Responsibilities:** Resolve IDs through Repository.songById; enforce MAX_BATCH_SONGS; read all readable files with MetadataReader; report missing/unreadable songs; build BatchTagEditPlan with sparse per-song overrides; run prepare/write jobs; expose cancelPrepare/stopWriting; call MetadataWriter.writeBatch with cooperative stop; append skipped/cancelled reports.
  - **Inputs:** Repository, selected song IDs.
  - **Outputs:** StateFlow<BatchTagEditorUiState> (Loading/Ready/Writing/Result/Error/Empty).
  - **Dependencies:** Repository, MetadataReader, MetadataWriter, TagEditPlan/TagEditReport, coroutines.
  - **Consumed by:** BatchTagEditorActivity/Screen.
  - **Related files:** BatchTagEditorScreen.kt, BatchTagEditorActivity.kt, TagEditField.kt, TagEditPlan.kt.
  - **Notes:** Never reads only the first file; each field uses the same MetadataReader.merge value shown in the UI. Limit 500 songs. Per-song Keep overrides a common Set; stop takes effect at the next file boundary and remaining files are reported cancelled.

- `ui/screen/tageditor/BatchTagEditorScreen.kt` — Compose field editor and per-file/per-field save report.
  - **Purpose:** Present common/Varies values, explicit Keep/Set/Clear controls, progress and read-back failures.
  - **Responsibilities:** Render one row per TagEditField; Set prefill; block blank Set; per-file override dialog; skipped-file disclosure; save summary; stop action; result cards with file/field status; no cover or autofill controls.
  - **Inputs:** BatchTagEditorUiState and callbacks.
  - **Outputs:** Field/per-file operation changes, save/stop requests, close.
  - **Dependencies:** Compose Material3, TagEditField/TagEditReport, BatchTagEditorViewModel state.
  - **Consumed by:** BatchTagEditorActivity.
  - **Related files:** BatchTagEditorActivity.kt, BatchTagEditorViewModel.kt, res/values/strings.xml.
  - **Notes:** Keep is the default; Clear is the only removal operation; a per-file Keep can override a common Set; no blanket success when a requested field fails.


- `ui/screen/update/UpdateDialog.kt` — Update dialog showing the GitHub release with markdown changelog, links and ignore option.
  - **Purpose:** Present available update to user.
  - **Responsibilities:** Check isNewer, render release body as markdown, info/download actions, ignore on cancel.
  - **Inputs:** UpdateViewModel latestRelease, GitHubRelease.
  - **Outputs:** BottomSheet/alert dialog; downloadUpdate call.
  - **Dependencies:** UpdateViewModel, GitHubRelease, setMarkdownText, DialogUpdateInfoBinding.
  - **Consumed by:** MainActivity (UPDATE_FOUND dialog).
  - **Related files:** MainActivity.kt, UpdateViewModel.kt, UpdateSearchResult.kt.
  - **Notes:** Falls back to "up to date" alert.


- `ui/screen/update/UpdateSearchResult.kt` — UpdateSearchResult states (Idle/Searching/Completed/Failed) and shouldStartNewSearchFor logic.
  - **Purpose:** Model update-check state machine.
  - **Responsibilities:** State enum, executedAtMillis/wasFromUser/wasExperimentalQuery, dedupe logic.
  - **Inputs:** GitHubRelease.
  - **Outputs:** State consumed by UpdateViewModel.
  - **Dependencies:** GitHubRelease.
  - **Consumed by:** UpdateViewModel, PreferencesScreenFragment.
  - **Related files:** UpdateViewModel.kt, PreferencesScreenFragment.kt.
  - **Notes:** shouldStartNewSearchFor prevents redundant searches.


- `ui/screen/update/UpdateViewModel.kt` — Queries GitHubService.latestRelease, records search timestamp and publishes result/download events.
  - **Purpose:** Drive update checks and APK downloads.
  - **Responsibilities:** searchForUpdate with experimental flag, Preferences.lastUpdateSearch/lastUpdateId persistence, DownloadManager enqueue/remove.
  - **Inputs:** GitHubService.
  - **Outputs:** Event-wrapped UpdateSearchResult; download requests.
  - **Dependencies:** GitHubService, GitHubRelease.getDownloadRequest, Event, Preferences.
  - **Consumed by:** MainActivity, UpdateDialog, PreferencesScreenFragment.
  - **Related files:** UpdateSearchResult.kt, UpdateDialog.kt, MainActivity.kt.
  - **Notes:** Registered in MainModule with GitHubService.


- `ui/screen/player/aurora/AuroraPlayerFragment.kt` — Optional Compose chrome host using inherited player behavior.
  - **Purpose:** Adapt the saved native player style without replacing playback/cover/action logic.
  - **Responsibilities:** Attach the existing CoverPagerFragment through AndroidView, view-owned composition, favorite/lyrics/panel state, inherited menus/dialogs, style-aware root sizing, and the `onOverflowItem(id)` callback: the action-row More surfaces are routed by inflating `menu_now_playing` in a non-shown `PopupMenu` container and dispatching through the inherited `onMenuItemClick` (same action set, no popup window).
  - **Inputs:** Saved NowPlayingScreen, PlayerViewModel and inherited cover/favorite callbacks.
  - **Outputs:** AuroraPlayerScreen composition; existing navigation/playback actions.
  - **Dependencies:** AbsPlayerFragment, FragmentRibbonPlayerBinding, SsilencceTheme, MainActivity.
  - **Consumed by:** AbsSlidingMusicPanelActivity when Aurora is selected.
  - **Related files:** `ui/screen/player/aurora/AuroraPlayerScreen.kt`, `ui/screen/player/cover/CoverPagerFragment.kt`.
  - **Notes:** White accents do not erase alternative player-color preferences; Peek remains wrap-content.


- `ui/screen/player/aurora/AuroraPlayerScreen.kt` — Style-aware Compose metadata, actions, progress and transport.
  - **Purpose:** Reference-based chrome for all seven saved player variants.
  - **Responsibilities:** Native cover interoperability, full-cover/gradient/plain/M3/expressive/compact-Peek anatomy, lifecycle-collected playback state, seeking/remaining time, hold-to-seek, configured flings, reactive extra-info/album settings, gated marquee and accessible controls; a square cover centered in its stage (bounded by the available space, 360 dp normally and 280 dp compact) with a smooth pre-blurred cached bitmap halo (bounded raster cache, one drawImage per frame, no per-frame blur) matching the reference cover shadows, FullCover keeping its glass controls and reference backdrop behind header, cover and controls; the action-row More opens an anchored Material3 `DropdownMenu` whose items route by id through `onOverflowItem` (same action set).
  - **Inputs:** PlayerViewModel, cover View, favorite/lyrics/visibility state, shell navigation height and inherited action callbacks.
  - **Outputs:** Real playback commands, cover gestures and action/dialog callbacks.
  - **Dependencies:** Compose Foundation/Material 3, AndroidView, MusicSlider, MorphicIconButton, shared motion gates, Preferences.
  - **Consumed by:** AuroraPlayerFragment.
  - **Related files:** `ui/component/compose/aurora/AuroraRibbon.kt`, `ui/screen/player/PlayerViewModel.kt`.
  - **Notes:** Progress collection is isolated and stops while collapsed; expressive rotation, squiggly and marquee motion honor lifecycle/system/power/user gates. Expressive has a Compose-owned accessible play action and switches to correct static play/pause icons when motion is disallowed; transport-edge sound/EQ actions reuse native routing. Platform safe-drawing insets protect landscape controls. Time and long-press controls expose remaining/total state and long-press labels.


### res/ (resources)

- `res/drawable/` — ~180 XML drawables: Material icons (ic_*), logos, selectors, shadows, animated vector drawables (play/pause/favorite), squiggly thumb, Cast/screencast/crossfade/randomize/repeat-pause/bookmark/copy/pin/battery icons.
  - **Purpose:** Static and animated vector drawable assets for the whole UI.
  - **Responsibilities:** Material icons, player AVDs (play/pause/favorite), selectors, shadows, squiggly seekbar thumb.
  - **Inputs:** Referenced from layouts, menus, code via R.drawable.
  - **Outputs:** Rendered icons/animations across app, widgets and notifications.
  - **Dependencies:** AAPT2 vector resources, referenced drawables from code (nowPlayingButtonStyle, MorphicIconButton).
  - **Consumed by:** All layouts/menus, player styles, widget Glance composables, AdapterExt, NowPlayingAction.
  - **Related files:** res/values/, res/layout/, core/model/theme/NowPlayingButtonStyle.kt.
  - **Notes:** Animated vector drawables named avd_* (avd_play/avd_pause) drive player animations.


- `res/layout/` — ~100 layouts: player fragments per style, home, details, item layouts, dialogs, tag editor, widgets, and the two custom notification RemoteViews; with `layout-land/` variants.
  - **Purpose:** XML layouts for the legacy View UI (library, player, dialogs, widgets).
  - **Responsibilities:** Fragment layouts per player style, item_* adapter rows, detail screens, dialogs, tag editor, permission views.
  - **Inputs:** ViewBinding-generated classes consumed by fragments/adapters.
  - **Outputs:** Inflated legacy Views bound in Kotlin.
  - **Dependencies:** AndroidX View/ViewBinding, Material Components, custom views (MusicSlider, BaselineGridTextView, WidthFitSquareLayout).
  - **Consumed by:** All legacy fragments/adapters/activities (Abs*Fragment, adapters, dialog bindings).
  - **Related files:** res/values/, ui/component/views/, ui/adapters/.
  - **Notes:** layout-land/ variants; Compose migration in progress (rule 12: new UI in Compose). `fragment_main_recycler.xml` hosts the optional `QuickSearchBar` ComposeView (`quick_search_bar`) and `fragment_detail_list.xml` keeps the period/statistics description slot.


- `res/values/` — Strings (main file plus ~40 locale translations), colors (night/v31), dimens (land/large/sw600dp/xlarge), styles/themes, arrays, attrs, bools, ids, integers.
  - **Purpose:** Localized strings and static resource values.
  - **Responsibilities:** strings.xml base + ~40 locales, colors (incl. night/v31 black theme), dimens buckets, themes/styles, attributes, booleans, integers.
  - **Inputs:** Referenced via R.string/R.color/R.dimen/R.style/R.attr everywhere.
  - **Outputs:** Localized UI text and themed resources.
  - **Dependencies:** Android resource system; Material 3 themes.
  - **Consumed by:** Entire app (string/color/dimension resolution in Kotlin and XML).
  - **Related files:** res/layout/, res/xml/, ui/theme/.
  - **Notes:** Rule 4: only edit values/strings.xml; translations are managed elsewhere.


- `res/navigation/` — NavGraphs: `graph_main.xml` and `graph_settings.xml`, including notes, ghost search/settings and lock-screen destinations.
  - **Purpose:** Navigation graphs wiring the main and settings destinations.
  - **Responsibilities:** Declare library/player/settings destinations, actions, arguments (SafeArgs).
  - **Inputs:** Referenced by MainActivity NavHost and SettingsFragment child NavHost.
  - **Outputs:** Generated NavDirections/Args classes used by NavigationExt and fragments.
  - **Dependencies:** AndroidX Navigation + SafeArgs plugin.
  - **Consumed by:** MainActivity, SettingsFragment, extensions/navigation/NavigationExt.kt.
  - **Related files:** extensions/navigation/NavigationExt.kt, ui/screen/*.
  - **Notes:** Destinations match the "nav_*" ids referenced in Kotlin navigation code.


- `res/menu/` - 23 XML menus (song/album/artist/playlist/detail/selection/now-playing/settings), including A-B/seek/share/screencast/cast and shuffle-selection entries.
  - **Purpose:** Menu definitions for popups, toolbars and contextual action bars.
  - **Responsibilities:** item_* row popups, library screens menus, selection (CAB) menus, now-playing menus, settings search action, history period/removal actions.
  - **Inputs:** Referenced via R.menu in adapters (OnClickMenu) and fragments.
  - **Outputs:** Menu items resolved in MenuItemClickExt/onSongMenu handlers.
  - **Dependencies:** Android Menu API, PopupMenu.
  - **Consumed by:** All adapters, AbsPlayerFragment, AbsRecyclerViewFragment, SettingsFragment, SearchAdapter.
  - **Related files:** ui/component/menu/, ui/adapters/, AbsMultiSelectAdapter, menu_settings.xml.
  - **Notes:** Selection menus share ids like action_multi_select_adapter_check_all; history removal items are hidden by default and enabled by SongAdapter.historyMode (ADR-044). The detail menu includes the stats-only export-statistics action, hidden until DetailListFragment enables it for Top Tracks/Top Artists.


- `res/xml/` — Preference screens per category, widget info files, shortcuts, searchable, backup descriptors, provider paths, automotive app descriptor.
  - **Purpose:** XML configuration: preference screens, widget metadata, shortcuts, providers.
  - **Responsibilities:** preferences_screen_* per category, widget_info files, shortcuts.xml, searchable.xml, backup rules, file_paths, automotive_app_desc.
  - **Inputs:** Referenced from AndroidManifest and PreferencesScreenFragment/SettingsScreen.
  - **Outputs:** Preference screens rendered by PreferenceFragmentCompat; system metadata.
  - **Dependencies:** AndroidX Preference, App Widget metadata, FileProvider paths.
  - **Consumed by:** PreferencesScreenFragment, manifest components, Glance widget receivers.
  - **Related files:** ui/screen/settings/, app/src/main/AndroidManifest.xml.
  - **Notes:** Preference keys must match util/Preferences.kt constants. `preferences_screen_library.xml` includes the minimum and maximum song-duration SeekBars (`maximum_song_duration` 0..7200 s, 60 s step, 0 = no limit). `preferences_screen_playback.xml` adds the audio-focus mode and per-track/session-recovery keys; `preferences_screen_advanced.xml` hosts the custom `ScheduledBackupPreference` row and the Statistics category's `StatisticsCsvPreference` row; `provider_paths.xml` adds only the narrow `backup_share/` cache path used by the manual backup share sheet (ADR-042, ADR-043). New integration screens add `preferences_screen_battery.xml`, `preferences_screen_lock_screen.xml` and `preferences_screen_lock_screen_permissions.xml`; `preferences_screen_playback.xml` also hosts crossfade/fade/gapless/transport, notification layout/style and sleep-timer foreground-only keys, and `preferences_screen_library.xml` hosts extra-format indexing and folder scope/rescan actions. Static shortcuts use `app/src/main/res/xml/shortcuts.xml` (targetPackage `com.ssilencce`) plus the `app/src/debug/res/xml/shortcuts.xml` override (targetPackage `com.ssilencce.debug`); both target the exported `.activities.MainActivity` alias, and the duplicate four entries must stay in sync.


- `res/font/`, `res/anim/`, `res/raw/` — Google Sans Flex font, fragment/item animations, keep.xml.
  - **Purpose:** Bundled font family, animation resources and asset keep-rules.
  - **Responsibilities:** googlesansflex_* font weights, fragment/item animations, raw/keep.xml preserving resources.
  - **Inputs:** R.font.googlesansflex_* (Type.kt), R.anim references, keep rules.
  - **Outputs:** App typography (ui/theme/Type.kt), view/fragment transitions.
  - **Dependencies:** Android resource system.
  - **Consumed by:** ui/theme/Type.kt, FragmentExt transitions, item animations.
  - **Related files:** ui/theme/Type.kt, extensions/FragmentExt.kt.
  - **Notes:** Font is the sole bundled family; custom fonts importable via settings.

### 2026-10 additions (A-L)

- `data/local/room/PlayEventEntity.kt` — Append-only playback event for period statistics.
  - **Purpose:** Record each counted play with song id, artist name and timestamp.
  - **Responsibilities:** Store songId, artistName and playedAt; index played_at and song_id.
  - **Inputs:** Written by PlayEventDao.recordPlay with the aggregate play-count increment.
  - **Outputs:** Event rows used for this-week/month/12-month/year queries.
  - **Dependencies:** Room, PlayCountEntity.
  - **Consumed by:** PlayEventDao, RealSmartRepository.
  - **Related files:** PlayEventDao.kt, PlayCountEntity.kt, ListeningPeriod.kt.
  - **Notes:** Added in Room v8; period data starts at introduction, while all-time includes earlier aggregate counts (ADR-040). No retention policy yet.


- `data/local/room/PlayEventDao.kt` — DAO for playback events and period/artist aggregates.
  - **Purpose:** Query period statistics from the append-only event log.
  - **Responsibilities:** Insert events; `recordPlay` transaction; cutoff song counts; recorded-name artist grouping with unbounded period total in the same SQL; start-inclusive/end-exclusive song and artist range queries for automatic CSV; Flow/suspend queries; delete-older/clear; ArtistEventPlayCount/ArtistIdPlayCount DTOs.
  - **Inputs:** Song/timePlayed, cutoff, start/end millis, limit.
  - **Outputs:** Event rows, SongPlayCount, ArtistEventPlayCount with range-restricted total, shared ArtistIdPlayCount DTO, song/artist Flows.
  - **Dependencies:** Room, Flow, PlayCountDao, PlayCountEntity, Song.
  - **Consumed by:** RealSmartRepository, LibraryViewModel stats flows, DetailListFragment.
  - **Related files:** PlayEventEntity.kt, PlayCountDao.kt, ListeningPeriod.kt, ArtistPlayStat.kt.
  - **Notes:** Period attribution uses exact event `artist_name`, independent of aggregate rename/deletion. Only outer rows are limited; total and counts share one snapshot and Flow invalidation. All-time aggregate IDs/counts remain in PlayCountDao (ADR-040). `topSongsBetween`/`topArtistsBetween` use `played_at >= start AND played_at < end`; the artist total subquery uses the same range.


- `data/local/room/TrackPositionEntity.kt` — Remembered playback position for one media item.
  - **Purpose:** Persist long-form per-track resume positions.
  - **Responsibilities:** Store mediaId as primary key, positionMs and updatedAt.
  - **Inputs:** PlaybackService position observer and transition/pause logic.
  - **Outputs:** TrackPositionEntity rows.
  - **Dependencies:** Room.
  - **Consumed by:** TrackPositionDao, PlaybackService.
  - **Related files:** TrackPositionDao.kt, PlaybackService.kt.
  - **Notes:** Added in Room v8; short tracks and positions near start/end are discarded by service policy (ADR-042).


- `data/local/room/TrackPositionDao.kt` - DAO for remembered track positions.
  - **Purpose:** Upsert/read/delete/prune per-track resume positions, including backup snapshots.
  - **Responsibilities:** Upsert position; bulk upsert and read all for backup restore; get by mediaId; delete one/all; prune to newest N entries.
  - **Inputs:** TrackPositionEntity, TrackPositionEntity lists, mediaId, keep count.
  - **Outputs:** TrackPositionEntity?/lists and mutation effects.
  - **Dependencies:** Room.
  - **Consumed by:** TrackPositionWriter, PlaybackService, BackupHelper (all/upsertAll).
  - **Related files:** TrackPositionEntity.kt, TrackPositionWriter.kt, PlaybackService.kt.
  - **Notes:** Prune keeps the most recently updated rows; long-form resume is opt-in (ADR-042).


- `playback/TrackPositionWriter.kt` — Serial nonblocking writer for per-track resume positions.
  - **Purpose:** Order TrackPosition DAO writes off the main thread and preserve the final snapshot during service destruction.
  - **Responsibilities:** Channel-backed upsert/delete/prune queue on a SupervisorJob + IO scope; `persistFinalSnapshot` deletes near-end positions; write failures are contained.
  - **Inputs:** TrackPositionDao, media id, position/duration, keep count.
  - **Outputs:** Ordered DAO upsert/delete/prune operations.
  - **Dependencies:** TrackPositionDao, TrackPositionEntity, coroutines Channel/Dispatchers/SupervisorJob.
  - **Consumed by:** PlaybackService (position observer, transitions, pause, destroy).
  - **Related files:** TrackPositionDao.kt, TrackPositionEntity.kt, PlaybackService.kt.
  - **Notes:** Application-owned serial writer; service callbacks enqueue before destruction so an older in-flight write cannot overtake the final snapshot. Non-blocking by design; pending writes can still be lost if Android kills the whole process. Near-end final snapshots delete instead of persisting (ADR-042).


- `playback/PlaybackStateWriter.kt` — Serial application-owned persistence of immutable queue snapshots.
  - **Purpose:** Preserve queue edits through pause/service destruction and order earlier writes before restoration.
  - **Responsibilities:** Channel-backed named/global transactional replacements; resume-only updates; awaitable outgoing-queue saves and restoration barriers; contain/log write failures.
  - **Inputs:** QueueDao, NamedQueueDao, Main-captured QueueStateSnapshot with media ids, binding, index/position and modes.
  - **Outputs:** Ordered QueueEntity/NamedQueueItemEntity/resume writes; completion barriers.
  - **Dependencies:** Room DAOs, Channel, CompletableDeferred, SupervisorJob, Dispatchers.IO.
  - **Consumed by:** PersistentStorage normal/pause/final saves, named-queue switching and restore.
  - **Related files:** PersistentStorage.kt, QueueDao.kt, NamedQueueDao.kt, PlaybackService.kt.
  - **Notes:** Never reads or retains a player. FIFO is shared across service instances; queued IO is not a whole-process-kill durability guarantee (ADR-045).


- `ui/dialogs/AudioFocusAskDialog.kt` — Restorable Compose focus-consent DialogFragment.
  - **Purpose:** Keep the pending Ask visible across activity recreation without granting stale consent.
  - **Responsibilities:** Stable fragment tag; request-id arguments; Compose title/message/buttons; FragmentResult confirmation/cancellation; explicit cancellation only.
  - **Inputs:** Request id and strings from values/strings.xml.
  - **Outputs:** Token-bound confirm/cancel fragment results to MainActivity.
  - **Dependencies:** DialogFragment, ComposeView, Material 3, SsilencceTheme.
  - **Consumed by:** MainActivity focus-state reconciliation.
  - **Related files:** MainActivity.kt, PlayerViewModel.kt, AudioFocusController.kt, Playback.kt.
  - **Notes:** No constructor callbacks or transient visible flag; onDestroy does not cancel the service request (ADR-045).


- `ui/dialogs/ComposeChoiceDialog.kt` — Reusable Compose single-choice and confirmation dialogs.
  - **Purpose:** Replace short legacy MaterialAlertDialogBuilder item lists/confirmations without retaining transient fragment callbacks across recreation.
  - **Responsibilities:** Render title/options or title/message in a Material 3 surface; result mode uses a stable FragmentResult key; export mode carries playlist ids and writes the chosen format; shortcut mode carries entity data and adds the dynamic shortcut; 88%-width window.
  - **Inputs:** Title, option labels, selected index/message/positive label; optional stable result key, playlist ids or shortcut entity data.
  - **Outputs:** FragmentResult selected index for result dialogs; export file/toast or added shortcut/toast for action-owned dialogs; dismissal without action on cancel.
  - **Dependencies:** DialogFragment, ComposeView, Material 3, SsilencceTheme, PlaylistExportExecutor, ShortcutHelper.
  - **Consumed by:** Playlist export format, add-shortcut confirmation and listening-period dialogs.
  - **Related files:** MenuItemClickExt.kt, DetailListFragment.kt, AudioFocusAskDialog.kt.
  - **Notes:** Period listener is registered in `DetailListFragment.onCreate`; export and shortcut actions use only serialized arguments. Selection dismisses the dialog and hands the request to the process-owned `PlaylistExportExecutor` synchronously on Main, so rotation cannot cancel it and a restored token cannot start it twice. Options scroll within a screen-height cap and radio rows expose a single selectable target. No constructor callback or extra test configuration.


- `ui/dialogs/NotPlayedDaysDialog.kt` — Compose dialog for the Not Recently Played threshold.
  - **Purpose:** Let the smart list choose the default history interval or an arbitrary day window.
  - **Responsibilities:** Radio choice between history interval and custom days, numeric field with 1–3650 validation, FragmentResult with the normalized day count, 88%-width window.
  - **Inputs:** Current day count, current history-interval description, user input.
  - **Outputs:** `RESULT_DAYS` with 0 or a positive day count; dismissal without a result on cancel.
  - **Dependencies:** DialogFragment, ComposeView, Material 3, SsilencceTheme, NotPlayedDaysPolicy.
  - **Consumed by:** DetailListFragment's Not Recently Played action.
  - **Related files:** core/model/NotPlayedDaysPolicy.kt, ui/screen/other/DetailListFragment.kt, util/Preferences.kt.
  - **Notes:** The result listener is registered in `DetailListFragment.onCreate`; the dialog owns no transient fragment callback.


- `data/local/room/NamedQueueEntity.kt` — Persistent named-queue header.
  - **Purpose:** Store user-named queue metadata separate from the active QueueEntity.
  - **Responsibilities:** id/name, created/updated timestamps, resume index/position, repeat mode and shuffle-enabled state.
  - **Inputs:** NamedQueueRepository create/update/rename.
  - **Outputs:** NamedQueueEntity rows.
  - **Dependencies:** Room.
  - **Consumed by:** NamedQueueDao, NamedQueueRepository, PersistentStorage.
  - **Related files:** NamedQueueItemEntity.kt, NamedQueueDao.kt, QueueEntity.kt.
  - **Notes:** Room v8 additive table; a named queue is a snapshot loaded into the active queue, not a second live timeline (ADR-041).


- `data/local/room/NamedQueueItemEntity.kt` — Ordered media ids for a named queue.
  - **Purpose:** Persist each named queue's ordered media-id list.
  - **Responsibilities:** Composite key `(queue_id, order)` with mediaId.
  - **Inputs:** NamedQueueRepository snapshot conversion.
  - **Outputs:** Ordered item rows.
  - **Dependencies:** Room.
  - **Consumed by:** NamedQueueDao, NamedQueueRepository.
  - **Related files:** NamedQueueEntity.kt, NamedQueueDao.kt.
  - **Notes:** Deleting a queue deletes its items explicitly through the DAO; no foreign key/cascade (ADR-041).


- `data/local/room/NamedQueueDao.kt` — DAO for named queues, summaries and ordered items.
  - **Purpose:** Transactional persistence for named-queue snapshots.
  - **Responsibilities:** Entity/items reads; summary list/Flow with counts and `createdAt`; stable `created_at ASC, id ASC` summary order; upsert queue; insert/delete items; transactional saveQueue/deleteQueueWithItems; rename; update resume state.
  - **Inputs:** NamedQueueEntity, item list, ids, resume values.
  - **Outputs:** NamedQueueSummary/WithItems, entities, summaries Flow.
  - **Dependencies:** Room (@Dao, @Transaction, @Relation, Flow).
  - **Consumed by:** NamedQueueRepository, PersistentStorage.
  - **Related files:** NamedQueueEntity.kt, NamedQueueItemEntity.kt, NamedQueueRepository.kt.
  - **Notes:** `saveQueue` replaces items inside one transaction; summariesFlow/summaries expose `createdAt` and use `created_at ASC, id ASC` so manager display and the named chain share one deterministic order (ADR-041). No schema migration is needed; `queues()` remains an internal read.


- `ui/screen/player/NamedQueueChainPolicy.kt` - Pure named-queue chain resolver.
  - **Purpose:** Resolve First/Previous/Next against the stable createdAt/id id list.
  - **Responsibilities:** `matchesTimeline` requires exact active snapshot/timeline media ID list equality (order/count); resolve finds the active id, First = first, Previous = prior, Next = following; return Boundary at first/last and NoActive for null/missing active; never wrap.
  - **Inputs:** Ordered ids, active id, direction, active/timeline media id lists.
  - **Outputs:** Target id, Boundary or NoActive.
  - **Dependencies:** Kotlin stdlib only.
  - **Consumed by:** PlayerViewModel chain actions, compiled chain probe.
  - **Related files:** PlayerViewModel.kt, NamedQueueRepository.kt, NamedQueueDao.kt.
  - **Notes:** No Android/Room dependency; the repository supplies the same stable order used by the manager.

- `data/local/repository/NamedQueueRepository.kt` - Single writer for named-queue management.
  - **Purpose:** Create/update/replace/rename/delete named-queue snapshots with explicit naming and limits.
  - **Responsibilities:** Map NamedQueueSnapshot to entity/items; UUID ids; serialized create/replace/rename under a Mutex; 100-queue cap and 80-character name enforcement; keep-both suffixing through QueueNamePolicy; snapshot-only save replace; rename with optional target deletion + source rename inside one Room `database.withTransaction`; transactional save; delete/delete-others; expose summary and queue reads; expose `orderedQueueIds()` from the same summaries query for the named chain.
  - **Inputs:** Requested name, NamedQueueSnapshot, queue id, optional replace queue id.
  - **Outputs:** NamedQueueEntity, actual renamed name, NamedQueueWithItems, summary Flow, ordered ids; NamedQueueLimitException/NamedQueueNameException.
  - **Dependencies:** NamedQueueDao, SsilencceDatabase (Room.withTransaction), QueueNamePolicy, UUID, Mutex.
  - **Consumed by:** PlayerViewModel (save/switch/rename/delete/replace/chain).
  - **Related files:** NamedQueueDao.kt, NamedQueueEntity.kt, QueueNamePolicy.kt, NamedQueueChainPolicy.kt, PlayerViewModel.kt.
  - **Notes:** Active playback state remains in QueueEntity; exact shuffle permutation is not stored, only shuffle-enabled mode (ADR-041). Name uniqueness is serialized in-process but still not DB-constrained; no schema/unique index is added. Rename Replace deletes the selected target and renames the source; normal Rename keeps the source. Both run in one Room transaction. Replace never touches the active playback queue; renaming the active source keeps its ID, while deleting an active target clears the marker. PersistentStorage also updates the active named queue through NamedQueueDao, so the repository is the creation/management writer but not the only runtime writer.

- `data/local/repository/QueueNamePolicy.kt` - Pure named-queue naming/limit policy.
  - **Purpose:** Keep name validation, keep-both suffixing and count limits deterministic and JVM-probeable.
  - **Responsibilities:** Trim/default, case-insensitive duplicate validation, 80-character cap, `Name (2)` suffixing with truncation, 100-queue cap.
  - **Inputs:** Requested name, existing names, existing count.
  - **Outputs:** Validation result, unique name or can-create boolean.
  - **Dependencies:** Kotlin stdlib only.
  - **Consumed by:** NamedQueueRepository, QueueScreen name/duplicate dialogs, compiled policy probe.
  - **Related files:** NamedQueueRepository.kt, QueueScreen.kt.
  - **Notes:** UI and repository share the same limits; repository enforcement prevents bypass.


- `data/model/ListeningPeriod.kt` — Periods available for play statistics.
  - **Purpose:** Identify this-week/month/12-month/year/all-time windows.
  - **Responsibilities:** Enum entries; `cutoffMillis(now)` using device-local calendar boundaries; AllTime returns null.
  - **Inputs:** Optional ZonedDateTime.
  - **Outputs:** Cutoff epoch millis or null.
  - **Dependencies:** java.time (ZonedDateTime, DayOfWeek, TemporalAdjusters).
  - **Consumed by:** RealSmartRepository, DetailListFragment, period selector.
  - **Related files:** ArtistPlayStat.kt, PlayEventDao.kt, DetailListFragment.kt.
  - **Notes:** Period data begins when PlayEventEntity starts recording; AllTime uses aggregate PlayCountEntity (ADR-040).


- `data/model/ArtistPlayStat.kt` — Artist row with play count and percentage.
  - **Purpose:** Carry one artist's period/all-time statistics to the UI.
  - **Responsibilities:** Typed recorded-name/aggregate-ID identity; snapshot artistName; optional actionable Artist; playCount/full total; overflow-safe integer percentage.
  - **Inputs:** RealSmartRepository aggregation.
  - **Outputs:** ArtistPlayStat rows.
  - **Dependencies:** Artist.
  - **Consumed by:** SmartRepository Flow/suspend results, DetailListFragment, ArtistAdapter.
  - **Related files:** ListeningPeriod.kt, ArtistAdapter.kt, SmartRepository.kt, SongPlayStat.kt.
  - **Notes:** Recorded periods use exact-name keys; all-time uses aggregate IDs. Full denominator participates in model equality, so outside-top-100 changes remain observable even if rounded percentage is unchanged (ADR-040).


- `data/model/SongPlayStat.kt` - Song row with its displayed play count.
  - **Purpose:** Carry both song metadata and the count used by Top Tracks CSV export.
  - **Responsibilities:** Pair a `Song` snapshot with a long play count while preserving repository ordering.
  - **Inputs:** RealSmartRepository all-time aggregate or period event aggregation.
  - **Outputs:** `SongPlayStat` rows.
  - **Dependencies:** Song.
  - **Consumed by:** `Repository.songPlayStats`, `StatisticsCsvExporter`.
  - **Related files:** Song.kt, ArtistPlayStat.kt, SmartRepository.kt.
  - **Notes:** Counts are kept separate from the screen's `List<Song>` flow; all-time values come from `PlayCountEntity`, period values from `PlayEventEntity`.


- `playback/AudioFocusController.kt` — Single owner of audio focus for playback.
  - **Purpose:** Implement Normal/Ignore/Ask/ResumeAfterCall focus behavior outside ExoPlayer.
  - **Responsibilities:** Request/abandon focus; uniquely identified Ask state; explicit intent gate and token-validated confirm/cancel; transient loss/resume; ducking/volume restore; mode/release cleanup.
  - **Inputs:** Context/AudioManager, raw ExoPlayer, SharedPreferences, onPendingAskChanged callback.
  - **Outputs:** Focus requests, player pause/play/volume changes, ask event.
  - **Dependencies:** AudioManager, AudioFocusRequest, Player, AudioFocusMode, Preferences.
  - **Consumed by:** PlaybackService, MainActivity (ask dialog), PlayerViewModel (confirm/cancel commands).
  - **Related files:** AudioFocusMode.kt, PlaybackService.kt, MainActivity.kt.
  - **Notes:** ExoPlayer's built-in focus handling is disabled. Repeated play preserves a pending Ask; explicit/no-op pause clears it synchronously. Internal focus pauses bypass user-intent interception. A synchronous GRANTED result normalizes stale transient-loss state without waiting for a GAIN callback. Confirm/cancel must match the live request id; no automatic timeout (ADR-042, ADR-045).


- `playback/PlaybackTileService.kt` — Quick Settings playback tile.
  - **Purpose:** Toggle playback and mirror session state from Quick Settings.
  - **Responsibilities:** Connect a MediaController only while listening; handle pending toggles; mirror active/inactive/unavailable state; release controller on stop/destroy; update label/subtitle/state description.
  - **Inputs:** TileService lifecycle, MediaController/Player events.
  - **Outputs:** QS Tile state and play/pause commands.
  - **Dependencies:** TileService, MediaController, SessionToken, PlaybackService, Media3 Player.
  - **Consumed by:** AndroidManifest QS tile declaration.
  - **Related files:** PlaybackService.kt, AndroidManifest.xml.
  - **Notes:** No polling or persistent controller; manifest exports the QS_TILE action (ADR-042).


- `util/AudioFocusMode.kt` — Persisted audio-focus mode enum.
  - **Purpose:** Define the four user-selectable focus policies.
  - **Responsibilities:** Normal, Ignore, Ask, ResumeAfterCall; parse by enum name with Normal fallback.
  - **Inputs:** Stored string.
  - **Outputs:** AudioFocusMode value.
  - **Dependencies:** None.
  - **Consumed by:** Preferences.audioFocusMode, AudioFocusController, PreferencesScreenFragment.
  - **Related files:** AudioFocusController.kt, util/Preferences.kt.
  - **Notes:** Legacy IGNORE_AUDIO_FOCUS still forces Ignore (ADR-042).


- `util/RestoreSummary.kt` - Pure restore accounting and honest outcome selection.
  - **Purpose:** Track what a backup restore actually recognized, selected and imported.
  - **Responsibilities:** Count total/recognized/selected entries; distinguish Settings, Lyrics, ArtistImages, Playlists, Favorites, PlayCounts, Queues, PerFileSettings and Unrecognized entries; record imported items and selected-entry failures; classify playlist creation/song additions; produce RestoreOutcome (NothingSelected/NoRecognizedContent/NothingImported/Partial/Success/SuccessWithCount).
  - **Inputs:** RestoreEntryKind, selected flag, import counts.
  - **Outputs:** Counters and RestoreOutcome.
  - **Dependencies:** None (Android-free).
  - **Consumed by:** BackupHelper.restoreBackup.
  - **Related files:** BackupHelper.kt, RestoreOutcome.kt, util/backup/BackupArchivePolicy.kt.
  - **Notes:** Keeps restore warnings factual without Android dependencies; a created empty playlist still counts as an import (ADR-039 change set). A selected entry that imports nothing and is a real failure is counted separately; any import plus a failure reports `Partial`, while zero imports keeps `NothingImported` and valid-only imports keep `Success`/`SuccessWithCount` (ADR-047). AA-09 adds Favorites/PlayCounts/Queues/PerFileSettings and records malformed selected JSON entries through `recordFailure()`; mixed queue batches can report both an import and a failure (ADR-048).


- `util/ShortcutHelper.kt` — Dynamic per-entity launcher shortcuts.
  - **Purpose:** Add/remove album/artist/folder/playlist launcher shortcuts.
  - **Responsibilities:** Build ShortcutInfoCompat with entity type/id/path/name extras; push dynamic shortcut; remove by typed id; stable shortcutId.
  - **Inputs:** Context, ShortcutEntityType, id, name, optional path.
  - **Outputs:** Boolean push result; removed shortcuts.
  - **Dependencies:** ShortcutManagerCompat, ShortcutInfoCompat, IconCompat, MainActivity.
  - **Consumed by:** MenuItemClickExt (add dialog), MainActivity (resolve intent), DeletePlaylistDialog (remove).
  - **Related files:** MainActivity.kt, MenuItemClickExt.kt, DeletePlaylistDialog.kt.
  - **Notes:** Launcher capacity failures are reported by the caller; shortcuts carry `com.ssilencce.shortcut.ENTITY` (ADR-044). The intent targets the exported `.activities.MainActivity` compatibility alias, not the non-exported `.ui.screen.MainActivity`; a cold launch can precede the MediaController connection, so PlayerViewModel defers the queue request until the controller is published.


- `util/statistics/StatisticsCsvWriter.kt` - Pure CSV formatter for exported statistics rows.
  - **Purpose:** Keep CSV headers, row formatting, quoting and spreadsheet-safety rules independent of Android.
  - **Responsibilities:** Emit UTF-8-compatible CRLF text for Top Tracks/Top Artists headers; 1-based ranks; RFC 4180 text quoting; apostrophe-prefix formula neutralization for `= + - @`; plain numeric fields.
  - **Inputs:** `List<SongPlayStat>` or `List<ArtistPlayStat>`.
  - **Outputs:** Complete CSV `String` with a final CRLF, no BOM.
  - **Dependencies:** SongPlayStat, ArtistPlayStat.
  - **Consumed by:** StatisticsCsvExporter.
  - **Related files:** StatisticsCsvExporter.kt, SongPlayStat.kt, ArtistPlayStat.kt.
  - **Notes:** Quotes only comma/quote/CR/LF text and doubles embedded quotes; formula prefix is applied before quoting. No Android or IO dependencies.


- `util/statistics/StatisticsCsvExporter.kt` - Koin-backed CSV preparation and SAF writer.
  - **Purpose:** Snapshot the selected statistics period or an automatic completed window and write it through a document URI.
  - **Responsibilities:** Query Repository off Main; cap at 100 rows; choose manual period/type names or automatic `ssilencce-auto-...-<start>_<end>.csv` names; detect empty statistics; write UTF-8 through ContentResolver; best-effort delete partial output; rethrow cancellation. Automatic names come from `StatisticsCsvFiles.AUTO_PREFIX`; the worker appends `.partial` for staging.
  - **Inputs:** ContentType.TopTracks/TopArtists, ListeningPeriod or StatisticsCsvWindow, ContentResolver, document URI.
  - **Outputs:** `StatisticsCsvDocument?` (suggested file name + content) and written SAF document.
  - **Dependencies:** Koin Repository, StatisticsCsvWriter, StatisticsCsvWindow, ContentResolver, Dispatchers.IO.
  - **Consumed by:** DetailListFragment manual export action; StatisticsCsvWorker automatic export.
  - **Related files:** StatisticsCsvWriter.kt, StatisticsCsvPeriod.kt, Repository.kt, DetailListFragment.kt, StatisticsCsvWorker.kt.
  - **Notes:** Empty rows return null before any picker; failures delete the selected URI best-effort then propagate. Manual names remain `ssilencce-top-tracks-<period>.csv` / `ssilencce-top-artists-<period>.csv`; automatic names are `ssilencce-auto-top-tracks-<start>_<end>.csv` / `...artists...`. The writer and manual API are unchanged.

- `util/statistics/StatisticsCsvPeriod.kt` - Pure automatic period window and cadence helper.
  - **Purpose:** Compute the latest completed local calendar period and persisted keys without Android.
  - **Responsibilities:** StatisticsCsvCadence storage values; weekly Monday–Sunday/monthly/yearly windows; inclusive file end; exclusive query end; DST-safe start/end millis; guarded key parsing (year 1..9999, Monday weekly keys, valid months); `needsExport` gate; automatic prefix and `.partial` staging-name helpers.
  - **Inputs:** Cadence, ZonedDateTime, ZoneId, key string.
  - **Outputs:** StatisticsCsvWindow, key, file stem, millis, from-key window or null.
  - **Dependencies:** java.time only.
  - **Consumed by:** StatisticsCsvWorker, StatisticsCsvDialog/Preference labels, compiled period probe.
  - **Related files:** StatisticsCsvExporter.kt, StatisticsCsvStore.kt, StatisticsCsvWorker.kt.
  - **Notes:** No Android imports; manual ListeningPeriod is not extended.

- `util/statistics/StatisticsCsvStore.kt` - Dedicated settings store for automatic statistics CSV.
  - **Purpose:** Persist enable/cadence/folder/last-result/per-cadence keys and one pending period.
  - **Responsibilities:** `statistics_csv` SharedPreferences; result/failure enums; read/write settings; cadence change clears pending; success/no-data advances per-cadence key; failure records pending key/error; clear pending.
  - **Inputs:** Context, configuration, result/failure and timestamps.
  - **Outputs:** StatisticsCsvState; persisted preferences.
  - **Dependencies:** SharedPreferences, Uri, string resources.
  - **Consumed by:** StatisticsCsvController, Worker, Preference, Dialog.
  - **Related files:** StatisticsCsvPeriod.kt, StatisticsCsvWorker.kt, StatisticsCsvController.kt.
  - **Notes:** Separate from scheduled backups and default preferences; disabling keeps configuration/last keys.

- `util/statistics/StatisticsCsvScheduler.kt` - Daily WorkManager scheduling for automatic CSV.
  - **Purpose:** Enqueue/cancel one unique daily check.
  - **Responsibilities:** battery/storage-not-low constraints; 1-day PeriodicWorkRequest; unique name `automatic_statistics_csv`; ExistingPeriodicWorkPolicy.UPDATE.
  - **Inputs:** Context.
  - **Outputs:** Unique PeriodicWorkRequest / cancellation.
  - **Dependencies:** WorkManager, StatisticsCsvWorker.
  - **Consumed by:** StatisticsCsvController.
  - **Related files:** StatisticsCsvWorker.kt, StatisticsCsvController.kt, StatisticsCsvStore.kt.
  - **Notes:** No AlarmManager, charging constraint, notification or exact-boundary guarantee.

- `util/statistics/StatisticsCsvWorker.kt` - Daily automatic statistics CSV worker.
  - **Purpose:** Export the latest completed or pending period into the persisted SAF folder.
  - **Responsibilities:** enabled/no-folder/permission/folder checks; fail-soft pending-window selection; stale feature-owned `.partial` cleanup; existing non-empty skip; zero-byte verification; prepare both top-100 documents; write each to `<final>.partial`, close and `renameTo(final)`; delete staging on write/rename failure/cancellation; Success/NoData/Failure records; handled failures return WorkManager success.
  - **Inputs:** Worker parameters, StatisticsCsvStore, SAF tree Uri, current clock.
  - **Outputs:** Up to two CSV files; store result/pending state; WorkManager Result.
  - **Dependencies:** WorkManager, DocumentFile, StatisticsCsvExporter, StatisticsCsvStore, StatisticsCsvPeriod.
  - **Consumed by:** WorkManager.
  - **Related files:** StatisticsCsvScheduler.kt, StatisticsCsvController.kt, StatisticsCsvExporter.kt.
  - **Notes:** Latest-only catch-up; pending retry before advancing; no historical backfill; no old-file deletion; partial pair keeps successful final; a final CSV only exists after a complete staged write and rename, and a stale `.partial` is never counted as success.

- `util/statistics/StatisticsCsvController.kt` - Settings/WorkManager facade for automatic statistics CSV.
  - **Purpose:** Persist SAF read/write access, save configuration and reconcile the unique daily work.
  - **Responsibilities:** StateFlow of store state; rememberDestination with READ|WRITE; configure missing-folder/permission results; schedule/cancel; destination-name resolution; persisted read/write access check.
  - **Inputs:** Context, enabled/cadence/destination.
  - **Outputs:** ConfigureResult, StatisticsCsvState.
  - **Dependencies:** StatisticsCsvStore, Scheduler, DocumentFile.
  - **Consumed by:** StatisticsCsvDialogFragment.
  - **Related files:** StatisticsCsvStore.kt, StatisticsCsvScheduler.kt, StatisticsCsvDialogFragment.kt.
  - **Notes:** Disabling cancels work but keeps folder/cadence/last keys; no Koin binding.

- `ui/screen/settings/statistics/StatisticsCsvPreference.kt` - Advanced Statistics row for automatic CSV.
  - **Purpose:** Open the Compose sheet and show cadence/folder/last result.
  - **Responsibilities:** Register dedicated preference listener; build summary; find hosting FragmentActivity; show StatisticsCsvDialogFragment.
  - **Inputs:** StatisticsCsvStore state, preference context.
  - **Outputs:** Dialog launch; dynamic summary.
  - **Dependencies:** Preference, SharedPreferences listener, StatisticsCsvStore/DialogFragment.
  - **Consumed by:** preferences_screen_advanced.xml.
  - **Related files:** StatisticsCsvDialogFragment.kt, StatisticsCsvStore.kt.
  - **Notes:** Custom preference owns its click; no PreferencesScreenFragment route.

- `ui/screen/settings/statistics/StatisticsCsvDialogFragment.kt` - Compose automatic statistics CSV sheet.
  - **Purpose:** Enable/cadence/folder/last-status configuration.
  - **Responsibilities:** SAF OpenDocumentTree picker; persisted permission; enable switch; Weekly/Monthly/Yearly radios; folder label; last attempt/result/error; save validation; cancel.
  - **Inputs:** StatisticsCsvController, user selections.
  - **Outputs:** Configure call; dismiss/error state.
  - **Dependencies:** Compose Material3, SsilencceTheme, BottomSheetDialogFragment, StatisticsCsvController, DocumentFile.
  - **Consumed by:** StatisticsCsvPreference.
  - **Related files:** StatisticsCsvController.kt, StatisticsCsvPreference.kt.
  - **Notes:** Compose sheet modeled on scheduled backup; no notification or run-now action.


- `util/m3u/PlaylistExportExecutor.kt` — Process-owned playlist-export runner for dialog actions.
  - **Purpose:** Keep a selected export alive when its dialog is dismissed or recreated, and prevent a restored dialog from starting the same request twice.
  - **Responsibilities:** Reserve a dialog-supplied token, query playlists off Main, call `M3UWriter` for one or many playlists, report failure toasts on Main, release the token.
  - **Inputs:** Export token, playlist ids, `PlaylistExportFormat`, application context.
  - **Outputs:** Export files/toasts; process-owned coroutine work.
  - **Dependencies:** PlaylistRepository, M3UWriter, Koin GlobalContext, CoroutineScope SupervisorJob + Main, ConcurrentHashMap.
  - **Consumed by:** ComposeChoiceDialogFragment export action.
  - **Related files:** M3UWriter.kt, PlaylistExportFormat.kt, ComposeChoiceDialog.kt, PlaylistRepository.kt.
  - **Notes:** Main-scoped coroutine only coordinates; Room playlist resolution runs on IO. The token set is process-local; a whole-process kill still loses pending work.

- `util/m3u/PlaylistExportFormat.kt` - Playlist export format enum.
  - **Purpose:** Single source of export extensions, MediaStore MIME types and labels.
  - **Responsibilities:** M3U/M3U8/PLS with extension, concrete playlist MIME and string resource.
  - **Inputs:** None.
  - **Outputs:** Format selection used by writer/UI.
  - **Dependencies:** R.string export labels.
  - **Consumed by:** M3UWriter, MenuItemClickExt format chooser.
  - **Related files:** M3UWriter.kt, MenuItemClickExt.kt.
  - **Notes:** Single and batch export share the same chooser; each entry carries the exact extension, concrete MediaStore MIME and label (ADR-044).


- `ui/component/base/QuickSearchable.kt` — Shared inline quick-search contract.
  - **Purpose:** Let list fragments own the durable query and accept filtering updates without owning the search UI.
  - **Responsibilities:** `quickSearchQuery` durable property; `onQuickSearch(query)` callback; case-insensitive string matching helper.
  - **Inputs:** Query string edited in or restored by the host.
  - **Outputs:** Updated fragment query and filtered adapter data in implementers.
  - **Dependencies:** None.
  - **Consumed by:** SongListFragment, AlbumListFragment, ArtistListFragment, PlaylistListFragment, GenresListFragment, YearsListFragment, FoldersListFragment.
  - **Related files:** AbsRecyclerViewFragment.kt, QuickSearchBar.kt, QuickSearchSupport.kt.
  - **Notes:** The seven top-level lists implement the interface; detail/secondary screens use `QuickSearchHost` with the same `quick_search_query` saved-state key and fragment-owned query (ADR-044). Compose state is only a view-lifetime mirror so recreation cannot leave the text and filtered list divergent.


- `ui/component/base/QuickSearchPolicy.kt` - Pure inline filter policy.
  - **Purpose:** Keep the detail quick-search matching rules Android-free and JVM-probeable.
  - **Responsibilities:** Blank matches all; otherwise case-insensitive `contains` over supplied strings; `isNoMatch` only for a non-blank query with a non-empty source and zero visible rows.
  - **Inputs:** Query and candidate fields/counts.
  - **Outputs:** Match/no-match booleans.
  - **Dependencies:** Kotlin stdlib only.
  - **Consumed by:** QuickSearchHost, DetailQuickSearch, all in-scope detail fragments, compiled filter probe.
  - **Related files:** QuickSearchSupport.kt, DetailQuickSearch.kt.
  - **Notes:** No Android, repository or persistence dependency.

- `ui/component/base/QuickSearchSupport.kt` - Shared detail-screen quick-search host/state.
  - **Purpose:** Host the Compose `QuickSearchBar` in View-based detail screens and own the shared saved-state key.
  - **Responsibilities:** `QuickSearchHost` sets DisposeOnViewTreeLifecycleDestroyed, hosts the bar in `SsilencceTheme`, uses the mini-player margin for list padding and `max(FAB/navigation margin, navigationHeight)` for the bar's bottom clearance, adds 72 dp list padding, replays a restored query; `QuickSearchState` restores/saves `quick_search_query`.
  - **Inputs:** Fragment, ComposeView, RecyclerView, mini-player and FAB/navigation margin LiveData, navigation-height provider, optional extra padding, query provider and change callback.
  - **Outputs:** Visible bottom bar, updated list padding/margin, query callbacks, saved-state writes.
  - **Dependencies:** ComposeView, QuickSearchBar, SsilencceTheme, LibraryMargin.
  - **Consumed by:** DetailListFragment, Album/Artist/Genre/Year/Folder/Playlist detail fragments.
  - **Related files:** QuickSearchPolicy.kt, QuickSearchable.kt, QuickSearchBar.kt.
  - **Notes:** The fragment remains the durable query owner; the host does not filter or query repositories.

- `ui/component/base/DetailQuickSearch.kt` - Model-to-field mapping for detail quick search.
  - **Purpose:** Keep the per-entity match fields in one place.
  - **Responsibilities:** Song title/album/display artist (+ optional file name), album name/artist/album-artist, artist display name, ArtistPlayStat recorded/resolved artist name.
  - **Inputs:** Query and model instances.
  - **Outputs:** Match boolean.
  - **Dependencies:** QuickSearchPolicy, Song/Album/Artist/ArtistPlayStat, media display extensions.
  - **Consumed by:** DetailListFragment and album/artist/genre/year/folder/playlist detail fragments.
  - **Related files:** QuickSearchPolicy.kt, QuickSearchSupport.kt.
  - **Notes:** No matching fields beyond the documented list; percentages are preserved because ArtistPlayStat instances are not rewritten.


- `ui/component/compose/QuickSearchBar.kt` - Compose inline quick-search field.
  - **Purpose:** Reusable pill search field for library lists.
  - **Responsibilities:** Query field, search icon, clear action, Material3 styling.
  - **Inputs:** Query and onQueryChange callback.
  - **Outputs:** Query updates/clear.
  - **Dependencies:** Compose Material3, R.drawable search/close.
  - **Consumed by:** AbsRecyclerViewFragment ComposeView, QuickSearchHost in detail/secondary screens.
  - **Related files:** QuickSearchable.kt, AbsRecyclerViewFragment.kt, QuickSearchSupport.kt.
  - **Notes:** No filtering logic; the fragment/adapter owns data (ADR-044).


- `ui/screen/settings/SettingsSearchIndex.kt` — Public settings index and filter.
  - **Purpose:** Index preference titles/summaries across all SettingsScreen XML files.
  - **Responsibilities:** Parse preference XML with Resources.getXml; resolve title/summary string resources and key attributes; filter case-insensitively; group/order by SettingsScreen.
  - **Inputs:** Context, query.
  - **Outputs:** SettingsSearchResult list.
  - **Dependencies:** Android Resources/XmlResourceParser, SettingsScreen.
  - **Consumed by:** SettingsSearchDialog.
  - **Related files:** SettingsSearchDialog.kt, SettingsScreen.kt, res/xml/preferences_screen_*.
  - **Notes:** Public API only; does not use private PreferenceInflater (ADR-044).


- `ui/screen/settings/SettingsSearchDialog.kt` — Compose settings-search sheet.
  - **Purpose:** Search settings and navigate to a matching section.
  - **Responsibilities:** Query field, grouped results by section title, empty message, fragment-result selection, legacy-key-compatible result keys.
  - **Inputs:** SettingsSearchIndex, user query.
  - **Outputs:** Fragment result (screen name + preference key) to SettingsFragment.
  - **Dependencies:** Compose Material3, SsilencceTheme, BottomSheetDialogFragment, SettingsSearchIndex.
  - **Consumed by:** SettingsFragment menu action.
  - **Related files:** SettingsSearchIndex.kt, SettingsFragment.kt, PreferencesScreenFragment.kt.
  - **Notes:** Selecting a result navigates to the section; scrolling is best-effort (ADR-044).


- `util/backup/BackupArchivePolicy.kt` - Pure archive layout, merge and named-queue planning rules for backup/restore types.
  - **Purpose:** Keep AA-09 classification and merge semantics testable without Android or a device.
  - **Responsibilities:** Define archive paths for playlists, favorites, settings, lyrics, artist images, play counts, named queues and track positions; classify ZIP entries; preserve legacy `Playlists/` favorites; merge play-count counters monotonically; accept only newer track positions; plan named-queue id/name/cap handling with `QueueNamePolicy`; expose helper checks for custom artist entries.
  - **Inputs:** ZIP entry name/directory flag, play-count counters, current/incoming position timestamps, queue id/name and existing id-to-name map.
  - **Outputs:** `RestoreEntryKind`, merged `PlayCountCounters`, apply boolean, `NamedQueueRestorePlan` or null.
  - **Dependencies:** `QueueNamePolicy`, `RestoreEntryKind`; Kotlin stdlib only; no Android/AndroidX imports.
  - **Consumed by:** `BackupHelper`, the compiled policy probe.
  - **Related files:** `util/BackupHelper.kt`, `util/RestoreSummary.kt`, `data/local/repository/QueueNamePolicy.kt`, `util/backup/BackupEntryModels.kt`.
  - **Notes:** Exact JSON paths avoid accepting arbitrary nested files; directories are unrecognized. Unknown fields remain tolerated by the writer's `Json` configuration. See ADR-048.

- `util/backup/BackupEntryModels.kt` - Serializable DTOs and entity mappings for AA-09 restore types.
  - **Purpose:** Keep the backup JSON schema stable and independent of Room annotations.
  - **Responsibilities:** `PlayCountBackupEntry` (full song snapshot plus timePlayed/playCount/skipCount), `NamedQueueBackupEntry` (id/name/timestamps/resume/repeat/shuffle/media ids), `TrackPositionBackupEntry` (mediaId/positionMs/updatedAt); entity-to-DTO and DTO-to-entity mappings, including ordered named-queue items.
  - **Inputs:** `PlayCountEntity`, `NamedQueueWithItems`, `TrackPositionEntity`.
  - **Outputs:** DTO lists and entities.
  - **Dependencies:** kotlinx.serialization `@Serializable`, Room entity types.
  - **Consumed by:** `BackupHelper` creation/restore.
  - **Related files:** `util/BackupHelper.kt`, `util/backup/BackupArchivePolicy.kt`, the three Room entities.
  - **Notes:** Optional song fields use defaults so future/older JSON remains readable with `ignoreUnknownKeys`; no Room schema change. See ADR-048.

- `util/backup/BackupSharePolicy.kt` - Pure staging rules for the manual backup share copy.
  - **Purpose:** Keep naming, size, containment and stale-entry rules testable without Android.
  - **Responsibilities:** Sanitize SAF display names (separators/NUL/leading dots/length/`.bmgbak`); validate 1..256 MiB; canonical containment strictly inside the staging directory; detect `.backup-share-*.tmp`; select stale entries to delete.
  - **Inputs:** Raw display name, extension, sizes, canonical paths, directory entry names.
  - **Outputs:** Safe file name, validity booleans, stale-name list.
  - **Dependencies:** Kotlin stdlib only; no Android or AndroidX imports.
  - **Consumed by:** BackupShareStaging, the compiled policy probe.
  - **Related files:** BackupShareStaging.kt, preferences_screen_advanced.xml (no share-existing row).
  - **Notes:** No Android imports, so it can be compiled and probed on the JVM without Gradle or a device.

- `util/backup/BackupShareStaging.kt` — Copies a successful manual SAF backup into a private cache share slot.
  - **Purpose:** Expose the just-written `.bmgbak` through the existing FileProvider without changing the archive writer or scheduled backups.
  - **Responsibilities:** `cacheDir/backup_share` directory; SAF display-name resolution; zero/size/free-space preflight; cancellable copy; hidden temp file; atomic replace with non-atomic fallback; single-slot stale cleanup; process-wide staging Mutex; `isShareable`/`isStagedFile` existence, size and canonical containment checks; FileProvider URI creation.
  - **Inputs:** Context, SAF document Uri.
  - **Outputs:** `StagedBackup(uri, file, displayName)` or null; logs; no toasts.
  - **Dependencies:** FileProvider, DocumentFile, `fileProviderAuthority`, `getFormattedFileName`, BackupHelper.BACKUP_EXTENSION, BackupSharePolicy, kotlinx coroutines.
  - **Consumed by:** PreferencesScreenFragment manual backup callback; BackupShareDialogFragment validation.
  - **Related files:** BackupSharePolicy.kt, BackupShareDialogFragment.kt, res/xml/provider_paths.xml, util/BackupHelper.kt.
  - **Notes:** Never touches the SAF source or archive contents. A failed stage keeps the previous slot intact; a successful stage replaces it and deletes other entries. No scheduled worker calls this object. The staging Mutex does not cover the pre-existing `filesDir/Playlists` archive-creation race.

- `util/backup/ScheduledBackupPreferences.kt` — Scheduled-backup settings model and store.
  - **Purpose:** Persist opt-in schedule, destination and last-run status.
  - **Responsibilities:** Frequency enum (daily/weekly/30-day); last-result enum; state data class; dedicated SharedPreferences store; write settings, record success/failure.
  - **Inputs:** Settings values, destination Uri/name, result timestamps/errors.
  - **Outputs:** ScheduledBackupState, persisted preferences.
  - **Dependencies:** SharedPreferences, Uri, string labels.
  - **Consumed by:** ScheduledBackupController, Worker, Preference, Dialog.
  - **Related files:** ScheduledBackupScheduler.kt, ScheduledBackupController.kt, ScheduledBackupPreference.kt.
  - **Notes:** The 30-day option is labeled “Every 30 days”; it is a fixed cadence, not a calendar month (ADR-043).


- `util/backup/ScheduledBackupScheduler.kt` — WorkManager scheduling for automatic backups.
  - **Purpose:** Enqueue/cancel the single unique periodic backup.
  - **Responsibilities:** battery-not-low + storage-not-low constraints; optional charging; ExistingPeriodicWorkPolicy.UPDATE; unique name `scheduled_backup`; fixed-duration epoch window keys matching the daily/weekly/30-day WorkManager intervals so DST cannot suppress a due run.
  - **Inputs:** Context, frequency, chargingOnly.
  - **Outputs:** Unique PeriodicWorkRequest; window key strings.
  - **Dependencies:** WorkManager, ScheduledBackupWorker/Frequency.
  - **Consumed by:** ScheduledBackupController.
  - **Related files:** ScheduledBackupWorker.kt, ScheduledBackupPreferences.kt, ScheduledBackupController.kt.
  - **Notes:** First run is eligible immediately after enable; timing is inexact by platform design (ADR-043).


- `util/backup/ScheduledBackupWorker.kt` — CoroutineWorker writing one scheduled backup.
  - **Purpose:** Create a timestamped `.bmgbak` in the persisted SAF tree.
  - **Responsibilities:** Check enabled/window/permission; create DocumentFile; call BackupHelper.createBackupFile; delete partial document on failure/cancel; record success/error; return success to avoid retry storms.
  - **Inputs:** Worker parameters, ScheduledBackupStore, persisted tree Uri.
  - **Outputs:** Backup document, last-run preferences, Result.
  - **Dependencies:** WorkManager, DocumentFile, BackupHelper, ScheduledBackupStore.
  - **Consumed by:** WorkManager.
  - **Related files:** ScheduledBackupScheduler.kt, ScheduledBackupPreferences.kt, BackupHelper.kt.
  - **Notes:** One backup per schedule window; no fallback destination; destination-volume free space is not reliably queryable, so write failures are reported (ADR-043).


- `util/backup/ScheduledBackupController.kt` — Settings/WorkManager facade for scheduled backups.
  - **Purpose:** Save configuration, persist SAF permission and reconcile the unique work.
  - **Responsibilities:** StateFlow of store state; rememberDestination/takePersistableUriPermission; configure with MissingFolder/PermissionDenied results; schedule/cancel; destination-name resolution.
  - **Inputs:** Context, enabled/frequency/chargingOnly/destination.
  - **Outputs:** ConfigureResult, ScheduledBackupState.
  - **Dependencies:** ScheduledBackupStore, Scheduler, DocumentFile.
  - **Consumed by:** ScheduledBackupDialogFragment.
  - **Related files:** ScheduledBackupPreferences.kt, ScheduledBackupScheduler.kt, ScheduledBackupDialogFragment.kt.
  - **Notes:** Enabling without a usable destination is rejected; disabling always cancels work (ADR-043).


- `ui/screen/settings/backup/ScheduledBackupPreference.kt` — Advanced-settings row for scheduled backups.
  - **Purpose:** Expose scheduled-backup configuration in settings.
  - **Responsibilities:** Open the Compose dialog; refresh summary on shared-preference changes; show enabled/frequency/folder/last failure.
  - **Inputs:** ScheduledBackupStore state, preference context.
  - **Outputs:** Dialog launch; dynamic summary.
  - **Dependencies:** Preference, SharedPreferences listener, ScheduledBackupStore.
  - **Consumed by:** preferences_screen_advanced.xml.
  - **Related files:** ScheduledBackupDialogFragment.kt, ScheduledBackupPreferences.kt.
  - **Notes:** Uses the custom preference directly, so no fragment code is required (ADR-043).


- `ui/screen/settings/backup/BackupShareDialogFragment.kt` — Compose confirmation sheet for sharing a just-created manual backup.
  - **Purpose:** Offer **Backup saved**, a disclosure, **Share backup** and **Done** after a successful manual backup.
  - **Responsibilities:** Expanded BottomSheetDialogFragment + ComposeView/SsilencceTheme; pass URI/file path/display name as arguments; validate the staged file through BackupShareStaging.isShareable; build `ACTION_SEND` with EXTRA_STREAM, ClipData and `FLAG_GRANT_READ_URI_PERMISSION`; chooser titled **Share backup**; MIME `application/zip`; dismiss on launch; show clear toasts for missing/oversized file, no target and launch failure.
  - **Inputs:** StagedBackup URI, file path and display name from PreferencesScreenFragment.
  - **Outputs:** System share chooser; dismiss; error toasts/log.
  - **Dependencies:** Compose Material3, SsilencceTheme, BottomSheetDialogFragment, BackupShareStaging.
  - **Consumed by:** PreferencesScreenFragment.showManualBackupResult.
  - **Related files:** BackupShareStaging.kt, PreferencesScreenFragment.kt, res/values/strings.xml, res/xml/provider_paths.xml.
  - **Notes:** Shown only for manual backups. The chooser has no result callback, so cancel is silent. The sheet never deletes the SAF backup or creates a backup.

- `ui/screen/settings/backup/ScheduledBackupDialogFragment.kt` — Compose scheduled-backup configuration sheet.
  - **Purpose:** Let users enable, choose frequency/folder/charging and see last status.
  - **Responsibilities:** SAF OpenDocumentTree picker; persist permission; switches/radio rows; last-run/error display; save via Controller; validation messages.
  - **Inputs:** ScheduledBackupController, user selections.
  - **Outputs:** Configure call; dismiss/error state.
  - **Dependencies:** Compose Material3, SsilencceTheme, BottomSheetDialogFragment, ScheduledBackupController, DocumentFile.
  - **Consumed by:** ScheduledBackupPreference.
  - **Related files:** ScheduledBackupController.kt, ScheduledBackupPreference.kt.
  - **Notes:** Compose UI; controller calls remain context-scoped, no Koin binding (ADR-043).

### 2026-11 additions and integration updates

#### Core models, sorting and database

- `core/model/ArtistNamePolicy.kt` — Pure album-artist fallback policy.
  - **Purpose:** Resolve a display artist when the song artist is empty or `<unknown>`.
  - **Responsibilities:** Return the album artist only when the user preference is enabled; leave known artist names untouched.
  - **Inputs:** Artist tag, album-artist tag, preference flag.
  - **Outputs:** Resolved artist name or null.
  - **Dependencies:** None.
  - **Consumed by:** Song display extensions, navigation and `RealArtistRepository`.
  - **Related files:** `data/model/Song.kt`, `extensions/media/DataExt.kt`.
  - **Notes:** The fallback is default-off; no Room change.

- `core/model/MediaItemExtras.kt` — Shared media-item metadata extras.
  - **Purpose:** Carry album id and disc number through Media3 items.
  - **Responsibilities:** Define the album-id and disc-number Bundle keys used by the gapless-album policy.
  - **Inputs:** None.
  - **Outputs:** String constants.
  - **Dependencies:** None.
  - **Consumed by:** `data/model/Song.kt`, `playback/crossfade/GaplessAlbumDetector.kt`.
  - **Related files:** `Song.toMediaItem`.
  - **Notes:** Extras survive queue persistence and upcoming-timeline wrappers.

- `core/model/action/QuickAccessAction.kt` — Configurable Now Playing shortcut enum.
  - **Purpose:** Model the seven optional quick-access shortcuts.
  - **Responsibilities:** Icon/title resources plus comma-separated preference parsing and serialization.
  - **Inputs:** Stored preference string.
  - **Outputs:** Ordered action list.
  - **Dependencies:** `R.string`/`R.drawable`.
  - **Consumed by:** `Preferences`, `AbsPlayerFragment`, `AuroraPlayerScreen`, `QuickAccessPreferenceDialog`.
  - **Related files:** `ui/component/compose/QuickAccessRow.kt`, `NowPlayingAction.kt`.
  - **Notes:** An empty list renders no row, preserving the default layout.

- `core/model/equalizer/EqBandFrequencies.kt` — ISO band-center tables.
  - **Purpose:** Supply 3/5/10/15/31 band layouts and the legacy 32-band migration table.
  - **Responsibilities:** Center frequencies, `closestBandCount`, log-spaced fallback for foreign layouts.
  - **Inputs:** Requested band count.
  - **Outputs:** Frequency arrays.
  - **Dependencies:** Kotlin stdlib.
  - **Consumed by:** `EqBandResampler`, `EqBandCapabilities`, `DynamicsProcessingEngine`.
  - **Related files:** `EqBandResampler.kt`, `EqBandCapabilities.kt`.
  - **Notes:** 31 replaces the legacy 32-band table; the old table remains migration source data.

- `core/model/equalizer/EqBandResampler.kt` — Log-frequency gain interpolation.
  - **Purpose:** Move a gain curve between band layouts without resetting it.
  - **Responsibilities:** Interpolate gains in log-frequency space, clamp at the source edges, support 32→31 migration and display fallback.
  - **Inputs:** Source/target frequencies and gains.
  - **Outputs:** Resampled gains.
  - **Dependencies:** `EqBandFrequencies`.
  - **Consumed by:** `EqualizerManager`, `EqBandCapabilities`.
  - **Related files:** `EqBandFrequencies.kt`, `EqState.kt`.
  - **Notes:** Pure Kotlin; no Android effects dependency.

- `core/model/equalizer/ReverbState.kt` — Reverb preset and parameter model.
  - **Purpose:** Represent the six `EnvironmentalReverb` presets plus Custom.
  - **Responsibilities:** Millibel/millisecond/per-mille parameters, preset bundles and current state.
  - **Inputs:** DataStore values.
  - **Outputs:** `ReverbSettings`/`ReverbState`.
  - **Dependencies:** None.
  - **Consumed by:** `ReverbController`, `ReverbControls`, `EqualizerManager`.
  - **Related files:** `playback/effects/ReverbController.kt`.
  - **Notes:** No new preference file; keys live in the equalizer DataStore.

- `core/model/equalizer/SystemEqualizer.kt` — System/OEM equalizer hand-off contract.
  - **Purpose:** Describe the external effect-control panel.
  - **Responsibilities:** Panel `Intent` (optional audio session), open/close broadcast intents and panel availability probing.
  - **Inputs:** Context, optional session id.
  - **Outputs:** Intents and availability.
  - **Dependencies:** Android `Intent`/`PackageManager`.
  - **Consumed by:** `EqualizerManager`, `EqualizerViewModel`, `SystemEqualizerCard`.
  - **Related files:** `EqEngineMode.kt`.
  - **Notes:** The manifest already declares the panel action; no new permission.

- `core/model/filesystem/FolderInfo.kt` — Immutable folder-info snapshot.
  - **Purpose:** Carry path, direct/total counts, sub-folders, duration/size and timestamps.
  - **Responsibilities:** Plain data holder for the folder info dialog.
  - **Inputs:** Repository queries and filesystem timestamps.
  - **Outputs:** `FolderInfo`.
  - **Dependencies:** None.
  - **Consumed by:** `FolderInfoProvider`, `FolderInfoDialog`.
  - **Related files:** `util/FolderInfoProvider.kt`.
  - **Notes:** Timestamp resolution stays in the provider under `runCatching`.

- `core/model/lockscreen/LockScreenWallpaper.kt` — Lock-screen wallpaper enum.
  - **Purpose:** Select blurred cover or a custom image.
  - **Responsibilities:** Stable stored values.
  - **Inputs:** Preference string.
  - **Outputs:** Wallpaper mode.
  - **Dependencies:** None.
  - **Consumed by:** `LockScreenPlayerScreen`, `LockScreenPreferencesFragment`.
  - **Related files:** `util/Preferences.kt`.
  - **Notes:** Custom images use a persisted `OpenDocument` URI.

- `core/model/menu/MenuCustomizationGroup.kt` — Per-entity menu catalog and state.
  - **Purpose:** Declare configurable entity menu actions and their saved order.
  - **Responsibilities:** `MenuCustomizationItem`, `MenuCustomizationState` CSV serialization with `-` hidden prefix, group-to-preference/resource mapping and normalization of new catalog entries.
  - **Inputs:** Saved preference string.
  - **Outputs:** Normalized state.
  - **Dependencies:** `R.id`/`R.string`.
  - **Consumed by:** `Preferences.menuCustomization`, `MenuCustomizationFilter`, `MenuCustomizationDialog`.
  - **Related files:** `ui/component/menu/MenuCustomizationFilter.kt`.
  - **Notes:** Stable string keys are stored instead of resource ids.

- `core/model/player/NotificationButton.kt` — Notification button model and layout policy.
  - **Purpose:** Define available notification actions and keep compact/expanded layouts valid.
  - **Responsibilities:** Stable values, default layout, parse/serialize, slot editing, subset/limit normalization.
  - **Inputs:** Preference strings.
  - **Outputs:** `NotificationButtonLayout`.
  - **Dependencies:** Android string resources.
  - **Consumed by:** `SsilencceMediaNotificationProvider`, `NotificationButtonLayoutDialog`, `Preferences`.
  - **Related files:** `NotificationStyle.kt`, `preferences_screen_playback.xml`.
  - **Notes:** Compact is capped at 3 and must be a subset of expanded (max 5).

- `core/model/player/NotificationStyle.kt` — Notification styles and per-style policy.
  - **Purpose:** Describe System/Small/BigSimple/BigAdvanced presentations and backgrounds.
  - **Responsibilities:** Stable style/background values, `isCustom`/`showsSeekBar`/`showsUpcomingSongs`, compact/expanded/big-advanced button selection.
  - **Inputs:** `NotificationButtonLayout`, availability predicate.
  - **Outputs:** Ordered `NotificationButton` lists.
  - **Dependencies:** `NotificationButton`, string resources.
  - **Consumed by:** `Preferences`, `CustomLayoutMediaNotificationProvider`, `NotificationBackgroundColorDialog`.
  - **Related files:** `NotificationButton.kt`.
  - **Notes:** System is the default; Android 13+ may ignore custom layouts.

- `core/model/shuffle/ShuffleBehavior.kt` — Shuffle order behavior.
  - **Purpose:** Choose between all-songs and below-current shuffle.
  - **Responsibilities:** Map the `shuffle_behavior` preference to `AllSongs`/`BelowCurrent`.
  - **Inputs:** Preference string.
  - **Outputs:** Behavior enum.
  - **Dependencies:** None.
  - **Consumed by:** `Preferences`, `PlaybackService`, `PersistentStorage`.
  - **Related files:** `ShuffleModeState.kt`, `ImprovedShuffleOrder.kt`.
  - **Notes:** One list preference keeps the modes mutually exclusive.

- `core/model/shuffle/ShuffleModeState.kt` — Persisted three-state shuffle.
  - **Purpose:** Model Off / Shuffle / Randomize.
  - **Responsibilities:** Stable string values used by playback-state preferences and IPC.
  - **Inputs:** Stored string.
  - **Outputs:** State enum.
  - **Dependencies:** None.
  - **Consumed by:** `PlaybackService`, `PersistentStorage`, `PlayerViewModel`, player/queue UI.
  - **Related files:** `ShuffleBehavior.kt`, `RandomizeShuffleOrder.kt`.
  - **Notes:** Persisted as `shuffle_state` with the legacy boolean fallback.

- `core/model/tag/TagEditCover.kt` — Batch cover model.
  - **Purpose:** Represent Keep/Replace/Delete cover operations.
  - **Responsibilities:** `CoverOperation`, `CoverWriteStatus` and `hasChange` for the writer/UI summary.
  - **Inputs:** Picked `Bitmap`.
  - **Outputs:** Per-file cover status.
  - **Dependencies:** `android.graphics.Bitmap`.
  - **Consumed by:** `MetadataWriter.writeBatch`, `BatchTagEditorViewModel`, `BatchTagEditorScreen`, `BatchTagEditorCover`.
  - **Related files:** `TagEditPlan.kt`, `TagEditReport.kt`.
  - **Notes:** Cover replace/delete applies to every written file.

- `core/sort/SongPlayStatisticsCache.kt` — Cached play-statistics snapshot.
  - **Purpose:** Back the date-last-played/most-played/least-played sort keys without per-row queries.
  - **Responsibilities:** Five-second in-memory snapshot from one joined `PlayCountDao` query.
  - **Inputs:** `PlayCountDao`.
  - **Outputs:** Song id → play count/time played.
  - **Dependencies:** `PlayCountDao`, `SongPlayStatistics`.
  - **Consumed by:** `SortMode`, list adapters.
  - **Related files:** `data/local/room/SongPlayStatistics.kt`.
  - **Notes:** No Flow; callers request a snapshot per sort.

- `data/model/ExtraFormatUriStore.kt` — Negative-id URI resolver.
  - **Purpose:** Map synthetic extra-format song ids to `file://` URIs.
  - **Responsibilities:** Process-wide id→URI registration and lookup.
  - **Inputs:** `ExtraFormatLibrary` population.
  - **Outputs:** URI for a negative song id.
  - **Dependencies:** `android.net.Uri`.
  - **Consumed by:** `Song.uri`, repositories and playback.
  - **Related files:** `data/local/extra/ExtraFormatLibrary.kt`.
  - **Notes:** Checked before building a MediaStore content URI.

- `data/model/lyrics/LyricsSourceSelection.kt` — Per-song lyrics source choice.
  - **Purpose:** Model Automatic/Embedded/Downloaded/File overrides.
  - **Responsibilities:** Carry choice and picked-document metadata, expose title/summary resources.
  - **Inputs:** `LyricsSourceChoice`, file URI/name.
  - **Outputs:** Immutable selection.
  - **Dependencies:** Compose `@Immutable`, string resources.
  - **Consumed by:** `LyricsSourceStore`, `LyricsRepository`, `LyricsViewModel`, `LyricsScreen`, `LyricsSourceDialog`, `LyricsEditorScreen`.
  - **Related files:** `LyricsSource.kt`, `LyricsSourceStore.kt`.
  - **Notes:** Automatic keeps file → embedded → downloaded priority.

- `data/model/notes/SongNotes.kt` — Song notes model.
  - **Purpose:** Hold one notes file's content.
  - **Responsibilities:** `SongNotes(songId, notes, bookmarks)` and `SongBookmark(timestampMs, text)`, normalize positions and expose `hasContent`.
  - **Inputs/Outputs:** Plain Kotlin data.
  - **Dependencies:** None.
  - **Consumed by:** `SongNotesRepository`, `SongNotesCodec`, `SongNotesViewModel`, `SongNotesScreen`.
  - **Related files:** `SongNotesCodec.kt`.
  - **Notes:** No Room entity.

- `data/model/notes/SongNotesCodec.kt` — Human-readable notes file codec.
  - **Purpose:** Encode/decode the `#` header, `[Notes]` and `[Bookmarks]` sections.
  - **Responsibilities:** Parse/format `m:ss`/`h:mm:ss` timestamps, tolerate missing sections, treat section-less files as plain notes, build share text.
  - **Inputs:** File text; song id/title/artist, notes, bookmarks.
  - **Outputs:** Parsed content, encoded text, share text.
  - **Dependencies:** None (pure Kotlin).
  - **Consumed by:** `SongNotesRepository`, `SongNotesViewModel`, `SongNotesFragment`.
  - **Related files:** `SongNotes.kt`.
  - **Notes:** Import unions bookmarks by position + text.

- `data/local/BatchWriteJournal.kt` — Session tag-write backup and restore.
  - **Purpose:** Preserve original bytes before destructive batch tag writes and support session undo.
  - **Responsibilities:** Copy to `cacheDir/tag-write-backup/<uuid>/<songId>.bak`, fail closed on copy/size/cap failures (2 GiB/file, 4 GiB/session), verify/restore with `"rwt"` and restored-size check, notify/scan, discard entries and prune sessions older than seven days.
  - **Inputs:** `EditTarget.Content`, context cache dir.
  - **Outputs:** `Entry` tokens and restore results.
  - **Dependencies:** `ContentResolver`, `MediaScannerConnection`.
  - **Consumed by:** `MetadataWriter.writeBatch`, `BatchTagEditorViewModel`.
  - **Related files:** `core/model/tag/TagEditCover.kt`, ADR-051.
  - **Notes:** Transient cache storage; no hash sidecar or process-death recovery.

- `data/local/LibraryRescan.kt` — Chosen-folder MediaStore rescan.
  - **Purpose:** Refresh the user's library folders after folder/copy operations or setup.
  - **Responsibilities:** Resolve whitelist (when non-empty/valid) or storage roots plus the main folder, call `MediaScannerConnection.scanFile`, serialize concurrent scans, debounced background variant, permission check and 120 s guard.
  - **Inputs:** Context, optional overrides, preferences, whitelist paths.
  - **Outputs:** Scan requests and requested-path count.
  - **Dependencies:** MediaScannerConnection, StorageUtil, Preferences, InclExclDao, coroutines.
  - **Consumed by:** `SetupWizardViewModel`, library preferences, `MediaMountRescanReceiver`.
  - **Related files:** `MediaMountRescanReceiver.kt`, `util/Preferences.kt`.
  - **Notes:** `/` is never scanned; no polling.

- `data/local/MediaMountRescanReceiver.kt` — Debounced mount rescan receiver.
  - **Purpose:** Trigger the library rescan when a storage volume mounts.
  - **Responsibilities:** Filter `ACTION_MEDIA_MOUNTED`, require the auto-rescan preference and audio-read permission, hand off to `LibraryRescan.scheduleDebounced`.
  - **Inputs:** Mount broadcast, preferences, permission state.
  - **Outputs:** Debounced rescan request.
  - **Dependencies:** `LibraryRescan`, `Preferences`.
  - **Consumed by:** `App` dynamic registration.
  - **Related files:** `App.kt`, `LibraryRescan.kt`.
  - **Notes:** Deliberately not manifest-registered; runs only while the process is alive.

- `data/local/audio/clip/AudioClipFormat.kt` — Audio clip models.
  - **Purpose:** Define clip formats and range/failure/result models.
  - **Responsibilities:** Map extractor MIME types to lossless output formats, gate OGG on API 30, carry duration and unsupported reasons.
  - **Inputs:** Extractor MIME, duration, selected range.
  - **Outputs:** `AudioClipFormat`, `AudioClipInspection`, `AudioClipRange`, `AudioClipFailure`, `AudioClipExportResult`.
  - **Dependencies:** None beyond Android APIs.
  - **Consumed by:** `AudioClipExporter`, `AudioCutterViewModel`, `AudioCutterScreen`, `RingtoneClipStore`.
  - **Related files:** `AudioClipNaming.kt`.
  - **Notes:** MediaMuxer format constants are local ints so older APIs never resolve the OGG constant.

- `data/local/audio/clip/AudioClipNaming.kt` — Clip naming helper.
  - **Purpose:** Build sanitized `<title> (<start>-<end>s).<ext>` names.
  - **Responsibilities:** Title/extension sanitization and range formatting.
  - **Inputs:** Song title, range, format.
  - **Outputs:** File name/title.
  - **Dependencies:** Kotlin stdlib.
  - **Consumed by:** `AudioCutterScreen`, `AudioCutterViewModel`.
  - **Related files:** `AudioClipFormat.kt`.
  - **Notes:** Used for both SAF suggested names and ringtone copies.

- `data/local/audio/clip/AudioClipExporter.kt` — Lossless range exporter.
  - **Purpose:** Copy a selected range without re-encoding.
  - **Responsibilities:** Probe the first audio track, trim AAC/AMR-era containers through `MediaMuxer`, route MP3 to `Mp3RawTrimmer`, write SAF output through a cache temp file and copy for legacy ringtone folders.
  - **Inputs:** Source URI, range, destination stream/file, format.
  - **Outputs:** `Success(durationMs, bytes)` or typed failure.
  - **Dependencies:** `MediaExtractor`, `MediaMuxer`, `Mp3RawTrimmer`, IO dispatcher.
  - **Consumed by:** `AudioCutterViewModel`, `RingtoneClipStore`.
  - **Related files:** `AudioClipFormat.kt`, `Mp3RawTrimmer.kt`.
  - **Notes:** Cancellation deletes temp/target files.

- `data/local/audio/clip/Mp3RawTrimmer.kt` — MPEG frame-aligned trimmer.
  - **Purpose:** Trim MP3 byte-for-byte while preserving ID3v2.
  - **Responsibilities:** Copy ID3v2 verbatim, resynchronize over junk, accumulate frame durations, drop leading Xing/Info/VBRI metadata frames, tolerate truncated trailing frames.
  - **Inputs:** MP3 bytes/stream, range.
  - **Outputs:** Trimmed MP3 output.
  - **Dependencies:** Kotlin/Java IO.
  - **Consumed by:** `AudioClipExporter.trimMp3`.
  - **Related files:** `AudioClipExporter.kt`.
  - **Notes:** MP3 inside MP4 is sniffed and rejected as unsupported.

- `data/local/audio/clip/RingtoneClipStore.kt` — System sound publisher.
  - **Purpose:** Publish clips as ringtone/notification/alarm sounds and set defaults.
  - **Responsibilities:** MediaStore insert/publish with `IS_PENDING` and matching flags on API 29+, legacy public-folder writes plus media scan on API 26–28, `WRITE_SETTINGS`/storage checks and `RingtoneManager.setActualDefaultRingtoneUri`.
  - **Inputs:** Clip, tone kind, destination.
  - **Outputs:** `RingtoneSetResult`.
  - **Dependencies:** MediaStore, RingtoneManager, MediaScannerConnection.
  - **Consumed by:** `AudioCutterViewModel`.
  - **Related files:** `AudioClipExporter.kt`.
  - **Notes:** Copies are published, not moved; the existing default sound is never deleted.

- `data/local/extra/ExtraFormatLibrary.kt` — Extra-format JSON cache and Song mapping.
  - **Purpose:** Persist supplementary scanner results and resolve them to `Song`.
  - **Responsibilities:** JSON cache, deterministic negative ids, `ExtraFormatUriStore` population, refresh rules.
  - **Inputs:** Scanner output, cache file.
  - **Outputs:** `ExtraFormatTrack`/`Song` list.
  - **Dependencies:** kotlinx.serialization, `ExtraFormatUriStore`.
  - **Consumed by:** `SongRepository`, `ExtraFormatScanner`, `App`.
  - **Related files:** `ExtraFormatScanner.kt`, `ExtraFormatFileProbe.kt`.
  - **Notes:** Albums/artists use separate negative synthetic ids and are not merged into MediaStore aggregates.

- `data/local/extra/ExtraFormatScanner.kt` — Opt-in supplementary scanner.
  - **Purpose:** Find AIFF/DSF/DFF files MediaStore does not index.
  - **Responsibilities:** Scan allowed folders, six-hour freshness, blacklist/duration filters, ghost-folder filtering and cache refresh.
  - **Inputs:** Context, preferences, whitelist, ghost folders.
  - **Outputs:** Updated `ExtraFormatLibrary` cache.
  - **Dependencies:** `ExtraFormatFileProbe`, `MetadataReader`, `ExtraFormatSupport`, IO dispatcher.
  - **Consumed by:** `App`, `LibraryRescan`, library settings.
  - **Related files:** `ExtraFormatLibrary.kt`, `merge-extra-formats` decisions.
  - **Notes:** Off by default; APE/WMA are excluded while unplayable.

- `data/local/extra/ExtraFormatFileProbe.kt` — Header-only duration fallback.
  - **Purpose:** Read AIFF/DSF/DFF duration when TagLib cannot report it.
  - **Responsibilities:** Parse container headers without full decode.
  - **Inputs:** File path/descriptor.
  - **Outputs:** Duration milliseconds or null.
  - **Dependencies:** Java IO.
  - **Consumed by:** `ExtraFormatScanner`.
  - **Related files:** `ExtraFormatLibrary.kt`.
  - **Notes:** Header-only; no audio decoding.

- `data/local/lyrics/LyricsSourceStore.kt` — Per-song lyrics source JSON store.
  - **Purpose:** Persist source choices without a Room migration.
  - **Responsibilities:** Lazy load, mutex-protected get/put, atomic temp-file write, tolerant decoding.
  - **Inputs:** Song id, `LyricsSourceSelection`.
  - **Outputs:** Selection/automatic default; updated JSON.
  - **Dependencies:** kotlinx.serialization, coroutines, File.
  - **Consumed by:** `RealLyricsRepository`.
  - **Related files:** `LyricsSourceSelection.kt`.
  - **Notes:** Automatic removes the entry; corrupt documents load as empty.

- `data/local/lyrics/lrc/LrcTools.kt` — LRC timestamp helpers.
  - **Purpose:** Convert plain lines to/from `[mm:ss.xx]` LRC text.
  - **Responsibilities:** Format/parse 1–3 fraction digits, strip existing timestamps, skip metadata/TTML markup, serialize stamped lines in order.
  - **Inputs:** Lyric text, stamped lines, playback millis.
  - **Outputs:** LRC string or stamped-line list.
  - **Dependencies:** `java.util.Locale`.
  - **Consumed by:** `LyricsSyncEditor`, `LyricsEditorScreen`.
  - **Related files:** `LrcLyricsParser.kt`.
  - **Notes:** Sorting on write keeps third-party players compatible.

- `data/local/notes/SongNotesRepository.kt` — File-backed song notes and bookmarks.
  - **Purpose:** Read/write per-song notes as user-visible text files without a schema change.
  - **Responsibilities:** Resolve `<song id> - <title>.txt`, sanitize titles, atomic writes, delete empty files, clean old-name duplicates, append one-tap bookmarks, list/validate backup files.
  - **Inputs:** Context, Song, notes text, bookmark list.
  - **Outputs:** Notes, append result, file list, notes directory.
  - **Dependencies:** `SongNotesCodec`, `java.io.File`, IO dispatcher, Mutex.
  - **Consumed by:** `SongNotesViewModel`, `AbsPlayerFragment`, `BackupHelper`.
  - **Related files:** `SongNotes.kt`, `SongNotesCodec.kt`.
  - **Notes:** Falls back to `filesDir/notes`; only numeric-id files are backed up.

- `data/local/repository/MissingSongsCleanup.kt` — Missing-song cleanup result.
  - **Purpose:** Count rows removed by the maintenance action.
  - **Responsibilities:** Carry playlist-entry, play-count, play-event and history counts.
  - **Inputs:** DAO delete results.
  - **Outputs:** `MissingSongsCleanupResult`.
  - **Dependencies:** None.
  - **Consumed by:** `Repository.removeMissingSongs`, `RemoveMissingSongsDialog`.
  - **Related files:** `data/local/repository/Repository.kt`.
  - **Notes:** Never deletes files or MediaStore rows.

- `data/local/repository/NamedQueueMaintenancePolicy.kt` — Pure named-queue maintenance.
  - **Purpose:** Keep merge and cap-eviction rules testable.
  - **Responsibilities:** Append-merge media-id lists with optional duplicate filtering and pick the least-recently-used saved queue while protecting the active queue.
  - **Inputs:** Ordered ids, saved queue entities, protected id.
  - **Outputs:** Merged list or eviction candidate.
  - **Dependencies:** Kotlin stdlib.
  - **Consumed by:** `NamedQueueRepository`.
  - **Related files:** `NamedQueueRepository.kt`.
  - **Notes:** `NamedQueueEntity.updatedAt` is the recency signal.

- `data/local/room/SongPlayStatistics.kt` — Play-statistics projection.
  - **Purpose:** Carry id/playCount/timePlayed for sorting.
  - **Responsibilities:** Room query projection only.
  - **Inputs:** `PlayCountDao` query.
  - **Outputs:** Projection rows.
  - **Dependencies:** Room.
  - **Consumed by:** `SongPlayStatisticsCache`.
  - **Related files:** `PlayCountDao.kt`.
  - **Notes:** Not an entity; no schema change.

- `data/local/room/SongPlaybackParamEntity.kt` — Per-song speed/pitch entity.
  - **Purpose:** Store optional per-song tempo overrides.
  - **Responsibilities:** `song_id` primary key, `speed` and `pitch`; absence means global tempo.
  - **Inputs:** Values from `PlayerViewModel`/restore.
  - **Outputs:** Rows read on media-item transition.
  - **Dependencies:** Room.
  - **Consumed by:** `SongPlaybackParamDao`, `PlaybackService`, `PlayerViewModel`, `BackupHelper`.
  - **Related files:** `SsilencceDatabase.kt` (v9, MIGRATION_8_9).
  - **Notes:** Existing songs remain on the global tempo.

- `data/local/room/SongPlaybackParamDao.kt` — DAO for per-song playback parameters.
  - **Purpose:** Persist and read per-song overrides.
  - **Responsibilities:** Point lookup, full export, single/all upsert, delete one and clear.
  - **Inputs:** `songId`, entity lists.
  - **Outputs:** Stored overrides.
  - **Dependencies:** Room.
  - **Consumed by:** `PlaybackService`, `PlayerViewModel`, `BackupHelper`.
  - **Related files:** `SongPlaybackParamEntity.kt`, `MainModule.kt`.
  - **Notes:** No Flow; callers are lifecycle-owned coroutines.

#### Playback engine, cast and notification

- `playback/PlaybackDecoderRegistry.kt` — Last decoder/container snapshot.
  - **Purpose:** Report the decoder actually used for the current media id.
  - **Responsibilities:** Record audio format/decoder from `AnalyticsListener` and expose the latest snapshot.
  - **Inputs:** Media3 analytics events.
  - **Outputs:** Decoder/container info for song info.
  - **Dependencies:** Media3 common.
  - **Consumed by:** `PlaybackService`, `InfoViewModel`.
  - **Related files:** `ui/screen/info/InfoViewModel.kt`.
  - **Notes:** Falls back to `System MediaCodec` before playback.

- `playback/PlaybackFadeController.kt` — Play/pause volume ramps.
  - **Purpose:** Run 0–4000 ms fades and defer pause until fade-out completes.
  - **Responsibilities:** Arm silent start, ramp from actual playback, delay/commit pause, cancel on newer play/pause/transition, expose sleep-timer fade-out.
  - **Inputs:** Duration providers, `PlaybackVolumeController`, play/pause requests.
  - **Outputs:** Volume-ramp steps and deferred pause commits.
  - **Dependencies:** Coroutines, `SystemClock`, Media3 callbacks.
  - **Consumed by:** `PlaybackService`, `AdvancedForwardingPlayer`, `SleepTimer`.
  - **Related files:** `PlaybackVolumeController.kt`, `SleepTimer.kt`.
  - **Notes:** 40 ms steps on the service scope; 0 ms keeps immediate pause.

- `playback/PlaybackVolumeController.kt` — Single writer of player volume.
  - **Purpose:** Compose equalizer volume, audio-focus ducking and fade factors.
  - **Responsibilities:** Hold base/duck/fade/crossfade multipliers and write their product to `Player.volume`; expose `currentOutputVolume`.
  - **Inputs:** Equalizer volume, duck changes, fade/crossfade ramps.
  - **Outputs:** Player volume updates.
  - **Dependencies:** Media3 `Player`.
  - **Consumed by:** `PlaybackService`, `PlaybackFadeController`, `AudioFocusController`, `CrossfadeController`.
  - **Related files:** `PlaybackFadeController.kt`, `AudioFocusController.kt`.
  - **Notes:** All calls run on the player application thread (Main).

- `playback/RandomizeShuffleOrder.kt` — Dynamic random-next shuffle order.
  - **Purpose:** Implement randomize without materializing a permutation.
  - **Responsibilities:** Pick/cache a random destination per navigation, support below-current forward-only behavior, implement next/previous/first/last and clone operations.
  - **Inputs:** Timeline length, below-current flag, random source.
  - **Outputs:** ShuffleOrder indices.
  - **Dependencies:** Media3 `ShuffleOrder`, `kotlin.random.Random`.
  - **Consumed by:** `PlaybackService`, `PersistentStorage`.
  - **Related files:** `ShuffleStateHolder.kt`, `ShuffleBehavior.kt`.
  - **Notes:** No fixed order to persist; queue stays in timeline order.

- `playback/RepeatPolicy.kt` — Pure four-state repeat policy.
  - **Purpose:** Map Media3 repeat modes plus load-next-and-pause to one state.
  - **Responsibilities:** Off/All/One/LoadNextAndPause transitions, pause decision on automatic transitions, persistence mapping.
  - **Inputs:** Media3 repeat mode, active-policy flag, transition reason.
  - **Outputs:** Next state and pause decision.
  - **Dependencies:** Media3 `Player`.
  - **Consumed by:** `PlaybackService`, repeat controls.
  - **Related files:** `PlaybackService.kt`, `util/Preferences.kt`.
  - **Notes:** The fourth state is gated by its setting and persisted separately.

- `playback/ResumeAutoplayPolicy.kt` — Connect/resume/autoplay policy.
  - **Purpose:** Remember pause cause and evaluate resume/autoplay guards.
  - **Responsibilities:** `pausedByDisconnect`, resume-only-if-paused-by-disconnect, 5-second standard guard, strict background guard, pause/start/connection notifications.
  - **Inputs:** `Preferences`, `AppVisibility`, elapsed realtime.
  - **Outputs:** Resume/block decisions.
  - **Dependencies:** `util/Preferences.kt`, `util/AppVisibility.kt`, `SystemClock`.
  - **Consumed by:** `PlaybackService`.
  - **Related files:** `AdvancedForwardingPlayer.kt`.
  - **Notes:** Main-thread only; defaults leave all policies disabled.

- `playback/ShuffleCyclePolicy.kt` — Pure shuffle-button cycle.
  - **Purpose:** Cycle only through enabled shuffle states.
  - **Responsibilities:** Standard/randomize enablement and fallback to standard when both are disabled.
  - **Inputs:** Current state, enabled flags.
  - **Outputs:** Next state.
  - **Dependencies:** `ShuffleModeState`.
  - **Consumed by:** `PlaybackService`.
  - **Related files:** `ShuffleStateHolder.kt`.
  - **Notes:** The cycle never becomes a no-op.

- `playback/ShuffleStateHolder.kt` — Process-wide shuffle state.
  - **Purpose:** Let UI distinguish randomize from fixed order without IPC.
  - **Responsibilities:** Service-written state flow.
  - **Inputs:** `PlaybackService` state changes.
  - **Outputs:** Current `ShuffleModeState`.
  - **Dependencies:** Coroutines/StateFlow.
  - **Consumed by:** `PlaybackService`, `PlayerViewModel`.
  - **Related files:** `ShuffleModeState.kt`.
  - **Notes:** Same-process only; the custom event/query remains the controller contract.

- `playback/cast/CastPlayback.kt` — Flavor-agnostic Cast boundary.
  - **Purpose:** Keep GMS and non-GMS flavors compiling from one `main` source set.
  - **Responsibilities:** Expose availability, menu setup, device chooser, session-player factory and release; no Cast SDK types leak into `main`.
  - **Inputs:** Context, Activity, Menu, local Player.
  - **Outputs:** Local or `CastPlayer` facade.
  - **Dependencies:** Media3 `Player`.
  - **Consumed by:** `PlaybackService`, `AbsPlayerFragment`, `AuroraPlayerFragment`.
  - **Related files:** `src/cast/.../CastPlaybackImpl.kt`, `src/nocast/.../CastPlaybackProvider.kt`.
  - **Notes:** `CastPlaybackProvider` exists only in flavor source sets.

- `playback/crossfade/CrossfadeController.kt` — Dual-player crossfade state machine.
  - **Purpose:** Overlap the outgoing and incoming tracks with equal-power ramps.
  - **Responsibilities:** One-shot preload, secondary pre-buffer, ramps, main-player commit, handoff/release, abort on pause/seek/timeline/speed/queue changes, intercept manual next.
  - **Inputs:** Config, player state, incoming ReplayGain, mirrored processor state.
  - **Outputs:** Main-player seeks, volume multipliers, commit callback.
  - **Dependencies:** Media3 `ExoPlayer`, `PlaybackVolumeController`, `CrossfadePlayer`, `GaplessAlbumDetector`.
  - **Consumed by:** `PlaybackService`.
  - **Related files:** `AdvancedForwardingPlayer.kt`, `PlaybackFadeController.kt`.
  - **Notes:** Main/application looper only; one-shot handler messages, no polling.

- `playback/crossfade/CrossfadePlayer.kt` — Secondary pre-buffering player.
  - **Purpose:** Play the incoming track during the overlap.
  - **Responsibilities:** Mirror renderers (balance, mono, ReplayGain, ALAC, FFmpeg, extractors, skip-silence, float), own an audio-priority thread, share the main audio session id, run `WAKE_MODE_NONE` without focus.
  - **Inputs:** Context, audio session id, renderer options.
  - **Outputs:** `ExoPlayer` and processors.
  - **Dependencies:** `SsilencceRenderersFactory`, `ExtraExtractorsFactory`.
  - **Consumed by:** `CrossfadeController`.
  - **Related files:** `SsilencceRenderersFactory.kt`, `CrossfadeController.kt`.
  - **Notes:** Released only when the playback service is destroyed.

- `playback/crossfade/GaplessAlbumDetector.kt` — Album-continuity detector.
  - **Purpose:** Prevent crossfade from defeating gapless playback.
  - **Responsibilities:** Same non-zero album id plus immediate successor track (same disc) or track 1 of the next disc; unknown track numbers never count.
  - **Inputs:** Current/next `MediaItem`.
  - **Outputs:** Boolean.
  - **Dependencies:** `MediaItemExtras`.
  - **Consumed by:** `CrossfadeController`.
  - **Related files:** `Song.toMediaItem`.
  - **Notes:** The gapless-album opt-out defaults on.

- `playback/effects/ReverbController.kt` — Environmental reverb owner.
  - **Purpose:** Attach `EnvironmentalReverb` to the playback audio session.
  - **Responsibilities:** Create/apply presets/custom parameters/release, gate on effect availability and equalizer disable reasons, recreate on session change.
  - **Inputs:** Audio session id, `ReverbState`.
  - **Outputs:** Effect lifecycle and applied parameters.
  - **Dependencies:** `android.media.audiofx.EnvironmentalReverb`, `ReverbState`.
  - **Consumed by:** `EqualizerManager`, `PlaybackService`.
  - **Related files:** `ReverbState.kt`, `ReverbControls.kt`.
  - **Notes:** Disabled for bit-perfect/audio-offload; experimental label in UI.

- `playback/extractor/ExtraExtractorsFactory.kt` — Extra-format extractor stack.
  - **Purpose:** Prepend AIFF/DSD extractors to Media3's defaults.
  - **Responsibilities:** Create fresh `AiffExtractor`/`DsdExtractor` instances per call.
  - **Inputs:** None.
  - **Outputs:** `ExtractorsFactory`.
  - **Dependencies:** `DefaultExtractorsFactory`, `AiffExtractor`, `DsdExtractor`.
  - **Consumed by:** `PlaybackService`, `CrossfadePlayer`.
  - **Related files:** `AiffExtractor.kt`, `DsdExtractor.kt`.
  - **Notes:** Stateful extractors are never shared between players.

- `playback/extractor/AiffExtractor.kt` — AIFF/AIFC parser and PCM converter.
  - **Purpose:** Play AIFF/AIFC without a platform extractor.
  - **Responsibilities:** Parse PCM 8/16/24/32, `sowt`, `raw `, float32/float64; linear PCM seek map; rewrite samples to sink encodings; explicit rejection of IMA4/µ-law/A-law.
  - **Inputs:** Extractor input.
  - **Outputs:** Media3 samples and format.
  - **Dependencies:** `ExtractorSupport`, Media3 extractor APIs.
  - **Consumed by:** `ExtraExtractorsFactory`.
  - **Related files:** `ExtractorSupport.kt`.
  - **Notes:** 24/32-bit integer is reduced to 16-bit where AudioTrack lacks packed encodings.

- `playback/extractor/DsdExtractor.kt` — DSF/DSDIFF parser.
  - **Purpose:** Decode 1-bit DSD into PCM for playback.
  - **Responsibilities:** Parse block/planar layouts and LSB/MSB bit order, reject DST, block-aligned seek map, output 88.2 kHz PCM16.
  - **Inputs:** Extractor input.
  - **Outputs:** PCM samples and format.
  - **Dependencies:** `DsdChannelDecoder`, `ExtractorSupport`.
  - **Consumed by:** `ExtraExtractorsFactory`.
  - **Related files:** `DsdChannelDecoder.kt`.
  - **Notes:** DST-compressed DSDIFF reports an unsupported-container error.

- `playback/extractor/DsdChannelDecoder.kt` — Two-stage DSD decimator.
  - **Purpose:** Convert DSD64/128/256 to 88.2 kHz PCM.
  - **Responsibilities:** Boxcar cascade to 352.8 kHz plus 64-tap Blackman-windowed sinc FIR by 4, per-channel state.
  - **Inputs:** DSD bytes per channel.
  - **Outputs:** PCM16 samples.
  - **Dependencies:** Kotlin stdlib.
  - **Consumed by:** `DsdExtractor`.
  - **Related files:** `DsdExtractor.kt`.
  - **Notes:** Runs once per output sample; no polling or wakelocks.

- `playback/extractor/ExtractorSupport.kt` — Shared extractor utilities.
  - **Purpose:** Keep binary parsing and PCM output in one place.
  - **Responsibilities:** Big/little-endian readers, 80-bit extended float, `LinearPcmSeekMap`, `PcmOutputWriter` and extra MIME constants.
  - **Inputs:** Extractor input bytes.
  - **Outputs:** Parsed values/samples.
  - **Dependencies:** Media3 extractor APIs.
  - **Consumed by:** `AiffExtractor`, `DsdExtractor`.
  - **Related files:** `ExtraExtractorsFactory.kt`.
  - **Notes:** Android-free except Media3 interfaces.

- `playback/extractor/ExtraFormatSupport.kt` — Extra-format descriptors.
  - **Purpose:** Describe kinds, containers, codecs, decoders and playability.
  - **Responsibilities:** Scanner extension gate, container/codec/decoder labels, API-gated playability.
  - **Inputs:** File extension/MIME.
  - **Outputs:** Descriptor/playability.
  - **Dependencies:** `BundledFfmpegProbe`.
  - **Consumed by:** `ExtraFormatScanner`, song info.
  - **Related files:** `BundledFfmpegProbe.kt`.
  - **Notes:** `@OptIn(UnstableApi::class)` for container MIME constants.

- `playback/extractor/BundledFfmpegProbe.kt` — FFmpeg capability probe.
  - **Purpose:** Report exactly which bundled decoders exist.
  - **Responsibilities:** Reflect `FfmpegLibrary.ffmpegHasDecoder`/version and platform MediaCodec lookup.
  - **Inputs:** Decoder name/MIME.
  - **Outputs:** Availability and reason.
  - **Dependencies:** Media3 FFmpeg extension, MediaCodecList.
  - **Consumed by:** `ExtraFormatSupport`, song info.
  - **Related files:** `ExtraFormatSupport.kt`.
  - **Notes:** APE/WMA report unavailable because the packaged build has no decoder symbols.

- `playback/library/AutoMediaButtons.kt` — Android Auto media buttons.
  - **Purpose:** Build FF/Rewind, favorite and shuffle buttons for Auto.
  - **Responsibilities:** Stateful favorite heart, shuffle icon, seek buttons omitted below the threshold, custom session commands.
  - **Inputs:** Context, timeline/seek availability, favorite/shuffle state.
  - **Outputs:** Ordered `CommandButton` list.
  - **Dependencies:** Media3 `CommandButton`/`SessionCommand`, `Playback`, string resources.
  - **Consumed by:** `PlaybackService`.
  - **Related files:** `Playback.kt`, `MediaIDs.kt`.
  - **Notes:** Repeat is appended by the service because it depends on repeat policy.

- `playback/library/MediaPathCodec.kt` — URL-safe media-id path codec.
  - **Purpose:** Embed arbitrary paths/media ids in one colon-free component.
  - **Responsibilities:** Base64 URL_SAFE/NO_WRAP/NO_PADDING encode/decode; null on malformed input.
  - **Inputs:** Raw strings.
  - **Outputs:** Encoded component or decoded string.
  - **Dependencies:** `android.util.Base64`.
  - **Consumed by:** `LibraryProvider`.
  - **Related files:** `MediaIDs.kt`, `LibraryProvider.kt`.
  - **Notes:** Media-id paths are colon-separated, so raw paths need encoding.

- `playback/notification/SsilencceMediaNotificationProvider.kt` — Configurable system notification provider.
  - **Purpose:** Render the user-selected actions instead of Media3's fixed transport order.
  - **Responsibilities:** Build built-in/custom command buttons, filter unavailable commands, assign compact indices, add actions; mask ghost-song title/artist.
  - **Inputs:** `NotificationButtonLayout`, favorite/seek state, `Player.Commands`.
  - **Outputs:** `CommandButton` list/compact indices.
  - **Dependencies:** Media3 session/notification APIs, `Playback`, `RepeatPolicy`.
  - **Consumed by:** `PlaybackService.setMediaNotificationProvider`.
  - **Related files:** `NotificationButton.kt`, `PlaybackService.kt`.
  - **Notes:** Custom buttons send session commands exposed to in-process controllers.

- `playback/notification/NotificationArtworkPolicy.kt` — Lock-screen artwork toggle.
  - **Purpose:** Omit or restore artwork in session metadata.
  - **Responsibilities:** Strip/restore artwork URI/data, restore the cover URI for numeric ids, apply to item lists.
  - **Inputs:** `MediaItem`s, `Preferences.notificationBroadcastAlbumArt`.
  - **Outputs:** Updated metadata.
  - **Dependencies:** Media3, `CoverProvider`.
  - **Consumed by:** `PlaybackService`, `PersistentStorage`.
  - **Related files:** `PlaybackService.kt`.
  - **Notes:** Uses `replaceMediaItems`; audio is not reloaded.

- `playback/notification/CustomLayoutMediaNotificationProvider.kt` — RemoteViews notification provider.
  - **Purpose:** Render the opt-in small/big/advanced notification styles.
  - **Responsibilities:** Build small/big RemoteViews, player/session actions, honor button preferences, load/cache artwork, cache cover palettes, resolve theme/cover/custom backgrounds and contrasting colors, always-expanded behavior, ghost-song masking.
  - **Inputs:** Session, action factory, channel info, favorite/seek state, preferences, metadata.
  - **Outputs:** `MediaNotification` with custom content views.
  - **Dependencies:** Media3 notification APIs, BitmapLoader, `PaletteProcessor`, `Playback`.
  - **Consumed by:** `PlaybackService` when a custom style is selected.
  - **Related files:** `SsilencceMediaNotificationProvider.kt`, `NotificationStyle.kt`.
  - **Notes:** Android 13+ may ignore custom media layouts; progress ticks only while a big style plays.

- `playback/processor/MonoAudioProcessor.kt` — Runtime stereo→mono downmix.
  - **Purpose:** Toggle mono without reconfiguring the sink.
  - **Responsibilities:** Average 16-bit stereo to both channels, keep the output channel count, bypass multichannel/float.
  - **Inputs:** PCM16 buffers.
  - **Outputs:** Mono-on-both-channels buffers.
  - **Dependencies:** Media3 `BaseAudioProcessor`.
  - **Consumed by:** `SsilencceRenderersFactory` (between balance and ReplayGain).
  - **Related files:** `BalanceAudioProcessor.kt`, `ReplayGainAudioProcessor.kt`.
  - **Notes:** Takes effect on the next buffer.

- `playback/renderer/GaplessGateAudioSink.kt` — Delegating gapless gate.
  - **Purpose:** Provide a gapless toggle on Media3 1.10.1.
  - **Responsibilities:** Strip encoder delay/padding on `configure`, zero `setOffloadDelayPadding` when disabled, pass all other calls through.
  - **Inputs:** Delegate `AudioSink`, gapless provider.
  - **Outputs:** Configured or untrimmed sink.
  - **Dependencies:** Media3 `AudioSink`/`DefaultAudioSink`/`Format`.
  - **Consumed by:** `SsilencceRenderersFactory`.
  - **Related files:** `SsilencceRenderersFactory.kt`.
  - **Notes:** Applies from the next configure; keep in sync with future sink methods.

#### UI components, dialogs and settings

- `ui/component/compose/QuickAccessRow.kt` — Now Playing shortcut row.
  - **Purpose:** Render the selected quick-access actions.
  - **Responsibilities:** Horizontal icon buttons from `QuickAccessAction`, emit selection; compose nothing for an empty list.
  - **Inputs:** Ordered action list, callbacks.
  - **Outputs:** Compose row.
  - **Dependencies:** Compose Material3, `QuickAccessAction`.
  - **Consumed by:** `AuroraPlayerScreen`, legacy player overlay.
  - **Related files:** `QuickAccessPreferenceDialog.kt`.
  - **Notes:** Legacy styles host it as a top overlay; Aurora uses its controls column.

- `ui/component/compose/lyrics/AbRepeatLineSheet.kt` — A-B repeat line menu.
  - **Purpose:** Offer line/range repeat from a synced lyric line.
  - **Responsibilities:** Repeat this line / Set A / Set B / Clear; disable Set B until A exists or when the line ends before A; apply the synced offset.
  - **Inputs:** Line text, active/started flags, callbacks.
  - **Outputs:** Material bottom sheet.
  - **Dependencies:** Compose Material3.
  - **Consumed by:** `LyricsScreen` synced branch (full and cover lyrics).
  - **Related files:** `LyricsView.kt`, `PlayerViewModel.kt`.
  - **Notes:** Long-press remains available in normal mode only.

- `ui/component/compose/lyrics/LyricsSelectionBar.kt` — Lyric multi-select top bar.
  - **Purpose:** Host count, select-all, copy, share and close actions.
  - **Responsibilities:** Render enabled/disabled actions; Select all is enabled whenever the bar is visible.
  - **Inputs:** Selected count, callbacks.
  - **Outputs:** Material3 TopAppBar.
  - **Dependencies:** Material3, string resources.
  - **Consumed by:** `LyricsScreen`.
  - **Related files:** `LyricsView.kt`.
  - **Notes:** Selection can start with zero lines.

- `ui/component/compose/player/AbRepeatChip.kt` — A-B repeat cover chip.
  - **Purpose:** Show staged/A-B markers and clear the loop.
  - **Responsibilities:** Render `A-B start - end` or `A start · set B`, tap-to-clear with an accessibility label.
  - **Inputs:** `AbRepeatState?`, staged A, clear callback.
  - **Outputs:** Player cover overlay.
  - **Dependencies:** Compose, `asReadableDuration()`.
  - **Consumed by:** `CoverPagerFragment`.
  - **Related files:** `AbRepeatState.kt`.
  - **Notes:** Drawn above cover and cover lyrics in every style, including Aurora.

- `ui/component/menu/MenuCustomizationFilter.kt` — Inflated-menu filter.
  - **Purpose:** Apply hidden/order state to XML entity menus.
  - **Responsibilities:** Remove hidden catalog actions, re-insert the rest in saved order preserving group/icon/visibility/enabled/checked/shortcuts/intent/submenu children, leave non-catalog actions in place.
  - **Inputs:** Inflated `Menu`, resource/group.
  - **Outputs:** Mutated menu.
  - **Dependencies:** `MenuCustomizationGroup`, `Preferences`.
  - **Consumed by:** `newPopupMenu`, genre/year detail overflow.
  - **Related files:** `MenuClick.kt`, `MenuCustomizationGroup.kt`.
  - **Notes:** No-op when the saved order already matches.

- `ui/component/preferences/dialog/MenuCustomizationDialog.kt` — Entity menu editor.
  - **Purpose:** Show/hide and reorder one entity's menu actions.
  - **Responsibilities:** Checkboxes and up/down arrows; persist each change through `Preferences.setMenuCustomization`.
  - **Inputs:** `MenuCustomizationGroup` argument.
  - **Outputs:** Persisted state.
  - **Dependencies:** Compose Material3, `SsilencceTheme`, `Preferences`.
  - **Consumed by:** `PreferenceScreenFragment` for the six `menu_customization_*` keys.
  - **Related files:** `MenuCustomizationGroup.kt`.
  - **Notes:** Static labels use `stringResource` during composition.

- `ui/component/preferences/dialog/NotificationBackgroundColorDialog.kt` — Custom notification background picker.
  - **Purpose:** Pick an exact ARGB background for custom styles.
  - **Responsibilities:** Preset swatches, HSV sliders, live hex preview, persist color and switch to custom mode.
  - **Inputs:** Current custom color/theme palette.
  - **Outputs:** Updated preferences.
  - **Dependencies:** Compose Material3, `Preferences`.
  - **Consumed by:** `PreferencesScreenFragment`.
  - **Related files:** `NotificationStyle.kt`.
  - **Notes:** No effect on the system Media3 style.

- `ui/component/preferences/dialog/NotificationButtonLayoutDialog.kt` — Compact/expanded button editor.
  - **Purpose:** Edit notification button slots and reset defaults.
  - **Responsibilities:** Compact/expanded slot selection, availability filtering and subset/limit normalization.
  - **Inputs:** `NotificationButtonLayout`.
  - **Outputs:** Persisted layout.
  - **Dependencies:** Compose Material3, `NotificationButton`.
  - **Consumed by:** `PreferencesScreenFragment`.
  - **Related files:** `NotificationButton.kt`, `preferences_screen_playback.xml`.
  - **Notes:** Compact max 3, expanded max 5.

- `ui/component/preferences/dialog/QuickAccessPreferenceDialog.kt` — Quick-access checklist/reorder.
  - **Purpose:** Choose and order the Now Playing shortcuts.
  - **Responsibilities:** Toggle/reorder `QuickAccessAction` entries and persist through `Preferences.quickAccessActions`.
  - **Inputs:** Current action list.
  - **Outputs:** Persisted list.
  - **Dependencies:** Compose Material3, `Preferences`.
  - **Consumed by:** `PreferencesScreenFragment` for `now_playing_quick_access`.
  - **Related files:** `QuickAccessAction.kt`, `QuickAccessRow.kt`.
  - **Notes:** Empty selection keeps the default layout.

- `ui/screen/library/folders/FolderInfoDialog.kt` — Folder info dialog.
  - **Purpose:** Show the `FolderInfo` snapshot.
  - **Responsibilities:** Loading state and path/count/duration/size/timestamp rows.
  - **Inputs:** Folder path, `Repository`.
  - **Outputs:** Compose dialog.
  - **Dependencies:** Compose, `FolderInfoProvider`.
  - **Consumed by:** `FoldersListFragment`, `FolderDetailFragment`.
  - **Related files:** `core/model/filesystem/FolderInfo.kt`.
  - **Notes:** Read-only.

- `ui/screen/library/folders/FolderNameDialog.kt` — Create/rename folder prompt.
  - **Purpose:** Run create/rename with SAF fallback.
  - **Responsibilities:** Validate the name, call `FolderFileOperations`, handle SAF grant retry, refresh Folders and pop a stale detail screen.
  - **Inputs:** Mode, current path, callbacks.
  - **Outputs:** Created/renamed folder or error.
  - **Dependencies:** Compose, `FolderFileOperations`, `SAFDialog`.
  - **Consumed by:** Folder list/detail menus.
  - **Related files:** `FolderFileOperations.kt`.
  - **Notes:** Direct writes are attempted first.

- `ui/screen/library/folders/FolderTransferDialog.kt` — Move/copy picker and progress.
  - **Purpose:** Move or copy songs to a chosen folder.
  - **Responsibilities:** Folder search/selection, progress/result summary and Android 10/11+ source-deletion consent.
  - **Inputs:** Song ids, operation mode.
  - **Outputs:** Transfer result.
  - **Dependencies:** Compose, `FolderFileOperations`, `StartIntentSenderForResult`.
  - **Consumed by:** Song/file menus and multi-select.
  - **Related files:** `MenuItemClickExt.kt`.
  - **Notes:** Configuration changes cancel the transfer; failed writes are deleted.

- `ui/screen/library/folders/HideSongsDialog.kt` — Hide songs confirmation.
  - **Purpose:** Add song paths to the blacklist.
  - **Responsibilities:** Insert `BLACKLIST` rows, report count or disabled hint, refresh lists.
  - **Inputs:** Selected song paths.
  - **Outputs:** Updated blacklist.
  - **Dependencies:** Compose, `InclExclDao`, `Preferences`.
  - **Consumed by:** Song menus.
  - **Related files:** `FolderMediaRefresh.kt`.
  - **Notes:** Reversible in Library > Blacklist.

- `ui/screen/library/folders/ExcludeFolderDialog.kt` — Exclude folder confirmation.
  - **Purpose:** Add a folder path to the blacklist.
  - **Responsibilities:** Confirm, insert blacklist row and refresh the library.
  - **Inputs:** Folder path.
  - **Outputs:** Updated blacklist.
  - **Dependencies:** Compose, `InclExclDao`.
  - **Consumed by:** Folder menus.
  - **Related files:** `FolderMediaRefresh.kt`.
  - **Notes:** Does not delete files.

- `ui/screen/library/folders/FolderMediaRefresh.kt` — MediaStore refresh helper.
  - **Purpose:** Notify MediaStore after folder/blacklist changes.
  - **Responsibilities:** `notifyMediaStoreChanged(context)`.
  - **Inputs:** Context.
  - **Outputs:** Media scanner notifications.
  - **Dependencies:** MediaScannerConnection.
  - **Consumed by:** Folder dialogs.
  - **Related files:** `LibraryRescan.kt`.
  - **Notes:** Best-effort.

- `ui/screen/ghost/GhostSearchFragment.kt` — Ghost search host.
  - **Purpose:** Bridge nav graph, PIN session and player to the screen.
  - **Responsibilities:** Lock/unlock on resume, refresh on return, open the selected list as the playback queue.
  - **Inputs:** `GhostSearchViewModel`, `PlayerViewModel`, `GhostMode`.
  - **Outputs:** Screen composition and playback requests.
  - **Dependencies:** Compose, `GhostMode`, `CollapsibleAppBarScaffold`.
  - **Consumed by:** `nav_ghost_search`.
  - **Related files:** `GhostSearchScreen.kt`, `GhostSearchViewModel.kt`.
  - **Notes:** Also reachable from the lock-screen ghost entry.

- `ui/screen/ghost/GhostSearchViewModel.kt` — Ghost search state.
  - **Purpose:** Load ghost-only songs off the main thread.
  - **Responsibilities:** Read `SongRepository.ghostSongs()`, expose loading/list/query/mode state.
  - **Inputs:** `SongRepository`.
  - **Outputs:** `GhostSearchUiState`.
  - **Dependencies:** `SongRepository`, `GhostMode`, `viewModelScope`.
  - **Consumed by:** `GhostSearchFragment`/`GhostSearchScreen`.
  - **Related files:** `SongRepository.kt`.
  - **Notes:** No Room query of its own.

- `ui/screen/ghost/GhostSearchScreen.kt` — Ghost search UI.
  - **Purpose:** Search/play hidden songs and explain configuration/lock states.
  - **Responsibilities:** Quick-search bar, filtered list, empty/disabled/no-folder states, PIN keypad and play rows.
  - **Inputs:** UI state, locked flag, callbacks.
  - **Outputs:** User intents.
  - **Dependencies:** Compose Material3, `QuickSearchBar`.
  - **Consumed by:** `GhostSearchFragment`.
  - **Related files:** `GhostSearchFragment.kt`.
  - **Notes:** PIN is an access barrier, not encryption.

- `ui/screen/settings/GhostSettingsFragment.kt` — Ghost settings host.
  - **Purpose:** Manage the master switch, folders and PIN.
  - **Responsibilities:** Folder picker, preference writes, library reload and open-search action.
  - **Inputs:** `GhostMode`, `LibraryViewModel`.
  - **Outputs:** `GhostSettingsScreen` and persisted preferences.
  - **Dependencies:** `GhostSettingsScreen`, `FolderChooserDialog`, `GhostMode`.
  - **Consumed by:** `nav_ghost_settings` from Advanced > Privacy.
  - **Related files:** `GhostSettingsScreen.kt`, `graph_settings.xml`.
  - **Notes:** Hidden with unimportant settings when that policy is off.

- `ui/screen/settings/GhostSettingsScreen.kt` — Ghost settings UI.
  - **Purpose:** Present ghost controls and PIN dialogs.
  - **Responsibilities:** Master switch, folder list/remove/add, PIN validation, open-search and explanation.
  - **Inputs:** Enabled/folders/PIN state and callbacks.
  - **Outputs:** User intents.
  - **Dependencies:** Compose Material3, `java.io.File`.
  - **Consumed by:** `GhostSettingsFragment`.
  - **Related files:** `GhostSettingsFragment.kt`.
  - **Notes:** Adding a folder auto-enables ghost mode.

- `ui/screen/settings/AccentColorDialog.kt` — Accent color picker.
  - **Purpose:** Choose a preset or custom accent.
  - **Responsibilities:** Preset palette, HSV picker and Default reset.
  - **Inputs:** Current accent.
  - **Outputs:** Persisted accent color.
  - **Dependencies:** Compose Material3, `Preferences`.
  - **Consumed by:** `PreferencesScreenFragment`.
  - **Related files:** `ui/theme/Theme.kt`.
  - **Notes:** Aurora Glass keeps its fixed white accent.

- `ui/screen/settings/BatteryGuideFragment.kt` — OEM battery guide screen.
  - **Purpose:** Render tailored background-playback guidance.
  - **Responsibilities:** OEM steps, battery-optimization status, pin-in-recents tip and intents for battery/app/OEM screens.
  - **Inputs:** `PowerManagementGuide`.
  - **Outputs:** Preference screen and launched intents.
  - **Dependencies:** AndroidX Preference, `PowerManagementGuide`.
  - **Consumed by:** Advanced settings.
  - **Related files:** `util/oem/PowerManagementGuide.kt`, `preferences_screen_battery.xml`.
  - **Notes:** No new permission.

- `ui/screen/settings/LockScreenPreferencesFragment.kt` — Lock-screen settings.
  - **Purpose:** Configure the lock-screen player and permissions guide.
  - **Responsibilities:** Enable/preview, wallpaper picker, behavior switches and OEM permission guide.
  - **Inputs:** `Preferences`.
  - **Outputs:** Persisted settings and preview launch.
  - **Dependencies:** PreferenceFragmentCompat, `LockScreenPermissionGuide`.
  - **Consumed by:** Settings > Lock screen player.
  - **Related files:** `preferences_screen_lock_screen.xml`, `LockScreenPlayerActivity.kt`.
  - **Notes:** Default disabled.

- `ui/screen/settings/RemoveMissingSongsDialog.kt` — Missing-song cleanup dialog.
  - **Purpose:** Scan/confirm/progress/result for missing songs.
  - **Responsibilities:** Call `findMissingSongs`/`removeMissingSongs` and report playlist/count/event/history totals.
  - **Inputs:** `Repository`.
  - **Outputs:** Cleanup result.
  - **Dependencies:** Compose Material3, `Repository`.
  - **Consumed by:** Settings > Advanced > Maintenance.
  - **Related files:** `MissingSongsCleanup.kt`.
  - **Notes:** Never deletes files or MediaStore rows.

- `ui/screen/settings/SettingsVisibilityPolicy.kt` — Unimportant-settings policy.
  - **Purpose:** List advanced preference keys hidden by the toggle.
  - **Responsibilities:** Key set and empty-category hiding rule.
  - **Inputs:** Preference visibility flag.
  - **Outputs:** Keys/categories to hide.
  - **Dependencies:** `R.string`/keys.
  - **Consumed by:** `PreferencesScreenFragment`.
  - **Related files:** `PreferencesScreenFragment.kt`.
  - **Notes:** The toggle defaults to on.

- `ui/screen/setup/SetupWizardActivity.kt` — First-run setup host.
  - **Purpose:** Host the four-step library setup.
  - **Responsibilities:** Render the screen, route back/skip, finish into MainActivity on first run and finish when opened from Settings.
  - **Inputs:** `EXTRA_FROM_SETTINGS`, ViewModel state.
  - **Outputs:** Persisted choices, MainActivity launch.
  - **Dependencies:** `AbsThemeActivity`, Compose, Koin, `SsilencceTheme`.
  - **Consumed by:** `MainActivity` first-run gate and Library settings.
  - **Related files:** `SetupWizardScreen.kt`, `SetupWizardViewModel.kt`.
  - **Notes:** Exported false; gated by `setup_wizard_completed`.

- `ui/screen/setup/SetupWizardScreen.kt` — Four-step setup UI.
  - **Purpose:** Welcome, folders, scanning and finish steps.
  - **Responsibilities:** Include/exclude lists, main folder, folder picker, auto-rescan switch, MediaStore limits explanation, rescan result and Back/Next/Finish.
  - **Inputs:** UI state and callbacks.
  - **Outputs:** State changes and picker selections.
  - **Dependencies:** Compose Material3, `SetupFolderPicker`.
  - **Consumed by:** `SetupWizardActivity`.
  - **Related files:** `SetupFolderPicker.kt`, `SetupWizardViewModel.kt`.
  - **Notes:** Explains that MediaStore cannot honor `.nomedia`/hidden-folder toggles.

- `ui/screen/setup/SetupWizardViewModel.kt` — Setup wizard state holder.
  - **Purpose:** Hold and persist setup choices.
  - **Responsibilities:** Load lists/main folder, seed default blacklist once, add/remove folders, track step/auto-rescan, run rescan, persist and expose progress/failure.
  - **Inputs:** Application, `Repository`, `InclExclDao`.
  - **Outputs:** `SetupWizardUiState` and Room/preference writes.
  - **Dependencies:** `LibraryRescan`, `Preferences`, `FileUtil`, Koin.
  - **Consumed by:** `SetupWizardActivity`/`SetupWizardScreen`.
  - **Related files:** `LibraryRescan.kt`, `MainModule.kt`.
  - **Notes:** Saving replaces rows and force-enables non-empty lists.

- `ui/screen/setup/SetupFolderPicker.kt` — Setup folder browser.
  - **Purpose:** Pick a folder without SAF.
  - **Responsibilities:** List volumes, navigate read-only directories, hide dot-folders, go up and return an absolute path; loads off the main thread.
  - **Inputs:** Dialog title/start path/callbacks.
  - **Outputs:** Selected path.
  - **Dependencies:** Compose Material3, `StorageUtil`.
  - **Consumed by:** `SetupWizardScreen`.
  - **Related files:** `FolderChooserDialog.kt`.
  - **Notes:** Mirrors the legacy chooser's volume-list start.

#### Player, utility and remaining screens

- `ui/screen/audiocutter/AudioCutterActivity.kt` — Compose trimmer host.
  - **Purpose:** Host the audio cutter and request permissions before preparing state.
  - **Responsibilities:** Request audio-read/pre-Q write permissions; intent factory takes a song id; render the screen.
  - **Inputs:** Song id, permissions.
  - **Outputs:** `AudioCutterScreen` composition.
  - **Dependencies:** Compose, Koin ViewModel.
  - **Consumed by:** Song/playlist/now-playing/multi-select menus.
  - **Related files:** `AudioCutterScreen.kt`, `AudioCutterViewModel.kt`.
  - **Notes:** Exported false.

- `ui/screen/audiocutter/AudioCutterViewModel.kt` — Trimmer state and IO.
  - **Purpose:** Load song/inspection and run export/ringtone work.
  - **Responsibilities:** Expose UI state and one-shot events; run SAF export and system-sound publishing on IO; guard concurrent work.
  - **Inputs:** Song id, `AudioClipExporter`, `RingtoneClipStore`.
  - **Outputs:** `AudioCutterUiState`, `AudioCutterEvent`.
  - **Dependencies:** `AudioClipFormat`, `AudioClipNaming`, coroutines.
  - **Consumed by:** `AudioCutterActivity`.
  - **Related files:** `AudioClipExporter.kt`.
  - **Notes:** No background playback.

- `ui/screen/audiocutter/AudioCutterScreen.kt` — Trimmer UI.
  - **Purpose:** Select a range, preview and export.
  - **Responsibilities:** Range slider, labels, clipped ExoPlayer preview, SAF create-document, `WRITE_SETTINGS` launcher/retry, snackbar events and unsupported/loading/missing states.
  - **Inputs:** UI state and callbacks.
  - **Outputs:** User intents.
  - **Dependencies:** Compose Material3, ExoPlayer, SAF.
  - **Consumed by:** `AudioCutterActivity`.
  - **Related files:** `AudioCutterViewModel.kt`.
  - **Notes:** Minimum selection is ±1 s; unsupported codecs show an explicit state.

- `ui/screen/equalizer/ReverbControls.kt` — Reverb settings card.
  - **Purpose:** Edit reverb presets and parameters.
  - **Responsibilities:** Experimental label, preset chips and custom sliders.
  - **Inputs:** `ReverbState`, callbacks.
  - **Outputs:** Compose card and preference writes.
  - **Dependencies:** Compose Material3, `ReverbState`.
  - **Consumed by:** `EqualizerScreen`.
  - **Related files:** `ReverbController.kt`.
  - **Notes:** Hidden/disabled where effects are unavailable.

- `ui/screen/equalizer/SystemEqualizerCard.kt` — System equalizer card.
  - **Purpose:** Explain and open the OEM panel.
  - **Responsibilities:** Panel action and unavailable message.
  - **Inputs:** Availability, open callback.
  - **Outputs:** Compose card.
  - **Dependencies:** Compose Material3.
  - **Consumed by:** `EqualizerScreen`.
  - **Related files:** `SystemEqualizer.kt`.
  - **Notes:** Replaces the in-app cards while System mode is selected.

- `ui/screen/lockscreen/LockScreenTrigger.kt` — Lock-screen launch policy and receiver.
  - **Purpose:** Decide when to show the lock-screen player on screen-off.
  - **Responsibilities:** In-memory pause clock, feature/enable/skip policy, dynamic screen-off receiver.
  - **Inputs:** Active player state, preferences, broadcast.
  - **Outputs:** Launch/dismiss decision.
  - **Dependencies:** Android broadcasts, `Preferences`.
  - **Consumed by:** `PlaybackService`.
  - **Related files:** `LockScreenPlayerActivity.kt`, `PlaybackService.kt`.
  - **Notes:** Never manifest-registered; follows the active local or Cast player.

- `ui/screen/lockscreen/LockScreenPlayerActivity.kt` — Keyguard-overlay player activity.
  - **Purpose:** Show the opt-in full-screen player over the system lock screen.
  - **Responsibilities:** `showWhenLocked`/`turnScreenOn`, preview intent, MediaController connection, favorite-playlist/genre resolution for shortcuts and USER_PRESENT dismissal.
  - **Inputs:** Preview/enable intents, media session.
  - **Outputs:** Compose lock-screen UI and playback commands.
  - **Dependencies:** AppCompat/Activity, MediaController, `LockScreenPlayerScreen`.
  - **Consumed by:** `PlaybackService` screen-off trigger and the settings preview.
  - **Related files:** `LockScreenPlayerScreen.kt`, `LockScreenTrigger.kt`.
  - **Notes:** Excluded from recents; API 26 uses the deprecated window flags.

- `ui/screen/lockscreen/LockScreenPlayerScreen.kt` — Compose lock-screen UI.
  - **Purpose:** Render wallpaper, cover gestures, controls, queue and lyrics on the keyguard.
  - **Responsibilities:** Seek bar, transport/repeat/shuffle, queue/lyrics panels, wallpaper, entity shortcuts, upward-flick dismissal and optional ghost-search entry.
  - **Inputs:** Playback/queue/lyrics state and callbacks.
  - **Outputs:** User intents.
  - **Dependencies:** Compose Material3, Coil, `PlayerGesturesController`.
  - **Consumed by:** `LockScreenPlayerActivity`.
  - **Related files:** `LockScreenPlayerActivity.kt`.
  - **Notes:** Ghost entry delegates to the PIN-gated GhostSearch destination; no ghost songs are listed on the lock screen.

- `ui/screen/lockscreen/LockScreenEntityLauncher.kt` — Lock-screen shortcut intents.
  - **Purpose:** Route artist/folder/genre/playlist requests to MainActivity.
  - **Responsibilities:** Build typed entity intents and also route GhostSearch openings.
  - **Inputs:** Entity type/id/path, context.
  - **Outputs:** `ACTION_OPEN_ENTITY` intents.
  - **Dependencies:** `MainActivity`, `ShortcutEntityType`.
  - **Consumed by:** `LockScreenPlayerActivity`/`LockScreenPlayerScreen`.
  - **Related files:** `MainActivity.kt`.
  - **Notes:** The activity closes after a successful hand-off.

- `ui/screen/lyrics/LyricsSourceDialog.kt` — Per-song lyrics source picker.
  - **Purpose:** Override the resolved lyrics source.
  - **Responsibilities:** Radio list of Automatic/Embedded/Downloaded/File and picked-file summary.
  - **Inputs:** Current selection, callbacks.
  - **Outputs:** Choice or file-picker request.
  - **Dependencies:** AlertDialog, `DialogListItemWithRadio`.
  - **Consumed by:** `LyricsScreen`.
  - **Related files:** `LyricsSourceSelection.kt`.
  - **Notes:** The File row doubles as the chooser.

- `ui/screen/lyrics/LyricsSyncEditor.kt` — Manual LRC sync surface.
  - **Purpose:** Stamp plain lines against playback and export the result.
  - **Responsibilities:** Play/pause/position, monotonic Stamp, ±100 ms, embedded/LRC save, Bundle-saveable state.
  - **Inputs:** `LyricsSyncState`, position/play state, callbacks.
  - **Outputs:** Stamped LRC through embedded/file save.
  - **Dependencies:** `LrcTools`, Compose, `Saver`.
  - **Consumed by:** `LyricsEditorScreen`.
  - **Related files:** `LrcTools.kt`.
  - **Notes:** Disabled when the song is not current; TTML yields no stampable lines.

- `ui/screen/miniplayer/MiniPlayerActivity.kt` — External-audio/preview host.
  - **Purpose:** Show the floating mini player for `ACTION_VIEW`/preview.
  - **Responsibilities:** Build the request, apply app theme, finish on outside touch/back/stop and launch MainActivity for Open in app.
  - **Inputs:** View intent or preview extra, preferences.
  - **Outputs:** Compose screen and MainActivity hand-off.
  - **Dependencies:** Compose, Koin, `MiniPlayerViewModel`.
  - **Consumed by:** Audio VIEW intents and the song Preview action.
  - **Related files:** `MiniPlayerScreen.kt`, `MiniPlayerModels.kt`.
  - **Notes:** `excludeFromRecents`, `singleTop`, dialog theme; no foreground service.

- `ui/screen/miniplayer/MiniPlayerViewModel.kt` — Local preview playback.
  - **Purpose:** Play one external/preview track without touching the main queue.
  - **Responsibilities:** Resolve URI, own a local ExoPlayer, publish/seek/end state, gate start while muted, pause/restore the main controller and stop on Open in app.
  - **Inputs:** `MiniPlayerRequest`, `Repository`, preferences.
  - **Outputs:** UI state, main-session pause/play, open URI.
  - **Dependencies:** Media3 ExoPlayer/MediaController, `Repository`, `CoverProvider`.
  - **Consumed by:** `MiniPlayerActivity`, `MiniPlayerScreen`.
  - **Related files:** `MiniPlayerModels.kt`.
  - **Notes:** Checks `PlaybackService.isRunning`; no history/queue writes.

- `ui/screen/miniplayer/MiniPlayerScreen.kt` — Mini player UI.
  - **Purpose:** Render artwork, play/pause, seek, mute feedback and Open in app.
  - **Responsibilities:** Loading/error/ready states, drag-safe slider and accessible labels.
  - **Inputs:** UI state and callbacks.
  - **Outputs:** Compose UI.
  - **Dependencies:** Compose Material3, Coil.
  - **Consumed by:** `MiniPlayerActivity`.
  - **Related files:** `MiniPlayerViewModel.kt`.
  - **Notes:** All static text uses `stringResource`.

- `ui/screen/miniplayer/MiniPlayerModels.kt` — Mini-player models.
  - **Purpose:** Keep intent parsing and media-item construction out of the activity.
  - **Responsibilities:** External/Preview/Unsupported requests, `EXTRA_SONG`, track-to-MediaItem and UI state.
  - **Inputs:** `Intent`, `Song`, URI metadata.
  - **Outputs:** Media3 item and UI state.
  - **Dependencies:** Media3 common, `Song`.
  - **Consumed by:** `MiniPlayerActivity`, `MiniPlayerViewModel`, `MiniPlayerScreen`.
  - **Related files:** `MiniPlayerActivity.kt`.
  - **Notes:** None.

- `ui/screen/notes/SongNotesViewModel.kt` — Notes state holder.
  - **Purpose:** Own notes/bookmark state and persist mutations.
  - **Responsibilities:** Load/edit/save, add/update/delete bookmark, merge imported text, emit one-shot events; serialize mutations with a Mutex.
  - **Inputs:** `SongNotesRepository`, `Song`.
  - **Outputs:** UI state and events.
  - **Dependencies:** `SongNotesRepository`, `SongNotesCodec`.
  - **Consumed by:** `SongNotesFragment`/`SongNotesScreen`.
  - **Related files:** `SongNotesRepository.kt`.
  - **Notes:** Import appends notes and unions bookmarks.

- `ui/screen/notes/SongNotesFragment.kt` — Notes screen host.
  - **Purpose:** Bridge nav args, player state, file picker and sharing.
  - **Responsibilities:** Read song/fromPlayer, collect state/position, import text, share notes, seek only for the current song and re-expand the player panel on exit.
  - **Inputs:** SafeArgs, `PlayerViewModel`, `SongNotesViewModel`.
  - **Outputs:** Screen composition and seek/share/import effects.
  - **Dependencies:** `SongNotesScreen`, `SongNotesCodec`, OpenDocument.
  - **Consumed by:** `nav_song_notes`.
  - **Related files:** `graph_main.xml`.
  - **Notes:** `fromPlayer` controls panel re-expansion.

- `ui/screen/notes/SongNotesScreen.kt` — Notes editor and bookmark list.
  - **Purpose:** Edit notes, browse bookmarks and jump to them.
  - **Responsibilities:** Top bar add/import/share, notes field with Save, bookmark rows, add dialog, retry card, storage-path hint and Aurora background.
  - **Inputs:** Song, UI state, current position, callbacks.
  - **Outputs:** User intents.
  - **Dependencies:** Compose Material3, `TopAppBarMenu`, `SeekTimeParser`.
  - **Consumed by:** `SongNotesFragment`.
  - **Related files:** `SongNotesFragment.kt`.
  - **Notes:** Time input accepts `mm:ss`/`h:mm:ss`.

- `ui/screen/player/AbRepeatState.kt` — A-B repeat UI contract.
  - **Purpose:** Mirror the service-owned loop for the UI.
  - **Responsibilities:** Active range, A→B→clear menu result, 500 ms minimum range.
  - **Inputs:** Position/duration, service state.
  - **Outputs:** State for menu/chip/lyrics.
  - **Dependencies:** None.
  - **Consumed by:** `PlayerViewModel`, `AbsPlayerFragment`, `AbRepeatChip`, `LyricsScreen`.
  - **Related files:** `PlaybackService.kt`.
  - **Notes:** Only the service owns the live loop.

- `ui/screen/player/PlayerScreenshot.kt` — Share Now Playing image.
  - **Purpose:** Render/crop the player view to a JPEG and share it.
  - **Responsibilities:** Bitmap render, optional centered square crop, `cacheDir/now_playing_share/`, FileProvider URI and `ACTION_SEND`.
  - **Inputs:** Player view, crop option.
  - **Outputs:** Share intent.
  - **Dependencies:** Android graphics, FileProvider.
  - **Consumed by:** `AbsPlayerFragment`.
  - **Related files:** `provider_paths.xml`.
  - **Notes:** JPEG quality 95; temporary file.

- `ui/screen/player/SeekTimeParser.kt` — Seek-time parser/formatter.
  - **Purpose:** Parse `mm:ss`/`h:mm:ss` and clamp to duration.
  - **Responsibilities:** Validation, clamp and total-minutes formatting.
  - **Inputs:** Text, duration.
  - **Outputs:** Milliseconds or null; formatted total.
  - **Dependencies:** Kotlin stdlib.
  - **Consumed by:** `SeekToDialogFragment`, elapsed-time label.
  - **Related files:** `SeekToDialogFragment.kt`.
  - **Notes:** Shared with notes bookmark input.

- `ui/screen/player/SeekToDialogFragment.kt` — Precise seek dialog.
  - **Purpose:** Seek by typed time.
  - **Responsibilities:** Read activity-scoped `PlayerViewModel`, validate and call `seekTo`.
  - **Inputs:** Current duration/position.
  - **Outputs:** Seek command.
  - **Dependencies:** Compose Material3, `PlayerViewModel`.
  - **Consumed by:** Elapsed-time tap and overflow action.
  - **Related files:** `SeekTimeParser.kt`.
  - **Notes:** Clamps to track duration.

- `ui/screen/player/art/ArtworkStore.kt` — Artwork save/wallpaper helper.
  - **Purpose:** Load artwork, save to gallery and set wallpaper.
  - **Responsibilities:** CoverProvider fallback, MediaStore pending insert on Q+, legacy scanner path, `WallpaperManager` home/lock/both.
  - **Inputs:** Song, target.
  - **Outputs:** Save/wallpaper result.
  - **Dependencies:** Coil/CoverProvider, MediaStore, WallpaperManager.
  - **Consumed by:** `FullScreenArtDialogFragment`.
  - **Related files:** `AndroidManifest.xml` (`SET_WALLPAPER`).
  - **Notes:** Decodes up to 2048 px.

- `ui/screen/player/art/FullScreenArtDialogFragment.kt` — Fullscreen artwork viewer.
  - **Purpose:** Pinch-zoom cover viewer with save/wallpaper actions.
  - **Responsibilities:** Zoom, target menu, safe-area toolbars.
  - **Inputs:** Song/artwork.
  - **Outputs:** Gallery/wallpaper action.
  - **Dependencies:** Compose, `ArtworkStore`.
  - **Consumed by:** `AbsPlayerFragment`.
  - **Related files:** `ArtworkStore.kt`.
  - **Notes:** Hosted as a DialogFragment.

- `ui/screen/player/screencast/ScreencastManager.kt` — External-display presentation owner.
  - **Purpose:** Discover presentation displays and drive the Now Playing presentation.
  - **Responsibilities:** Enumerate displays/live-video route, show/hide presentation, open chooser/settings, connect a read-only MediaController, publish metadata/position/lyrics and stop on display removal.
  - **Inputs:** Context, `Repository`, `LyricsRepository`, display/player events.
  - **Outputs:** display/active/state flows and presentation lifecycle.
  - **Dependencies:** DisplayManager, MediaRouter, Presentation, MediaController, `ScreencastLyricsLoader`.
  - **Consumed by:** `AbsPlayerFragment`, picker, Koin `dataModule`.
  - **Related files:** `ScreencastPresentation.kt`, `ScreencastModels.kt`.
  - **Notes:** No MediaProjection/audio route change; 500 ms ticker only while visible and playing.

- `ui/screen/player/screencast/ScreencastPresentation.kt` — External-display window.
  - **Purpose:** Host the Compose Now Playing screen on a chosen display.
  - **Responsibilities:** ComposeView, forced dark theme, hidden system bars, keep display awake, collect manager state.
  - **Inputs:** Outer context, `Display`, state flow.
  - **Outputs:** Full-screen external window.
  - **Dependencies:** `android.app.Presentation`, Compose, `SsilencceTheme`.
  - **Consumed by:** `ScreencastManager`.
  - **Related files:** `ScreencastNowPlayingScreen.kt`.
  - **Notes:** `FLAG_KEEP_SCREEN_ON` applies to the presentation only.

- `ui/screen/player/screencast/ScreencastNowPlayingScreen.kt` — External Now Playing UI.
  - **Purpose:** Render cover, metadata, lyrics and progress.
  - **Responsibilities:** Adaptive wide/tall layout, synced line highlight, first six plain lines, instrumental/idle states, progress and times.
  - **Inputs:** `ScreencastNowPlayingState`.
  - **Outputs:** Composable UI.
  - **Dependencies:** Compose Material3, Coil, `SyncedLyrics`.
  - **Consumed by:** `ScreencastPresentation`.
  - **Related files:** `ScreencastModels.kt`.
  - **Notes:** Non-interactive; no transport controls.

- `ui/screen/player/screencast/ScreencastModels.kt` — Screencast presentation models.
  - **Purpose:** Define display descriptors, lyrics variants and the Now Playing snapshot.
  - **Responsibilities:** `ScreencastDisplayInfo`, `ScreencastLyrics`, `ScreencastNowPlayingState` with derived progress.
  - **Inputs:** Display/media/lyrics/position data.
  - **Outputs:** Immutable state.
  - **Dependencies:** `SyncedLyrics`, Compose `@Immutable`, `Uri`.
  - **Consumed by:** `ScreencastManager`, `ScreencastPresentation`, picker.
  - **Related files:** `ScreencastLyricsLoader.kt`.
  - **Notes:** None.

- `ui/screen/player/screencast/ScreencastLyricsLoader.kt` — Local-only lyrics resolver.
  - **Purpose:** Reuse per-song source selection without network downloads.
  - **Responsibilities:** Honor an explicit SAF file, then file/embedded/stored priority; return synced/plain/instrumental.
  - **Inputs:** `Song`, `LyricsRepository`.
  - **Outputs:** `ScreencastLyrics`.
  - **Dependencies:** `LyricsRepository`, `LyricsSourceSelection`.
  - **Consumed by:** `ScreencastManager` on IO.
  - **Related files:** `ScreencastModels.kt`.
  - **Notes:** Calls `storedLyrics(allowDownload = false)`.

- `ui/screen/player/screencast/ScreencastPickerDialogFragment.kt` — Screencast picker host.
  - **Purpose:** Host the toggle/display picker from Now Playing.
  - **Responsibilities:** Collect flows, wire switch/selection/chooser/settings/close and refresh on resume.
  - **Inputs:** `ScreencastManager`.
  - **Outputs:** Manager calls and dialog UI.
  - **Dependencies:** `ScreencastPickerScreen`, `SsilencceTheme`.
  - **Consumed by:** `AbsPlayerFragment` (`action_screencast`).
  - **Related files:** `menu_now_playing.xml`.
  - **Notes:** Feature is runtime-only and off by default.

- `ui/screen/player/screencast/ScreencastPickerScreen.kt` — Screencast picker content.
  - **Purpose:** Present the switch, displays, discovery/settings actions and audio-stays-local explanation.
  - **Responsibilities:** Scrollable content, enable switch, display rows with resolution, empty guidance and labelled buttons.
  - **Inputs:** Displays/active id/callbacks.
  - **Outputs:** Composable UI.
  - **Dependencies:** Compose Material3, `ScreencastDisplayInfo`.
  - **Consumed by:** `ScreencastPickerDialogFragment`.
  - **Related files:** `ScreencastModels.kt`.
  - **Notes:** None.

- `ui/screen/queue/NamedQueueMergeDialog.kt` — Saved-queue merge picker.
  - **Purpose:** Choose the target queue for an append-merge.
  - **Responsibilities:** List targets, skip-duplicates switch and dispatch.
  - **Inputs:** Source/target summaries and callbacks.
  - **Outputs:** Target id + duplicate preference.
  - **Dependencies:** Compose Material3, `NamedQueueRepository`.
  - **Consumed by:** `QueueScreen`, `QueuesFragment`.
  - **Related files:** `NamedQueueRepository.kt`.
  - **Notes:** The source queue is kept.

- `ui/screen/sleeptimer/SleepTimerHistory.kt` — Timer history model/codec.
  - **Purpose:** Persist the last five time-based choices.
  - **Responsibilities:** Duration/EndAt entries, `d:45`/`e:22:30` codec, deduplicated most-recent-first insertion, max five and validation.
  - **Inputs:** Raw DataStore string.
  - **Outputs:** Entry list or encoded string.
  - **Dependencies:** None.
  - **Consumed by:** `SleepTimerViewModel`.
  - **Related files:** `SleepTimerViewModel.kt`.
  - **Notes:** Durations bounded 5–180 minutes; invalid rows dropped.

- `ui/screen/sound/CrossfadeSettingsCard.kt` — Crossfade controls.
  - **Purpose:** Edit crossfade duration and related switches.
  - **Responsibilities:** 0–12 s slider (500 ms steps), crossfade-on-skip and keep-gapless-albums switches.
  - **Inputs:** SharedPreferences.
  - **Outputs:** Preference writes.
  - **Dependencies:** `TitledCard`, `LabeledSwitch`, `IconifiedSliderTrack`.
  - **Consumed by:** `SoundSettingsScreen`.
  - **Related files:** `preferences_screen_playback.xml`.
  - **Notes:** Uses `stringResource`; 0 means off.

- `ui/screen/sound/SongTempoOverrideControls.kt` — Per-song tempo actions.
  - **Purpose:** Apply/reset per-song speed and pitch.
  - **Responsibilities:** Show current song and stored values, offer Apply to this song only / Reset song, explain unavailable playback.
  - **Inputs:** Song title, override values, callbacks.
  - **Outputs:** Compose controls.
  - **Dependencies:** Compose Material3.
  - **Consumed by:** `SoundSettingsScreen`.
  - **Related files:** `SongPlaybackParamEntity.kt`.
  - **Notes:** Apply pins the current tempo values.

- `ui/screen/tageditor/BatchTagEditorCover.kt` — Batch cover card.
  - **Purpose:** Preview/replace/remove the batch cover.
  - **Responsibilities:** Current/picked bitmap preview, Keep/Replace/Remove chips and no-cover/load-failure notes.
  - **Inputs:** `BatchCoverState`, callbacks.
  - **Outputs:** Cover operation.
  - **Dependencies:** Compose Material3.
  - **Consumed by:** `BatchTagEditorScreen`.
  - **Related files:** `TagEditCover.kt`.
  - **Notes:** Applies to every written file.

- `util/AppVisibility.kt` — Process-wide UI visibility tracker.
  - **Purpose:** Distinguish a visible app from a backgrounded one.
  - **Responsibilities:** Register `ActivityLifecycleCallbacks`, started-activity count and volatile `isUiVisible`.
  - **Inputs:** Application registration.
  - **Outputs:** Visibility boolean.
  - **Dependencies:** Android lifecycle.
  - **Consumed by:** `PlaybackService` (sleep-timer foreground-only stop), `ResumeAutoplayPolicy`.
  - **Related files:** `App.kt`.
  - **Notes:** Callbacks run on Main; the flag is readable from any thread.

- `util/FolderFileOperations.kt` — Folder and file mutations.
  - **Purpose:** Create/rename folders and move/copy songs across API 26–36.
  - **Responsibilities:** Direct or SAF `DocumentFile` writes, streamed copy with unique names, source deletion via SAF/direct/MediaStore with consent hand-off, MediaStore rescans.
  - **Inputs:** Source/destination paths, songs, context.
  - **Outputs:** Operation result rows.
  - **Dependencies:** `ContentResolver`, `DocumentFile`, `SAFExt`, MediaStore.
  - **Consumed by:** `FolderNameDialog`, `FolderTransferDialog`.
  - **Related files:** `LibraryRescan.kt` (ADR-062).
  - **Notes:** Move is copy-then-delete; failed writes are deleted.

- `util/FolderInfoProvider.kt` — Folder-info builder.
  - **Purpose:** Assemble a `FolderInfo` snapshot.
  - **Responsibilities:** Query `folderByPath`, `filesInPath` and songs with subfolders; resolve timestamps under `runCatching`.
  - **Inputs:** `Repository`, folder path.
  - **Outputs:** `FolderInfo`.
  - **Dependencies:** `Repository`, java.io/nio.
  - **Consumed by:** `FolderInfoDialog`.
  - **Related files:** `FolderInfo.kt`.
  - **Notes:** Read-only.

- `util/GhostMode.kt` — Ghost-folder filter and PIN gate.
  - **Purpose:** Hide selected folder trees from library surfaces without a Room change.
  - **Responsibilities:** Normalize/match paths, build MediaStore exclusion/inclusion selections, filter Song/SongEntity lists, hash/verify the PIN and hold the per-process unlock flag.
  - **Inputs:** Preferences (folders/enabled/PIN), song paths.
  - **Outputs:** SQL selections, filtered lists, PIN result.
  - **Dependencies:** `Preferences`, `AudioColumns`, `MessageDigest`, `SecureRandom`.
  - **Consumed by:** Real song/playlist/smart/genre repositories, `PlaybackService`, Ghost search/settings.
  - **Related files:** `SongRepository.kt`.
  - **Notes:** Direct ID/path/media-item lookup bypasses the filter.

- `util/oem/LockScreenPermissionGuide.kt` — Lock-screen permission guide.
  - **Purpose:** Explain and open overlay/notification/OEM settings.
  - **Responsibilities:** System intents, status checks and OEM-aware steps.
  - **Inputs:** Context/OEM.
  - **Outputs:** Intents and status labels.
  - **Dependencies:** OEM detection, Settings intents.
  - **Consumed by:** `LockScreenPreferencesFragment`.
  - **Related files:** `PowerManagementGuide.kt`.
  - **Notes:** Reuses the battery guide's OEM detection.

- `util/oem/PowerManagementGuide.kt` — OEM power-management guide.
  - **Purpose:** Tailor background-playback guidance per OEM.
  - **Responsibilities:** Detect Xiaomi/Huawei/Samsung/OPPO/Realme/Vivo/ASUS/generic, battery-optimization status/intents, app details, auto-start and MIUI shortcut-permission intents.
  - **Inputs:** Context/build manufacturer.
  - **Outputs:** Guide rows and intents.
  - **Dependencies:** Android intents, `Build`.
  - **Consumed by:** `BatteryGuideFragment`, `LockScreenPermissionGuide`.
  - **Related files:** `preferences_screen_battery.xml`.
  - **Notes:** Uses `ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS`; no new permission.

#### Cast flavor sources and resources

- `app/src/cast/java/com/ssilencce/playback/cast/CastPlaybackImpl.kt` — Real Chromecast implementation.
  - **Purpose:** Create the media3 `CastPlayer` facade and own Cast session lifecycle.
  - **Responsibilities:** Build `CastPlayer` around the local player, register/unregister `SessionManagerListener`, start/stop the LAN server, show the device chooser, expose the Cast menu entry, end the receiver session on teardown and provide the process-wide `CastPlaybackProvider`.
  - **Inputs:** Application context, session events, player-menu inflation.
  - **Outputs:** Session player, chooser, toasts, server lifecycle.
  - **Dependencies:** media3-cast, mediarouter, play-services-cast-framework, `CastMediaServer`, `CastMediaItemConverter`.
  - **Consumed by:** `PlaybackService`, player fragments.
  - **Related files:** `CastPlayback.kt`, `src/cast/AndroidManifest.xml`.
  - **Notes:** Falls back to the local player when Play services/Cast is unavailable.

- `app/src/cast/java/com/ssilencce/playback/cast/CastMediaServer.kt` — Embedded LAN HTTP server.
  - **Purpose:** Serve the current queue's audio and artwork to the receiver.
  - **Responsibilities:** Bind a random LAN port, register tokenised resources, parse GET/HEAD, serve `Range`/`206`, use `AssetFileDescriptor`/file streams and clear resources on stop.
  - **Inputs:** Context, registered URIs/MIME types, HTTP requests.
  - **Outputs:** `baseUrl`, media streams and range responses.
  - **Dependencies:** `ServerSocket`, `ContentResolver`, `NetworkInterface`.
  - **Consumed by:** `CastPlaybackImpl`, `CastMediaItemConverter`.
  - **Related files:** `CastMediaItemConverter.kt`.
  - **Notes:** No wakelock/polling; only registered token paths are served.

- `app/src/cast/java/com/ssilencce/playback/cast/CastMediaItemConverter.kt` — Local↔Cast item adapter.
  - **Purpose:** Rewrite local content/file URIs to HTTP URLs for the receiver.
  - **Responsibilities:** Register audio/artwork, set Cast-friendly MIME/media types, delegate metadata serialization and map remote timeline items back to local items.
  - **Inputs:** Local `MediaItem`s, `MediaQueueItem`s, `CastMediaServer`.
  - **Outputs:** HTTP `MediaQueueItem`s; local `MediaItem`s.
  - **Dependencies:** media3 cast converter, `CastMediaServer`, `CoverProvider`.
  - **Consumed by:** `RemoteCastPlayer` in `CastPlaybackImpl`.
  - **Related files:** `CastMediaServer.kt`, `data/model/Song.kt`.
  - **Notes:** The media-id→item map is rebuilt after process recreation.

- `app/src/cast/java/com/ssilencce/playback/cast/SsilencceCastOptionsProvider.kt` — Cast SDK options.
  - **Purpose:** Supply options for GMS flavors.
  - **Responsibilities:** Use Google's default media receiver (`CC1AD845`) and stop the receiver on session end.
  - **Inputs:** Context.
  - **Outputs:** `CastOptions`.
  - **Dependencies:** play-services-cast-framework.
  - **Consumed by:** Cast SDK via the manifest metadata.
  - **Related files:** `src/cast/AndroidManifest.xml`.
  - **Notes:** The default receiver is sufficient because the app serves media.

- `app/src/nocast/java/com/ssilencce/playback/cast/CastPlaybackProvider.kt` — fdroid no-op provider.
  - **Purpose:** Keep fdroid GMS-free while satisfying the main contract.
  - **Responsibilities:** Report unavailable, hide the Cast item, return the local player and no-op release.
  - **Inputs:** Context.
  - **Outputs:** `NoOpCastPlayback`.
  - **Dependencies:** `CastPlayback` only.
  - **Consumed by:** `PlaybackService`, player fragments.
  - **Related files:** `CastPlayback.kt`.
  - **Notes:** No Cast/Play Services classes are referenced.

- `app/src/cast/AndroidManifest.xml` — Cast flavor manifest overlay.
  - **Purpose:** Declare the Cast options provider for github/playstore only.
  - **Responsibilities:** `OPTIONS_PROVIDER_CLASS_NAME` and `com.google.android.gms.version` metadata.
  - **Inputs:** None.
  - **Outputs:** Merged manifest metadata.
  - **Dependencies:** `SsilencceCastOptionsProvider`.
  - **Consumed by:** Android manifest merger.
  - **Related files:** `app/build.gradle.kts`.
  - **Notes:** Deliberately absent from fdroid.

- `res/drawable/ic_cast_24dp.xml`, `res/drawable/ic_screencast_24dp.xml`, `res/drawable/ic_crossfade_24dp.xml`, `res/drawable/ic_randomize_24dp.xml`, `res/drawable/ic_repeat_pause_24dp.xml`, `res/drawable/ic_bookmark_24dp.xml`, `res/drawable/ic_content_copy_24dp.xml`, `res/drawable/ic_push_pin_24dp.xml`, `res/drawable/ic_battery_charging_full_24dp.xml` — Integration icons.
  - **Purpose:** Toolbar/menu/status glyphs for Cast, screencast, crossfade, randomize, repeat-pause, bookmarks, copy, pin and battery guide.
  - **Responsibilities:** Static vector drawables referenced by the new menus and cards.
  - **Inputs:** None.
  - **Outputs:** Rendered icons.
  - **Dependencies:** AAPT2 vector resources.
  - **Consumed by:** Player menus, queue/notification controls, battery guide.
  - **Related files:** `res/menu/`, `res/drawable/`.
  - **Notes:** `ic_cast_24dp` is shared with EQ output routing.

- `res/layout/notification_custom_small.xml`, `res/layout/notification_custom_big.xml` — Custom notification RemoteViews.
  - **Purpose:** Define the Small and Big/Advanced notification layouts.
  - **Responsibilities:** Artwork, title/artist, progress, upcoming rows and action slots.
  - **Inputs:** Bound by `CustomLayoutMediaNotificationProvider`.
  - **Outputs:** Notification content/big content views.
  - **Dependencies:** RemoteViews-compatible views, media style.
  - **Consumed by:** `CustomLayoutMediaNotificationProvider`.
  - **Related files:** `NotificationStyle.kt`.
  - **Notes:** Android 11+ may clamp tall layouts; Android 13+ may replace them entirely.

- `res/xml/preferences_screen_battery.xml` — Battery & background guide layout.
  - **Purpose:** Render the OEM battery guide rows.
  - **Responsibilities:** Status and intent rows for battery optimization, app details, auto-start and MIUI shortcut permission.
  - **Inputs:** `BatteryGuideFragment`/`PowerManagementGuide`.
  - **Outputs:** Preference screen.
  - **Dependencies:** AndroidX Preference.
  - **Consumed by:** `BatteryGuideFragment`.
  - **Related files:** `PowerManagementGuide.kt`.
  - **Notes:** No new permission.

- `res/xml/preferences_screen_lock_screen.xml`, `res/xml/preferences_screen_lock_screen_permissions.xml` — Lock-screen settings layouts.
  - **Purpose:** Configure the lock-screen player and its permission guide.
  - **Responsibilities:** Enable/preview/wallpaper/behavior switches and overlay/notification/OEM rows.
  - **Inputs:** `LockScreenPreferencesFragment`.
  - **Outputs:** Preference screens.
  - **Dependencies:** AndroidX Preference.
  - **Consumed by:** `LockScreenPreferencesFragment`.
  - **Related files:** `LockScreenPermissionGuide.kt`.
  - **Notes:** Default disabled.

#### 2026-11 integration update notes

- `playback/PlaybackService.kt` — now builds the media session around the flavor session player (local or `CastPlayer` facade), owns crossfade/fade/volume/gapless, A-B repeat and per-song params, the song-count sleep timer, the ghost-search command and the screen-off lock-screen receiver; guards local-only persistence/DSP while casting and refreshes Auto/media-button/custom-notification state (ADR-052, ADR-053, ADR-055, ADR-058).
- `playback/Playback.kt` — added `SET_AB_REPEAT`/`GET_AB_REPEAT`, `SET_SONG_PLAYBACK_PARAMS`/`RESET_SONG_PLAYBACK_PARAMS`, `OPEN_GHOST_SEARCH`, `EVENT_REPEAT_LOAD_NEXT_PAUSE_CHANGED`, `EXTRA_LOAD_NEXT_PAUSE_ACTIVE`, shuffle-state events and crossfade/cast-adjacent extras.
- `playback/PlaybackService.kt` notes — a crossfade commit is presented as an automatic transition; local crossfade is cancelled/suppressed while Cast owns playback or when A-B/stop-after/repeat-load-next applies; the notification progress ticker follows the active player.
- `playback/PersistentStorage.kt` — persists `shuffle_state` with legacy fallback and restores a `RandomizeShuffleOrder` when randomize was active.
- `playback/AudioFocusController.kt` — ducking delegates to `PlaybackVolumeController`; no direct `player.volume` writes; confirmed-Ask/focus-gain play arms the fade-in.
- `playback/ImprovedShuffleOrder.kt` — optional `keepPrefix` mode (ordered prefix, shuffled tail) for below-current shuffle.
- `playback/QueueReshufflePolicy.kt` — reshuffle is unavailable while randomize is active.
- `data/model/Song.kt` — `discNumber` (MediaStore API 30+), resolved artist name in Media3 metadata, MIME from extension, extra-format URI lookup and album-id/disc extras in `toMediaItem()`.
- `data/model/Artist.kt`, `ExpandedSong.kt`, `QueueSong.kt` — fallback artist name/stable synthetic id and disc-number round-trips.
- `data/local/repository/ArtistRepository.kt`, `AlbumRepository.kt`, `GenreRepository.kt`, `SmartRepository.kt`, `PlaylistRepository.kt` — album-artist fallback grouping/search, disc-then-track opening song, genre duration parity, ghost filtering on Room-backed mappings and missing-song cleanup queries.
- `data/local/room/PlayCountDao.kt`, `PlaylistDao.kt`, `HistoryDao.kt`, `PlayEventDao.kt` — `allPlayStatistics()` and maintenance count/delete queries.
- `data/local/LibraryRescan.kt`, `App.kt`, `MainActivity.kt` — first-run gate, process-scoped mount receiver and chosen-folder rescan wiring.
- `data/local/MetadataWriter.kt` — `writeBatch(cover, journal, albumIdBySongId, onProgress, shouldContinue)` with current-file rollback, cover read-back and album-art refresh; `write(context, target)` unchanged.
- `core/model/tag/TagEditReport.kt` — cover status, `holdsChanges`, cover counters and cover-aware `isFullSuccess`.
- `ui/screen/tageditor/BatchTagEditorViewModel.kt`, `BatchTagEditorScreen.kt`, `BatchTagEditorActivity.kt` — cover picking, journal/undo states, skipped reports, busy-state close blocking and SAVE/UNDO write requests.
- `ui/component/menu/MenuItemClickExt.kt` — shared `launchBatchTagEditor`, trim-audio, preview, move/copy/hide, shuffle-and-then, three queue actions and single-entity album/artist routing.
- `ui/component/menu/MenuClick.kt` — `newPopupMenu` applies `MenuCustomizationFilter` before the caller's preparation.
- `ui/component/base/AbsPlayerFragment.kt` — keep-screen-on owner, fullscreen art/share/seek-to/screencast/cast/queue-action/A-B overflow entries, quick-access overlay and bookmark/notes actions.
- `ui/component/base/AbsPlayerControlsFragment.kt` — elapsed-time seek dialog, thumb-only slider mode and fourth repeat-state icon/color.
- `ui/component/views/MusicSlider.kt` — `touchOnlyOnThumb` blocks track taps while preserving dragging.
- `ui/screen/player/PlayerViewModel.kt` — A-B mirror/staging, per-song params, tri-state shuffle, queue actions, named-queue merge/eviction, long-audio seek gating and `loadNextSongAndPause`.
- `ui/screen/player/aurora/AuroraPlayerScreen.kt` — quick-access row, seek dialog, thumb-only slider, Cast/screencast overflow, fourth repeat state and A-B chip host.
- `ui/screen/player/cover/CoverPagerFragment.kt` — A-B chip ComposeView and keep-screen-on preference checks.
- `ui/screen/queue/QueueScreen.kt` notes — block drag with two or more selected rows uses a block-only `detectDragGestures` path: the dragged row center is resolved from `listState.layoutInfo` by `QueueSong.key`, the accumulated delta selects the row under it and `blockDropTarget` is key-resolved (never a LazyList index or `queueContentOffset`); `userScrollEnabled` is disabled during block drag. The single-item path keeps the reorderable handle.
- `ui/screen/queue/QueuesFragment.kt` — saved-queue rows start the merge flow.
- `ui/screen/settings/PreferencesScreenFragment.kt` — menu customization, notification layout/style/background, quick access, lock screen, setup wizard, rescan/auto-rescan, remove-missing-songs, accent color, unimportant-settings and extra-format scan wiring.
- `ui/screen/MainActivity.kt` — first-run setup redirect, lock-screen/ghost entity routing, add-to-playlist notification action, stop-after/queue feedback and repeat-policy forwarding.
- `ui/screen/lockscreen/LockScreenPlayerScreen.kt` — optional ghost-search entry when Ghost mode is enabled; delegates to GhostSearch so the PIN gate applies.
- `ui/screen/lockscreen/LockScreenEntityLauncher.kt` — also routes GhostSearch openings.
- `ui/screen/library/folders/FoldersListFragment.kt`, `FolderDetailFragment.kt` — folder info/create/rename/exclude/move/copy actions, mode-aware menu visibility, resume refresh and stale-folder pop.
- `ui/screen/library/albums/AlbumDetailFragment.kt` — one song adapter per disc plus section headers; quick search/compact toggling updates all groups.
- `ui/screen/library/genres/GenreDetailFragment.kt`, `YearsFragment`/`YearDetailFragment.kt` — duration parity and Genre menu-customization group on the shared detail overflow.
- `ui/screen/library/search/SearchFragment.kt` — removable search-history chips, commit on submit/result click and empty-state interplay.
- `ui/screen/lyrics/LyricsScreen.kt` — select-lines action (coexisting with A-B long-press), source picker, SAF document pick and selectable plain-lyric rows.
- `ui/component/compose/lyrics/LyricsView.kt` — normal-mode tap seeks, long-press opens A-B; in selection mode tap and long-press toggle lines; selected lines highlight and auto-scroll pauses.
- `ui/theme/Theme.kt`, `core/model/theme/AppTheme.kt` — custom accent seed applied to Compose and legacy DynamicColors.
- `coil/fetcher/AudioCoverFetcher.kt`, `ArtistImageFetcher.kt`, `AutoGeneratedImageFetcher.kt`, `PlaylistImageFetcher.kt` — short-circuit when album art is disabled.
- `res/menu/menu_now_playing.xml` — A-B repeat, fullscreen art, seek-to, share, screencast and cast entries.
- `res/menu/menu_media_selection.xml` — Shuffle play and Shuffle and then… CAB entries.
- `res/values/strings.xml` — English labels for all new controls; no translated file was edited.
- `res/xml/preferences_screen_library.xml` — extra-format indexing and folder scope/rescan rows.
- `res/xml/preferences_screen_playback.xml` — crossfade/fade/gapless/transport, notification layout/style, lock-screen trigger and sleep-timer foreground-only rows.
- `res/xml/preferences_screen_now_playing.xml` — keep screen on, touch-only-on-thumb and quick-access shortcuts.
- `res/xml/provider_paths.xml` — `now_playing_share/` cache path for the screenshot.
- `res/navigation/graph_main.xml`, `graph_settings.xml` — notes, ghost search/settings and lock-screen destinations.
- `app/src/main/AndroidManifest.xml` — `AudioCutterActivity`, `MiniPlayerActivity` (audio VIEW filters moved off the MainActivity alias), `SetupWizardActivity`, `LockScreenPlayerActivity`, `SET_WALLPAPER`, `WRITE_SETTINGS` and the lock-screen dialog theme.
- `app/build.gradle.kts` — per-flavor Chromecast source sets (`src/cast` github/playstore, `src/nocast` fdroid), `media3-cast` dependency (existing media3 version) and cast manifest overlay.
- `gradle/libs.versions.toml` — `media3-cast` reuses the existing `media3` version.
- `core/model/audiodevice/AudioDeviceType.kt` — adds the `Cast` type mapped from MediaRouter TV/speaker/remote-speaker/AVR/group routes with a hashed route key.
- `core/model/equalizer/EqBandCapabilities.kt`, `EqEngineMode.kt`, `EqState.kt` — safe capability/frequency lookup, closest band count and resampled bands; nullable effect UUID plus the `System` mode; `usesSystemEqualizer` and `isUsable` excluding System.
- `playback/equalizer/EqualizerManager.kt` — resamples the active curve on band-count changes, migrates legacy 32-band data, normalizes the stored count to the engine layout, accepts legacy imports and releases the in-app engine/opens the OEM session for System mode; owns mono/reverb keys and the session-bound reverb lifecycle.
- `playback/equalizer/engine/DynamicsProcessingEngine.kt` — 3/5/10/15/31 ISO layouts from `EqBandFrequencies` (31 replaces 32).
- `ui/screen/equalizer/EqualizerScreen.kt`, `EqualizerViewModel.kt` — always-visible band-count selector (disabled with explanation on Legacy), System engine entry/filtering and card, Aurora pages hidden for System, system-panel availability/open without a session, reverb card and ReplayGain extras.
- `playback/processor/ReplayGainAudioProcessor.kt` — `useEmbeddedTags`, `preventClipping`, `headroomDb` and `constantVolume` controls, all preserving previous defaults.
- `ui/screen/sound/SoundSettingsScreen.kt` — mono output switch, crossfade card and per-song tempo controls.
