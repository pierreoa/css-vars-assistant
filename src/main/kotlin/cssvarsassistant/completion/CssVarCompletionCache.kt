package cssvarsassistant.completion

import com.intellij.openapi.project.Project
import java.util.concurrent.ConcurrentHashMap

/** Global cache for LESS / SCSS fallback resolutions used by completion + docs. */
object CssVarCompletionCache {
    private val map = ConcurrentHashMap<Pair<Project, String>, String?>()

    fun get(project: Project, name: String): String? = map[project to name]

    fun put(project: Project, name: String, value: String?) {
        map[project to name] = value
    }

    fun clear() = map.clear()

    @JvmStatic
    fun clearCaches() = clear()
    
}
