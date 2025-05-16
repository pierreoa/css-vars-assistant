package cssvarsassistant.documentation

import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.lang.documentation.DocumentationMarkup
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.FileBasedIndex
import cssvarsassistant.index.CssVariableIndex
import cssvarsassistant.index.DELIMITER
import cssvarsassistant.model.DocParser

/**
 * Shows Quick‑Docs (Ctrl‑Q / hover) for CSS custom properties (var(--…)).
 * Registered via <lang.documentationProvider language="CSS|SCSS"/>.
 */
class CssVariableDocumentation : AbstractDocumentationProvider() {
    private val LOG = Logger.getInstance(CssVariableDocumentation::class.java)

    override fun generateDoc(element: PsiElement, original: PsiElement?): String? {
        return try {
            // 1) extract the var(--name) if present
            val varName = extractVariableName(element) ?: return null

            // 2) lookup in our index
            val values = FileBasedIndex.getInstance()
                .getValues(CssVariableIndex.NAME, varName, GlobalSearchScope.projectScope(element.project))
            if (values.isEmpty()) return null

            val raw = values.first()
            val value = raw.substringBefore(DELIMITER)
            val docText = raw.substringAfter(DELIMITER, "")
            val doc = DocParser.parse(docText, value)

            // 3) build HTML with DocumentationMarkup
            buildString {
                append(DocumentationMarkup.DEFINITION_START)
                append("CSS Variable: <b>").append(varName).append("</b>")
                if (doc.name.isNotBlank()) {
                    append(" (").append(doc.name).append(")")
                }
                append(DocumentationMarkup.DEFINITION_END)

                append(DocumentationMarkup.CONTENT_START)
                append("<p><b>Value:</b> ").append(value).append("</p>")

                if (doc.description.isNotBlank()) {
                    append("<p><b>Description:</b><br>").append(doc.description).append("</p>")
                }
                if (doc.examples.isNotEmpty()) {
                    append("<p><b>Example:</b></p><pre>")
                    doc.examples.forEach { append(it).append("\n") }
                    append("</pre>")
                }
                if (isColorValue(value)) {
                    append("<div style=\"display:inline-block;width:12px;height:12px;")
                    append("border:1px solid #aaa;background:$value;\"></div>")
                }
                append(DocumentationMarkup.CONTENT_END)
            }
        } catch (e: Exception) {
            LOG.error("Error generating CSS var docs", e)
            null
        }
    }

    override fun getQuickNavigateInfo(element: PsiElement, originalElement: PsiElement?): String? {
        // show a one‑liner on Ctrl‑hover
        val varName = extractVariableName(element) ?: return null
        val values = FileBasedIndex.getInstance()
            .getValues(CssVariableIndex.NAME, varName, GlobalSearchScope.projectScope(element.project))
        if (values.isEmpty()) return null
        val raw = values.first()
        val value = raw.substringBefore(DELIMITER)
        return "var($varName) = $value"
    }

    private fun extractVariableName(element: PsiElement): String? {
        // direct "--foo"
        val txt = element.text.trim()
        if (txt.startsWith("--")) return txt

        // look for var(--foo) in the surrounding text
        val match = "var\\((--[\\w-]+?)\\)".toRegex().find(element.parent?.text ?: return null)
        return match?.groupValues?.get(1)
    }

    private fun isColorValue(value: String): Boolean {
        val v = value.trim()
        return v.startsWith("#") || v.startsWith("rgb") || v.startsWith("hsl")
    }
}
