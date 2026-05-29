package me.kuku.common.logic

import com.openai.client.OpenAIClientAsync
import com.openai.client.okhttp.OpenAIOkHttpClientAsync
import com.openai.core.MultipartField
import com.openai.models.chat.completions.ChatCompletionContentPart
import com.openai.models.chat.completions.ChatCompletionContentPartImage
import com.openai.models.chat.completions.ChatCompletionContentPartText
import com.openai.models.chat.completions.ChatCompletionCreateParams
import com.openai.models.chat.completions.ChatCompletionUserMessageParam
import com.openai.models.images.Image
import com.openai.models.images.ImageEditParams
import com.openai.models.images.ImageGenerateParams
import com.openai.models.responses.ResponseCreateParams
import com.openai.models.responses.WebSearchTool
import com.github.benmanes.caffeine.cache.Cache
import kotlinx.coroutines.future.await
import me.kuku.common.utils.CacheManager
import java.io.ByteArrayInputStream
import java.time.Duration
import java.util.Base64

data class OpenaiPojo(
    val client: OpenAIClientAsync,
    val cacheBody: ChatCompletionCreateParams,
    val cache: Cache<String, ChatCompletionCreateParams>,
    val chatCompletionCreateParams: ChatCompletionCreateParams
)

object OpenaiLogic {

    private val cache = CacheManager.getCache<String, ChatCompletionCreateParams>("gpt-chat-context", Duration.ofMinutes(2))

    private val client = OpenAIOkHttpClientAsync.builder()
        .fromEnv()
        .build()
    private val model by lazy { System.getenv("OPENAI_MODEL") ?: "grok-4.3" }
    private val webSearchTool by lazy {
        WebSearchTool.builder()
            .type(WebSearchTool.Type.of("web_search"))
            .build()
    }

    private fun detectImageTypeFromBase64(base64: String): String? {
        val pureBase64 = base64.substringAfter(",")
        val bytes = Base64.getDecoder().decode(pureBase64)

        return when {
            bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() -> "jpg"
            bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() -> "png"
            bytes[0] == 0x47.toByte() && bytes[1] == 0x49.toByte() -> "gif"
            bytes[0] == 0x42.toByte() && bytes[1] == 0x4D.toByte() -> "bmp"
            String(bytes.copyOfRange(0, 4)) == "RIFF" &&
                    String(bytes.copyOfRange(8, 12)) == "WEBP" -> "webp"
            else -> null
        }
    }

    private fun detectImageTypeFromBytes(bytes: ByteArray): String? {
        return when {
            bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() -> "jpg"
            bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() -> "png"
            bytes[0] == 0x47.toByte() && bytes[1] == 0x49.toByte() -> "gif"
            bytes[0] == 0x42.toByte() && bytes[1] == 0x4D.toByte() -> "bmp"
            String(bytes.copyOfRange(0, 4)) == "RIFF" &&
                    String(bytes.copyOfRange(8, 12)) == "WEBP" -> "webp"
            else -> null
        }
    }

    fun build(key: String, text: String, photoList: List<String>, systemMessage: String? = null): OpenaiPojo {

        var cacheBody = cache.getIfPresent(key)

        val fileList = mutableListOf<ChatCompletionContentPart>()

        for (photo in photoList) {
            val part = ChatCompletionContentPart.ofImageUrl(ChatCompletionContentPartImage.builder().imageUrl(
                ChatCompletionContentPartImage.ImageUrl.builder().url("data:image/${detectImageTypeFromBase64(photo)};base64,$photo").build()).build())
            fileList.add(part)
        }

        val chatCompletionCreateParams = (cacheBody?.toBuilder() ?: ChatCompletionCreateParams.builder().also {
            systemMessage?.let { m -> it.addSystemMessage(m) }
        }).model(model)
            .addUserMessage(ChatCompletionUserMessageParam.Content.ofArrayOfContentParts(mutableListOf(
                ChatCompletionContentPart.ofText(ChatCompletionContentPartText.builder().text(text).build())
            ).also { it.addAll(fileList) }))
            .build()
        cacheBody = chatCompletionCreateParams

        return OpenaiPojo(client, cacheBody, cache, chatCompletionCreateParams)

    }

    suspend fun openai(key: String, text: String, photoList: List<String>, systemMessage: String? = null): String {
        if (photoList.isEmpty()) return response(key, text, systemMessage)
        val pojo = build(key, text, photoList, systemMessage)
        val cacheBody = pojo.cacheBody
        val chatCompletion = client.chat().completions().create(pojo.chatCompletionCreateParams).await()
        val openaiText = chatCompletion.choices()[0].message().content().orElse("")
        cache.put(key, cacheBody.toBuilder().addAssistantMessage(openaiText).build())
        val usage = chatCompletion.usage().orElseThrow()
        val model = chatCompletion.model()
        val prefix = "model: $model\npromptToken: ${usage.promptTokens()}\ncompletionToken: ${usage.completionTokens()}\n"
        return "$prefix\n$openaiText"
    }

    private suspend fun response(key: String, text: String, systemMessage: String? = null): String {
        val cacheBody = cache.getIfPresent(key)
        val input = buildString {
            systemMessage?.let { append("system: ").append(it).append("\n\n") }
            cacheBody?.messages()?.forEach { append(it.toString()).append("\n") }
            append(text)
        }
        val params = ResponseCreateParams.builder()
            .model(model)
            .input(input)
            .addTool(webSearchTool)
            .build()
        val response = client.responses().create(params).await()
        val responseText = response.output().mapNotNull { output ->
            output.message().orElse(null)?.content()?.mapNotNull { content ->
                content.outputText().orElse(null)?.text()
            }?.joinToString("")
        }.joinToString("\n")
        cache.put(key, (cacheBody?.toBuilder() ?: ChatCompletionCreateParams.builder())
            .addUserMessage(text)
            .addAssistantMessage(responseText)
            .model(model)
            .build())
        val usage = response.usage().orElse(null)
        val prefix = "model: ${response.model().asString()}" + if (usage == null) "" else
            "\npromptToken: ${usage.inputTokens()}\ncompletionToken: ${usage.outputTokens()}"
        return "$prefix\n\n$responseText"
    }

    suspend fun image(prompt: String, model: String = System.getenv("OPENAI_IMAGE_MODEL") ?: "gpt-image-2"): Image {
        val params = ImageGenerateParams.builder()
            .prompt(prompt)
            .model(model)
            .build()
        val response = client.images().generate(params).await()
        return response.data().orElseThrow().first()
    }

    suspend fun image(prompt: String, image: ByteArray, model: String = System.getenv("OPENAI_IMAGE_MODEL") ?: "gpt-image-2"): Image {
        val imageType = detectImageTypeFromBytes(image) ?: "png"
        val contentType = if (imageType == "jpg") "image/jpeg" else "image/$imageType"
        val imageField = MultipartField.builder<ImageEditParams.Image>()
            .value(ImageEditParams.Image.ofInputStream(ByteArrayInputStream(image)))
            .filename("image.$imageType")
            .contentType(contentType)
            .build()
        val params = ImageEditParams.builder()
            .prompt(prompt)
            .image(imageField)
            .model(model)
            .build()
        val response = client.images().edit(params).await()
        return response.data().orElseThrow().first()
    }

}
