package cssvarsassistant.documentation.tooltip

import com.intellij.openapi.project.Project
import com.intellij.ui.awt.RelativePoint
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.event.HyperlinkEvent
import javax.swing.event.HyperlinkListener

const val RES_HREF_PREFIX = "cssvar-resolve://"

class CssVarHyperlinkListener(private val project: Project) : HyperlinkListener {
    override fun hyperlinkUpdate(e: HyperlinkEvent) {
        if (e.eventType != HyperlinkEvent.EventType.ACTIVATED) return
        val url = e.description
        if (!url.startsWith(RES_HREF_PREFIX)) return
        val parts = url.removePrefix(RES_HREF_PREFIX).split('/')
        if (parts.size != 2) return
        val varName = parts[0]
        val idx = parts[1].toIntOrNull() ?: return
        val steps = CssVarStepResolver.resolve(project, varName, idx)
        if (steps.isEmpty()) return
        val src = e.source as? JComponent ?: return
        val point = (e.inputEvent as? MouseEvent)?.point ?: java.awt.Point(src.width / 2, 0)
        CssVariableResolutionTooltipManager.getInstance(project).show(src, RelativePoint(src, point), steps)
        e.inputEvent?.consume()
    }
}