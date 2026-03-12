package me.kuku.qqbot.ktor

import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.doublereceive.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import org.koin.core.qualifier.named
import org.koin.ktor.ext.getKoin
import org.koin.ktor.plugin.Koin

fun Application.config() {

    install(StatusPages) {

        exception<MissingRequestParameterException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ""
            )
        }

        exception<Throwable> { call, cause ->
            call.respond(HttpStatusCode.InternalServerError, "")
            throw cause
        }
    }

    install(CallLogging)


    install(ContentNegotiation) {
        jackson()
    }

    install(DoubleReceive)

    install(Koin) {
        modules()
    }

}

fun Application.declareConfig() {
    val koin = getKoin()
    val openaiConfig = environment.config.config("openai")
    koin.declare(openaiConfig, named("openaiConfig"))
    val qqConfig = environment.config.config("qq")
    koin.declare(qqConfig, named("qqConfig"))
    val rapidConfig = environment.config.config("rapid")
    koin.declare(rapidConfig, named("rapidConfig"))
}