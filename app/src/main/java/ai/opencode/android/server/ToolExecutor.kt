package ai.opencode.android.server

import android.util.Log
import kotlinx.serialization.Serializable
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

@Serializable
data class ToolResult(
    val success: Boolean,
    val output: String = "",
    val error: String = "",
    val exitCode: Int = 0
)

class ToolExecutor {
    companion object {
        private const val TAG = "ToolExecutor"
        private const val SANDBOX_DIR = "/data/local/tmp/opencode_sandbox"
        private const val MAX_OUTPUT_SIZE = 100 * 1024 // 100KB
        private const val TIMEOUT_SECONDS = 30L
    }

    init {
        val sandboxDir = File(SANDBOX_DIR)
        if (!sandboxDir.exists()) {
            sandboxDir.mkdirs()
        }
    }

    fun execute(command: String, args: List<String>): ToolResult {
        return try {
            val allowedCommands = listOf(
                "ls", "cat", "head", "tail", "wc", "grep", "find",
                "echo", "pwd", "date", "whoami", "uname",
                "python3", "python", "node", "sh"
            )

            val baseCommand = command.split(" ").firstOrNull() ?: ""
            if (baseCommand !in allowedCommands) {
                return ToolResult(
                    success = false,
                    error = "Command not allowed: $baseCommand"
                )
            }

            val processBuilder = ProcessBuilder()
            processBuilder.directory(File(SANDBOX_DIR))
            processBuilder.redirectErrorStream(true)
            processBuilder.environment()["HOME"] = SANDBOX_DIR
            processBuilder.environment()["PATH"] = "/system/bin:/system/xbin:$SANDBOX_DIR"

            val fullCommand = mutableListOf("sh", "-c", "$command ${args.joinToString(" ")}")
            processBuilder.command(fullCommand)

            val process = processBuilder.start()
            val output = StringBuilder()

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.appendLine(line)
                if (output.length > MAX_OUTPUT_SIZE) {
                    output.appendLine("\n... (output truncated)")
                    break
                }
            }

            val completed = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            if (!completed) {
                process.destroyForcibly()
                return ToolResult(
                    success = false,
                    error = "Command timed out after ${TIMEOUT_SECONDS}s",
                    exitCode = -1
                )
            }

            val exitCode = process.exitValue()
            ToolResult(
                success = exitCode == 0,
                output = output.toString().trim(),
                exitCode = exitCode
            )
        } catch (e: Exception) {
            Log.e(TAG, "Execute error", e)
            ToolResult(success = false, error = e.message ?: "Unknown error")
        }
    }

    fun readFile(path: String): ToolResult {
        return try {
            val file = resolveFile(path)
            if (!file.exists()) {
                return ToolResult(success = false, error = "File not found: $path")
            }
            if (!file.isFile) {
                return ToolResult(success = false, error = "Not a file: $path")
            }
            if (file.length() > MAX_OUTPUT_SIZE) {
                return ToolResult(success = false, error = "File too large (${file.length()} bytes)")
            }

            val content = file.readText()
            ToolResult(success = true, output = content)
        } catch (e: Exception) {
            Log.e(TAG, "Read error", e)
            ToolResult(success = false, error = e.message ?: "Unknown error")
        }
    }

    fun writeFile(path: String, content: String): ToolResult {
        return try {
            val file = resolveFile(path)
            file.parentFile?.mkdirs()
            file.writeText(content)
            ToolResult(success = true, output = "Written ${content.length} bytes to $path")
        } catch (e: Exception) {
            Log.e(TAG, "Write error", e)
            ToolResult(success = false, error = e.message ?: "Unknown error")
        }
    }

    fun listFiles(path: String): ToolResult {
        return try {
            val dir = resolveFile(path)
            if (!dir.exists()) {
                return ToolResult(success = false, error = "Directory not found: $path")
            }
            if (!dir.isDirectory) {
                return ToolResult(success = false, error = "Not a directory: $path")
            }

            val files = dir.listFiles()?.map { file ->
                val type = if (file.isDirectory) "d" else "-"
                val size = if (file.isFile) "${file.length()}B" else ""
                "$type ${file.name} $size"
            }?.sorted() ?: emptyList()

            ToolResult(success = true, output = files.joinToString("\n"))
        } catch (e: Exception) {
            Log.e(TAG, "List error", e)
            ToolResult(success = false, error = e.message ?: "Unknown error")
        }
    }

    private fun resolveFile(path: String): File {
        return if (path.startsWith("/")) {
            File(path)
        } else {
            File(SANDBOX_DIR, path)
        }
    }
}
