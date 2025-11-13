import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType

plugins {
    id("cmdlineargs-common-conventions")
}

dependencies {
    intellijPlatform{
        create(IntelliJPlatformType.PyCharmCommunity, ppString("platform.version")) {
            useInstaller = false
            useCustomCache = true
        }

        bundledPlugins("PythonCore")
        jetbrainsRuntime()
    }

    implementation(project(":cmdlineargs-plugin:args-core"))
}
