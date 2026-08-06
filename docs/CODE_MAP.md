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


- `app/build.gradle.kts` — Module build: 3 flavors (github/fdroid/playstore), semantic version classes (Alpha/Beta/RC/Stable), signing, ABI splits, Last.fm key injection, CI APK naming with git hash.
  - **Purpose:** Android application module configuration for `com.ssilencce`.
  - **Responsibilities:** Flavors, sealed `Version` classes, signing config, ABI splits, BuildConfig/ResValue injection, lint, dependencies.
  - **Inputs:** `keystore.properties`, `local.properties`, `public.properties`, `RELEASE_TYPE` env, git hash, flavor names.
  - **Outputs:** `ssilencce-<version>-<flavor>-<abi>.apk`; BuildConfig `IS_CI_BUILD`, `LASTFM_API_KEY`, `LASTFM_SECRET`, flavor props.
  - **Dependencies:** AGP 9.3.1, KSP, Kotlin 2.3.21, `jvmToolchain(21)`, version catalog bundles.
  - **Consumed by:** Gradle wrapper, `.github/workflows/android.yml` and `release.yml`.
  - **Related files:** `gradle/libs.versions.toml`, `public.properties`, `AndroidManifest.xml`.
  - **Notes:** `check(versionCode == currentVersionCode)`; current `Version.Beta(1,4,0,3)` maps to 1400103; `github`/`fdroid` add `src/shared/java`.


- `gradle/libs.versions.toml` — Version catalog for all dependencies (Media3, Compose, Koin, Ktor, Coil, Room, TagLib, etc.).
  - **Purpose:** Central version catalog for plugins and libraries.
  - **Responsibilities:** `[versions]`, `[libraries]`, `[bundles]`, `[plugins]` sections.
  - **Inputs:** Coordinates: AGP 9.3.1, Kotlin 2.3.21, Media3 1.10.1, Room 2.8.4, Koin 4.2.2.
  - **Outputs:** Typed accessors used by both Gradle scripts.
  - **Dependencies:** Google, Maven Central, JitPack repositories.
  - **Consumed by:** `build.gradle.kts`, `app/build.gradle.kts`.
  - **Related files:** `gradle.properties`, `settings.gradle.kts`.
  - **Notes:** Bundles: kotlinx, lifecycle, media3, navigation, glance, koin, coil, ktor, markwon.


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
  - **Notes:** Fork still points `GITHUB_API_URL` at upstream `mardous/BoomingMusic`.


- `app/src/main/AndroidManifest.xml` — Permissions, activities, PlaybackService (MediaLibraryService), widget receivers, FileProvider, CoverProvider, Android Auto, searchable config.
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

- `core/SsilencceDatabase.kt` — Room database (v6) with all entities and migrations.
  - **Purpose:** Room database v6 for persistent app data.
  - **Responsibilities:** Declares 7 entities, 6 DAOs, 5 migrations (1_2 to 5_6).
  - **Inputs:** Entity class declarations, migration SQL statements.
  - **Outputs:** DAO instances, migrated SQLite schema.
  - **Dependencies:** androidx.room (Database, RoomDatabase, Migration), data/local/room package.
  - **Consumed by:** MainModule.kt (Room.databaseBuilder, DAO singletons), repository implementations.
  - **Related files:** PlaylistEntity, SongEntity, HistoryEntity, PlayCountEntity, QueueEntity, InclExclEntity, LyricsEntity.
  - **Notes:** exportSchema=false; 3_4 creates CanvasEntity, dropped again in 4_5; 5_6 recreates LyricsEntity.


- `core/appwidgets/SsilencceGlanceWidget.kt` — Responsive Glance widget with 5 layouts showing cover, title, artist and controls.
  - **Purpose:** Responsive main Glance home-screen widget.
  - **Responsibilities:** 5 layouts (Classic, Simplified, Medium, Large, ExtraLarge), playback/shuffle/favorite controls.
  - **Inputs:** PlaybackState from DataStore, LocalSize, WidgetTheme colors.
  - **Outputs:** Composed Glance UI, media-key Actions to PlaybackService.
  - **Dependencies:** Glance (GlanceAppWidget, SizeMode.Exact), PlaybackState, PlaybackStateDefinition, WidgetComponent.
  - **Consumed by:** SsilencceGlanceWidgetReceiver; AndroidManifest receiver.
  - **Related files:** CardWidget, FullWidget, WidgetComponent, WidgetExtension.
  - **Notes:** Click opens MainActivity; small layout chooses Simplified vs Classic per state flag.


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
  - **Notes:** SizeMode.Exact; whole widget clickable to MainActivity.


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
  - **Notes:** Scrim from primaryContainer; showAllActions when width ≥ 220dp.


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
  - **Responsibilities:** AlbumArtGlance decode + placeholder, ControlIconGlance, CircularControlIconGlance, ShapeableControlIconGlance.
  - **Inputs:** PlaybackState, drawable resIds, Dp sizes, ColorProviders, GlanceModifier.
  - **Outputs:** Reusable widget UI building blocks.
  - **Dependencies:** Glance (Image, ImageProvider, ColorFilter), BitmapFactory, PlaybackState.
  - **Consumed by:** SsilencceGlanceWidget, CardWidget, FullWidget.
  - **Related files:** PlaybackState, WidgetExtension.
  - **Notes:** Tinted music-note placeholder fallback; logs decode errors.


- `core/appwidgets/WidgetExtension.kt` — Widget helpers: Dp→px, theme building with dynamic colors, service intents.
  - **Purpose:** Widget helpers for sizing, theming and service intents.
  - **Responsibilities:** Dp→px, WidgetTheme from dynamicColorSchemes, getColors ColorProviders, 4 service Actions.
  - **Inputs:** Context, source color, PaletteStyle, media key codes.
  - **Outputs:** ColorProviders, playbackAction/toggleShuffleAction/cycleRepeatAction/toggleFavoriteAction.
  - **Dependencies:** Glance, dynamicColorSchemes (ui/theme/Monet), PlaybackService.
  - **Consumed by:** SsilencceGlanceWidget, CardWidget, FullWidget, PlaybackService.
  - **Related files:** PlaybackState (WidgetTheme), PlaybackService.
  - **Notes:** PaletteStyle.Fidelity, contrast 0.75; intents target PlaybackService component.


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


