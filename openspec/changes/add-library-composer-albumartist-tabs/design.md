# Design: Composers and Album-Artists library tabs

## Context

`CategoryInfo.Category` drives the persisted library categories, dynamic bottom navigation,
Aurora drawer and settings category editor. Existing category lists are serialized, so new enum
entries do not appear for existing installs unless the getter merges missing entries.

Album-artist support already exists: `RealArtistRepository.albumArtists()`, `Artist.isAlbumArtist`,
`ArtistDetailFragment.artist_name`, and `MediaIDs.ALBUM_ARTISTS`. Composers do not: ordinary song
queries omit `MediaStore.MediaColumns.COMPOSER`, which is available only from API 30 while the app
supports API 26.

## Goals / Non-Goals

**Goals:**

- Separate Album artists and Composers tabs with existing list/detail/playback behavior.
- Use MediaStore data, with no Room schema migration.
- Preserve existing category order/defaults and existing `onlyAlbumArtists` behavior when the new
  Album artists tab is not used.
- Reuse the existing artist UI; no new Compose screen.

**Non-Goals:**

- Composer support on API 26–29 through a metadata scan.
- Android Auto root/browse entries for the new categories.
- Dedicated global-search filter modes for composers/album artists.
- Splitting a multi-composer tag into separate names.

## Decisions

### Category migration

Append `AlbumArtists` and `Composers` to `CategoryInfo.Category`. `Preferences.libraryCategories`
filters unavailable categories, preserves the stored order and appends missing available categories
with `visible = false`. `getDefaultLibraryCategoryInfos()` also filters unavailable categories, so
the original first five visible tabs remain unchanged. `Composers.isAvailable` is true only on API
30+.

### Artists / Album artists separation

- Album artists always uses `repository.allAlbumArtists()`.
- When the Album artists category is visible, `fetchArtists()` returns regular artists and the
  Artists list hides the old `action_album_artist` toggle.
- When it is hidden, existing `onlyAlbumArtists` behavior is retained.
- Navigation, search and Android Auto’s existing `onlyAlbumArtists` behavior are untouched.

### Composer data

`Song.composerName` is read from `MediaStore.MediaColumns.COMPOSER` only when
`Build.VERSION.SDK_INT >= R`; the projection includes it only on R+. No Room field/entity/migration
is added, so Room-backed songs keep a null composer.

`RealArtistRepository.composers()` groups nonblank composer names case-insensitively using
`ComposerNamePolicy`, splits each group into `Album`s with the existing album repository, and
builds `Artist(composerName = ...)` entries with synthetic stable IDs. `composer(name)` performs the
same grouping for one normalized name. The existing minimum-song and ignore-singles preferences
apply.

### Detail and actions

Composer rows and detail reuse `ArtistListFragment`/`ArtistDetailFragment`:
- `composer_name` is a new nullable detail argument.
- Composer detail skips Last.fm biography and similar artists.
- Composer menus hide tag-editor, add-to-home and play-info actions; playback/multi-select remain.
- The adapter transition key uses album-artist name or composer name instead of the global
  `onlyAlbumArtists` flag.

## Risks / Trade-offs

- Composers are unavailable on API 26–29; the category is hidden there.
- Room-backed songs (playlists/history) do not carry composer; composer browsing uses MediaStore
  songs only.
- Composer names are treated as one literal value; splitting multiple composers is deferred.
- When the Album artists tab is visible, the Artists list intentionally ignores the old preference.

## Verification

- Pure `ComposerNamePolicy` probe for normalization, case-insensitive keys, stable IDs and blank
  handling.
- Static source probe for API-30 projection guard, category migration/availability, nav/tab
  routing, Artists fallback, composer detail/action hiding, no schema/deps/translated strings and
  no Android Auto/global-search changes.
- ktlint, `git diff --check`, OpenSpec strict validation.
- No build/emulator until the root grants the lease after AA-03 runtime/D.
