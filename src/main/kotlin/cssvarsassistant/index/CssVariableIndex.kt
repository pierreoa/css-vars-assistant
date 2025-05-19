package cssvarsassistant.index

import com.intellij.util.indexing.*
import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.EnumeratorStringDescriptor
import com.intellij.util.io.IOUtil
import com.intellij.util.io.KeyDescriptor
import java.io.DataInput
import java.io.DataOutput

const val DELIMITER = "\u001F"
private const val ENTRY_SEP = "|||"

class CssVariableIndex : FileBasedIndexExtension<String, String>() {
    companion object {
        val NAME = ID.create<String, String>("cssvarsassistant.index")
    }

    override fun getName(): ID<String, String> = NAME
    override fun getVersion(): Int = 3

    override fun getInputFilter(): FileBasedIndex.InputFilter =
        FileBasedIndex.InputFilter { virtualFile ->
            // Accept only files that LOOK like style‑sheets
            when (virtualFile.extension?.lowercase()) {
                "css", "scss", "sass", "less" -> true                    // example extra types
                else -> false                                       // ignore *.txt, *.md, …
            }
        }

    override fun dependsOnFileContent(): Boolean = true

    override fun getIndexer(): DataIndexer<String, String, FileContent> = DataIndexer { inputData ->
        val map = mutableMapOf<String, String>()

        val text = inputData.contentAsText
        val lines = text.lines()
        var currentContext = "default"
        val contextStack = ArrayDeque<String>()

        var lastComment: String? = null
        var inBlockComment = false
        val blockComment = StringBuilder()

        for (rawLine in lines) {
            val line = rawLine.trim()

            // --- Media Query Context Handling ---
            if (line.startsWith("@media")) {
                val m = Regex("""@media\s*\(([^)]+)\)""").find(line)
                val mediaLabel = m?.groupValues?.get(1)?.trim() ?: "media"
                contextStack.addLast(mediaLabel)
                currentContext = contextStack.last()
                continue
            }
            if (line == "}") {
                if (contextStack.isNotEmpty()) {
                    contextStack.removeLast()
                    currentContext = contextStack.lastOrNull() ?: "default"
                }
                continue
            }

            // --- Comment Extraction ---
            // Start of block comment
            if (!inBlockComment && (line.startsWith("/*") || line.startsWith("/**"))) {
                inBlockComment = true
                blockComment.clear()
                // handle one-liner
                if (line.contains("*/")) {
                    blockComment.append(
                        line
                            .removePrefix("/**").removePrefix("/*")
                            .removeSuffix("*/").trim()
                    )
                    lastComment = blockComment.toString().trim()
                    inBlockComment = false
                    continue
                } else {
                    blockComment.append(
                        line
                            .removePrefix("/**").removePrefix("/*").trim()
                    )
                    continue
                }
            }
            if (inBlockComment) {
                if (line.contains("*/")) {
                    blockComment.append("\n" + line.removeSuffix("*/"))
                    lastComment = blockComment.toString().trim()
                    inBlockComment = false
                } else {
                    blockComment.append("\n" + line)
                }
                continue
            }

            // --- Variable Extraction ---
            val varDecl = Regex("""(--[A-Za-z0-9\-_]+)\s*:\s*([^;]+);""").find(line)
            if (varDecl != null) {
                val varName = varDecl.groupValues[1]
                val value = varDecl.groupValues[2].trim()
                val comment = lastComment ?: ""
                val entry = "$currentContext$DELIMITER$value$DELIMITER$comment"
                val prev = map[varName]
                map[varName] = if (prev == null) entry else prev + ENTRY_SEP + entry
                lastComment = null // clear comment after associating
            }
        }
        map
    }

    override fun getKeyDescriptor(): KeyDescriptor<String> = EnumeratorStringDescriptor.INSTANCE

    override fun getValueExternalizer(): DataExternalizer<String> =
        object : DataExternalizer<String> {
            override fun save(out: DataOutput, value: String) = IOUtil.writeUTF(out, value)
            override fun read(`in`: DataInput): String = IOUtil.readUTF(`in`)
        }
}
