# Proposal: Composers and Album-Artists library tabs

## Why

The library exposes one Artists category. Album-artist data already exists inside Artists through
an `onlyAlbumArtists` toggle, and composers are not browsable at all even though MediaStore exposes
a composer column on API 30+.

## What Changes

- Add separate **Album artists** and **Composers** library categories.
- Album artists always use the existing `albumArtists()` queries and artist detail path.
- When Album artists is visible, Artists lists regular artists and hides the old “Only album
  artists” toggle; when Album artists is hidden, existing `onlyAlbumArtists` behavior is unchanged.
- Composers use `MediaStore.MediaColumns.COMPOSER` on API 30+. The category is hidden on API < 30;
  no metadata-scan fallback is added.
- Reuse the existing artist list/detail/playback screens with a nullable composer name and
  composer-aware `Artist`/`Song` fields.
- Preserve existing persisted category order and append new categories invisible by default.
- No Room schema migration is required: album-artist data and API 30+ composer data come from
  MediaStore.

## Impact

- New: `AlbumArtistListFragment`, `ComposerListFragment`, pure `ComposerNamePolicy`, OpenSpec.
- Modified: category/preferences/nav/view-model/artist/song/repository/adapter/detail/menu/docs.
- Out of scope: Android Auto root entries, dedicated global-search filter modes, metadata-scan
  composer fallback on API 26–29, and multi-composer splitting.
