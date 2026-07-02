import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType

plugins {
    id("cmdlineargs-common-conventions")
}

dependencies {
    intellijPlatform{
        create(IntelliJPlatformType.Rider, ppString("platform.rd-version")) {
            useInstaller = false
            useCache = true
        }

        bundledPlugins("com.jetbrains.rider-cpp")
        bundledModule("intellij.rider.debugger.shared")
        jetbrainsRuntime()
    }

    implementation(project(":cmdlineargs-plugin:args-core"))
}
