import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType

plugins {
    id("cmdlineargs-common-conventions")
}

dependencies {
    intellijPlatform{
        if (tryGetPluginProperty("minimal-build-environment")?.toBoolean() == true) {
            create(IntelliJPlatformType.IntellijIdea, ppString("platform.version")) {
                useInstaller = false
                useCache = true
            }
            compatiblePlugins("com.intellij.clion", "com.intellij.cmake", "com.intellij.nativeDebug", "name.kropp.intellij.makefile", "org.jetbrains.plugins.clion.radler")
        } else {
            create(IntelliJPlatformType.CLion, ppString("platform.version")) {
                useInstaller = false
                useCache = true
            }
            bundledPlugins("com.intellij.clion", "com.intellij.clion-makefile")
        }
        jetbrainsRuntime()
    }

    implementation(project(":cmdlineargs-plugin:args-core"))
}
