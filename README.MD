# CSS Variables Assistant

**Version**: 1.0.0  
**IDE**: WebStorm, IntelliJ 2025.1+  
**Author**: Stian Larsen ([GitHub](https://github.com/stianlars1/css-vars-assistant))

Enhances CSS Custom Properties (variables) support with:

- **Autocomplete** inside `var(--...)`
- **Quick Documentation** (Ctrl+Q / hover) showing value, description, example and color swatch
- **Comment Tags**: `@name`, `@description` (or `@desc`/`@doc`), `@example`
- **Sorted Suggestions**: largest CSS values first
- **Works in**: CSS, SCSS, SASS, LESS, JS/TS, JSX/TSX

---

## Installation (Locally)

1. `git clone https://github.com/stianlars1/css-vars-assistant.git`
2. `cd css-vars-assistant`
3. `./gradlew buildPlugin`
4. In WebStorm: **Plugins → ⚙️ → Install Plugin from Disk...** → select `build/distributions/css-vars-assistant-1.0.0.zip`
5. Restart IDE.

---

## Publishing to JetBrains Marketplace

**Prerequisites**
- A JetBrains Account with “Plugin Developer” role
- [Token for plugin repository (PUBLISH_TOKEN)](https://plugins.jetbrains.com/docs/marketplace/generate-plugin-signing-token.html)

### 1) Configure Gradle IntelliJ Plugin

In your `build.gradle.kts`, add:

```kotlin
plugins {
  kotlin("jvm") version "1.8.21"
  id("org.jetbrains.intellij") version "1.12.1"
}

intellij {
  version.set("2025.1")
  type.set("WS")               // WS = WebStorm; IC = IntelliJ IDEA
  pluginName.set("CSS Variables Assistant")
  plugins.set(listOf("css"))   // CSS language support
}

tasks {
  publishPlugin {
    token.set(System.getenv("PUBLISH_TOKEN"))
  }
}
