package me.kuku.common.logic

import com.openai.client.OpenAIClientAsync
import com.openai.client.okhttp.OpenAIOkHttpClientAsync
import com.openai.models.chat.completions.ChatCompletionContentPart
import com.openai.models.chat.completions.ChatCompletionContentPartImage
import com.openai.models.chat.completions.ChatCompletionContentPartText
import com.openai.models.chat.completions.ChatCompletionCreateParams
import com.openai.models.chat.completions.ChatCompletionUserMessageParam
import kotlinx.coroutines.future.await
import me.kuku.common.utils.Cache
import me.kuku.common.utils.CacheManager
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

    private val client = OpenAIOkHttpClientAsync.fromEnv()
    private val model by lazy { System.getenv("OPENAI_MODEL") }

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

    fun build(key: String, text: String, photoList: List<String>, systemMessage: String? = null): OpenaiPojo {

        var cacheBody = cache[key]

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
        val pojo = build(key, text, photoList, systemMessage)
        val cacheBody = pojo.cacheBody
        val chatCompletion = client.chat().completions().create(pojo.chatCompletionCreateParams).await()
        val openaiText = chatCompletion.choices()[0].message().content().orElse("")
        cache[key] = cacheBody.toBuilder().addAssistantMessage(openaiText).build()
        val usage = chatCompletion.usage().orElseThrow()
        val model = chatCompletion.model()
        val prefix = "model: $model\npromptToken: ${usage.promptTokens()}\ncompletionToken: ${usage.completionTokens()}\n"
        return "$prefix\n$openaiText"
    }

}