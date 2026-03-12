package me.kuku.common.logic

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.ContentType
import me.kuku.common.entity.NodeLocEntity
import me.kuku.common.ktor.client
import me.kuku.common.ktor.cookieString

object NodeLocLogic {

    suspend fun sign(entity: NodeLocEntity) {
        val jsonNode = client.post("https://www.nodeloc.com/checkin") {
            headers {
                accept(ContentType.Application.Json)
                cookieString(entity.cookie)
                append("x-csrf-token", entity.csrf)
            }
        }.body<JsonNode>()
        // {"success":true,"points":10,"user_date":"2025-05-13","timezone":"Asia/Shanghai"}
        if (jsonNode.has("errors")) {
            error(jsonNode["errors"][0].asText())
        }
    }


}
