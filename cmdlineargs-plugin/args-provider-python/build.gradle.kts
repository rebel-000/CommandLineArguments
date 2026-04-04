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
            compatiblePlugins("PythonCore")
        } else {
            create(IntelliJPlatformType.PyCharm, ppString("platform.version")) {
                useInstaller = false
                useCache = true
            }

            bundledPlugins("PythonCore")
        }
        jetbrainsRuntime()
    }

    implementation(project(":cmdlineargs-plugin:args-core"))
}
