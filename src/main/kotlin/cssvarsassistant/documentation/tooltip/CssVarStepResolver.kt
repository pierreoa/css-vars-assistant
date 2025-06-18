package cssvarsassistant.documentation.tooltip

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.Project
import com.intellij.util.indexing.FileBasedIndex
import cssvarsassistant.documentation.resolveVarValue
import cssvarsassistant.index.CSS_VARIABLE_INDEXER_NAME
import cssvarsassistant.index.DELIMITER               // ← allerede i CssVariableIndex.kt
import cssvarsassistant.util.ScopeUtil
import cssvarsassistant.settings.CssVarsAssistantSettings

/**
 * Slår opp én rad (displayIdx) for gitt CSS-variabel og returnerer
 * listen av “resolution steps”.  Kjøres i bakgrunnstråd via ReadAction.
 */
object CssVarStepResolver {

    fun resolve(project: Project, varName: String, displayIdx: Int): List<String> =
        ReadAction.nonBlocking<List<String>> {
            val scope = ScopeUtil.effectiveCssIndexingScope(
                project,
                CssVarsAssistantSettings.getInstance()
            )

            val rawEntries = FileBasedIndex.getInstance()
                .getValues(CSS_VARIABLE_INDEXER_NAME, varName, scope)
                .flatMap { it.split("|||") }                // ENTRY_SEP i CssVariableIndex
                .filter { it.isNotBlank() }

            if (displayIdx !in rawEntries.indices) return@nonBlocking emptyList()

            val parts = rawEntries[displayIdx].split(DELIMITER, limit = 3)
            if (parts.size < 2) return@nonBlocking emptyList()

            resolveVarValue(project, parts[1]).steps       // <- du har allerede resolveVarValue()
        }.executeSynchronously()                           // umiddelbart – raskt nok til popup
}
