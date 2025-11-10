import org.jetbrains.intellij.platform.gradle.toIntelliJPlatformType

plugins {
    id("cmdlineargs-common-conventions")
}

dependencies {
    intellijPlatform{
        if (!project.name.startsWith("args-")) {
            error("Only 'args-*' subprojects are supported")
        }

        project.name
            .substringAfter('-')
            .replace('-', '.')
            .let { provider ->
                val platformTypeString = tryGetPluginProperty("${provider}.platform.type") ?: ppString("platform.type")
                val platformType = platformTypeString.toIntelliJPlatformType()
                val platformVersion = tryGetPluginProperty("${provider}.platform.version") ?: ppString("platform.version")
                create(platformType, platformVersion) {
                    useInstaller = false
                    useCustomCache = true
                }
                jetbrainsRuntime()
                logger.lifecycle("registered cmdlineargs provider: $provider")
                ppWithList("${provider}.bundledPlugins") {
                    bundledPlugins(it)
                    logger.lifecycle("    with bundledPlugins: ${it.joinToString(", ")}")
                }
                ppWithList("${provider}.compatiblePlugins") {
                    compatiblePlugins(it)
                    logger.lifecycle("    with compatiblePlugins: ${it.joinToString(", ")}")
                }
                ppWithList("${provider}.plugins") {
                    plugins(it)
                    logger.lifecycle("    with plugins: ${it.joinToString(", ")}")
                }
            }
    }
    
    if (project.name != "args-core") {
        implementation(project(":cmdlineargs-plugin:args-core"))
    }
}
