// Fixed version of DebugImportResolutionAction.kt

package cssvarsassistant.actions

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import cssvarsassistant.index.ImportResolver
import cssvarsassistant.settings.CssVarsAssistantSettings
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Font
import javax.swing.*

class DebugImportResolutionAction : AnAction() {
    private val logger = Logger.getInstance(DebugImportResolutionAction::class.java)

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        val project = e.project

        e.presentation.isEnabledAndVisible = project != null &&
                file != null &&
                isCssFile(file)
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return

        // Show immediate feedback notification
        NotificationGroupManager.getInstance()
            .getNotificationGroup("CSS Vars Assistant")
            .createNotification(
                "CSS IMPORT DEBUG STARTED",
                "Starting CSS import resolution analysis for ${file.name}...",
                NotificationType.INFORMATION
            )
            .notify(project)

        logger.info("DEBUG ACTION TRIGGERED for file: ${file.path}")

        ProgressManager.getInstance().run(object : Task.Backgroundable(
            project,
            "CSS Import Debug: ${file.name}",
            true  // Make it cancellable
        ) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    indicator.isIndeterminate = false
                    indicator.fraction = 0.1
                    indicator.text = "Analyzing imports for ${file.name}..."

                    val settings = CssVarsAssistantSettings.getInstance()
                    val importChain = StringBuilder()

                    importChain.append("=== CSS Import Resolution Debug ===\n")
                    importChain.append("File: ${file.path}\n")
                    importChain.append("Extension: ${file.extension}\n")
                    importChain.append("Max Import Depth: ${settings.maxImportDepth}\n")
                    importChain.append("Indexing Scope: ${settings.indexingScope}\n")
                    importChain.append("File Size: ${file.length} bytes\n\n")

                    indicator.fraction = 0.3
                    indicator.text = "Reading file content..."

                    // Add file content preview
                    try {
                        val content = String(file.contentsToByteArray())
                        val importMatches = Regex("""@import\s+(?:"([^"]+)"|'([^']+)'|\burl\([^)]+\))""")
                            .findAll(content).toList()
                        importChain.append("Found ${importMatches.size} @import statements in source file\n")
                        importMatches.forEach { match ->
                            indicator.checkCanceled()
                            importChain.append("  - ${match.value}\n")
                        }
                        importChain.append("\n")
                    } catch (ex: Exception) {
                        importChain.append("Error reading file content: ${ex.message}\n\n")
                    }

                    indicator.fraction = 0.5
                    indicator.text = "Resolving import chain..."

                    // FIXED: Build the tree structure properly
                    debugImportChainFixed(file, project, settings.maxImportDepth, importChain, 0)

                    indicator.fraction = 0.8
                    indicator.text = "Generating report..."

                    // Get the total resolved imports for summary
                    val allImports = ImportResolver.resolveImports(file, project, settings.maxImportDepth)
                    val totalImports = allImports.size

                    // Add summary
                    importChain.append("\n=== SUMMARY ===\n")
                    importChain.append("Total files in import chain: ${totalImports + 1}\n")
                    importChain.append("External imports resolved: $totalImports\n")
                    importChain.append("Debug completed at: ${java.time.LocalDateTime.now()}\n")

                    // Count variables in each file
                    indicator.fraction = 0.9
                    indicator.text = "Counting variables..."

                    importChain.append("\n=== VARIABLE COUNT ANALYSIS ===\n")
                    val mainFileVars = countVariablesInFile(file)
                    importChain.append("Variables in main file (${file.name}): $mainFileVars\n")

                    var totalVarsFromImports = 0
                    allImports.forEach { importedFile ->
                        indicator.checkCanceled()
                        val vars = countVariablesInFile(importedFile)
                        totalVarsFromImports += vars
                        importChain.append("Variables in ${importedFile.name}: $vars\n")
                    }

                    importChain.append("Total variables from imports: $totalVarsFromImports\n")
                    importChain.append("Grand total variables: ${mainFileVars + totalVarsFromImports}\n")

                    // Log to IDE log with clear separator
                    logger.info("CSS IMPORT DEBUG RESULTS:\n" + "=".repeat(80) + "\n$importChain" + "=".repeat(80))

                    indicator.fraction = 1.0
                    indicator.text = "Debug complete"

                    // Show results in dedicated dialog instead of just logging
                    SwingUtilities.invokeLater {
                        showDebugResultsDialog(project, file.name, importChain.toString(), totalImports)
                    }

                } catch (e: Exception) {
                    logger.error("CRITICAL ERROR in CSS Import Debug Action", e)
                    NotificationGroupManager.getInstance()
                        .getNotificationGroup("CSS Vars Assistant")
                        .createNotification(
                            "CSS IMPORT DEBUG FAILED",
                            "Error: ${e.message ?: "Unknown error"}",
                            NotificationType.ERROR
                        )
                        .notify(project)

                    // Also show error in dialog
                    SwingUtilities.invokeLater {
                        Messages.showErrorDialog(
                            project,
                            "CSS Import Debug failed with error:\n\n${e.message ?: "Unknown error"}\n\nCheck IDE log for full stack trace.",
                            "CSS IMPORT DEBUG ERROR"
                        )
                    }
                }
            }

            override fun onThrowable(error: Throwable) {
                logger.error("Background task failed", error)
                NotificationGroupManager.getInstance()
                    .getNotificationGroup("CSS Vars Assistant")
                    .createNotification(
                        "DEBUG TASK FAILED",
                        "Background task error: ${error.message}",
                        NotificationType.ERROR
                    )
                    .notify(project)
            }
        })
    }

    private fun showDebugResultsDialog(
        project: com.intellij.openapi.project.Project,
        fileName: String,
        debugResults: String,
        totalImports: Int
    ) {
        // Create a custom dialog to show debug results
        val dialog = object : DialogWrapper(project) {
            init {
                title = "CSS Import Debug Results - $fileName"
                init()
            }

            override fun createCenterPanel(): JComponent {
                val panel = JPanel(BorderLayout())
                val fileWord = if (totalImports == 1) "file" else "files"

                // Summary at top
                val summaryLabel = JLabel(
                    "<html><b>Analysis Complete:</b> Found $totalImports imported $fileWord<br/>" +
                            "<small>Full import chain analysis below:</small></html>"
                )
                summaryLabel.border = JBUI.Borders.empty(10)
                panel.add(summaryLabel, BorderLayout.NORTH)

                // Debug results in scrollable text area
                val textArea = JTextArea(debugResults)
                textArea.isEditable = false
                textArea.font = Font(Font.MONOSPACED, Font.PLAIN, 12)
                textArea.background = UIManager.getColor("Panel.background")

                val scrollPane = JBScrollPane(textArea).apply {
                    preferredSize = Dimension(800, 600)
                    verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
                    horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
                }
                panel.add(scrollPane, BorderLayout.CENTER)

                // Bottom tip label
                val tipLabel = JLabel(
                    "<html><small>💡 <b>Tip:</b> This analysis is also logged to IDE Log " +
                            "(View → Tool Windows → Log) for future reference.</small></html>"
                )
                tipLabel.border = JBUI.Borders.empty(10)
                panel.add(tipLabel, BorderLayout.SOUTH)

                return panel
            }

            override fun createActions(): Array<Action> {
                return arrayOf(okAction)
            }
        }
        dialog.isResizable = true
        dialog.show()

        // Show success notification
        NotificationGroupManager.getInstance()
            .getNotificationGroup("CSS Vars Assistant")
            .createNotification(
                "CSS IMPORT DEBUG COMPLETE",
                "Analysis finished for $fileName. Found $totalImports imported files. Results window opened.",
                NotificationType.INFORMATION
            )
            .notify(project)
    }

    /**
     * FIXED: Build the import tree by parsing @import statements directly in each file
     * rather than relying on ImportResolver's visited set tracking
     */
    private fun debugImportChainFixed(
        file: VirtualFile,
        project: com.intellij.openapi.project.Project,
        maxDepth: Int,
        output: StringBuilder,
        depth: Int
    ) {
        val indent = "  ".repeat(depth)

        if (depth >= maxDepth) {
            output.append("${indent}[MAX DEPTH REACHED]\n")
            return
        }

        output.append("${indent}📄 ${file.name} (${file.path})\n")

        try {
            // Parse @import statements directly from this file
            val content = String(file.contentsToByteArray())
            val importPaths = extractImportPaths(content)

            if (importPaths.isEmpty()) {
                output.append("${indent}  └─ No @import statements found in ${file.name} \n")

                // But let's also count variables to show why this file is valuable
                val varCount = countVariablesInFile(file)
                if (varCount > 0) {
                    output.append("${indent}  └─ Contains $varCount CSS variables\n")
                }
            } else {
                output.append("${indent}  └─ Found ${importPaths.size} @import statements:\n")

                importPaths.forEachIndexed { index, importPath ->
                    val isLast = index == importPaths.size - 1
                    val connector = if (isLast) "└─" else "├─"

                    // Try to resolve this specific import
                    val resolvedFile = resolveImportPath(file, importPath, project)

                    if (resolvedFile != null && resolvedFile.exists()) {
                        output.append("${indent}    $connector ")
                        debugImportChainFixed(resolvedFile, project, maxDepth, output, depth + 1)
                    } else {
                        output.append("${indent}    $connector ❌ Failed to resolve: $importPath\n")
                    }
                }
            }
        } catch (e: Exception) {
            output.append("${indent}  └─ Error analyzing file: ${e.message}\n")
        }
    }

    /**
     * Extract @import paths from CSS content (copied from ImportResolver logic)
     */
    private fun extractImportPaths(content: String): List<String> {
        val imports = mutableListOf<String>()
        val IMPORT_PATTERN =
            Regex("""@import\s+(?:"([^"]+)"|'([^']+)'|\burl\(\s*(?:"([^"]+)"|'([^']+)'|([^)]+))\s*\))""")

        IMPORT_PATTERN.findAll(content).forEach { match ->
            val importPath = match.groupValues.drop(1).firstOrNull { it.isNotBlank() }
            if (importPath != null) {
                imports.add(importPath.trim())
            }
        }

        return imports
    }

    /**
     * Resolve import path to VirtualFile (simplified version of ImportResolver logic)
     */
    private fun resolveImportPath(
        currentFile: VirtualFile,
        importPath: String,
        project: com.intellij.openapi.project.Project
    ): VirtualFile? {
        return try {
            when {
                importPath.startsWith("./") || importPath.startsWith("../") -> {
                    resolveRelativePath(currentFile, importPath)
                }

                importPath.startsWith("/") -> {
                    project.guessProjectDir()?.findFileByRelativePath(importPath.removePrefix("/"))
                }

                importPath.startsWith("@") -> {
                    resolveNodeModulesPath(currentFile, importPath, project)
                }

                else -> {
                    val localFile = resolveRelativePath(currentFile, importPath)
                    if (localFile != null && localFile.exists()) {
                        localFile
                    } else {
                        resolveNodeModulesPath(currentFile, importPath, project)
                    }
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun resolveRelativePath(currentFile: VirtualFile, relativePath: String): VirtualFile? {
        val currentDir = currentFile.parent ?: return null

        if (relativePath.contains('.')) {
            return com.intellij.openapi.vfs.VfsUtil.findRelativeFile(
                currentDir,
                *relativePath.split('/').toTypedArray()
            )
        }

        val currentExtension = currentFile.extension?.lowercase()
        val prioritizedExtensions = when (currentExtension) {
            "scss" -> listOf("scss", "css", "sass", "less")
            "sass" -> listOf("sass", "scss", "css", "less")
            "less" -> listOf("less", "css", "scss", "sass")
            else -> listOf("css", "scss", "sass", "less")
        }

        for (ext in prioritizedExtensions) {
            val pathWithExtension = "$relativePath.$ext"
            val resolved = com.intellij.openapi.vfs.VfsUtil.findRelativeFile(
                currentDir,
                *pathWithExtension.split('/').toTypedArray()
            )
            if (resolved != null && resolved.exists()) {
                return resolved
            }
        }

        return null
    }

    private fun resolveNodeModulesPath(
        currentFile: VirtualFile,
        packagePath: String,
        project: com.intellij.openapi.project.Project
    ): VirtualFile? {
        var searchDir = currentFile.parent

        while (searchDir != null) {
            val nodeModules = searchDir.findChild("node_modules")
            if (nodeModules != null && nodeModules.isDirectory) {
                val resolvedFile = resolveInNodeModules(nodeModules, packagePath, currentFile)
                if (resolvedFile != null) return resolvedFile
            }
            searchDir = searchDir.parent
        }

        val projectNodeModules = project.guessProjectDir()?.findChild("node_modules")
        if (projectNodeModules != null && projectNodeModules.isDirectory) {
            return resolveInNodeModules(projectNodeModules, packagePath, currentFile)
        }

        return null
    }

    private fun resolveInNodeModules(
        nodeModules: VirtualFile,
        packagePath: String,
        importingFile: VirtualFile
    ): VirtualFile? {
        if (packagePath.contains('.')) {
            val pathParts = packagePath.split('/')
            var current = nodeModules

            for (part in pathParts) {
                current = current.findChild(part) ?: return null
            }

            return if (current.isDirectory) null else current
        }

        val currentExtension = importingFile.extension?.lowercase()
        val prioritizedExtensions = when (currentExtension) {
            "scss" -> listOf("scss", "css", "sass", "less")
            "sass" -> listOf("sass", "scss", "css", "less")
            "less" -> listOf("less", "css", "scss", "sass")
            else -> listOf("css", "scss", "sass", "less")
        }

        for (ext in prioritizedExtensions) {
            val pathWithExtension = "$packagePath.$ext"
            val pathParts = pathWithExtension.split('/')
            var current = nodeModules

            for (part in pathParts) {
                current = current.findChild(part) ?: break
            }

            if (current != nodeModules && !current.isDirectory && current.exists()) {
                return current
            }
        }

        return null
    }

    /**
     * Count CSS variables in a file
     */
    private fun countVariablesInFile(file: VirtualFile): Int = try {
        val content = String(file.contentsToByteArray())
        val patterns = listOf(
            Regex("""--[\w-]+\s*:\s*[^;]+;"""),  // css custom prop
            Regex("""@[\w-]+\s*:\s*[^;]+;"""),   // less
            Regex("""\$[\w-]+\s*:\s*[^;]+;""")   // scss / sass
        )
        patterns.sumOf { it.findAll(content).count() }
    } catch (_: Exception) {
        0
    }

    private fun isCssFile(file: VirtualFile): Boolean {
        val extension = file.extension?.lowercase()
        return extension in setOf("css", "scss", "sass", "less")
    }
}