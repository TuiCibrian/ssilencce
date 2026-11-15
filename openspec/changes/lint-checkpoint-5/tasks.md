# Tasks - lint-checkpoint-5

- [x] Inspect `BatchTagEditorActivity`: `requestSave`/`requestUndo` already send API < 30 to the
      direct write/undo path and only request `MediaStore` write consent on API 30+.
- [x] Replace the lint-invisible `hasR()` dependency inside `requestWriteAccess` with a real
      `Build.VERSION.SDK_INT >= Build.VERSION_CODES.R` guard; the helper keeps returning `false`
      when invoked below API 30, so API 26-29 behavior is unchanged.
- [x] Compile gate green (`:app:compileGithubDebugKotlin`).
- [x] `:app:lintGithubDebug` green (0 errors; `NewApi` finding for
      `MediaStore.createWriteRequest` gone).
