# Docs delta — menu-customization

## docs/FEATURES.md — add under "## 9. UI, Themes & System" (after the Library UI bullet)

- **Entity menu customization** — Appearance > Menu actions lets you show/hide and reorder the actions of the song, album, artist, genre, playlist and folder option menus. Defaults keep every action in its current order; hidden actions are removed from the inflated menu and the rest are re-inserted in the saved order while submenus and uncatalogued actions are preserved. Genre customization applies to the genre and year detail overflow menu because genre rows have no item popup of their own.

## docs/CODE_MAP.md — add these file entries (or merge into the closest package entry)

- `core/model/menu/MenuCustomizationGroup.kt` — **Purpose:** Catalog and persisted state for the per-entity option-menu customization. **Responsibilities:** declares `MenuCustomizationItem` (stable string key, menu item id, title), `MenuCustomizationState` (display order + hidden keys, CSV serialization with a `-` prefix for hidden entries) and the `MenuCustomizationGroup` enum mapping each entity to its preference key, settings title and `menu_item_*`/`menu_genre_detail` resource. Normalization appends catalog entries missing from an older saved value as enabled so new actions keep their default behavior. **Inputs:** saved preference string. **Outputs:** normalized state consumed by `Preferences` and `MenuCustomizationFilter`. **Dependencies:** `R.id`/`R.string`. **Consumed by:** `util/Preferences.menuCustomization`, `MenuCustomizationFilter`, `MenuCustomizationDialog`, `PreferencesScreenFragment`. **Notes:** keys are stored instead of resource ids because ids can shift between builds.
- `ui/component/menu/MenuCustomizationFilter.kt` — **Purpose:** Filter layer applied over inflated entity menus. **Responsibilities:** removes hidden catalog actions, re-inserts the remaining catalog actions in the saved order (preserving group id, icon, visibility, enabled/checked state, shortcuts, intent and submenu children) and leaves non-catalog actions such as sort entries or the search icon in place. **Inputs:** inflated `Menu` plus a menu resource or `MenuCustomizationGroup`. **Outputs:** mutated menu. **Dependencies:** `MenuCustomizationGroup`, `Preferences`. **Consumed by:** `newPopupMenu` (all item popups) and the genre/year detail overflow inflation. **Notes:** a no-op when the saved order already matches the inflated order, so the default path does not rebuild anything.
- `ui/component/preferences/dialog/MenuCustomizationDialog.kt` — **Purpose:** Compose settings dialog for one entity menu. **Responsibilities:** shows enabled/disabled checkboxes and up/down arrows for every catalog action and persists each change immediately through `Preferences.setMenuCustomization`. **Inputs:** `MenuCustomizationGroup` argument. **Outputs:** persisted menu state. **Dependencies:** Material 3 Compose, `SsilencceTheme`, `Preferences`. **Consumed by:** `PreferenceScreenFragment.onDisplayPreferenceDialog` for the six `menu_customization_*` keys. **Notes:** static labels use `stringResource` during composition; no `getString` calls in callbacks.

Update existing entries:

- `ui/component/menu/MenuClick.kt` — `newPopupMenu` now applies `MenuCustomizationFilter` to the inflated menu before the caller's preparation callback.
- `util/Preferences.kt` — add `menuCustomization(group)` / `setMenuCustomization(group, state)` next to the quick-access accessors (CSV string per entity preference key).
- `ui/screen/settings/PreferencesScreenFragment.kt` — dispatches the six `menu_customization_*` dialog preferences to `MenuCustomizationDialog` and refreshes their summaries from the saved state.
- `ui/screen/library/genres/GenreDetailFragment.kt`, `ui/screen/library/years/YearDetailFragment.kt` — apply `MenuCustomizationGroup.Genre` to the shared `menu_genre_detail` inflation before sort entries are appended.

## docs/MISSING_FEATURES.md — replace the bullet

Replace:

- **Per-entity option/menu customization** — main menu, quick access, song/folder/album/artist/genre/playlist option lists.

with:

- **Main menu customization** — configurable entries for the primary navigation/drawer (quick access and the song/album/artist/genre/playlist/folder option lists are implemented).

(Only keep this bullet if main-menu customization is still missing after merge; otherwise remove it entirely.)

## docs/architecture/ADR-052-menu-customization-filter.md (new ADR; renumber if taken)

**Status:** Accepted

**Context:** Entity option menus are XML resources inflated at many call sites (list item popups, search results, folder rows, genre detail overflow). Users need to hide and reorder their actions, but rewriting each menu as Compose or duplicating comparisons per adapter would be invasive and error-prone.

**Decision:** Keep the XML menus as the single source of truth and add a filter layer that runs immediately after menu inflation. A code-level catalog declares the configurable actions per entity with stable string keys (resource ids are not stable across builds). The saved state is one CSV string per entity holding display order and hidden keys, stored in the existing default `SharedPreferences` so backup/restore carries it automatically. The filter only removes/re-adds catalogued actions, preserving submenus and non-catalogue entries, and is a no-op when the saved order matches the inflated order. Settings use a single Compose dialog per entity reached from Appearance > Menu actions.

**Consequences:** Menus keep their existing click handling and XML titles; new menu actions stay enabled by default and appear after a saved order is normalized. Non-popup menus must opt in by calling the filter explicitly (currently the genre/year detail overflow). Customizing an action's title still requires editing the XML and the catalog entry.

## Decisions

- **Genre group uses the detail overflow.** Genre rows have no existing item popup, so mapping the group to `menu_genre_detail` avoids adding a new genre menu (which would change default UI). The same resource is used by year detail, so the customization applies there too; this is documented in the feature bullet.
- **Context-only actions are not catalogued.** Song "Remove from history" only appears in history mode, and the genre search icon is a toolbar `showAsAction="ifRoom"` item whose action layout cannot be preserved when an item is removed and re-added. Both keep their current behavior.
- **No Room/schema change.** State is two simple preference strings per group and is included in existing preference backup.
- **Defaults keep behavior.** No stored preference means every catalog action stays visible in XML order; the filter returns without rebuilding the menu.
