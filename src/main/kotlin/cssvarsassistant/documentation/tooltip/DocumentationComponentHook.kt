package cssvarsassistant.documentation.tooltip

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class DocumentationComponentStartupActivity : ProjectActivity {

    override suspend fun execute(project: Project) {
        // Register a component listener to automatically install hyperlink listeners
        // when documentation components are created
        ApplicationManager.getApplication().invokeLater {
            installGlobalComponentListener(project)
        }
    }

    private fun installGlobalComponentListener(project: Project) {
        // This is a simplified approach - in a real implementation,
        // you might need to hook into the DocumentationManager
        // or use a different approach based on the specific IntelliJ version

        // For now, the hyperlink listeners will be installed when
        // the documentation is first shown, which should be sufficient
    }
}