- `core/audio/AudioOutputObserver.kt` — BroadcastReceiver exposing Flows for current audio device, system volume and bit-perfect state.
  - **Purpose:** Observe audio output device, volume and bit-perfect state.
  - **Responsibilities:** BroadcastReceiver + AudioDeviceCallback, StateFlows, USB bit-perfect config (API 34+).
  - **Inputs:** AudioManager, MediaRouter route, volume intents, playback sample rate/channels.
  - **Outputs:** audioDevice, systemVolumeState, bitPerfectState, availableBitPerfectDevices Flows.
  - **Dependencies:** AudioManager, MediaRouter, AudioDevice/AudioDeviceType/BitPerfectState, VolumeState, SystemMediaControlResolver.
  - **Consumed by:** MainModule, PlaybackService, EqualizerManager, EqualizerViewModel.
  - **Related files:** core/model/audiodevice/*, util/oem/SystemMediaControlResolver.
  - **Notes:** setPreferredMixerAttributes USB-only; headset/volume actions; export receiver.


- `core/audio/AutoEqTxtParser.kt` — Parser for AutoEq text files (`GraphicEQ: ...`) producing an AutoEqProfile.
  - **Purpose:** Parse AutoEq text files into AutoEqProfile.
  - **Responsibilities:** Find GraphicEQ line, split frequency/gain pairs, derive preset name from Uri.
  - **Inputs:** Context, content Uri, InputStream via ContentResolver.
  - **Outputs:** AutoEqProfile (name, points, preamp) or null.
  - **Dependencies:** AutoEqProfile/AutoEqPoint models, OpenableColumns.
  - **Consumed by:** EqualizerViewModel (AutoEq TXT import).
  - **Related files:** core/model/equalizer/autoeq/AutoEqProfile, EqualizerScreen.
  - **Notes:** Skips malformed bands; fallback name "AutoEq Preset"; strips .txt suffix.


- `core/model/CategoryInfo.kt` — Parcelable navigation category mapping (Home, Songs, Albums...) with visibility (max 5).
  - **Purpose:** Model visible library navigation categories and their ordering.
  - **Responsibilities:** Define eight Category entries with nav-id, title, icon; track visibility; cap at five visible.
  - **Inputs:** User reorder/visibility from Preferences.libraryCategories.
  - **Outputs:** Visible category list driving MainActivity navigation.
  - **Dependencies:** R.id, R.string, R.drawable, kotlinx Parcelable/Serializable.
  - **Consumed by:** Preferences, CategoryInfoAdapter, CategoriesPreferenceDialog, MainActivity, LibraryProvider.
  - **Related files:** ui/adapters/preference/CategoryInfoAdapter.kt, ui/component/preferences/dialog/CategoriesPreferenceDialog.kt.
  - **Notes:** MAX_VISIBLE_CATEGORIES enforced by adapter check logic; serialized to SharedPreferences JSON.


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
  - **Responsibilities:** Combine margin, additionalSpace, bottomInsets; expose totalMargin and getWithSpace.
  - **Inputs:** Base margin, optional space, inset values.
  - **Outputs:** Total pixel margin for layout padding.
  - **Dependencies:** None.
  - **Consumed by:** LibraryViewModel (fabMargin, miniPlayerMargin), LyricsScreen, EqualizerScreen.
  - **Related files:** ui/screen/library/LibraryViewModel.kt, ui/component/base/AbsRecyclerViewFragment.kt.
  - **Notes:** includeInsets flag controls insets contribution.


- `core/model/MediaEvent.kt` — Media event enum (content/favorite changed, playback restored/started).
  - **Purpose:** Signal media-collection and playback lifecycle changes.
  - **Responsibilities:** Define four event kinds for ViewModel/UI notification.
  - **Inputs:** — (pure enum).
  - **Outputs:** Events submitted to PlayerViewModel.
  - **Dependencies:** None.
  - **Consumed by:** MainActivity, AbsMainActivityFragment, PlayerViewModel.
  - **Related files:** ui/screen/player/PlayerViewModel.kt, ui/component/base/AbsMainActivityFragment.kt.
  - **Notes:** submitEvent routes to LiveData/Flow consumers.


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


- `core/model/audiodevice/AudioDevice.kt` — Output device model resolving displayable name by type.
  - **Purpose:** Represent the current audio output device.
  - **Responsibilities:** Resolve device name from product name, manufacturer/model, or type label.
  - **Inputs:** AudioDeviceType, product name.
  - **Outputs:** Display name string.
  - **Dependencies:** Context, Build.
  - **Consumed by:** AudioOutputObserver, EqualizerManager, SoundSettingsScreen.
  - **Related files:** core/audio/AudioOutputObserver.kt, playback/equalizer/EqualizerManager.kt.
  - **Notes:** UnknownDevice constant for unset state.


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


- `core/model/equalizer/EqProfile.kt` — Serializable EQ profile (name, band levels, device associations, custom/AutoEq).
  - **Purpose:** Persist a named EQ preset.
  - **Responsibilities:** Store levels/associations; validate; localized name/description; custom equals/hashCode.
  - **Inputs:** Name, FloatArray levels, device associations.
  - **Outputs:** JSON-serializable profile for export/import.
  - **Dependencies:** kotlinx.serialization, AudioDeviceType, R.
  - **Consumed by:** EqualizerManager, EqualizerViewModel, EqualizerScreen.
  - **Related files:** playback/equalizer/EqualizerManager.kt, ui/screen/equalizer/EqualizerViewModel.kt.
  - **Notes:** CUSTOM_PRESET_NAME renders localized "Custom".


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
  - **Responsibilities:** Define eight modes with icon/title/description resources.
  - **Inputs:** — (pure enum).
  - **Outputs:** Mode for ShuffleManager.applySmartShuffle.
  - **Dependencies:** R.drawable, R.string.
  - **Consumed by:** ShuffleManager, ShuffleModeBottomSheet, ShuffleModeFragment.
  - **Related files:** playback/shuffle/ShuffleManager.kt, ui/screen/other/ShuffleModeBottomSheet.kt.
  - **Notes:** Includes MostPlayedArtists/MostPlayedAlbums variants.


- `core/model/sort/SortItem.kt` — Sort menu items (KeySortItem with all keys, DescendingItem).
  - **Purpose:** Model sort menu entries.
  - **Responsibilities:** Define KeySortItem objects per SortKey plus DescendingItem; group ids.
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
  - **Notes:** Values like "az_key" kept for backward compatibility.


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


- `core/sort/SortCollator.kt` — Cached locale Collator and name-based sorting helpers (ignores articles).
  - **Purpose:** Provide cached locale Collators and name sorting helpers.
  - **Responsibilities:** Frozen cached collators (PRIMARY, numeric), sortedByName, sortedByMediaName.
  - **Inputs:** Locale, Iterable plus name selector.
  - **Outputs:** Collator instances, sorted lists.
  - **Dependencies:** android.icu.text.Collator, normalizeForSorting, Preferences.
  - **Consumed by:** SortMode, SpecialRepository, ArtistDetailViewModel.
  - **Related files:** core/sort/SortMode, extensions/media/DataExt.
  - **Notes:** ConcurrentHashMap cache; article ignoring via Preferences.


- `core/sort/SortMode.kt` — Sealed sort-mode base: persists key/descending, builds sort menus; subclasses per entity.
  - **Purpose:** Sealed base persisting sort key/descending and building menus.
  - **Responsibilities:** SharedPreferences persistence, sort menu creation/selection, per-entity sorted() implementations.
  - **Inputs:** SharedPreferences, Menu/MenuItem, entity collections.
  - **Outputs:** Sorted lists, checked menu state.
  - **Dependencies:** Koin (KoinComponent), Preferences, SortKey/SortItem, sortingCollator, PlaylistWithSongs.
  - **Consumed by:** Library fragments (SongListFragment, AlbumListFragment, ArtistListFragment, GenresListFragment, YearsListFragment, PlaylistListFragment, FoldersListFragment, GenreDetailFragment, FolderDetailFragment, AlbumDetailFragment, ArtistDetailFragment, YearDetailFragment).
  - **Related files:** SortCollator, SortKey, SortItem.
  - **Notes:** Preference keys "<id>_sort_order"/"<id>_descending"; SongSortMode.Dynamic variant.

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


- `data/model/Song.kt` — Main song model (parcelable, FileSystemItem) with uri helpers and Song.emptySong.
  - **Purpose:** Central parcelable song model implementing FileSystemItem.
  - **Responsibilities:** MediaStore fields, volume-aware uri, toMediaItem(), equality, emptySong.
  - **Inputs:** MediaStore cursor data, volumeName.
  - **Outputs:** Song instances, MediaItem, content URIs.
  - **Dependencies:** CoverProvider, getAudioContentUri, FileSystemItem, MediaItem.
  - **Consumed by:** SongRepository, PlaybackExtension.toMediaItems, PlayerViewModel, SongMapper, adapters.
  - **Related files:** ExpandedSong.kt, QueueSong.kt, Album/Artist/Folder/ReleaseYear.
  - **Notes:** Open class subclassed by ExpandedSong and QueueSong.


- `data/model/Album.kt` — Album with songs (SongProvider), name, duration, isSingle, cover and Album.empty.
  - **Purpose:** Album grouping songs, deriving name/artist from first song.
  - **Responsibilities:** songCount/duration/dateAdded/isSingle, albumCover, safeGetFirstSong.
  - **Inputs:** id, artistName, albumArtistName, year, firstSongIndex, songs.
  - **Outputs:** Album with computed metadata and cover URI.
  - **Dependencies:** SongProvider, Song, asAlbumCoverUri.
  - **Consumed by:** AlbumRepository.splitIntoAlbums, AlbumDetailViewModel/Fragment, InfoViewModel, PlayInfoFragment.
  - **Related files:** Artist.kt, Song.kt, extensions/media/DataExt.kt.
  - **Notes:** UNKNOWN_ALBUM_DISPLAY_NAME constant; equality by id and songs.


- `data/model/Artist.kt` — Artist with albums (SongProvider), counts, duration, album-artist support.
  - **Purpose:** Artist/album-artist grouping albums with sort and counts.
  - **Responsibilities:** Name resolution, albumCount/songCount/duration, sortedAlbums/sortedSongs, safeGetFirstAlbum.
  - **Inputs:** id, albums, filterSingles, isAlbumArtist.
  - **Outputs:** Artist with aggregated metrics.
  - **Dependencies:** Album, SongProvider, SongSortMode.ArtistSongs.
  - **Consumed by:** ArtistRepository, ArtistDetailViewModel/Fragment, TagEditorViewModel, PlayInfoFragment.
  - **Related files:** Album.kt, core/sort/SongSortMode, VARIOUS_ARTISTS constants.
  - **Notes:** Secondary constructor derives id from first album's artistId.


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
  - **Notes:** Key matches QueueEntity (id + order) composite.


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


- `data/model/replaygain/ReplayGainMode.kt` — ReplayGain mode enum (Album/Track/Off) and ReplayGain data class (gains/peaks).
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

- `data/local/MediaQueryDispatcher.kt` — Fluent MediaStore query builder dispatching cursors via ContentResolver.
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
  - **Responsibilities:** Insert ContentValues; stream output; notifyChange; plain-file fallback; Request/Result helpers.
  - **Inputs:** contentUri, Request (name/path/mime), streamConsumer, optional File.
  - **Outputs:** Result code (SUCCESS/NO_SCOPED_STORAGE/ERROR) with destination Uri.
  - **Dependencies:** ContentResolver, MediaStore, FileUtil.PLAYLISTS_DIRECTORY_NAME.
  - **Consumed by:** M3UWriter, EqualizerViewModel; registered in MainModule.
  - **Related files:** M3UWriter.kt, EqualizerViewModel.kt, MainModule.kt.
  - **Notes:** Requires API 29+ (hasQ); deletes inserted rows on failure.


- `data/local/MetadataReader.kt` — Reads audio metadata with TagLib (properties, tags, images, bitrate, sample rate, channel mode).
  - **Purpose:** Read audio file metadata and properties via TagLib.
  - **Responsibilities:** Expose tags/propertyMap, pictures, bitrate, sample rate, channel mode, genre normalization.
  - **Inputs:** Uri, readPictures flag.
  - **Outputs:** Metadata accessors (all/get/first/merge/frontCover), Bitmap via Picture.toBitmap.
  - **Dependencies:** TagLib (com.kyant.taglib), ContentResolver, Jaudiotagger GenreTypes.
  - **Consumed by:** InfoViewModel, TagEditorViewModel, MetadataField, ReplayGainTagExtractor, LyricsRepository.
  - **Related files:** MetadataWriter.kt, ReplayGainTagExtractor.kt, MetadataField.kt.
  - **Notes:** Catches all throwables; dup()-detached file descriptors.


- `data/local/MetadataWriter.kt` — Writes metadata and covers via TagLib (batch) and launches MediaScanner.
  - **Purpose:** Batch-write metadata properties and covers to audio files.
  - **Responsibilities:** Build EditTarget; write propertyMap/pictures via TagLib; album-art thumb; MediaScanner scan.
  - **Inputs:** EditTarget (Song/Artist/Album), propertyMap, picture bitmap, deletion flag.
  - **Outputs:** WriteResult (contents, failed, scanned counts).
  - **Dependencies:** TagLib, ContentResolver, MediaScannerConnection, insertAlbumArt/deleteAlbumArt extensions.
  - **Consumed by:** TagEditorViewModel, LyricsRepository.saveLyrics.
  - **Related files:** MetadataReader.kt, EditTarget (same file), TagEditorViewModel.kt.
  - **Notes:** Filters REPLAYGAIN tags to avoid duplicates; runs on IO dispatcher.


- `data/local/ReplayGainTagExtractor.kt` — Extracts ReplayGain tags (standard, iTunes `----:com.apple.iTunes:`, Opus R128) with LruCache per Uri.
  - **Purpose:** Extract ReplayGain values from audio tag variants.
  - **Responsibilities:** Parse standard/iTunes/Opus R128 tags; cache per Uri (LruCache 128); invalidation.
  - **Inputs:** Song.
  - **Outputs:** ReplayGain (album/track gains and peaks).
  - **Dependencies:** MetadataReader, ReplayGain model, LruCache.
  - **Consumed by:** PlaybackService, DataExt.kt, App.kt (clearCache).
  - **Related files:** MetadataReader.kt, ReplayGainMode.kt, PlaybackService.kt.
  - **Notes:** R128 converted via (raw/256f)+5 dB.


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


- `data/local/room/QueueEntity.kt` — Room entity for a saved queue item (id + order).
  - **Purpose:** Room entity for a persisted queue item.
  - **Responsibilities:** Store media id and its order position.
  - **Inputs:** id (mediaId), order.
  - **Outputs:** QueueEntity row.
  - **Dependencies:** Room.
  - **Consumed by:** QueueDao, PersistentStorage.
  - **Related files:** QueueDao.kt, PersistentStorage.kt.
  - **Notes:** Simple key-value pair, no relations.


- `data/local/room/QueueDao.kt` — Queue DAO: save, list ordered, prune missing and transactional replace.
  - **Purpose:** Persist and restore the playback queue.
  - **Responsibilities:** Replace queue transactionally; save; list ordered; prune absent ids.
  - **Inputs:** List<QueueEntity>.
  - **Outputs:** Ordered QueueEntity list.
  - **Dependencies:** Room (@Dao, @Transaction).
  - **Consumed by:** PersistentStorage.
  - **Related files:** QueueEntity.kt, PersistentStorage.kt.
  - **Notes:** removeNotIn keeps stored rows in sync.


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


- `data/local/room/PlaylistDao.kt` — Playlists/songs DAO: CRUD, relations as LiveData/Flow, searches, cascade delete.
  - **Purpose:** Room DAO for playlists and their songs.
  - **Responsibilities:** CRUD playlists/songs; relation queries LiveData/Flow; searches; cascade deletes; favorites.
  - **Inputs:** PlaylistEntity/SongEntity, ids, names.
  - **Outputs:** Entities, PlaylistWithSongs, LiveData/Flow lists.
  - **Dependencies:** Room, LiveData, Flow.
  - **Consumed by:** RealPlaylistRepository, SsilencceDatabase.
  - **Related files:** PlaylistEntity.kt, SongEntity.kt, PlaylistWithSongs.kt.
  - **Notes:** favoritesSongsFlow matches playlist by name.


- `data/local/room/PlayCountEntity.kt` — Room play-count entity (playCount, skipCount, timePlayed) with song snapshot.
  - **Purpose:** Room entity tracking per-song playback statistics.
  - **Responsibilities:** Persist song snapshot plus timePlayed/playCount/skipCount.
  - **Inputs:** Song data via toPlayCount mapping.
  - **Outputs:** PlayCountEntity row.
  - **Dependencies:** Room.
  - **Consumed by:** PlayCountDao, SongMapper, InfoResult.
  - **Related files:** PlayCountDao.kt, SongMapper.kt, InfoResult.kt.
  - **Notes:** id is the MediaStore song id.


- `data/local/room/PlayCountDao.kt` — Play-count DAO: upsert, transactional play/skip increments, top 100 in Flow.
  - **Purpose:** Room DAO for play/skip counting.
  - **Responsibilities:** Upsert; transactional increments; top-100 queries (list/Flow); delete/clear.
  - **Inputs:** Song, timePlayed, song ids.
  - **Outputs:** PlayCountEntity lists and Flows.
  - **Dependencies:** Room (@Dao, @Upsert, @Transaction), SongMapper.toPlayCount.
  - **Consumed by:** RealSmartRepository.
  - **Related files:** PlayCountEntity.kt, SmartRepository.kt.
  - **Notes:** PLAY_COUNT_LIMIT constant is 100.


- `data/local/room/LyricsEntity.kt` — Room cached-lyrics entity (song id, lyrics, provider, isInstrumental).
  - **Purpose:** Room entity caching lyrics per song.
  - **Responsibilities:** Store lyrics text, provider, instrumental flag.
  - **Inputs:** id, lyrics, provider, instrumental.
  - **Outputs:** LyricsEntity row.
  - **Dependencies:** Room.
  - **Consumed by:** LyricsDao, RealLyricsRepository, BackupHelper.
  - **Related files:** LyricsDao.kt, LyricsRepository.kt.
  - **Notes:** Instrumental songs stored without lyrics text.


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

- `data/local/repository/Repository.kt` — Main Repository interface: songs/albums/artists/genres/years/folders/playlists, searches, favorites, scrobbling.
  - **Purpose:** Single facade over all local and network data.
  - **Responsibilities:** Delegate to sub-repositories; delete cleanup; home suggestions; search; scrobbling.
  - **Inputs:** Queries, ids, uris, MediaItems, SearchFilter.
  - **Outputs:** Songs, albums, artists, playlists, suggestions, scrobble results.
  - **Dependencies:** RealSongRepository, RealAlbumRepository, RealArtistRepository, RealGenreRepository, RealSmartRepository, RealSpecialRepository, RealPlaylistRepository, RealSearchRepository, NetworkRepositoryImpl.
  - **Consumed by:** LibraryViewModel, PlayerViewModel, PlaybackService, LibraryProvider.
  - **Related files:** All repository implementations, MainModule.kt.
  - **Notes:** MAX_ITEMS_PER_CHUNK (900) batching.


- `data/local/repository/SongRepository.kt` — Song queries from MediaStore (cursors, uris, MediaItems, blacklist) + RealSongRepository.
  - **Purpose:** MediaStore song queries and cursor-to-Song mapping.
  - **Responsibilities:** Build song cursors; blacklist/whitelist selection; Uri/MediaItem resolution; blacklist init.
  - **Inputs:** Selections, uris, MediaItems, file paths.
  - **Outputs:** Song lists, Songs, missing MediaItems.
  - **Dependencies:** MediaQueryDispatcher, InclExclDao, Preferences, SongSortMode.
  - **Consumed by:** RealRepository and all sibling repositories.
  - **Related files:** MediaQueryDispatcher.kt, InclExclDao.kt, AlbumRepository.kt.
  - **Notes:** BASE_SELECTION filters IS_MUSIC and non-empty titles.


- `data/local/repository/AlbumRepository.kt` — Albums from MediaStore grouping songs (splitIntoAlbums) + RealAlbumRepository.
  - **Purpose:** Album aggregation from MediaStore songs.
  - **Responsibilities:** Group songs into Albums; sort; similar albums; min-song filtering.
  - **Inputs:** Queries, album ids, Songs.
  - **Outputs:** Album lists and single Album.
  - **Dependencies:** RealSongRepository, Preferences, AlbumSortMode, SongSortMode.
  - **Consumed by:** RealRepository, RealSearchRepository, SmartSearchFilter, LastAddedSearchFilter.
  - **Related files:** SongRepository.kt, ArtistRepository.kt.
  - **Notes:** splitIntoAlbums reused by other repositories.


- `data/local/repository/ArtistRepository.kt` — Artists/album-artists grouped into albums + RealArtistRepository.
  - **Purpose:** Artist and album-artist aggregation.
  - **Responsibilities:** Group albums by artist; Various Artists special case; similar album artists via genre.
  - **Inputs:** Queries, artist ids, names.
  - **Outputs:** Artist lists, single Artist.
  - **Dependencies:** RealSongRepository, RealAlbumRepository, MediaQueryDispatcher, Preferences.
  - **Consumed by:** RealRepository, RealSearchRepository, LastAddedSearchFilter.
  - **Related files:** AlbumRepository.kt, SongRepository.kt.
  - **Notes:** splitIntoAlbumArtists shared with SmartRepository.


- `data/local/repository/GenreRepository.kt` — Genres and their songs via MediaStore.Genres + RealGenreRepository.
  - **Purpose:** Genre queries via MediaStore.Genres.
  - **Responsibilities:** List genres with counts; genre songs incl. no-genre set; per-genre search.
  - **Inputs:** Genre ids, queries, Song.
  - **Outputs:** Genre lists, Song lists.
  - **Dependencies:** ContentResolver, RealSongRepository, GenreSortMode, SongSortMode.
  - **Consumed by:** RealRepository, RealSearchRepository, BasicSearchFilter.
  - **Related files:** SongRepository.kt, SearchRepository.kt.
  - **Notes:** genreId -1 represents songs without genre.


- `data/local/repository/LyricsRepository.kt` — Lyrics parsing (file/embedded/stored/remote), encoding detection, LruCache, saving.
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


- `data/local/repository/PlaylistRepository.kt` — Device and Room playlists (create, rename, delete, favorites, songs).
  - **Purpose:** Device and Room playlist management.
  - **Responsibilities:** List device playlists/songs; Room CRUD; favorites playlist logic; bulk delete chunks.
  - **Inputs:** Playlist ids, PlaylistEntity, Song lists.
  - **Outputs:** Playlists, PlaylistWithSongs, favorite state.
  - **Dependencies:** PlaylistDao, RealSongRepository, SortedCursorUtil, SongMapper.
  - **Consumed by:** RealRepository, LibraryViewModel.
  - **Related files:** PlaylistDao.kt, SongEntity.kt, SongMapper.kt.
  - **Notes:** Favorites is a Room playlist named via R.string.favorites_label.


- `data/local/repository/SearchRepository.kt` — Global search with grouped filters and scoped searches (genre/playlist/year/folder).
  - **Purpose:** Global and scoped search orchestration.
  - **Responsibilities:** Run filters or grouped mode searches; scoped genre/playlist/year/folder song searches.
  - **Inputs:** Context, SearchQuery, SearchFilter.
  - **Outputs:** Titled result lists (headers plus items).
  - **Dependencies:** RealAlbumRepository, RealSongRepository, RealArtistRepository, RealPlaylistRepository, GenreRepository, SpecialRepository.
  - **Consumed by:** RealRepository, SearchViewModel, BasicSearchFilter.
  - **Related files:** SearchQuery.kt, SearchFilter.kt, BasicSearchFilter.kt.
  - **Notes:** addTitled inserts localized headers between groups.


- `data/local/repository/SmartRepository.kt` — Smart content (top albums/artists, recent, never played), play counts and history.
  - **Purpose:** Computed "smart" content and playback stats.
  - **Responsibilities:** Top/recent albums-artists; recent songs by cutoff; not-recently-played; play counts and history.
  - **Inputs:** ContentType, queries, cutoffs from Preferences.
  - **Outputs:** Song/Album/Artist lists, Flows, counts.
  - **Dependencies:** RealSongRepository, RealAlbumRepository, RealArtistRepository, HistoryDao, PlayCountDao, MediaQueryDispatcher.
  - **Consumed by:** RealRepository, LastAddedSearchFilter, HomeFragment.
  - **Related files:** HistoryDao.kt, PlayCountDao.kt, Repository.kt.
  - **Notes:** Prunes entities whose files were deleted.


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
  - **Inputs:** user, repo (default mardous/BoomingMusic), authToken, allowExperimental.
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

- `playback/PlaybackService.kt` — MediaLibraryService owning the ExoPlayer, MediaSession, equalizer, notifications, Bluetooth and widgets (playback core).
  - **Purpose:** Central service hosting ExoPlayer, MediaLibrarySession, notifications and widget state.
  - **Responsibilities:** ExoPlayer/MediaSession setup, library browsing callbacks, custom commands, scrobbling, play counts, widget updates, receivers.
  - **Inputs:** MediaSession commands, SharedPreferences, MediaStoreObserver, AudioOutputObserver, Bluetooth/headset broadcasts.
  - **Outputs:** PlaybackState for Glance widgets, notifications, custom commands/events, persisted state, scrobbles.
  - **Dependencies:** AdvancedForwardingPlayer, EqualizerManager, SleepTimer, AudioOutputObserver, Repository, LibraryProvider, SsilencceRenderersFactory, processors.
  - **Consumed by:** MediaControllerOwner, WidgetExtension, MainActivity (custom events), AndroidManifest service.
  - **Related files:** Playback.kt, MediaIDs.kt, PersistentStorage.kt, MainModule.kt.
  - **Notes:** Player runs on dedicated HandlerThread "Ssilencce-ExoPlayer"; foreground service timeout set.


- `playback/AdvancedForwardingPlayer.kt` — ForwardingPlayer wrapper adding sequential timeline handling and upcoming-range realignment.
  - **Purpose:** Wrap ExoPlayer with upcoming-range queue semantics.
  - **Responsibilities:** Track listeners, mark upcoming MediaItems via extras, realign/clear ranges on discontinuities and moves.
  - **Inputs:** Wrapped Player, addMediaItem(s)/moveMediaItem calls, sequentialTimelineEnabled flag.
  - **Outputs:** MediaItems tagged `__is_upcoming__`/`__normal_index__`; adjusted queue mutations.
  - **Dependencies:** ForwardingPlayer, ExoPlayer, MediaItem, KoinComponent.
  - **Consumed by:** PlaybackService, PersistentStorage.
  - **Related files:** PlaybackExtension.kt, PlaybackService.kt.
  - **Notes:** QUEUE_NEXT_MODE preference drives sequential timeline; internal listener on position discontinuity.


- `playback/ImprovedShuffleOrder.kt` — Custom Media3 ShuffleOrder with random seed, atomic index and kotlinx-serialization persistence.
  - **Purpose:** Deterministic shuffle order with JSON persistence.
  - **Responsibilities:** Shuffled IntArray + inverse index, cloneAndInsert/Remove/Move/Clear, playerIndex AtomicInt, SerializedOrder.
  - **Inputs:** firstIndex, length, randomSeed; ShuffleOrder mutation operations.
  - **Outputs:** ShuffleOrder instances; serialized JSON strings.
  - **Dependencies:** ShuffleOrder, Random, AtomicInt, kotlinx-serialization Json.
  - **Consumed by:** PlaybackService, PersistentStorage.
  - **Related files:** PlaybackService.kt, PersistentStorage.kt.
  - **Notes:** @UnstableApi; persisted via "shuffle_order" SharedPreferences key.


- `playback/PersistentStorage.kt` — Persists queue, position and shuffle order in Room/SharedPreferences and restores asynchronously.
  - **Purpose:** Restore and save playback state across sessions.
  - **Responsibilities:** restoreState via QueueDao/Repository, debounced saveState, waitForRestoration/waitForMediaItems, RestorationState machine.
  - **Inputs:** AdvancedForwardingPlayer state, QueueDao, Repository, "playback_state" SharedPreferences.
  - **Outputs:** MediaItemsWithStartPosition + ShuffleOrder; QueueEntity rows and prefs.
  - **Dependencies:** QueueDao, Repository, AdvancedForwardingPlayer, SerializedOrder, CoroutineScope.
  - **Consumed by:** PlaybackService, playback resumption flow.
  - **Related files:** ImprovedShuffleOrder.kt, QueueEntity.kt, Repository.kt.
  - **Notes:** 500 ms debounce; skips saves while Restoring; validates saved shuffle order length.


- `playback/Playback.kt` — Constants for custom MediaSession commands/events (shuffle toggle, repeat cycle, favorite, playback restore).
  - **Purpose:** Single source of custom command/event names.
  - **Responsibilities:** Defines six commands and four event constants.
  - **Inputs:** —.
  - **Outputs:** Identifiers used in SessionCommand and broadcastCustomCommand.
  - **Dependencies:** None.
  - **Consumed by:** PlaybackService, PlayerViewModel, MainActivity.
  - **Related files:** PlaybackService.kt, PlayerViewModel.kt, MainActivity.kt.
  - **Notes:** Includes SET_UNSHUFFLED_ORDER and SET_STOP_POSITION commands.


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


- `playback/SleepTimer.kt` — AlarmManager-based sleep timer with countdown, fade-out and pending-quit option, exposing StateFlows.
  - **Purpose:** Schedule playback stop after configurable time.
  - **Responsibilities:** Exact alarm set/cancel, OnAlarmListener dispatch, pendingQuit consumption, CountDownTimer updater, StateFlows.
  - **Inputs:** millisInFuture, allowPendingQuit, fadeOut, fadeDuration; finish listeners.
  - **Outputs:** SleepParams callbacks, SleepTimerWaitingFor states.
  - **Dependencies:** AlarmManager, CountDownTimer, StateFlow, SleepTimerWaitingFor.
  - **Consumed by:** PlaybackService, SleepTimerViewModel, MainModule.
  - **Related files:** SleepTimerScreen.kt, SleepTimerViewModel.kt.
  - **Notes:** ELAPSED_REALTIME_WAKEUP; Android 12+ canScheduleExactAlarms permission.


- `playback/equalizer/EqualizerManager.kt` — Central manager applying EQ/balance/replay-gain/bass effects per output device, persisted in DataStore.
  - **Purpose:** Orchestrate all audio-effect state and engine application.
  - **Responsibilities:** eqDataStore persistence, state flows, engine lifecycle, profile CRUD/import, AutoEq, device-based profile selection.
  - **Inputs:** AudioOutputObserver, audioSessionId, DataStore prefs, EqProfile/AutoEqProfile.
  - **Outputs:** Engine/processor config, StateFlows for UI, AudioEffect session broadcasts.
  - **Dependencies:** EQEngine, BasicEQEngine, DynamicsProcessingEngine, processors, AudioOutputObserver, AudioEffect.
  - **Consumed by:** PlaybackService, EqualizerViewModel.
  - **Related files:** EQEngine.kt, EqualizerFragment.kt, MainModule.kt.
  - **Notes:** DataStore "equalizer"; EQ disabled under bit-perfect/audio offload; pro mode gates compressor/limiter.


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


- `playback/library/LibraryProvider.kt` — Resolves MediaItems for playback (including AAOS paths and search results) via the repository.
  - **Purpose:** Bridge repository data into MediaLibrary browsing/playback.
  - **Responsibilities:** getMediaItemsForPlayback, getMediaItemsForAAOSPlayback, getChildren/getItem/search.
  - **Inputs:** MediaItem lists, mediaId paths, search query, Repository.
  - **Outputs:** Resolved/playable MediaItems, browsable children, searchResult.
  - **Dependencies:** Repository, MediaIDs, CoverProvider URIs, Preferences.
  - **Consumed by:** PlaybackService.
  - **Related files:** MediaIDs.kt, PlaybackService.kt.
  - **Notes:** AAOS paths handle Android Auto controllers; internal SEARCH prefix.


- `playback/library/MediaIDs.kt` — Constants/helpers for hierarchical media ID paths (ROOT, SONGS, ALBUMS...).
  - **Purpose:** Canonical media ID path scheme.
  - **Responsibilities:** Category constants; getPathId/splitPath/isPath helpers with ":" separator.
  - **Inputs:** parentId, mediaId (Long/String), path strings.
  - **Outputs:** Path IDs and split segments.
  - **Dependencies:** None.
  - **Consumed by:** LibraryProvider, PlaybackService, MainActivity.
  - **Related files:** LibraryProvider.kt.
  - **Notes:** RECENT_SONGS uses "HISTORY"; LAST_ADDED/FAVORITES for shortcuts.


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


- `playback/processor/ReplayGainAudioProcessor.kt` — Media3 BaseAudioProcessor applying ReplayGain (album/track mode, preamp, peak limiting) to 16/24-bit PCM.
  - **Purpose:** Normalize loudness via ReplayGain tags.
  - **Responsibilities:** Gain computation (album/track, preamp, peak limit), queueInput scaling 16/24-bit, passthrough at zero gain.
  - **Inputs:** ReplayGain (ReplayGainTagExtractor), mode, preAmpGain, PCM buffers.
  - **Outputs:** Scaled PCM ByteBuffer.
  - **Dependencies:** BaseAudioProcessor, ReplayGain/ReplayGainMode, ByteUtils.
  - **Consumed by:** SsilencceRenderersFactory, EqualizerManager, PlaybackService.
  - **Related files:** SsilencceRenderersFactory.kt, ByteUtils.kt, ReplayGainTagExtractor.kt.
  - **Notes:** currentGain set on media item transition; dB→linear via 10^(gain/20).


- `playback/renderer/AlacWorkaroundCodecSelector.kt` — MediaCodecSelector filtering hardware ALAC decoders to avoid broken high-bitrate M4A playback.
  - **Purpose:** Force stable software ALAC decoding.
  - **Responsibilities:** Delegates to DEFAULT selector; filters hardwareAccelerated infos for AUDIO_ALAC.
  - **Inputs:** mimeType, requiresSecureDecoder, requiresTunnelingDecoder.
  - **Outputs:** List<MediaCodecInfo> (software ALAC only).
  - **Dependencies:** MediaCodecSelector.DEFAULT, MimeTypes, MediaCodecInfo.
  - **Consumed by:** PlaybackService (ExoPlayer).
  - **Related files:** SsilencceRenderersFactory.kt.
  - **Notes:** Falls back to extension decoders (FFmpeg) if none.


- `playback/renderer/SsilencceRenderersFactory.kt` — DefaultRenderersFactory subclass injecting balance and ReplayGain processors into DefaultAudioSink.
  - **Purpose:** Wire custom audio processors into playback.
  - **Responsibilities:** buildAudioSink with both processors, float-output/playback-params passthrough.
  - **Inputs:** Context, BalanceAudioProcessor, ReplayGainAudioProcessor.
  - **Outputs:** DefaultAudioSink with processor array.
  - **Dependencies:** DefaultRenderersFactory, DefaultAudioSink.
  - **Consumed by:** PlaybackService (ExoPlayer builder).
  - **Related files:** BalanceAudioProcessor.kt, ReplayGainAudioProcessor.kt.
  - **Notes:** Same MainModule-injected processor instances app-wide.


- `playback/shuffle/ShuffleManager.kt` — Implements smart/weighted shuffle modes (pure random, most played, favorites, forgotten, recent, combined, by-group).
  - **Purpose:** Generate smart shuffle orders from play stats.
  - **Responsibilities:** Weighted shuffle modes via ln weights, combined scoring, expandSongsBatch enrichment, shuffleByProvider group modes.
  - **Inputs:** List<Song>, SpecialShuffleMode, providers, GroupShuffleMode, SongSortMode, Repository stats.
  - **Outputs:** Shuffled List<Song>.
  - **Dependencies:** Repository, ExpandedSong, SpecialShuffleMode, GroupShuffleMode, SongSortMode, SongProvider.
  - **Consumed by:** PlayerViewModel.
  - **Related files:** PlayerViewModel.kt, core model shuffle modes.
  - **Notes:** Scores ln(r)/weight sorted descending; falls back to shuffled on failure.

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
  - **Notes:** 7-day cache expiry, 1024 px, genre covers auto-generated.


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

- `util/BackupHelper.kt` — Creates/restores ZIP backups with playlists (M3U), settings, lyrics and custom artist images.
  - **Purpose:** ZIP backup and restore of app data (playlists, settings, lyrics, artist images).
  - **Responsibilities:** Zip playlist M3Us, settings XML, lyrics JSON, custom artist images; restore each entry type; show success/failure toasts; clean cached playlist dir.
  - **Inputs:** Context, backup Uri, `BackupContent` selection, injected `Repository` and `LyricsDao`.
  - **Outputs:** `.bmgbak` ZIP via `ZipOutputStream`; restored playlists, prefs, lyrics, images; toast messages.
  - **Dependencies:** Repository, LyricsDao, M3UWriter, FileUtil, kotlinx Json, `zipOutputStream` extension, KoinComponent.
  - **Consumed by:** `PreferencesScreenFragment` (createBackup/restoreBackup, BACKUP_EXTENSION).
  - **Related files:** `util/m3u/M3UWriter.kt`, `util/FileUtil.kt`, `data/local/room/LyricsDao.kt`, `data/mapper/SongMapper.kt`.
  - **Notes:** Backup extension "bmgbak"; zip paths `Playlists/`, `prefs/`, `lyrics/`, `artistImages/`.


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


- `util/Preferences.kt` — Central typed SharedPreferences access for all app settings.
  - **Purpose:** Single typed access point for all app settings and preference keys.
  - **Responsibilities:** Typed getters/setters (theme, player, queue, shuffle, cutoffs, folder actions, updates); listener registration; enum helpers; ~100 key constants.
  - **Inputs:** Injected SharedPreferences, model enums (NowPlayingAction, SongClickBehavior...), `Cutoff`/`CategoryInfo`, string resources.
  - **Outputs:** Typed settings values; SharedPreferences writes via `edit`; constant keys.
  - **Dependencies:** SharedPreferences, AppCompatDelegate, CategoryInfo, Cutoff, FileUtil, Calendar/JSON helpers.
  - **Consumed by:** PlaybackService, MainActivity, AbsThemeActivity, AbsPlayerFragment, EqualizerViewModel, ViewExt, NavigationExt, nearly all UI.
  - **Related files:** `App.kt` (defaults), `extensions/SharedPreferencesExt.kt`, `extensions/utilities/Utilities.kt`, `extensions/utilities/CalendarExt.kt`.
  - **Notes:** `LASTFM_LOGIN`/`LISTENBRAINZ_LOGIN` keys; lists serialized via kotlinx JSON; enums by name or ordinal.


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
  - **Notes:** `order` array preserves the desired external ordering.


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


- `util/m3u/M3UWriter.kt` — Exports playlists as .m3u files (plain file pre-Q, MediaStore on Q+).
  - **Purpose:** Export Room playlists to `.m3u` files.
  - **Responsibilities:** Export single/multiple with toasts; MediaStoreWriter on Q+, `FileUtil.playlistsDirectory` pre-Q; `writeToDirectory`; `writeImpl` sorted by songPrimaryKey with EXTINF lines.
  - **Inputs:** Context, `PlaylistWithSongs` (SongEntity list).
  - **Outputs:** `.m3u` files in MediaStore or app dir; boolean success.
  - **Dependencies:** MediaStoreWriter, FileUtil, M3UConstants, `hasQ`, `toSongs` mapper, KoinComponent.
  - **Consumed by:** BackupHelper (writeToDirectory), MenuItemClickExt (export).
  - **Related files:** `util/m3u/M3UConstants.kt`, `util/BackupHelper.kt`, `data/local/MediaStoreWriter.kt`, `data/local/room/PlaylistWithSongs.kt`.
  - **Notes:** EXTINF line format "duration,artist - title"; MediaStore.Audio.Playlists content URI on Q+.


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


- `extensions/files/SAFExt.kt` — Storage Access Framework helpers: persisted URI permissions, document lookup by path, SAF deletion.
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
  - **Responsibilities:** Album/artist/song info strings; plural counts; Spanned titles with color spans; playlist info/durations; `replayGainStr`; `searchQuery` URLs; favorites check.
  - **Inputs:** Album/Artist/Song/PlaylistEntity, Context, WebSearchEngine.
  - **Outputs:** Formatted strings, CharSequences, query URLs.
  - **Dependencies:** `buildInfoString`, ReplayGainTagExtractor, text color spans, plurals.
  - **Consumed by:** SongAdapter, SimpleSongAdapter, AlbumAdapter, SearchAdapter, AlbumDetailFragment, InfoViewModel.
  - **Related files:** `extensions/media/ArtistNameExt.kt`, `extensions/utilities/Utilities.kt`, `extensions/resources/ColorExt.kt`.
  - **Notes:** Uses `DEFAULT_INFO_DELIMITER` " • ".


- `extensions/media/MediaExt.kt` — MediaStore helpers: insert/delete/album-art URIs, readable track/duration numbers, thumbnail files.
  - **Purpose:** MediaStore and formatting helpers.
  - **Responsibilities:** Album art thumb file; insertAlbumArt/deleteAlbumArt; iTunes track modulo; song/times plurals; album cover Uris; `asReadableDuration`; section names and article-stripping normalization.
  - **Inputs:** ContentResolver, albumId, Long durations, SortMode.
  - **Outputs:** Uris, Files, formatted strings, section letters.
  - **Dependencies:** MediaStore AlbumColumns/MediaColumns, FileUtil, SortMode, plurals.
  - **Consumed by:** MetadataWriter, AlbumDetailFragment, SongAdapter, TagEditor, LibraryViewModel.
  - **Related files:** `util/FileUtil.kt`, `data/local/MetadataWriter.kt`, `core/sort/SortMode.kt`.
  - **Notes:** Per-language article regexes (en/es/fr/de/it/pt/nl).


- `extensions/navigation/NavigationExt.kt` — Builds navigation argument bundles for detail screens and navigation actions.
  - **Purpose:** SafeArgs bundle builders and navigation helpers.
  - **Responsibilities:** playInfo/detail/songDetail/genreDetail/folderDetail/playlistDetail/albumDetail/artistDetail/search args; `findActivityNavController`; `isValidCategory`; `asFragmentExtras` shared elements.
  - **Inputs:** Album/Artist/Song/Folder/Genre/Playlist/ContentType/SearchFilter, views.
  - **Outputs:** Navigation argument Bundles, NavController, Extras.
  - **Dependencies:** Generated Args classes, NavController, CategoryInfo, Preferences.
  - **Consumed by:** MenuItemClickExt, DetailListFragment, HomeFragment, AlbumListFragment, ArtistDetailFragment, SearchFragment, AbsPlayerFragment, QueueFragment, CoverPagerFragment.
  - **Related files:** res/navigation graphs, `ui/screen/library` detail fragments.
  - **Notes:** Album-artists use id -1 plus name; `Preferences.onlyAlbumArtists` respected.


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


- `MainModule.kt` — Koin DI modules: network services, Room database, repositories, playback components, media writers, image managers, ViewModels.
  - **Purpose:** Defines the complete Koin dependency graph.
  - **Responsibilities:** `networkModule`, `mainModule`, `roomModule`, `dataModule`, `viewModule` definitions.
  - **Inputs:** Ktor client, OkHttp, Room DAOs, audio processors, repositories, ViewModels.
  - **Outputs:** `appModules` list with `single`/`factory`/`viewModel` providers.
  - **Dependencies:** Koin, Room (v6 migrations 1→6), Ktor, Media3 processors, Koin viewModel DSL.
  - **Consumed by:** `App.startKoin`.
  - **Related files:** `App.kt`, `SsilencceDatabase`, repository implementations.
  - **Notes:** `Real*` implementations bound to interfaces; `github`/`playstore` Last.fm access via injected services.


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
  - **Responsibilities:** View-type dispatch (HEADER/ALBUM/ARTIST/SONG/PLAYLIST/GENRE), Coil image loading, popup menus, song multi-select.
  - **Inputs:** List<Any> of mixed models, ISearchCallback, menu_media_selection.
  - **Outputs:** songClick/songMenuItemClick, album/artist/playlist clicks and menus, genreClick, onMultipleItemAction.
  - **Dependencies:** AbsMultiSelectAdapter, OnClickMenu, coil songImage/albumImage/artistImage/playlistImage, displayArtistName/albumInfo helpers.
  - **Consumed by:** SearchFragment.
  - **Related files:** `ui/screen/library/search/SearchFragment.kt`, `ui/LibraryCallbacks.kt`.
  - **Notes:** Only Song rows participate in quick-select; long-press on other rows opens popup menu.


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
  - **Responsibilities:** Bind Suggestion type title, create inner horizontal adapter via callback, open-suggestion button.
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


- `ui/adapters/song/SongAdapter.kt` — Open base adapter for songs with multi-select, palette image, selection menu and section popup text.
  - **Purpose:** Reusable base song adapter for all song lists.
  - **Responsibilities:** Palette cover loading, sort-mode-aware titles/texts, FastScroller popup, click plays via PlayerViewModel.openSongs, popup menu, multi-select.
  - **Inputs:** List<Song>, SongSortMode, ISongCallback, item layout, menu_media_selection.
  - **Outputs:** songsMenuItemClick; openSongs honoring SongClickBehavior/playOptionClickBehavior.
  - **Dependencies:** AbsMultiSelectAdapter, MediaEntryViewHolder, OnClickMenu, PlayerViewModel, Preferences.
  - **Consumed by:** SongListFragment, GenreDetailFragment, YearDetailFragment, FolderDetailFragment, DetailListFragment, HomeFragment.
  - **Related files:** `ui/adapters/song/SimpleSongAdapter.kt`, `ui/adapters/song/PlaylistSongAdapter.kt`, `ui/screen/player/PlayerViewModel.kt`.
  - **Notes:** Open ViewHolder/songMenuRes/onPrepareSongMenu for subclass customization; hides play button.


- `ui/adapters/song/SimpleSongAdapter.kt` — SongAdapter subclass showing track number, duration and info text per sort key.
  - **Purpose:** Compact song rows with track number, duration, and sort-key text.
  - **Responsibilities:** Bind imageText track number, time duration, getSongText per SortKey (Album/Track/Year).
  - **Inputs:** FragmentActivity, songs, layoutRes, SongSortMode, ISongCallback.
  - **Outputs:** Rendered rows; inherits SongAdapter click/menu behavior.
  - **Dependencies:** SongAdapter, asReadableTrackNumber/asReadableDuration, buildInfoString.
  - **Consumed by:** AlbumDetailFragment, ArtistDetailFragment.
  - **Related files:** `ui/adapters/song/SongAdapter.kt`, `ui/screen/library/albums/AlbumDetailFragment.kt`, `ui/screen/library/artists/ArtistDetailFragment.kt`.
  - **Notes:** "-" placeholder when track number unknown.


- `ui/adapters/song/PlaylistSongAdapter.kt` — SongAdapter subclass with drag & drop (advrecyclerview) for playlist reordering.
  - **Purpose:** Song rows inside playlist detail with drag reordering and persistence.
  - **Responsibilities:** DraggableItemAdapter contract (start-drag on dragView, move, drop), lock-drag toggle, saveSongs via LibraryViewModel.insertSongs.
  - **Inputs:** FragmentActivity, songs, layout, isLockDrag, ISongCallback, PlaylistEntity.
  - **Outputs:** Reordered dataSet; Room playlist song persistence; playlist-specific menu.
  - **Dependencies:** advrecyclerview DraggableItemAdapter, SongAdapter, LibraryViewModel, DraggableItemState.
  - **Consumed by:** PlaylistDetailFragment (wrapped by DragDropManager).
  - **Related files:** `ui/screen/library/playlists/PlaylistDetailFragment.kt`, `ui/adapters/song/SongAdapter.kt`.
  - **Notes:** Uses menu_playlist_songs_selection and menu_item_playlist_song menus.


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


- `ui/adapters/artist/ArtistAdapter.kt` — Multi-select artist adapter with palette image and album-artists-only support.
  - **Purpose:** Bind Artist rows in artist lists and similar-artist rows.
  - **Responsibilities:** Palette cover, transition names (album-artist aware), popup menu, play button opening sorted artist queue, FastScroller popup, multi-select.
  - **Inputs:** List<Artist>, ArtistSortMode, IArtistCallback.
  - **Outputs:** artistClick/artistMenuItemClick/artistsMenuItemClick; openQueue of sorted songs.
  - **Dependencies:** AbsMultiSelectAdapter, MediaEntryViewHolder, OnClickMenu, PlayerViewModel, Preferences.onlyAlbumArtists.
  - **Consumed by:** ArtistListFragment, ArtistDetailFragment (similar), DetailListFragment, HomeFragment.
  - **Related files:** `ui/screen/library/artists/ArtistListFragment.kt`, `ui/adapters/album/AlbumAdapter.kt`.
  - **Notes:** Transition name is artist name when album-artists-only mode is active.


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
  - **Responsibilities:** Activity-scoped PlayerViewModel/LibraryViewModel, MenuProvider registration, MediaEvent collection (favorite/content changed, playback restored/started), window insets and bottom margins.
  - **Inputs:** MediaEvent flow, preferences (PLAY_ON_STARTUP_MODE, OPEN_ON_PLAY).
  - **Outputs:** onFavoriteContentChanged/onMediaContentChanged hooks; panel expansion.
  - **Dependencies:** Fragment, MenuProvider, Koin activityViewModel, applyWindowInsets, MainActivity.
  - **Consumed by:** AbsRecyclerViewFragment, lyrics/settings/equalizer fragments.
  - **Related files:** `ui/screen/MainActivity.kt`, `ui/screen/library/LibraryViewModel.kt`, `ui/screen/player/PlayerViewModel.kt`.
  - **Notes:** expandPanel triggered when playback starts or restores per preferences.


- `ui/component/base/AbsRecyclerViewFragment.kt` — Base fragment with RecyclerView, toolbar, shuffle button, fast-scroller, mini-player margins.
  - **Purpose:** Standard library list scaffolding with toolbar, shuffle FAB, empty view.
  - **Responsibilities:** Create adapter/layout manager, FastScroller, shuffle click/long-press (ShuffleModeFragment), mini-player/FAB margins, library menu, scroll-to-top.
  - **Inputs:** Abstract createAdapter/createLayoutManager, isShuffleVisible, titleRes.
  - **Outputs:** Shuffle/scan/equalizer/import actions; empty-state visibility.
  - **Dependencies:** AbsMainActivityFragment, FragmentMainRecyclerBinding, FastScroller, ImportPlaylistDialog, ShuffleModeFragment, NavController.
  - **Consumed by:** AbsRecyclerViewCustomGridSizeFragment and all library list fragments.
  - **Related files:** `ui/component/base/AbsRecyclerViewCustomGridSizeFragment.kt`, `ui/component/views/TopAppBarLayout.kt`.
  - **Notes:** Finishes CAB action mode on pause; dismisses shuffle dialog on PlaybackStarted.


- `ui/component/base/AbsRecyclerViewCustomGridSizeFragment.kt` — Extends AbsRecyclerViewFragment with list/grid modes and configurable grid size/view type.
  - **Purpose:** Add list/grid switching, grid-size and view-type menus to library lists.
  - **Responsibilities:** Persist grid size/view type, rebuild adapter/layout on change, item layout per GridViewType, menu setup with max columns.
  - **Inputs:** Preferences-backed getSaved*/save* overrides, maxGridSize per orientation.
  - **Outputs:** viewType/gridSize changes triggering invalidateAdapter/invalidateLayoutManager.
  - **Dependencies:** AbsRecyclerViewFragment, GridViewType, R.integer column resources.
  - **Consumed by:** SongListFragment, AlbumListFragment, ArtistListFragment, GenresListFragment, PlaylistListFragment, YearsListFragment, FoldersListFragment.
  - **Related files:** `ui/component/base/AbsRecyclerViewFragment.kt`, `core/model/GridViewType.kt`.
  - **Notes:** Grid mode when gridSize exceeds maxGridSizeForList; recycler padding per layout.


