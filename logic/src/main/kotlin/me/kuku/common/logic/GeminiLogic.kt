package me.kuku.common.logic

import com.google.genai.AsyncChat
import com.google.genai.Client
import com.google.genai.types.Content
import com.google.genai.types.GenerateContentConfig
import com.google.genai.types.Part
import io.ktor.util.decodeBase64Bytes
import kotlinx.coroutines.future.await
import me.kuku.common.utils.CacheManager
import java.io.Closeable
import kotlin.io.encoding.Base64
import kotlin.jvm.optionals.getOrNull
import kotlin.time.Duration.Companion.minutes

data class GeminiCache(
    val client: Client,
    var chat: AsyncChat
): Closeable {
    override fun close() {
        client.close()
    }
}

object GeminiLogic {

    private val cache = CacheManager.getCache<String, GeminiCache>("gemini-chat-context", 2.minutes)
    private val key by lazy {
        System.getenv("GOOGLE_API_KEY")
    }
    private val model by lazy {
        System.getenv("GOOGLE_API_MODEL")
    }

    suspend fun gemini(key: String, text: String, photoList: List<String>, systemMessage: String? = null): AiResponse {
        var geminiCache = cache[key]
        if (geminiCache == null) {
            val client = Client.builder().apiKey(this.key).build()
            val chat = client.async.chats.create(model)
            geminiCache = GeminiCache(client, chat)
            cache[key] = geminiCache
        }
        val chat = geminiCache.chat
        val partList = mutableListOf<Part>()
        partList.add(Part.fromText(text))
        for (photo in photoList.map { Base64.decode(it) }) {
            partList.add(Part.fromBytes(photo, "image/jpeg"))
        }
        val content = Content.fromParts(*partList.toTypedArray())
        val responseFuture = chat.sendMessage(content,
            GenerateContentConfig.builder().responseModalities("TEXT").also {
                systemMessage?.let { s ->
                    it.systemInstruction(Content.fromParts(Part.fromText(s)))
                }
            }.build())
        val response = responseFuture.await()
        val model = response.modelVersion().getOrNull()
        val promptToken = response.usageMetadata().getOrNull()?.promptTokenCount()?.getOrNull()
        val totalToken = response.usageMetadata().getOrNull()?.candidatesTokenCount()?.getOrNull()
        val sb = StringBuilder("model: $model\npromptToken: ${promptToken}\ncompletionToken: ${totalToken}\n\n")
        val byteArrays = mutableListOf<ByteArray>()
        response.parts()?.forEach { part ->
            part.text().ifPresent { sb.append(it) }
            part.inlineData().ifPresent { it.data().ifPresent { ba -> byteArrays.add(ba)  } }
        }
        return AiResponse(sb.toString(), byteArrays)
    }

}

data class AiResponse(val text: String, val images: List<ByteArray>)
