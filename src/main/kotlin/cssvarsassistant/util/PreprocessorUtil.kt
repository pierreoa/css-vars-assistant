package cssvarsassistant.util

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope

object PreprocessorUtil {
    private val LOG = Logger.getInstance(PreprocessorUtil::class.java)
    private val cache = mutableMapOf<Triple<Project, String, Int>, String?>()

    /**
     * Finn verdien til en LESS/SCSS/CSS-variabel (@foo, $foo, --foo) uansett filnavn.
     */
    fun resolveVariable(
        project: Project,
        varName: String,
        scope: GlobalSearchScope,
        visited: Set<String> = emptySet()
    ): String? {
        if (varName in visited) return null          // syklussikring
        val key = Triple(project, varName, scope.hashCode())
        cache[key]?.let { return it }

        try {
            for (ext in listOf("less", "scss", "sass", "css")) {
                // Check for cancellation before expensive operations
                ProgressManager.checkCanceled()

                // This line was causing ProcessCanceledException
                val files = FilenameIndex.getAllFilesByExt(project, ext, scope)

                for (vf in files) {
                    // Check for cancellation in loops
                    ProgressManager.checkCanceled()

                    val text = String(vf.contentsToByteArray())

                    // LESS / SCSS / CSS matcher
                    val value = when {
                        Regex("""@${Regex.escape(varName)}\s*:\s*([^;]+);""")
                            .find(text) != null ->
                            Regex("""@${Regex.escape(varName)}\s*:\s*([^;]+);""")
                                .find(text)!!.groupValues[1].trim()

                        Regex("""\$${Regex.escape(varName)}\s*:\s*([^;]+);""")
                            .find(text) != null ->
                            Regex("""\$${Regex.escape(varName)}\s*:\s*([^;]+);""")
                                .find(text)!!.groupValues[1].trim()

                        Regex("""--${Regex.escape(varName)}\s*:\s*([^;]+);""")
                            .find(text) != null ->
                            Regex("""--${Regex.escape(varName)}\s*:\s*([^;]+);""")
                                .find(text)!!.groupValues[1].trim()

                        else -> null
                    }

                    // fant definisjon?
                    if (value != null) {
                        // peker det fortsatt på en @foo / $foo ?
                        val refMatch = Regex("""^[\s]*[@$]([\w-]+)""").find(value)
                        val resolved = if (refMatch != null) {
                            resolveVariable(project, refMatch.groupValues[1], scope, visited + varName)
                                ?: value                       // fall tilbake
                        } else {
                            value
                        }

                        cache[key] = resolved                // cache KUN når vi har noe
                        return resolved
                    }
                }
            }
            return null                                      // ikke funnet → ingen cache
        } catch (e: ProcessCanceledException) {
            // CRITICAL: Always rethrow ProcessCanceledException
            throw e
        } catch (e: Exception) {
            LOG.warn("Error resolving preprocessor variable: $varName", e)
            return null
        }
    }

    fun clearCache() = cache.clear()
}