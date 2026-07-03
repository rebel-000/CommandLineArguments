import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.extensions.IntelliJPlatformTestingExtension
import org.jetbrains.intellij.platform.gradle.models.ProductRelease.Channel
import org.jetbrains.intellij.platform.gradle.tasks.RunIdeTask
import org.jetbrains.intellij.platform.gradle.toIntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.utils.extensionProvider

plugins {
    id("cmdlineargs-common-conventions")
    alias(libs.plugins.changelog)
    alias(libs.plugins.qodana)
    alias(libs.plugins.kover)
}

dependencies {
    intellijPlatform {
        create(IntelliJPlatformType.IntellijIdea, ppString("platform.iu-version")) {
            useInstaller = false
            useCache = true
        }

        pluginComposedModule(implementation(project("args-core")))
        pluginComposedModule(implementation(project("args-provider-clion")))
        pluginComposedModule(implementation(project("args-provider-python")))
        pluginComposedModule(implementation(project("args-provider-rider")))
        pluginComposedModule(implementation(project("args-provider-rust")))
    }
}

changelog {
    groups = listOf("Added", "Changed", "Removed", "Fixed")
    path = layout.projectDirectory.file("../CHANGELOG.md").asFile.path
    ppWithString("repositoryUrl") {
        repositoryUrl = it
    }
}

version = ppString("version")

intellijPlatform {
    projectName = "CommandLineArguments"
    pluginConfiguration {
        id = ppString("id")
        name = ppString("name")
        version = ppString("version")

        description = providers.fileContents(layout.projectDirectory.file("../README.md")).asText.map {
            val start = "<!-- Plugin description -->"
            val end = "<!-- Plugin description end -->"

            with(it.lines()) {
                if (!containsAll(listOf(start, end))) {
                    throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
                }
                subList(indexOf(start) + 1, indexOf(end)).joinToString("\n").let(::markdownToHTML)
            }
        }

        changeNotes = provider {
            changelog.renderItem(
                (changelog.getOrNull(ppString("version")) ?: changelog.getUnreleased())
                    .withHeader(false)
                    .withEmptySections(false),
                Changelog.OutputType.HTML,
            )
        }

        ideaVersion {
            sinceBuild = ppString("sinceBuild")
            untilBuild = ppString("untilBuild")
        }
    }

    signing {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }

    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
        ppString("version")
            .substringAfter('-', "")
            .substringBefore('.')
            .let {
                if (it.isNotEmpty()) {
                    channels = listOf(it)
                }
            }
    }

    pluginVerification {
        ides {
            val verifyIdeList = providers.environmentVariable("PLUGIN_VERIFY_IDE")
                .getOrElse("")
                .split(",")
                .mapNotNull{ it.trim().lowercase().ifEmpty { null }}
            val ideList = listOf(
                IntelliJPlatformType.IntellijIdea,
                IntelliJPlatformType.CLion,
                IntelliJPlatformType.PyCharmProfessional,
                IntelliJPlatformType.Rider,
//                IntelliJPlatformType.RustRover,
            ).filter { verifyIdeList.isEmpty() || it.code.lowercase() in verifyIdeList }
            ideList.forEach { type ->
                select {
                    channels = listOf(Channel.RELEASE/*, Channel.EAP, Channel.RC*/)
                    types.convention(listOf(type))
                    sinceBuild = ppString("verify.sinceBuild")
                    untilBuild = ppString("verify.untilBuild")
                }
                ppWithList("verify.${type.code.lowercase()}-version") {
                    it.forEach { version ->
                        create(type, version)
                    }
                }
            }
        }
    }
}

kover {
    reports {
        total {
            xml {
                onCheck = true
            }
        }
    }
}

tasks {
    composedJar {
        archiveBaseName.convention(project.extensionProvider.flatMap { it.projectName })
    }

    publishPlugin {
        dependsOn(patchChangelog)
    }

    withType<RunIdeTask> {
        autoReload = false
        jvmArgs("-Xmx2g")
    }
}

intellijPlatformTesting {
    runIde {
        register("runIdeForUiTests", Action {
            useInstaller = false

            task {
                jvmArgumentProviders += CommandLineArgumentProvider {
                    listOf(
                        "-Drobot-server.port=8082",
                        "-Dide.mac.message.dialogs.as.sheets=false",
                        "-Djb.privacy.policy.text=<!--999.999-->",
                        "-Djb.consents.confirmation.enabled=false",
                    )
                }
            }

            plugins {
                robotServerPlugin()
            }
        })

        runPluginWithIdeTask(IntelliJPlatformType.IntellijIdea) {
            plugins {
                compatiblePlugins("org.toml.lang", "PythonCore")
                if (tryGetPluginProperty("minimal-build-environment")?.toBoolean() == true) {
                    compatiblePlugins("com.intellij.clion", "com.intellij.cmake", "com.intellij.nativeDebug", "name.kropp.intellij.makefile", "org.jetbrains.plugins.clion.radler")
                    compatiblePlugins("com.jetbrains.rust")
                }
            }
        }
        runPluginWithIdeTask(IntelliJPlatformType.CLion)
        runPluginWithIdeTask(IntelliJPlatformType.PyCharmProfessional)
        runPluginWithIdeTask(IntelliJPlatformType.Rider) {
            ppWithString("rider.platform.version") {
                this.version = it
            }
        }
        runPluginWithIdeTask(IntelliJPlatformType.RustRover)
    }
}

fun IntelliJPlatformTestingExtension.runPluginWithIdeTask(
    type: IntelliJPlatformType,
    name: String = type.name,
    configure: IntelliJPlatformTestingExtension.RunIdeParameters.() -> Unit = {},
) {
    runIde.register("runPluginWith$name", Action {
        this.useInstaller = false
        this.type = type
        this.version = ppString("platform.${type.code.lowercase()}-version")
        this.sandboxDirectory = intellijPlatform.sandboxContainer.dir("${name.lowercase()}-sandbox")
        configure()
    })
}
