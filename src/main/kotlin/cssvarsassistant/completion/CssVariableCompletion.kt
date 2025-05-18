package cssvarsassistant.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.openapi.diagnostic.Logger
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.css.CssFunction
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.ui.ColorIcon
import cssvarsassistant.documentation.ColorParser
import cssvarsassistant.index.CssVariableIndex
import cssvarsassistant.index.DELIMITER
import cssvarsassistant.model.DocParser
import java.awt.Component
import java.awt.Graphics
import javax.swing.Icon

private const val ENTRY_SEP = "|||"

class CssVariableCompletion : CompletionContributor() {
    private val LOG = Logger.getInstance(CssVariableCompletion::class.java)

    init {
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement(),
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(
                    params: CompletionParameters,
                    ctx: ProcessingContext,
                    result: CompletionResultSet
                ) {
                    try {
                        val pos = params.position
                        val fn = PsiTreeUtil.getParentOfType(pos, CssFunction::class.java) ?: return
                        if (fn.name != "var") return
                        val l = fn.lParenthesis?.textOffset ?: return
                        val r = fn.rParenthesis?.textOffset ?: return
                        val off = params.offset
                        if (off <= l || off > r) return

                        val rawPref = result.prefixMatcher.prefix
                        val simple = rawPref.removePrefix("--")
                        val project = pos.project
                        val scope = GlobalSearchScope.projectScope(project)

                        data class Entry(
                            val rawName: String,
                            val display: String,
                            val valueEntries: List<Triple<String, String, String>>, // context, value, doc
                            val doc: String
                        )

                        val entries = mutableListOf<Entry>()
                        FileBasedIndex.getInstance()
                            .getAllKeys(CssVariableIndex.NAME, project)
                            .forEach { rawName ->
                                val display = rawName.removePrefix("--")
                                if (!display.startsWith(simple, ignoreCase = true)) return@forEach

                                val allVals = FileBasedIndex.getInstance()
                                    .getValues(CssVariableIndex.NAME, rawName, scope)
                                    .flatMap { it.split(ENTRY_SEP) }
                                    .filter { it.isNotBlank() }

                                if (allVals.isEmpty()) return@forEach

                                // Parse context/value/doc for each entry
                                val valueEntries = allVals.mapNotNull {
                                    val parts = it.split(DELIMITER, limit = 3)
                                    if (parts.size >= 2) {
                                        Triple(parts[0], parts[1], parts.getOrElse(2) { "" })
                                    } else null
                                }
                                // Find the default context if present
                                val default = valueEntries.find { it.first == "default" }
                                val main = default ?: valueEntries.first()
                                // Pick doc from first entry with doc-comment
                                val docEntry = valueEntries.firstOrNull { it.third.isNotBlank() } ?: main
                                val doc = DocParser.parse(docEntry.third, main.second).description

                                entries += Entry(rawName, display, valueEntries, doc)
                            }

                        entries.sortBy { it.display }

                        for (e in entries) {
                            val short = e.doc.takeIf { it.isNotBlank() }
                                ?.let { it.take(40) + if (it.length > 40) "…" else "" }
                                ?: ""

                            // Gather color values only (for icon rendering)
                            val colorVals = e.valueEntries.mapNotNull { ColorParser.parseCssColor(it.second) }
                            val mainVal =
                                (e.valueEntries.find { it.first == "default" } ?: e.valueEntries.first()).second

                            val (icon, tailText) = when {
                                colorVals.size == 2 -> DoubleColorIcon(
                                    ColorIcon(12, colorVals[0], false),
                                    ColorIcon(12, colorVals[1], false)
                                ) to
                                        e.valueEntries.take(2).joinToString(" / ") { it.second }

                                colorVals.size > 2 -> ColorIcon(
                                    12,
                                    colorVals[0],
                                    false
                                ) to "${mainVal} (+${colorVals.size - 1})"

                                colorVals.size == 1 -> ColorIcon(12, colorVals[0], false) to mainVal
                                else -> {
                                    val extraCount = e.valueEntries.size - 1
                                    val iconFallback =
                                        if (e.valueEntries.any { isSizeValue(it.second) }) AllIcons.FileTypes.Css
                                        else AllIcons.Nodes.Property
                                    iconFallback to (mainVal + if (extraCount > 0) " (+$extraCount)" else "")
                                }
                            }

                            val elt = LookupElementBuilder
                                .create(e.rawName)
                                .withPresentableText(e.display)
                                .withLookupString(e.display)
                                .withIcon(icon)
                                .withTypeText(tailText, true)
                                .withTailText(if (short.isNotBlank()) " — $short" else "", true)
                                .withInsertHandler { ctx2, _ ->
                                    ctx2.document.replaceString(ctx2.startOffset, ctx2.tailOffset, e.rawName)
                                }

                            result.addElement(elt)
                        }
                        result.stopHere()
                    } catch (ex: Exception) {
                        LOG.error("CSS var completion error", ex)
                    }
                }
            }
        )
    }

    private fun isSizeValue(raw: String): Boolean {
        return Regex("""^-?\d+(\.\d+)?(px|em|rem|ch|ex|vh|vw|vmin|vmax|%)$""", RegexOption.IGNORE_CASE)
            .matches(raw.trim())
    }
}

class DoubleColorIcon(private val icon1: Icon, private val icon2: Icon) : Icon {
    override fun getIconWidth() = icon1.iconWidth + icon2.iconWidth + 2
    override fun getIconHeight() = maxOf(icon1.iconHeight, icon2.iconHeight)
    override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
        icon1.paintIcon(c, g, x, y)
        icon2.paintIcon(c, g, x + icon1.iconWidth + 2, y)
    }
}
