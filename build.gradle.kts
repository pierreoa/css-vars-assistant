import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.intellij.platform") version "2.6.0"
    kotlin("jvm") version "2.1.20"
}

group = "com.stianlarsen"
version = "1.0.1"



repositories {
    mavenCentral()
    intellijPlatform { defaultRepositories() }
}

dependencies {
    intellijPlatform {
        // should work for all jetbrains IDE´s where CSS is used
        webstorm("2025.1")
        bundledPlugin("com.intellij.css")
    }
}

intellijPlatform {
    sandboxContainer.set(layout.buildDirectory.dir("sandbox"))
    buildSearchableOptions = false

    pluginConfiguration {
        id = "cssvarsassistant"
        name = "CSS Variables Assistant"
        version = project.version.toString()
        description = """
            <h2>CSS Variables Assistant</h2>
            <p>
              Enhances CSS variable usage with autocomplete and documentation.
              Shows <strong>variable values</strong> in completion suggestions <strong>on typing</strong> and 
              documentation on hover. Also displays color swatches for color variables.
            </p>
            <p>
              Supercharge your CSS custom properties in WebStorm and IntelliJ-based IDEs:
            </p>
            <ul>
              <li><b>Smart Autocomplete</b> inside <code>var(--…)</code></li>
              <li><b>Quick Documentation</b> (Ctrl+Q) showing value, description & color swatch</li>
              <li><b>JSDoc‑style</b> comment support (<code>@name</code>, <code>@description</code>, <code>@example</code>)</li>
              <li><b>Sorted suggestions</b> by value (largest first)</li>
            </ul>
            <p>
              Works in CSS, SCSS, SASS, LESS, JavaScript/TypeScript, and JSX/TSX files.
            </p>
        """.trimIndent()

        vendor {
            name = "StianLarsen"
            email = "stian.larsen@mac.com"
            url = "https://github.com/stianlars1/css-vars-assistant"
        }

        ideaVersion {
            sinceBuild = "241"
        }

        changeNotes = """
            <h3>1.0.0</h3>
            <ul>
              <li>Production release: v1.0.0</li>
              <li>Autocomplete only in <code>var(…)</code>, sorted by CSS value (high→low)</li>
              <li>Hover & Quick‑Docs with color swatch and comment parsing</li>
              <li>Removed <code>@value</code> override tags—always shows real CSS value</li>
            </ul>

            <h3>0.1.2 → 1.0.0</h3>
            <ul>
              <li>Flipped suggestion order: largest first</li>
              <li>Minor bugfixes and context‑checks improved</li>
            </ul>
        """.trimIndent()
    }
}

tasks {
    withType<KotlinCompile> {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
            languageVersion.set(KotlinVersion.KOTLIN_2_0)
        }
    }

}