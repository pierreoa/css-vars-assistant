// src/main/kotlin/cssvarsassistant/documentation/v2/CssVariablePsiDocumentationTargetProvider.kt
package cssvarsassistant.documentation.v2

import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.documentation.PsiDocumentationTargetProvider
import com.intellij.psi.PsiElement

class CssVariablePsiDocumentationTargetProvider : PsiDocumentationTargetProvider {

    override fun documentationTarget(element: PsiElement, originalElement: PsiElement?): DocumentationTarget? {
        val varName = extractCssVariableName(element) ?: return null
        return CssVariableDocumentationTarget(element, varName)
    }

    private fun extractCssVariableName(element: PsiElement): String? =
        element.text.trim().takeIf { it.startsWith("--") }
            ?: element.parent?.text?.let {
                Regex("""var\(\s*(--[\w-]+)\s*\)""").find(it)?.groupValues?.get(1)
            }
}

