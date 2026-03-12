plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.kotlin.ksp)
}

allprojects {
    group = "me.kuku"
    version = "1.0-SNAPSHOT"

    repositories {
        mavenLocal()
        mavenCentral()
        maven("https://packages.rtast.cn/snapshots")
        maven("https://repo.maven.rtast.cn/releases")
        maven("https://maven.kuku.me/central/")
    }
}

subprojects {

    apply {
        plugin("org.jetbrains.kotlin.jvm")
        plugin("com.google.devtools.ksp")
    }

    dependencies {
        testImplementation(kotlin("test"))
    }

    tasks.test {
        useJUnitPlatform()
    }

    kotlin {
        jvmToolchain(21)

        compilerOptions {
            freeCompilerArgs.add("-Xcontext-parameters")
        }
    }

    ksp {
        arg("KOIN_DEFAULT_MODULE","true")
    }
}
