package cssvarsassistant.documentation.v2

import com.intellij.model.Pointer
import com.intellij.openapi.diagnostic.Logger
import com.intellij.platform.backend.documentation.DocumentationResult
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.presentation.TargetPresentation
import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPointerManager

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
        return html?.let { DocumentationResult.documentation(it) }
    }

    override fun computeDocumentationHint(): String? =
        CssVariableDocumentationService.generateHint(element, varName)

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