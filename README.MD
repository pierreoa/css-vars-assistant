# CSS Variables Assistant  🔧🎨

**Version:** 1.5.0<br/>
**Compatible IDEs:** WebStorm / IntelliJ Ultimate 2024.1+ (and 2025.1+)  
**Author:** Stian Larsen  
**Repo:** [github.com/Stianlars1/css-vars-assistant](https://github.com/Stianlars1/css-vars-assistant)

**Links:** 
- [Official Plugin Website](https://www.css-variables-assistant.dev)
- [Official JetBrains Marketplace page](https://plugins.jetbrains.com/plugin/27392-css-variables-assistant)
- [Stian Larsen](https://stianlarsen.com)


Supercharge your CSS custom properties and preprocessor variables in JetBrains IDEs with advanced autocomplete, documentation, and debugging tools.

---

## ✨ What's new in 1.5.0

| Category                    | Highlights                                                                                      |
|-----------------------------|-------------------------------------------------------------------------------------------------|
| **📚 IntelliJ 2024.1+ docs** | Full support for the new documentation API with rich popups and fallback for older IDEs.        |
| **📊 Dynamic docs table** | Docs auto-add *px Eq.*, *Hex* and *WCAG* columns whenever the value type warrants it. |
| **↗ Derived var indicator**  | Recursive/derived variable completions now show a special indicator (↗).                        |
| **🔎 Debug import resolution**| Improved DebugImportResolution helper for easier tracing of variable origins and import chains. |
| **🧠 Context/cascade logic** | Completions and docs now always use the last value per context (CSS cascade honored).           |
| **⚡ Robustness & accuracy** | Smarter context labels, improved media query parsing, color parsing, and documentation rendering. |
| **🚀 Performance & refactors**| Faster, more maintainable codebase with centralized index versioning and better caching.        |

[Full Changelog →](https://github.com/Stianlars1/css-vars-assistant/blob/main/CHANGELOG.md)

---

## 🏅 Previous highlights

| Category                      | Highlights                                                                                  |
|-------------------------------|---------------------------------------------------------------------------------------------|
| **🛠️ Preprocessor index**      | LESS/SCSS variables are now indexed for instant lookup and resolution.                      |
| **🔍 Smarter var() detection** | Completions work even while typing incomplete `var(` and resolve aliases more quickly.       |
| **📏 Value-based sorting**     | Variable completions are sorted by their numeric value (asc/desc, configurable).            |
| **📐 Pixel equivalents in docs**| Documentation now shows the pixel equivalent for rem/em/%/vh/vw/pt values.                  |
| **🧠 Context sorting**         | Contexts (Default, Light, Dark, min/max-width, etc.) are now ranked logically.             |
| **⚡ Performance & robustness**| Race conditions and duplicate completions fixed, smarter scope caching, better error handling.|
| **🧪 Test coverage**           | Extensive automated tests for value handling, context ranking, and pixel conversion.        |

---

## Features

- **Instant variable lookup:** Blazing-fast completions and documentation for `CSS`, `SCSS`, `SASS` and `LESS` variables.
- **Smart autocomplete:** Context-aware suggestions for `var(--…)`, `@less`, and `$scss` with value/context-based sorting.
- **Rich documentation popups:**
    - Value tables (with px equivalents for rem/em/%/vh/vw/pt)
    - Context labels (Default, Dark, min-width, etc.)
    - Color swatches and contrast info
    - Dynamic columns: *px Eq.*, *Hex*, *WCAG* appear only when relevant
- **JSDoc-style comments:** Auto-parsing and display of `@name`, `@description`, and `@example`.
- **Advanced import resolution:** Follows and indexes imports across `CSS`, `SCSS`, `SASS` & `LESS`.
- **Debugging tools:** Trace variable origins and import chains visually.
- **Configurable sorting:** Completion list sorted by value _(ascending/descending)_.
- **Works everywhere:** `CSS`, `SCSS`, `SASS`, `LESS`.

---

## Configuration

Open **Settings → Tools → CSS Variables Assistant**

| Option | Effect                                                                        |
|--------|-------------------------------------------------------------------------------|
| **Show context values** | Display dark/light & media-query variants                                     |
| **Allow IDE built-in completions** | Fall back to IntelliJ suggestions for misses                                  |
| **Indexing scope** | *Project only* • *Project + imports* (experimental) • *Full global* (default) |
| **Max @import depth** | 1 – 20 (default 20)                                                           |
| **Completion sorting** | Ascending or descending value order                                           |
| **🔄 Re-index Now** | Flush caches and rebuild the variable index immediately                       |

---

## 🐞 Debugging import chains

Need to verify *exactly* which files your `@import`s resolve to?
1. **Right-click** any *.css / .scss / .sass / .less* file in the Project view <br>
   *or* inside the editor.
2. Select **“Debug CSS Import Resolution”**.
3. A dialog shows:
    * full import tree (`@import` depth-limited)
    * circular-reference warnings
    * variable counts per file  
      A copy is also written to the IDE Log for later inspection.

> 💡 Tip You can keep the dialog open while you edit – rerun the action to refresh.

## Troubleshooting

| Symptom | Resolution |
|---------|------------|
| Variable shows `@lessVar` instead of real colour | Click **Re-index Now**, wait for *Updating indexes…* to finish. |
| Completions missing after changing scope | Re-index or restart IDE (button normally suffices). |
| Slow indexing | Use *Project + imports*, lower max-depth, exclude large folders. |
| Need to see where a variable came from | Right-click file → **Debug CSS Import Resolution**. |

---

## Building & Testing Locally

```bash
git clone https://github.com/stianlars1/css-vars-assistant.git
cd css-vars-assistant
./gradlew clean buildPlugin       # produces ZIP under build/distributions
./gradlew runIde                  # start sandbox IDE
```
