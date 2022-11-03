val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project
val json_version: String by project
val bc_version: String by project

plugins {
    application
    kotlin("jvm") version "1.7.20"
    kotlin("plugin.serialization") version "1.7.20"
    id("io.ktor.plugin") version "2.1.3"
}

group = "me.kohpai"
version = "0.0.1"
application {
    mainClass.set("io.ktor.server.netty.EngineMain")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

ktor {
    docker {
        jreVersion.set(io.ktor.plugin.features.JreVersion.JRE_11)
        localImageName.set("spychat-signaling-server")
        imageTag.set("0.0.1-preview")
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-core-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-websockets-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-netty-jvm:$ktor_version")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("org.bouncycastle:bcprov-jdk18on:$bc_version")
    implementation("org.bouncycastle:bcpkix-jdk18on:$bc_version")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$json_version")
    testImplementation("io.ktor:ktor-server-tests-jvm:$ktor_version")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
}