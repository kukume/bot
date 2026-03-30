package me.kuku.common.logic

import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.models.messages.Base64ImageSource
import com.anthropic.models.messages.ContentBlockParam
import com.anthropic.models.messages.ImageBlockParam
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.MessageParam
import com.anthropic.models.messages.TextBlockParam
import com.anthropic.models.messages.ToolUnion
import com.anthropic.models.messages.WebSearchTool20250305
import me.kuku.common.utils.CacheManager
import java.util.Base64
import kotlin.time.Duration.Companion.minutes

object AnthropicLogic {

    private val cache = CacheManager.getCache<String, MutableList<MessageParam>>("anthropic-chat-context", 5.minutes)

    // ANTHROPIC_BASE_URL
    private val client = AnthropicOkHttpClient.fromEnv()

    fun claude(key: String, text: String, photoList: List<String>, systemMessage: String? = null): String {
        val messageCache = cache[key] ?: mutableListOf<MessageParam>().also { cache[key] = it }

        messageCache.add(MessageParam.builder().role(MessageParam.Role.USER).content(MessageParam.Content.ofBlockParams(mutableListOf(
            ContentBlockParam.ofText(TextBlockParam.builder().text(text).build())
        ).also {
            for (base64 in photoList) {
                it.add(ContentBlockParam.ofImage(ImageBlockParam.builder().source(Base64ImageSource.builder().mediaType(detectBase64ImageFormat(base64)).data(base64).build()).build()))
            }
        })).build())

        val messageCreateParams = MessageCreateParams.builder()
            .maxTokens(10000)
            .also { build ->
                systemMessage?.let { build.system(it) }
            }
            .model(System.getenv("ANTHROPIC_MODEL"))
            .messages(messageCache)
            .also {
                if (text.contains("(http|联网)".toRegex())) {
                    it.tools(listOf(ToolUnion.ofWebSearchTool20250305(WebSearchTool20250305.builder().build())))
                }
            }
            .build()

        val message = client.messages().create(messageCreateParams)
        val usage = message.usage()
        val inputTokens = usage.inputTokens()
        val outputTokens = usage.outputTokens()
        val model = message.model().asString()
        val prefix = "model: $model\ninputTokens: ${inputTokens}\noutputTokens: ${outputTokens}\n"
        val content = message.content()
        val sb = StringBuilder()
        content.forEach {
            it.text().ifPresent { textBlock -> sb.append(textBlock.text()) }
        }
        val resultText = sb.toString()
        messageCache.add(MessageParam.builder().role(MessageParam.Role.ASSISTANT).content(resultText).build())
        return "$prefix\n$resultText"
    }

    private fun detectBase64ImageFormat(base64: String): Base64ImageSource.MediaType {
        val pureBase64 = base64.substringAfter(",", base64).trim()

        val bytes = try {
            // 使用 MIME 解码器，容忍换行、空格等
            Base64.getMimeDecoder().decode(pureBase64)
        } catch (_: IllegalArgumentException) {
            error("unrecognizable image")
        }

        if (bytes.isEmpty()) error("unrecognizable image")

        // JPEG: FF D8 (通常后面紧跟 FF)
        if (bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte()) {
            return Base64ImageSource.MediaType.IMAGE_JPEG
        }

        // PNG: 89 50 4E 47 0D 0A 1A 0A
        if (bytes.size >= 8 &&
            bytes[0] == 0x89.toByte() &&
            bytes[1] == 0x50.toByte() &&
            bytes[2] == 0x4E.toByte() &&
            bytes[3] == 0x47.toByte() &&
            bytes[4] == 0x0D.toByte() &&
            bytes[5] == 0x0A.toByte() &&
            bytes[6] == 0x1A.toByte() &&
            bytes[7] == 0x0A.toByte()
        ) {
            return Base64ImageSource.MediaType.IMAGE_PNG
        }

        // GIF: "GIF87a" 或 "GIF89a"
        if (bytes.size >= 6) {
            val header6 = String(bytes, 0, 6, Charsets.US_ASCII)
            if (header6 == "GIF87a" || header6 == "GIF89a") {
                return Base64ImageSource.MediaType.IMAGE_GIF
            }
        }

        // WEBP: "RIFF"...."WEBP"
        if (bytes.size >= 12) {
            val riff = String(bytes, 0, 4, Charsets.US_ASCII)
            val webp = String(bytes, 8, 4, Charsets.US_ASCII)
            if (riff == "RIFF" && webp == "WEBP") {
                return Base64ImageSource.MediaType.IMAGE_WEBP
            }
        }

        error("unrecognizable image")
    }


}
