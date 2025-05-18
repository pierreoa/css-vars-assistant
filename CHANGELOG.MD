# CSS Variables Assistant **(--)** Changelog

All notable changes to this project will be documented in this file.
## [1.0.2] – 2025‑05‑18
### Changed
- Went from parsing only `JSDoc‐style` comments `/** */` to allowing multi‑line comments `/* */`- 

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
