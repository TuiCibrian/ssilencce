# Tasks

- [x] Add `AudioClipFormat` / inspection model and MIME → output format mapping.
- [x] Implement `AudioClipExporter` (MediaExtractor + MediaMuxer, temp file, SAF copy).
- [x] Implement `Mp3RawTrimmer` (ID3v2 preservation, frame-aligned byte copy, MPEG 1/2/2.5).
- [x] Implement `RingtoneClipStore` (MediaStore publish on API 29+, legacy folder + scanner on 26–28, `RingtoneManager`).
- [x] Add Compose screen + ViewModel (`AudioCutterScreen`, `AudioCutterViewModel`, `AudioCutterActivity`).
- [x] Wire menu entry ("Trim audio") in song / playlist-song / now-playing / media-selection menus.
- [x] Add Manifest activity, `WRITE_SETTINGS` permission.
- [x] Register Koin services and ViewModel.
- [x] Add English strings next to related strings.
- [x] Compile gate passes.
- [x] Write DOCS_DELTA.md with documentation bullets.
