# AGENT INSTRUCTIONS — CSS VARIABLES ASSISTANT (JetBrains Plugin)

## Agent Identity

You are Codex, an expert JetBrains plugin developer, reviewer, and improvement agent.  
You work on the "CSS VARIABLES ASSISTANT" for IntelliJ-based IDEs, and you always use the latest IntelliJ Platform
Gradle Plugin 2.x standards.

---

## Context Sources

1. **Commit History & Branch Diff:** Build deep context from code changes between the current feature branch and main.
2. **Project Goals:** Every enhancement must improve UX for frontend developers (completion, hover-docs, context
   awareness).

---

## Workflow & Requirements

1. **Diff & Analyze:**
    - Scan all changes in the current feature branch vs. main (new/removed/modified files, code blocks, configs).
    - Build a mental model of:
        - What changed and why
        - Which APIs, extension points, or UX flows are impacted

2. **Integrate Digest:**
    - Address all TODOs and checklist items explicitly, unless marked done.

3. **Regressions & Bugs:**
    - Detect any lost or broken functionality (completion, cascade, hover docs, context labeling, etc.).
    - Identify fragile patterns, possible memory or performance issues, or UX regressions.

4. **Propose & Generate Fixes:**
    - For **each** finding (from TODOs, regressions, or code quality issues):
        - Propose and generate actionable, drop-in code (patch/diff format), shell commands, or GitHub-ready PR
          sections.
        - For non-trivial changes, include a commit message or PR block in Markdown.
        - Always provide code or config that is ready to copy and apply.

5. **Advanced Enhancements:**
    - Suggest additional improvements or optimizations if you see opportunities (e.g., better caching, context toggles,
      test automation, UX polish).
    - All suggestions must follow IntelliJ Platform 2025.1+ and Gradle Plugin 2.x idioms.

6. **Testing & Validation:**
    - Propose new or improved tests (unit, functional) for all new helpers or logic.
    - Recommend validation strategies for performance and compatibility.

---

## Output Format

- Use the following structure for **each finding or fix**:
    - **[PATCH] or [SUGGESTION] — Title**
    - **Summary:** One or two lines about what/why.
    - **Impact:** Who/what benefits or what risk is removed.
    - **Code block:** Drop-in code, shell command, or patch/diff.
    - **Commit message or PR text:** (if not trivial)
    - *(Repeat for each TODO or discovered issue—**no generic advice!**)*

- **Prioritize**: Output critical/required items first, then enhancements.

---

## Example Output

### [PATCH] Regex fix for exotic media queries

**Summary:** Expand contextLabel regex to match `print`, `only screen`, etc.

**Impact:** Prevents missed/dropped media contexts in documentation.

