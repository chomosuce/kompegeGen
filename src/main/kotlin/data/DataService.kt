package data

import java.io.Closeable
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.sql.Connection
import java.sql.DriverManager

class DataService(dbPath: String = "build/data/kompege.db") : Closeable {
    private val connection: Connection
    val taskService: TaskData
    val studentService: StudentData

    init {
        ensureParentDirectory(dbPath)
        connection = DriverManager.getConnection("jdbc:sqlite:$dbPath")
        connection.createStatement().use { stmt ->
            stmt.execute("PRAGMA foreign_keys = ON")
        }

        taskService = TaskData(connection)
        studentService = StudentData(connection)
    }

    override fun close() {
        connection.close()
    }

    private fun ensureParentDirectory(dbPath: String) {
        val path: Path = Paths.get(dbPath).toAbsolutePath().normalize()
        val parent = path.parent ?: return
        Files.createDirectories(parent)
    }
}
