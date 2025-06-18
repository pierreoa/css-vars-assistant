package cssvarsassistant.documentation.tooltip

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.Disposer
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.ui.JBUI
import java.awt.Component
import java.awt.Point

@Service(Service.Level.PROJECT)
class CssVariableResolutionTooltipManager(private val project: Project) {

    fun showResolutionTooltip(
        component: Component,
        point: Point,
        resolutionSteps: List<String>
    ) {

        ApplicationManager.getApplication().invokeLater {

            if (project.isDisposed) return@invokeLater

            val htmlContent = buildResolutionTooltipHtml(resolutionSteps)

            val balloon = JBPopupFactory.getInstance()
                .createHtmlTextBalloonBuilder(
                    htmlContent,
                    null, // No icon
                    JBUI.CurrentTheme.Popup.BACKGROUND,
                    null  // No hyperlink listener needed for tooltip content
                )
                .setHideOnClickOutside(true)
                .setHideOnKeyOutside(true)
                .setAnimationCycle(0)
                .setFadeoutTime(0)
                .setBlockClicksThroughBalloon(true)
                .setRequestFocus(false)
                .createBalloon()

            // Register for disposal
            Disposer.register(project, balloon)

            // Show relative to the clicked point
            balloon.show(RelativePoint(component, point), Balloon.Position.above)
        }
    }

    private fun buildResolutionTooltipHtml(steps: List<String>): String {
        return buildString {
            append("<html><body style='font-family: ")
            append(JBUI.Fonts.label().family)
            append("; font-size: ")
            append(JBUI.Fonts.label().size)
            append("px; margin: 8px;'>")
            append("<b>Resolution Path:</b><br/>")

            steps.forEachIndexed { index, step ->
                if (index > 0) append(" → ")
                append("<code style='background: rgba(128,128,128,0.2); padding: 2px 4px; border-radius: 3px;'>")
                append(step.replace("<", "&lt;").replace(">", "&gt;"))
                append("</code>")
                if (index < steps.size - 1) append("<br/>")
            }

            append("</body></html>")
        }
    }

    companion object {
        fun getInstance(project: Project): CssVariableResolutionTooltipManager =
            project.getService(CssVariableResolutionTooltipManager::class.java)
    }
}