# Tasks - build readiness

- [x] Ran `powershell -NoProfile -ExecutionPolicy Bypass -File .orchestra\build-gate.ps1 -Tasks :app:assembleGithubDebug` on `_integration`; exit 0, `BUILD SUCCESSFUL`.
- [x] Ran `powershell -NoProfile -ExecutionPolicy Bypass -File .orchestra\build-gate.ps1 -Tasks :app:lintGithubDebug` separately; exit 0, `BUILD SUCCESSFUL` (lint report written, no blocking findings).
- [x] No fixes were required; all 22 integrated briefs, defaults, versions and previous fixes remain untouched.
- [x] No tests, commits/history, translated strings, project docs, or edits outside `_integration`.
