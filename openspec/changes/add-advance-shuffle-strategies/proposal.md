# Proposal: Album-artist and composer most-played shuffle strategies

## Contract decision (2026-11-10)

The product decision selects the **original weighted-shuffle contract**: rebuild the weighted
queue, start at position 0 and play. The later 4DA/5C current-item/position/play-intent
preservation requirement is rejected and superseded. The historical 5C preservation FAIL remains
a historical FAIL, never a PASS; its separate Source error is retained as a distinct playback
failure, not waived by this decision.

## Why

The smart shuffle sheet already has most-played strategies for songs, artists and albums, but no
strategy that weights songs by album artist or composer. The documented Advance Shuffle gap names
those two entities specifically. A source audit also found that `ExpandedSong` drops
`Song.composerName`, so a composer-aware strategy cannot read the tag from the expanded shuffle
rows without fixing that model projection.

## What Changes

- Add `MostPlayedAlbumArtists` and `MostPlayedComposers` to `SpecialShuffleMode`, with main English
  titles/descriptions and existing icons.
- Add a pure `ShuffleGroupKeyPolicy` for normalized album-artist/composer group keys and blank-tag
  fallbacks.
- Weight both new modes like the existing artist/album strategies: group play counts are summed,
  transformed with `ln(sum + 1)`, and combined with the same `1.0` base weight.
- Propagate `Song.composerName` through `ExpandedSong` so the composer strategy sees the tag.
- Gate composer availability at Android 11/API 30+ because the library exposes composer tags from
  that version (matching the Composers category); album-artist mode remains available on every
  supported API.
- The existing Compose shuffle sheet lists the new enum entries automatically; no new UI screen is
  required.
- Keep the original special-shuffle replacement contract: install the weighted queue from its first
  item at position 0 and start playback. The previous current item, position and paused/playing
  intent are intentionally not preserved (USER DECISION 2026-11-10).
- Update FEATURES, MISSING_FEATURES, CODE_MAP and OpenSpec.
- No Room schema, migration, dependency, version or translated-string change.

## Impact

- New: `core/model/shuffle/ShuffleGroupKeyPolicy.kt`,
  `core/model/shuffle/ShuffleModeAvailabilityPolicy.kt`, OpenSpec
  `add-advance-shuffle-strategies`.
- Modified: `SpecialShuffleMode`, `ShuffleModeBottomSheet`, `ShuffleManager`, `ExpandedSong`,
  `PlayerViewModel`, `PlaybackService`, `Playback`, `values/strings.xml`, docs.
- Existing `MostPlayedArtists`, `MostPlayedAlbums` and all other modes keep their current keys,
  weighting and output behavior.
