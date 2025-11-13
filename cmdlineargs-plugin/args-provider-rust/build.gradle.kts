import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType

plugins {
    id("cmdlineargs-common-conventions")
}

dependencies {
    intellijPlatform{
        create(IntelliJPlatformType.RustRover, ppString("platform.version")) {
            useInstaller = false
            useCustomCache = true
        }

        bundledPlugins("com.jetbrains.rust")
        jetbrainsRuntime()
    }

    implementation(project(":cmdlineargs-plugin:args-core"))
}
