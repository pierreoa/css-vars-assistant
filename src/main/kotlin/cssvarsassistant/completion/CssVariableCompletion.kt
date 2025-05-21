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
import cssvarsassistant.settings.CssVarsAssistantSettings
import java.awt.Component
import java.awt.Graphics
import javax.swing.Icon

class CssVariableCompletion : CompletionContributor() {
    private val LOG = Logger.getInstance(CssVariableCompletion::class.java)
    private val ENTRY_SEP = "|||"


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
                        // Only inside var(...) args
                        val pos = params.position
                        val fn = PsiTreeUtil.getParentOfType(pos, CssFunction::class.java) ?: return
                        if (fn.name != "var") return
                        val l = fn.lParenthesis?.textOffset ?: return
                        val r = fn.rParenthesis?.textOffset ?: return
                        val off = params.offset
                        if (off <= l || off > r) return

                        // Prefix without leading --
                        val rawPref = result.prefixMatcher.prefix
                        val simple = rawPref.removePrefix("--")
                        val project = pos.project

                        // Get settings
                        val settings = CssVarsAssistantSettings.getInstance()

                        // Choose scope based on settings
                        val scope = if (settings.useGlobalSearchScope) {
                            GlobalSearchScope.allScope(project)
                        } else {
                            GlobalSearchScope.projectScope(project)
                        }

                        // Track which variables we've processed to avoid duplicates
                        val processedVariables = mutableSetOf<String>()

                        fun resolveVarValue(raw: String, visited: Set<String> = emptySet()): String {
                            val m = Regex("""var\(\s*(--[\w-]+)\s*\)""").find(raw)
                            return if (m != null) {
                                val ref = m.groupValues[1]
                                if (ref in visited) raw
                                else {
                                    // fetch all entries for ref
                                    val refEntries = FileBasedIndex.getInstance()
                                        .getValues(CssVariableIndex.NAME, ref, scope)
                                        .flatMap { it.split(ENTRY_SEP) }
                                        .distinct()
                                        .filter { it.isNotBlank() }
                                    // find default or first
                                    val refDefault = refEntries
                                        .mapNotNull {
                                            val p = it.split(DELIMITER, limit = 3)
                                            if (p.size >= 2) p[0] to p[1] else null
                                        }
                                        .let { pairs ->
                                            pairs.find { it.first == "default" }?.second
                                                ?: pairs.firstOrNull()?.second
                                        }
                                    if (refDefault != null)
                                        resolveVarValue(refDefault, visited + ref)
                                    else
                                        raw
                                }
                            } else raw
                        }

                        data class Entry(
                            val rawName: String,
                            val display: String,
                            val mainValue: String,
                            val allValues: List<Pair<String, String>>, // context, value
                            val doc: String,
                            val isAllColor: Boolean
                        )

                        val entries = mutableListOf<Entry>()
                        FileBasedIndex.getInstance()
                            .getAllKeys(CssVariableIndex.NAME, project)
                            .forEach { rawName ->
                                val display = rawName.removePrefix("--")
                                if (!display.startsWith(simple, ignoreCase = true)) return@forEach

                                // Add this variable name to our processed set
                                processedVariables.add(rawName)

                                val allVals = FileBasedIndex.getInstance()
                                    .getValues(CssVariableIndex.NAME, rawName, scope)
                                    .flatMap { it.split(ENTRY_SEP) }
                                    .distinct()
                                    .filter { it.isNotBlank() }


                                if (allVals.isEmpty()) return@forEach

                                // List of context-value pairs
                                val valuePairs = allVals.mapNotNull {
                                    val parts = it.split(DELIMITER, limit = 3)
                                    if (parts.size >= 2) {
                                        val ctx = parts[0]
                                        val rawVal = parts[1]
                                        val resolved = resolveVarValue(rawVal)
                                        ctx to resolved
                                    } else null
                                }

                                // ─── DEDUPE ─────────────────────────────────────────────────────
                                // if the same var is declared twice in the same context with the same value,
                                // collapse it down to a single entry here:
                                val uniqueValuePairs: List<Pair<String, String>> =
                                    valuePairs.distinctBy { (ctx, v) -> ctx to v }
                                val values = uniqueValuePairs.map { it.second }.distinct()
                                val mainValue = uniqueValuePairs.find { it.first == "default" }?.second
                                    ?: values.first()


                                // Doc comment from any doc-comment entry, fallback to default, fallback to first
                                val docEntry = allVals.firstOrNull { it.substringAfter(DELIMITER).isNotBlank() }
                                    ?: allVals.first()
                                val commentTxt = docEntry.substringAfter(DELIMITER)
                                val doc = DocParser.parse(commentTxt, mainValue).description

                                // Only true if ALL values parse as color
                                val isAllColor =
                                    values.isNotEmpty() && values.all { ColorParser.parseCssColor(it) != null }

                                entries += Entry(
                                    rawName,
                                    display,
                                    mainValue,
                                    uniqueValuePairs,   // ← using the deduped list
                                    doc,
                                    isAllColor
                                )
                            }

