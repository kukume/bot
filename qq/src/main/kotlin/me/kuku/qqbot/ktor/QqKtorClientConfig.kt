package me.kuku.qqbot.ktor

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.config.*
import me.kuku.common.utils.convertValue
import me.kuku.qqbot.api.BaseRequest
import me.kuku.qqbot.api.BaseResponse
import org.koin.core.qualifier.named
import org.koin.mp.KoinPlatform.getKoin

private lateinit var qqAuth: QqAuth

private val qqConfig by lazy {
    getKoin().get<ApplicationConfig>(named("qqConfig"))
}

val qqClient by lazy {
    val client = HttpClient(OkHttp) {
        engine {
            config {
                followRedirects(false)
            }
        }

        followRedirects = false

        install(ContentNegotiation) {
            jackson()
        }

        install(Logging) {
            sanitizeHeader { header -> header == HttpHeaders.Authorization }
        }

    }
    client.plugin(HttpSend).intercept { request ->
        if (request.url.buildString() == "https://bots.qq.com/app/getAppAccessToken") return@intercept execute(request)
        if (!::qqAuth.isInitialized || qqAuth.expire()) {
            val id = qqConfig.property("id").getString()
            val secret = qqConfig.property("secret").getString()
            val jsonNode = client.post("https://bots.qq.com/app/getAppAccessToken") {
                contentType(ContentType.Application.Json)
                setBody(
                    """
                    {
                      "appId": "$id",
                      "clientSecret": "$secret"
                    }
                """.trimIndent()
                )
            }.body<JsonNode>()
            qqAuth = QqAuth(jsonNode["access_token"].asText(), jsonNode["expires_in"].asLong())
        }
        request.header("Authorization", "QQBot ${qqAuth.accessToken}")
        execute(request)
    }
    client
}

private data class QqAuth(
    @field:JsonProperty("access_token") val accessToken: String,
    @field:JsonProperty("expires_in") val expiresIn: Long
) {
    private val expireTime = System.currentTimeMillis() + expiresIn * 1000

    fun expire() = System.currentTimeMillis() > expireTime
}

val objectMapper: ObjectMapper = ObjectMapper().registerModules(kotlinModule())

suspend inline fun <reified T: BaseResponse> HttpClient.execute(baseRequest: BaseRequest<T>): T {
    val response = request("https://api.sgroup.qq.com" + baseRequest.requestUrl) {
        method = baseRequest.method
        contentType(ContentType.Application.Json)
        setBody(objectMapper.writeValueAsString(baseRequest))
    }
    val jsonNode = response.body<JsonNode>()
    if (response.status == HttpStatusCode.OK)
        return jsonNode.convertValue()
    else error(jsonNode["message"].asText())
}