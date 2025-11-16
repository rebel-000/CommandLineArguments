rootProject.name = "commandline-arguments-plugin"

include(
    "cmdlineargs-plugin",
    "cmdlineargs-plugin:args-core",
    "cmdlineargs-plugin:args-provider-clion",
    "cmdlineargs-plugin:args-provider-python",
    "cmdlineargs-plugin:args-provider-rider",
    "cmdlineargs-plugin:args-provider-rust",
)

buildCache {
    local {
        isEnabled = true
        directory = File(rootDir, "build/build-cache")
    }
}

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://central.sonatype.com/repository/maven-snapshots/")
    }
}
