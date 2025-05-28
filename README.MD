# CSS Variables Assistant  🔧🎨

**Version:** 1.3.0  
**Compatible IDEs:** WebStorm / IntelliJ Ultimate **2025.1+**  
**Author:** Stian Larsen ([GitHub](https://github.com/stianlars1))

Boosts CSS Custom Properties with smart completion, rich documentation and full pre-processor support.

---

## ✨ What’s new in 1.3.0

| Category | Highlights |
|----------|------------|
| **🚀 Value resolution** | Follows chained `@less`, `$scss` and alias `var(--foo)` references – even across imported packages – until it reaches a literal value. |
| **📦 Import cache** | Remembers every file reached via `@import`, giving instant look-ups after the first pass. |
| **🔄 Re-index Now button** | One-click rebuild in **Settings → Tools → CSS Variables Assistant** – no more *Invalidate Caches / Restart*. |
| **🧭 Debug action** | *Debug CSS Import Resolution* (context menu) prints the full import chain and indicates skipped circular references. |
| **🛡️ Stability** | Race conditions fixed, proper cancellation handling, fewer `ProcessCanceledException`s in large repos. |

See full details in [`CHANGELOG.MD`](./CHANGELOG.MD).

---

## Core Features

* **Context-aware completion** inside `var(-- … )`
* **Quick Documentation** (<kbd>Ctrl Q</kbd> / hover) with table, swatches, description & examples
* **@import traversal** for CSS, SCSS, SASS & LESS with smart extension inference
* **Recursive var() & pre-processor alias resolution**
* **JSDoc-style tags** `@name`, `@description`, `@example`
* Works inside **CSS / SCSS / SASS / LESS / JS / TS / JSX / TSX**

---

## Configuration

Open **Settings → Tools → CSS Variables Assistant**

| Option | Effect |
|--------|--------|
| **Indexing scope** | *Project only* • *Project + imports* (recommended) • *Full global* |
| **Max @import depth** | 1 – 10 (default 10) |
| **Show context values** | Display dark/light & media-query variants |
| **Allow IDE built-in completions** | Fall back to IntelliJ suggestions for misses |
| **🔄 Re-index Now** | Flush caches and rebuild the variable index immediately |

---

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
