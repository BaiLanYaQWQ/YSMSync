import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.1.21"
    id("com.gradleup.shadow") version "8.3.6"
}

group = "local.mcserver"
version = "1.0.1-1.19.4-port"

val serverDir = (findProperty("serverDir") as String?)
    ?: System.getenv("MC_SERVER_DIR")
    ?: "/disk2/mcserver/1"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://mvn-repo.arim.space/lesser-gpl3/")
}

dependencies {
    compileOnly(files("$serverDir/libraries/io/papermc/paper/paper-api/1.19.4-R0.1-SNAPSHOT/paper-api-1.19.4-R0.1-SNAPSHOT.jar"))
    compileOnly(files("$serverDir/libraries/io/netty/netty-all/4.1.87.Final/netty-all-4.1.87.Final.jar"))
    compileOnly(fileTree("$serverDir/libraries") { include("**/*.jar") })
    compileOnly(fileTree("$serverDir/libs") { include("**/*.jar") })
    compileOnly(files("$serverDir/plugins/ProtocolLib.jar"))
    implementation("space.arim.morepaperlib:morepaperlib:0.4.3")
    implementation(files("libs/ysm-2.6.5-forge+mc1.20.1-all.jar"))
}

kotlin {
    // The host provides Java 21 as a JDK and Java 17 as a runtime. Emit
    // Java-17-compatible classes so either server launcher can load YSMSync.
    jvmToolchain(21)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions.freeCompilerArgs.add("-opt-in=kotlin.ExperimentalStdlibApi")
    compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
}

tasks.processResources {
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}

tasks.shadowJar {
    archiveBaseName.set("YSMSync")
    archiveClassifier.set("")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
