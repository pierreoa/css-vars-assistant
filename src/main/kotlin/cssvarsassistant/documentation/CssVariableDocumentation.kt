package cssvarsassistant.documentation

import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.lang.documentation.DocumentationMarkup
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.FileBasedIndex
import cssvarsassistant.index.CssVariableIndex
import cssvarsassistant.index.DELIMITER
import cssvarsassistant.model.DocParser
import cssvarsassistant.settings.CssVarsAssistantSettings


/**
 * Quick‑doc / hover popup for CSS custom properties.
 * – Resolves alias chains  var(--x) → literal value
 * – Deduplicates identical (context,value) pairs
 * – Smart sorts: Default/Light → Dark → max‑width (large→small) →
 *   min‑width (small→large) → other media queries → alphabetical rest
 */
class CssVariableDocumentation : AbstractDocumentationProvider() {

    private val LOG = Logger.getInstance(CssVariableDocumentation::class.java)
    private val ENTRY_SEP = "|||"

    /*──────────────────────── main entry ────────────────────────*/

    override fun generateDoc(element: PsiElement, original: PsiElement?): String? {
        try {
            val varName = extractVariableName(element) ?: return null
            val scope = GlobalSearchScope.projectScope(element.project)

            // raw database entries -> List( context␟value␟comment )
            val rawEntries = FileBasedIndex.getInstance().getValues(CssVariableIndex.NAME, varName, scope)
                .flatMap { it.split(ENTRY_SEP) }.filter { it.isNotBlank() }

            if (rawEntries.isEmpty()) return null

            // parse + resolve aliases
            val parsed: List<Triple<String, String, String>> = rawEntries.mapNotNull {
                val p = it.split(DELIMITER, limit = 3)
                if (p.size >= 2) {
                    val ctx = p[0]
                    val value = resolveVarValue(p[1], scope)
                    val comment = p.getOrElse(2) { "" }
                    Triple(ctx, value, comment)
                } else null
            }

            // collapse identical (context, value) rows
            val unique = parsed.distinctBy { it.first to it.second }

            // sort according to rank()
            val settings = CssVarsAssistantSettings.getInstance()
            val sorted = unique.sortedWith(
                compareBy(
                    { rank(it.first).first },                  // gruppe 0‑5
                    { rank(it.first).second ?: Int.MAX_VALUE }, // tall‑hint (null ⇒ sist)
                    { rank(it.first).third }                   // alfabetisk fallback
                )
            )


            // pick entry with doc comments (if any)
            val docEntry =
                unique.firstOrNull { it.third.isNotBlank() } ?: unique.find { it.first == "default" } ?: unique.first()
            val doc = DocParser.parse(docEntry.third, docEntry.second)

            /*────────── HTML build ──────────*/
            val sb = StringBuilder()
            sb.append("<html><body>").append(DocumentationMarkup.DEFINITION_START)
            if (doc.name.isNotBlank()) sb.append("<b>").append(doc.name).append("</b><br/>")
            sb.append("<small>CSS Variable: <code>$varName</code></small>").append(DocumentationMarkup.DEFINITION_END)
                .append(DocumentationMarkup.CONTENT_START)

            sb.append("<p><b>Values:</b></p>")
                .append("<table>")
                .append("<tr><td>Context</td>")
                .append("<td>&nbsp;</td>")
                .append("<td align='left'>Value</td></tr>")

            for ((ctx, value, _) in sorted) {
                val isColour = ColorParser.parseCssColor(value) != null
                sb.append("<tr><td style='color:#888;padding-right:10px'>")
                    .append(contextLabel(ctx))
                    .append("</td><td>")
                if (isColour) sb.append(colorSwatchHtml(value)) else sb.append("&nbsp;")
                sb.append("</td><td>")
                    .append(StringUtil.escapeXmlEntities(value))
                    .append("</td></tr>")
            }
            sb.append("</table>")

            if (doc.description.isNotBlank()) sb.append("<p><b>Description:</b><br/>")
                .append(StringUtil.escapeXmlEntities(doc.description)).append("</p>")

            if (doc.examples.isNotEmpty()) {
                sb.append("<p><b>Examples:</b></p><pre>")
                doc.examples.forEach { sb.append(StringUtil.escapeXmlEntities(it)).append('\n') }
                sb.append("</pre>")
            }

            // WebAIM link for first colour value
            sorted.mapNotNull { ColorParser.parseCssColor(it.second) }.firstOrNull()?.let { c ->
                val hex = "%02x%02x%02x".format(c.red, c.green, c.blue)
                sb.append(
                    """<p style='margin-top:10px'>
                             |<a target="_blank"
                             |   href="https://webaim.org/resources/contrastchecker/?fcolor=$hex&bcolor=000000">
                             |Check contrast on WebAIM Contrast Checker
                             |</a></p>""".trimMargin()
                )
            }

            sb.append(DocumentationMarkup.CONTENT_END).append("</body></html>")

            return sb.toString()
        } catch (e: Exception) {
            LOG.error("Error generating documentation", e)
            return null
        }
    }

