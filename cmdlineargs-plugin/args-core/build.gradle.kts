import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType

plugins {
    id("cmdlineargs-common-conventions")
}

dependencies {
    intellijPlatform{
        create(IntelliJPlatformType.IntellijIdea, ppString("platform.iu-version")) {
            useInstaller = false
            useCache = true
        }
        compatiblePlugins("intellij.execution.multilaunch")
        bundledPlugins("com.intellij.java")
        bundledPlugins("com.jetbrains.sh")
        bundledPlugins("org.jetbrains.kotlin")
        jetbrainsRuntime()
    }
}
