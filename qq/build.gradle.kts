plugins {
    alias(libs.plugins.koin.compiler)
    alias(libs.plugins.shadow)
    application
}

dependencies {
    implementation(libs.koin.core)
    compileOnly(libs.koin.annotations)

    implementation(project(":logic"))
    implementation(libs.jsoup)

    implementation(libs.qqpd.bot)
    implementation(libs.aws.s3)
}

application {
    mainClass.set("me.kuku.qqbot.QqApplicationKt")
}

tasks.shadowJar {
    archiveFileName.set("qq.jar")
    mergeServiceFiles()
}

koinCompiler {
    compileSafety = false
}