package cssvarsassistant.util

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import cssvarsassistant.index.ImportCache
import cssvarsassistant.settings.CssVarsAssistantSettings

object ScopeUtil {

    /**
     * For CSS variable indexing (FileBasedIndex) - respects import restrictions
     * This can be cached since it's used during indexing, not resolution
     */
    fun effectiveCssIndexingScope(project: Project, settings: CssVarsAssistantSettings): GlobalSearchScope =
        when (settings.indexingScope) {
            CssVarsAssistantSettings.IndexingScope.PROJECT_ONLY ->
                GlobalSearchScope.projectScope(project)

            CssVarsAssistantSettings.IndexingScope.GLOBAL ->
                GlobalSearchScope.allScope(project)

            CssVarsAssistantSettings.IndexingScope.PROJECT_WITH_IMPORTS -> {
                val extra = ImportCache.get(project).get(project)
                if (extra.isEmpty())
                    GlobalSearchScope.projectScope(project)
                else
                    GlobalSearchScope.projectScope(project)
                        .uniteWith(GlobalSearchScope.filesScope(project, extra))
            }
        }

    /**
     * FIXED: Always computes fresh scope for preprocessor resolution
     * This ensures we see newly discovered imports immediately, fixing the race condition
     */
    fun currentPreprocessorScope(project: Project): GlobalSearchScope {
        val settings = CssVarsAssistantSettings.getInstance()

        val projectRoots = GlobalSearchScope.projectScope(project)
        val libraryRoots = ProjectScope.getLibrariesScope(project)


        return when (settings.indexingScope) {
            CssVarsAssistantSettings.IndexingScope.GLOBAL ->
                GlobalSearchScope.allScope(project)                     // already includes libs

            CssVarsAssistantSettings.IndexingScope.PROJECT_ONLY ->
                projectRoots.uniteWith(libraryRoots)                   // project + node_modules

            CssVarsAssistantSettings.IndexingScope.PROJECT_WITH_IMPORTS -> {
                val extra = ImportCache.get(project).get(project)
                val base = projectRoots.uniteWith(libraryRoots)       // include libs too
                if (extra.isEmpty()) base
                else base.uniteWith(GlobalSearchScope.filesScope(project, extra))
            }
        }

    }
}