package cssvarsassistant.documentation.v2

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.util.indexing.FileBasedIndex
import cssvarsassistant.completion.CssVarCompletionCache
import cssvarsassistant.completion.CssVariableCompletion
import cssvarsassistant.documentation.ResolutionInfo
import cssvarsassistant.index.CSS_VARIABLE_INDEXER_NAME
import cssvarsassistant.index.DELIMITER
import cssvarsassistant.model.DocParser
import cssvarsassistant.settings.CssVarsAssistantSettings
import cssvarsassistant.util.PreprocessorUtil
import cssvarsassistant.util.RankUtil.rank
import cssvarsassistant.util.ScopeUtil
import cssvarsassistant.util.ValueUtil
import kotlin.math.roundToInt

val ENTRY_SEP = "|||"

object CssVariableDocumentationService {
    private val logger = Logger.getInstance(CssVariableCompletion::class.java)

    fun generateDocumentation(element: PsiElement, varName: String): String? {
        try {
            val project = element.project
            if (DumbService.isDumb(project)) return null
            ProgressManager.checkCanceled()

            val settings = CssVarsAssistantSettings.getInstance()
            val cssScope = ScopeUtil.effectiveCssIndexingScope(project, settings)

            val rawEntries = FileBasedIndex.getInstance().getValues(CSS_VARIABLE_INDEXER_NAME, varName, cssScope)
                .flatMap { it.split(ENTRY_SEP) }.filter { it.isNotBlank() }

            if (rawEntries.isEmpty()) return null

            val parsed = rawEntries.mapNotNull {
                val parts = it.split(DELIMITER, limit = 3)
                if (parts.size >= 2) {
                    val ctx = parts[0]
                    val resInfo = resolveVarValue(project, parts[1])
                    val comment = parts.getOrElse(2) { "" }
                    Triple(ctx, resInfo, comment)
                } else null
            }

            val collapsed = parsed.groupBy { it.first }.mapValues { (_, list) -> list.last() }.values.toList()

            val sorted = collapsed.sortedWith(
                compareBy(
                    { rank(it.first).first },
                    { rank(it.first).second ?: Int.MAX_VALUE },
                    { rank(it.first).third })
            )

            val docEntry = collapsed.firstOrNull { it.third.isNotBlank() } ?: collapsed.find { it.first == "default" }
            ?: collapsed.first()
            val doc = DocParser.parse(docEntry.third, docEntry.second.resolved)

            val showPixelCol = sorted.any { (_, res, _) ->
                if (!ValueUtil.isSizeValue(res.resolved)) return@any false
                val unit = res.resolved.replace(Regex("[0-9.+\\-]"), "").trim().lowercase()
                val pxVal = ValueUtil.convertToPixels(res.resolved)
                val numericRaw = res.resolved.replace(Regex("[^0-9.+\\-]"), "").toDoubleOrNull() ?: pxVal
                unit != "px" || pxVal.roundToInt() != numericRaw.roundToInt()
            }

            return buildHtmlDocument(varName, doc, sorted, showPixelCol)

        } catch (e: ProcessCanceledException) {
            throw e
        } catch (e: Exception) {
            logger.error("Error generating documentation", e)
            return null
        }
    }

    fun generateHint(element: PsiElement, varName: String): String? {
        val project = element.project
        val settings = CssVarsAssistantSettings.getInstance()
        val scope = ScopeUtil.effectiveCssIndexingScope(project, settings)

        return try {
            val rawEntries = FileBasedIndex.getInstance().getValues(CSS_VARIABLE_INDEXER_NAME, varName, scope)
                .flatMap { it.split(ENTRY_SEP) }.filter { it.isNotBlank() }

            val resolved = rawEntries.mapNotNull {
                val parts = it.split(DELIMITER, limit = 3)
                if (parts.size >= 2) parts[0] to parts[1] else null
            }.let { list ->
                list.find { it.first == "default" }?.second ?: list.firstOrNull()?.second
            }

            resolved?.let { "$varName → $it" }
        } catch (e: Exception) {
            logger.warn("Failed to generate hint for $varName", e)
            null
        }
    }

    private fun resolveVarValue(
        project: Project,
        raw: String,
        visited: Set<String> = emptySet(),
        depth: Int = 0,
        steps: List<String> = emptyList()
    ): ResolutionInfo {
        val settings = CssVarsAssistantSettings.getInstance()
        if (depth > settings.maxImportDepth) return ResolutionInfo(raw, raw, steps)

        try {
            ProgressManager.checkCanceled()

            Regex("""var\(\s*(--[\w-]+)\s*\)""").find(raw)?.let { m ->
                val ref = m.groupValues[1]
                if (ref !in visited) {
                    val newSteps = steps + "var($ref)"
                    val cssScope = ScopeUtil.effectiveCssIndexingScope(project, settings)
                    val entries = FileBasedIndex.getInstance().getValues(CSS_VARIABLE_INDEXER_NAME, ref, cssScope)
                        .flatMap { it.split(ENTRY_SEP) }.filter { it.isNotBlank() }

                    val defVal = entries.mapNotNull {
                        val p = it.split(DELIMITER, limit = 3)
                        if (p.size >= 2) p[0] to p[1] else null
                    }.let { list ->
                        list.find { it.first == "default" }?.second ?: list.firstOrNull()?.second
                    }

                    if (defVal != null) return resolveVarValue(project, defVal, visited + ref, depth + 1, newSteps)
                }
                return ResolutionInfo(raw, raw, steps)
            }

            val preprocessorMatch = Regex("""^[\s]*[@$]([\w-]+)$""").find(raw.trim())
            if (preprocessorMatch != null) {
                val varName = preprocessorMatch.groupValues[1]
                val prefix = if (raw.contains("@")) "@" else "$"
                val currentScope = ScopeUtil.currentPreprocessorScope(project)

                CssVarCompletionCache.get(project, varName, currentScope)?.let {
                    return ResolutionInfo(raw, it, steps + "$prefix$varName")
                }

                val resolved = findPreprocessorVariableValue(project, varName)
                if (resolved != null && resolved != raw) {
                    CssVarCompletionCache.put(project, varName, currentScope, resolved)
                    return ResolutionInfo(raw, resolved, steps + "$prefix$varName")
                }
            }

            return ResolutionInfo(raw, raw, steps)
        } catch (e: ProcessCanceledException) {
            throw e
        } catch (e: Exception) {
            logger.warn("Failed to resolve variable in documentation: $raw", e)
            return ResolutionInfo(raw, raw, steps)
        }
    }

    private fun findPreprocessorVariableValue(project: Project, varName: String): String? {
        return try {
            val freshScope = ScopeUtil.currentPreprocessorScope(project)
            PreprocessorUtil.resolveVariable(project, varName, freshScope)
        } catch (e: ProcessCanceledException) {
            throw e
        } catch (e: Exception) {
            logger.warn("Failed to find preprocessor variable in doc-menu: $varName", e)
            null
        }
    }
}