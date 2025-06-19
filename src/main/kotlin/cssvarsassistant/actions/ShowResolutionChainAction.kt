// src/main/kotlin/cssvarsassistant/actions/ShowResolutionChainAction.kt
package cssvarsassistant.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.JBUI
import java.awt.Component
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.border.EmptyBorder

class ShowResolutionChainAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val steps = e.getData(RESOLUTION_STEPS_KEY) ?: return
        showResolutionPopup(e, steps)
    }

    private fun showResolutionPopup(e: AnActionEvent, steps: List<String>) {
        val panel = JBPanel<JBPanel<*>>().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = EmptyBorder(12, 16, 12, 16)
        }

        steps.forEachIndexed { index, step ->
            val isLast = index == steps.size - 1
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

        JBPopupFactory.getInstance()
            .createComponentPopupBuilder(panel, null)
            .setTitle("Variable Resolution Chain")
            .setResizable(false)
            .setMovable(true)
            .setRequestFocus(true)
            .createPopup()
            .showInBestPositionFor(e.dataContext)
    }

    companion object {
        const val ACTION_ID = "cssvarsassistant.ShowResolutionChain"
        val RESOLUTION_STEPS_KEY = com.intellij.openapi.actionSystem.DataKey.create<List<String>>("ResolutionSteps")
    }
}