package cssvarsassistant.documentation.tooltip

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.util.containers.ContainerUtil
import javax.swing.event.HyperlinkListener

@Service(Service.Level.APP)
class CssVariableDocumentationRegistry {

    private val hyperlinkListeners = ContainerUtil.createConcurrentWeakMap<String, HyperlinkListener>()

    fun registerHyperlinkListener(varName: String, listener: HyperlinkListener) {
        hyperlinkListeners[varName] = listener
    }

    fun getHyperlinkListener(varName: String): HyperlinkListener? {
        return hyperlinkListeners[varName]
    }

    companion object {
        fun getInstance(): CssVariableDocumentationRegistry {
            return ApplicationManager.getApplication().getService(CssVariableDocumentationRegistry::class.java)
        }

        fun registerHyperlinkListener(varName: String, listener: HyperlinkListener) {
            getInstance().registerHyperlinkListener(varName, listener)
        }

        fun getHyperlinkListener(varName: String): HyperlinkListener? {
            return getInstance().getHyperlinkListener(varName)
        }
    }
}