package cssvarsassistant.completion

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.containers.CollectionFactory.createConcurrentWeakKeySoftValueMap
import cssvarsassistant.documentation.ResolutionInfo

/**
 * Scope-aware cache for LESS / SCSS fallback resolutions.
 * Now stores complete ResolutionInfo objects to preserve resolution steps.
 * The map key includes *project.hashCode()* and *scope.hashCode()*
 * so a change in either automatically invalidates the entry.
 */
object CssVarCompletionCache {

    private val map = createConcurrentWeakKeySoftValueMap<Triple<Int, String, Int>, ResolutionInfo?>()

    fun get(project: Project, name: String, scope: GlobalSearchScope): ResolutionInfo? =
        map[Triple(project.hashCode(), name, scope.hashCode())]

    fun put(project: Project, name: String, scope: GlobalSearchScope, value: ResolutionInfo?) {
        map[Triple(project.hashCode(), name, scope.hashCode())] = value
    }

    fun clear() = map.clear()

    /* ---------- Convenience wrappers ------------------------------------- */

    fun get(project: Project, name: String): ResolutionInfo? {
        val current = cssvarsassistant.util.ScopeUtil.currentPreprocessorScope(project)
        return get(project, name, current)
    }

    fun put(project: Project, name: String, value: ResolutionInfo?) {
        val current = cssvarsassistant.util.ScopeUtil.currentPreprocessorScope(project)
        put(project, name, current, value)
    }

    /* ---------- Backwards compatibility for completion use cases --------- */

    fun getResolved(project: Project, name: String, scope: GlobalSearchScope): String? =
        get(project, name, scope)?.resolved

    fun getResolved(project: Project, name: String): String? =
        get(project, name)?.resolved

    @JvmStatic
    fun clearCaches() = clear()
}
