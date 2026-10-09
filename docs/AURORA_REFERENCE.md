# Aurora Reference and Capability Mapping

Implementation target: `ssilencce-auroras.html` (black-and-white glass with a horizontal-wave white ribbon). The navy brand specification and reverted `apply-aurora-glass-*` changes are historical, not implementation targets. Architectural reconciliation: ADR-036.

## Native surfaces

| Reference surface | Native implementation and retained behavior |
| --- | --- |
| Home | `HomeFragment`, compact Compose action rows and repository-backed suggestions; real top tracks, recent songs, history and shuffle; always-visible search action with Settings in overflow |
| Queue | `AuroraQueueFragment` and the shared `QueueScreen`; one real queue, playback, repeat/shuffle, lock, reorder, locate, song menus and stop-after-song |
| Player | `AuroraPlayerFragment` / `AuroraPlayerScreen`; the same seven saved styles, compact horizontal Peek, expressive native morph button, `PlayerViewModel`, cover pager, configured cover actions and supported cover lyrics |
| Albums / album detail | Existing adapters, sorting/grid settings, selection, song actions and navigation |
| Artists / artist detail | Existing artist/album-artist modes, covers, repository data and menus |
| Folders | Existing filesystem/SAF browser and folder-scoped search; native permission and missing-storage states |
| Playlists / playlist detail | Existing create/import/export/edit, selection, reorder and song actions |
| Tags | No custom-tag browser; existing genres and release years remain reachable as real categories |
| Search | Existing debounced, filtered search and Android voice input, not prototype search simulation |
| Lyrics | Existing plain/LRC/TTML rendering, translation, karaoke, seeking, download sources and empty/loading states |
| Lyrics editor | Existing text editing, preview, sources, clipboard, online search, save and undo; no manual timing editor |
| Equalizer | Actual output-device header, engine band-count/profile chips, vertical band sliders and Bands/Sound pages; retained effects, profile management, reset and import/export, without invented ten-band capability |
| Sound | Existing output selection, volume, balance, tempo/pitch, offload, float output and skip-silence capability gates |
| Sleep timer | Existing countdown, finish-current-song and fade-out behaviors |
| Settings | Existing nested settings navigation and all retained preferences; appearance/motion switches added |
| Song details / metadata | Existing info dialogs and tag-editor activities; real metadata and write/permission handling |
| Drawer / More / menus | Real Navigation destinations, configurable categories, song menus and dialogs |

## Explicitly unavailable

- Multiple named queues and queue-local search.
- Custom-tag and composer browsing.
- Manual lyrics timing/synchronization and lyrics QR sharing.
- Audio crossfade, mono output and timers by number of songs.

Unavailable items do not mutate state or report simulated success. Library data, durations, names, cover art and EQ/output capabilities come from the app, not HTML fixtures.

## Compatibility and verification

- Existing installations keep their saved appearance until Aurora is explicitly enabled. New empty preference stores select Aurora and the system font.
- Turning Aurora off restores the retained theme and player-color settings. Saved player style, category visibility/order and cover transition keys remain unchanged.
- The native adaptation retains the mini-player, accessible touch targets, labelled long-press/time controls and accessibility headings, and Android system/keyboard insets rather than hiding working functionality to imitate a screenshot.
- Shell-owned `LibraryMargin.navigationMargin` supplies supporting and detail-screen clearance; More expansion does not rely on duplicated 72 dp constants or revive a destination-hidden mini-player. Floating legacy headers and category hold-to-search retain the native actions. The home surface uses compact horizontal action rows (38 dp icon box, 66 dp minimum) and an always-visible toolbar search action backed by real Navigation; Settings stays in the overflow and the library drawer entry keeps the hamburger icon.
- Shell destinations save isolated navigation stacks; replacing a category-filtered search discards the previous search snapshot. Back dismisses More before popping nested settings.
- FullCover has a dark header backplate for contrast over light artwork. Unsupported hardware EQ retains an explicit explanation without hiding the independent ReplayGain settings.
- Opaque lyrics backgrounds, FullCover and modal overlays stop the obscured shared ribbon. Expressive, squiggly, marquee and queue/lyrics indicators reuse platform/lifecycle/power gates with their own saved preferences. Separate metadata activities own their backdrop; the paused main activity does not animate behind them.
- The ribbon uses two cached bitmap meshes with draw-phase state reads; disabled-motion lyrics loading is static text, not a fictitious determinate progress value.
- Aurora queue headers and unavailable-search feedback scroll with the songs instead of consuming a fixed portion of a narrow or landscape viewport. Reordering and locate-current translate past the non-song header.
- Reduced-motion Expressive uses static play/pause icons, not the first frame of a paused animated vector.
- Runtime and visual evidence, limitations and unperformed acceptance checks are recorded with the active OpenSpec change. Do not infer a passed device/performance gate from build or lint output.
