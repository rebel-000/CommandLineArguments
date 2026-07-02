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
        compatiblePlugins("com.jetbrains.rust")
        bundledModule("intellij.platform.testRunner")
//        if (tryGetPluginProperty("minimal-build-environment")?.toBoolean() == true) {
//            create(IntelliJPlatformType.IntellijIdea, ppString("platform.iu-version")) {
//                useInstaller = false
//                useCache = true
//            }
//            compatiblePlugins("com.jetbrains.rust")
//            bundledModule("intellij.platform.testRunner")
//        } else {
//            create(IntelliJPlatformType.RustRover, ppString("platform.rr-version")) {
//                useInstaller = false
//                useCache = true
//            }
//            bundledPlugins("com.jetbrains.rust")
//            bundledModule("intellij.platform.testRunner")
//        }
        jetbrainsRuntime()
    }

    implementation(project(":cmdlineargs-plugin:args-core"))
}
