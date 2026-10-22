package tawsor.ka.http

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import tawsor.ka.core.PipelineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

object KaHttp {
    val client: HttpClient by lazy {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                })
            }
        }
    }
}

suspend fun PipelineContext.httpGet(url: String): String {
    log("HTTP GET: $url")
    val response = KaHttp.client.get(url)
    return response.bodyAsText()
}

suspend fun PipelineContext.httpPost(url: String, jsonBody: String): String {
    log("HTTP POST: $url")
    val response = KaHttp.client.post(url) {
        contentType(ContentType.Application.Json)
        setBody(jsonBody)
    }
    return response.bodyAsText()
}

suspend fun PipelineContext.sendWebhook(url: String, payload: String) {
    log("Triggering Webhook: $url")
    val response = KaHttp.client.post(url) {
        contentType(ContentType.Application.Json)
        setBody(payload)
    }
    log("Webhook response status: ${response.status}")
}

/**
 * Polls an endpoint or condition until it returns true or times out.
 */
suspend fun PipelineContext.pollUntil(
    timeout: Duration = 30.seconds,
    interval: Duration = 1000.milliseconds,
    description: String = "condition",
    condition: suspend () -> Boolean
): Boolean {
    val deadline = System.currentTimeMillis() + timeout.inWholeMilliseconds
    log("Polling until '$description' (timeout: $timeout)...")

    while (System.currentTimeMillis() < deadline) {
        try {
            if (condition()) {
                log("Polling condition '$description' met!")
                return true
            }
        } catch (e: Exception) {
            log("Polling check failed: ${e.message}")
        }
        delay(interval)
    }

    throw IllegalStateException("Polling timed out after $timeout for '$description'")
}

