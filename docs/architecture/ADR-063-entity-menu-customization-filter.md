# ADR-063: Entity Menu Customization Filter

## Status

Accepted.

## Context

Entity option menus are XML resources inflated at many call sites (list item popups, search
results, folder rows, genre detail overflow). Users need to hide and reorder their actions, but
rewriting each menu as Compose or duplicating comparisons per adapter would be invasive and
error-prone.

## Decision

Keep the XML menus as the single source of truth and add a filter layer that runs immediately
after menu inflation. A code-level catalog declares the configurable actions per entity with
stable string keys (resource ids are not stable across builds). The saved state is one CSV string
per entity holding display order and hidden keys, stored in the existing default
`SharedPreferences` so backup/restore carries it automatically. The filter only removes/re-adds
catalogued actions, preserving submenus and non-catalogue entries, and is a no-op when the saved
order matches the inflated order. Settings use a single Compose dialog per entity reached from
Appearance > Menu actions.

## Consequences

- Menus keep their existing click handling and XML titles; new menu actions stay enabled by default
  and appear after a saved order is normalized.
- Non-popup menus must opt in by calling the filter explicitly (currently the genre/year detail
  overflow).
- Customizing an action's title still requires editing the XML and the catalog entry.
- Context-only actions (song "Remove from history", the genre search icon) are not catalogued and
  keep their current behavior.
- No Room or schema change; state travels with existing preference backup.

## Related

- ADR-026 (hybrid View/Compose UI), ADR-029 (theming), ADR-044 (library actions).
- `openspec/changes/menu-customization/`.