- `ui/component/base/AbsPlayerFragment.kt` — Player screen base fragment (toolbar, song menu, animated background, gestures, player colors, dialogs).
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
  - **Notes:** Requires parent AbsPlayerFragment; prefers remaining time on total-time click.


- `ui/component/base/AbsSlidingMusicPanelActivity.kt` — Base activity with sliding mini-player bottom sheet, bottom nav/rail, player styles, gestures, search.
  - **Purpose:** Main app shell combining library navigation, mini-player sheet and player container.
  - **Responsibilities:** BottomSheetBehavior state management, player style selection, bottom nav/rail tab updates, hold-tab search, back press routing, queue-driven sheet hiding, preference reactions.
  - **Inputs:** MediaControllerOwner connection, PlayerViewModel queue/currentSong/colorScheme flows, Preferences.
  - **Outputs:** Panel expand/collapse, library margins, tab menu rebuild, player fragment replacement.
  - **Dependencies:** AbsBaseActivity, MediaControllerOwner, MiniPlayerFragment, all player style fragments, BottomSheetBehavior, NavigationBarView.
  - **Consumed by:** MainActivity.
  - **Related files:** `ui/screen/MainActivity.kt`, `ui/screen/other/MiniPlayerFragment.kt`, `ui/component/base/AbsBaseActivity.kt`.
  - **Notes:** STATE_HIDDEN clears the queue (RemoveAllSongs); peek height accounts for bottom insets.


