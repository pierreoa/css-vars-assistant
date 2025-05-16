package cssvarsassistant.index

import com.intellij.lang.css.CSSLanguage
import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.indexing.*
import com.intellij.util.io.EnumeratorStringDescriptor
import com.intellij.util.io.KeyDescriptor
import com.intellij.openapi.diagnostic.Logger

const val DELIMITER = "\u001F" // Non-printable character unlikely to appear in CSS

class CssVariableIndex : FileBasedIndexExtension<String, String>() {

    private val LOG = Logger.getInstance(CssVariableIndex::class.java)

    companion object {
        val NAME: ID<String, String> = ID.create("cssvarsassistant.CssVariableIndex")
    }

    override fun getName(): ID<String, String> = NAME
    override fun getKeyDescriptor(): KeyDescriptor<String> = EnumeratorStringDescriptor.INSTANCE
    override fun getValueExternalizer() = EnumeratorStringDescriptor.INSTANCE
    override fun dependsOnFileContent() = true
    override fun getVersion() = 1

    override fun getInputFilter(): FileBasedIndex.InputFilter =
        DefaultFileTypeSpecificInputFilter(
            FileTypeRegistry.getInstance().getFileTypeByExtension("css"),
            FileTypeRegistry.getInstance().getFileTypeByExtension("scss"),
            FileTypeRegistry.getInstance().getFileTypeByExtension("sass"),
            FileTypeRegistry.getInstance().getFileTypeByExtension("less")

        )


    override fun getIndexer(): DataIndexer<String, String, FileContent> = DataIndexer { content ->
        val map = hashMapOf<String, String>()
        val file: PsiFile = content.psiFile

        if (file.language.isKindOf(CSSLanguage.INSTANCE)) {
            file.text.split('\n').forEachIndexed { i, line ->
                val trimmed = line.trim()
                if (trimmed.startsWith("--")) {
                    try {
                        val name = trimmed.substringBefore(':').trim()
                        val value = trimmed.substringAfter(':').substringBefore(';').trim()
                        val commentText = findDocCommentAbove(file, i)

                        // Store the entire comment as is, we'll parse it when needed
                        map[name] = "$value$DELIMITER$commentText"
                    } catch (e: Exception) {
                        LOG.warn("Error indexing CSS variable: $trimmed", e)
                    }
                }
            }
        }

        map
    }


    private fun findDocCommentAbove(file: PsiFile, lineNumber: Int): String {
        val offset = file.text.split('\n').take(lineNumber).sumOf { it.length + 1 }
        val element = file.findElementAt(offset)
        val comment = PsiTreeUtil.getPrevSiblingOfType(element, PsiComment::class.java)

        // Get the raw comment text without stripping prefixes for parsing
        return comment?.text?.removePrefix("/**")?.removeSuffix("*/")?.trim() ?: ""
    }
}