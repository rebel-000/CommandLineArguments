import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType

plugins {
    id("cmdlineargs-common-conventions")
}

dependencies {
    intellijPlatform{
        create(IntelliJPlatformType.CLion, ppString("platform.version")) {
            useInstaller = false
            useCustomCache = true
        }

        bundledPlugins("com.intellij.clion", "com.intellij.clion-makefile")
        jetbrainsRuntime()
    }

    implementation(project(":cmdlineargs-plugin:args-core"))
}
