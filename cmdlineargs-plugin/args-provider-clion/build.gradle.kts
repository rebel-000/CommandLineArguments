import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.toIntelliJPlatformType

plugins {
    id("cmdlineargs-common-conventions")
}

dependencies {
    intellijPlatform{
        if (tryGetPluginProperty("minimal-build-environment")?.toBoolean() == true) {
            create(ppString("platform.type").toIntelliJPlatformType(), ppString("platform.version")) {
                useInstaller = false
                useCustomCache = true
            }
            compatiblePlugins("com.intellij.clion", "com.intellij.cmake", "com.intellij.nativeDebug", "name.kropp.intellij.makefile", "org.jetbrains.plugins.clion.radler")
        } else {
            create(IntelliJPlatformType.CLion, ppString("platform.version")) {
                useInstaller = false
                useCustomCache = true
            }

            bundledPlugins("com.intellij.clion", "com.intellij.clion-makefile")
        }
        jetbrainsRuntime()
    }

    implementation(project(":cmdlineargs-plugin:args-core"))
}
