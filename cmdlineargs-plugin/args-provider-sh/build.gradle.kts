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

        bundledPlugins("com.jetbrains.sh")
        jetbrainsRuntime()
    }

    implementation(project(":cmdlineargs-plugin:args-core"))
}
