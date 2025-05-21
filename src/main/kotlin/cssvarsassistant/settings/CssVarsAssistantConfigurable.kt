package cssvarsassistant.settings

import com.intellij.openapi.options.Configurable
import javax.swing.*
import java.awt.GridBagLayout
import java.awt.GridBagConstraints
import java.awt.Insets

class CssVarsAssistantConfigurable : Configurable {
    private val settings = CssVarsAssistantSettings.getInstance()

    // UI Components
    private val showContextValuesCheck = JCheckBox("Show context-based variable values", settings.showContextValues)
    private val useGlobalSearchScopeCheck = JCheckBox("Use global search scope (includes libraries/node_modules)", settings.useGlobalSearchScope)
    private val allowIdeCompletionsCheck = JCheckBox("Allow IDE built-in completions for variables not found by plugin", settings.allowIdeCompletions)

    override fun getDisplayName() = "CSS Variables Assistant"

    override fun createComponent(): JComponent {
        val panel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints()

        // Common constraints
        gbc.anchor = GridBagConstraints.WEST
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.insets = Insets(5, 5, 5, 5)
        gbc.weightx = 1.0

        // Add components with proper layout
        gbc.gridy = 0
        panel.add(showContextValuesCheck, gbc)

        gbc.gridy = 1
        panel.add(useGlobalSearchScopeCheck, gbc)

        gbc.gridy = 2
        panel.add(allowIdeCompletionsCheck, gbc)

        // Description labels
        gbc.gridy = 3
        gbc.insets = Insets(20, 5, 5, 5)
        panel.add(createSectionLabel("Search Scope:"), gbc)

        gbc.gridy = 4
        gbc.insets = Insets(2, 15, 2, 5)
        panel.add(createDescriptionLabel(
            "Project scope: Only variables defined in your project files are indexed."
        ), gbc)

        gbc.gridy = 5
        panel.add(createDescriptionLabel(
            "Global scope: Also indexes variables from libraries and node_modules."
        ), gbc)

        gbc.gridy = 6
        gbc.insets = Insets(20, 5, 5, 5)
        panel.add(createSectionLabel("Completion Behavior:"), gbc)

        gbc.gridy = 7
        gbc.insets = Insets(2, 15, 2, 5)
        panel.add(createDescriptionLabel(
            "When checked, the plugin will merge its suggestions with the IDE's built-in completions."
        ), gbc)

        gbc.gridy = 8
        panel.add(createDescriptionLabel(
            "When unchecked, only variables found by the plugin will be suggested."
        ), gbc)

        // Add glue to push everything up
        gbc.gridy = 9
        gbc.weighty = 1.0
        panel.add(Box.createVerticalGlue(), gbc)

        return panel
    }

    private fun createSectionLabel(text: String): JLabel {
        val label = JLabel(text)
        label.font = label.font.deriveFont(label.font.style or java.awt.Font.BOLD)
        return label
    }

    private fun createDescriptionLabel(text: String): JLabel {
        val label = JLabel(text)
        label.font = label.font.deriveFont(label.font.size - 1f)
        return label
    }

    override fun isModified(): Boolean =
        showContextValuesCheck.isSelected != settings.showContextValues ||
                useGlobalSearchScopeCheck.isSelected != settings.useGlobalSearchScope ||
                allowIdeCompletionsCheck.isSelected != settings.allowIdeCompletions

    override fun apply() {
        settings.showContextValues = showContextValuesCheck.isSelected
        settings.useGlobalSearchScope = useGlobalSearchScopeCheck.isSelected
        settings.allowIdeCompletions = allowIdeCompletionsCheck.isSelected
    }

    override fun reset() {
        showContextValuesCheck.isSelected = settings.showContextValues
        useGlobalSearchScopeCheck.isSelected = settings.useGlobalSearchScope
        allowIdeCompletionsCheck.isSelected = settings.allowIdeCompletions
    }

    override fun disposeUIResources() {}
}