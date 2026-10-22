package tawsor.ka.http

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import tawsor.ka.core.PipelineContext

object Notifications {
    suspend fun sendTelegram(botToken: String, chatId: String, message: String): Boolean {
        val url = "https://api.telegram.org/bot$botToken/sendMessage"
        val payload = buildJsonObject {
            put("chat_id", chatId)
            put("text", message)
            put("parse_mode", "Markdown")
        }.toString()

        val response = KaHttp.client.post(url) {
            contentType(ContentType.Application.Json)
            setBody(payload)
        }
        return response.status.isSuccess()
    }

    suspend fun sendDiscord(webhookUrl: String, content: String, username: String = "Kotlin Automation"): Boolean {
        val payload = buildJsonObject {
            put("content", content)
            put("username", username)
        }.toString()

        val response = KaHttp.client.post(webhookUrl) {
            contentType(ContentType.Application.Json)
            setBody(payload)
        }
        return response.status.isSuccess()
    }
}

suspend fun PipelineContext.telegramNotify(botToken: String, chatId: String, message: String): Boolean {
    logInfo("Sending Telegram notification to chat: $chatId...")
    return try {
        val ok = Notifications.sendTelegram(botToken, chatId, message)
        if (ok) logSuccess("Telegram alert sent successfully") else logWarn("Telegram alert failed")
        ok
    } catch (e: Exception) {
        logError("Failed to deliver Telegram notification: ${e.message}")
        false
    }
}

suspend fun PipelineContext.discordNotify(webhookUrl: String, content: String, username: String = "Kotlin Automation"): Boolean {
    logInfo("Sending Discord webhook notification...")
    return try {
        val ok = Notifications.sendDiscord(webhookUrl, content, username)
        if (ok) logSuccess("Discord alert sent successfully") else logWarn("Discord alert failed")
        ok
    } catch (e: Exception) {
        logError("Failed to deliver Discord notification: ${e.message}")
        false
    }
}
