# CSS Variables Assistant

**Version**: 1.0.1
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
3. `./gradlew clean buildPlugin`
4. Test plugin by either:
   1. In WebStorm: **Plugins → ⚙️ → Install Plugin from Disk...** → select `build/distributions/css-vars-assistant-1.0.0.zip`
   or
   2. From Plugin terminal: `./gradlew runIde` (this will start a new instance of WebStorm with the plugin loaded)
---

## Publishing to JetBrains Marketplace

**Prerequisites**
- A JetBrains Account with “Plugin Developer” role
- [Token for plugin repository (PUBLISH_TOKEN)](https://plugins.jetbrains.com/docs/marketplace/generate-plugin-signing-token.html)