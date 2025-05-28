package cssvarsassistant.completion

import com.intellij.util.indexing.FileBasedIndex
import cssvarsassistant.index.CSS_VARIABLE_INDEXER_NAME

/** Global cache for LESS / SCSS fallback resolutions used by completion + docs. */
object CssVariableIndexRebuilder {
    @JvmStatic
    fun forceRebuild() = FileBasedIndex.getInstance()
        .requestRebuild(CSS_VARIABLE_INDEXER_NAME)
}
