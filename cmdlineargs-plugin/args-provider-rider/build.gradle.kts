import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType

plugins {
    id("cmdlineargs-common-conventions")
}

dependencies {
    intellijPlatform{
        create(IntelliJPlatformType.Rider, ppString("rider.platform.version")) {
            useInstaller = false
            useCustomCache = true
        }

        bundledPlugins("com.jetbrains.rider-cpp")
        jetbrainsRuntime()
    }

    implementation(project(":cmdlineargs-plugin:args-core"))
}
