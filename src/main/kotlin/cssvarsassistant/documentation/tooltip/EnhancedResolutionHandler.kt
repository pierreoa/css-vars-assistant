// src/main/kotlin/cssvarsassistant/documentation/tooltip/EnhancedResolutionHandler.kt
package cssvarsassistant.documentation.tooltip

import com.intellij.platform.backend.documentation.DocumentationLinkHandler
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.documentation.LinkResolveResult
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.JBUI
import java.awt.Component
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.border.EmptyBorder

const val CSS_VAR_RESOLUTION_LINK = "css-var-resolution://"

class EnhancedResolutionHandler : DocumentationLinkHandler {

    override fun resolveLink(
        target: DocumentationTarget,
        url: String,
    ): LinkResolveResult? {

        if (!url.startsWith(CSS_VAR_RESOLUTION_LINK)) return null

        val steps = url.removePrefix(CSS_VAR_RESOLUTION_LINK)
            .split("|")
            .filter { it.isNotBlank() }

        if (steps.isEmpty()) return null

        /* ------------------------------------------------------------------
         * 2024.1+ `resolveLink` can no longer pop UI directly (no Project).
         * If you still want the fancy popup you had earlier you can:
         *   1) fetch the project lazily from the target or
         *   2) use LinkResolveResult.resolvedTarget like here, and let
         *      the platform render the HTML we return.
         * For now we take the simple route – hand back a rich target.
         * ------------------------------------------------------------------ */
        return LinkResolveResult.resolvedTarget(CustomResolutionTarget(steps))
    }

    private fun createResolutionStepsComponent(steps: List<String>): JComponent {
        val panel = JBPanel<JBPanel<*>>().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = EmptyBorder(12, 16, 12, 16)
        }

        steps.forEachIndexed { index, step ->
            val isLast = index == steps.size - 1

            // Step component
            val stepLabel = JBLabel("<html><code>$step</code></html>").apply {
                border = EmptyBorder(6, 8, 6, 8)
                isOpaque = true
                background = if (isLast) {
                    JBUI.CurrentTheme.NotificationInfo.backgroundColor()
                } else {
                    JBUI.CurrentTheme.Popup.BACKGROUND
                }
            }

            panel.add(stepLabel)

            // Arrow between steps
            if (!isLast) {
                val arrow = JBLabel("↓").apply {
                    alignmentX = Component.CENTER_ALIGNMENT
                    foreground = JBUI.CurrentTheme.Label.disabledForeground()
                }
                panel.add(Box.createVerticalStrut(4))
                panel.add(arrow)
                panel.add(Box.createVerticalStrut(4))
            }
        }

        return panel
    }
}
