package me.kuku.telegram.ktor

import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.doublereceive.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*

fun Application.config() {

    install(StatusPages) {

        exception<MissingRequestParameterException> { call, _ ->
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

}