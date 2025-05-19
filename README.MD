# CSS Variables Assistant

**Version**: 1.1.0  
**IDE**: WebStorm / IntelliJ 2025.1+  
**Author**: Stian Larsen ([GitHub](https://github.com/stianlars1/css-vars-assistant))

Enhances CSS Custom Properties with:

- **Context‑aware autocomplete** inside <code>var(--…)</code>
- **Quick Documentation** (<kbd>Ctrl</kbd>+<kbd>Q</kbd> / hover) showing value table, description, examples &amp; colour swatches
- **JSDoc‑style comment tags**: <code>@name</code>, <code>@description</code> (or <code>@desc</code>), <code>@example</code>
- **Smartly ordered suggestions**: Default/Light → Dark → desktop‑first break‑points → mobile‑first → other media
- **Works in**: CSS, SCSS, SASS, LESS, JavaScript / TypeScript, and JSX / TSX files

## v 1.1.0 Highlights

- **Alias resolution** – completions &amp; docs follow <code>var(--alias)</code> chains and show the real value.
- **Duplicate squashing** – identical <em>(context + value)</em> rows collapse into one.
- **Light counts as primary** – <code>prefers-color-scheme: light</code> is treated the same as <code>default</code>.
- **Smart documentation order** – Default/Light → Dark → <code>max‑width</code> (large→small) → <code>min‑width</code> (small→large) → other media.
- **Dual colour swatches** – always displayed for light/dark pairs, even when they alias to identical hues.
- **Stricter index filter** – ignores templates / non‑style files (<code>.txt</code>, <code>.md</code>, etc.).
- **Settings page** – toggle context value display under **Settings → CSS Variables Assistant**.

---

## Installation (locally)

1. `git clone https://github.com/stianlars1/css-vars-assistant.git`
2. `cd css-vars-assistant`
3. `./gradlew clean buildPlugin`
4. Test the plugin by either
   1. **Plugins → ⚙️ → Install Plugin from Disk…** → select `build/distributions/css-vars-assistant-1.1.0.zip`, or
   2. `./gradlew runIde` (start a sandbox IDE with the plugin loaded).

---

## Publishing to JetBrains Marketplace

**Prerequisites**

- JetBrains account with **Plugin Developer** role
- [Marketplace publish token (PUBLISH_TOKEN)](https://plugins.jetbrains.com/docs/marketplace/generate-plugin-signing-token.html)
