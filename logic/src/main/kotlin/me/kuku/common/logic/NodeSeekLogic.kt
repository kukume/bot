package me.kuku.common.logic

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import me.kuku.common.entity.NodeSeekEntity
import me.kuku.common.ktor.client
import me.kuku.common.ktor.cookieString
import me.kuku.common.ktor.origin
import me.kuku.common.ktor.referer
import me.kuku.common.utils.toJsonNode
import me.kuku.common.utils.toUrlEncode

object NodeSeekLogic {

    private const val api = "https://api.jpa.cc"

    suspend fun sign(entity: NodeSeekEntity, random: Boolean = false) {
        client.get("$api/nodeseek/sign?cookie=${entity.cookie.toUrlEncode()}&random=$random")
            .bodyAsText()
    }

    suspend fun sign0(entity: NodeSeekEntity, random: Boolean = false): Int {
        val response = client.post("https://www.nodeseek.com/api/attendance?random=$random") {
            headers {
                cookieString(entity.cookie)
                origin("https://www.nodeseek.com")
                referer("https://www.nodeseek.com/board")
                userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/133.0.0.0 Safari/537.36")
            }
        }
        if (response.status == HttpStatusCode.Forbidden) error("未通过cloudflare验证，请上传浏览器在NodeSeek上的全部cookie")
        val jsonNode = response.body<JsonNode>()
        if (jsonNode["success"].asBoolean()) return jsonNode["gain"].asInt()
        else error(jsonNode["message"].asText())
    }

    suspend fun querySign(entity: NodeSeekEntity): Int {
        val jsonNode = client.get("$api/nodeseek/sign/query?cookie=${entity.cookie.toUrlEncode()}")
            .body<JsonNode>()
        // gain current
        if (!(jsonNode["success"]?.asBoolean() ?: error("未获取到NodeSeek签到执行结果"))) error(jsonNode["message"].asText())
        return jsonNode["gain"].asInt()
    }

    suspend fun login(username: String, password: String, token: String? = null): String {
        val jsonNode = client.submitForm("$api/nodeseek/login",
            parameters {
                append("username", username)
                append("password", password)
                token?.let {
                    append("token", token)
                }
            }) {
        }.bodyAsText().toJsonNode()
        if (jsonNode.has("cookie")) return jsonNode["cookie"].asText()
        else error(jsonNode["message"].asText())
    }

}