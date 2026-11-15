# Audio cutter / trimmer

## Why

Users need to turn a song or recording into a short clip for a ringtone, notification, alarm or
sharing, without installing another app or re-encoding audio (which costs quality, CPU and battery).

## What Changes

- New Compose screen `AudioCutterActivity`, opened from song menus ("Trim audio").
- RangeSlider selection of start/end (waveform-less), live labels, and preview playback of the
  selected range with clipping (ExoPlayer).
- Lossless range export using `MediaExtractor` + `MediaMuxer`:
  - M4A/MP4 AAC and ADTS AAC → MP4 muxer.
  - MP3 → frame-aligned raw byte copy (no muxer, no re-encode).
  - OGG Opus/Vorbis → OGG muxer on Android 11+.
  - Other codecs report a clear unsupported message.
- Export destination via Storage Access Framework (`ACTION_CREATE_DOCUMENT`).
- "Set as ringtone / notification / alarm" actions: the clip is published to the matching
  MediaStore collection (legacy `Ringtones`/`Notifications`/`Alarms` folder on API 26–28), then set
  through `RingtoneManager`; `WRITE_SETTINGS` is requested via `ACTION_MANAGE_WRITE_SETTINGS`.
- No new settings; existing playback behaviour is unchanged.
