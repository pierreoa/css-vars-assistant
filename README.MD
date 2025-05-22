# CSS Variables Assistant

**Version**: 1.2.0  
**IDE**: WebStorm / IntelliJ 2025.1+  
**Author**: Stian Larsen ([GitHub](https://github.com/stianlars1/css-vars-assistant))

Enhances CSS Custom Properties with:

- **Context‑aware autocomplete** inside <code>var(--…)</code>
- **Quick Documentation** (<kbd>Ctrl</kbd>+<kbd>Q</kbd> / hover) showing value table, description, examples &amp; colour swatches
- **Advanced @import resolution** with configurable scope and depth control
- **JSDoc‑style comment tags**: <code>@name</code>, <code>@description</code> (or <code>@desc</code>), <code>@example</code>
- **Multi‑preprocessor support**: CSS, SCSS, SASS, LESS with intelligent extension resolution
- **Works in**: CSS, SCSS, SASS, LESS, JavaScript / TypeScript, and JSX / TSX files

## v 1.2.0 Highlights

- **🚀 Advanced @import resolution** – intelligently resolves <code>@import</code> statements across all supported preprocessor formats.
- **📦 Smart node_modules handling** – properly resolves scoped packages like <code>@company/package/path</code> and handles relative imports.
- **⚙️ Configurable indexing scope** – choose your indexing strategy:
   - **Project only** – index variables from your project files only
   - **Project + imports** – selectively index <code>@import</code> targets from external packages
   - **Global** – index all CSS variables from entire <code>node_modules</code> (performance impact)
- **🔧 Enhanced settings panel** – fine‑tune plugin behavior under **Settings → Tools → CSS Variables Assistant**.
- **🛠️ Debug tools** – new "Debug CSS Import Resolution" action for troubleshooting import chains.
- **🎯 Multi‑extension support** – automatically tries <code>.css</code>, <code>.scss</code>, <code>.sass</code>, <code>.less</code> when resolving imports.

## Previous Features

- **Alias resolution** – completions &amp; docs follow <code>var(--alias)</code> chains and show the real value.
- **Duplicate squashing** – identical <em>(context + value)</em> rows collapse into one.
- **Light counts as primary** – <code>prefers-color-scheme: light</code> is treated the same as <code>default</code>.
- **Smart documentation order** – Default/Light → Dark → <code>max‑width</code> (large→small) → <code>min‑width</code> (small→large) → other media.
- **Dual colour swatches** – always displayed for light/dark pairs, even when they alias to identical hues.

---

## Configuration

Access plugin settings via **Settings → Tools → CSS Variables Assistant**:

### Indexing Scope Options

**Project files only**  
Only variables defined in your project files are indexed. Fastest option with minimal memory usage.

**Project files + @import resolution** *(recommended)*  
Project files plus selective resolution of `@import` statements to external packages. Only the exact imported files are indexed, not entire node_modules.

**Full global scope**  
Complete indexing of all CSS files in node_modules and libraries. May impact IDE performance with large projects.

### Additional Settings

- **Show context-based variable values** – display different values for light/dark modes, media queries, etc.
- **Allow IDE built-in completions** – fallback to IntelliJ's completions for variables not found by the plugin
- **Maximum @import chain depth** – prevent infinite recursion in import chains (1-10, default: 3)

---

## Troubleshooting

### Debug Import Resolution

Right‑click on any CSS/SCSS/SASS/LESS file and select **"Debug CSS Import Resolution"** to see:
- Which `@import` statements were found
- How each import path was resolved
- Which files were successfully indexed
- Import chain depth and circular dependency detection

### Common Issues

**Variables not appearing in completions:**
1. Check your indexing scope setting
2. Verify `@import` statements use correct syntax
3. Use the debug tool to trace import resolution
4. Try invalidating caches: **File → Invalidate Caches and Restart**

**Performance issues:**
- Switch from "Full global scope" to "Project + imports" indexing
- Reduce maximum import depth setting
- Exclude unnecessary directories from project scope

---

## Installation (locally)

1. `git clone https://github.com/stianlars1/css-vars-assistant.git`
2. `cd css-vars-assistant`
3. `./gradlew clean buildPlugin`
4. Test the plugin by either
   1. **Plugins → ⚙️ → Install Plugin from Disk…** → select `build/distributions/css-vars-assistant-1.2.0.zip`, or
   2. `./gradlew runIde` (start a sandbox IDE with the plugin loaded).

---

## Publishing to JetBrains Marketplace

**Prerequisites**

- JetBrains account with **Plugin Developer** role
- [Marketplace publish token (PUBLISH_TOKEN)](https://plugins.jetbrains.com/docs/marketplace/generate-plugin-signing-token.html)