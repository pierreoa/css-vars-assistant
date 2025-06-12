package cssvarsassistant.documentation

import cssvarsassistant.util.RankUtil.rank
import kotlin.test.Test
import kotlin.test.assertEquals

class RankingTest {
    @Test
    fun testRankingOrder() {
        val testCases = listOf(
            // Expected order with (input, expectedRank, expectedSecondary)
            "default" to Triple(0, null, "default"),
            "prefers-color-scheme: light" to Triple(0, null, "prefers-color-scheme: light"),
            "prefers-color-scheme: dark" to Triple(1, null, "prefers-color-scheme: dark"),

            // Min-width (larger first)
            "min-width: 1200px" to Triple(2, -1200, "min-width: 1200px"),
            "min-width: 768px" to Triple(2, -768, "min-width: 768px"),

            // Max-width (larger first)
            "max-width: 1024px" to Triple(3, -1024, "max-width: 1024px"),
            "max-width: 768px" to Triple(3, -768, "max-width: 768px"),
            "max-width: 350px" to Triple(3, -350, "max-width: 350px"),

            // Height queries
            "min-height: 600px" to Triple(4, -600, "min-height: 600px"),
            "max-height: 400px" to Triple(5, -400, "max-height: 400px"),

            // Preference states
            "prefers-reduced-motion: reduce" to Triple(6, 0, "prefers-reduced-motion: reduce"),
            "prefers-contrast: high" to Triple(6, 1, "prefers-contrast: high"),
            "orientation: portrait" to Triple(6, 2, "orientation: portrait"),
            "orientation: landscape" to Triple(6, 3, "orientation: landscape"),
            "hover: none" to Triple(6, 4, "hover: none"),

            // Interaction states
            "hover: hover" to Triple(7, 0, "hover: hover"),
            ":focus" to Triple(7, 1, ":focus"),
            ":active" to Triple(7, 2, ":active"),

            // Media types
            "print" to Triple(8, 0, "print"),
            "screen" to Triple(8, 1, "screen"),

            // Unknown
            "custom-query" to Triple(9, null, "custom-query")
        )

        testCases.forEach { (input, expected) ->
            val result = rank(input)
            assertEquals(expected, result, "Failed for input: $input")
        }
    }

    @Test
    fun testUnitConversions() {
        val testCases = listOf(
            "max-width: 48rem" to Triple(3, -768, "max-width: 48rem"), // 48 * 16 = 768
            "min-width: 2em" to Triple(2, -32, "min-width: 2em"), // 2 * 16 = 32
            "max-height: 50vh" to Triple(5, -384, "max-height: 50vh"), // 50 * 7.68 = 384
            "min-width: 800.5px" to Triple(2, -800, "min-width: 800.5px"), // Decimal handling
            "max-width: 100%" to Triple(3, -1000, "max-width: 100%") // 100 * 10 = 1000
        )

        testCases.forEach { (input, expected) ->
            val result = rank(input)
            assertEquals(expected, result, "Failed unit conversion for: $input")
        }
    }

    @Test
    fun testSortingBehavior() {
        val contexts = listOf(
            "default",
            "prefers-color-scheme: dark",
            "min-width: 1200px",
            "min-width: 768px",
            "max-width: 768px",
            "max-width: 350px",
            "hover: hover",
            "custom-unknown"
        )

        val sorted = contexts.sortedWith { a, b ->
            val rankA = rank(a)
            val rankB = rank(b)
            compareValuesBy(rankA, rankB,
                { it.first },
                { it.second ?: Int.MAX_VALUE },
                { it.third }
            )
        }

        val expected = listOf(
            "default",
            "prefers-color-scheme: dark",
            "min-width: 1200px", // -1200 comes before -768
            "min-width: 768px",
            "max-width: 768px", // -768 comes before -350
            "max-width: 350px",
            "hover: hover",
            "custom-unknown"
        )

        assertEquals(expected, sorted)
    }

    @Test
    fun testEdgeCases() {
        assertEquals(Triple(0, null, ""), rank(""))
        assertEquals(Triple(9, null, "malformed"), rank("malformed"))
        assertEquals(Triple(9, null, "max-width: invalid"), rank("max-width: invalid"))
        assertEquals(Triple(3, -0, "max-width: 0px"), rank("max-width: 0px"))
    }
}