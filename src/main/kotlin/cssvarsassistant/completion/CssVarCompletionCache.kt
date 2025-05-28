package cssvarsassistant.completion

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import java.util.concurrent.ConcurrentHashMap

/**
 * ENHANCED: Scope-aware cache for LESS / SCSS fallback resolutions.
 * Cache key now includes scope hash to prevent stale values when scope changes.
 */
object CssVarCompletionCache {
    private val map = ConcurrentHashMap<Triple<Project, String, Int>, String?>()

    fun get(project: Project, name: String, scope: GlobalSearchScope): String? =
        map[Triple(project, name, scope.hashCode())]

    fun put(project: Project, name: String, scope: GlobalSearchScope, value: String?) {
        map[Triple(project, name, scope.hashCode())] = value
    }

    fun clear() = map.clear()

    fun get(project: Project, name: String): String? {
        val currentScope = cssvarsassistant.util.ScopeUtil.currentPreprocessorScope(project)
        return get(project, name, currentScope)
    }

    fun put(project: Project, name: String, value: String?) {
        val currentScope = cssvarsassistant.util.ScopeUtil.currentPreprocessorScope(project)
        put(project, name, currentScope, value)
    }

    @JvmStatic
    fun clearCaches() = clear()
}