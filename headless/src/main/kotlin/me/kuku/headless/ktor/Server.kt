package me.kuku.headless.ktor

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.content.OutputStreamContent
import io.ktor.http.withCharsetIfNeeded
import io.ktor.serialization.ContentConverter
import io.ktor.serialization.JsonConvertException
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.http.content.staticResources
import io.ktor.server.plugins.MissingRequestParameterException
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.routing
import io.ktor.server.thymeleaf.Thymeleaf
import io.ktor.util.reflect.TypeInfo
import io.ktor.util.reflect.reifiedType
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.charsets.Charset
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver
import java.net.URLDecoder

fun Application.config() {

    install(CallLogging)

    install(Thymeleaf) {
        setTemplateResolver(ClassLoaderTemplateResolver().apply {
            prefix = "templates/"
            suffix = ".html"
            characterEncoding = "utf-8"
            isCacheable = false
        })
    }

    install(ContentNegotiation) {
        val mapper = ObjectMapper()
        mapper.apply {
            setDefaultPrettyPrinter(
                DefaultPrettyPrinter().apply {
                    indentArraysWith(DefaultPrettyPrinter.FixedSpaceIndenter.instance)
                    indentObjectsWith(DefaultIndenter("  ", "\n"))
                }
            )
        }
        mapper.registerKotlinModule()
        register(ContentType.Application.Json, JacksonConverter(mapper, true))
        register(ContentType.Application.FormUrlEncoded, FormUrlEncodedConverter(mapper))
    }

    install(StatusPages) {

        exception<MissingRequestParameterException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                mapOf("code" to 400, "message" to (cause.message ?: "参数丢失"))
            )
        }

        exception<Throwable> { call, cause ->
            call.respond(HttpStatusCode.InternalServerError, mapOf("code" to 500, "message" to cause.message))
            throw cause
        }
    }

    routing {
        staticResources("static", "/static")
    }

}


class FormUrlEncodedConverter(private val objectMapper: ObjectMapper) : ContentConverter {

    override suspend fun serialize(
        contentType: ContentType,
        charset: Charset,
        typeInfo: TypeInfo,
        value: Any?
    ): OutgoingContent {
        return OutputStreamContent(
            {
                val jsonNode = objectMapper.readTree(objectMapper.writeValueAsString(value))
                val sb = StringBuilder()
                jsonNode.fieldNames().forEach {
                    sb.append(it).append(jsonNode.get(it)).append("&")
                }
                objectMapper.writeValue(this, sb.removeSuffix("&").toString())
            },
            contentType.withCharsetIfNeeded(charset)
        )
    }

    override suspend fun deserialize(charset: Charset, typeInfo: TypeInfo, content: ByteReadChannel): Any? {
        try {
            return withContext(Dispatchers.IO) {
                val reader = content.toInputStream().reader(charset)
                val body = reader.readText()
                val objectNode = objectMapper.createObjectNode()
                body.split("&").forEach {
                    val arr = it.split("=")
                    val k = arr[0]
                    val v = URLDecoder.decode(arr[1], "utf-8")
                    objectNode.put(k, v)
                }
                objectMapper.treeToValue(objectNode, objectMapper.constructType(typeInfo.reifiedType))
            }
        } catch (deserializeFailure: Exception) {
            val convertException = JsonConvertException("Illegal json parameter found", deserializeFailure)

            when (deserializeFailure) {
                is JsonParseException -> throw convertException
                is JsonMappingException -> throw convertException
                else -> throw deserializeFailure
            }
        }
    }
}