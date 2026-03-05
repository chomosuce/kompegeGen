import com.openhtmltopdf.pdfboxout.PdfRendererBuilder
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Entities
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.outputStream
import kotlin.io.path.writeText

class PdfgeneratorImpl(
    private val htmlOutputPath: Path = Paths.get("build", "generated", "tasks.html"),
    private val pdfOutputPath: Path = Paths.get("build", "generated", "tasks.pdf"),
    private val fontPath: Path? = null
) : PDFgenerator {
    private val bundledFontPath: Path? by lazy {
        extractBundledFont(BUNDLED_FONT_PATH, "pdf-font-")
    }
    private val mathFontPath: Path? by lazy {
        extractBundledFont(MATH_FONT_PATH, "pdf-math-font-")
    }

    override fun getPdf(tasks: List<TaskItem>) {
        val html = buildHtml(tasks)
        val resolvedFontPath = resolveFontPath()

        htmlOutputPath.parent?.createDirectories()
        htmlOutputPath.writeText(html)

        pdfOutputPath.parent?.createDirectories()
        pdfOutputPath.outputStream().use { outputStream ->
            val builder = PdfRendererBuilder()
                .useFastMode()
                .withHtmlContent(html, htmlOutputPath.toUri().toString())
                .toStream(outputStream)

            if (resolvedFontPath != null) {
                builder.useFont(resolvedFontPath.toFile(), "PdfCyrillic")
            } else {
                System.err.println(
                    "Cyrillic font is not configured."
                )
            }
            val resolvedMathFontPath = mathFontPath
            if (resolvedMathFontPath != null) {
                builder.useFont(resolvedMathFontPath.toFile(), "PdfMath")
            }

            builder.run()
        }
    }

    private fun buildHtml(tasks: List<TaskItem>): String {
        val taskBlocks = tasks.joinToString("\n") { task -> buildTaskHtml(task) }
        return loadHtmlTemplate()
            .replace(TASK_COUNT_PLACEHOLDER, tasks.size.toString())
            .replace(TASK_BLOCKS_PLACEHOLDER, taskBlocks)
    }

    private fun loadHtmlTemplate(): String {
        val resource = javaClass.classLoader.getResource(TEMPLATE_PATH)
            ?: error("PDF template not found: $TEMPLATE_PATH")
        return resource.openStream()
            .bufferedReader(Charsets.UTF_8)
            .use { reader -> reader.readText() }
    }

    private fun resolveFontPath(): Path? {
        if (fontPath != null && fontPath.exists()) return fontPath
        return bundledFontPath
    }

    private fun extractBundledFont(resourcePath: String, tempPrefix: String): Path? {
        val resource = javaClass.classLoader.getResourceAsStream(resourcePath) ?: return null
        return resource.use { input ->
            val tempFile = kotlin.io.path.createTempFile(tempPrefix, ".ttf")
            Files.copy(input, tempFile, StandardCopyOption.REPLACE_EXISTING)
            tempFile.toFile().deleteOnExit()
            tempFile
        }
    }

    private fun buildTaskHtml(task: TaskItem): String {
        val materialBlock = renderSection("", task.text)
        val subTaskBlocks = task.subTask.joinToString("\n") { subTask ->
            buildItemHtml(
                cssClass = "task",
                titleTag = "h2",
                title = "Задача №${subTask.number}",
                metaBlocks = buildList {
                    add("""<div class="meta">key=${escapeHtml(subTask.key)}</div>""")
                    if (subTask.table.cols != null || subTask.table.rows != null) {
                        val rows = subTask.table.rows?.toString() ?: "?"
                        val cols = subTask.table.cols?.toString() ?: "?"
                        add("""<div class="meta">table=${rows}x${cols}</div>""")
                    }
                },
                contentBlocks = listOf(renderSection("Задача", subTask.text))
            )
        }
        val filesBlock = renderFiles(task.files)

        return buildItemHtml(
            cssClass = "task",
            titleTag = "h2",
            title = "Задача №${task.number}",
            metaBlocks = listOf(
                """
                    <div class="meta">
                        taskId=${task.taskId}, key=${escapeHtml(task.key)}, difficulty=${task.difficulty}
                    </div>
                """.trimIndent()
            ),
            contentBlocks = listOf(materialBlock, filesBlock)
        )
            .plus(if (subTaskBlocks.isBlank()) "" else "\n$subTaskBlocks")
    }

    private fun renderSection(
        title: String,
        content: String,
        titleCssClass: String? = null,
        contentIsHtml: Boolean = false
    ): String {
        if (content.isBlank()) return ""
        val text = if (contentIsHtml) {
            content
        } else {
            val normalizedText = normalizeMathNotation(content)
            formatSectionContent(normalizedText)
        }
        val titleClassAttr = titleCssClass?.let { """ class="${escapeHtml(it)}"""" } ?: ""
        return """
            <div class="section">
                <h3$titleClassAttr>${escapeHtml(title)}</h3>
                <div>$text</div>
            </div>
        """.trimIndent()
    }

    private fun normalizeMathNotation(source: String): String {
        var result = source

        val commandReplacements = listOf(
            Regex("""\\equiv\b""") to "≡",
            Regex("""\\leftrightarrow\b""") to "↔",
            Regex("""\\Rightarrow\b""") to "⇒",
            Regex("""\\to\b""") to "→",
            Regex("""\\lor\b""") to "∨",
            Regex("""\\land\b""") to "∧",
            Regex("""\\neg\b""") to "¬",
            Regex("""\\oplus\b""") to "⊕",
            Regex("""\\leq\b""") to "≤",
            Regex("""\\geq\b""") to "≥"
        )

        commandReplacements.forEach { (pattern, replacement) ->
            result = result.replace(pattern, replacement)
        }

        result = result
            .replace("\\{", "{")
            .replace("\\}", "}")
            .replace("\\_", "_")

        result = result.replace(BRACED_SUBSCRIPT_REGEX, "$1<sub>$2</sub>")
        result = result.replace(SIMPLE_SUBSCRIPT_REGEX, "$1<sub>$2</sub>")
        result = result.replace(BRACED_SUPERSCRIPT_REGEX, "$1<sup>$2</sup>")
        result = result.replace(SIMPLE_SUPERSCRIPT_REGEX, "$1<sup>$2</sup>")

        return result
            .replace("\\(", "")
            .replace("\\)", "")
            .replace("\\[", "")
            .replace("\\]", "")
    }

    private fun formatSectionContent(rawText: String): String {
        val trimmed = rawText.trim()
        return if (looksLikeHtml(trimmed)) {
            normalizeHtmlFragment(trimmed)
        } else {
            escapeHtml(rawText).replace("\n", "<br />")
        }
    }

    private fun looksLikeHtml(value: String): Boolean {
        return HTML_TAG_REGEX.containsMatchIn(value)
    }

    private fun normalizeHtmlFragment(fragment: String): String {
        val normalizedInput = if (BROKEN_OPEN_TAG_REGEX.containsMatchIn(fragment)) {
            "<$fragment"
        } else {
            fragment
        }

        val doc = Jsoup.parseBodyFragment(normalizedInput)
        doc.outputSettings()
            .syntax(Document.OutputSettings.Syntax.xml)
            .escapeMode(Entities.EscapeMode.xhtml)
            .prettyPrint(false)

        val body = doc.body()
        body.select("script,style").remove()
        return body.html()
    }

    private fun buildItemHtml(
        cssClass: String,
        titleTag: String,
        title: String,
        metaBlocks: List<String>,
        contentBlocks: List<String>
    ): String {
        val meta = metaBlocks
            .filter { it.isNotBlank() }
            .joinToString("\n")
        val content = contentBlocks
            .filter { it.isNotBlank() }
            .joinToString("\n")

        return """
            <div class="$cssClass">
                <$titleTag>${escapeHtml(title)}</$titleTag>
                $meta
                $content
            </div>
        """.trimIndent()
    }

    private fun renderFiles(files: List<TaskFile>): String {
        if (files.isEmpty()) return ""
        val rows = files.joinToString("\n") { file ->
            val name = if (file.name.isBlank()) file.url else file.name
            val escapedUrl = escapeHtml(file.url)
            "<li><a href=\"$escapedUrl\">${escapeHtml(name)}</a></li>"
        }
        return """
            <div class="section">
                <h3>Файлы</h3>
                <ul>
                    $rows
                </ul>
            </div>
        """.trimIndent()
    }

    private fun escapeHtml(value: String): String {
        val source = value.ifEmpty { return "" }
        return buildString(source.length) {
            source.forEach { ch ->
                when (ch) {
                    '&' -> append("&amp;")
                    '<' -> append("&lt;")
                    '>' -> append("&gt;")
                    '"' -> append("&quot;")
                    '\'' -> append("&#39;")
                    else -> append(ch)
                }
            }
        }
    }

    companion object {
        private const val TEMPLATE_PATH = "templates/pdf/tasks.html"
        private const val BUNDLED_FONT_PATH = "fonts/timesnrcyrmt.ttf"
        private const val MATH_FONT_PATH = "fonts/NotoSansMath-Regular.ttf"
        private const val TASK_COUNT_PLACEHOLDER = "{{TASK_COUNT}}"
        private const val TASK_BLOCKS_PLACEHOLDER = "{{TASK_BLOCKS}}"
        private val HTML_TAG_REGEX = Regex("""</?[a-zA-Z][^>]*>""")
        private val BROKEN_OPEN_TAG_REGEX = Regex("""^\s*[a-zA-Z][a-zA-Z0-9]*>""")
        private val BRACED_SUBSCRIPT_REGEX =
            Regex("""(?<![A-Za-zА-Яа-я0-9])([A-Za-zА-Яа-я])_\{([^}]+)}""")
        private val SIMPLE_SUBSCRIPT_REGEX =
            Regex("""(?<![A-Za-zА-Яа-я0-9])([A-Za-zА-Яа-я])_([A-Za-zА-Яа-я0-9]+)""")
        private val BRACED_SUPERSCRIPT_REGEX =
            Regex("""(?<![A-Za-zА-Яа-я0-9])([A-Za-zА-Яа-я0-9])\^\{([^}]+)}""")
        private val SIMPLE_SUPERSCRIPT_REGEX =
            Regex("""(?<![A-Za-zА-Яа-я0-9])([A-Za-zА-Яа-я0-9])\^([A-Za-zА-Яа-я0-9]+)""")
    }
}
