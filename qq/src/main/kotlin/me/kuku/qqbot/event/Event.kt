package me.kuku.qqbot.event

import com.fasterxml.jackson.annotation.JsonProperty

data class Event<T>(
    val op: Int,
    val id: String,
    val d: T,
    val t: String
)

open class Author(
    open val id: String
)

data class GroupAuthor(
    override val id: String,
    @field:JsonProperty("member_openid")
    val memberOpenid: String,
    @field:JsonProperty("union_openid")
    val unionOpenid: String
): Author(id)

data class PrivateAuthor(
    override val id: String,
    @field:JsonProperty("user_openid")
    val userOpenid: String,
    @field:JsonProperty("union_openid")
    val unionOpenid: String
): Author(id)

data class MessageScene(
    val source: String,
    @field:JsonProperty("callback_data")
    val callbackData: String?
)