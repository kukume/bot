plugins {
    alias(libs.plugins.koin.compiler)
    alias(libs.plugins.ktor)
}

dependencies {
    implementation(libs.koin.core)
    compileOnly(libs.koin.annotations)

    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.double.receive)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.koin.ktor)

    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.json)

    implementation(libs.hikari)

    implementation(libs.logback.classic)
    implementation(libs.postgresql)

    implementation(project(":logic"))
    implementation(libs.bouncycastle.bcprov)
    implementation(libs.openai)
    implementation(libs.jsoup)
}

application {
    mainClass.set("me.kuku.qqbot.QqApplicationKt")
}
