package me.kuku.telegram.handler

import io.github.dehuckakpyt.telegrambot.handler.BotHandler
import io.github.dehuckakpyt.telegrambot.model.telegram.PhotoSize
import io.github.dehuckakpyt.telegrambot.model.telegram.ReplyParameters
import io.ktor.client.statement.*
import io.ktor.server.config.*
import kotlinx.coroutines.runBlocking
import me.kuku.common.logic.OpenaiLogic
import org.koin.core.annotation.Factory
import org.koin.core.qualifier.named
import org.koin.mp.KoinPlatform.getKoin
import kotlin.io.encoding.Base64

@Factory
class OpenaiHandler: BotHandler({

    val openaiConfig by lazy {
        getKoin().get<ApplicationConfig>(named("openai"))
    }

    command("/chat") {

        val replyToMessage = message.replyToMessage
        val text: String
        val photoList = mutableListOf<String>()
        val photoSizeList: List<PhotoSize>?
        if (replyToMessage != null) {
            text = (replyToMessage.text ?: "") + message.text
            photoSizeList = replyToMessage.photo
        } else {
            text = this.text
            photoSizeList = message.photo
        }
        photoSizeList?.groupBy { it.fileUniqueId.dropLast(1) }?.mapNotNull { (_, group) -> group.maxByOrNull { it.fileSize!! } }
            ?.forEach { photoSize ->
                val getFile = getFile(photoSize.fileId)
                val base64 = Base64.encode(bot.client.getFileApi(getFile.filePath!!).bodyAsBytes())
                photoList.add(base64)
            }
        val key = chat.id.toString() + message.from?.id

        val pojo = OpenaiLogic.build(key, text, photoList)
        val client = pojo.client
        val params = pojo.chatCompletionCreateParams
        val cacheBody = pojo.cacheBody
        client.chat().completions().createStreaming(params).subscribe {
            runBlocking {
                val response = sendMessage("Processing\\.\\.\\.", parseMode = "MarkdownV2",
                    replyParameters = ReplyParameters(messageId, chat.id))
                val sendMessageId = response.messageId
                var openaiText = ""
                var prefix = ">model: ${it.model().replace("-", "\\-")}\n"
                var alreadySendText = ""
                var i = 5
                it.choices().getOrNull(0)?.delta()?.content()?.orElse("")?.let { content ->
                    openaiText += content
                }
                it.usage().orElseThrow().let { usage ->
                    prefix += ">promptToken: ${usage.promptTokens()}\n>completionToken: ${usage.completionTokens()}\n"
                }
                if (i++ % 20 == 0) {
                    val sendText = "$prefix\n\n```text\n${openaiText}```"
                    if (alreadySendText != sendText) {
                        alreadySendText = sendText
                        editMessageText(sendMessageId, sendText, parseMode = "MarkdownV2")
                    }
                }
            }
        }.onCompleteFuture().whenComplete { _, _ ->
//            val sendText = "$prefix\n\n```text\n${openaiText}\n```"
//            cacheBody.add(ChatMessage(ChatRole.Assistant, openaiText))
//            cache[key] = cacheBody
//            if (alreadySendText != sendText) {
//                alreadySendText = sendText
//                editMessageText(sendMessageId, sendText, parseMode = "MarkdownV2")
//            }
        }

    }

})
