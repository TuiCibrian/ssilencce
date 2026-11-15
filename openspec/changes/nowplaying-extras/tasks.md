# Tasks: Now Playing extras

## 1. Preferences and resources

- [x] 1.1 Add `KEEP_SCREEN_ON_NOW_PLAYING`, `SEEK_BAR_TOUCH_ONLY_ON_THUMB` and `NOW_PLAYING_QUICK_ACCESS` keys plus typed accessors with behaviour-preserving defaults.
- [x] 1.2 Add switches and the quick-access preference to `preferences_screen_now_playing.xml`; wire the custom dialog and summaries.
- [x] 1.3 Add English strings next to the existing Now Playing strings.
- [x] 1.4 Add the `SET_WALLPAPER` permission and the screenshot cache path to `provider_paths.xml`.

## 2. Artwork viewer and sharing

- [x] 2.1 Add `ShowFullScreenArt` to `NowPlayingAction` and handle it from cover taps and the overflow menu.
- [x] 2.2 Add a full-screen Compose artwork viewer with pinch zoom, save to gallery and wallpaper targets.
- [x] 2.3 Add an artwork store using `MediaStore.Images` and `WallpaperManager`.
- [x] 2.4 Add player-view screenshot capture, optional square crop, FileProvider share.

## 3. Player behaviour

- [x] 3.1 Apply the keep-screen-on preference while the player panel is expanded, preserving lyrics-on-cover behaviour.
- [x] 3.2 Add a `Seek to...` Compose dialog and hooks on the elapsed-time label/menu.
- [x] 3.3 Implement thumb-only touch for `MusicSlider` and the Compose slider.
- [x] 3.4 Add `QuickAccessAction`, the quick-access row, and show it in Aurora and legacy player styles.

## 4. Verification

- [x] 4.1 Run the mandatory compile gate until green.
- [x] 4.2 Self-review the diff and record documentation deltas.
