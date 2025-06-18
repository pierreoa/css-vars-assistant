/*
package cssvarsassistant.documentation.tooltip

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import javax.swing.event.HyperlinkEvent
import javax.swing.event.HyperlinkListener
import java.awt.event.KeyEvent

const val CSS_VAR_RESOLVE_PREFIX = "cssvar-resolve://"

class CssVarPopupLinkListener(
    private val project: Project,
    private val varName: String
) : HyperlinkListener {

    override fun hyperlinkUpdate(e: HyperlinkEvent) {
        if (e.eventType == HyperlinkEvent.EventType.ACTIVATED) {
            handleLinkClick(e)
        }
    }

    private fun handleLinkClick(e: HyperlinkEvent) {
        val url = e.description
        if (!url.startsWith(CSS_VAR_RESOLVE_PREFIX)) return

        // Parse the resolution steps from URL
        val stepsData = url.removePrefix(CSS_VAR_RESOLVE_PREFIX)
        val steps = stepsData.split("|").filter { it.isNotBlank() }

        if (steps.isEmpty()) return

        showResolutionPopup(e, steps)
    }

    private fun showResolutionPopup(e: HyperlinkEvent, steps: List<String>) {
        val htmlContent = buildResolutionHtml(steps)

        val balloon = JBPopupFactory.getInstance()
            .createHtmlTextBalloonBuilder(
                htmlContent,
                null,
                JBUI.CurrentTheme.Popup.BACKGROUND,
                null
            )
            .setHideOnClickOutside(true)
            .setHideOnKeyOutside(true)
            .setHideOnFrameResize(true)
            .setRequestFocus(true)
            .setAnimationCycle(150)
            .setFadeoutTime(150)
            .createBalloon()

        // Handle keyboard accessibility
        balloon.setActionProvider { actions ->
            listOf(
                balloon.createDefaultActionHandler(KeyEvent.VK_ESCAPE) { balloon.hide() }
            )
        }

        // Position balloon relative to the clicked element
        val source = e.source as? java.awt.Component
        if (source != null) {
            val point = if (e.inputEvent is java.awt.event.MouseEvent) {
                (e.inputEvent as java.awt.event.MouseEvent).point
            } else {
                java.awt.Point(source.width / 2, 0)
            }

            balloon.show(
                com.intellij.ui.awt.RelativePoint(source, point),
                Balloon.Position.above
            )
        }
    }

    private fun buildResolutionHtml(steps: List<String>): String = buildString {
        append("<html><body style='")
        append("font-family: ${JBUI.Fonts.label().family}; ")
        append("font-size: ${JBUI.Fonts.label().size}px; ")
        append("margin: 8px; ")
        append("max-width: 400px;")
        append("'>")

        append("<div style='font-weight: bold; margin-bottom: 6px; color: ")
        append(JBColor.foreground().rgb.toString(16))
        append(";'>Resolution Path:</div>")

        steps.forEachIndexed { index, step ->
            append("<div style='margin: 3px 0; font-family: monospace; ")
            append("background: rgba(128,128,128,0.1); ")
            append("padding: 4px 6px; ")
            append("border-radius: 3px; ")
            append("border-left: 3px solid ")

            if (index == steps.size - 1) {
                append("#4CAF50") // Green for final value
            } else {
                append("#2196F3") // Blue for intermediate steps
            }
            append(";'>")

            if (index > 0) {
                append("<span style='color: #666; margin-right: 6px;'>↓</span>")
            }

            append(escapeHtml(step))
            append("</div>")
        }

        append("<div style='margin-top: 8px; font-size: smaller; color: #666;'>")
        append("Press Esc to close")
        append("</div>")

        append("</body></html>")
    }

    private fun escapeHtml(text: String): String =
        text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
}*/
