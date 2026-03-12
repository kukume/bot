package me.kuku.qqbot.controller

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.logging.*
import me.kuku.common.utils.convertValue
import me.kuku.qqbot.context.Subscribe
import me.kuku.qqbot.event.Event
import me.kuku.qqbot.event.GroupMessage
import me.kuku.qqbot.event.PrivateMessage
import org.bouncycastle.crypto.CipherParameters
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer
import org.bouncycastle.util.encoders.Hex
import org.koin.core.qualifier.named
import org.koin.ktor.ext.inject
import java.nio.charset.StandardCharsets

private val subscribe by lazy {
    Subscribe().also { it.init() }
}

@Suppress("DuplicatedCode")
fun Application.hook() {

    val qqConfig by inject<ApplicationConfig>(named("qqConfig"))

    routing {
        get {
            call.respondText("hello world")
        }

        post("webhook") {
            val botSecret = qqConfig.property("secret").getString()
            val jsonNode = call.receive<JsonNode>()
            log.info("receive event: $jsonNode")
            val cipherParameters = validateRequest(botSecret, call)
            val op = jsonNode["op"].asInt()
            when (op) {
                13 -> verifyRequest(cipherParameters, call, jsonNode)
                0 -> {
                    call.respondText("ok")
                    val t = jsonNode["t"].asText()
                    when (t) {
                        "GROUP_AT_MESSAGE_CREATE" -> {
                            val event = jsonNode.convertValue<Event<GroupMessage>>()
                            val d = event.d
                            val content = d.content
                            val command = content.trim().substringBefore(" ").replace("/", "")
                            val match1 = subscribe.messageContext.map[command]
                            if (match1 != null) {
                                kotlin.runCatching {
                                    match1.invoke(d)
                                }.onFailure {
                                    log.error(it)
                                    d.sendMessage("程序抛出了异常，异常信息：${it.message}", msgSeq = 0)
                                }
                            } else {
                                subscribe.messageContext.map[""]?.invoke(d)
                            }
                            val match2 = subscribe.groupMessageContext.map[command]
                            if (match2 != null) {
                                kotlin.runCatching {
                                    match2.invoke(d)
                                }.onFailure {
                                    log.error(it)
                                    d.sendMessage("程序抛出了异常，异常信息：${it.message}", msgSeq = 0)
                                }
                            } else {
                                subscribe.groupMessageContext.map[""]?.invoke(d)
                            }
                        }
                        "C2C_MESSAGE_CREATE" -> {
                            val event = jsonNode.convertValue<Event<PrivateMessage>>()
                            val d = event.d
                            val content = d.content
                            val command = content.substringBefore(" ").replace("/", "")
                            val match1 = subscribe.messageContext.map[command]
                            if (match1 != null) {
                                kotlin.runCatching {
                                    match1.invoke(d)
                                }.onFailure {
                                    log.error(it)
                                    d.sendMessage("程序抛出了异常，异常信息：${it.message}", msgSeq = 0)
                                }
                            } else {
                                subscribe.messageContext.map[""]?.invoke(d)
                            }
                            val match2 = subscribe.privateMessageContext.map[command]
                            if (match2 != null) {
                                kotlin.runCatching {
                                    match2.invoke(d)
                                }.onFailure {
                                    log.error(it)
                                    d.sendMessage("程序抛出了异常，异常信息：${it.message}", msgSeq = 0)
                                }
                            } else {
                                subscribe.privateMessageContext.map[""]?.invoke(d)
                            }
                        }
                    }
                }
            }
        }

    }
}


private suspend fun validateRequest(botSecret: String, call: ApplicationCall): CipherParameters {
    // Generate seed
    var seed = botSecret.toByteArray(StandardCharsets.UTF_8)
    while (seed.size < 32) { // ed25519 seed size is 32 bytes
        seed += seed
    }
    val finalSeed = seed.copyOf(32)

    // Generate Ed25519 key pair using BouncyCastle
    val privateKeyParams = Ed25519PrivateKeyParameters(finalSeed, 0)
    val publicKeyParams = privateKeyParams.generatePublicKey()

    // Get X-Signature-Ed25519 from HTTP header
    val signatureHeader = call.request.header("X-Signature-Ed25519") ?: error("no auth")
    val signature = try {
        Hex.decode(signatureHeader)
    } catch (e: IllegalArgumentException) {
        error("no auth")
    }

    if (signature.size != 64 || signature[63].toInt() and 224 != 0) {
        error("no auth")
    }

    // Get X-Signature-Timestamp from HTTP header
    val timestamp = call.request.header("X-Signature-Timestamp") ?: error("no auth")

    // Read HTTP body
    val body = call.receiveText()

    // Concatenate timestamp and body
    val message = (timestamp + body).toByteArray(StandardCharsets.UTF_8)

    // Verify signature using BouncyCastle
    val verifier = Ed25519Signer()
    verifier.init(false, publicKeyParams)
    verifier.update(message, 0, message.size)
    val check = verifier.verifySignature(signature)
    if (!check) error("no auth")
    return privateKeyParams
}

private suspend fun verifyRequest(privateKeyParams: CipherParameters, call: ApplicationCall, jsonNode: JsonNode) {
    // 创建签名器并初始化为签名模式
    val signer = Ed25519Signer()
    signer.init(true, privateKeyParams)

    val plainToken = jsonNode["d"]["plain_token"].asText()
    val eventTs = jsonNode["d"]["event_ts"].asText()
    // 提供需要签名的消息数据
    val messageBytes = (eventTs + plainToken).toByteArray(StandardCharsets.UTF_8)
    signer.update(messageBytes, 0, messageBytes.size)

    // 生成签名
    val generateSignature = signer.generateSignature()

    // 将签名转换为十六进制字符串返回
    val generateSignatureHex =  Hex.toHexString(generateSignature)

    call.respond(mapOf("plain_token" to plainToken, "signature" to generateSignatureHex))
}