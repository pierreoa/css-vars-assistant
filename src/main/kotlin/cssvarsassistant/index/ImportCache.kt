// ImportCache.kt - Enhanced version
package cssvarsassistant.index

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import cssvarsassistant.completion.CssVarCompletionCache
import cssvarsassistant.util.PreprocessorUtil
import cssvarsassistant.util.ScopeUtil
import java.util.concurrent.ConcurrentHashMap

@Service(Service.Level.PROJECT)
class ImportCache(private val project: Project) : Disposable {
    private val LOG = Logger.getInstance(ImportCache::class.java)
    private val importedFiles = ConcurrentHashMap.newKeySet<VirtualFile>()


    fun add(files: Collection<VirtualFile>) {
        val wasEmpty = importedFiles.isEmpty()
        importedFiles.addAll(files)

        if (wasEmpty && files.isNotEmpty()) {
            PreprocessorUtil.clearCache()
            CssVarCompletionCache.clearCaches()
            ScopeUtil.clearCache(project)
        }
    }

    fun get(): Set<VirtualFile> = importedFiles.toSet()

    fun clear() {
        try {
            importedFiles.clear()
            PreprocessorUtil.clearCache()
            CssVarCompletionCache.clearCaches()
            ScopeUtil.clearCache(project)
        } catch (e: Exception) {
            LOG.warn("Error clearing ImportCache", e)
        }
    }

    override fun dispose() {
        try {
            clear()
            LOG.debug("ImportCache disposed for project: ${project.name}")
        } catch (e: Exception) {
            LOG.warn("Error disposing ImportCache", e)
        }
    }

    companion object {
        @JvmStatic
        fun get(project: Project): ImportCache =
            project.getService(ImportCache::class.java)
    }
}