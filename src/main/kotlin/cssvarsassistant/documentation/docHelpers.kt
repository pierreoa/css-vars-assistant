package cssvarsassistant.documentation

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.util.indexing.FileBasedIndex
import cssvarsassistant.completion.CssVarCompletionCache
import cssvarsassistant.documentation.v2.ENTRY_SEP
import cssvarsassistant.index.CSS_VARIABLE_INDEXER_NAME
import cssvarsassistant.index.DELIMITER
import cssvarsassistant.settings.CssVarsAssistantSettings
import cssvarsassistant.util.PreprocessorUtil
import cssvarsassistant.util.ScopeUtil

/* ────────────────────────────────────────────────────────────────────────── */

data class ResolutionInfo(val original: String, val resolved: String, val steps: List<String> = emptyList())

private val LOG = Logger.getInstance("cssvarsassistant.docHelpers")

/* ────────────────────── resolver ─────────────────────────────── */
fun resolveVarValue(
    project: Project,
    raw: String,
    visited: Set<String> = emptySet(),
    depth: Int = 0,
    steps: List<String> = emptyList()
): ResolutionInfo {
    println("\n== resolveVarValue ==")
    val settings = CssVarsAssistantSettings.getInstance()
    if (depth > settings.maxImportDepth) return ResolutionInfo(raw, raw, steps)

    try {
        ProgressManager.checkCanceled()

        Regex("""var\(\s*(--[\w-]+)\s*\)""").find(raw)?.let { m ->
            val ref = m.groupValues[1]
            if (ref !in visited) {
                println("\ninside if")
                val newSteps = steps + "var($ref)"
                val cssScope = ScopeUtil.effectiveCssIndexingScope(project, settings)
                val entries = FileBasedIndex.getInstance()
                    .getValues(CSS_VARIABLE_INDEXER_NAME, ref, cssScope)
                    .flatMap { it.split(ENTRY_SEP) }
                    .filter { it.isNotBlank() }

                val defVal = entries.mapNotNull {
                    val p = it.split(DELIMITER, limit = 3)
                    if (p.size >= 2) p[0] to p[1] else null
                }.let { list ->
                    list.find { it.first == "default" }?.second ?: list.firstOrNull()?.second
                }
                println("\ndefVal: \n$defVal")
                println("\nvisited: \n$visited")
                println("\nsteps: \n$steps")
                println("\nnewSteps: \n$newSteps")
                println("\nref: \n$ref")
                println("\nentries: \n$entries")

                if (defVal != null)
                    return resolveVarValue(project, defVal, visited + ref, depth + 1, newSteps)
            }
            println("\ninside else")
            return ResolutionInfo(raw, raw, steps)
        }

        val preprocessorMatch = Regex("""^[\s]*[@$]([\w-]+)$""").find(raw.trim())
        println("\npreprocessorMatch: \n$preprocessorMatch")
        if (preprocessorMatch != null) {
            val varName = preprocessorMatch.groupValues[1]
            val prefix = if (raw.contains("@")) "@" else "$"
            val currentScope = ScopeUtil.currentPreprocessorScope(project)

            // **KEEP**: Check cache first (important for performance!)
            CssVarCompletionCache.get(project, varName, currentScope)?.let {
                val cachedValue = ResolutionInfo(raw, it, steps + "$prefix$varName")
                println("\nCache hit for variable: $varName")
                println("Cached Value:  $cachedValue")
                return cachedValue
            }

            // **FIXED**: Get resolution info with steps from preprocessor
            val resolution = findPreprocessorVariableValue(project, varName, steps)
            println("\nresolution: \n$resolution")
            if (resolution != null && resolution.resolved != raw) {
                println("\nCache miss for variable: $varName")
                // **KEEP**: Store in cache for future lookups
                CssVarCompletionCache.put(project, varName, currentScope, resolution.resolved)
                return ResolutionInfo(
                    original = raw,
                    resolved = resolution.resolved,
                    steps = resolution.steps  // **This now contains the full chain!**
                )
            }
        }

        return ResolutionInfo(raw, raw, steps)
    } catch (e: ProcessCanceledException) {
        throw e
    } catch (e: Exception) {
        LOG.warn("Error resolving variable value", e)
        return ResolutionInfo(raw, raw, steps)
    }
}


/* ────────────────────── Recursive Preprocessor finder ──────────────────────── */
fun findPreprocessorVariableValue(
    project: Project,
    varName: String,
    currentSteps: List<String> = emptyList()
): ResolutionInfo? {
    return try {
        println("\nfindPreprocessorVariableValue: $varName")
        val freshScope = ScopeUtil.currentPreprocessorScope(project)
        val resolution = PreprocessorUtil.resolveVariableWithSteps(
            project,
            varName,
            freshScope,
            emptySet(),
            currentSteps
        )
        println("\nResolution result: $resolution")
        return resolution
    } catch (e: ProcessCanceledException) {
        throw e
    } catch (e: Exception) {
        LOG.warn("Failed to resolve @$varName", e)
        null
    }
}

/* ────────────────────── ** extractor ──────────────────────── */

fun extractCssVariableName(element: PsiElement): String? {
    // Always check element validity first
    if (!element.isValid) return null

    // Safe handling of potentially null text
    val elementText = element.text
    if (elementText?.trim()?.startsWith("--") == true) {
        return elementText.trim()
    }

    // Check parent element safely
    val parent = element.parent
    if (parent?.isValid == true) {
        val parentText = parent.text
        if (parentText != null) {
            return Regex("""var\(\s*(--[\w-]+)\s*\)""").find(parentText)?.groupValues?.get(1)
        }
    }

    return null
}

/* ─────────────────────── other helpers ───────────────────────── */
fun lastLocalValueInFile(fileText: String, varName: String): String? =
    Regex("""\Q$varName\E\s*:\s*([^;]+);""")
        .findAll(fileText)
        .map { it.groupValues[1].trim() }
        .lastOrNull()


