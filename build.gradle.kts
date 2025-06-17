import org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.ByteArrayOutputStream

plugins {
    id("org.jetbrains.intellij.platform") version "2.6.0"
    kotlin("jvm") version "2.1.20"
}

group = "com.stianlarsen"
version = "1.5.1"

repositories {
    mavenCentral()
    intellijPlatform { defaultRepositories() }
}

dependencies {
    intellijPlatform {
        webstorm("2025.1")
        bundledPlugin("com.intellij.css")

        // Add verification tools (instrumentationTools() is deprecated and removed)
        pluginVerifier()
        zipSigner()
    }
    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit5"))
}

// Configuration cache compatible version task that increments on every build
abstract class AutoIncrementVersionTask : DefaultTask() {

    @get:OutputFile
    abstract val indexVersionFile: RegularFileProperty

    @get:Internal  // Internal property, not an input
    abstract val buildCounterFile: RegularFileProperty

    @get:Inject
    abstract val execOperations: ExecOperations

    @get:Inject
    abstract val providers: ProviderFactory

    @get:Inject
    abstract val layout: ProjectLayout

    init {
        // Always run this task (don't cache it)
        outputs.upToDateWhen { false }
    }

    @TaskAction
    fun incrementVersion() {
        val (newVersion, versionInfo) = getVersionInfo()

        val newContent = """package cssvarsassistant.index

const val INDEX_VERSION = $newVersion"""

        indexVersionFile.asFile.get().writeText(newContent)
        println("🔄 INDEX_VERSION updated to $newVersion ($versionInfo)")
    }

    private fun getVersionInfo(): Pair<Int, String> {
        return try {
            // Try Git-based versioning first, but add build counter for uniqueness
            val commitCountOutput = ByteArrayOutputStream()
            execOperations.exec {
                commandLine("git", "rev-list", "--count", "HEAD")
                standardOutput = commitCountOutput
            }
            val commitCount = commitCountOutput.toString().trim().toInt()

            val branchOutput = ByteArrayOutputStream()
            execOperations.exec {
                commandLine("git", "rev-parse", "--abbrev-ref", "HEAD")
                standardOutput = branchOutput
            }
            val branch = branchOutput.toString().trim()

            // Get and increment build counter
            val counterFile = buildCounterFile.asFile.get()
            val buildCounter = if (counterFile.exists()) {
                counterFile.readText().trim().toIntOrNull() ?: 0
            } else {
                // Create parent directory if it doesn't exist
                counterFile.parentFile.mkdirs()
                0
            }
            val newBuildCounter = buildCounter + 1
            counterFile.writeText(newBuildCounter.toString())

            // Use git commit count + base version + build counter for unique builds
            val baseVersion = 500
            val gitVersion = baseVersion + commitCount + newBuildCounter

            Pair(gitVersion, "Git-based (commits: $commitCount, build: $newBuildCounter, branch: $branch)")
        } catch (e: Exception) {
            // Fallback to file-based increment if Git is not available
            val indexFile = indexVersionFile.asFile.get()

            if (indexFile.exists()) {
                val content = indexFile.readText()
                val currentVersion = Regex("""const val INDEX_VERSION = (\d+)""")
                    .find(content)?.groupValues?.get(1)?.toInt() ?: 300
                val newVersion = currentVersion + 1
                Pair(newVersion, "File-based increment (previous: $currentVersion)")
            } else {
                Pair(300, "Default fallback")
            }
        }
    }
}

