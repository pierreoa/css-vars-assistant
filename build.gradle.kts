import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.intellij.platform") version "2.6.0"
    kotlin("jvm") version "2.1.20"
}

group = "com.stianlarsen"
version = "1.3.1"



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
              <li><b>Advanced @import resolution</b> with configurable scope and depth</li>
              <li><b>Multi-preprocessor support</b> (CSS, SCSS, SASS, LESS)</li>
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
      <h2>1.3.0 – 2025-05-27</h2>

      <h3>Added</h3>
      <ul>
        <li><b>Dynamic pre-processor resolution:</b> automatic, depth-limited resolution of chained <code>@less</code>, <code>\${'$'}scss</code> and nested <code>var(--foo)</code> references.</li>
        <li><b>Import cache:</b> remembers every file reached via <code>@import</code>; instant look-ups after first pass.</li>
        <li><b>🔄 Re-index Now</b> button in the Settings panel – rebuilds the variable index without needing <em>Invalidate Caches / Restart</em>.</li>
        <li><b>Debug CSS Import Resolution</b> action to print the full, resolved import chain for any stylesheet.</li>
        <li><b>Background-task integration:</b> long operations are cancellable and show progress.</li>
      </ul>

      <h3>Changed</h3>
      <ul>
        <li>Default <code>maxImportDepth</code> raised from <code>3</code> to <code>10</code> (still user-configurable).</li>
        <li>Consistent plugin-shield icon for all completions originating from the assistant.</li>
        <li>Scope utilities refactored – fresh scope calculated for every resolution to avoid stale caches.</li>
      </ul>

      <h3>Fixed</h3>
      <ul>
        <li><b>Project + Imports</b> scope now resolves real values (e.g. <code>--ffe-farge-vann → #005aa4</code>) instead of showing <code>@lessVar</code>.</li>
        <li>Race condition that caused occasional <code>ProcessCanceledException</code> in large projects.</li>
        <li>Index rebuild no longer double-counts <code>node_modules</code> in Global scope.</li>
        <li>Numerous threading and cancellation-handling improvements.</li>
      </ul>
    """.trimIndent()

    }
    pluginVerification {
        ides {
            recommended()
        }
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
