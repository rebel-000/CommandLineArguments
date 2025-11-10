import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("kotlin")
}

kotlin {
    sourceSets {
        main {
            kotlin.srcDirs("src")
            resources.srcDirs("resources")
        }
    }

    jvmToolchain(21)
}

repositories {
    mavenCentral()
}

tasks {
    withType<KotlinCompile> {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_21
            languageVersion = KotlinVersion.DEFAULT
            apiVersion = KotlinVersion.KOTLIN_1_8
            freeCompilerArgs = listOf("-Xjvm-default=all")
        }
    }
}