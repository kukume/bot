plugins {
    alias(libs.plugins.ktor)
}

dependencies {

    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.serialization.jackson)

    implementation(libs.ktor.server.thymeleaf)
    implementation(libs.logback.classic)

    implementation(libs.okhttp)

    implementation(libs.playwright)
}

application {
    mainClass.set("me.kuku.headless.HeadlessApplicationKt")
}