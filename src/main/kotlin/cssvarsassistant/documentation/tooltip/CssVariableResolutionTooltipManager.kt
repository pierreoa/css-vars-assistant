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

@Service(Service.Level.PROJECT)
class CssVariableResolutionTooltipManager(private val project: Project) {

    fun show(component: Component, point: RelativePoint, resolutionSteps: List<String>) {
        ApplicationManager.getApplication().invokeLater {
            if (project.isDisposed) return@invokeLater
            val html = buildHtml(resolutionSteps)
            val balloon = JBPopupFactory.getInstance()
                .createHtmlTextBalloonBuilder(html, null,
                    JBUI.CurrentTheme.Popup.BACKGROUND, null)
                .setHideOnClickOutside(true)
                .setHideOnKeyOutside(true)
                .setAnimationCycle(0)
                .setFadeoutTime(0)
                .setBlockClicksThroughBalloon(true)
                .setRequestFocus(false)
                .createBalloon()
            Disposer.register(project, balloon)
            balloon.show(point, Balloon.Position.above)
        }
    }

    private fun buildHtml(steps: List<String>): String = buildString {
        append("<html><body style='font-family:")
        append(JBUI.Fonts.label().family)
        append("; font-size:")
        append(JBUI.Fonts.label().size)
        append("px; margin:8px;'>")
        append("<b>Resolution Path:</b><br/>")
        steps.forEachIndexed { i, step ->
            if (i > 0) append(" → ")
            append("<code style='background:rgba(128,128,128,0.2);padding:2px 4px;border-radius:3px;'>")
            append(step.replace("<", "&lt;").replace(">", "&gt;"))
            append("</code>")
            if (i < steps.size - 1) append("<br/>")
        }
        append("</body></html>")
    }

    companion object {
        fun getInstance(project: Project): CssVariableResolutionTooltipManager =
            project.getService(CssVariableResolutionTooltipManager::class.java)
    }
}