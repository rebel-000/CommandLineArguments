import org.jetbrains.intellij.platform.gradle.extensions.intellijPlatform

plugins {
    id("common-conventions")
    id("org.jetbrains.intellij.platform")
}

repositories {
    intellijPlatform {
        defaultRepositories()
        jetbrainsRuntime()
    }
}

intellijPlatform {
    buildSearchableOptions = false
    instrumentCode = false
}
