package tawsor.ka.core

import java.util.concurrent.ConcurrentHashMap

/**
 * Context shared across pipeline steps.
 * Stores runtime variables, artifacts, and execution metrics.
 */
class PipelineContext {
    private val data = ConcurrentHashMap<String, Any>()

    operator fun set(key: String, value: Any) {
        data[key] = value
    }

    @Suppress("UNCHECKED_CAST")
    operator fun <T> get(key: String): T? {
        return data[key] as? T
    }

    fun require(key: String): Any {
        return data[key] ?: throw IllegalArgumentException("Missing required context key: '$key'")
    }

    fun contains(key: String): Boolean = data.containsKey(key)

    fun log(message: String) {
        println("[KA] $message")
    }
}

