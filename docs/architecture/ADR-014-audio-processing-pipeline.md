# ADR-014: In-Pipeline Audio Processing (Balance and ReplayGain)

## Status

Accepted

## Context

Stereo balance and ReplayGain loudness normalization must apply to *all* decoded audio before
it reaches the sink. Applying them at the UI/volume level would be imprecise (channels mixed by
the system) and tag-derived gain would not survive OS volume abstraction. Media3 exposes an
`AudioProcessor` hook inside `DefaultAudioSink`, which is the correct injection point.

## Decision

- Implement two custom Media3 `BaseAudioProcessor`s:
  - `BalanceAudioProcessor` — per-channel gain applied to 16-bit PCM (`setBalance(left,right)`),
    mono averages both gains; validated PCM-16 input only.
  - `ReplayGainAudioProcessor` — applies tag-derived ReplayGain in Album/Track/Off modes with
    preamp and peak limiting; supports 16- and 24-bit PCM (via `ByteUtils` int24 helpers) and
    passes through untouched at zero gain; gain switches on media-item transitions.
- Inject both into the audio sink via `SsilencceRenderersFactory`, a `DefaultRenderersFactory`
  subclass whose `buildAudioSink` returns a `DefaultAudioSink` wired with the processor array.
- Share the same singleton instances app-wide (Koin `single`) so state changes from
  `EqualizerManager`/`PlaybackService` affect the pipeline immediately.
- Extract ReplayGain tags with `ReplayGainTagExtractor`: standard tags, iTunes
  `----:com.apple.iTunes:` tags and Opus R128 (converted via `(raw/256) + 5 dB`), cached per URI
  in an `LruCache` (128 entries) and invalidated when the song changes.

## Consequences

- Balance and ReplayGain are sample-accurate and work with any output device, including
  bit-perfect USB paths.
- ReplayGain is computed on the fly from tags — no destructive audio modification, and albums
  stay consistent when tags change.
- Both processors add CPU work on the audio thread for PCM-16/24; passthrough at zero gain
  keeps the cost negligible for unaffected users.
- 24-bit support requires careful endianness handling; the processors are intentionally
  restricted to PCM to avoid format-conversion surprises.
