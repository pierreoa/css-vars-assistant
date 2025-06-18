package cssvarsassistant.documentation.tooltip

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.Project
import com.intellij.util.indexing.FileBasedIndex
import cssvarsassistant.documentation.resolveVarValue
import cssvarsassistant.index.CSS_VARIABLE_INDEXER_NAME
import cssvarsassistant.index.DELIMITER
import cssvarsassistant.settings.CssVarsAssistantSettings
import cssvarsassistant.util.ScopeUtil

/**
 * Resolves the resolution steps for a specific CSS variable table entry.
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
                .flatMap { it.split("|||") }
                .filter { it.isNotBlank() }

            if (displayIdx !in rawEntries.indices) return@nonBlocking emptyList()
            val parts = rawEntries[displayIdx].split(DELIMITER, limit = 3)
            if (parts.size < 2) return@nonBlocking emptyList()

            resolveVarValue(project, parts[1]).steps
        }.executeSynchronously()
}