package tawsor.ka.tasks

import tawsor.ka.core.PipelineContext
import java.io.File

object Git {
    suspend fun currentBranch(workDir: File? = null): String {
        val result = Shell.execute("git rev-parse --abbrev-ref HEAD", workDir)
        return if (result.isSuccess) result.stdout.trim() else "unknown"
    }

    suspend fun commitHash(short: Boolean = true, workDir: File? = null): String {
        val flag = if (short) "--short" else ""
        val result = Shell.execute("git rev-parse $flag HEAD".trim(), workDir)
        return if (result.isSuccess) result.stdout.trim() else "unknown"
    }

    suspend fun isClean(workDir: File? = null): Boolean {
        val result = Shell.execute("git status --porcelain", workDir)
        return result.isSuccess && result.stdout.isBlank()
    }
}

object FileSystem {
    fun cleanDirectory(dir: File) {
        if (dir.exists()) {
            dir.deleteRecursively()
        }
        dir.mkdirs()
    }

    fun copy(source: File, target: File) {
        source.copyRecursively(target, overwrite = true)
    }

    fun findFiles(dir: File, extension: String): List<File> {
        return dir.walkTopDown().filter { it.isFile && it.extension == extension }.toList()
    }
}

suspend fun PipelineContext.gitBranch(workDir: File? = null): String = Git.currentBranch(workDir)
suspend fun PipelineContext.gitCommit(short: Boolean = true, workDir: File? = null): String = Git.commitHash(short, workDir)

