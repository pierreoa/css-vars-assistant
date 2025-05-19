import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.intellij.platform") version "2.6.0"
    kotlin("jvm") version "2.1.20"
}

group = "com.stianlarsen"
version = "1.0.2"



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
    <h2>1.0.2 – 2025‑05‑19</h2>
    <h3>Added</h3>
    <ul>
      <li><b>Settings page:</b> Plugin is now user-configurable (toggle context-based variable values).</li>
      <li><b>Smarter completions:</b>
        <ul>
          <li>Dual color swatch if a variable has both light and dark color values.</li>
          <li>All original value syntaxes shown (e.g., <code>#fff</code>, <code>hsl(0 0% 100%)</code>, <code>rgb(...)</code>).</li>
          <li>Context-aware completions: e.g. “🌙” for dark mode, no overlays.</li>
        </ul>
      </li>
      <li><b>Color swatches:</b>
        <ul>
          <li>Now supports <b>shadcn/ui</b> color variable format, <code>--foreground: 0 0% 100%;</code> and usage like <code>hsl(var(--foreground))</code>.</li>
        </ul>
      </li>
      <li><b>Robust comment parsing:</b> All major CSS comment styles now supported for variable docs (JSDoc, plain, single-line, multiline).</li>
      <li><b>Context tracking:</b> Indexer now properly tracks context (media queries, dark/light, nested queries, etc).</li>
      <li><b>Documentation enhancements:</b>
        <ul>
          <li>Multi-context variables shown as a table.</li>
          <li>Color swatches always use original value syntax.</li>
          <li>WebAIM contrast checker link for color variables.</li>
        </ul>
      </li>
    </ul>
    <h3>Changed</h3>
    <ul>
      <li><b>Consistent UI:</b>
        <ul>
          <li>No overlay/context icons on completion swatch—context info is right-aligned only.</li>
          <li>Completions/docs now fully respect user settings.</li>
        </ul>
      </li>
    </ul>
    <h3>Fixed</h3>
    <ul>
      <li><b>Comment parsing bugs:</b> One-line, multi-line, and mixed-format doc-comments now always recognized.</li>
      <li><b>Threading issues:</b> Docs and completions no longer trigger read-access warnings.</li>
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
