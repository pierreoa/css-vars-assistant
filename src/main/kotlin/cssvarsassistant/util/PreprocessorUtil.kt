package cssvarsassistant.util

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.containers.CollectionFactory
import com.intellij.util.indexing.FileBasedIndex
import cssvarsassistant.documentation.ResolutionInfo
import cssvarsassistant.index.PREPROCESSOR_VARIABLE_INDEX_NAME
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.*

/**
 * Resolves LESS / SCSS variables and simple arithmetic expressions.
 *
 * Supports both simple resolution (for completion) and step tracking (for documentation).
 */
object PreprocessorUtil {

    private val LOG = Logger.getInstance(PreprocessorUtil::class.java)

    /**
     * Cache key uses project.hashCode() instead of Project object to avoid
     * strong references that would pin the class-loader and block dynamic unload.
     * Now stores complete ResolutionInfo objects to preserve resolution steps.
     */
    private val cache = CollectionFactory.createConcurrentWeakKeySoftValueMap<Triple<Int, String, Int>, ResolutionInfo?>()

    /**
     * Simple resolution for completion - returns just the final resolved value.
     */
    fun resolveVariable(
        project: Project,
        varName: String,
        scope: GlobalSearchScope,
        visited: Set<String> = emptySet()
    ): String? {
        return resolveVariableWithSteps(project, varName, scope, visited).resolved
    }

    /**
     * Full resolution with step tracking for documentation.
     */
    fun resolveVariableWithSteps(
        project: Project,
        varName: String,
        scope: GlobalSearchScope,
        visited: Set<String> = emptySet(),
        steps: List<String> = emptyList()
    ): ResolutionInfo {
        ProgressManager.checkCanceled()
        if (varName in visited) return ResolutionInfo(varName, varName, steps)

        // Use project.hashCode() instead of project object for cache key
        val key = Triple(project.hashCode(), varName, scope.hashCode())
        cache[key]?.let { cachedResolutionInfo ->
            // **FIXED**: Return the complete cached ResolutionInfo with preserved steps
            return cachedResolutionInfo
        }

        return try {
            val values = FileBasedIndex.getInstance()
                .getValues(PREPROCESSOR_VARIABLE_INDEX_NAME, varName, scope)

            if (values.isEmpty()) return ResolutionInfo(varName, varName, steps)

            for (raw in values) {
                ProgressManager.checkCanceled()

                /* ---------- arithmetic expressions ----------------------------- */
                parseArithmetic(raw)?.let { (baseVar, op, rhsMaybe) ->
                    val baseResolution = resolveVariableWithSteps(
                        project, baseVar, scope, visited + varName, steps + "@$varName"
                    )
                    val baseVal = baseResolution.resolved

                    compute(baseVal, op, rhsMaybe)?.let { computed ->
                        val result = ResolutionInfo(
                            original = "@$varName",
                            resolved = computed,
                            steps = baseResolution.steps + "($baseVal $op ${rhsMaybe ?: ""}) = $computed"
                        )
                        // **FIXED**: Cache complete ResolutionInfo instead of just final value
                        cache[key] = result
                        return result
                    }
                }

                /* ---------- reference to another variable ------------------- */
                Regex("""^[\s]*[@$]([\w-]+)""").find(raw)?.let { m ->
                    val refVar = m.groupValues[1]
                    val resolution = resolveVariableWithSteps(
                        project, refVar, scope, visited + varName, steps + "@$varName"
                    )
                    val result = ResolutionInfo(
                        original = "@$varName",
                        resolved = resolution.resolved,
                        steps = resolution.steps
                    )
                    // **FIXED**: Cache complete ResolutionInfo instead of just final value
                    cache[key] = result
                    return result
                }

                /* ---------- literal value ----------------------------------- */
                val result = ResolutionInfo(
                    original = "@$varName",
                    resolved = raw,
                    steps = steps + "@$varName"
                )
                // **FIXED**: Cache complete ResolutionInfo instead of just final value
                cache[key] = result
                return result
            }

            ResolutionInfo(varName, varName, steps)
        } catch (e: ProcessCanceledException) {
            throw e
        } catch (e: Exception) {
            LOG.warn("Error resolving @$varName", e)
            ResolutionInfo(varName, varName, steps)
        }
    }

    /* --------------------------------------------------------------------- */
    /*  Helpers                                                              */
    /* --------------------------------------------------------------------- */

    /** Matches the full `(@foo * 0.5)` - style expression. */
    private val ARITH_RE = Regex(
        """\(\s*[@$]([\w-]+)\s*(\*\*|[*/%+\-]|min|max|floor|ceil|round)\s*([+-]?\d*\.?\d+)?\s*\)""",
        RegexOption.IGNORE_CASE
    )

    private fun parseArithmetic(raw: String): Triple<String, String, String?>? =
        ARITH_RE.find(raw)?.let { m ->
            val base = m.groupValues[1]
            val op = m.groupValues[2].lowercase()
            val rhs = m.groupValues.getOrNull(3)?.takeIf { it.isNotBlank() }
            Triple(base, op, rhs)
        }

    private val NUM_UNIT = Regex("""([+-]?\d*\.?\d+)([a-z%]+)""", RegexOption.IGNORE_CASE)

    /**
     * Computes `baseVal (op) rhs` and returns a formatted string with unit.
     * `rhs` may be null for unary ops.
     */
    private fun compute(baseVal: String, op: String, rhs: String?): String? {
        val m = NUM_UNIT.find(baseVal) ?: return null
        val num = m.groupValues[1].toDouble()
        val unit = m.groupValues[2]

        val rhsNum = rhs?.toDoubleOrNull()

        val resultNum = when (op) {
            "*" -> rhsNum?.let { num * it }
            "/" -> rhsNum?.let { num / it }
            "+" -> rhsNum?.let { num + it }
            "-" -> rhsNum?.let { num - it }
            "%" -> rhsNum?.let { num % it }
            "**" -> rhsNum?.let { num.pow(it) }
            "min" -> rhsNum?.let { min(num, it) }
            "max" -> rhsNum?.let { max(num, it) }
            "floor" -> floor(num)
            "ceil" -> ceil(num)
            "round" -> round(num)
            else -> null
        } ?: return null

        if (resultNum.isNaN() || resultNum.isInfinite()) return null

        return format(resultNum, unit)
    }

    /** Keeps integers pretty, decimals ≤ 3 dp. */
    private fun format(n: Double, unit: String): String =
        if (n % 1 == 0.0) "${n.roundToInt()}$unit"
        else "${"%.3f".format(n).trimEnd('0').trimEnd('.')}$unit"

    fun clearCache() = cache.clear()
}