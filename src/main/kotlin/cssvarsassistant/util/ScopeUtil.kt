package cssvarsassistant.util

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import com.intellij.util.containers.CollectionFactory
import cssvarsassistant.index.ImportCache
import cssvarsassistant.settings.CssVarsAssistantSettings

/**
 * Helper methods for choosing the correct [GlobalSearchScope] when
 * looking up CSS or pre-processor variables.
 */
object ScopeUtil {

    /**
     * Prosjekt → pre-processor-scope cache.
     *
     * Bruker `CollectionFactory.createConcurrentWeakKeySoftValueMap` som
     * lager en concurrent weak-key / soft-value-map. Dermed forsvinner
     * oppføringen automatisk når [Project] blir garbage-collected eller
     * pluginen dynamisk avlastes.
     */
    private val preprocessorScopes =
        CollectionFactory.createConcurrentWeakKeySoftValueMap<Project, GlobalSearchScope>()

    /* ---------- CSS scopes ------------------------------------------------ */

    fun effectiveCssIndexingScope(
        project: Project,
        settings: CssVarsAssistantSettings
    ): GlobalSearchScope =
        when (settings.indexingScope) {
            CssVarsAssistantSettings.IndexingScope.PROJECT_ONLY ->
                GlobalSearchScope.projectScope(project)

            CssVarsAssistantSettings.IndexingScope.GLOBAL ->
                GlobalSearchScope.allScope(project)

            CssVarsAssistantSettings.IndexingScope.PROJECT_WITH_IMPORTS -> {
                val extra = ImportCache.get(project).get()
                if (extra.isEmpty())
                    GlobalSearchScope.projectScope(project)
                else
                    GlobalSearchScope.projectScope(project)
                        .uniteWith(GlobalSearchScope.filesScope(project, extra))
            }
        }

    /* ---------- Pre-processor scopes -------------------------------------- */

    fun currentPreprocessorScope(project: Project): GlobalSearchScope =
        preprocessorScopes.computeIfAbsent(project) {
            val projectRoots = ProjectScope.getProjectScope(project)
            val libraryRoots = ProjectScope.getLibrariesScope(project)
            val settings = CssVarsAssistantSettings.getInstance()

            when (settings.indexingScope) {
                CssVarsAssistantSettings.IndexingScope.PROJECT_ONLY ->
                    projectRoots.uniteWith(libraryRoots)

                CssVarsAssistantSettings.IndexingScope.PROJECT_WITH_IMPORTS -> {
                    val extra = ImportCache.get(project).get()
                    val base = projectRoots.uniteWith(libraryRoots)
                    if (extra.isEmpty()) base
                    else base.uniteWith(GlobalSearchScope.filesScope(project, extra))
                }

                CssVarsAssistantSettings.IndexingScope.GLOBAL ->
                    GlobalSearchScope.allScope(project)
            }
        }

    /* ---------- Cache maintenance ----------------------------------------- */

    fun clearCache(project: Project) {
        preprocessorScopes.remove(project)
    }

    fun clearAll() = preprocessorScopes.clear()
}
