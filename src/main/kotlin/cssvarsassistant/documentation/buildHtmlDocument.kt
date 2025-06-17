package cssvarsassistant.documentation

import com.intellij.lang.documentation.DocumentationMarkup
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.util.text.StringUtil
import cssvarsassistant.model.CssVarDoc
import cssvarsassistant.util.ValueUtil
import kotlin.math.pow
import kotlin.math.roundToInt

fun buildHtmlDocument(
    varName: String,
    doc: CssVarDoc,
    sorted: List<Triple<String, ResolutionInfo, String>>,
    showPixelCol: Boolean,
    winnerIndex: Int = -1  // Default for backward compatibility
): String {

    /* ── dynamic column decisions ─────────────────────────────────────────── */
    val showWcagCol = sorted.any { (_, r, _) -> ColorParser.parseCssColor(r.resolved) != null }
    val showHexCol = sorted.any { (_, r, _) ->
        ColorParser.parseCssColor(r.resolved)?.let { !r.resolved.trim().startsWith("#") } ?: false
    }

    val winnerFirstSorted = if (winnerIndex >= 0) {
        val winner = sorted[winnerIndex]
        val others = sorted.filterIndexed { index, _ -> index != winnerIndex }
        listOf(winner) + others
    } else {
        sorted
    }

    /* ── inline-CSS helpers (survive IntelliJ trimming) ────────────────────── */
    val headerWrapperStyle =
        "style='color:#F2F2F2;padding:2px 4px;font-weight:bold;border-bottom:1px solid #BABABA;'"
    val rowStyle = "style='white-space:nowrap;padding:2px 4px;color:#BABABA;'"
    val rowResolvedStyle = "style='white-space:nowrap;color:#F2F2F2;font-size:9px!important;'"

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

    val hasResolvedValues = winnerFirstSorted.any { (_, resInfo, _) ->
        resInfo.steps.isNotEmpty() && resInfo.original != resInfo.resolved
    }

    if (hasResolvedValues) {
        sb.append("<p><small><b>Legend:</b> ↗ indicates a value resolved through variable references or imports</small></p>")
    }

    /* ── table header ─────────────────────────────────────────────────────── */
    sb.append(
        """
        <p><b>Values:</b></p>
        <table>
          <tr $headerWrapperStyle>
            <th><nobr>Context</nobr></th>
            <th>&nbsp;</th>
            <th><nobr>Value</nobr></th>
            <th><nobr>Type</nobr></th>
            <th><nobr>Source</nobr></th>""".trimIndent()
    )
    if (showPixelCol) sb.append("<th><nobr>px&nbsp;Eq.</nobr></th>")
    if (showHexCol) sb.append("<th><nobr>Hex</nobr></th>")
    if (showWcagCol) sb.append("<th><nobr>WCAG</nobr></th>")
    sb.append("</tr>")

    /* ── table rows ───────────────────────────────────────────────────────── */
    winnerFirstSorted.forEachIndexed { displayIndex, (ctx, resInfo, _) ->
        ProgressManager.checkCanceled()

        val isWinner = displayIndex == 0 && winnerIndex >= 0
        val isOverridden = !isWinner && ctx == "default" && displayIndex > 0

        val rowStyleExtra = if (isWinner) "font-weight:bold;" else ""
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
        sb.append("<tr style='$rowStyleExtra'>")
            .append("<td $rowStyle><nobr>${StringUtil.escapeXmlEntities(contextLabel(ctx, isColour))}</nobr></td>")
            .append("<td $rowStyle><nobr>${if (isColour) colorSwatchHtml(rawValue) else "&nbsp;"}</nobr></td>")
            .append("<td $rowStyle><nobr>")
            .append(StringUtil.escapeXmlEntities(rawValue).lowercase())

        // Mark overridden values
        if (isOverridden) {
            sb.append(" <span style='opacity:.6'><i>(overridden)</i></span>")
        }

        // Add resolution indicator
        if (resInfo.steps.isNotEmpty() && resInfo.original != resInfo.resolved)
            sb.append(
                """&nbsp;<small title="${StringUtil.escapeXmlEntities(resInfo.steps.joinToString(" → "))}" 
                            $rowResolvedStyle>↗</small>"""
            )
        sb.append("</nobr></td>")
            .append("<td $rowStyle><nobr>${StringUtil.escapeXmlEntities(typeStr).lowercase()}</nobr></td>")
            .append("<td $rowStyle><nobr>${StringUtil.escapeXmlEntities(sourceStr)}</nobr></td>")

        if (showPixelCol) sb.append("<td $rowStyle><nobr>$pixelEq</nobr></td>")
        if (showHexCol) sb.append("<td $rowStyle><nobr>$hexValue</nobr></td>")
        if (showWcagCol) sb.append("<td $rowStyle><nobr>$contrast</nobr></td>")

        sb.append("</tr>")
    }
    sb.append("</table>")

    /* ── description / examples ──────────────────────────────────────────── */
    if (doc.description.isNotBlank()) {
        sb.append("<p><b>Description:</b><br/>")
            .append(StringUtil.escapeXmlEntities(doc.description))
            .append("</p>")
    }
    if (doc.examples.isNotEmpty()) {
        sb.append("<p><b>Examples:</b></p><pre>")
        doc.examples.forEach { sb.append(StringUtil.escapeXmlEntities(it)).append('\n') }
        sb.append("</pre>")
    }

    /* ── WebAIM helper link for first colour found ───────────────────────── */
    sorted.mapNotNull { ColorParser.parseCssColor(it.second.resolved) }
        .firstOrNull()?.let { c ->
            sb.append(
                """<p style='margin-top:10px'>
                       <a target="_blank"
                          href="https://webaim.org/resources/contrastchecker/?fcolor=${
                    c.toHex().removePrefix("#")
                }&bcolor=000000">
                          Check contrast on WebAIM Contrast Checker
                       </a></p>"""
            )
        }

    sb.append(DocumentationMarkup.CONTENT_END)
    return sb.toString()
}

