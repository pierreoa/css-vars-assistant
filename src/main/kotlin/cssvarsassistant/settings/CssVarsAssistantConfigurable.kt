package cssvarsassistant.settings

import com.intellij.openapi.options.Configurable
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.BoxLayout

class CssVarsAssistantConfigurable : Configurable {
    private val settings = CssVarsAssistantSettings.getInstance()
    private val showContextValuesCheck = JCheckBox("Show context-based variable values", settings.showContextValues)

    override fun getDisplayName() = "CSS Variables Assistant"
    override fun createComponent(): JComponent {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.add(showContextValuesCheck)
        return panel
    }

    override fun isModified() = showContextValuesCheck.isSelected != settings.showContextValues

    override fun apply() {
        settings.showContextValues = showContextValuesCheck.isSelected
    }

    override fun reset() {
        showContextValuesCheck.isSelected = settings.showContextValues
    }

    override fun disposeUIResources() {}
}
