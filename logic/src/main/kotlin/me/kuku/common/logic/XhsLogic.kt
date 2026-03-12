package me.kuku.common.logic

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.NullNode
import io.ktor.client.call.body
import io.ktor.client.request.post
import me.kuku.common.ktor.client
import me.kuku.common.ktor.setJsonBody
import me.kuku.common.utils.convertValue
import java.time.LocalDateTime

object XhsLogic {

    suspend fun detail(url: String): XhsDetail {
        val jsonNode = client.post("http://localhost:5556/xhs/detail") {
            setJsonBody("""
                {"url": "$url"}
            """.trimIndent())
        }.body<JsonNode>()
        val data = jsonNode["data"]
        if (data is NullNode) error("未获取到数据")
        return data.convertValue()
    }

}

data class XhsDetail(
    @field:JsonProperty("作品ID")
    val id: String,
    @field:JsonProperty("作品标题")
    val title: String,
    @field:JsonProperty("作品描述")
    val description: String,
    @field:JsonProperty("发布时间")
    @field:JsonFormat(pattern = "yyyy-MM-dd_HH:mm:ss")
    val pushTime: LocalDateTime,
    @field:JsonProperty("最后更新时间")
    @field:JsonFormat(pattern = "yyyy-MM-dd_HH:mm:ss")
    val updateTime: LocalDateTime,
    @field:JsonProperty("作者昵称")
    val username: String,
    @field:JsonProperty("作者ID")
    val userid: String,
    @field:JsonProperty("下载地址")
    val downloadUrls: List<String>

)