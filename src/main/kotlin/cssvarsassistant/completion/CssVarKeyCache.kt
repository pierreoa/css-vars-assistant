package cssvarsassistant.completion

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.FileBasedIndex
import cssvarsassistant.index.CSS_VARIABLE_INDEXER_NAME
import cssvarsassistant.settings.CssVarsAssistantSettings
import cssvarsassistant.util.ScopeUtil
import java.util.concurrent.ConcurrentHashMap

/**
 * Scope-aware cache for CSS variable names.
 * Caches keys per effective indexing scope to keep
 * completions in sync with documentation lookup.
 */
@Service(Service.Level.PROJECT)
class CssVarKeyCache(private val project: Project) {

    // Map: scope.hashCode() -> list of variable names in that scope
    private val scopeToKeys = ConcurrentHashMap<Int, List<String>>()

    /** Returns all CSS custom property names available in current scope */
    fun getKeys(): List<String> {
        val settings = CssVarsAssistantSettings.getInstance()
        val scope = ScopeUtil.effectiveCssIndexingScope(project, settings)
        return getKeys(scope)
    }

    /** Returns all CSS custom property names for a specific scope */
    fun getKeys(scope: GlobalSearchScope): List<String> {
        val scopeHash = scope.hashCode()
        scopeToKeys[scopeHash]?.let { return it }

        ProgressManager.checkCanceled()
        // Load all keys, then filter to only those that have values in this scope
        val allKeys = FileBasedIndex.getInstance()
            // still need project to enumerate all keys
            .getAllKeys(CSS_VARIABLE_INDEXER_NAME, project)
            .filter { key ->
                ProgressManager.checkCanceled()
                FileBasedIndex.getInstance()
                    .getValues(CSS_VARIABLE_INDEXER_NAME, key, scope)
                    .isNotEmpty()
            }

        scopeToKeys[scopeHash] = allKeys
        return allKeys
    }

    /** Clears every scope’s cache */
    fun clear() {
        scopeToKeys.clear()
    }

    companion object {
        @JvmStatic
        fun get(project: Project): CssVarKeyCache =
            project.getService(CssVarKeyCache::class.java)
    }
}
