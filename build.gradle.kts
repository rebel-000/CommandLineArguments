import org.gradle.plugins.ide.idea.model.IdeaLanguageLevel

plugins {
    idea
    id("common-conventions")
}

idea {
    layout.buildDirectory = file("${rootProject.projectDir}/build")
    project {
        jdkName = "21"
        languageLevel = IdeaLanguageLevel("11")
        vcs = "Git"
    }
    subprojects {
        layout.buildDirectory = file("${rootProject.projectDir}/build/${generateSequence(project){ it.parent }.takeWhile { it != rootProject }.map{ it.name }.toList().reversed().joinToString("-")}")
    }
    module {
        excludeDirs.add(file("dependencies"))
        excludeDirs.add(file(".intellijPlatform"))
    }
}

tasks {
    wrapper {
        gradleVersion = providers.gradleProperty("gradle.version").get()
    }
}
