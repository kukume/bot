package me.kuku.qqbot.api

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import io.ktor.http.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.kuku.qqbot.ktor.execute
import me.kuku.qqbot.ktor.qqClient
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

open class MessageResponse : BaseResponse()

data class GroupMessageRequest(
    @JsonIgnore
    val openid: String,
    val content: String? = null,
    // 消息类型：0 是文本，2 是 markdown， 3 ark，4 embed，7 media 富媒体
    @field:JsonProperty("msg_type")
    val msgType: Int = 0,
    @field:JsonProperty("event_id")
    val eventId: String? = null,
    @field:JsonProperty("msg_id")
    val msgId: String? = null,
    @field:JsonProperty("msg_seq")
    val msgSeq: Int? = null,
    val markdown: Markdown? = null,
    val ark: Ark? = null,
    val media: Media? = null
): BaseRequest<GroupMessageResponse>("/v2/groups/$openid/messages")

data class GroupMessageResponse(
    val id: String,
    val timestamp: String,
    var openid: String? = null
): MessageResponse() {

    @OptIn(DelicateCoroutinesApi::class)
    fun delete(delay: Duration = 0.seconds) {
        GlobalScope.launch {
            delay(delay)
            val request = GroupDeleteMessageRequest(openid ?: error("not found openid"), id)
            qqClient.execute(request)
        }
    }

}


data class PrivateMessageRequest(
    @JsonIgnore
    val openid: String,
    val content: String? = null,
    @field:JsonProperty("msg_type")
    val msgType: Int = 0,
    @field:JsonProperty("event_id")
    val eventId: String? = null,
    @field:JsonProperty("msg_id")
    val msgId: String? = null,
    @field:JsonProperty("msg_seq")
    val msgSeq: Int? = null,
    val markdown: Markdown? = null,
    val ark: Ark? = null,
    val media: Media? = null
): BaseRequest<PrivateMessageResponse>("/v2/users/$openid/messages")

data class PrivateMessageResponse(
    val id: String,
    val timestamp: String,
    var openid: String? = null
): MessageResponse() {

    @OptIn(DelicateCoroutinesApi::class)
    fun delete(delay: Duration = 0.seconds) {
        GlobalScope.launch {
            val request = PrivateDeleteMessageRequest(openid ?: error("not found openid"), id)
            qqClient.execute(request)
        }
    }

}

data class PrivateDeleteMessageRequest(
    @JsonIgnore
    val openid: String,
    @JsonIgnore
    val messageId: String
): BaseRequest<PrivateDeleteMessageResponse>("/v2/users/$openid/messages/$messageId", HttpMethod.Delete)

class PrivateDeleteMessageResponse: BaseResponse()

data class GroupDeleteMessageRequest(
    @JsonIgnore
    val openid: String,
    @JsonIgnore
    val messageId: String
): BaseRequest<GroupDeleteMessageResponse>("/v2/groups/$openid/messages/$messageId", HttpMethod.Delete)

class GroupDeleteMessageResponse: BaseResponse()