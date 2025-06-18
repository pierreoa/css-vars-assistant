/*
// src/main/kotlin/cssvarsassistant/documentation/tooltip/EnhancedResolutionHandler.kt
package cssvarsassistant.documentation.tooltip

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
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

class EnhancedResolutionHandler : DocumentationLinkHandler {

    override fun resolveLink(
        target: DocumentationTarget,
        url: String,
        project: Project
    ): LinkResolveResult? {
        if (!url.startsWith("css-var-resolution://")) return null

        val steps = url.removePrefix("css-var-resolution://")
            .split("|")
            .filter { it.isNotBlank() }

        if (steps.isEmpty()) return null

        // Create rich popup with better visual design
        val content = createResolutionStepsComponent(steps)

        val popup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(content, null)
            .setTitle("Variable Resolution Chain")
            .setResizable(false)
            .setMovable(true)
            .setRequestFocus(true)
            .setFocusable(true)
            .createPopup()

        // Find the current editor to position popup
        val editor = com.intellij.openapi.fileEditor.FileEditorManager
            .getInstance(project)
            .selectedTextEditor

        if (editor != null) {
            popup.showInBestPositionFor(editor)
        } else {
            popup.showCenteredInCurrentWindow(project)
        }

        return LinkResolveResult.resolvedTarget(target) // Mark as handled
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
}*/