- `ui/component/base/AbsTagEditorActivity.kt` — Tag editor base activity: image selection (Photo Picker), SAF writing, Coil loading, tag saving.
  - **Purpose:** Shared scaffolding for song/album/artist tag editors.
  - **Responsibilities:** Photo Picker + write-request launchers, image dialog (download/pick/web-search/restore/remove), Coil loading to bitmap, save with MediaStore write request, genre selector.
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
  - **Responsibilities:** Find image/imageContainer/menu/play/imageText/title/text/time/dragView/paletteColorContainer, click/long-click listeners, Koin getViewModel helper.
  - **Inputs:** Inflated item View.
  - **Outputs:** Click events to subclasses; ViewModel access.
  - **Dependencies:** RecyclerView.ViewHolder, Koin getViewModel, MaterialButton.
  - **Consumed by:** SongAdapter, AlbumAdapter, ArtistAdapter, GenreAdapter, YearAdapter, PlaylistAdapter, FileAdapter.
  - **Related files:** `ui/adapters/*`, `extensions/AdapterExt.kt`.
  - **Notes:** Optional views may be null depending on layout; onLongClick defaults false.


- `ui/component/views/TopAppBarLayout.kt` — Custom AppBarLayout toggling between simple or collapsing mode per preference.
  - **Purpose:** Provide toolbar/app-bar variant chosen by app-bar preference.
  - **Responsibilities:** Inflate Simple or Collapsing appbar bindings, expose toolbar/title, pinWhenScrolled.
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


