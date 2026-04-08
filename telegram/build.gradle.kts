plugins {
    alias(libs.plugins.ktor)
    alias(libs.plugins.koin.compiler)
}

dependencies {
    implementation(project(":logic"))
    implementation(libs.koin.core)
    compileOnly(libs.koin.annotations)

    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.java.time)
    implementation(libs.exposed.json)

    implementation(libs.hikari)

    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.config.yaml)
    implementation(libs.ktor.server.double.receive)
    implementation(libs.ktor.serialization.jackson)

    implementation(libs.logback.classic)
    implementation(libs.postgresql)

    implementation(libs.telegram.bot.core)
    implementation(libs.telegram.bot.ktor)
    implementation(libs.telegram.bot.source.exposed)

    implementation(libs.quartz)
    implementation(libs.quartz.jobs)

    implementation(libs.openai)

    implementation(libs.zxing.javase)
}

application {
    mainClass.set("me.kuku.telegram.TelegramApplicationKt")
}

koinCompiler {
    compileSafety = false
}