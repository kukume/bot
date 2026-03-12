package me.kuku.telegram.ktor

import io.ktor.server.application.*
import org.koin.ksp.generated.defaultModule
import org.koin.ktor.plugin.Koin

fun Application.koin() {
    install(Koin) {
        modules(defaultModule)
    }
}