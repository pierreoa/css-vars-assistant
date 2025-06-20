package cssvarsassistant.completion

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.containers.CollectionFactory
import com.intellij.util.containers.CollectionFactory.createConcurrentWeakKeySoftValueMap
import com.intellij.util.indexing.FileBasedIndex
import cssvarsassistant.index.CSS_VARIABLE_INDEXER_NAME
import cssvarsassistant.settings.CssVarsAssistantSettings
import cssvarsassistant.util.ScopeUtil
import java.util.concurrent.ConcurrentHashMap

/**
 * Scope-aware cache for CSS variable names.
 *
 * The keys are cached **per effective indexing scope**
 * to keep completions in sync with documentation lookup.
 *
 * Implements [Disposable] so that the map is cleared automatically
 * when the project closes *or* the plugin is unloaded.
 */
@Service(Service.Level.PROJECT)
class CssVarKeyCache(private val project: Project) : Disposable {

    /** map: scopeHash -> variable names that exist in that scope */
    private val scopeToKeys = createConcurrentWeakKeySoftValueMap<Int, List<String>>()

    /**
     * Returns all variable names that exist in the given [scope].
     *
     * The result is cached; call [clear] to wipe everything.
     */
    fun keys(scope: GlobalSearchScope): List<String> {
        val scopeHash = scope.hashCode()
        ProgressManager.checkCanceled()

        scopeToKeys[scopeHash]?.let { return it }

        val settings = CssVarsAssistantSettings.getInstance()
        val effectiveScope =
            ScopeUtil.effectiveCssIndexingScope(project, settings).intersectWith(scope)

        val allKeys = FileBasedIndex.getInstance()
            .getAllKeys(CSS_VARIABLE_INDEXER_NAME, project)
            .filter { key ->
                ProgressManager.checkCanceled()
                FileBasedIndex.getInstance()
                    .getValues(CSS_VARIABLE_INDEXER_NAME, key, effectiveScope)
                    .isNotEmpty()
            }

        scopeToKeys[scopeHash] = allKeys
        return allKeys
    }

    /** Clears every scope’s cache */
    fun clear() = scopeToKeys.clear()

    /** Disposable implementation – invoked automatically by the platform */
    override fun dispose() = clear()

    companion object {
        @JvmStatic
        fun get(project: Project): CssVarKeyCache =
            project.getService(CssVarKeyCache::class.java)
    }
}
