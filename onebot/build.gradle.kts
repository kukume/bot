plugins {
    application
    alias(libs.plugins.shadow)
    alias(libs.plugins.koin.compiler)
}

dependencies {
    implementation(project(":logic"))
    implementation(libs.logback.classic)
    implementation(libs.aws.s3)
    implementation(libs.koin.core)
    compileOnly(libs.koin.annotations)
    implementation(libs.ronebot.onebot)
    implementation(libs.openpdf)
    implementation(libs.batik.transcoder)
    implementation(libs.batik.codec)
    implementation(libs.google.genai)
    implementation(libs.openai)
    implementation(libs.jsoup)
    implementation(libs.anthropic)
}

application {
    mainClass = "me.kuku.onebot.OneBotApplicationKt"
}
