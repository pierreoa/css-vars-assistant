package cssvarsassistant.lifecycle

import com.intellij.ide.plugins.DynamicPluginListener
import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.ProjectManager
import com.intellij.util.indexing.FileBasedIndex
import cssvarsassistant.completion.CssVarCompletionCache
import cssvarsassistant.completion.CssVarKeyCache
import cssvarsassistant.index.CSS_VARIABLE_INDEXER_NAME
import cssvarsassistant.index.ImportCache
import cssvarsassistant.index.PREPROCESSOR_VARIABLE_INDEX_NAME
import cssvarsassistant.util.PreprocessorUtil
import cssvarsassistant.util.ScopeUtil

class DynamicPluginCleanup : DynamicPluginListener {
    private val LOG = Logger.getInstance(DynamicPluginCleanup::class.java)

    override fun beforePluginUnload(pluginDescriptor: IdeaPluginDescriptor, isUpdate: Boolean) {
        if (pluginDescriptor.pluginId.idString == "cssvarsassistant") {
            LOG.info("🧹 Starting comprehensive CSS Variables Assistant cleanup...")

            try {
                // 1. Clear all static caches first
                PreprocessorUtil.clearCache()
                ScopeUtil.clearAll()
                CssVarCompletionCache.clearCaches()

                // 2. Clear project-level services for all open projects
                val openProjects = ProjectManager.getInstance().openProjects
                LOG.info("📁 Clearing caches for ${openProjects.size} open projects...")

                openProjects.forEach { project ->
                    try {
                        if (!project.isDisposed) {
                            LOG.debug("🧽 Cleaning project: ${project.name}")

                            // Clear project-specific caches
                            CssVarKeyCache.get(project).clear()
                            ImportCache.get(project).clear()
                            ScopeUtil.clearCache(project)
                        }
                    } catch (e: Exception) {
                        LOG.warn("⚠️ Error clearing project-level caches for ${project.name}", e)
                    }
                }

                // 3. Request index rebuilds to clear internal index caches
                try {
                    LOG.debug("🔄 Requesting index rebuilds...")
                    val fileBasedIndex = FileBasedIndex.getInstance()
                    fileBasedIndex.requestRebuild(CSS_VARIABLE_INDEXER_NAME)
                    fileBasedIndex.requestRebuild(PREPROCESSOR_VARIABLE_INDEX_NAME)
                } catch (e: Exception) {
                    LOG.warn("⚠️ Error requesting index rebuilds", e)
                }

                // 4. Force garbage collection to help cleanup (removed deprecated runFinalization)
                LOG.debug("🗑️ Requesting garbage collection...")
                System.gc()

                LOG.info("✅ CSS Variables Assistant cleanup completed successfully")

            } catch (e: Exception) {
                LOG.error("❌ Critical error during plugin cleanup", e)
                // Don't throw - let the plugin unload attempt continue
            }
        }
    }

}