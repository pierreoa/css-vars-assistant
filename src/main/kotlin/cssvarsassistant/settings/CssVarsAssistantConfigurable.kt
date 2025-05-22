package cssvarsassistant.settings

import com.intellij.openapi.options.Configurable
import javax.swing.*
import java.awt.GridBagLayout
import java.awt.GridBagConstraints
import java.awt.Insets
import java.awt.event.ActionEvent
import java.awt.event.ActionListener

class CssVarsAssistantConfigurable : Configurable {
    private val settings = CssVarsAssistantSettings.getInstance()

    private val showContextValuesCheck = JCheckBox("Show context-based variable values", settings.showContextValues)
    private val allowIdeCompletionsCheck = JCheckBox("Allow IDE built-in completions for variables not found by plugin", settings.allowIdeCompletions)

    // Radio buttons for indexing scope
    private val projectOnlyRadio = JRadioButton("Project files only", settings.indexingScope == CssVarsAssistantSettings.IndexingScope.PROJECT_ONLY)
    private val projectWithImportsRadio = JRadioButton("Project files + @import resolution", settings.indexingScope == CssVarsAssistantSettings.IndexingScope.PROJECT_WITH_IMPORTS)
    private val globalRadio = JRadioButton("Full global scope (includes all node_modules)", settings.indexingScope == CssVarsAssistantSettings.IndexingScope.GLOBAL)

    private val maxImportDepthSpinner = JSpinner(SpinnerNumberModel(settings.maxImportDepth, 1, 10, 1))

    init {
        val scopeGroup = ButtonGroup()
        scopeGroup.add(projectOnlyRadio)
        scopeGroup.add(projectWithImportsRadio)
        scopeGroup.add(globalRadio)

        // Add listener to enable/disable import depth spinner
        val importDepthListener = ActionListener { updateImportDepthState() }
        projectOnlyRadio.addActionListener(importDepthListener)
        projectWithImportsRadio.addActionListener(importDepthListener)
        globalRadio.addActionListener(importDepthListener)

        updateImportDepthState()
    }

    override fun getDisplayName() = "CSS Variables Assistant"

    override fun createComponent(): JComponent {
        val panel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints()

        gbc.anchor = GridBagConstraints.WEST
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.insets = Insets(5, 5, 5, 5)
        gbc.weightx = 1.0

        // Display options
        gbc.gridy = 0
        panel.add(createSectionLabel("Display Options:"), gbc)

        gbc.gridy = 1
        gbc.insets = Insets(2, 15, 2, 5)
        panel.add(showContextValuesCheck, gbc)

        gbc.gridy = 2
        panel.add(allowIdeCompletionsCheck, gbc)

        // Indexing scope section
        gbc.gridy = 3
        gbc.insets = Insets(20, 5, 5, 5)
        panel.add(createSectionLabel("Variable Indexing Scope:"), gbc)

        gbc.gridy = 4
        gbc.insets = Insets(5, 15, 2, 5)
        panel.add(projectOnlyRadio, gbc)

        gbc.gridy = 5
        gbc.insets = Insets(2, 25, 2, 5)
        panel.add(createDescriptionLabel("Only variables defined in your project files are indexed."), gbc)

        gbc.gridy = 6
        gbc.insets = Insets(5, 15, 2, 5)
        panel.add(projectWithImportsRadio, gbc)

        gbc.gridy = 7
        gbc.insets = Insets(2, 25, 2, 5)
        panel.add(createDescriptionLabel("Project files + selective resolution of @import statements to external packages."), gbc)

        gbc.gridy = 8
        gbc.insets = Insets(2, 25, 2, 5)
        panel.add(createDescriptionLabel("Only the exact imported files are indexed, not entire node_modules."), gbc)

        gbc.gridy = 9
        gbc.insets = Insets(5, 15, 2, 5)
        panel.add(globalRadio, gbc)

        gbc.gridy = 10
        gbc.insets = Insets(2, 25, 2, 5)
        panel.add(createDescriptionLabel("Full indexing of all CSS files in node_modules and libraries."), gbc)

        // Import depth setting
        gbc.gridy = 11
        gbc.insets = Insets(15, 5, 5, 5)
        panel.add(createSectionLabel("@import Resolution Depth:"), gbc)

        val depthPanel = JPanel()
        depthPanel.add(JLabel("Maximum @import chain depth:"))
        depthPanel.add(maxImportDepthSpinner)
        depthPanel.add(JLabel("(prevents infinite recursion)"))

        gbc.gridy = 12
        gbc.insets = Insets(2, 15, 2, 5)
        panel.add(depthPanel, gbc)

        // Performance warning
        gbc.gridy = 13
        gbc.insets = Insets(20, 5, 5, 5)
        panel.add(createSectionLabel("Performance Note:"), gbc)

        gbc.gridy = 14
        gbc.insets = Insets(2, 15, 2, 5)
        panel.add(createDescriptionLabel("Global scope indexing may impact IDE performance with large projects."), gbc)

        gbc.gridy = 15
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

    private fun updateImportDepthState() {
        val enableDepthSpinner = projectWithImportsRadio.isSelected
        maxImportDepthSpinner.isEnabled = enableDepthSpinner
    }

    override fun isModified(): Boolean =
        showContextValuesCheck.isSelected != settings.showContextValues ||
                allowIdeCompletionsCheck.isSelected != settings.allowIdeCompletions ||
                getSelectedScope() != settings.indexingScope ||
                (maxImportDepthSpinner.value as Int) != settings.maxImportDepth

    override fun apply() {
        settings.showContextValues = showContextValuesCheck.isSelected
        settings.allowIdeCompletions = allowIdeCompletionsCheck.isSelected
        settings.indexingScope = getSelectedScope()
        settings.maxImportDepth = maxImportDepthSpinner.value as Int
    }

    override fun reset() {
        showContextValuesCheck.isSelected = settings.showContextValues
        allowIdeCompletionsCheck.isSelected = settings.allowIdeCompletions

        when (settings.indexingScope) {
            CssVarsAssistantSettings.IndexingScope.PROJECT_ONLY -> projectOnlyRadio.isSelected = true
            CssVarsAssistantSettings.IndexingScope.PROJECT_WITH_IMPORTS -> projectWithImportsRadio.isSelected = true
            CssVarsAssistantSettings.IndexingScope.GLOBAL -> globalRadio.isSelected = true
        }

        maxImportDepthSpinner.value = settings.maxImportDepth
        updateImportDepthState()
    }

    private fun getSelectedScope(): CssVarsAssistantSettings.IndexingScope = when {
        projectOnlyRadio.isSelected -> CssVarsAssistantSettings.IndexingScope.PROJECT_ONLY
        projectWithImportsRadio.isSelected -> CssVarsAssistantSettings.IndexingScope.PROJECT_WITH_IMPORTS
        globalRadio.isSelected -> CssVarsAssistantSettings.IndexingScope.GLOBAL
        else -> CssVarsAssistantSettings.IndexingScope.PROJECT_ONLY
    }

    override fun disposeUIResources() {}
}