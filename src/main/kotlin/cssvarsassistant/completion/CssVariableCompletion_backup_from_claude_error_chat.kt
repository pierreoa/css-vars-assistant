/*
package cssvarsassistant.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.css.CssFunction
import com.intellij.psi.search.FilenameIndex
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
    private val LESS_VAR_PATTERN = Regex("""^@([\w-]+)$""")

    // Cache for preprocessor variables
    private val preprocessorVarCache = mutableMapOf<Pair<Project, String>, String?>()

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

                        val rawPref = result.prefixMatcher.prefix
                        val simple = rawPref.removePrefix("--")
                        val project = pos.project
                        val settings = CssVarsAssistantSettings.getInstance()

                        // Choose scope based on settings
                        val scope = if (settings.useGlobalSearchScope) {
                            GlobalSearchScope.allScope(project)
                        } else {
                            GlobalSearchScope.projectScope(project)
                        }

                        val processedVariables = mutableSetOf<String>()

                        fun resolveVarValue(raw: String, visited: Set<String> = emptySet(), depth: Int = 0): String {
                            if (depth > 5) return raw

                            // Handle var(--xyz) references
                            val varRef = Regex("""var\(\s*(--[\w-]+)\s*\)""").find(raw)
                            if (varRef != null) {
                                val ref = varRef.groupValues[1]
                                if (ref in visited) return raw

                                val refEntries = FileBasedIndex.getInstance()
                                    .getValues(CssVariableIndex.NAME, ref, scope)
                                    .flatMap { it.split(ENTRY_SEP) }
                                    .distinct()
                                    .filter { it.isNotBlank() }

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
                                    return resolveVarValue(refDefault, visited + ref, depth + 1)
                                else
                                    return raw
                            }

                            // Handle preprocessor variable references
                            val lessVarMatch = LESS_VAR_PATTERN.find(raw.trim())
                            if (lessVarMatch != null) {
                                val varName = lessVarMatch.groupValues[1]
                                val cacheKey = Pair(project, varName)
                                preprocessorVarCache[cacheKey]?.let { return it }

                                val resolvedValue = findPreprocessorVariableValue(project, varName, scope)
                                preprocessorVarCache[cacheKey] = resolvedValue
                                return resolvedValue ?: raw
                            }

                            return raw
                        }

                        data class Entry(
                            val rawName: String,
                            val display: String,
                            val mainValue: String,
                            val allValues: List<Pair<String, String>>,
                            val doc: String,
                            val isAllColor: Boolean
                        )

                        val entries = mutableListOf<Entry>()
                        FileBasedIndex.getInstance()
                            .getAllKeys(CssVariableIndex.NAME, project)
                            .forEach { rawName ->
                                val display = rawName.removePrefix("--")
                                if (!display.startsWith(simple, ignoreCase = true)) return@forEach

                                processedVariables.add(rawName)

                                val allVals = FileBasedIndex.getInstance()
                                    .getValues(CssVariableIndex.NAME, rawName, scope)
                                    .flatMap { it.split(ENTRY_SEP) }
                                    .distinct()
                                    .filter { it.isNotBlank() }

                                if (allVals.isEmpty()) return@forEach

                                val valuePairs = allVals.mapNotNull {
                                    val parts = it.split(DELIMITER, limit = 3)
                                    if (parts.size >= 2) {
                                        val ctx = parts[0]
                                        val rawVal = parts[1]
                                        val resolved = resolveVarValue(rawVal)
                                        ctx to resolved
                                    } else null
                                }

                                val uniqueValuePairs = valuePairs.distinctBy { (ctx, v) -> ctx to v }
                                val values = uniqueValuePairs.map { it.second }.distinct()
                                val mainValue = uniqueValuePairs.find { it.first == "default" }?.second
                                    ?: values.first()

                                val docEntry = allVals.firstOrNull { it.substringAfter(DELIMITER).isNotBlank() }
                                    ?: allVals.first()
                                val commentTxt = docEntry.substringAfter(DELIMITER)
                                val doc = DocParser.parse(commentTxt, mainValue).description

                                val isAllColor =
                                    values.isNotEmpty() && values.all { ColorParser.parseCssColor(it) != null }

                                entries += Entry(
                                    rawName,
                                    display,
                                    mainValue,
                                    uniqueValuePairs,
                                    doc,
                                    isAllColor
                                )
                            }

                        entries.sortBy { it.display }

                        for (e in entries) {
                            val short = e.doc.takeIf { it.isNotBlank() }
                                ?.let { it.take(40) + if (it.length > 40) "…" else "" }
                                ?: ""

                            val colorIcons = e.allValues.mapNotNull { (_, v) ->
                                ColorParser.parseCssColor(v)?.let { ColorIcon(12, it, false) }
                            }.distinctBy { it.iconColor }

                            val icon: Icon = when {
                                e.isAllColor && colorIcons.size == 2 -> DoubleColorIcon(colorIcons[0], colorIcons[1])
                                e.isAllColor && colorIcons.isNotEmpty() -> colorIcons[0]
                                isSizeValue(e.mainValue) -> AllIcons.FileTypes.Css
                                else -> AllIcons.Nodes.Property
                            }

                            val valueText = when {
                                e.isAllColor && e.allValues.size > 1 && settings.showContextValues -> {
                                    e.allValues.joinToString(" / ") { (ctx, v) ->
                                        when {
                                            "dark" in ctx.lowercase() -> "\uD83C\uDF19 $v"
                                            else -> v
                                        }
                                    }
                                }

                                e.isAllColor -> e.mainValue
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

                            result.addElement(elt)
                        }

                        // Handle IDE completions based on settings
                        if (settings.allowIdeCompletions) {
                            if (processedVariables.isNotEmpty()) {
                                val filteredResult = result.withPrefixMatcher(object : PrefixMatcher(rawPref) {
                                    override fun prefixMatches(name: String): Boolean {
                                        return processedVariables.contains(name)
                                    }

                                    override fun cloneWithPrefix(prefix: String): PrefixMatcher {
                                        return this
                                    }
                                })
                                filteredResult.stopHere()
                            }
                        } else {
                            result.stopHere()
                        }
                    } catch (ex: Exception) {
                        LOG.error("CSS var completion error", ex)
                    }
                }
            }
        )
    }

    private fun findPreprocessorVariableValue(
        project: Project,
        varName: String,
        scope: GlobalSearchScope
    ): String? {
        try {
            val potentialFiles = mutableListOf<com.intellij.openapi.vfs.VirtualFile>()

            for (commonName in listOf("variables", "vars", "theme", "colors", "spacing", "tokens")) {
                for (ext in listOf(".less", ".scss", ".sass", ".css")) {
                    FilenameIndex.getAllFilesByExt(project, "$commonName$ext", scope)
                        .forEach { potentialFiles.add(it) }
                }
            }

            for (file in potentialFiles) {
                try {
                    val content = String(file.contentsToByteArray())

                    // Look for different preprocessor variable patterns
                    listOf(
                        Regex("""@${Regex.escape(varName)}:\s*([^;]+);"""), // LESS
                        Regex("""\$${Regex.escape(varName)}:\s*([^;]+);"""), // SCSS
                        Regex("""--${Regex.escape(varName)}:\s*([^;]+);""") // CSS
                    ).forEach { pattern ->
                        pattern.find(content)?.let {
                            return it.groupValues[1].trim()
                        }
                    }
                } catch (e: Exception) {
                    LOG.debug("Error reading file ${file.path}", e)
                }
            }

            return null
        } catch (e: Exception) {
            LOG.error("Error finding preprocessor variable value", e)
            throw (e)
        }
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
}*/
