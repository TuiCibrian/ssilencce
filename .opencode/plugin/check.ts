import { spawn } from "node:child_process"
import { createWriteStream, existsSync } from "node:fs"
import path from "node:path"
import type { Plugin } from "@opencode-ai/plugin"

const KOTLIN_RE = /\.kts?$/i

const runSync = (cmd: string, args: string[]): Promise<void> =>
  new Promise((resolve) => {
    const child = spawn(cmd, args, { stdio: "ignore", windowsHide: true })
    child.on("close", () => resolve())
    child.on("error", () => resolve())
  })

export default (async ({ directory }) => {
  const root = directory
  const ktlintScript = path.join(root, "scripts", "ktlint.ps1")
  const gradlew = path.join(root, "gradlew.bat")

  return {
    "tool.execute.after": async (input) => {
      if (input.tool !== "edit" && input.tool !== "write") return
      const filePath = input.args?.filePath as string | undefined
      if (!filePath || !KOTLIN_RE.test(filePath)) return

      const normalized = filePath.replace(/\\/g, "/")
      if (
        normalized.startsWith(".opencode/") ||
        normalized.includes("/node_modules/") ||
        normalized.includes("/.gradle/")
      ) {
        return
      }
      if (normalized.endsWith(".kt") && !normalized.startsWith("app/src/")) return

      const abs = path.join(root, filePath)
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
    },
  }
}) satisfies Plugin
