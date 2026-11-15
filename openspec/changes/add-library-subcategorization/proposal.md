# Proposal: Library sub-categorization

## Why

The library still exposes flat album and genre detail lists. Composer data is available on API 30+
but albums cannot be narrowed by composer, and genre detail only lists songs even though the app
already has album and artist list/detail flows.

## What Changes

- Add an API 30+ **Filter by composer** action to the album list, shown only when at least one
  album song has a composer tag. The existing Compose choice dialog offers All composers plus
  normalized composer names and filters the existing album grid. No sectioned album adapter is
  added.
- Add a **View** action to genre detail with **Songs**, **Albums** and **Artists** subcategories.
  Album and artist content is derived from the existing genre song query through the existing
  album/artist grouping; existing adapters and detail screens are reused.
- Sort menus, Play/Shuffle and quick search follow the visible genre subcategory. The genre header
  continues to show the full genre song count/duration.
- Add a pure `GenreSubcategory` helper and a `GenreContent` value object. No Room schema migration
  or dependency is required.
- Out of scope: Android Auto subcategory entries, global-search filters, multi-composer splitting
  and sectioned album headers.

## Impact

- New: `GenreSubcategory`, `GenreContent`, OpenSpec change.
- Modified: album list, genre detail/view-model, repository grouping, genre sort modes, menus,
  main English strings, docs.
- No schema, dependency or version changes.
