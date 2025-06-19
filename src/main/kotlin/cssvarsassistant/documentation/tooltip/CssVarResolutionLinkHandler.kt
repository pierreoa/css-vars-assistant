// src/main/kotlin/cssvarsassistant/documentation/tooltip/CssVarResolutionLinkHandler.kt
package cssvarsassistant.documentation.tooltip

import com.intellij.platform.backend.documentation.DocumentationLinkHandler
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.documentation.LinkResolveResult
import cssvarsassistant.util.CSS_VAR_RESOLUTION_LINK

class CssVarResolutionLinkHandler : DocumentationLinkHandler {
    override fun resolveLink(target: DocumentationTarget, url: String): LinkResolveResult? {
        if (!url.startsWith(CSS_VAR_RESOLUTION_LINK)) return null

        val steps = url.removePrefix(CSS_VAR_RESOLUTION_LINK)
            .split("|")
            .filter { it.isNotBlank() }

        if (steps.isEmpty()) return null

        return LinkResolveResult.resolvedTarget(CustomResolutionTarget(steps))
    }
}

class CustomResolutionTarget(
    private val resolutionSteps: List<String>
) : DocumentationTarget {

    override fun computePresentation() =
        com.intellij.platform.backend.presentation.TargetPresentation
            .builder("Variable Resolution Chain")
            .presentation()

    override fun computeDocumentation() =
        com.intellij.platform.backend.documentation.DocumentationResult
            .documentation(buildResolutionHtml())

    override fun createPointer() =
        com.intellij.model.Pointer { this }

    private fun buildResolutionHtml(): String {
        val sb = StringBuilder()
        sb.append("<div style='font-family: monospace; line-height: 1.4;'>")
        sb.append("<h3>Resolution Chain</h3>")

        resolutionSteps.forEachIndexed { index, step ->
            val isLast = index == resolutionSteps.size - 1
            sb.append("<div style='margin: 4px 0; padding: 4px 8px; ")

            if (isLast) {
                sb.append("background-color: rgba(76, 175, 80, 0.1); ")
                sb.append("border-left: 3px solid #4CAF50;")
            } else {
                sb.append("border-left: 2px solid #666;")
            }

            sb.append("'>")
            sb.append("<code>").append(step).append("</code>")
            sb.append("</div>")

            if (!isLast) {
                sb.append("<div style='text-align: center; color: #888; font-size: 12px;'>↓</div>")
            }
        }

        sb.append("</div>")
        return sb.toString()
    }
}