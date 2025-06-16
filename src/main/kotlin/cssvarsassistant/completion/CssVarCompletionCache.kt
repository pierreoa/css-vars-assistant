package cssvarsassistant.completion

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import java.util.concurrent.ConcurrentHashMap

/**
 * Scope-aware cache for LESS / SCSS fallback resolutions.
 * The map key includes *project.hashCode()* and *scope.hashCode()*
 * so a change in either automatically invalidates the entry.
 */
object CssVarCompletionCache {

    private val map = ConcurrentHashMap<Triple<Int, String, Int>, String?>()

    fun get(project: Project, name: String, scope: GlobalSearchScope): String? =
        map[Triple(project.hashCode(), name, scope.hashCode())]

    fun put(project: Project, name: String, scope: GlobalSearchScope, value: String?) {
        map[Triple(project.hashCode(), name, scope.hashCode())] = value
    }

    fun clear() = map.clear()

    /* ---------- Convenience wrappers ------------------------------------- */

    fun get(project: Project, name: String): String? {
        val current = cssvarsassistant.util.ScopeUtil.currentPreprocessorScope(project)
        return get(project, name, current)
    }

    fun put(project: Project, name: String, value: String?) {
        val current = cssvarsassistant.util.ScopeUtil.currentPreprocessorScope(project)
        put(project, name, current, value)
    }

    @JvmStatic
    fun clearCaches() = clear()
}
