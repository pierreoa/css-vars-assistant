// src/main/kotlin/cssvarsassistant/documentation/v2/CssVariablePsiDocumentationTargetProvider.kt
package cssvarsassistant.documentation.v2


import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.documentation.PsiDocumentationTargetProvider
import com.intellij.psi.PsiElement
import cssvarsassistant.documentation.extractCssVariableName

class CssVariablePsiDocumentationTargetProvider : PsiDocumentationTargetProvider {

    override fun documentationTarget(element: PsiElement, originalElement: PsiElement?): DocumentationTarget? {
        println("DEBUG: documentationTarget called for element: ${element.text}") // Add this

        if (!element.isValid) return null

        val varName = extractCssVariableName(element)
        println("DEBUG: extracted varName: $varName") // Add this

        return varName?.let {
            println("DEBUG: returning CssVariableDocumentationTarget for $it") // Add this
            CssVariableDocumentationTarget(element, it)
        }
    }

}

