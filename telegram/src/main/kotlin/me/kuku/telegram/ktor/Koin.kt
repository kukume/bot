package me.kuku.telegram.ktor

import io.ktor.server.application.*
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.KoinApplication
import org.koin.core.annotation.Module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger
import org.koin.plugin.module.dsl.withConfiguration

fun Application.koin() {
    install(Koin) {
        slf4jLogger()
        withConfiguration<KtorKoinApp>()
    }
}

@Module
@Configuration
@ComponentScan("me.kuku.telegram.handler")
class KuKuHandlerModule


@KoinApplication(modules = [KuKuHandlerModule::class])
class KtorKoinApp