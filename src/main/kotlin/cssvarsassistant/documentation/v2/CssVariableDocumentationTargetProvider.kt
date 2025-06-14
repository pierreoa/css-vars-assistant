// src/main/kotlin/cssvarsassistant/documentation/v2/CssVariableDocumentationTargetProvider.kt
package cssvarsassistant.documentation.v2

import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.documentation.DocumentationTargetProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

class CssVariableDocumentationTargetProvider : DocumentationTargetProvider {

    override fun documentationTargets(file: PsiFile, offset: Int): List<DocumentationTarget> {
        val element = file.findElementAt(offset) ?: return emptyList()
        val varName = extractCssVariableName(element) ?: return emptyList()

        return listOf(CssVariableDocumentationTarget(element, varName))
    }

    private fun extractCssVariableName(element: PsiElement): String? =
        element.text.trim().takeIf { it.startsWith("--") }
            ?: element.parent?.text?.let {
                Regex("""var\(\s*(--[\w-]+)\s*\)""").find(it)?.groupValues?.get(1)
            }
}