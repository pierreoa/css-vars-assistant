package cssvarsassistant.model

/**
 * Structured CSS variable documentation
 */
data class CssVarDoc(
    val name: String = "",
    val description: String = "",
    val value: String = "",
    val examples: List<String> = emptyList()
)

/**
 * Parses CSS variable documentation from comment text.
 * Supports inline one‑liners as well as multi‑line blocks,
 * and is case‐insensitive to @Name/@name, @Description/@description, etc.
 */
object DocParser {

    /**
     * Parse a documentation comment into structured data
     *  • First tries inline tags (one‑liner comments).
     *  • Falls back to multi‑line extraction if no inline tags present.
     *  • Always uses the real CSS value (defaultValue).
     */
    fun parse(commentText: String, defaultValue: String = ""): CssVarDoc {
        // 1) Try inline (same‑line) @name and @description tags:
        val inlineName = INLINE_NAME
            .find(commentText)
            ?.groupValues
            ?.get(1)
            ?.trim()
            .orEmpty()

        val inlineDesc = INLINE_DESC
            .find(commentText)
            ?.groupValues
            ?.get(1)
            ?.trim()

        // 2) Split into logical lines for block parsing
        val lines = commentText
            .lines()
            .map { it.trim().removePrefix("*").trim() } // strip leading `*` in multi‑line
            .filter { it.isNotBlank() }

        // 3) Name: prefer inline, else first @name block
        val name = if (inlineName.isNotBlank()) {
            inlineName
        } else {
            extractMultilineTag(lines, arrayOf("@name")).firstOrNull().orEmpty()
        }

        // 4) Description: prefer inline, else block @description/desc/doc, else first non‑tag line
        val description = when {
            inlineDesc != null -> inlineDesc
            else -> {
                extractMultilineTag(lines, arrayOf("@description", "@desc", "@doc"))
                    .joinToString(" ")
                    .takeIf { it.isNotBlank() }
                    ?: extractMainDescription(lines)
                    ?: ""
            }
        }

        // 5) Examples (only multi‑line)
        val examples = extractMultilineTag(lines, arrayOf("@example"))

        return CssVarDoc(name, description, defaultValue, examples)
    }

    // Regexes for inline one‑liner extraction:
    private val INLINE_NAME = Regex("(?i)@name\\s+([^@]+?)(?=(?:@|$))")
    private val INLINE_DESC = Regex("(?i)@description\\s+([^@]+?)(?=(?:@|$))")

    // Finds the very first non‑tag line to use as a description fallback.
    private fun extractMainDescription(lines: List<String>): String? =
        lines.firstOrNull { !it.startsWith("@", ignoreCase = true) }

    /**
     * Pulls out all consecutive lines belonging to any of the given tags.
     * Stops when another “@” tag is encountered.
     */
    private fun extractMultilineTag(lines: List<String>, tags: Array<String>): List<String> {
        val result = mutableListOf<String>()
        var inTag = false
        // build a case‑insensitive “starts with” pattern for each tag
        val tagPattern = Regex("(?i)^(${tags.joinToString("|") { Regex.escape(it) }})\\b")

        for (line in lines) {
            val trimmed = line.trim()
            val tagMatch = tagPattern.find(trimmed)
            if (tagMatch != null) {
                // start of a new tag
                inTag = true
                val rest = trimmed.substring(tagMatch.range.last + 1).trim()
                if (rest.isNotBlank()) result.add(rest)
            } else if (inTag) {
                // continuation of the same tag
                if (trimmed.startsWith("@")) {
                    // a new tag starts → stop
                    inTag = false
                } else if (trimmed.isNotBlank()) {
                    result.add(trimmed)
                }
            }
        }
        return result
    }
}
