package cssvarsassistant.documentation.v1

import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.lang.documentation.DocumentationMarkup
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.util.indexing.FileBasedIndex
import cssvarsassistant.completion.CssVarCompletionCache
import cssvarsassistant.completion.CssVariableCompletion
import cssvarsassistant.documentation.ColorParser
import cssvarsassistant.documentation.ResolutionInfo
import cssvarsassistant.documentation.v2.colorSwatchHtml
import cssvarsassistant.documentation.v2.contextLabel
import cssvarsassistant.documentation.v2.toHex
import cssvarsassistant.index.CSS_VARIABLE_INDEXER_NAME
import cssvarsassistant.index.DELIMITER
import cssvarsassistant.model.DocParser
import cssvarsassistant.settings.CssVarsAssistantSettings
import cssvarsassistant.util.PreprocessorUtil
import cssvarsassistant.util.RankUtil.rank
import cssvarsassistant.util.ScopeUtil
import cssvarsassistant.util.ValueUtil
import kotlin.math.pow
import kotlin.math.roundToInt


val ENTRY_SEP = "|||"

class CssVariableDocumentation : AbstractDocumentationProvider() {
    private val logger = Logger.getInstance(CssVariableCompletion::class.java)


    /* ---------------------------------------------------------------------- */
    /*  generateDoc()                                                         */
    /* ---------------------------------------------------------------------- */
    override fun generateDoc(element: PsiElement, original: PsiElement?): String? {
        try {
            val project = element.project
            if (DumbService.isDumb(project)) return null
            ProgressManager.checkCanceled()

            val settings = CssVarsAssistantSettings.getInstance()
            val varName = extractVariableName(element) ?: return null
            val cssScope = ScopeUtil.effectiveCssIndexingScope(project, settings)

            /* ------------------- gather all values for this variable -------- */
            val rawEntries = FileBasedIndex.getInstance()
                .getValues(CSS_VARIABLE_INDEXER_NAME, varName, cssScope)
                .flatMap { it.split(ENTRY_SEP) }
                .filter { it.isNotBlank() }

            if (rawEntries.isEmpty()) return null

            // parsed = Triple(context, ResolveInfo, comment)
            val parsed = rawEntries.mapNotNull {
                val parts = it.split(DELIMITER, limit = 3)
                if (parts.size >= 2) {
                    val ctx = parts[0]
                    val resInfo = resolveVarValue(project, parts[1])
                    val comment = parts.getOrElse(2) { "" }
                    Triple(ctx, resInfo, comment)
                } else null
            }

            // collapse «last wins» per context
            val collapsed = parsed
                .groupBy { it.first }
                .mapValues { (_, list) -> list.last() }
                .values.toList()

            val sorted = collapsed.sortedWith(
                compareBy(
                    { rank(it.first).first },
                    { rank(it.first).second ?: Int.MAX_VALUE },
                    { rank(it.first).third }
                )
            )

            /* ---------- Doc-parser for heading/description ------------------ */
            val docEntry = collapsed.firstOrNull { it.third.isNotBlank() }
                ?: collapsed.find { it.first == "default" }
                ?: collapsed.first()
            val doc = DocParser.parse(docEntry.third, docEntry.second.resolved)

            /* -------------- do we need extra columns? ------------------------- */
            val showPixelCol = sorted.any { (_, res, _) ->
                if (!ValueUtil.isSizeValue(res.resolved)) return@any false
                val unit = res.resolved.replace(Regex("[0-9.+\\-]"), "").trim().lowercase()
                val pxVal = ValueUtil.convertToPixels(res.resolved)
                val numericRaw = res.resolved.replace(Regex("[^0-9.+\\-]"), "")
                    .toDoubleOrNull() ?: pxVal
                unit != "px" || pxVal.roundToInt() != numericRaw.roundToInt()
            }
            val showWcagCol = sorted.any { (_, r, _) -> ColorParser.parseCssColor(r.resolved) != null }
            val showHexCol = sorted.any { (_, r, _) ->
                ColorParser.parseCssColor(r.resolved)?.let { !r.resolved.trim().startsWith("#") } ?: false
            }
            /* ── inline-CSS helpers (survive IntelliJ trimming) ────────────────────── */
            val headerStyle = "style='color:#F2F2F2;font-size:16px;padding:2px 4px;border-bottom:1px solid #BABABA;'"
            val rowStyle = "style='white-space:nowrap;padding:2px 4px;color:#BABABA;font-size:14px;'"
            val rowResolvedStyle = "style='white-space:nowrap;padding:2px 4px;color:#F2F2F2;font-size:10px!important;'"

            /* ── builder start ─────────────────────────────────────────────────────── */
            val sb = StringBuilder()
                .append(DocumentationMarkup.DEFINITION_START)

            if (doc.name.isNotBlank())
                sb.append("<b>").append(StringUtil.escapeXmlEntities(doc.name)).append("</b><br/>")

            sb.append("<small>CSS Variable: <code>")
                .append(StringUtil.escapeXmlEntities(varName))
                .append("</code></small>")
                .append(DocumentationMarkup.DEFINITION_END)
                .append(DocumentationMarkup.CONTENT_START)

            /* ── table header ─────────────────────────────────────────────────────── */
            sb.append(
                """
        <p><b>Values:</b></p>
        <table style="border-collapse:collapse;table-layout:auto;font-size:10px;">
          <tr>
            <th $headerStyle><nobr>Context</nobr></th>
            <th $headerStyle>&nbsp;</th>
            <th $headerStyle><nobr>Value</nobr></th>
            <th $headerStyle><nobr>Type</nobr></th>
            <th $headerStyle><nobr>Source</nobr></th>""".trimIndent()
            )
            if (showPixelCol) sb.append("<th $headerStyle><nobr>px&nbsp;Eq.</nobr></th>")
            if (showHexCol) sb.append("<th $headerStyle><nobr>Hex</nobr></th>")
            if (showWcagCol) sb.append("<th $headerStyle><nobr>WCAG</nobr></th>")
            sb.append("</tr>")

            /* ── table rows ───────────────────────────────────────────────────────── */
            sorted.forEach { (ctx, resInfo, _) ->
                ProgressManager.checkCanceled()

                val rawValue = resInfo.resolved
                val colorObj = ColorParser.parseCssColor(rawValue)
                val isColour = colorObj != null
                val pixelEq = if (ValueUtil.isSizeValue(rawValue))
                    "${ValueUtil.convertToPixels(rawValue).roundToInt()}px" else "—"
                val typeStr = ValueUtil.getValueType(rawValue).name
                val sourceStr = resInfo.steps.firstOrNull() ?: "—"
                val contrast = colorObj?.let {
                    // WCAG contrast vs black
                    val l = listOf(it.red, it.green, it.blue).map { ch ->
                        val v = ch / 255.0
                        if (v <= 0.03928) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
                    }.let { 0.2126 * it[0] + 0.7152 * it[1] + 0.0722 * it[2] }
                    "%.2f:1".format((1.05) / (l + 0.05))
                } ?: "—"
                val hexValue = colorObj?.toHex() ?: "—"

                /* –– row –– */
                sb.append("<tr>")
                    .append("<td $rowStyle><nobr>${StringUtil.escapeXmlEntities(contextLabel(ctx, isColour))}</nobr></td>")
                    .append("<td $rowStyle><nobr>${if (isColour) colorSwatchHtml(rawValue) else "&nbsp;"}</nobr></td>")
                    .append("<td $rowStyle><nobr>")
                    .append(StringUtil.escapeXmlEntities(rawValue).lowercase())

                if (resInfo.steps.isNotEmpty() && resInfo.original != resInfo.resolved)
                    sb.append(
                        """&nbsp;<span title="${StringUtil.escapeXmlEntities(resInfo.steps.joinToString(" → "))}" 
                            $rowResolvedStyle>↗</span>"""
                    )
                sb.append("</nobr></td>")
                    .append("<td $rowStyle><nobr>${StringUtil.escapeXmlEntities(typeStr)}</nobr></td>")
                    .append("<td $rowStyle><nobr>${StringUtil.escapeXmlEntities(sourceStr)}</nobr></td>")

                if (showPixelCol) sb.append("<td $rowStyle><nobr>$pixelEq</nobr></td>")
                if (showHexCol) sb.append("<td $rowStyle><nobr>$hexValue</nobr></td>")
                if (showWcagCol) sb.append("<td $rowStyle><nobr>$contrast</nobr></td>")

                sb.append("</tr>")
            }
            sb.append("</table>")

            /* ---------------- Description / Examples ------------------------ */
            if (doc.description.isNotBlank()) {
                sb.append("<p><b>Description:</b><br/>")
                    .append(StringUtil.escapeXmlEntities(doc.description)).append("</p>")
            }

            if (doc.examples.isNotEmpty()) {
                sb.append("<p><b>Examples:</b></p><pre>")
                doc.examples.forEach { sb.append(StringUtil.escapeXmlEntities(it)).append('\n') }
                sb.append("</pre>")
            }

            /* -------------- Quick link to WebAIM contrast checker ------------ */
            sorted
                .mapNotNull { ColorParser.parseCssColor(it.second.resolved) }
                .firstOrNull()
                ?.let { c ->
                    val hex = "%02x%02x%02x".format(c.red, c.green, c.blue)
                    sb.append(
                        """<p style='margin-top:10px'>
                               <a target="_blank"
                                  href="https://webaim.org/resources/contrastchecker/?fcolor=$hex&bcolor=000000">
                                  Check contrast on WebAIM Contrast Checker
                               </a></p>"""
                    )
                }

            sb.append(DocumentationMarkup.CONTENT_END).append("</body></html>")
            return sb.toString()
        } catch (e: ProcessCanceledException) {
            throw e
        } catch (e: Exception) {
            logger.error("Error generating documentation", e)
            return null
        }
    }


