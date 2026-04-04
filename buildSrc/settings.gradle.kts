buildCache {
    local {
        isEnabled = true
        directory = File(rootDir, "../build/buildsrc-cache")
    }
}

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}