package cssvarsassistant.documentation.tooltip

import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.HyperlinkAdapter
import com.intellij.ui.JBColor
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.dsl.builder.panel
import java.awt.Component
import java.awt.Point
import javax.swing.JComponent
import javax.swing.event.HyperlinkEvent

class CssDocHyperlinkListener(
    private val parent: JComponent
) : HyperlinkAdapter() {

    private var balloon: Balloon? = null

    fun hyperlinkEntered(e: HyperlinkEvent) {
        if (e.description.startsWith("cssChain://")) {
            showChain(e.description.removePrefix("cssChain://").split('|'), e.inputEvent.component)
        }
    }

    fun hyperlinkExited(e: HyperlinkEvent) {
        balloon?.hide()
    }

    private fun showChain(chain: List<String>, anchor: Component) {
        val panel = panel {
            chain.forEach { row { label(it) } }
        }
        balloon = JBPopupFactory.getInstance()
            .createBalloonBuilder(panel)
            .setFillColor(JBColor.PanelBackground)
            .setHideOnClickOutside(true)
            .setHideOnKeyOutside(true)
            .setHideOnAction(true)
            .createBalloon().also {
                it.show(RelativePoint(anchor, Point(anchor.width / 2, 0)), Balloon.Position.above)
            }
    }

    override fun hyperlinkActivated(p0: HyperlinkEvent) {
        TODO("Not yet implemented")
    }
}