    private fun resolveVarValue(
        project: Project,
        raw: String,
        visited: Set<String> = emptySet(),
        depth: Int = 0,
        steps: List<String> = emptyList()
    ): ResolutionInfo {
        val settings = CssVarsAssistantSettings.getInstance()
        if (depth > settings.maxImportDepth) return ResolutionInfo(raw, raw, steps)

        try {
            ProgressManager.checkCanceled()

            Regex("""var\(\s*(--[\w-]+)\s*\)""").find(raw)?.let { m ->
                val ref = m.groupValues[1]
                if (ref !in visited) {
                    val newSteps = steps + "var($ref)"
                    val cssScope = ScopeUtil.effectiveCssIndexingScope(project, settings)
                    val entries = FileBasedIndex.getInstance()
                        .getValues(CSS_VARIABLE_INDEXER_NAME, ref, cssScope)
                        .flatMap { it.split(ENTRY_SEP) }
                        .filter { it.isNotBlank() }

                    val defVal = entries.mapNotNull {
                        val p = it.split(DELIMITER, limit = 3)
                        if (p.size >= 2) p[0] to p[1] else null
                    }.let { list ->
                        list.find { it.first == "default" }?.second ?: list.firstOrNull()?.second
                    }

                    if (defVal != null)
                        return resolveVarValue(project, defVal, visited + ref, depth + 1, newSteps)
                }
                return ResolutionInfo(raw, raw, steps)
            }

            val preprocessorMatch = Regex("""^[\s]*[@$]([\w-]+)$""").find(raw.trim())
            if (preprocessorMatch != null) {
                val varName = preprocessorMatch.groupValues[1]
                val prefix = if (raw.contains("@")) "@" else "$"
                val currentScope = ScopeUtil.currentPreprocessorScope(project)

                CssVarCompletionCache.get(project, varName, currentScope)?.let {
                    return ResolutionInfo(raw, it, steps + "$prefix$varName")
                }

                val resolved = findPreprocessorVariableValue(project, varName)
                if (resolved != null && resolved != raw) {
                    CssVarCompletionCache.put(project, varName, currentScope, resolved)
                    return ResolutionInfo(raw, resolved, steps + "$prefix$varName")
                }
            }

            return ResolutionInfo(raw, raw, steps)
        } catch (e: ProcessCanceledException) {
            throw e
        } catch (e: Exception) {
            logger.warn("Failed to resolve variable in documentation: $raw", e)
            return ResolutionInfo(raw, raw, steps)
        }
    }

    private fun findPreprocessorVariableValue(project: Project, varName: String): String? {
        return try {
            val freshScope = ScopeUtil.currentPreprocessorScope(project)
            PreprocessorUtil.resolveVariable(project, varName, freshScope)
        } catch (e: ProcessCanceledException) {
            throw e
        } catch (e: Exception) {
            logger.warn("Failed to find preprocessor variable in doc-menu: $varName", e)
            null
        }
    }


    private fun extractVariableName(el: PsiElement): String? = el.text.trim().takeIf { it.startsWith("--") }
        ?: el.parent?.text?.let { Regex("""var\(\s*(--[\w-]+)\s*\)""").find(it)?.groupValues?.get(1) }

}