# system-ux

## Why

Four long-standing system-level UX gaps remain versus Musicolet: aggressive OEM battery
managers silently kill playback, users cannot choose an accent color that applies to both UI
stacks, advanced preferences clutter the settings screens, and cover art is always decoded even
when the user wants to save memory/battery.

## What Changes

- Add a **Battery & background** settings screen. It detects the OEM (Xiaomi/MIUI, Huawei/EMUI,
  Samsung, OPPO, Realme, Vivo, ASUS or generic Android), shows tailored background-playback
  steps, a "pin in recents" tip, and opens the known auto-start / battery-optimization screens
  with safe fallbacks. On MIUI it also exposes the launcher-shortcut permission guide.
- Add an **Accent color** picker in Appearance. The chosen color overrides the theme-derived
  accent in Compose (`SsilencceTheme`) and, where Material dynamic colors are available, in
  legacy views. "Default" keeps the current theme-derived behavior.
- Add a **Show unimportant settings** toggle to Advanced. When off, a curated set of advanced
  preferences (and the categories left empty) are hidden. Default keeps today's layout.
- Add a **Disable album art** library preference. All cover-art Coil fetchers short-circuit to
  the existing placeholders, and media-session artwork uses a placeholder bitmap.
