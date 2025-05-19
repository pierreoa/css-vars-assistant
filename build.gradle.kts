import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.intellij.platform") version "2.6.0"
    kotlin("jvm") version "2.1.20"
}

group = "com.stianlarsen"
version = "1.1.0"



repositories {
    mavenCentral()
    intellijPlatform { defaultRepositories() }
}

dependencies {
    intellijPlatform {
        webstorm("2025.1")
        bundledPlugin("com.intellij.css")
    }
    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit5"))
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
    <h2>1.1.0 – 2025‑05‑19</h2>

    <h3>Added</h3>
    <ul>
      <li><b>Alias resolution:</b> completions &amp; docs now resolve <code>var(--alias)</code> chains to show the literal value.</li>
      <li><b>Duplicate squashing:</b> identical context + value pairs collapse into a single row.</li>
      <li><b>Smart documentation order:</b> rows are now Default / Light → Dark → desktop‑first break‑points → mobile‑first → other media.</li>
      <li><b>Light as primary:</b> <code>prefers-color-scheme: light</code> is treated the same as <code>default</code>.</li>
      <li><b>Stricter index filter:</b> non‑stylesheet files (e.g. <code>.txt</code>, templates) are ignored.</li>
    </ul>

    <h3>Changed</h3>
    <ul>
      <li><b>Completion row:</b> always lists context values (no more “(+N)” placeholders) and follows the new smart order.</li>
      <li><b>Colour chips:</b> dual‑swatch also shown when light/dark values alias to the same colour.</li>
    </ul>

    <h3>Fixed</h3>
    <ul>
      <li>No more duplicate “Default” rows when a variable is declared multiple times with the same value.</li>
      <li>Documentation no longer displays raw <code>var(--xyz)</code> strings when they can be resolved.</li>
      <li>Colour swatch detection &amp; WebAIM links work for resolved alias values.</li>
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

    withType<Test> {
        useJUnitPlatform()

        testLogging {
            events("passed", "skipped", "failed")
        }
    }

    buildPlugin {
        from(fileTree("lib")) {
            exclude("kotlin-stdlib*.jar")
        }
    }
}
