package tawsor.ka.core

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Context shared across pipeline steps.
 * Stores runtime variables, artifacts, secrets, and execution metrics.
 */
class PipelineContext {
    private val data = ConcurrentHashMap<String, Any>()
    private val secrets = CopyOnWriteArrayList<String>()

    operator fun set(key: String, value: Any) {
        data[key] = value
    }

    @Suppress("UNCHECKED_CAST")
    operator fun <T> get(key: String): T? {
        return data[key] as? T
    }

    fun setSecret(key: String, secret: String) {
        data[key] = secret
        if (secret.isNotBlank()) {
            secrets.add(secret)
        }
    }

    fun require(key: String): Any {
        return data[key] ?: throw IllegalArgumentException("Missing required context key: '$key'")
    }

    fun contains(key: String): Boolean = data.containsKey(key)

    fun log(message: String) {
        var masked = message
        for (secret in secrets) {
            masked = masked.replace(secret, "******")
        }
        println("[KA] $masked")
    }

    fun logInfo(message: String) = log("\u001B[34m[INFO] $message\u001B[0m")
    fun logSuccess(message: String) = log("\u001B[32m[SUCCESS] ✔ $message\u001B[0m")
    fun logWarn(message: String) = log("\u001B[33m[WARN] ⚠ $message\u001B[0m")
    fun logError(message: String) = log("\u001B[31m[ERROR] ✖ $message\u001B[0m")
}
