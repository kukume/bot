package me.kuku.telegram.ktor

import io.ktor.server.application.*
import org.koin.ktor.plugin.Koin

fun Application.koin() {
    install(Koin)
}