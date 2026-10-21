# Domain docs

Single-context repo: domain knowledge lives in Architecture Decision Records (ADRs) under `docs/architecture/` plus this `AGENTS.md`. No root `CONTEXT.md`.

## Layout

- `docs/architecture/ADR-*.md` — 45 ADRs, ADR-001 through ADR-045 (see directory); the numbered, dated record of architectural decisions
- `AGENTS.md` — repo conventions and rules for agents
- `docs/CODE_MAP.md` — per-file code map

## Consumer rules

- Before changing architecture, read the relevant ADR(s) and cite them by number when a decision constrains a change
- Key stack decisions: Koin (ADR-006), Ktor (ADR-020), Navigation 2.9.8 (ADR-027), hybrid View/Compose (ADR-026), legacy now-playing styles (ADR-028), dynamic color/theming (ADR-029), pre-Aurora UI restoration (ADR-035)
- ADR-033 and ADR-034 are superseded by ADR-035. The `apply-aurora-glass-*` proposals are historical records of a reverted implementation. ADR-036 authorizes the new optional native reference implementation tracked as `implement-native-aurora-reference`; see `docs/AURORA_REFERENCE.md` for capability mapping. ADR-037 refines the Aurora chrome (drawer-led navigation, retired category strip, floating header actions) under the `refine-aurora-chrome` change. ADR-038 keys the persisted playback queue by position (`order`), allowing repeated songs to survive restarts. ADR-039 refuses release packaging without configured release signing and bans the silent debug-keystore fallback (explicit local opt-in only). ADR-040 adds append-only play events for period statistics; ADR-041 implements named queues and queue-local search (superseding the ADR-036 availability decision); ADR-042 defines per-track resume, audio-focus modes, the Quick Settings tile and bounded session recovery; ADR-043 defines opt-in scheduled backups; ADR-044 covers settings/list search, export formats and entity shortcuts.
- Never propose something contradicting a settled ADR without a new ADR superseding it
- ADR-045 orders immutable queue snapshots across pause/service teardown and restore barriers; it also defines same-UID app commands and request-bound, restorable audio-focus consent.
