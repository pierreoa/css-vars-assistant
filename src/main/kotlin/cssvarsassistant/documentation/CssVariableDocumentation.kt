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

            val valueEntries = allEntries.mapNotNull {
                val parts = it.split(DELIMITER, limit = 3)
                if (parts.size >= 2) Triple(parts[0], parts[1], parts.getOrElse(2) { "" })
                else null
            }

            val settings = CssVarsAssistantSettings.getInstance()
            val sortedEntries = if (settings.showContextValues) {
                valueEntries.sortedWith(compareBy({ it.first != "default" }, { it.first }))
            } else {
                valueEntries.filter { it.first == "default" }.ifEmpty { valueEntries.take(1) }
            }

            val docEntry = valueEntries.firstOrNull { it.third.isNotBlank() }
                ?: valueEntries.find { it.first == "default" }
                ?: valueEntries.first()
            val doc = DocParser.parse(docEntry.third, docEntry.second)

            val sb = StringBuilder()
            sb.append("<html><body>")
            sb.append(DocumentationMarkup.DEFINITION_START)
            if (doc.name.isNotBlank()) {
                sb.append("<b>").append(doc.name).append("</b><br/>")
            }
            sb.append("<small>CSS Variable: <code>").append(varName).append("</code></small>")
            sb.append(DocumentationMarkup.DEFINITION_END)
            sb.append(DocumentationMarkup.CONTENT_START)

            if (settings.showContextValues && sortedEntries.size > 1) {
                sb.append("<p><b>Values:</b></p>")
                sb.append("<table>")
                // Put a non-breaking space in the color swatch header
                sb.append("<tr><th style='text-align:left; vertical-align:middle;'>Context</th><th style='text-align:left; vertical-align:middle;'>&nbsp;</th><th style='text-align:left; vertical-align:middle;'>Value</th></tr>")
                for ((context, value, _) in sortedEntries) {
                    val isColor = ColorParser.parseCssColor(value) != null
                    sb.append("<tr>")
                    sb.append("<td style='padding-right:10px;color:#888;'>")
                    sb.append(contextLabel(context)).append("</td>")
                    sb.append("<td style='padding-right:4px;vertical-align:middle;'>")
                    if (isColor) {
                        sb.append(colorSwatchHtml(value))
                    } else {
                        sb.append("&nbsp;")
                    }
                    sb.append("</td>")
                    sb.append("<td style='vertical-align:middle;'>")
                    sb.append(StringUtil.escapeXmlEntities(value)).append("</td>")
                    sb.append("</tr>")
                }
                sb.append("</table>")
            } else {
                sb.append("<p><b>Value:</b></p><br/>")
                sb.append("<code>")
                sb.append(StringUtil.escapeXmlEntities(sortedEntries.first().second)).append("</code>")
            }

            if (doc.description.isNotBlank()) {
                sb.append("<p><b>Description:</b><br/>")
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
            // WebAIM link for color variables
            val colorValue = sortedEntries
                .map { it.second }
                .firstNotNullOfOrNull { ColorParser.parseCssColor(it) }
            if (colorValue != null) {
                val hex = "%02x%02x%02x".format(colorValue.red, colorValue.green, colorValue.blue)
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
        // Try to parse to a Color object
        val awtColor = ColorParser.parseCssColor(color) ?: return "&nbsp;" // Only show swatch if it's a color
        // Convert to #RRGGBB
        val hex = "#%02x%02x%02x".format(awtColor.red, awtColor.green, awtColor.blue)
        // Use the original value as label
        return """<font color="$hex">&#9632;</font>"""
    }
}
