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
        <h2>1.3.1 – 2025-05-28</h2>

        <h3>Added</h3>
        <ul>
        <li><b>Enhanced Debug CSS Import Resolution</b> action – comprehensive import chain analysis with tree visualization, variable counting, and detailed resolution paths.</li>
        <li><b>Dedicated completion cache</b> (<code>CssVarCompletionCache</code>) – separate caching system for LESS/SCSS variable resolution with improved performance.</li>
        <li><b>Index rebuilder utility</b> (<code>CssVariableIndexRebuilder</code>) – centralized index management for better code organization.</li>
        <li><b>Dynamic completion popup width</b> – automatically adjusts popup width based on longest variable name for better readability.</li>
        <li><b>Enhanced progress reporting</b> – detailed progress indicators for re-indexing operations with step-by-step feedback.</li>
        </ul>

        <h3>Changed</h3>
        <ul>
        <li><b>Code architecture improvements</b> – extracted cache and index management into dedicated utility classes for better maintainability.</li>
        <li>Index version bump to <code>50</code> (from <code>36</code>) for improved stability and compatibility.</li>
        <li><b>Re-index process enhancement</b> – more detailed progress reporting with visual feedback and error handling.</li>
        <li><b>Import resolution debugging</b> – comprehensive analysis shows full import trees, variable counts, and resolution failures.</li>
        </ul>

        <h3>Fixed</h3>
        <ul>
        <li><b>Cache management</b> – resolved issues with stale cache entries affecting completion accuracy.</li>
        <li><b>Index rebuilding reliability</b> – improved error handling and progress tracking during index operations.</li>
        <li><b>Threading improvements</b> – better handling of background tasks and cancellation scenarios.</li>
        <li><b>Memory optimization</b> – more efficient cache management reducing memory footprint.</li>
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
