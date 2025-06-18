package cssvarsassistant.documentation.tooltip

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.util.containers.ContainerUtil
import javax.swing.event.HyperlinkListener

@Service(Service.Level.APP)
class CssVariableDocumentationRegistry {
    private val listeners = ContainerUtil.createConcurrentWeakMap<String, HyperlinkListener>()

    fun register(varName: String, listener: HyperlinkListener) {
        listeners[varName] = listener
    }

    fun get(varName: String): HyperlinkListener? = listeners[varName]

    companion object {
        fun getInstance(): CssVariableDocumentationRegistry =
            ApplicationManager.getApplication().getService(CssVariableDocumentationRegistry::class.java)
    }
}