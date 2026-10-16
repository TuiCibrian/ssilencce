# Domain docs

Single-context repo: domain knowledge lives in Architecture Decision Records (ADRs) under `docs/architecture/` plus this `AGENTS.md`. No root `CONTEXT.md`.

## Layout

- `docs/architecture/ADR-*.md` — ADR-001…ADR-037 (see directory); the numbered, dated record of architectural decisions
- `AGENTS.md` — repo conventions and rules for agents
- `docs/CODE_MAP.md` — per-file code map

## Consumer rules

- Before changing architecture, read the relevant ADR(s) and cite them by number when a decision constrains a change
- Key stack decisions: Koin (ADR-006), Ktor (ADR-020), Navigation 2.9.8 (ADR-027), hybrid View/Compose (ADR-026), legacy now-playing styles (ADR-028), dynamic color/theming (ADR-029), pre-Aurora UI restoration (ADR-035)
- ADR-033 and ADR-034 are superseded by ADR-035. The `apply-aurora-glass-*` proposals are historical records of a reverted implementation. ADR-036 authorizes the new optional native reference implementation tracked as `implement-native-aurora-reference`; see `docs/AURORA_REFERENCE.md` for capability mapping. ADR-037 refines the Aurora chrome (drawer-led navigation, retired category strip, floating header actions) under the `refine-aurora-chrome` change.
- Never propose something contradicting a settled ADR without a new ADR superseding it
