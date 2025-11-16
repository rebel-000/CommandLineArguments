import org.jetbrains.intellij.platform.gradle.toIntelliJPlatformType

plugins {
    id("cmdlineargs-common-conventions")
}

dependencies {
    intellijPlatform{
        create(ppString("platform.type").toIntelliJPlatformType(), ppString("platform.version")) {
            useInstaller = false
            useCustomCache = true
        }

        bundledPlugins("com.intellij.java")
        bundledPlugins("com.jetbrains.sh")
        bundledPlugins("org.jetbrains.kotlin")
        jetbrainsRuntime()
    }

//    implementation(project(":cmdlineargs-plugin:args-core"))
}
