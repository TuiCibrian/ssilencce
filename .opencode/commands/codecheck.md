---
description: "Run the code-quality pass on the working tree (skills + lint gate)"
---

Run the code-quality pass after code changes: analyze the working-tree diff, apply the relevant skills, fix what is trivially fixable, and finish with the lint gate.

1. **Inventory the change**: `git status --porcelain` (includes untracked) and `git diff` against HEAD. Scope all analysis to those files.
2. **Load skills based on the touched files**:
   - Always: `code-security`
   - `**/*.kt` (Compose/UI): `compose-performance-audit` + `android-accessibility`
   - `.github/workflows/**`: `gha-security-review`
   - `*.kts` / `gradle/libs.versions.toml`: `gradle-build-performance` (diagnosis only)
   - Any Kotlin: `code-review` (Standards + Spec axes; spec source per `docs/agents/issue-tracker.md`)
3. **Apply** each skill to the diff. Fix trivial violations directly; report judgement calls without changing them.
4. **Gate**: run `./gradlew :app:lintGithubDebug`; fix lint errors (abortOnError fails the build) or report them.
5. **Report**: per-skill findings, lint summary (errors/warnings), and anything left for human decision.

Constraints: never commit (AGENTS.md rule 2); English only (rule 11); never touch versions (rule 8), metadata/README/contributors (rule 9), or non-main strings files (rule 3).
