package cssvarsassistant.documentation.tooltip

import com.intellij.platform.backend.documentation.DocumentationLinkHandler
import java.awt.event.MouseEvent

/**
 * Håndterer custom-URLer av typen  cssvar-resolve://<var>/<rowIdx>
 * som vi setter på ↗-lenken i HTML-tabellen.
 */
class CssVarResolutionLinkHandler : DocumentationLinkHandler {
    override fun handleLink(ctx: LinkContext, url: String): Boolean {
        if (!url.startsWith(RES_HREF_PREFIX)) return false       // let IDE handle others

        val (idx, varName) = url.removePrefix(RES_HREF_PREFIX).split('_', limit = 2)
        val displayIndex = idx.toIntOrNull() ?: return false
        val steps = CssVarStepResolver.resolve(ctx.project, varName, displayIndex)
        if (steps.isEmpty()) return true

        CssVariableResolutionTooltipManager                       // balloon builder below
            .getInstance(ctx.project)
            .show(ctx.component, ctx.inputEventPoint(), steps)

        return true
    }

    private fun LinkContext.inputEventPoint() =
        (inputEvent as? MouseEvent)?.point ?: Point(0, 0)
}
