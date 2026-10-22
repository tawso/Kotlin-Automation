package tawsor.ka.tasks

import tawsor.ka.core.PipelineContext
import java.io.File

object Dotenv {
    fun parse(file: File): Map<String, String> {
        if (!file.exists() || !file.isFile) return emptyMap()

        val result = mutableMapOf<String, String>()
        file.forEachLine { line ->
            val trimmed = line.trim()
            if (trimmed.isNotEmpty() && !trimmed.startsWith("#") && trimmed.contains("=")) {
                val index = trimmed.indexOf("=")
                val key = trimmed.substring(0, index).trim()
                var value = trimmed.substring(index + 1).trim()
                if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
                    value = value.substring(1, value.length - 1)
                }
                result[key] = value
            }
        }
        return result
    }
}

fun PipelineContext.loadDotenv(file: File = File(".env")): Map<String, String> {
    val vars = Dotenv.parse(file)
    if (vars.isNotEmpty()) {
        logInfo("Loaded ${vars.size} environment variables from '${file.name}'")
        for ((k, v) in vars) {
            set(k, v)
        }
    }
    return vars
}

fun PipelineContext.env(key: String, default: String? = null): String {
    return get<String>(key) ?: System.getenv(key) ?: default ?: throw IllegalStateException("Environment variable '$key' not found")
}
