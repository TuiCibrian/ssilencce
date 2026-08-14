# Domain docs

Single-context repo: domain knowledge lives in Architecture Decision Records (ADRs) under `docs/architecture/` plus this `AGENTS.md`. No root `CONTEXT.md`.

## Layout

- `docs/architecture/ADR-*.md` — ADR-001…ADR-032 (see directory); the numbered, dated record of architectural decisions
- `AGENTS.md` — repo conventions and rules for agents
- `docs/CODE_MAP.md` — per-file code map

## Consumer rules

- Before changing architecture, read the relevant ADR(s) and cite them by number when a decision constrains a change
- Key stack decisions: Koin (ADR-006), Ktor (ADR-020), Navigation 2.9.8 (ADR-027), hybrid View/Compose (ADR-026)
- Never propose something contradicting a settled ADR without a new ADR superseding it
