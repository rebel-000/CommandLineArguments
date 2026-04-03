import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.toIntelliJPlatformType

plugins {
    id("cmdlineargs-common-conventions")
}

dependencies {
    intellijPlatform{
        create(IntelliJPlatformType.IntellijIdea, ppString("platform.version")) {
            useInstaller = false
            useCache = true
        }

        bundledPlugins("com.intellij.java")
        bundledPlugins("com.jetbrains.sh")
        bundledPlugins("org.jetbrains.kotlin")
        jetbrainsRuntime()
    }

//    implementation(project(":cmdlineargs-plugin:args-core"))
}
