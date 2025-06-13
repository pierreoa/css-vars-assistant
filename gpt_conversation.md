### Conversation digest

**Overall goal:**  
Build a rock-solid IDEA/IntelliJ plugin that:
1. **Shows smart hover-docs** for CSS/LESS/SCSS variables (incl. Sass @use)
2. **Offers ranked completion** inside `var(-- … )`
3. Respects **CSS cascade** (last declaration in *each* context wins)
4. Handles **pre-processor refs**, resolution chains, px-conversion & colour swatches
5. Packs extra insight: usage stats, references, related vars, file locations.

---

#### Key fixes already applied

| Area               | Change                                                                                                                           | Why it matters                        |
| ------------------ | -------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------- |
| **Pixel column**   | Arrow chain removed; resolved size now placed in the existing “Pixel Equivalent” cell.                                           | Cleaner table.                        |
| **Font sizing**    | Inline CSS (`body {11px}` / `table {10px}` / wrap)                                                                               | Prevent overflow.                     |
| **Cascade logic**  | `collapsed = parsed.groupBy{ctx}.mapValues{list.last()}`                                                                         | Last declaration wins per context.    |
| **Completion**     | Same “last-in-context” rule by replacing `list.first()` → `list.last()`; main value = last from *default* context.               | Suggests `25rem (+3)` in the example. |
| **contextLabel()** | Robust parsing: detects light/dark/reduced-motion and min/max/combined widths → returns “≤768px”, “300-768px”, etc.              | No more plain “media”.                |
| **Filter removal** | Dropped the size/number/colour triage (`ValueUtil.isSizeValue …`) because cascade order now solely decides which value is shown. |                                       |

---

#### New features added

* **Usage stats** – counts files & total `var(--x)` hits.
* **Dependencies** – lists variables referenced (recursive, deduped).
* **Related vars** – heuristics on name parts, max 8 suggestions.
* **File locations** – up to 5 defining files.
* All four blocks injected into the doc HTML right after the values table.

Helper methods: `getUsageStats()`, `findDependencies()`, `findRelatedVariables()`, `getFileLocations()` (see code).

---

#### Remaining TODO / sanity checklist

1. **Regex tweaks** – make sure `contextLabel` still matches exotic media combos (`print`, `only screen`, etc.).
2. **Performance** – large projects might need caching for file-io heavy helpers.
3. **Unit tests** for cascade, context labels, and new stats helpers.
4. **Settings toggles** – let users hide Usage / Related / Files blocks if they feel noisy.
5. **Dark-mode contrast** – maybe auto-generate contrast badge for both light & dark if colours differ by context.
