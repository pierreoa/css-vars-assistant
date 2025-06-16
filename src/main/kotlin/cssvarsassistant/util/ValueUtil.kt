package cssvarsassistant.util

import cssvarsassistant.documentation.ColorParser

import java.awt.Color

object ValueUtil {
    enum class ValueType { SIZE, COLOR, NUMBER, OTHER }

    
    fun getValueType(value: String): ValueType {
        val cleaned = value.trim()
        return when {
            isSizeValue(cleaned) -> {
                ValueType.SIZE
            }

            ColorParser.parseCssColor(cleaned) != null -> {
                ValueType.COLOR
            }

            isNumericValue(cleaned) -> {
                ValueType.NUMBER
            }

            else -> {
                ValueType.OTHER
            }
        }
    }


    fun isSizeValue(value: String): Boolean {
        val cleaned = value.trim()
        val isSizeValue = Regex("""^\d+(\.\d+)?(px|rem|em|%|vh|vw|pt)$""", RegexOption.IGNORE_CASE).matches(cleaned)
        return isSizeValue
    }

    fun isNumericValue(value: String): Boolean {
        val isNumericVal = value.trim().toDoubleOrNull() != null
        return isNumericVal
    }

    fun convertToPixels(value: String): Double {
        val trimmed = value.trim()
        val number = Regex("""^(\d+(?:\.\d+)?)""").find(trimmed)?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0


        return when {
            trimmed.endsWith("rem") -> number * 16
            trimmed.endsWith("em") -> number * 16
            trimmed.endsWith("px") -> number
            trimmed.endsWith("pt") -> number * 1.33
            trimmed.endsWith("%") -> number
            trimmed.endsWith("vh") -> number * 10
            trimmed.endsWith("vw") -> number * 10
            else -> number
        }
    }

    fun compareSizes(a: String, b: String): Int {
        val aPixels = convertToPixels(a)
        val bPixels = convertToPixels(b)
        return aPixels.compareTo(bPixels)
    }

    fun compareNumbers(a: String, b: String): Int {
        val aNum = a.trim().toDoubleOrNull() ?: 0.0
        val bNum = b.trim().toDoubleOrNull() ?: 0.0
        return aNum.compareTo(bNum)
    }

    fun compareColors(a: String, b: String): Int {
        val colorA = ColorParser.parseCssColor(a)
        val colorB = ColorParser.parseCssColor(b)

        if (colorA == null || colorB == null) {
            return a.compareTo(b, true)
        }

        // Sort by HSB: Hue first, then Saturation, then Brightness
        val hsbA = Color.RGBtoHSB(colorA.red, colorA.green, colorA.blue, null)
        val hsbB = Color.RGBtoHSB(colorB.red, colorB.green, colorB.blue, null)

        // Compare hue (0-1)
        val hueCompare = hsbA[0].compareTo(hsbB[0])
        if (hueCompare != 0) return hueCompare

        // Compare saturation
        val satCompare = hsbA[1].compareTo(hsbB[1])
        if (satCompare != 0) return satCompare

        // Compare brightness
        return hsbA[2].compareTo(hsbB[2])
    }
}