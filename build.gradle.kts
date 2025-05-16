import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.intellij.platform") version "2.6.0"
    kotlin("jvm") version "2.1.20"
}

group = "cssvarsassistant"
version = "1.0.0"

repositories {
    mavenCentral()
    intellijPlatform { defaultRepositories() }
}

dependencies {
    intellijPlatform {
        webstorm("2025.1")
        bundledPlugin("com.intellij.css")
    }
}

intellijPlatform {
    sandboxContainer.set(layout.buildDirectory.dir("sandbox"))
    buildSearchableOptions = false  // Optional: speeds up build

    pluginConfiguration {
        id = "cssvarsassistant"
        name = "CSS Variables Assistant"
        version = project.version.toString()
        description = """
            Enhances CSS variable usage with autocomplete and documentation.
            Shows variable values and documentation on hover, provides
            completion suggestions, and displays color swatches for color variables.
        """.trimIndent()

        vendor {
            name = "Stian Larsen"
            email = "stian.larsen@mac.com"
            url = "https://stianlarsen.com"
        }

        ideaVersion {
            sinceBuild = "241"  // 2024.1+
        }

        changeNotes = """
            Initial release:
            - CSS variable indexing across CSS/SCSS files
            - Autocompletion in CSS, JS, TS, and JSX/TSX files
            - Documentation popups with color previews
        """.trimIndent()
    }
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "21"
            languageVersion = "2.0"
        }
    }


    withType<PublishPluginTask>().configureEach {
        val env    = System.getenv("PUBLISH_TOKEN")
        val byProp = providers.gradleProperty("PUBLISH_TOKEN")
        token.set(
            env?.takeUnless { it.isBlank() }
                ?: byProp.orNull
                ?: throw GradleException(
                    "PUBLISH_TOKEN must be set either as ENV or in ~/.gradle/gradle.properties"
                )
        )
    }
}