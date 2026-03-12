package me.kuku.qqbot.subscribe

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.util.*
import me.kuku.common.ktor.client
import me.kuku.common.logic.OpenaiLogic
import me.kuku.common.logic.ToolLogic
import me.kuku.qqbot.context.Subscribe
import kotlin.io.encoding.Base64

fun Subscribe.openai() {

    group {
        handler("") {
            val photoList = mutableListOf<String>()
            attachments?.filter { it.contentType.startsWith("image") }?.forEach {
                val base64 = client.get(it.url).bodyAsBytes().encodeBase64()
                photoList.add(base64)
            }
            val text = OpenaiLogic.openai("group:$groupId:${author.unionOpenid}", content, photoList)
            if (text.contains(".")) {
                val bytes = ToolLogic.renderMarkdown1(text)
                sendImage(fileData = Base64.encode(bytes))
            } else {
                sendMessage(text)
            }
        }
    }

}
