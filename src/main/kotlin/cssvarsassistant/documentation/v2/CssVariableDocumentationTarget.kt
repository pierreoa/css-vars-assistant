// CssVariableDocumentationTarget.kt
package cssvarsassistant.documentation.v2

import com.intellij.model.Pointer
import com.intellij.openapi.diagnostic.Logger
import com.intellij.platform.backend.documentation.DocumentationResult
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.presentation.TargetPresentation
import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPointerManager
import cssvarsassistant.documentation.CssVariableDocumentationService

class CssVariableDocumentationTarget(
    private val element: PsiElement,
    private val varName: String
) : DocumentationTarget {

    private val LOG = Logger.getInstance(CssVariableDocumentationTarget::class.java)

    override fun computePresentation(): TargetPresentation =
        TargetPresentation.builder(varName)
            .icon(com.intellij.icons.AllIcons.FileTypes.Css)
            .presentation()

    override fun computeDocumentation(): DocumentationResult? {
        LOG.debug("V2 API called for $varName")
        val html = CssVariableDocumentationService.generateDocumentation(element, varName)
            ?: return null

        // Create custom hyperlink listener for this documentation
        val linkListener = CssVarPopupLinkListener(element.project, varName)

        return DocumentationResult.documentation(html)
            .hyperlinks(linkListener)
    }

    override fun computeDocumentationHint(): String? {
        LOG.info("computeDocumentationHint() called for $varName")
        return CssVariableDocumentationService.generateHint(element, varName)
    }

    override fun createPointer(): Pointer<out DocumentationTarget> {
        val elementPointer = SmartPointerManager.getInstance(element.project)
            .createSmartPsiElementPointer(element)

        return Pointer {
            elementPointer.element?.let {
                CssVariableDocumentationTarget(it, varName)
            }
        }
    }
}