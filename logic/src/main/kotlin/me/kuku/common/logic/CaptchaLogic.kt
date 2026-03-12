package me.kuku.common.logic

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.delay
import me.kuku.common.ktor.client
import me.kuku.common.ktor.setJsonBody
import me.kuku.common.utils.Jackson
import me.kuku.common.utils.toJsonNode
import kotlin.time.Duration.Companion.seconds

object TwoCaptchaLogic  {

    private val key by lazy { System.getenv("TWOCAPTCHA_KEY") }

    private suspend inline fun <reified T> captcha(task: Map<String, Any>): T {
        val paramNode = Jackson.createObjectNode()
        paramNode.put("clientKey", key)
        paramNode.putPOJO("task", task)
        val jsonNode = client.post("https://api.2captcha.com/createTask") {
            setJsonBody(paramNode)
        }.body<JsonNode>()
        if (jsonNode["errorId"].asInt() != 0) error("识别验证码失败：" + jsonNode["errorDescription"].asText())
        val code = jsonNode["taskId"].asLong()
        var i = 0
        while (true) {
            if (i++ > 35) error("无法识别验证码")
            delay(2000)
            val resultJsonNode = client.post("https://api.2captcha.com/getTaskResult") {
                setJsonBody("""
                    {
                       "clientKey": "$key", 
                       "taskId": $code
                    }
                """.trimIndent())
            }.body<JsonNode>()
            val resultCode = resultJsonNode["errorId"].asInt()
            if (resultCode == 0) {
                val status = resultJsonNode["status"].asText()
                if (status == "processing") continue
                else return Jackson.convertValue(resultJsonNode["solution"])
            } else {
                error("识别验证码失败：" + resultJsonNode["errorDescription"].asText())
            }
        }
    }

    suspend fun geeTest(gt: String, challenge: String, pageUrl: String): GeeTest {
        return captcha<GeeTest>(mapOf("type" to "GeeTestTaskProxyless", "gt" to gt, "challenge" to challenge, "websiteURL" to pageUrl))
    }

    suspend fun geeTestV4(key: String, captchaId: String, pageUrl: String, extraParams: Map<String, String> = mapOf()): GeeTestV4 {
        return captcha(mapOf("type" to "GeeTestTaskProxyless", "captcha_id" to captchaId, "websiteURL" to pageUrl, "version" to 4,
                "initParameters" to mutableMapOf("captcha_id" to captchaId).also { it.putAll(extraParams) }
            ))
    }

}

object DaMaGouLogic {

    private val key by lazy { System.getenv("DAMAGOU_KEY") }

    suspend fun geeTest(gt: String, challenge: String, pageUrl: String): GeeTest {
        val jsonNode = client.get("http://api.damagou.top/apiv1/jiyanRecognize.html") {
            url {
                parameters.append("userkey", key)
                parameters.append("gt", gt)
                parameters.append("challenge", challenge)
                parameters.append("isJson", "2")
                parameters.append("headers", "referer|$pageUrl")
            }
        }.bodyAsText().toJsonNode()
        if (jsonNode["status"].asInt() == 0) {
            val data = jsonNode["data"].asText()
            val split = data.split("|")
            return GeeTest(split[0], split[1], "${split[1]}|jordan")
        } else error(jsonNode["msg"].asText())
    }

    suspend fun geeTestV4(captchaId: String, riskType: String, challenge: String, referer: String): GeeTestV4 {
        val jsonNode = client.get("http://api.damagou.top/apiv1/jiyan4Recognize.html") {
            url {
                parameters.append("userkey", key)
                parameters.append("captchaId", captchaId)
                parameters.append("riskType", riskType)
                parameters.append("referer", referer)
                parameters.append("challenge", challenge)
                parameters.append("isJson", "2")
            }
        }.bodyAsText().toJsonNode()
        if (jsonNode["status"].asInt() == 0) {
            val data = jsonNode["data"].asText()
            val split = data.split("|")
            return GeeTestV4(split[0], split[2], split[1], split[3])
        } else error(jsonNode["msg"].asText())
    }

}

data class GeeTest(@param: JsonProperty("challenge") val challenge: String,
                   @param: JsonProperty("validate") val validate: String,
                   @param: JsonProperty("seccode") val secCode: String)


data class GeeTestV4(@param: JsonProperty("lot_number") val lotNumber: String,
                     @param: JsonProperty("gen_time") val genTime: String,
                     @param: JsonProperty("pass_token") val passToken: String,
                     @param: JsonProperty("captcha_output") val captchaOutput: String
)


object DeathByCaptchaLogic {

    private val username by lazy { System.getenv("DEATHBYCAPTCHA_ACCOUNT") }
    private val password by lazy { System.getenv("DEATHBYCAPTCHA_PASSWORD") }


    private suspend fun request(type: Int, paramName: String, params: Map<String, Any>): JsonNode {
        val jsonNode = client.submitForm("http://api.dbcapi.me/api/captcha", parameters {
            append("username", username)
            append("password", password)
            append("type", type.toString())
            append(paramName, Jackson.writeValueAsString(params))
        }) {
            accept(ContentType.Application.Json)
        }.bodyAsText().toJsonNode()
        val status = jsonNode["status"].asInt()
        if (status != 0) error(jsonNode["error"])
        val captchaId = jsonNode["captcha"].asInt()
        while (true) {
            delay(2.seconds)
            val jsonNode0 = client.get("http://api.dbcapi.me/api/captcha/$captchaId") {
                accept(ContentType.Application.Json)
            }.bodyAsText().toJsonNode()
            val status0 = jsonNode0["status"].asInt()
            if (status0 == 0) {
                val text = jsonNode0["text"].asText()
                if (text.isEmpty()) continue
                return text.toJsonNode()
            } else {
                error("")
            }
        }
    }

    suspend fun tencent(appid: Int, pageUrl: String): TencentCaptcha {
        val jsonNode = request(23, "tencent_params",
            mapOf("appid" to appid.toString(), "pageurl" to pageUrl))
        return TencentCaptcha(jsonNode["ticket"].asText(), jsonNode["randstr"].asText())
    }

}

data class TencentCaptcha(val ticket: String, val randStr: String)