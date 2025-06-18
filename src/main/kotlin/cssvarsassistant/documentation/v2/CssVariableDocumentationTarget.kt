package cssvarsassistant.documentation.v2

import com.intellij.model.Pointer
import com.intellij.openapi.diagnostic.Logger
import com.intellij.platform.backend.documentation.DocumentationResult
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.presentation.TargetPresentation
import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPointerManager
import cssvarsassistant.documentation.CssVariableDocumentationService

/**
 * Documentation Target for CSS Variables using the new Documentation Target API (2023.1+).
 *
 * This API is recommended by JetBrains for all new documentation providers.
 * See: https://plugins.jetbrains.com/docs/intellij/documentation.html
 *
 * Note: Some parts are marked @ApiStatus.Experimental but this is the official
 * recommended approach. We suppress these warnings in build.gradle.kts.
 */
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
        val html = CssVariableDocumentationService.generateDocumentation(element, varName) ?: return null


        return DocumentationResult.documentation(html)

    }

    override fun computeDocumentationHint(): String? {
        LOG.info("computeDocumentationHint() called for $varName")
        val result = CssVariableDocumentationService.generateHint(element, varName)
        LOG.info("generateHint() returned: $result")
        return result
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