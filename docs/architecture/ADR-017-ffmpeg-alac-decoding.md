# ADR-017: FFmpeg Decoder and ALAC Hardware-Decoder Workaround

## Status

Accepted

## Context

Local libraries contain formats Media3's bundled extractors/decoders do not cover (various
lossless/legacy containers) and ALAC M4A files where vendor hardware decoders produce broken
or missing audio at high bitrates. The project needs a software fallback for broad format
support and a targeted workaround for ALAC.

## Decision

- Add the Jellyfin fork of the Media3 FFmpeg extension decoder
  (`org.jellyfin.media3:media3-ffmpeg-decoder` 1.9.0+1) alongside the official Media3 artifacts
  so unknown/rare formats can fall back to FFmpeg software decoding.
- Force ALAC through the software path with `AlacWorkaroundCodecSelector`, a
  `MediaCodecSelector` that delegates to `MediaCodecSelector.DEFAULT` but filters out
  `hardwareAccelerated` codec infos for `MimeTypes.AUDIO_ALAC`; if no software codec exists,
  the extension decoder (FFmpeg) is used.
- Wire the selector into the ExoPlayer builder in `PlaybackService`.

## Consequences

- High-bitrate ALAC M4A playback is stable across devices regardless of broken vendor
  decoders; the failure mode (silence/garbage audio) observed upstream is eliminated.
- Software ALAC decoding costs more CPU/battery for ALAC content — an accepted trade-off for
  correctness on the affected devices.
- The Jellyfin fork version must be tracked against official Media3 releases (version 1.9.0+1
  vs media3 1.10.1); upgrades require checking decoder compatibility.
- ABI splits (ADR-005) are now meaningful: each ABI carries its own native decoder blobs.
