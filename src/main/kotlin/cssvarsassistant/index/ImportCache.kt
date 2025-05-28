package cssvarsassistant.index

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.util.concurrent.ConcurrentHashMap

// 🔧  ⇩  Legg til Service-nivå
@Service(Service.Level.PROJECT)
class ImportCache {

    /** project → set of imported VirtualFiles */
    private val map = ConcurrentHashMap<Project, MutableSet<VirtualFile>>()

    fun add(project: Project, files: Collection<VirtualFile>) {
        map.computeIfAbsent(project) { ConcurrentHashMap.newKeySet() }.addAll(files)
    }

    fun get(project: Project): Set<VirtualFile> = map[project] ?: emptySet()


    fun clear(project: Project) {
        map[project]?.clear()
    }

    companion object {
        @JvmStatic
        fun get(project: Project): ImportCache =
            project.getService(ImportCache::class.java)
    }
}