- `ui/component/menu/MenuItemClickExt.kt` — Extensions resolving menu item actions (play next, add to queue/playlist, share, delete, edit tags, navigate).
  - **Purpose:** Central song/album/artist/playlist menu action handling.
  - **Responsibilities:** onSongMenu/onSongsMenu/onAlbumMenu/onAlbumsMenu/onArtistMenu/onArtistsMenu/onPlaylistMenu/onPlaylistsMenu routing to PlayerViewModel, dialogs, tag editors, navigation, M3U export.
  - **Inputs:** Fragment, MenuItem, entity models.
  - **Outputs:** Queue/play commands, toast feedback, dialog/navigation side effects.
  - **Dependencies:** PlayerViewModel, LibraryViewModel, AddToPlaylistDialog, DeleteSongsDialog, DeletePlaylistDialog, EditPlaylistDialog, M3UWriter, NavigationExt args.
  - **Consumed by:** AbsPlayerFragment, AbsRecyclerViewFragment menus, library fragments, SearchFragment.
  - **Related files:** `ui/component/menu/MenuClick.kt`, `extensions/navigation/NavigationExt.kt`, `util/m3u/M3UWriter.kt`.
  - **Notes:** Playlist actions guard PlaylistWithSongs.Empty; list actions flatten songs on IO.


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
  - **Responsibilities:** Canvas draws rounded bars; per-bar sine phase; activity animates to 1f when playing, 0f paused.
  - **Inputs:** color, isPlaying, barCount, barWidth, gap, min/maxHeightFraction, basePeriodMillis.
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
  - **Notes:** Content insets exclude top, add miniPlayerMargin bottom.


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


- `ui/component/compose/lyrics/LyricsView.kt` — Full Compose lyrics view: LazyColumn with synced scrolling, active line highlight, blur/shadow effects, RTL.
  - **Purpose:** Render synced lyrics with playback-driven effects.
  - **Responsibilities:** Auto-scroll/center active line, line scale/alpha, blur & shadow, karaoke/syllable selection, translations/transliterations, actor alignment, provider footer, BubblesLine.
  - **Inputs:** LyricsViewState, LyricsViewSettings, contentPadding, fadingEdges, contentColor, isPowerSaveMode, hasBackgroundEffects, onSeekTo.
  - **Outputs:** Composable LazyColumn.
  - **Dependencies:** LyricsViewState/Settings, SyncedLyrics, KaraokeLineView, SpannedWordText, fadingEdges, BlurEffect.
  - **Consumed by:** LyricsScreen.
  - **Related files:** KaraokeStyleText.kt, SpannedWordText.kt, FadingEdge.kt.
  - **Notes:** BubblesLine from Lotus player; power-save disables advanced effects.


- `ui/component/compose/lyrics/KaraokeStyleText.kt` — Karaoke-style line rendering measuring syllables and animating text fill by playback time.
  - **Purpose:** Per-syllable karaoke fill rendering.
  - **Responsibilities:** TextMeasurer syllable measurement, greedy word wrapping, static line layout, Canvas drawText with gradient brush fill via saveLayer/DstIn.
  - **Inputs:** selectedLine, shadowEffect, currentMillis, syllables, contentColor, style, align.
  - **Outputs:** Composable Canvas.
  - **Dependencies:** rememberTextMeasurer, BoxWithConstraints, Paint, Brush.horizontalGradient.
  - **Consumed by:** LyricsView (WordSyncedText).
  - **Related files:** LyricsView.kt, SpannedWordText.kt.
  - **Notes:** RTL-aware fill direction; shadow on active line.


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
  - **Responsibilities:** Observe getDevicePlaylists, RecyclerView adapter, per-playlist song multi-choice, viewModel.importPlaylist, progress indicator.
  - **Inputs:** —.
  - **Outputs:** Import result toasts.
  - **Dependencies:** LibraryViewModel (getDevicePlaylists, importPlaylist), ImportablePlaylistResult, DialogProgressRecyclerViewBinding.
  - **Consumed by:** Playlist screens (import action).
  - **Related files:** LibraryViewModel.kt, ImportablePlaylistResult.
  - **Notes:** Dismisses with toast if no importable playlists.


- `ui/dialogs/playlists/EditPlaylistDialog.kt` — Edits an existing playlist (name, description, image with Photo Picker).
  - **Purpose:** Edit playlist metadata.
  - **Responsibilities:** Pre-fill name/description/custom cover; PickVisualMedia image selection with Coil load; save via updatePlaylist + forceReload.
  - **Inputs:** PlaylistEntity via EXTRA_PLAYLIST.
  - **Outputs:** LibraryViewModel.updatePlaylist(ReloadType.Playlists).
  - **Dependencies:** ActivityResultContracts.PickVisualMedia, coil3.load, DialogCreatePlaylistBinding, LibraryViewModel.
  - **Consumed by:** MenuItemClickExt.
  - **Related files:** CreatePlaylistDialog.kt, MenuItemClickExt.kt.
  - **Notes:** Favorites playlist name field disabled; validates non-empty name.


- `ui/dialogs/playlists/DeletePlaylistDialog.kt` — Confirmation dialog to delete one or more playlists.
  - **Purpose:** Confirm playlist deletion.
  - **Responsibilities:** Singular/plural title and message; calls LibraryViewModel.deletePlaylists.
  - **Inputs:** List<PlaylistEntity> via EXTRA_PLAYLISTS.
  - **Outputs:** ViewModel deletion.
  - **Dependencies:** MaterialAlertDialogBuilder, LibraryViewModel, toHtml.
  - **Consumed by:** MenuItemClickExt.
  - **Related files:** MenuItemClickExt.kt, PlaylistDetailFragment.kt.
  - **Notes:** create() accepts PlaylistWithSongs and maps entities.


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
  - **Responsibilities:** Build colorScheme from dark/black/dynamic preferences; dynamicDark/LightColorScheme on S+; black overrides; PlayerTheme copies player colors.
  - **Inputs:** darkTheme, blackTheme, dynamicColor, playerColorScheme, content.
  - **Outputs:** MaterialTheme wrappers.
  - **Dependencies:** Preferences, PlayerColorScheme, dynamicColorScheme APIs, customTypography.
  - **Consumed by:** App entry screens, AddToPlaylistDialog, LyricsScreen (PlayerTheme).
  - **Related files:** Color.kt, Type.kt, Monet.kt.
  - **Notes:** Black theme uses surfaceContainerBlack resources.


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

