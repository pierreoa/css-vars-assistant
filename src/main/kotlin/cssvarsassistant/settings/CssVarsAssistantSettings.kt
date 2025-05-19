package cssvarsassistant.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@State(
    name = "CssVarsAssistantSettings",
    storages = [Storage("cssVarsAssistant.xml")]
)
@Service
class CssVarsAssistantSettings : PersistentStateComponent<CssVarsAssistantSettings.State> {
    data class State(
        var showContextValues: Boolean = true
    )

    private var state = State()

    override fun getState() = state
    override fun loadState(state: State) {
        this.state = state
    }

    var showContextValues: Boolean
        get() = state.showContextValues
        set(value) { state.showContextValues = value }

    companion object {
        @JvmStatic
        fun getInstance() = com.intellij.openapi.application.ApplicationManager
            .getApplication()
            .getService(CssVarsAssistantSettings::class.java)
    }
}
