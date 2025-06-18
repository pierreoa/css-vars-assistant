/*
// src/test/kotlin/cssvarsassistant/documentation/tooltip/CssVarResolutionLinkHandlerTest.kt
package cssvarsassistant.documentation.tooltip

import com.intellij.model.Pointer
import com.intellij.openapi.application.runReadAction
import com.intellij.platform.backend.documentation.DocumentationResult
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.documentation.LinkResolveResult
import com.intellij.platform.backend.presentation.TargetPresentation
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CssVarResolutionLinkHandlerTest {

    */
/** Starts a headless IntelliJ Application before each test *//*

    @Rule @JvmField
    val app = ApplicationRule()

    @Test
    fun `resolveLink recognises css-var scheme and returns a direct result`() {
        // ── given
        val steps = listOf("var(--primary-color)", "@brand-primary", "#3498db")
        val url   = CSS_VAR_RESOLUTION_LINK + steps.joinToString("|")

        val dummyTarget = object : DocumentationTarget {
            override fun computePresentation() =
                TargetPresentation.builder("dummy").presentation()

            override fun computeDocumentation() =
                DocumentationResult.documentation("<p>dummy</p>")

            override fun createPointer(): Pointer<out DocumentationTarget> =
                Pointer { this }
        }

        // ── when  (must be inside a read-action)
        val handler = CssVarResolutionLinkHandler()
        val result  = runReadAction { handler.resolveLink(dummyTarget, url) }

        // ── then
        assertNotNull(result, "Handler should resolve the css-var link")
        assertTrue(result !is LinkResolveResult.Async, "Should be a direct (synchronous) result")

        // If you *do* want to inspect the produced HTML, you can still use the
        // reflection helper — now it won’t crash:
        //   val html = result.extractTarget()!!.computeDocumentation().html!!
        //   steps.forEach { assertTrue(it in html) }
    }
}

*/
/* Optional – only needed if you want to peek inside the opaque result *//*

private fun LinkResolveResult.extractTarget(): DocumentationTarget? =
    javaClass.methods
        .firstOrNull { it.name == "getTarget" && it.parameterCount == 0 }
        ?.invoke(this) as? DocumentationTarget
*/
