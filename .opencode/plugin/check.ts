import { spawn } from "node:child_process"
import { createWriteStream, existsSync } from "node:fs"
import path from "node:path"

const KOTLIN_RE = /\.kts?$/i

interface ToolExecuteAfterEvent {
  tool: string
  input: unknown
}

interface PluginContext {
  location: { directory: string }
  tool: {
    hook(
      name: "execute.after",
      callback: (event: ToolExecuteAfterEvent) => Promise<void> | void,
    ): Promise<unknown>
  }
}

const runSync = (cmd: string, args: string[]): Promise<void> =>
  new Promise((resolve) => {
    const child = spawn(cmd, args, { stdio: "ignore", windowsHide: true })
    child.on("close", () => resolve())
    child.on("error", () => resolve())
  })

// OpenCode V2 validates the default export as a definition object ({ id, setup });
// the V1 factory-function shape is rejected at load time.
export default {
  id: "ssilencce.check",
  async setup(ctx: PluginContext) {
    const root = ctx.location.directory
    const ktlintScript = path.join(root, "scripts", "ktlint.ps1")
    const gradlew = path.join(root, "gradlew.bat")

    await ctx.tool.hook("execute.after", async (event) => {
      if (event.tool !== "edit" && event.tool !== "write") return

      const args = event.input as { path?: string; filePath?: string } | undefined
      const filePath = args?.path ?? args?.filePath
      if (!filePath || !KOTLIN_RE.test(filePath)) return

      const abs = path.isAbsolute(filePath) ? filePath : path.join(root, filePath)
      const normalized = path.relative(root, abs).replace(/\\/g, "/")
      if (
        normalized.startsWith(".opencode/") ||
        normalized.includes("/node_modules/") ||
        normalized.includes("/.gradle/")
      ) {
        return
      }
      if (normalized.endsWith(".kt") && !normalized.startsWith("app/src/")) return
      if (!existsSync(abs)) return

      await runSync("powershell.exe", [
        "-NoProfile",
        "-ExecutionPolicy",
        "Bypass",
        "-File",
        ktlintScript,
        "format",
        abs,
      ])

      const logStream = createWriteStream(path.join(root, ".opencode", "lint.log"), { flags: "a" })
      const lint = spawn(
        "powershell.exe",
        ["-NoProfile", "-Command", `& '${gradlew}' :app:lintGithubDebug`],
        { cwd: root, detached: true, stdio: ["ignore", logStream, logStream], windowsHide: true },
      )
      lint.unref()
    })
  },
}
