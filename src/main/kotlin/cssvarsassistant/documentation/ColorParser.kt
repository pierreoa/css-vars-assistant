package cssvarsassistant.documentation

import java.awt.Color
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Parses CSS color strings into AWT Color,
 * and converts Colors back to hex strings.
 */
object ColorParser {
    // ---- Regexes for various CSS syntaxes ----
    private val hexRe     = Regex("^#([0-9a-fA-F]{3,8})\$")
    private val rgbRe     = Regex("^rgba?\\(([^)]*)\\)\$")
    private val hslRe     = Regex("^hsla?\\(([^)]*)\\)\$")
    private val bareHslRe = Regex("^([\\d.]+)\\s+([\\d.]+%)\\s+([\\d.]+%)\$")
    private val hwbRe     = Regex("^hwb\\(([^)]*)\\)\$")

    /**
     * Try to parse **any** CSS color string into a java.awt.Color.
     * Supports:
     *  • #RGB, #RRGGBB, #AARRGGBB
     *  • rgb(…)/rgba(…) with comma or space separated, percentages or integers
     *  • hsl(…)/hsla(…) with comma or slash separators
     *  • bare HSL triplets (e.g. `0 50% 50%`)
     *  • hwb(…)
     *  • TODO: lab()/lch()/oklab()/oklch() if you ever need them
     */
    fun parseCssColor(input: String): Color? {
        val s = input.trim()

        // 1) Hex (#RGB, #RRGGBB or #AARRGGBB)
        hexRe.matchEntire(s)?.groupValues?.get(1)?.let { rawHex ->
            var hex = rawHex
            // expand #RGB → RRGGBB
            if (hex.length == 3) {
                hex = hex.map { "$it$it" }.joinToString("")
            }
            return try {
                // if length==8, it's AARRGGBB
                Color(hex.toLong(16).toInt(), hex.length > 6)
            } catch (_: NumberFormatException) {
                null
            }
        }

        // 2) rgb()/rgba()
        rgbRe.matchEntire(s)?.groupValues
            ?.get(1)
            ?.let { body ->
                val parts = body
                    .split(',', ' ')
                    .filter { it.isNotBlank() }
                if (parts.size >= 3) {
                    fun parseChannel(v: String): Int =
                        if (v.endsWith("%")) {
                            // percent → 0–255
                            (v.dropLast(1).toFloatOrNull()?.times(2.55f)
                                ?.roundToInt())?.coerceIn(0, 255) ?: 0
                        } else {
                            v.toIntOrNull()?.coerceIn(0, 255) ?: 0
                        }
                    val r = parseChannel(parts[0])
                    val g = parseChannel(parts[1])
                    val b = parseChannel(parts[2])
                    val a = parts.getOrNull(3)
                        ?.toFloatOrNull()
                        ?.times(255f)
                        ?.roundToInt()
                        ?.coerceIn(0, 255)
                        ?: 255
                    return Color(r, g, b, a)
                }
            }

        // 3) hsl()/hsla()
        hslRe.matchEntire(s)?.groupValues
            ?.get(1)
            ?.let { body ->
                // allow comma or slash or space separated
                val parts = body
                    .split(',', '/', ' ')
                    .filter { it.isNotBlank() }
                if (parts.size >= 3) {
                    val h = parts[0].toFloatOrNull()?.rem(360f) ?: 0f
                    val sPct = parts[1].removeSuffix("%").toFloatOrNull()?.div(100f) ?: 0f
                    val lPct = parts[2].removeSuffix("%").toFloatOrNull()?.div(100f) ?: 0f
                    val alpha = parts.getOrNull(3)?.toFloatOrNull()?.coerceIn(0f, 1f) ?: 1f
                    return hslToRgb(h, sPct, lPct, alpha)
                }
            }

        // 4) bare HSL triplet (e.g. `0 50% 50%`)
        bareHslRe.matchEntire(s)?.destructured?.let { (h, sPct, lPct) ->
            return hslToRgb(
                h.toFloatOrNull()?.rem(360f) ?: 0f,
                sPct.removeSuffix("%").toFloatOrNull()?.div(100f) ?: 0f,
                lPct.removeSuffix("%").toFloatOrNull()?.div(100f) ?: 0f,
                1f
            )
        }

        // 5) HWB
        hwbRe.matchEntire(s)?.groupValues
            ?.get(1)
            ?.let { body ->
                val parts = body
                    .split(',', '/', ' ')
                    .filter { it.isNotBlank() }
                if (parts.size >= 3) {
                    val h = parts[0].toFloatOrNull()?.rem(360f) ?: 0f
                    val w = parts[1].removeSuffix("%").toFloatOrNull()?.div(100f) ?: 0f
                    val b = parts[2].removeSuffix("%").toFloatOrNull()?.div(100f) ?: 0f
                    val alpha = parts.getOrNull(3)?.removeSuffix("%")?.toFloatOrNull()?.coerceIn(0f, 1f) ?: 1f

                    // convert via HSL at 50% light
                    val c = (1f - w - b).coerceAtLeast(0f)
                    val base = hslToRgb(h, 1f, 0.5f, 1f)
                    val r = (w + c * (base.red   / 255f)).coerceIn(0f, 1f)
                    val g = (w + c * (base.green / 255f)).coerceIn(0f, 1f)
                    val bl = (w + c * (base.blue  / 255f)).coerceIn(0f, 1f)
                    return Color(
                        (r * 255).roundToInt(),
                        (g * 255).roundToInt(),
                        (bl* 255).roundToInt(),
                        (alpha * 255).roundToInt()
                    )
                }
            }

        // 6) (Future) lab(), lch(), oklab(), oklch() → add here if you need them

        return null
    }

    /**
     * Take any parsed [Color] and turn it into an uppercase "#RRGGBB" string.
     * Alpha is dropped (for your swatch previews you can assume fully opaque).
     */
    fun colorToHex(color: Color): String {
        return String.format("#%02X%02X%02X", color.red, color.green, color.blue)
    }

    /**
     * Combines parsing + hex conversion in one call.
     * Returns e.g. "#1A90FF" or null if it wasn't a valid color.
     */
    fun toHexString(input: String): String? {
        return parseCssColor(input)?.let(::colorToHex)
    }

    // ----------- Internal HSL → RGB conversion -------------
    private fun hslToRgb(h: Float, s: Float, l: Float, alpha: Float): Color {
        val c = (1f - abs(2 * l - 1f)) * s
        val x = c * (1f - abs((h / 60f) % 2 - 1f))
        val m = l - c / 2f
        val (r1, g1, b1) = when {
            h < 60f   -> Triple(c, x, 0f)
            h < 120f  -> Triple(x, c, 0f)
            h < 180f  -> Triple(0f, c, x)
            h < 240f  -> Triple(0f, x, c)
            h < 300f  -> Triple(x, 0f, c)
            else      -> Triple(c, 0f, x)
        }
        return Color(
            ((r1 + m) * 255).roundToInt(),
            ((g1 + m) * 255).roundToInt(),
            ((b1 + m) * 255).roundToInt(),
            (alpha * 255).roundToInt()
        )
    }
}