                        entries.sortBy { it.display }

                        // Create our enhanced lookup elements
                        for (e in entries) {
                            val short = e.doc.takeIf { it.isNotBlank() }
                                ?.let { it.take(40) + if (it.length > 40) "…" else "" }
                                ?: ""

                            // ICON LOGIC (with double swatch if 2 color values)
                            val colorIcons = e.allValues.mapNotNull { (_, v) ->
                                ColorParser.parseCssColor(v)?.let { ColorIcon(12, it, false) }
                            }.distinctBy { it.iconColor }
                            val icon: Icon = when {
                                e.isAllColor && colorIcons.size == 2 -> DoubleColorIcon(colorIcons[0], colorIcons[1])
                                e.isAllColor && colorIcons.isNotEmpty() -> colorIcons[0]
                                isSizeValue(e.mainValue) -> AllIcons.FileTypes.Css
                                else -> AllIcons.Nodes.Property
                            }

                            // TYPE TEXT LOGIC
                            val valueText = when {
                                // Both context and color, context-aware value listing (if enabled in settings)
                                e.isAllColor && e.allValues.size > 1 && settings.showContextValues -> {
                                    e.allValues.joinToString(" / ") { (ctx, v) ->
                                        when {
                                            "dark" in ctx.lowercase() -> "\uD83C\uDF19 $v" // 🌙
                                            else -> v
                                        }
                                    }
                                }
                                // Just color(s)
                                e.isAllColor -> e.mainValue

                                // Not all are color: normal value (+N) if >1 and context enabled
                                e.allValues.size > 1 && settings.showContextValues -> {
                                    "${e.mainValue} (+${e.allValues.size - 1})"
                                }

                                else -> e.mainValue
                            }

                            val elt = LookupElementBuilder
                                .create(e.rawName)
                                .withPresentableText(e.display)
                                .withLookupString(e.display)
                                .withIcon(icon)
                                .withTypeText(valueText, true)
                                .withTailText(if (short.isNotBlank()) " — $short" else "", true)
                                .withInsertHandler { ctx2, _ ->
                                    ctx2.document.replaceString(ctx2.startOffset, ctx2.tailOffset, e.rawName)
                                }
                            // Priority / order is important for the completion list, and is declared in plugin.xml

                            result.addElement(elt)
                        }

                        // Handle IDE completions based on settings
                        if (settings.allowIdeCompletions) {
                            // Only stop the completion chain for variables we've processed
                            if (processedVariables.isNotEmpty()) {
                                // Create a filtered result set that only stops completion for our processed variables
                                val filteredResult = result.withPrefixMatcher(object : PrefixMatcher(rawPref) {
                                    override fun prefixMatches(name: String): Boolean {
                                        // Only claim matches for variables we've processed
                                        return processedVariables.contains(name)
                                    }

                                    override fun cloneWithPrefix(prefix: String): PrefixMatcher {
                                        return this
                                    }
                                })

                                // Stop further processing only for our variables
                                filteredResult.stopHere()
                            }
                        } else {
                            // If IDE completions are disabled, stop the chain completely
                            result.stopHere()
                        }
                    } catch (ex: Exception) {
                        LOG.error("CSS var completion error", ex)
                    }
                }
            }
        )
    }

    /** Detect simple "size" constants (px, rem, em, %…) */
    private fun isSizeValue(raw: String): Boolean {
        return Regex("""^-?\d+(\.\d+)?(px|em|rem|ch|ex|vh|vw|vmin|vmax|%)$""", RegexOption.IGNORE_CASE)
            .matches(raw.trim())
    }
}

// Render two icons side by side (for two colors)
class DoubleColorIcon(private val icon1: Icon, private val icon2: Icon) : Icon {
    override fun getIconWidth() = icon1.iconWidth + icon2.iconWidth + 2
    override fun getIconHeight() = maxOf(icon1.iconHeight, icon2.iconHeight)
    override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
        icon1.paintIcon(c, g, x, y)
        icon2.paintIcon(c, g, x + icon1.iconWidth + 2, y)
    }
}