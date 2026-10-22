package tawsor.ka.tasks

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tawsor.ka.core.PipelineContext
import java.io.File

data class ShellResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String
) {
    val isSuccess: Boolean get() = exitCode == 0
}

object Shell {
    suspend fun execute(
        command: String,
        workDir: File? = null,
        env: Map<String, String> = emptyMap()
    ): ShellResult = withContext(Dispatchers.IO) {
        val isWindows = System.getProperty("os.name").lowercase().contains("win")
        val processBuilder = if (isWindows) {
            ProcessBuilder("cmd.exe", "/c", command)
        } else {
            ProcessBuilder("sh", "-c", command)
        }

        if (workDir != null) {
            processBuilder.directory(workDir)
        }
        processBuilder.environment().putAll(env)

        val process = processBuilder.start()
        val stdout = process.inputStream.bufferedReader().readText()
        val stderr = process.errorStream.bufferedReader().readText()
        val exitCode = process.waitFor()

        ShellResult(exitCode, stdout, stderr)
    }
}

suspend fun PipelineContext.sh(
    command: String,
    workDir: File? = null,
    env: Map<String, String> = emptyMap()
): String {
    log("Executing command: $command")
    val result = Shell.execute(command, workDir, env)
    if (!result.isSuccess) {
        throw IllegalStateException("Command failed with code ${result.exitCode}:\n${result.stderr.ifBlank { result.stdout }}")
    }
    return result.stdout.trim()
}

