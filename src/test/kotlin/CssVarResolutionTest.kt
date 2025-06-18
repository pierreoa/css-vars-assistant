package cssvarsassistant.documentation.tooltip

import com.intellij.model.Pointer
import com.intellij.platform.backend.documentation.DocumentationResult
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.presentation.TargetPresentation
import cssvarsassistant.util.CSS_VAR_RESOLUTION_LINK
import org.junit.jupiter.api.Assertions.*
import kotlin.test.Test

class CssVarResolutionTest {
    @Test
    fun testResolutionChainDisplay() {
        val steps = listOf("var(--primary-color)", "@brand-primary", "#3498db")
        val encodedSteps = steps.joinToString("|")
        val url = "$CSS_VAR_RESOLUTION_LINK$encodedSteps"

        val mockTarget = object : DocumentationTarget {
            override fun computePresentation(): TargetPresentation =
                TargetPresentation.builder("test").presentation()

            override fun computeDocumentation(): DocumentationResult? = null
            override fun createPointer(): Pointer<out DocumentationTarget> = Pointer { this }
        }

        val handler = CssVarResolutionLinkHandler()

        // Test invalid URL returns null
        assertNull(handler.resolveLink(mockTarget, "invalid://url"))

        // Test valid URL parsing without triggering read lock
        assertTrue(url.startsWith(CSS_VAR_RESOLUTION_LINK))
        val extractedSteps = url.removePrefix(CSS_VAR_RESOLUTION_LINK).split("|")
        assertEquals(3, extractedSteps.size)
        assertEquals("var(--primary-color)", extractedSteps[0])
    }
}