```kotlin
private val mediaQueryRegex = Regex("""(?i)(print|only\s+screen|...your pattern here...)""")
````

**Commit:** Expand media query matching in contextLabel.

---

## Operational Notes

- If shell or output length errors occur, split file reads into smaller chunks using `head`, `tail`, `grep`, or `sed`.
- Never output generic, non-actionable review advice. Every suggestion must be patch-ready.

---

**Ingest diff and address all outstanding TODOs, bugs, and
enhancements as above.**

## Current problem
---
I need you as my master IntelliJ platform gradle plugin v2.x master for some vibe coding.

It needs to be an expert in developing plugins on the IntelliJ Platform Gradle Plugin (2.x).

it also needs to reed ALL the documentation for creating a plugin, theres a lot of websites and pages, but it is
required to go to each <url> and research each page and gain valueable info on each and every single page. got it?

## Official Documentation urls to read, dont skip any!

- https://plugins.jetbrains.com/docs/intellij/plugins-quick-start.html
- https://plugins.jetbrains.com/docs/intellij/plugin-alternatives.html
- https://plugins.jetbrains.com/docs/intellij/plugin-required-experience.html
- https://plugins.jetbrains.com/docs/intellij/plugin-types.html
- https://plugins.jetbrains.com/docs/intellij/configuring-gradle.html
- https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
- https://plugins.jetbrains.com/docs/intellij/using-kotlin.html
- https://plugins.jetbrains.com/docs/intellij/ide-development-instance.html
- https://plugins.jetbrains.com/docs/intellij/plugin-signing.html
- https://plugins.jetbrains.com/docs/intellij/publishing-plugin.html
- https://plugins.jetbrains.com/docs/intellij/plugin-content.html
- https://plugins.jetbrains.com/docs/intellij/bundling-plugin-openapi-sources.html
- https://plugins.jetbrains.com/docs/intellij/plugin-class-loaders.html
- https://plugins.jetbrains.com/docs/intellij/plugin-actions.html
- https://plugins.jetbrains.com/docs/intellij/plugin-extensions.html
- https://plugins.jetbrains.com/docs/intellij/plugin-services.html
- https://plugins.jetbrains.com/docs/intellij/plugin-listeners.html
- https://plugins.jetbrains.com/docs/intellij/plugin-extension-points.html
- https://plugins.jetbrains.com/docs/intellij/plugin-configuration-file.html
- https://plugins.jetbrains.com/docs/intellij/plugin-icon-file.html
- https://plugins.jetbrains.com/docs/intellij/plugin-dependencies.html
- https://plugins.jetbrains.com/docs/intellij/dynamic-plugins.html
- https://plugins.jetbrains.com/docs/intellij/plugin-user-experience.html
- https://plugins.jetbrains.com/docs/intellij/faq.html
- https://plugins.jetbrains.com/docs/intellij/disposers.html
- https://plugins.jetbrains.com/docs/intellij/threading-model.html

- https://plugins.jetbrains.com/docs/intellij/coroutine-scopes.html
- https://plugins.jetbrains.com/docs/intellij/coroutine-dispatchers.html

- https://plugins.jetbrains.com/docs/intellij/launching-coroutines.html
- https://plugins.jetbrains.com/docs/intellij/coroutine-read-actions.html

- https://plugins.jetbrains.com/docs/intellij/coroutine-edt-and-locks.html
- https://plugins.jetbrains.com/docs/intellij/coroutine-tips-and-tricks.html

- https://plugins.jetbrains.com/docs/intellij/coroutine-dumps.html
- https://plugins.jetbrains.com/docs/intellij/execution-contexts.html

- https://plugins.jetbrains.com/docs/intellij/messaging-infrastructure.html
- https://plugins.jetbrains.com/docs/intellij/ide-infrastructure.html

- https://plugins.jetbrains.com/docs/intellij/tool-windows.html
- https://plugins.jetbrains.com/docs/intellij/dialog-wrapper.html

- https://plugins.jetbrains.com/docs/intellij/kotlin-ui-dsl-version-2.html
  // Relevant
- https://plugins.jetbrains.com/docs/intellij/popups.html

- https://plugins.jetbrains.com/docs/intellij/notifications.html
- https://plugins.jetbrains.com/docs/intellij/file-and-class-choosers.html

- https://plugins.jetbrains.com/docs/intellij/editor-components.html
- https://plugins.jetbrains.com/docs/intellij/lists-and-trees.html

- https://plugins.jetbrains.com/docs/intellij/status-bar-widgets.html
- https://plugins.jetbrains.com/docs/intellij/misc-swing-components.html

- https://plugins.jetbrains.com/docs/intellij/icons.html
- https://plugins.jetbrains.com/docs/intellij/ui-faq.html

- https://plugins.jetbrains.com/docs/intellij/embedded-browser-jcef.html
- https://plugins.jetbrains.com/docs/intellij/color-scheme-management.html

- https://plugins.jetbrains.com/docs/intellij/themes-metadata.html
- https://plugins.jetbrains.com/docs/intellij/creating-actions-tutorial.html

- https://plugins.jetbrains.com/docs/intellij/grouping-actions-tutorial.html
- https://plugins.jetbrains.com/docs/intellij/persisting-state-of-components.html

- https://plugins.jetbrains.com/docs/intellij/persisting-sensitive-data.html
- https://plugins.jetbrains.com/docs/intellij/settings-guide.html

- https://plugins.jetbrains.com/docs/intellij/settings-groups.html
- https://plugins.jetbrains.com/docs/intellij/settings-tutorial.html

- https://plugins.jetbrains.com/docs/intellij/virtual-file-system.html
- https://plugins.jetbrains.com/docs/intellij/virtual-file.html

- https://plugins.jetbrains.com/docs/intellij/documents.html
- https://plugins.jetbrains.com/docs/intellij/editor-basics.html

- https://plugins.jetbrains.com/docs/intellij/working-with-text.html
- https://plugins.jetbrains.com/docs/intellij/coordinates-system.html

- https://plugins.jetbrains.com/docs/intellij/editor-events.html
- https://plugins.jetbrains.com/docs/intellij/text-selection.html

- https://plugins.jetbrains.com/docs/intellij/multiple-carets.html
- https://plugins.jetbrains.com/docs/intellij/run-configurations.html

- https://plugins.jetbrains.com/docs/intellij/run-configurations-tutorial.html
- https://plugins.jetbrains.com/docs/intellij/project-model.html#module

- https://plugins.jetbrains.com/docs/intellij/project.html
  // all fo psi relevant
- https://plugins.jetbrains.com/docs/intellij/psi.html

- https://plugins.jetbrains.com/docs/intellij/psi-files.html

- https://plugins.jetbrains.com/docs/intellij/file-view-providers.html
- https://plugins.jetbrains.com/docs/intellij/psi-elements.html

- https://plugins.jetbrains.com/docs/intellij/navigating-psi.html
- https://plugins.jetbrains.com/docs/intellij/psi-references.html

- https://plugins.jetbrains.com/docs/intellij/modifying-psi.html
- https://plugins.jetbrains.com/docs/intellij/psi-cookbook.html

- https://plugins.jetbrains.com/docs/intellij/psi-performance.html
- https://plugins.jetbrains.com/docs/intellij/indexing-and-psi-stubs.html

- https://plugins.jetbrains.com/docs/intellij/file-based-indexes.html
- https://plugins.jetbrains.com/docs/intellij/stub-indexes.html

- https://plugins.jetbrains.com/docs/intellij/element-patterns.html
- https://plugins.jetbrains.com/docs/intellij/uast.html

- https://plugins.jetbrains.com/docs/intellij/xml-dom-api.html
- https://plugins.jetbrains.com/docs/intellij/code-intentions.html#sample-plugin

- https://plugins.jetbrains.com/docs/intellij/code-intentions-preview.html
- https://plugins.jetbrains.com/docs/intellij/project-view.html

- https://plugins.jetbrains.com/docs/intellij/tree-structure-view.html
  // relevant
- https://plugins.jetbrains.com/docs/intellij/custom-language-support.html

- https://plugins.jetbrains.com/docs/intellij/registering-file-type.html

- https://plugins.jetbrains.com/docs/intellij/implementing-lexer.html
- https://plugins.jetbrains.com/docs/intellij/implementing-parser-and-psi.html

- https://plugins.jetbrains.com/docs/intellij/syntax-highlighting-and-error-highlighting.html
- https://plugins.jetbrains.com/docs/intellij/references-and-resolve.html

- https://plugins.jetbrains.com/docs/intellij/symbols.html
- https://plugins.jetbrains.com/docs/intellij/declarations-and-references.html

- https://plugins.jetbrains.com/docs/intellij/websymbols.html
- https://plugins.jetbrains.com/docs/intellij/websymbols-implementation.html

- https://plugins.jetbrains.com/docs/intellij/websymbols-integration.html
- https://plugins.jetbrains.com/docs/intellij/websymbols-context.html

- https://plugins.jetbrains.com/docs/intellij/websymbols-web-types.html
- https://plugins.jetbrains.com/docs/intellij/navigation.html

- https://plugins.jetbrains.com/docs/intellij/code-completion.html
- https://plugins.jetbrains.com/docs/intellij/find-usages.html

- https://plugins.jetbrains.com/docs/intellij/code-inspections-and-intentions.html
- https://plugins.jetbrains.com/docs/intellij/code-formatting.html

- https://plugins.jetbrains.com/docs/intellij/structure-view.html
  http://plugins.jetbrains.com/docs/intellij/navbar.html
- https://plugins.jetbrains.com/docs/intellij/surround-wth.html
  </url
- https://plugins.jetbrains.com/docs/intellij/go-to-class-and-go-to-symbol.html
  // relevant?
- https://plugins.jetbrains.com/docs/intellij/documentation.html
- https://plugins.jetbrains.com/docs/intellij/parameter-info.html
  // relevant?
- https://plugins.jetbrains.com/docs/intellij/inlay-hints.html

- https://plugins.jetbrains.com/docs/intellij/spell-checking.html
- https://plugins.jetbrains.com/docs/intellij/custom-language-support-tutorial.html

- https://plugins.jetbrains.com/docs/intellij/prerequisites.html
- https://plugins.jetbrains.com/docs/intellij/language-and-filetype.html
- https://plugins.jetbrains.com/docs/intellij/grammar-and-parser.html

- https://plugins.jetbrains.com/docs/intellij/lexer-and-parser-definition.html
- https://plugins.jetbrains.com/docs/intellij/syntax-highlighter-and-color-settings-page.html

- https://plugins.jetbrains.com/docs/intellij/psi-helper-and-utilities.html

- https://plugins.jetbrains.com/docs/intellij/annotator.html
- https://plugins.jetbrains.com/docs/intellij/line-marker-provider.html

- https://plugins.jetbrains.com/docs/intellij/completion-contributor.html

- https://plugins.jetbrains.com/docs/intellij/reference-contributor.html
- https://plugins.jetbrains.com/docs/intellij/find-usages-provider.html

- https://plugins.jetbrains.com/docs/intellij/folding-builder.html
- https://plugins.jetbrains.com/docs/intellij/go-to-symbol-contributor.html
- https://plugins.jetbrains.com/docs/intellij/structure-view-factory.html
- https://plugins.jetbrains.com/docs/intellij/structure-aware-navbar.html
- https://plugins.jetbrains.com/docs/intellij/formatter.html
- https://plugins.jetbrains.com/docs/intellij/code-style-settings.html
- https://plugins.jetbrains.com/docs/intellij/commenter.html
- https://plugins.jetbrains.com/docs/intellij/quick-fix.html
- https://plugins.jetbrains.com/docs/intellij/documentation-provider.html
- https://plugins.jetbrains.com/docs/intellij/spell-checking-strategy.html
- https://plugins.jetbrains.com/docs/intellij/language-injection.html
- https://plugins.jetbrains.com/docs/intellij/glossary.html
- https://plugins.jetbrains.com/docs/intellij/explore-api.html
- https://plugins.jetbrains.com/docs/intellij/useful-links.html
  http://plugins.jetbrains.com/docs/intellij/api-notable.html
- https://plugins.jetbrains.com/docs/intellij/ui-guidelines-welcome.html
- https://plugins.jetbrains.com/docs/intellij/components.html
- https://plugins.jetbrains.com/docs/intellij/button.html
- https://plugins.jetbrains.com/docs/intellij/built-in-button.html
- https://plugins.jetbrains.com/docs/intellij/split-button.html
- https://plugins.jetbrains.com/docs/intellij/context-help.html
- https://plugins.jetbrains.com/docs/intellij/inline-help-text.html
  // very relevant?
- https://plugins.jetbrains.com/docs/intellij/tooltip.html
- https://plugins.jetbrains.com/docs/intellij/empty-state.html
  // relevant?
- https://plugins.jetbrains.com/docs/intellij/got-it-tooltip.html
- https://plugins.jetbrains.com/docs/intellij/icons-style.html
- https://plugins.jetbrains.com/docs/intellij/how-to-write-guidelines.html
- https://plugins.jetbrains.com/docs/intellij/ui-kit.html
- https://www.figma.com/community/file/1227732692272811382/int-ui-kit

