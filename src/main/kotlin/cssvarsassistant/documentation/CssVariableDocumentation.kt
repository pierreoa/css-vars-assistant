package cssvarsassistant.documentation

import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.lang.documentation.DocumentationMarkup
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.ui.ColorIcon
import cssvarsassistant.index.CssVariableIndex
import cssvarsassistant.index.DELIMITER
import cssvarsassistant.model.DocParser

class CssVariableDocumentation : AbstractDocumentationProvider() {
    private val LOG = Logger.getInstance(CssVariableDocumentation::class.java)
    private val ENTRY_SEP = "|||"

    override fun generateDoc(element: PsiElement, original: PsiElement?): String? {
        try {
            val varName = extractVariableName(element) ?: return null

            val allEntries = FileBasedIndex.getInstance()
                .getValues(CssVariableIndex.NAME, varName, GlobalSearchScope.projectScope(element.project))
                .flatMap { it.split(ENTRY_SEP) }
                .filter { it.isNotBlank() }

            if (allEntries.isEmpty()) return null

            // Parse out context, value, doc for each declaration
            val valueEntries = allEntries.mapNotNull {
                val parts = it.split(DELIMITER, limit = 3)
                if (parts.size >= 2) Triple(parts[0], parts[1], parts.getOrElse(2) { "" })
                else null
            }

            // Always show "default" first, then others
            val sortedEntries = valueEntries.sortedWith(compareBy({ it.first != "default" }, { it.first }))

            // Get doc-comment (from the first with one, fallback to "default", fallback to first)
            val docEntry = valueEntries.firstOrNull { it.third.isNotBlank() }
                ?: valueEntries.find { it.first == "default" }
                ?: valueEntries.first()
            val doc = DocParser.parse(docEntry.third, docEntry.second)

            val sb = StringBuilder()
            sb.append("<html><body>")
            sb.append(DocumentationMarkup.DEFINITION_START)
            if (doc.name.isNotBlank()) {
                sb.append("<b>").append(doc.name).append("</b><br>")
            }
            sb.append("<small>CSS Variable: <code>").append(varName).append("</code></small>")
            sb.append(DocumentationMarkup.DEFINITION_END)
            sb.append(DocumentationMarkup.CONTENT_START)

            sb.append("<p><b>Value${if (sortedEntries.size > 1) "s" else ""}:</b></p>")
            sb.append("<table>")
            sb.append("<tr><th align='left'>Context</th><th></th><th align='left'>Value</th></tr>")
            for ((context, value, _) in sortedEntries) {
                sb.append("<tr>")
                sb.append("<td style='padding-right:10px;color:#888;'>")
                sb.append(contextLabel(context)).append("</td>")

                sb.append("<td style='padding-right:4px;'>")
                sb.append(colorSwatchHtml(value)).append("</td>")

                sb.append("<td>")
                sb.append(StringUtil.escapeXmlEntities(value)).append("</td>")
                sb.append("</tr>")
            }
            sb.append("</table>")

            if (doc.description.isNotBlank()) {
                sb.append("<p><b>Description:</b><br>")
                    .append(StringUtil.escapeXmlEntities(doc.description))
                    .append("</p>")
            }
            if (doc.examples.isNotEmpty()) {
                sb.append("<p><b>Examples:</b></p><pre>")
                doc.examples.forEach {
                    sb.append(StringUtil.escapeXmlEntities(it)).append("\n")
                }
                sb.append("</pre>")
            }

            sb.append("<p style='margin-top:10px'>")

            val colorValue = sortedEntries
                .map { it.second }.firstNotNullOfOrNull { ColorParser.parseCssColor(it) }

            if (colorValue != null) {
                // Format as 6-digit hex, no '#'
                val hex = "%02x%02x%02x".format(colorValue.red, colorValue.green, colorValue.blue)
                // Set a default bg (or detect if you want)
                val bg = "000000"
                sb.append(
                    """
        <a href="https://webaim.org/resources/contrastchecker/?fcolor=$hex&bcolor=$bg" target="_blank" style="text-decoration:underline;">
            Check contrast on WebAIM Contrast Checker
        </a>
        """.trimIndent()
                )
            }

            sb.append("</p>")


            sb.append(DocumentationMarkup.CONTENT_END)
            sb.append("</body></html>")

            LOG.info("Generated CSS-var doc HTML:\n$sb")
            return sb.toString()
        } catch (e: Exception) {
            LOG.error("Error generating documentation", e)
            return null
        }
    }

    private fun contextLabel(context: String): String {
        return when {
            context == "default" -> "Default"
            "dark" in context.lowercase() -> "Dark"
            Regex("""max-width:\s*(\d+)""").find(context)?.groupValues?.getOrNull(1) != null ->
                "≤${Regex("""max-width:\s*(\d+)""").find(context)?.groupValues?.get(1)}px"

            Regex("""min-width:\s*(\d+)""").find(context)?.groupValues?.getOrNull(1) != null ->
                "≥${Regex("""min-width:\s*(\d+)""").find(context)?.groupValues?.get(1)}px"

            else -> context
        }
    }

    private fun extractVariableName(element: PsiElement): String? {
        val txt = element.text.trim()
        if (txt.startsWith("--")) return txt
        return element.parent?.text
            ?.let { Regex("var\\((--[\\w-]+)\\)").find(it)?.groupValues?.get(1) }
    }

    private fun colorSwatchHtml(color: String): String {
        val awtColor = ColorParser.parseCssColor(color) ?: return ""
        val icon = ColorIcon(16, awtColor, false)
        val dataUri = icon.toPngDataUri()
        return """<img src="$dataUri" width="16" height="16" style="vertical-align:middle; margin-right:6px;"/>"""
    }
}