- `ui/screen/MainActivity.kt` — Main activity extending AbsSlidingMusicPanelActivity; NavHost, playback intents, MediaController commands, dynamic shortcuts, update search.
  - **Purpose:** App entry activity hosting navigation, playback intents, MediaController custom events.
  - **Responsibilities:** NavHost graph setup, last-page start destination, custom command handling, playback shortcuts, bottom-nav visibility, update search, scan dialog.
  - **Inputs:** Intents/actions (shortcuts, media URIs), MediaController custom commands, Preferences.
  - **Outputs:** PlayerViewModel commands (playMediaId, openQueue, restorePlayback), UpdateDialog, toasts.
  - **Dependencies:** AbsSlidingMusicPanelActivity, PlayerViewModel, LibraryViewModel, UpdateViewModel, MediaController.Listener, Playback, MediaIDs.
  - **Consumed by:** Launcher activity (AndroidManifest); AbsRecyclerViewFragment calls scanAllPaths().
  - **Related files:** navigation/graph_main.xml, UpdateDialog, IScrollHelper.
  - **Notes:** Custom commands relay MediaEvent to PlayerViewModel; implements MediaController.Listener.


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
  - **Responsibilities:** Collect EqualizerViewModel flows, band waveform, sliders, dialogs (save/edit/delete/import/export/share), system EQ launcher, share via ShareCompat, snackbars.
  - **Inputs:** LibraryViewModel, EqualizerViewModel, onBackClick; file picker Uris.
  - **Outputs:** UI state changes; profile CRUD and export events.
  - **Dependencies:** EqualizerViewModel, EqualizerWaveform, TopAppBarMenu, ButtonGroup, MediaStoreWriter via VM, EqEngineMode.
  - **Consumed by:** EqualizerFragment.
  - **Related files:** EqualizerFragment.kt, EqualizerViewModel.kt, EqualizerResult.kt, ui/component/compose/EqualizerWaveform.kt.
  - **Notes:** Band count selector; AutoEq TXT import; pro-mode gates compressor/limiter.


- `ui/screen/equalizer/EqualizerViewModel.kt` — Exposes EqualizerManager state, implements profile import/export (JSON/TXT AutoEq), launches system AudioEffect.
  - **Purpose:** Bridge EqualizerManager and AudioOutputObserver state to UI.
  - **Responsibilities:** Expose eqState/bandCapabilities/profiles flows, profile save/edit/delete/import/export, AutoEqTxtParser import, system equalizer intent, MediaStoreWriter export.
  - **Inputs:** ContentResolver, EqualizerManager, AudioOutputObserver, MediaStoreWriter.
  - **Outputs:** State flows and Channel events (export/import/save/delete).
  - **Dependencies:** EqualizerManager, AudioOutputObserver, AutoEqTxtParser, MediaStoreWriter, kotlinx.serialization Json, AudioEffect.
  - **Consumed by:** EqualizerScreen, SoundSettingsFragment, SoundSettingsScreen.
  - **Related files:** EqualizerScreen.kt, EqualizerResult.kt, playback/equalizer/EqualizerManager.kt, core/audio/AudioOutputObserver.kt.
  - **Notes:** Share writes JSON to externalCacheDir; volume/tempo/balance/replayGain also exposed.


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


- `ui/screen/library/LibraryResult.kt` — Library operation results (HandleIntentResult, AddToPlaylistResult, ImportResult with messages).
  - **Purpose:** Data classes modeling outcomes of library operations.
  - **Responsibilities:** HandleIntentResult (handled, songs, position, failed), AddToPlaylistResult progress flags, ImportResult localized success/error messages.
  - **Inputs:** Song lists, playlist names, Context for string resources.
  - **Outputs:** LiveData payloads consumed by UI and dialogs.
  - **Dependencies:** Song model, string resources.
  - **Consumed by:** LibraryViewModel, CreatePlaylistDialog, ImportPlaylistDialog, MainActivity.
  - **Related files:** ui/dialogs/playlists/CreatePlaylistDialog, ImportPlaylistDialog.
  - **Notes:** ImportResult.success/error build localized messages via Context.


- `ui/screen/library/LibraryViewModel.kt` — Central library ViewModel: LiveData of all collections, forced reloads, intents, shuffle, playlist import, loading animations.
  - **Purpose:** Central ViewModel feeding all library screens with collections and operations.
  - **Responsibilities:** LiveData for songs/albums/artists/playlists/genres/years/filesystem/suggestions, forceReload(ReloadType), intent handling, playlist CRUD/import, blacklist, margin animations, scrobbling login.
  - **Inputs:** Repository, InclExclDao, CustomPlaylistImageManager, intents, Preferences, StorageUtil.
  - **Outputs:** Collection LiveData, HandleIntentResult/AddToPlaylistResult/ImportResult, FileSystemQuery, LibraryMargin.
  - **Dependencies:** Repository, InclExclDao, CustomPlaylistImageManager, Preferences, MediaScannerConnection.
  - **Consumed by:** Library fragments, MainActivity, AddToPlaylistDialog, CreatePlaylistDialog, ImportPlaylistDialog, ShuffleModeBottomSheet.
  - **Related files:** HomeResult (SuggestedResult), ReloadType enum, playlist dialogs.
  - **Notes:** Init initializes blacklist and deletes missing content; declares ReloadType enum.


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
  - **Responsibilities:** HomeAdapter over SuggestedResult, header title, shuffle/last-added/top-tracks/history clicks, suggestion adapters per ContentType, margins, settings-only menu.
  - **Inputs:** LibraryViewModel suggestions/miniPlayerMargin, PlayerViewModel.
  - **Outputs:** Navigation to detail lists/playlist/album/artist, openAndShuffleQueue, forceReload.
  - **Dependencies:** AbsMainActivityFragment, HomeAdapter, AlbumAdapter/ArtistAdapter/SongAdapter, NavigationExt args, onSongMenu extensions.
  - **Consumed by:** NavHost graph (nav_home) via AbsSlidingMusicPanelActivity.
  - **Related files:** HomeBinding, HomeResult, DetailListFragment, AlbumDetailFragment, ArtistDetailFragment.
  - **Notes:** Implements IHomeCallback/IScrollHelper; reloads suggestions on media/favorite changes.


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
  - **Responsibilities:** Observe getAlbums(), shuffle via PlayerViewModel.openShuffle, AlbumSortMode.AllAlbums menu, detail navigation, SharedPreferences persistence.
  - **Inputs:** LibraryViewModel albums, Preferences, AlbumAdapter.
  - **Outputs:** Navigation nav_album_detail, forceReload(ReloadType.Albums).
  - **Dependencies:** AbsRecyclerViewCustomGridSizeFragment, AlbumAdapter, AlbumSortMode, SongSortMode.Dynamic(SortKey.Track).
  - **Consumed by:** NavHost graph (nav_albums).
  - **Related files:** AlbumDetailFragment, LibraryViewModel.
  - **Notes:** Reloads on resume and onMediaContentChanged.


- `ui/screen/library/albums/AlbumDetailFragment.kt` — Album detail with cover header, song list, similar albums, Last.fm wiki and container transition.
  - **Purpose:** Album detail screen combining header, songs, similar albums, wiki.
  - **Responsibilities:** ConcatAdapter assembly, sort popups, play/shuffle, search/play-info navigation, MaterialContainerTransform, compact song view and duration toggles.
  - **Inputs:** AlbumDetailFragmentArgs (albumId), AlbumDetailViewModel, LastFmAlbum wiki.
  - **Outputs:** Navigation to artist/search/play-info, openQueue/openAndShuffleQueue.
  - **Dependencies:** HeaderAdapter, SimpleSongAdapter, AlbumAdapter, WikiAdapter, HorizontalListAdapter, Coil albumImage.
  - **Consumed by:** Navigation nav_album_detail.
  - **Related files:** AlbumDetailViewModel, AlbumListFragment, ArtistDetailFragment.
  - **Notes:** Navigates up on empty album; retries wiki in default language.


- `ui/screen/library/albums/AlbumDetailViewModel.kt` — Loads album by id, similar albums and Last.fm wiki.
  - **Purpose:** Per-album detail loader with albumId constructor parameter.
  - **Responsibilities:** loadAlbumDetail(), getAlbumDetail()/getAlbum(), getSimilarAlbums(), getAlbumWiki() gated by NetworkFeature.Lastfm.
  - **Inputs:** Repository, albumId, Application.
  - **Outputs:** LiveData<Album>, LiveData<List<Album>>, LiveData<LastFmAlbum>.
  - **Dependencies:** Repository, NetworkFeature, AndroidViewModel.
  - **Consumed by:** AlbumDetailFragment via parametersOf(arguments.albumId).
  - **Related files:** AlbumDetailFragment.
  - **Notes:** Wiki emission skipped when feature unavailable.


- `ui/screen/library/artists/ArtistListFragment.kt` — Artist list/grid fragment with shuffle, sorting and context menus.
  - **Purpose:** Browse artists grid with sorting and album-artist toggle.
  - **Responsibilities:** Observe getArtists(), shuffle via PlayerViewModel.openShuffle, onlyAlbumArtists preference, ArtistSortMode.AllArtists menu, detail navigation.
  - **Inputs:** LibraryViewModel artists, Preferences, ArtistAdapter.
  - **Outputs:** Navigation nav_artist_detail, forceReload(ReloadType.Artists).
  - **Dependencies:** AbsRecyclerViewCustomGridSizeFragment, ArtistAdapter, ArtistSortMode, SongSortMode.Dynamic(SortKey.Name).
  - **Consumed by:** NavHost graph (nav_artists).
  - **Related files:** ArtistDetailFragment, LibraryViewModel.
  - **Notes:** Default view type Circle; persists grid settings.


- `ui/screen/library/artists/ArtistDetailFragment.kt` — Artist detail: header image, albums/songs, similar artists and Last.fm biography.
  - **Purpose:** Artist detail combining albums, songs, similar artists, biography.
  - **Responsibilities:** ConcatAdapter with GridLayoutManager span lookup, sort popups, play/shuffle, preferences (horizontal albums, ignore singles, compact songs), container transition.
  - **Inputs:** ArtistDetailFragmentArgs (artistId, artistName), ArtistDetailViewModel, LastFmArtist bio.
  - **Outputs:** Navigation to album/artist/search/play-info, openQueue/openAndShuffleQueue.
  - **Dependencies:** HeaderAdapter, SimpleAlbumAdapter, SimpleSongAdapter, ArtistAdapter, WikiAdapter, Coil artistImage.
  - **Consumed by:** Navigation nav_artist_detail.
  - **Related files:** ArtistDetailViewModel, ArtistListFragment, AlbumDetailFragment.
  - **Notes:** Album artist resolved by name; retries biography in default language.


- `ui/screen/library/artists/ArtistDetailViewModel.kt` — Loads artist by id/name, similar artists and Last.fm biography.
  - **Purpose:** Per-artist detail loader keyed by id or album-artist name.
  - **Responsibilities:** loadArtistDetail() (artistById or albumArtistByName), getSimilarArtists() sorted by media name, getArtistBio() feature-gated.
  - **Inputs:** Repository, artistId, artistName?, Application.
  - **Outputs:** LiveData<Artist>, LiveData<List<Artist>>, LiveData<LastFmArtist?>.
  - **Dependencies:** Repository, NetworkFeature, sortedByMediaName extension.
  - **Consumed by:** ArtistDetailFragment via parametersOf(artistId, artistName).
  - **Related files:** ArtistDetailFragment.
  - **Notes:** Returns Artist.empty when both id and name invalid.


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
  - **Responsibilities:** SongAdapter (SongSortMode.FolderSongs), play/shuffle buttons, search navigation, sort menu reload, pop back on empty.
  - **Inputs:** FolderDetailFragmentArgs (extraFolderPath), FolderDetailViewModel, miniPlayerMargin.
  - **Outputs:** openQueue/openAndShuffleQueue, nav_search, loadDetail().
  - **Dependencies:** AbsMainActivityFragment, SongAdapter, SongSortMode.FolderSongs.
  - **Consumed by:** Navigation nav_folder_detail (from FoldersListFragment).
  - **Related files:** FolderDetailViewModel, FoldersListFragment.
  - **Notes:** Shares fragment_detail_list layout with genre/year details.


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


- `ui/screen/library/genres/GenreDetailFragment.kt` — Genre detail listing its songs with song menus and shuffle.
  - **Purpose:** List songs belonging to a genre.
  - **Responsibilities:** SongAdapter (SongSortMode.GenreSongs), play/shuffle, search navigation, sort menu, navigate up when empty.
  - **Inputs:** GenreDetailFragmentArgs (extraGenre), GenreDetailViewModel, miniPlayerMargin.
  - **Outputs:** openQueue/openAndShuffleQueue, nav_search, loadGenreSongs().
  - **Dependencies:** AbsMainActivityFragment, SongAdapter, Genre model, DefaultItemAnimator.
  - **Consumed by:** Navigation nav_genre_detail.
  - **Related files:** GenreDetailViewModel, GenresListFragment, SearchFragment.
  - **Notes:** Shares fragment_detail_list layout.