intellijPlatform {
    sandboxContainer.set(layout.buildDirectory.dir("sandbox"))
    buildSearchableOptions = false

    pluginConfiguration {
        id = "cssvarsassistant"
        name = "CSS Variables Assistant"
        version = project.version.toString()
        vendor {
            name = "StianLarsen"
            email = "stian.larsen@mac.com"
            url = "https://github.com/stianlars1/css-vars-assistant"
        }

        ideaVersion {
            sinceBuild = "241"
        }

        description = """
<h2>CSS Variables Assistant</h2>
<p>
  Supercharge your CSS custom properties and pre-processor variables in JetBrains IDEs with advanced autocomplete, rich documentation, and powerful debugging tools.
</p>
<ul>
  <li><b>Instant variable lookup</b> – blazing-fast completions for <code>CSS</code>, <code>SCSS</code>, <code>SASS</code> and <code>LESS</code> variables.</li>
  <li><b>Smart autocomplete</b> – context-aware suggestions inside <code>var(--…)</code>, <code>@less</code> and <code>${'$'}scss</code>, sorted by value or context.</li>
  <li><b>Rich documentation pop-ups</b> – value tables (px equivalents), context labels, color swatches with contrast info, plus <i>dynamic</i> <code>px Eq.</code>, <code>Hex</code> and <code>WCAG</code> columns that appear only when relevant.</li>
  <li><b>IntelliJ 2024.1+ API support</b> – leverages the new docs API for richer pop-ups, with graceful fallback for older IDEs.</li>
  <li><b>Derived-variable indicator</b> – alias / recursive completions are marked with <code>↗</code> so you instantly know the value is inherited.</li>
  <li><b>JSDoc-style comments</b> – auto-parsing and display of <code>@name</code>, <code>@description</code>, and <code>@example</code>.</li>
  <li><b>Advanced <code>@import</code> resolution</b> – follows and indexes nested imports with depth and scope controls.</li>
  <li><b>Debugging tools</b> – visual tracing of variable origins and import chains via the "Debug CSS Import Resolution" action.</li>
  <li><b>Configurable sorting &amp; ranking</b> – numeric value order (asc/desc) and logical context ranking (Default → Dark / media queries).</li>
  <li><b>Performance &amp; robustness</b> – centralized index versioning, smarter caching and race-condition fixes keep everything fast in large projects.</li>
  <li><b>CSS cascade compliance</b> – documentation and completions now correctly follow CSS cascade rules where local declarations beat imports.</li>
  <li><b>Works everywhere</b> – <code>CSS</code>, <code>SCSS</code>, <code>SASS</code>, <code>LESS</code>.</li>
</ul>
<p>
  <b>🏅New in 1.5.1:</b> Updated documentation menu's table header styling.
</p>
<p>
  <b>✨ New in 1.5.0:</b> IntelliJ 2024.1+ documentation API support, dynamic value-table columns, recursively/derived variable completions marked with (↗), improved DebugImportResolution helper, CSS cascade compliance with winner-first documentation, and centralized index versioning.
</p>
""".trimIndent()

        changeNotes = """
<h2>1.5.1 – 2025-06-17</h2>

<h3>Changed</h3>
<ul>
  <li><b>Documentation menu styling</b> Removed border bottom from each table header and re-adds it to the wrapping table-header, to avoid annoying spaces between table headers.</li>
</ul>
""".trimIndent()
    }

    pluginVerification {
        ides {
            recommended()
        }

        // Suppress experimental API warnings while keeping all critical checks
        failureLevel.set(
            listOf(
                VerifyPluginTask.FailureLevel.COMPATIBILITY_PROBLEMS,
                VerifyPluginTask.FailureLevel.COMPATIBILITY_WARNINGS,
                VerifyPluginTask.FailureLevel.INVALID_PLUGIN,
                VerifyPluginTask.FailureLevel.MISSING_DEPENDENCIES,
                VerifyPluginTask.FailureLevel.PLUGIN_STRUCTURE_WARNINGS,
                VerifyPluginTask.FailureLevel.NOT_DYNAMIC,
                VerifyPluginTask.FailureLevel.INTERNAL_API_USAGES,
                VerifyPluginTask.FailureLevel.OVERRIDE_ONLY_API_USAGES,
                VerifyPluginTask.FailureLevel.NON_EXTENDABLE_API_USAGES,
                VerifyPluginTask.FailureLevel.SCHEDULED_FOR_REMOVAL_API_USAGES
                // Note: Deliberately excluding EXPERIMENTAL_API_USAGES since we use
                // the JetBrains-recommended V2 Documentation API
            )
        )
    }
}


tasks {
    // Register the version increment task
    val autoIncrementVersion = register<AutoIncrementVersionTask>("autoIncrementVersion") {
        group = "versioning"
        description = "Automatically increments INDEX_VERSION for both local dev and production"

        indexVersionFile.set(layout.projectDirectory.file("src/main/kotlin/cssvarsassistant/index/indexVersion.kt"))
        buildCounterFile.set(layout.projectDirectory.file(".gradle/build-counter.txt"))
    }

    withType<KotlinCompile> {
        // Run version increment before compilation
        dependsOn(autoIncrementVersion)

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
        // Ensure version is updated before building plugin
        dependsOn(autoIncrementVersion)

        from(fileTree("lib")) {
            exclude("kotlin-stdlib*.jar")
        }
    }

    // Also update version before running IDE (if you use runIde)
    named("runIde") {
        dependsOn(autoIncrementVersion)
    }
}