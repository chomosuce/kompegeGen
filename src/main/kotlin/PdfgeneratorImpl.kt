import com.openhtmltopdf.pdfboxout.PdfRendererBuilder
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Entities
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.outputStream
import kotlin.io.path.writeText

class PdfgeneratorImpl(
    private val htmlOutputPath: Path = Paths.get("build", "generated", "tasks.html"),
    private val pdfOutputPath: Path = Paths.get("build", "generated", "tasks.pdf"),
    private val fontPath: Path? = null
) : PDFgenerator {

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
                    "Cyrillic font is not configured. " +
                        "Provide fontPath in PdfgeneratorImpl to avoid '#' glyphs in PDF."
                )
            }

            builder.run()
        }
    }

    private fun buildHtml(tasks: List<TaskItem>): String {
        val taskBlocks = tasks.joinToString("\n") { task -> buildTaskHtml(task) }
        return """
            <!DOCTYPE html>
            <html xmlns="http://www.w3.org/1999/xhtml" lang="ru">
            <head>
                <meta charset="UTF-8" />
                <title>Материалы задач</title>
                <style>
                    @page { size: A4; margin: 14mm; }
                    body {
                        font-family: PdfCyrillic, sans-serif;
                        font-size: 12px;
                        line-height: 1.45;
                        color: #1a1a1a;
                    }
                    h1 { margin: 0 0 14px 0; font-size: 20px; }
                    h2 { margin: 0 0 8px 0; font-size: 16px; }
                    h3 { margin: 12px 0 6px 0; font-size: 14px; }
                    .task {
                        margin: 0 0 14px 0;
                        padding: 10px;
                        border: 1px solid #dcdcdc;
                        border-radius: 4px;
                        page-break-inside: avoid;
                    }
                    .subtask {
                        margin: 8px 0 10px 0;
                        padding: 10px;
                        border: 1px solid #e2e2e2;
                        border-radius: 4px;
                        background: #fcfcfc;
                        page-break-inside: avoid;
                    }
                    .subtasks-title {
                        margin: 12px 0 6px 0;
                        font-size: 14px;
                    }
                    .meta {
                        margin: 0 0 8px 0;
                        color: #4a4a4a;
                        font-size: 11px;
                    }
                    .section { margin: 8px 0; }
                    .section p { margin: 0 0 8px 0; }
                    .section table {
                        width: 100%;
                        border-collapse: collapse;
                        margin: 8px 0;
                    }
                    .section th, .section td {
                        border: 1px solid #9a9a9a;
                        padding: 4px 6px;
                        vertical-align: top;
                    }
                    .section img {
                        max-width: 100%;
                        height: auto;
                        display: block;
                        margin: 8px 0;
                    }
                    ul { margin: 6px 0 0 18px; padding: 0; }
                    li { margin: 0 0 4px 0; }
                    a { color: #0645ad; text-decoration: none; }
                </style>
            </head>
            <body>
                <h1>Материалы по задачам (${tasks.size})</h1>
                $taskBlocks
            </body>
            </html>
        """.trimIndent()
    }

    private fun resolveFontPath(): Path? {
        if (fontPath != null && fontPath.exists()) return fontPath

        val candidates = listOf(
            "/System/Library/Fonts/Supplemental/Arial.ttf",
            "/System/Library/Fonts/Supplemental/Times New Roman.ttf",
            "/System/Library/Fonts/Supplemental/Arial Unicode.ttf",
            "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
            "/usr/share/fonts/truetype/noto/NotoSans-Regular.ttf",
            "C:/Windows/Fonts/arial.ttf",
            "C:/Windows/Fonts/calibri.ttf"
        )

        return candidates
            .asSequence()
            .map(Paths::get)
            .firstOrNull(Path::exists)
    }

    private fun buildTaskHtml(task: TaskItem): String {
        val materialBlock = renderSection("Задача", task.text)
        val subTasksBlock = renderSubTasks(task.subTask)
        val filesBlock = renderFiles(task.files)

        return """
            <div class="task">
                <h2>Задача №${task.number}</h2>
                <div class="meta">
                    taskId=${task.taskId}, key=${escapeHtml(task.key)}, difficulty=${task.difficulty}
                </div>
                $materialBlock
                $subTasksBlock
                $filesBlock
            </div>
        """.trimIndent()
    }

    private fun renderSection(title: String, rawText: String): String {
        if (rawText.isBlank()) return ""
        val text = formatSectionContent(rawText)
        return """
            <div class="section">
                <h3>${escapeHtml(title)}</h3>
                <div>$text</div>
            </div>
        """.trimIndent()
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

    private fun renderSubTasks(subTasks: List<SubTaskItem>): String {
        if (subTasks.isEmpty()) return ""
        val blocks = subTasks.joinToString("\n") { subTask -> buildSubTaskHtml(subTask) }
        return """
            <div class="section">
                <h3 class="subtasks-title">Подзадачи</h3>
                $blocks
            </div>
        """.trimIndent()
    }

    private fun buildSubTaskHtml(subTask: SubTaskItem): String {
        val materialBlock = renderSection("Материал", subTask.text)
        val tableMeta = if (subTask.table.cols != null || subTask.table.rows != null) {
            val rows = subTask.table.rows?.toString() ?: "?"
            val cols = subTask.table.cols?.toString() ?: "?"
            """<div class="meta">table=${rows}x${cols}</div>"""
        } else {
            ""
        }

        return """
            <div class="subtask">
                <h3>Подзадача №${subTask.number}</h3>
                <div class="meta">key=${escapeHtml(subTask.key)}</div>
                $tableMeta
                $materialBlock
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
        private val HTML_TAG_REGEX = Regex("""</?[a-zA-Z][^>]*>""")
        private val BROKEN_OPEN_TAG_REGEX = Regex("""^\s*[a-zA-Z][a-zA-Z0-9]*>""")
    }
}
