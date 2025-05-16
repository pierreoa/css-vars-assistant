import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/*plugins {
  id("java")
  id("org.jetbrains.kotlin.jvm") version "1.9.25"
  id("org.jetbrains.intellij") version "1.17.4"
}*/

plugins {
    id("org.jetbrains.intellij.platform") version "2.6.0"
    kotlin("jvm") version "2.1.20" // Latest bundled in 2025.2
}

group = "cssvarsassistant"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
    intellijPlatform {
        releases()
        marketplace()
    }
}

dependencies {
    intellijPlatform {
        webstorm("2025.1")

        // Required CSS bundled plugin dependency
        bundledPlugin("com.intellij.css")
    }
}

intellijPlatform {
    pluginConfiguration {
        id = "com.example.webstorm.css-variables-assistant"
        name = "CSS Variables Assistant"
        version = project.version.toString()
        description = """
            Enhances CSS variable usage with:
            • Autocompletion for var(--) usage
            • Documentation popups showing variable values and comments
            • Color previews for CSS color variables
        """.trimIndent()

        vendor {
            name = "Your Company"
            email = "contact@example.com"
            url = "https://example.com"
        }

        changeNotes = """
            Initial version:
            - CSS variable indexing
            - Autocompletion in CSS/JS/TS files
            - Documentation with color previews
        """.trimIndent()

        ideaVersion {
            sinceBuild = "241" // 2024.1+
        }
    }

    // Provide a consistent sandbox location
    //sandboxDir.set(layout.buildDirectory.dir("sandbox"))

    runIde {
        sandboxDir.set(layout.buildDirectory.dir("sandbox"))
    }
}

// claude
tasks {
    withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "21"
            languageVersion = "2.0"
        }
    }

    // Verify plugin structure before building
    buildPlugin {
        dependsOn(verifyPluginStructure)
    }
}


// custom auto generated
/*tasks {
  // Set the JVM compatibility versions
  withType<JavaCompile> {
    sourceCompatibility = "17"
    targetCompatibility = "17"
  }
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
  }

  patchPluginXml {
    sinceBuild.set("241")
    untilBuild.set("243.*")
  }

  signPlugin {
    certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
    privateKey.set(System.getenv("PRIVATE_KEY"))
    password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
  }

  publishPlugin {
    token.set(System.getenv("PUBLISH_TOKEN"))
  }
}*/
