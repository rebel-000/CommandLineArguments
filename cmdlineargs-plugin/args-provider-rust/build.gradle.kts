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
            compatiblePlugins("com.jetbrains.rust")
        } else {
            create(IntelliJPlatformType.RustRover, ppString("platform.version")) {
                useInstaller = false
                useCustomCache = true
            }

            bundledPlugins("com.jetbrains.rust")
        }
        jetbrainsRuntime()
    }

    implementation(project(":cmdlineargs-plugin:args-core"))
}
