# CSS Variables Assistant **(--)** Changelog

All notable changes to this project will be documented in this file.

## [1.0.2] – 2025‑05‑19

### Added
- **Settings page:** Plugin is now user-configurable (toggle context-based variable values).
- **Smarter completions:**
  - Dual color swatch if a variable has both light and dark color values.
  - All original value syntaxes shown (e.g., `#fff`, `hsl(0 0% 100%)`, `rgb(…))`).
  - Context-aware completions: e.g. “🌙” for dark mode, no overlays.
- **Color swatches:**
  - Now supports **shadcn/ui** color variables format, `--foreground: 0 0% 100%;`.
- **Robust comment parsing:** All major CSS comment styles now supported for variable docs (JSDoc, plain, single-line, multiline).
- **Context tracking:** Indexer now properly tracks context (media queries, dark/light, nested queries, etc).
- **Documentation enhancements:**
  - Multi-context variables shown as a table.
  - Color swatches always use original value syntax.
  - WebAIM contrast checker link for color variables.


### Changed
- **Consistent UI:**
  - No overlay/context icons on completion swatch—context info is right-aligned only.
  - Completions/docs now fully respect user settings.

### Fixed
- **Comment parsing bugs:** One-line, multi-line, and mixed-format doc-comments now always recognized.
- **Threading issues:** Docs and completions no longer trigger read-access warnings.



## [1.0.1] – 2025‑05‑18
### Added
- logo for the plugin
### Changed
- Alphabetical sorting when all variable values are non‑numeric
- Miscellaneous bugfixes and performance tweaks
### Fixed
- Completions now trigger immediately after typing `var(--`
- Completions now trigger without needing `--` inside the `var()`
- Completions now trigger when clicking inside an existing `var(--your-var)`

## [1.0.0] – 2025‑05‑16
### Added
- Production release: **v1.0.0**
- Autocomplete inside `var(…)`, sorted by CSS value (high → low)
- Hover & Quick‑Docs popups with color swatch and comment parsing
- Removed `@value` override. tags **always** shows the **real** CSS **value**

## [0.1.2 → 1.0.0] – 2025‑05‑15
### Changed
- Flipped suggestion order: largest values first
- Improved context checks and minor bugfixes
- Improved performance and reduced memory usage
- Improved documentation and code comments
