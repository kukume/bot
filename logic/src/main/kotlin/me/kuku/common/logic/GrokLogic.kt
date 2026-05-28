package me.kuku.common.logic

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.delay
import me.kuku.common.ktor.client
import me.kuku.common.utils.toJsonNode
import kotlin.time.Duration.Companion.seconds

object GrokLogic {

    private val apiKey by lazy { System.getenv("OPENAI_API_KEY") ?: error("OPENAI_API_KEY is not set") }
    private val baseUrl by lazy { System.getenv("OPENAI_BASE_URL")?.trimEnd('/') ?: "https://api.openai.com/v1" }
    private val model by lazy { System.getenv("OPENAI_VIDEO_MODEL") ?: "grok-imagine-video" }

    suspend fun video(prompt: String, model: String = GrokLogic.model): String {
        val createResponse = client.post("$baseUrl/videos") {
            bearerAuth(apiKey)
            contentType(ContentType.Application.Json)
            setBody(mapOf("model" to model, "prompt" to prompt))
        }.bodyAsText().toJsonNode()

        val id = createResponse["id"]?.asText() ?: error("video id not found: $createResponse")
        var video = createResponse

        repeat(120) {
            when (video["status"]?.asText()) {
                "completed", "succeeded" -> return "$baseUrl/videos/$id/content"
                "done" -> return video["video"]?.get("url")?.asText()
                    ?: error("video url not found: $video")
                "failed", "cancelled", "canceled" -> error(video["error"]?.asText() ?: "video generation failed: $video")
            }

            delay(5.seconds)
            video = client.get("$baseUrl/videos/$id") {
                bearerAuth(apiKey)
            }.bodyAsText().toJsonNode()
        }

        error("video generation timeout: $video")
    }

}
