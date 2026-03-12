package me.kuku.qqbot.event

import com.fasterxml.jackson.annotation.JsonProperty
import me.kuku.qqbot.api.*
import me.kuku.qqbot.ktor.execute
import me.kuku.qqbot.ktor.qqClient

abstract class Message(
    open val id: String,
    open val content: String,
    open val attachments: List<Attachment>?
) {

    fun arg() = content.trim().substringAfter(" ")

    fun arg(num: Int) = content.trim().split(" ").filter { it.isNotEmpty() }.getOrNull(num) ?: error("miss arg $num")

    abstract suspend fun sendMessage(content: String? = null, msgType: Int = 0, media: String? = null, msgSeq: Int = 1): MessageResponse

    abstract suspend fun sendMedia(fileType: Int, url: String? = null, fileData: String? = null, msgSeq: Int = 1, content: String? = null): MessageResponse

    abstract suspend fun sendImage(url: String? = null, fileData: String? = null, msgSeq: Int = 1, content: String? = null): MessageResponse

    abstract suspend fun sendVideo(url: String? = null, fileData: String? = null, msgSeq: Int = 1): MessageResponse

    abstract suspend fun file(fileType: Int, url: String? = null, fileData: String? = null): FileResponse

}

data class GroupMessage(
    override val id: String,
    override val content: String,
    val timestamp: String,
    val author: GroupAuthor,
    override val attachments: List<Attachment>?,
    @field:JsonProperty("group_id")
    val groupId: String,
    @field:JsonProperty("group_openid")
    val groupOpenid: String,
    @field:JsonProperty("message_scene")
    val messageScene: MessageScene
): Message(id, content, attachments) {

    override suspend fun sendMessage(content: String?, msgType: Int, media: String?, msgSeq: Int): GroupMessageResponse {
        val request = GroupMessageRequest(groupOpenid, content, msgType = msgType, msgId = id,
            media = media?.let { Media(it) }, msgSeq = msgSeq)
        return qqClient.execute(request).also { it.openid = groupOpenid }
    }

    override suspend fun sendMedia(fileType: Int, url: String?, fileData: String?, msgSeq: Int, content: String?): GroupMessageResponse {
        val groupFileResponse = file(fileType, url, fileData)
        return sendMessage(media = groupFileResponse.fileInfo, msgType = 7, msgSeq = msgSeq, content = content)
    }

    override suspend fun sendImage(url: String?, fileData: String?, msgSeq: Int, content: String?): GroupMessageResponse {
        return sendMedia(url = url, fileData = fileData, fileType = 1, msgSeq = msgSeq, content = content)
    }

    override suspend fun sendVideo(url: String?, fileData: String?, msgSeq: Int): GroupMessageResponse {
        return sendMedia(2, url, fileData, msgSeq)
    }

    override suspend fun file(fileType: Int, url: String?, fileData: String?): GroupFileResponse {
        val request = GroupFileRequest(groupOpenid, fileType, url, fileData = fileData)
        return qqClient.execute(request)
    }



}

data class PrivateMessage(
    override val id: String,
    override val content: String,
    val timestamp: String,
    val author: PrivateAuthor,
    override val attachments: List<Attachment>?,
    @field:JsonProperty("message_scene")
    val messageScene: MessageScene
): Message(id, content, attachments) {

    override suspend fun sendMessage(content: String?, msgType: Int, media: String?, msgSeq: Int): PrivateMessageResponse {
        val request = PrivateMessageRequest(author.userOpenid, content, msgId = id, msgType = msgType,
            media = media?.let { Media(it) }, msgSeq = msgSeq)
        return qqClient.execute(request).also { it.openid = author.userOpenid }
    }

    override suspend fun sendMedia(fileType: Int, url: String?, fileData: String?, msgSeq: Int, content: String?): PrivateMessageResponse {
        val privateFileResponse = file(fileType, url, fileData)
        return sendMessage(media = privateFileResponse.fileInfo, msgType = 7, msgSeq = msgSeq, content = content)
    }

    override suspend fun sendImage(url: String?, fileData: String?, msgSeq: Int, content: String?): PrivateMessageResponse {
        return sendMedia(1, url, fileData, msgSeq, content)
    }

    override suspend fun sendVideo(url: String?, fileData: String?, msgSeq: Int): PrivateMessageResponse {
        return sendMedia(2, url, fileData, msgSeq)
    }

    override suspend fun file(fileType: Int, url: String?, fileData: String?): PrivateFileResponse {
        val request = PrivateFileRequest(author.userOpenid, fileType, url, fileData = fileData)
        return qqClient.execute(request)
    }
}

data class Attachment(
    val url: String,
    val filename: String,
    val width: Int?,
    val height: Int?,
    val size: Int,
    @field:JsonProperty("content_type")
    val contentType: String,
    val content: String?
)