// src/main/kotlin/cssvarsassistant/model/DocumentationSchema.kt
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
 * NOTE: @value tags are no longer supported — we always use the actual CSS value.
 */
object DocParser {

    /**
     * Parse a documentation comment into structured data
     * Supports @name, @description/@desc/@doc, and @example tags (case‑insensitive).
     * The 'value' field is always taken from the real CSS value (defaultValue).
     */
    fun parse(commentText: String, defaultValue: String = ""): CssVarDoc {
        val lines = commentText.lines().map { it.trim() }

        // @name
        val name = extractMultilineTag(lines, arrayOf("@name")).firstOrNull().orEmpty()

        // @description / @desc / @doc
        val description = extractMultilineTag(lines, arrayOf("@description", "@desc", "@doc"))
            .joinToString(" ")
            .takeIf { it.isNotBlank() }
            ?: extractMainDescription(lines)
            ?: ""

        // always use the actual CSS value, ignore any @value tags
        val value = defaultValue

        // @example
        val examples = extractMultilineTag(lines, arrayOf("@example"))

        return CssVarDoc(name, description, value, examples)
    }

    private fun extractMainDescription(lines: List<String>): String? =
        lines.firstOrNull { it.isNotBlank() && !it.startsWith("@", ignoreCase = true) }

    private fun extractMultilineTag(lines: List<String>, tags: Array<String>): List<String> {
        val result = mutableListOf<String>()
        var inTag = false

        // build a case-insensitive regex for any of the tags
        val tagPattern = Regex("(?i)^(${tags.joinToString("|") { Regex.escape(it) }})\\b")

        for (line in lines) {
            val trimmed = line.trim()
            val tagMatch = tagPattern.find(trimmed)
            if (tagMatch != null) {
                inTag = true
                val rest = trimmed.substring(tagMatch.range.last + 1).trim()
                if (rest.isNotBlank()) result.add(rest)
                continue
            }
            if (inTag) {
                if (trimmed.startsWith("@")) {
                    inTag = false
                } else if (trimmed.isNotBlank()) {
                    result.add(trimmed)
                }
            }
        }

        return result
    }
}
