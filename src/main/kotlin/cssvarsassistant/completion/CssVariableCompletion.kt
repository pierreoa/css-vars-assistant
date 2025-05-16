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
import cssvarsassistant.index.CssVariableIndex
import cssvarsassistant.index.DELIMITER
import cssvarsassistant.model.DocParser

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
                        // 1) Only inside var(...) function
                        val pos = params.position
                        val fn  = PsiTreeUtil.getParentOfType(pos, CssFunction::class.java) ?: return
                        if (fn.name != "var") return
                        val lpar = fn.lParenthesis?.textOffset ?: return
                        val rpar = fn.rParenthesis?.textOffset ?: return
                        val off  = params.offset
                        if (off <= lpar || off > rpar) return

                        // 2) Capture typed prefix
                        val rawPrefix  = result.prefixMatcher.prefix    // e.g. "--fs" or "fs"
                        val simplePref = rawPrefix.removePrefix("--")   // e.g. "fs"
                        val project    = pos.project
                        val scope      = GlobalSearchScope.projectScope(project)

                        // 3) Gather matches with numeric values
                        data class Entry(
                            val rawName: String,
                            val display: String,
                            val numeric: Double,
                            val value: String,
                            val doc: String
                        )

                        val entries = mutableListOf<Entry>()
                        FileBasedIndex.getInstance()
                            .getAllKeys(CssVariableIndex.NAME, project)
                            .forEach { rawName ->
                                val display = rawName.removePrefix("--")
                                if (!display.startsWith(simplePref, ignoreCase = true)) return@forEach

                                val vals = FileBasedIndex.getInstance()
                                    .getValues(CssVariableIndex.NAME, rawName, scope)
                                if (vals.isEmpty()) return@forEach

                                val rawEntry = vals.first()
                                val value    = rawEntry.substringBefore(DELIMITER)
                                val doc      = DocParser.parse(rawEntry.substringAfter(DELIMITER, ""), value).description

                                // parse leading number or NaN
                                val numeric = Regex("""^-?(\d+(\.\d+)?)""")
                                    .find(value)
                                    ?.groupValues
                                    ?.get(1)
                                    ?.toDoubleOrNull()
                                    ?: Double.NaN

                                entries += Entry(rawName, display, numeric, value, doc)
                            }

                        // 4) Sort: numeric first (largest → smallest), then NaN, then alpha
                        entries.sortWith(
                            compareBy<Entry> { it.numeric.isNaN() }           // false (numbers) before true (NaN)
                                .thenByDescending { it.numeric }                // bigger numbers first
                                .thenBy { it.display }                          // then by name
                        )

                        // 5) Build and add lookup elements
                        for (e in entries) {
                            val short = e.doc.takeIf { it.isNotBlank() }
                                ?.let { it.take(40) + if (it.length > 40) "…" else "" } ?: ""
                            val lookup = LookupElementBuilder.create(e.rawName)
                                .withPresentableText(e.display)
                                .withLookupString(e.display)
                                .withIcon(AllIcons.Nodes.Property)
                                .withTypeText(e.value, true)
                                .withTailText(if (short.isNotBlank()) " — $short" else "", true)
                                .withInsertHandler { ctx2, _ ->
                                    ctx2.document.replaceString(ctx2.startOffset, ctx2.tailOffset, e.rawName)
                                }
                            result.addElement(lookup)
                        }

                        // 6) Hide all other CSS var proposals
                        result.stopHere()
                    } catch (ex: Exception) {
                        LOG.error("CSS var completion error", ex)
                    }
                }
            }
        )
    }
}