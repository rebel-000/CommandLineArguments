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
            compatiblePlugins("com.jetbrains.rust")
//            compatiblePlugins("intellij.platform.testRunner")
        } else {
            create(IntelliJPlatformType.RustRover, ppString("platform.version")) {
                useInstaller = false
                useCache = true
            }

            bundledPlugins("com.jetbrains.rust")
            bundledModule("intellij.platform.testRunner")
        }
        jetbrainsRuntime()
    }

    implementation(project(":cmdlineargs-plugin:args-core"))
}
