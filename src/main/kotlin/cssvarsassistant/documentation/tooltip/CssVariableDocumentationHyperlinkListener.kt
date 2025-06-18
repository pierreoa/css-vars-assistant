package cssvarsassistant.documentation.tooltip

import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.indexing.FileBasedIndex
import cssvarsassistant.documentation.resolveVarValue
import cssvarsassistant.index.CSS_VARIABLE_INDEXER_NAME
import cssvarsassistant.index.DELIMITER
import cssvarsassistant.settings.CssVarsAssistantSettings
import cssvarsassistant.util.ScopeUtil
import java.awt.Component
import java.awt.Point
import java.awt.event.MouseEvent
import javax.swing.event.HyperlinkEvent
import javax.swing.event.HyperlinkListener

class CssVariableDocumentationHyperlinkListener(
    private val project: Project,
    private val varName: String
) : HyperlinkListener {
    private val logger = com.intellij.openapi.diagnostic.Logger.getInstance(CssVariableDocumentationHyperlinkListener::class.java)

    override fun hyperlinkUpdate(e: HyperlinkEvent) {
        if (e.eventType != HyperlinkEvent.EventType.ACTIVATED) return


        val href = e.description
        if (!href.startsWith("#resolution_")) return

        // Extract the display index from the href
        val parts = href.removePrefix("#resolution_").split("_")
        if (parts.size < 2) return

        val displayIndex = parts[0].toIntOrNull() ?: return

        // Extract mouse coordinates - cast InputEvent to MouseEvent
        val mouse = e.inputEvent as? MouseEvent ?: return
        val point = mouse.point
        val component = e.source as? Component ?: return   // <-- this is the Component you need


        // Get resolution steps for this specific entry using background thread
        CssVariableResolutionTooltipManager
            .getInstance(project)
            .showResolutionTooltip(component, point, resolveSteps)
    }

    private fun getResolutionStepsForIndex(displayIndex: Int, component: Component, point: Point) {
        ReadAction.nonBlocking<List<String>> {
            try {
                val settings = CssVarsAssistantSettings.getInstance()
                val cssScope = ScopeUtil.effectiveCssIndexingScope(project, settings)

                val rawEntries = FileBasedIndex.getInstance()
                    .getValues(CSS_VARIABLE_INDEXER_NAME, varName, cssScope)
                    .flatMap { it.split("|||") }
                    .filter { it.isNotBlank() }

                if (displayIndex >= rawEntries.size) return@nonBlocking emptyList()

                val entry = rawEntries[displayIndex]
                val parts = entry.split(DELIMITER, limit = 3)
                if (parts.size < 2) return@nonBlocking emptyList()

                val resInfo = resolveVarValue(project, parts[1])
                resInfo.steps
            } catch (e: Exception) {
                logger.error("Error resolving CSS variable documentation for $varName at index $displayIndex", e)
                emptyList()
            }
        }
            .expireWith(project) // Cancel if project is disposed
            .finishOnUiThread(ModalityState.defaultModalityState()) { resolutionSteps ->
                if (resolutionSteps.isNotEmpty()) {
                    val tooltipManager = CssVariableResolutionTooltipManager.getInstance(project)
                    tooltipManager.showResolutionTooltip(component, point, resolutionSteps)
                }
            }
            .submit(AppExecutorUtil.getAppExecutorService())
    }
}