- `ui/screen/library/genres/GenreDetailViewModel.kt` — Loads the songs of a genre.
  - **Purpose:** Fetch songs for a genre.
  - **Responsibilities:** getSongs() LiveData, loadGenreSongs() via repository.songsByGenre(genre.id), init auto-load.
  - **Inputs:** Repository, Genre.
  - **Outputs:** LiveData<List<Song>>.
  - **Dependencies:** Repository.
  - **Consumed by:** GenreDetailFragment via parametersOf(arguments.extraGenre).
  - **Related files:** GenreDetailFragment.
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
  - **Responsibilities:** PlaylistSongAdapter with RecyclerViewDragDropManager, lock toggle, remove-from-playlist dialogs, play/shuffle, header info, exists check, fast scroller.
  - **Inputs:** PlaylistDetailFragmentArgs (playlistId), PlaylistDetailViewModel, Preferences, LibraryViewModel.
  - **Outputs:** openQueue/openAndShuffleQueue, nav_search, RemoveFromPlaylistDialog, saveSongs on pause.
  - **Dependencies:** AbsMainActivityFragment, PlaylistSongAdapter, advrecyclerview drag/drop, Coil playlistImage, MaterialContainerTransform.
  - **Consumed by:** Navigation nav_playlist_detail.
  - **Related files:** PlaylistDetailViewModel, PlaylistListFragment, RemoveFromPlaylistDialog.
  - **Notes:** Delete action hidden for favorites playlist.


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
  - **Responsibilities:** SongAdapter, play/shuffle buttons, search navigation, SongSortMode.YearSongs menu, navigate up when empty.
  - **Inputs:** YearDetailFragmentArgs (extraYear), YearDetailViewModel, miniPlayerMargin.
  - **Outputs:** openQueue/openAndShuffleQueue, nav_search, loadDetail().
  - **Dependencies:** AbsMainActivityFragment, SongAdapter, ReleaseYear model.
  - **Consumed by:** Navigation nav_year_detail.
  - **Related files:** YearDetailViewModel, YearsListFragment.
  - **Notes:** Reuses menu_genre_detail resource.


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
  - **Responsibilities:** ComposeView with LyricsScreen, navigate to LyricsEditorFragment, expand panel on exit.
  - **Inputs:** PlayerViewModel (activity-scoped).
  - **Outputs:** LyricsScreen rendering; navigation events.
  - **Dependencies:** PlayerViewModel, LyricsEditorFragmentArgs, MainActivity.expandPanel.
  - **Consumed by:** Navigation graph from player cover actions.
  - **Related files:** LyricsScreen.kt, LyricsEditorFragment.kt.
  - **Notes:** currentFragment check avoids panel expansion over editor.


- `ui/screen/lyrics/LyricsScreen.kt` — Compose synced-lyrics screen: auto-scrolling, blur/background effects, keepScreenOn, auto-advance with playback.
  - **Purpose:** Render synced/plain lyrics with playback-synced highlighting.
  - **Responsibilities:** LyricsUiState sealed class, smooth playback position interpolation, gradient/blur backgrounds, LyricsView rendering, seek callbacks.
  - **Inputs:** LibraryViewModel, LyricsViewModel, PlayerViewModel, Song.
  - **Outputs:** Lyrics UI states (Loading/Empty/Instrumental/Plain/Synced).
  - **Dependencies:** LyricsViewModel, PlayerViewModel, LyricsView, AnimatedEqBars, Coil SingletonImageLoader, extractGradientColors.
  - **Consumed by:** LyricsFragment, CoverLyricsFragment (CoverLyricsScreen).
  - **Related files:** LyricsViewModel.kt, LyricsFragment.kt, player/cover/CoverLyricsFragment.kt, ui/component/compose/lyrics/LyricsView.kt.
  - **Notes:** keepScreenOn and power-save mode respected; rememberSmoothPlaybackPosition public helper.


- `ui/screen/lyrics/LyricsViewModel.kt` — Central lyrics ViewModel: loading from repository/network, InstrumentalDetector, view settings, editor state.
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


- `ui/screen/lyrics/LyricsEditorScreen.kt` — Compose lyrics editor: text editing with preview, instrumental detection, online search, saving lyrics/LRC files.
  - **Purpose:** Edit lyrics across Embedded/Downloaded/File sources.
  - **Responsibilities:** TextFieldState per source, MediaStore write permission requests, download/search dialogs, save via viewModel, clipboard/select-all/undo, landscape layouts.
  - **Inputs:** Song, LyricsViewModel, onBackClick.
  - **Outputs:** LyricsEditorResult events; lyrics saved through LyricsRepository.
  - **Dependencies:** LyricsViewModel, LyricsSource, LyricsMode, NetworkFeature, MediaStore.createWriteRequest, TopAppBarMenu.
  - **Consumed by:** LyricsEditorFragment.
  - **Related files:** LyricsEditorFragment.kt, LyricsViewModel.kt, LyricsResult helpers.
  - **Notes:** File source is read-only; SnapshotMapSaver persists edits across config changes.


- `ui/screen/other/DetailListFragment.kt` — Generic content list fragment (albums/artists/songs per ContentType) for "last added" with menus.
  - **Purpose:** Reusable screen for home "see all" content lists.
  - **Responsibilities:** ContentType switch loading (top/recent artists/albums, top tracks, history, recent songs, favorites, not-recently-played), per-type adapters, subtitle info, clear history, search navigation.
  - **Inputs:** DetailListFragmentArgs (type), LibraryViewModel flows/LiveData, Preferences cutoffs.
  - **Outputs:** Navigation to album/artist details, nav_search, clearHistory(), openQueue/openAndShuffleQueue.
  - **Dependencies:** AbsMainActivityFragment, SongAdapter/ArtistAdapter/AlbumAdapter, ContentType, fast scroller.
  - **Consumed by:** Navigation nav_detail_list (from HomeFragment detailArgs).
  - **Related files:** HomeFragment, LibraryViewModel, ContentType.
  - **Notes:** Shuffle hidden for non-song content types.


- `ui/screen/other/MiniPlayerFragment.kt` — Bottom mini-player: cover, title, play/next/prev buttons (with long-press gestures), progress, panel expansion.
  - **Purpose:** Compact playback controls above the sliding panel.
  - **Responsibilities:** Observe PlayerViewModel flows (currentSong, progress/duration, isPlaying), button styling via NowPlayingButtonStyle, SkipButtonTouchHandler gestures, swipe-to-skip fling, wavy progress.
  - **Inputs:** PlayerViewModel (activityViewModel), Preferences, resources.
  - **Outputs:** seekForward/seekBack, seekToNext/seekToPrevious, togglePlayPause.
  - **Dependencies:** Fragment, SkipButtonTouchHandler, Coil songImage, GestureDetector, ProgressState.
  - **Consumed by:** sliding_music_panel_layout.xml, AbsSlidingMusicPanelActivity (panel expansion, preference callbacks).
  - **Related files:** PlayerViewModel, AbsSlidingMusicPanelActivity.
  - **Notes:** Setup methods invoked by activity preference listeners.


- `ui/screen/other/ShuffleModeBottomSheet.kt` — Compose bottom sheet choosing special shuffle modes over the full library.
  - **Purpose:** Compose UI listing SpecialShuffleMode options.
  - **Responsibilities:** Observe songs/shuffleOperationState, forceReload Songs when empty, equal-height ShuffleModeItem rows, busy/progress states, mode click triggers openSpecialShuffle.
  - **Inputs:** LibraryViewModel, PlayerViewModel, SpecialShuffleMode entries.
  - **Outputs:** playerViewModel.openSpecialShuffle(allSongs, mode).
  - **Dependencies:** Compose Material3, BottomSheetDialogSurface, SpecialShuffleMode, ShuffleOperationState.
  - **Consumed by:** ShuffleModeFragment.
  - **Related files:** ShuffleModeFragment, PlayerViewModel, LibraryViewModel.
  - **Notes:** Equalizes row heights via onGloballyPositioned; crossfade spinner while shuffling.


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


- `ui/screen/player/PlayerViewModel.kt` — Central player ViewModel: MediaController subscription, progress, events, shuffle/queue, session commands, palette colors.
  - **Purpose:** Bind MediaController playback state to UI flows and dispatch session commands.
  - **Responsibilities:** queue/position/currentSong flows, ProgressObserver polling, Player.Listener events, color scheme generation, queue mutations, shuffle openers.
  - **Inputs:** MediaController via setMediaController, SharedPreferences, Repository, MediaEvent.
  - **Outputs:** StateFlows (queueFlow, currentSongFlow, colorSchemeFlow), mediaEvent, stopAfterPosition.
  - **Dependencies:** MediaController, ProgressObserver, ShuffleManager, Repository, Preferences, Playback constants.
  - **Consumed by:** AbsPlayerFragment, AbsPlayerControlsFragment, AbsSlidingMusicPanelActivity, MainActivity.
  - **Related files:** PlaybackService, MediaEvent, QueuePosition, QueueSong.
  - **Notes:** Koin-injected; debounced queue rebuild (QUEUE_DEBOUNCE=100ms); removes missing MediaItems.


- `ui/screen/player/cover/CoverPagerFragment.kt` — ViewPager of covers (carousel effect) with player gestures and palette color capture.
  - **Purpose:** Host the swipable cover pager and propagate palette colors upward.
  - **Responsibilities:** AlbumCoverPagerAdapter, page transformers, PlayerGesturesController, queue/position sync, lyrics overlay toggle, keepScreenOn.
  - **Inputs:** PlayerViewModel flows, Preferences, PaletteColor via ColorReceiver.
  - **Outputs:** onColorChanged/onLyricsVisibilityChange callbacks, playSongAt on page select.
  - **Dependencies:** PlayerViewModel, ImageFragment, CarouselPagerTransformer, CustomFragmentStatePagerAdapter.
  - **Consumed by:** All style player layouts (fragment_default_player, fragment_m3_player, etc.).
  - **Related files:** CoverLyricsFragment, ImageFragment, AbsPlayerFragment, NowPlayingScreen.
  - **Notes:** Lyrics loading gated by NowPlayingScreen.supportsCoverLyrics.


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
  - **Notes:** Navigation icon opens sound settings; hides queue item when next song shown.


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

- `ui/screen/queue/QueueFragment.kt` — Expanded BottomSheetDialogFragment containing the queue in Compose with song actions.
  - **Purpose:** Show playback queue as expanded bottom sheet.
  - **Responsibilities:** Force expanded state, render QueueScreen, route dialogs (playlist/delete/share/details), auto-dismiss on empty queue.
  - **Inputs:** PlayerViewModel queueFlow.
  - **Outputs:** Queue UI; dialog/navigation intents.
  - **Dependencies:** PlayerViewModel, AddToPlaylistDialog, DeleteSongsDialog, getShareSongIntent, songDetailArgs.
  - **Consumed by:** Mini-player/player queue button.
  - **Related files:** QueueScreen.kt, other/MiniPlayerFragment.kt, MainActivity.kt.
  - **Notes:** launchAndRepeatWithViewLifecycle observes queue.


- `ui/screen/queue/QueueScreen.kt` — Compose queue UI: highlighted current song, search, drag reordering, filters, actions menu.
  - **Purpose:** Interactive queue list with playback control.
  - **Responsibilities:** Reorderable drag list, shuffle/repeat/lock toggles, up-next summary, locate-current button, per-item menu, stop-after events, auto-scroll to current.
  - **Inputs:** PlayerViewModel flows, SharedPreferences LOCKED_QUEUE.
  - **Outputs:** PlayerViewModel commands (playSongAt, moveSong, stopAt, clearQueue).
  - **Dependencies:** PlayerViewModel, reorderable (sh.calvin), LazyColumnScrollbar, AnimatedEqBars, MediaImage.
  - **Consumed by:** QueueFragment.
  - **Related files:** QueueFragment.kt, PlayerViewModel.kt.
  - **Notes:** QUEUE_DEBOUNCE delays reorder commits; stop-after toasts observed.


- `ui/screen/scrobbling/ScrobblingServiceLoginFragment.kt` — Compose bottom sheet for scrobbling login (Last.fm/Libre.fm): user/token or API key form.
  - **Purpose:** Authenticate Last.fm/ListenBrainz scrobbling accounts.
  - **Responsibilities:** LastFmForm (username/password), ListenBrainzForm (token), LoginState crossfade, logout, connected user card.
  - **Inputs:** ScrobblingService extra, LibraryViewModel login state.
  - **Outputs:** logInToService/logoutFromService calls.
  - **Dependencies:** LibraryViewModel, LoginParams, LoginState, BottomSheetDialogSurface.
  - **Consumed by:** PreferencesScreenFragment (LASTFM_LOGIN/LISTENBRAINZ_LOGIN).
  - **Related files:** PreferencesScreenFragment.kt, LibraryViewModel.kt.
  - **Notes:** Logs out failed logins on dispose.


- `ui/screen/settings/SettingsFragment.kt` — Settings container with nested NavHostFragment, toolbar and destination listener.
  - **Purpose:** Host settings categories in nested navigation.
  - **Responsibilities:** Toolbar title per destination, back-press handling (collapse panel, pop child stack), child NavController listener.
  - **Inputs:** Layout fragment_settings, child NavHostFragment.
  - **Outputs:** Title updates, navigation pops.
  - **Dependencies:** AbsMainActivityFragment, NavController.OnDestinationChangedListener, BottomSheetBehavior.
  - **Consumed by:** Main navigation graph.
  - **Related files:** SettingsScreen.kt, PreferencesScreenFragment.kt.
  - **Notes:** onBackPressedCallback removed when child stack empty.


- `ui/screen/settings/SettingsScreen.kt` — Enum mapping each settings category to its XML preference layout and navigation action.
  - **Purpose:** Declarative mapping of settings categories.
  - **Responsibilities:** Enumerate Appearance/NowPlaying/Lyrics/Playback/Library/Network/Advanced with layoutRes and navAction.
  - **Inputs:** None.
  - **Outputs:** Lookup used by PreferencesScreenFragment navigation.
  - **Dependencies:** R.xml.preferences_screen_*, navigation actions.
  - **Consumed by:** PreferencesScreenFragment (toEnum), App.kt.
  - **Related files:** PreferencesScreenFragment.kt, App.kt.
  - **Notes:** Pure declaration, no logic.


