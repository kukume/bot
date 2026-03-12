@file:Suppress("VulnerableLibrariesLocal")

plugins {
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    api(libs.ktor.client.core)
    api(libs.ktor.client.okhttp)
    api(libs.ktor.client.content.negotiation)
    api(libs.ktor.client.logging)
    api(libs.ktor.serialization.jackson)

    compileOnly(libs.ktor.server.core)

    compileOnly(libs.exposed.core)
    compileOnly(libs.exposed.jdbc)
    compileOnly(libs.exposed.dao)
    compileOnly(libs.exposed.java.time)
    compileOnly(libs.exposed.json)

    compileOnly(libs.hikari)

    compileOnly(libs.jsoup)
    compileOnly(libs.openai)
    compileOnly(libs.openpdf)
    compileOnly(libs.batik.transcoder)
    compileOnly(libs.batik.codec)
    compileOnly(libs.google.genai)

    compileOnly(libs.zxing.javase)
    compileOnly(libs.aws.s3)

    compileOnly(libs.anthropic)

    compileOnly(libs.jimmer.sql.kotlin)
    ksp(libs.jimmer.ksp)

    compileOnly(libs.koin.core)

    compileOnly(libs.telegram.bot.source.exposed)

    testCompileOnly(libs.jimmer.sql.kotlin)
    testCompileOnly(libs.hikari)
    testCompileOnly(libs.postgresql)
}
