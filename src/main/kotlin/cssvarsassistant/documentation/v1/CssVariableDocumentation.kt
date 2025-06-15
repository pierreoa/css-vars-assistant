// CssVariableDocumentation.kt
package cssvarsassistant.documentation.v1

import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.DumbService
import com.intellij.psi.PsiElement
import com.intellij.util.indexing.FileBasedIndex
import cssvarsassistant.completion.CssVariableCompletion
import cssvarsassistant.documentation.ResolutionInfo
import cssvarsassistant.documentation.buildHtmlDocument
import cssvarsassistant.documentation.extractCssVariableName
import cssvarsassistant.documentation.resolveVarValue
import cssvarsassistant.index.CSS_VARIABLE_INDEXER_NAME
import cssvarsassistant.index.DELIMITER
import cssvarsassistant.model.CssVarDoc
import cssvarsassistant.model.DocParser
import cssvarsassistant.settings.CssVarsAssistantSettings
import cssvarsassistant.util.RankUtil.rank
import cssvarsassistant.util.ScopeUtil
import cssvarsassistant.util.ValueUtil
import kotlin.math.roundToInt

val ENTRY_SEP = "|||"

class CssVariableDocumentation : AbstractDocumentationProvider() {
    private val logger = Logger.getInstance(CssVariableCompletion::class.java)

    override fun generateDoc(element: PsiElement, original: PsiElement?): String? {
        try {
            val project = element.project
            if (DumbService.isDumb(project)) return null
            ProgressManager.checkCanceled()

            val settings = CssVarsAssistantSettings.getInstance()
            val varName = extractCssVariableName(element) ?: return null
            val cssScope = ScopeUtil.effectiveCssIndexingScope(project, settings)

            val rawEntries = FileBasedIndex.getInstance()
                .getValues(CSS_VARIABLE_INDEXER_NAME, varName, cssScope)
                .flatMap { it.split(ENTRY_SEP) }
                .filter { it.isNotBlank() }

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

            // Get local file text and find local values
            val activeText = element.containingFile.text
            val localValues = extractLocalValues(activeText, varName)

            // Mark entries as local or imported and determine cascade winner
            val enrichedEntries = parsed.map { (ctx, resInfo, comment) ->
                val isLocal = isLocalDeclaration(resInfo.resolved, localValues)
                EntryWithSource(ctx, resInfo, comment, isLocal)
            }

            val collapsed = enrichedEntries
                .asReversed()
                .distinctBy { it.context to it.resInfo.resolved }
                .asReversed()

            val sorted = collapsed.sortedWith(
                compareBy(
                    { rank(it.context).first },
                    { rank(it.context).second ?: Int.MAX_VALUE },
                    { rank(it.context).third }
                )
            )

            // Find cascade winner using CSS rules: local declarations beat imports
            val winnerIndex = findCascadeWinner(sorted)

            val docEntry = collapsed.firstOrNull { it.comment.isNotBlank() }
                ?: collapsed.find { it.context == "default" }
                ?: collapsed.first()
            val doc = DocParser.parse(docEntry.comment, docEntry.resInfo.resolved)

            val showPixelCol = sorted.any { entry ->
                if (!ValueUtil.isSizeValue(entry.resInfo.resolved)) return@any false
                val unit = entry.resInfo.resolved.replace(Regex("[0-9.+\\-]"), "").trim().lowercase()
                val pxVal = ValueUtil.convertToPixels(entry.resInfo.resolved)
                val numericRaw = entry.resInfo.resolved.replace(Regex("[^0-9.+\\-]"), "")
                    .toDoubleOrNull() ?: pxVal
                unit != "px" || pxVal.roundToInt() != numericRaw.roundToInt()
            }

            // Convert back to original format for buildHtmlDocument
            val sortedTriples = sorted.map { Triple(it.context, it.resInfo, it.comment) }

            return buildHtmlDocumentWithWinner(
                varName,
                doc,
                sortedTriples,
                showPixelCol,
                winnerIndex
            )

        } catch (e: ProcessCanceledException) {
            throw e
        } catch (e: Exception) {
            logger.error("Error generating documentation", e)
            return null
        }
    }

    private data class EntryWithSource(
        val context: String,
        val resInfo: ResolutionInfo,
        val comment: String,
        val isLocal: Boolean
    )

    private fun extractLocalValues(fileText: String, varName: String): Set<String> {
        return Regex("""\Q$varName\E\s*:\s*([^;]+);""")
            .findAll(fileText)
            .map { it.groupValues[1].trim() }
            .toSet()
    }

    private fun isLocalDeclaration(resolvedValue: String, localValues: Set<String>): Boolean {
        return localValues.contains(resolvedValue)
    }

    /**
     * Finds the cascade winner using CSS rules:
     * 1. Local declarations beat imported declarations
     * 2. Within same origin, last declared wins
     * 3. Context priority (default > others)
     */
    private fun findCascadeWinner(sorted: List<EntryWithSource>): Int {
        val defaultEntries = sorted.withIndex().filter { it.value.context == "default" }

        if (defaultEntries.isEmpty()) {
            return sorted.indexOfLast { it.context == "default" }
        }

        // First, prefer local declarations over imports
        val localDefaults = defaultEntries.filter { it.value.isLocal }
        if (localDefaults.isNotEmpty()) {
            return localDefaults.last().index
        }

        // If no local defaults, use imported defaults
        val importedDefaults = defaultEntries.filter { !it.value.isLocal }
        if (importedDefaults.isNotEmpty()) {
            return importedDefaults.last().index
        }

        // Fallback to original logic
        return sorted.indexOfLast { it.context == "default" }
    }
}

// Helper function to build HTML with correct winner highlighting
private fun buildHtmlDocumentWithWinner(
    varName: String,
    doc: CssVarDoc,
    sorted: List<Triple<String, ResolutionInfo, String>>,
    showPixelCol: Boolean,
    winnerIndex: Int
): String {
    // Your existing buildHtmlDocument function, but pass winnerIndex
    // instead of calculating it inside
    return buildHtmlDocument(varName, doc, sorted, showPixelCol, winnerIndex)
}