- `ui/screen/settings/PreferencesScreenFragment.kt` — PreferenceFragmentCompat with all preference screens and change handlers.
  - **Purpose:** Render and handle all XML preference screens.
  - **Responsibilities:** Bind change listeners (theme, language, font, backup/restore, update search), custom dialogs via onDisplayPreferenceDialog, cache clearing, summaries.
  - **Inputs:** SharedPreferences, LibraryViewModel, LyricsViewModel, UpdateViewModel.
  - **Outputs:** Preference changes, BackupHelper create/restore, ScrobblingServiceLoginFragment, font import.
  - **Dependencies:** ThemePreference, ProgressIndicatorPreference, SwitchWithButtonPreference, BackupHelper, CoverProvider, AppCompatDelegate.
  - **Consumed by:** SettingsFragment nested graph.
  - **Related files:** SettingsFragment.kt, SettingsScreen.kt, util/BackupHelper.kt.
  - **Notes:** Seven subclasses per category screen; DynamicColors applied for Material You.


- `ui/screen/sleeptimer/SleepTimerFragment.kt` — Expanded BottomSheetDialogFragment showing SleepTimerBottomSheet with SleepTimerViewModel.
  - **Purpose:** Container for sleep timer bottom sheet.
  - **Responsibilities:** Force expanded state, ComposeView with SleepTimerBottomSheet, Koin viewModel scoping.
  - **Inputs:** SleepTimerViewModel.
  - **Outputs:** SleepTimerBottomSheet composition.
  - **Dependencies:** SleepTimerViewModel, SsilencceTheme, BottomSheetBehavior.
  - **Consumed by:** Player UI sleep timer action.
  - **Related files:** SleepTimerScreen.kt, SleepTimerViewModel.kt.
  - **Notes:** Fragment-scoped viewModel.


- `ui/screen/sleeptimer/SleepTimerScreen.kt` — Compose sleep timer bottom sheet: minutes slider, end-music/fade-out options, start/stop.
  - **Purpose:** Configure and control the sleep timer.
  - **Responsibilities:** Slider 5-180 min, quick presets, finish-music and fade-out switches with duration slider, animated start/cancel button.
  - **Inputs:** SleepTimerViewModel uiState/events.
  - **Outputs:** setTimerState, startTimer, cancelTimer calls.
  - **Dependencies:** SleepTimerViewModel, SleepTimerUiState/WaitingFor/Event, haptic feedback.
  - **Consumed by:** SleepTimerFragment.
  - **Related files:** SleepTimerFragment.kt, SleepTimerViewModel.kt.
  - **Notes:** Haptics on events; countdown text from state.


- `ui/screen/sleeptimer/SleepTimerViewModel.kt` — Combines SleepTimer state with DataStore preferences (fade-out duration, timer value) and emits events.
  - **Purpose:** Bridge SleepTimer service state with persisted preferences.
  - **Responsibilities:** Combine isRunning/waitingFor with sleep_timer DataStore, persist options, start/cancel with exact-alarm permission fallback, timer updater lifecycle.
  - **Inputs:** Application, SleepTimer.
  - **Outputs:** SleepTimerUiState StateFlow, SleepTimerEvent channel.
  - **Dependencies:** SleepTimer, DataStore preferencesDataStore("sleep_timer").
  - **Consumed by:** SleepTimerScreen, SleepTimerFragment.
  - **Related files:** SleepTimerScreen.kt, playback/SleepTimer.kt.
  - **Notes:** createTimerUpdater/cancelTimerUpdater managed in init/onCleared.


- `ui/screen/sound/SoundSettingsFragment.kt` — Compose BottomSheetDialogFragment opening SoundSettingsScreen with EqualizerViewModel.
  - **Purpose:** Container for sound settings bottom sheet.
  - **Responsibilities:** Expanded sheet in landscape, ComposeView with SoundSettingsSheet, EqualizerViewModel injection.
  - **Inputs:** EqualizerViewModel.
  - **Outputs:** SoundSettingsSheet composition.
  - **Dependencies:** EqualizerViewModel, BottomSheetDialogSurface.
  - **Consumed by:** Player sound settings action.
  - **Related files:** SoundSettingsScreen.kt, EqualizerViewModel.kt.
  - **Notes:** DisposeOnViewTreeLifecycleDestroyed composition strategy.


- `ui/screen/sound/SoundSettingsScreen.kt` — Compose sound settings bottom sheet: balance, tempo, replay gain, volume, connected to EQ state.
  - **Purpose:** Quick volume/balance/tempo/advanced audio controls.
  - **Responsibilities:** Volume slider with iconified track, centered balance slider, speed/pitch sliders with presets, audio device card, offload/float/skip-silence switches.
  - **Inputs:** EqualizerViewModel state flows, AudioDevice, BitPerfectState.
  - **Outputs:** EqualizerViewModel setters (setVolume, setBalance, setTempo, showOutputDeviceSelector).
  - **Dependencies:** EqualizerViewModel, IconifiedSliderTrack, ShapedText, TitledCard.
  - **Consumed by:** SoundSettingsFragment.
  - **Related files:** SoundSettingsFragment.kt, EqualizerViewModel.kt, core/model/audiodevice.
  - **Notes:** Audio effects gated when offload/bit-perfect/float active; bit-perfect UI temporarily disabled.


- `ui/screen/tageditor/SongTagEditorActivity.kt` — Song tag editor activity with all fields (title, artist, year, track, lyrics...), cover and MusicBrainz search dialog.
  - **Purpose:** Edit full song metadata tags.
  - **Responsibilities:** Bind TagEditorResult to fields, genre selector, MusicBrainz autofill search, Deezer cover download, propertyMap for saving.
  - **Inputs:** EditTarget extra, TagEditorViewModel.
  - **Outputs:** TagEditorResult-driven fields; save via propertyMap.
  - **Dependencies:** AbsTagEditorActivity, TagEditorViewModel, MusicBrainzRecording, Deezer service, MetadataReader keys.
  - **Consumed by:** MenuItemClickExt, AbsPlayerFragment, SongDetailFragment.
  - **Related files:** TagEditorViewModel.kt, AbsTagEditorActivity.kt, ui/component/menu/MenuItemClickExt.kt.
  - **Notes:** MusicBrainz recordings dialog maps credits/releases into tags.


- `ui/screen/tageditor/AlbumTagEditorActivity.kt` — Album tag editor (via AbsTagEditorActivity): album/album artist/conductor fields, cover, web search.
  - **Purpose:** Batch-edit album-level tags.
  - **Responsibilities:** Album fields binding, default genre, Deezer album cover download, web search, propertyMap.
  - **Inputs:** EditTarget extra, TagEditorViewModel.
  - **Outputs:** Album TagEditorResult; save via propertyMap.
  - **Dependencies:** AbsTagEditorActivity, TagEditorViewModel, MetadataReader keys.
  - **Consumed by:** MenuItemClickExt (album edit).
  - **Related files:** TagEditorViewModel.kt, AbsTagEditorActivity.kt.
  - **Notes:** No MusicBrainz autofill.


- `ui/screen/tageditor/ArtistTagEditorActivity.kt` — Artist tag editor with artist image (Coil), name fields and change confirmation.
  - **Purpose:** Edit artist name/genre and custom artist image.
  - **Responsibilities:** Artist image picker/search/reset via CustomArtistImageManager, Coil loading with disabled cache, propertyMap with single-song ARTIST fallback.
  - **Inputs:** EditTarget extra, TagEditorViewModel.
  - **Outputs:** Artist image updates; save via propertyMap.
  - **Dependencies:** AbsTagEditorActivity, TagEditorViewModel, SingletonImageLoader, CustomArtistImageManager.
  - **Consumed by:** MenuItemClickExt (artist edit).
  - **Related files:** TagEditorViewModel.kt, AbsTagEditorActivity.kt.
  - **Notes:** downloadOnlineImage/restoreImage intentionally empty.


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
  - **Notes:** uris derived from target.contents.


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


### res/ (resources)

- `res/drawable/` — ~170 XML drawables: Material icons (ic_*), logos, selectors, shadows, animated vector drawables (play/pause/favorite), squiggly thumb.
  - **Purpose:** Static and animated vector drawable assets for the whole UI.
  - **Responsibilities:** Material icons, player AVDs (play/pause/favorite), selectors, shadows, squiggly seekbar thumb.
  - **Inputs:** Referenced from layouts, menus, code via R.drawable.
  - **Outputs:** Rendered icons/animations across app, widgets and notifications.
  - **Dependencies:** AAPT2 vector resources, referenced drawables from code (nowPlayingButtonStyle, MorphicIconButton).
  - **Consumed by:** All layouts/menus, player styles, widget Glance composables, AdapterExt, NowPlayingAction.
  - **Related files:** res/values/, res/layout/, core/model/theme/NowPlayingButtonStyle.kt.
  - **Notes:** Animated vector drawables named avd_* (avd_play/avd_pause) drive player animations.


- `res/layout/` — ~100 layouts: player fragments per style, home, details, item layouts, dialogs, tag editor, widgets; with `layout-land/` variants.
  - **Purpose:** XML layouts for the legacy View UI (library, player, dialogs, widgets).
  - **Responsibilities:** Fragment layouts per player style, item_* adapter rows, detail screens, dialogs, tag editor, permission views.
  - **Inputs:** ViewBinding-generated classes consumed by fragments/adapters.
  - **Outputs:** Inflated legacy Views bound in Kotlin.
  - **Dependencies:** AndroidX View/ViewBinding, Material Components, custom views (MusicSlider, BaselineGridTextView, WidthFitSquareLayout).
  - **Consumed by:** All legacy fragments/adapters/activities (Abs*Fragment, adapters, dialog bindings).
  - **Related files:** res/values/, ui/component/views/, ui/adapters/.
  - **Notes:** layout-land/ variants; Compose migration in progress (rule 14: new UI in Compose).


- `res/values/` — Strings (main file plus ~40 locale translations), colors (night/v31), dimens (land/large/sw600dp/xlarge), styles/themes, arrays, attrs, bools, ids, integers.
  - **Purpose:** Localized strings and static resource values.
  - **Responsibilities:** strings.xml base + ~40 locales, colors (incl. night/v31 black theme), dimens buckets, themes/styles, attributes, booleans, integers.
  - **Inputs:** Referenced via R.string/R.color/R.dimen/R.style/R.attr everywhere.
  - **Outputs:** Localized UI text and themed resources.
  - **Dependencies:** Android resource system; Material 3 themes.
  - **Consumed by:** Entire app (string/color/dimension resolution in Kotlin and XML).
  - **Related files:** res/layout/, res/xml/, ui/theme/.
  - **Notes:** Rule 4: only edit values/strings.xml; translations are managed elsewhere.


- `res/navigation/` — NavGraphs: `graph_main.xml` and `graph_settings.xml`.
  - **Purpose:** Navigation graphs wiring the main and settings destinations.
  - **Responsibilities:** Declare library/player/settings destinations, actions, arguments (SafeArgs).
  - **Inputs:** Referenced by MainActivity NavHost and SettingsFragment child NavHost.
  - **Outputs:** Generated NavDirections/Args classes used by NavigationExt and fragments.
  - **Dependencies:** AndroidX Navigation + SafeArgs plugin.
  - **Consumed by:** MainActivity, SettingsFragment, extensions/navigation/NavigationExt.kt.
  - **Related files:** extensions/navigation/NavigationExt.kt, ui/screen/*.
  - **Notes:** Destinations match the "nav_*" ids referenced in Kotlin navigation code.


- `res/menu/` — ~25 XML menus (song/album/artist/playlist/detail/selection/now-playing).
  - **Purpose:** Menu definitions for popups, toolbars and contextual action bars.
  - **Responsibilities:** item_* row popups, library screens menus, selection (CAB) menus, now-playing menus.
  - **Inputs:** Referenced via R.menu in adapters (OnClickMenu) and fragments.
  - **Outputs:** Menu items resolved in MenuItemClickExt/onSongMenu handlers.
  - **Dependencies:** Android Menu API, PopupMenu.
  - **Consumed by:** All adapters, AbsPlayerFragment, AbsRecyclerViewFragment, SearchAdapter.
  - **Related files:** ui/component/menu/, ui/adapters/, AbsMultiSelectAdapter.
  - **Notes:** Selection menus share ids like action_multi_select_adapter_check_all.


- `res/xml/` — Preference screens per category, widget info files, shortcuts, searchable, backup descriptors, provider paths, automotive app descriptor.
  - **Purpose:** XML configuration: preference screens, widget metadata, shortcuts, providers.
  - **Responsibilities:** preferences_screen_* per category, widget_info files, shortcuts.xml, searchable.xml, backup rules, file_paths, automotive_app_desc.
  - **Inputs:** Referenced from AndroidManifest and PreferencesScreenFragment/SettingsScreen.
  - **Outputs:** Preference screens rendered by PreferenceFragmentCompat; system metadata.
  - **Dependencies:** AndroidX Preference, App Widget metadata, FileProvider paths.
  - **Consumed by:** PreferencesScreenFragment, manifest components, Glance widget receivers.
  - **Related files:** ui/screen/settings/, app/src/main/AndroidManifest.xml.
  - **Notes:** Preference keys must match util/Preferences.kt constants.


- `res/font/`, `res/anim/`, `res/raw/` — Google Sans Flex font, fragment/item animations, keep.xml.
  - **Purpose:** Bundled font family, animation resources and asset keep-rules.
  - **Responsibilities:** googlesansflex_* font weights, fragment/item animations, raw/keep.xml preserving resources.
  - **Inputs:** R.font.googlesansflex_* (Type.kt), R.anim references, keep rules.
  - **Outputs:** App typography (ui/theme/Type.kt), view/fragment transitions.
  - **Dependencies:** Android resource system.
  - **Consumed by:** ui/theme/Type.kt, FragmentExt transitions, item animations.
  - **Related files:** ui/theme/Type.kt, extensions/FragmentExt.kt.
  - **Notes:** Font is the sole bundled family; custom fonts importable via settings.