/* ── tiny util helpers ─────────────────────────────────────────────────────── */
fun java.awt.Color.toHex(): String =
    "#%02x%02x%02x".format(red, green, blue)

fun contextLabel(ctx: String, isColor: Boolean): String {
    if (ctx == "default") return if (isColor) "Light mode" else "Default"

    if ("prefers-color-scheme" in ctx.lowercase()) {
        return when {
            "light" in ctx.lowercase() -> "Light mode"
            "dark" in ctx.lowercase() -> "Dark mode"
            else -> "Color scheme"
        }
    }

    if ("prefers-reduced-motion" in ctx.lowercase() && "reduce" in ctx.lowercase()) {
        return "Reduced motion"
    }

    if (Regex("\\bprint\\b", RegexOption.IGNORE_CASE).containsMatchIn(ctx)) {
        return "Print"
    }

    if (Regex("only\\s+screen", RegexOption.IGNORE_CASE).containsMatchIn(ctx)) {
        return "Only screen"
    }

    val maxWidthRegex = Regex("""max-width:\s*(\d+)(?:px)?\s*\)""")
    val minWidthRegex = Regex("""min-width:\s*(\d+)(?:px)?\s*\)""")

    val maxWidthMatch = maxWidthRegex.find(ctx)
    val minWidthMatch = minWidthRegex.find(ctx)

    return when {
        maxWidthMatch != null && minWidthMatch != null -> {
            val maxWidth = maxWidthMatch.groupValues[1]
            val minWidth = minWidthMatch.groupValues[1]
            "${minWidth}px-${maxWidth}px"
        }

        maxWidthMatch != null -> "≤${maxWidthMatch.groupValues[1]}px"
        minWidthMatch != null -> "≥${minWidthMatch.groupValues[1]}px"
        else -> {
            ctx.replace(Regex("""@media\s+"""), "")
                .replace("screen and ", "")
                .replace(Regex("""\s+"""), " ")
                .trim()
                .takeIf { it.isNotEmpty() } ?: "Media query"
        }
    }
}

fun colorSwatchHtml(css: String): String =
    ColorParser.parseCssColor(css)?.let { "<font color='${it.toHex()}'>&#9632;</font>" } ?: "&nbsp;"