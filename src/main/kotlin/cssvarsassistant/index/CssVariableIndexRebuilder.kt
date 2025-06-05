package cssvarsassistant.index

import com.intellij.util.indexing.FileBasedIndex
import cssvarsassistant.index.CSS_VARIABLE_INDEXER_NAME
import com.intellij.openapi.project.ProjectManager
import cssvarsassistant.completion.CssVarKeyCache

/** Global cache for LESS / SCSS fallback resolutions used by completion + docs. */
object CssVariableIndexRebuilder {
    @JvmStatic
    fun forceRebuild() {
        FileBasedIndex.getInstance().requestRebuild(CSS_VARIABLE_INDEXER_NAME)
        // Clear cached variable names for all open projects
        ProjectManager.getInstance().openProjects.forEach {
            CssVarKeyCache.get(it).clear()
        }
    }
}
