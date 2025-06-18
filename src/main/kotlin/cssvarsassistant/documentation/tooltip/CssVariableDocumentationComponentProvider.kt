package cssvarsassistant.documentation.tooltip

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.util.containers.ContainerUtil
import java.lang.ref.WeakReference
import javax.swing.JEditorPane
import javax.swing.event.HyperlinkListener

@Service(Service.Level.APP)
class CssVariableDocumentationComponentProvider {

    companion object {
        @JvmStatic
        fun installHyperlinkListenerIfNeeded(
            component: JEditorPane,
            varName: String
        ) {
            ApplicationManager.getApplication().invokeLater {
                // Remove existing CSS variable hyperlink listeners to avoid duplicates
                component.hyperlinkListeners
                    .filterIsInstance<CssVariableDocumentationHyperlinkListener>()
                    .forEach { component.removeHyperlinkListener(it) }

                // Get the registered hyperlink listener for this variable
                val listener = CssVariableDocumentationRegistry.getHyperlinkListener(varName)
                if (listener != null) {
                    component.addHyperlinkListener(listener)
                }
            }
        }

        @JvmStatic
        fun installHyperlinkListenerForComponent(
            component: JEditorPane,
            listener: HyperlinkListener
        ) {
            ApplicationManager.getApplication().invokeLater {
                // Remove existing CSS variable hyperlink listeners to avoid duplicates
                component.hyperlinkListeners
                    .filterIsInstance<CssVariableDocumentationHyperlinkListener>()
                    .forEach { component.removeHyperlinkListener(it) }

                component.addHyperlinkListener(listener)
            }
        }


        private val hyperlinkData = ContainerUtil.createConcurrentWeakMap<WeakReference<PsiElement>, String>()

        @JvmStatic
        fun registerHyperlinkData(element: PsiElement, varName: String) {
            hyperlinkData[WeakReference(element)] = varName
        }

        /* Optional – if you want to look the name up later: */
        @JvmStatic
        fun varNameFor(element: PsiElement): String? =
            hyperlinkData.entries.firstOrNull { it.key.get() === element }?.value
    }



}