    /*──────────────────── helper functions ────────────────────*/

    /** Resolve a var(--alias) chain to its literal value (max 5 hops). */
    private fun resolveVarValue(
        raw: String, scope: GlobalSearchScope, visited: Set<String> = emptySet(), depth: Int = 0
    ): String {
        if (depth > 5) return raw
        val m = Regex("""var\(\s*(--[\w-]+)\s*\)""").find(raw) ?: return raw
        val ref = m.groupValues[1]
        if (ref in visited) return raw                          // cyclic guard

        val entries =
            FileBasedIndex.getInstance().getValues(CssVariableIndex.NAME, ref, scope).flatMap { it.split(ENTRY_SEP) }
                .filter { it.isNotBlank() }

        val defValue = entries.mapNotNull {
            val p = it.split(DELIMITER, limit = 3)
            if (p.size >= 2) p[0] to p[1] else null
        }.let { pairs ->
            pairs.find { it.first == "default" }?.second ?: pairs.firstOrNull()?.second
        } ?: return raw

        return resolveVarValue(defValue, scope, visited + ref, depth + 1)
    }

    private fun contextLabel(ctx: String): String = when {
        ctx == "default" -> "Default"
        "prefers-color-scheme" in ctx.lowercase() && "light" in ctx.lowercase() -> "Light"
        "prefers-color-scheme" in ctx.lowercase() && "dark" in ctx.lowercase() -> "Dark"
        Regex("""max-width:\s*(\d+)""").find(ctx) != null -> "≤${Regex("""max-width:\s*(\d+)""").find(ctx)!!.groupValues[1]}px"

        Regex("""min-width:\s*(\d+)""").find(ctx) != null -> "≥${Regex("""min-width:\s*(\d+)""").find(ctx)!!.groupValues[1]}px"

        else -> ctx
    }

    private fun extractVariableName(el: PsiElement): String? = el.text.trim().takeIf { it.startsWith("--") }
        ?: el.parent?.text?.let { Regex("""var\(\s*(--[\w-]+)\s*\)""").find(it)?.groupValues?.get(1) }

    private fun colorSwatchHtml(cssValue: String): String {
        val c = ColorParser.parseCssColor(cssValue) ?: return "&nbsp;"
        val hex = "#%02x%02x%02x".format(c.red, c.green, c.blue)
        return """<font color="$hex">&#9632;</font>"""
    }

    /**
     * Assign an ordering tuple (group, numericHint, ctx) used for sorting rows:
     * 0 = Default / Light, 1 = Dark, 2 = max‑width (‑largest → ‑smallest),
     * 3 = min‑width (smallest → largest), 4 = other media, 5 = rest alpha.
     */
    private fun rank(ctx: String): Triple<Int, Int?, String> {
        val c = ctx.lowercase()

        if (c == "default" || ("prefers-color-scheme" in c && "light" in c)) return Triple(0, null, c)

        if ("prefers-color-scheme" in c && "dark" in c) return Triple(1, null, c)

        Regex("""max-width:\s*(\d+)(px|rem|em)?""").find(c)?.let {
            return Triple(2, -it.groupValues[1].toInt(), c)
        }
        Regex("""min-width:\s*(\d+)(px|rem|em)?""").find(c)?.let {
            return Triple(3, it.groupValues[1].toInt(), c)
        }

        if (arrayOf("hover", "motion", "orientation", "print").any { it in c }) return Triple(4, null, c)

        return Triple(5, null, c)
    }
}
