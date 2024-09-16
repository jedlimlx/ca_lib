import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.dokka.gradle.DokkaTask

plugins {
    id("org.jetbrains.dokka") version "1.9.20"
    kotlin("multiplatform") version "2.0.20"
    id("org.jetbrains.compose") version "1.6.11"
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.20"
}

group = "org.jedlimlx"
version = "1.0.0"

repositories {
    mavenCentral()
    google()
}

kotlin {
    jvmToolchain(8)

    jvm {
        withJava()
        testRuns.named("test") {
            executionTask.configure {
                useJUnitPlatform()
            }
        }
    }

    js {
        browser {
            commonWebpackConfig {
                cssSupport {
                    enabled.set(true)
                }
            }
        }
    }

    val hostOs = System.getProperty("os.name")
    val isArm64 = System.getProperty("os.arch") == "aarch64"
    val isMingwX64 = hostOs.startsWith("Windows")
    val nativeTarget = when {
        hostOs == "Mac OS X" && isArm64 -> macosArm64("native")
        hostOs == "Mac OS X" && !isArm64 -> macosX64("native")
        hostOs == "Linux" && isArm64 -> linuxArm64("native")
        hostOs == "Linux" && !isArm64 -> linuxX64("native")
        isMingwX64 -> mingwX64("native")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-io-core:0.3.2")
                implementation("com.github.ajalt.mordant:mordant:3.0.0")
                implementation("com.github.ajalt.clikt:clikt:5.0.0")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-io-core:0.3.2")
                implementation(kotlin("test"))
                implementation("com.github.ajalt.mordant:mordant:2.7.2")
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation("it.skrape:skrapeit:1.1.5")
                implementation("androidx.compose.runtime:runtime:1.6.8")
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation("it.skrape:skrapeit:1.1.5")
            }
        }
        val jsMain by getting
        val jsTest by getting
        val nativeMain by getting
        val nativeTest by getting
    }
}

tasks.withType<DokkaTask>().configureEach {
    outputDirectory.set(File("${System.getProperty("user.dir")}/docs/api-reference"))
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            windows { console = true }
            targetFormats(TargetFormat.Pkg, TargetFormat.Exe, TargetFormat.Deb)
        }
    }
}
