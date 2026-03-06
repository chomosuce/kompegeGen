import java.io.Closeable
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.sql.Connection
import java.sql.DriverManager

class DataService(dbPath: String = "build/data/kompege.db") : Closeable {
    private val connection: Connection

    init {
        ensureParentDirectory(dbPath)
        connection = DriverManager.getConnection("jdbc:sqlite:$dbPath")
        connection.createStatement().use { stmt ->
            stmt.execute("PRAGMA foreign_keys = ON")
        }
        initSchema()
    }

    private fun initSchema() {
        connection.createStatement().use { stmt ->
            stmt.execute(
                """
                CREATE TABLE IF NOT EXISTS tasks (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    task_number INTEGER NOT NULL,
                    task_id INTEGER NOT NULL,
                    UNIQUE(task_id)
                )
                """.trimIndent()
            )
        }
    }

    fun saveTask(taskNumber: Int, taskIds: List<Int>) {
        if (taskIds.isEmpty()) return

        val sql = "INSERT OR IGNORE INTO tasks(task_number, task_id) VALUES(?, ?)"
        connection.prepareStatement(sql).use { ps ->
            connection.autoCommit = false
            try {
                for (taskId in taskIds) {
                    ps.setInt(1, taskNumber)
                    ps.setInt(2, taskId)
                    ps.addBatch()
                }
                ps.executeBatch()
                connection.commit()
            } catch (e: Exception) {
                connection.rollback()
                throw e
            } finally {
                connection.autoCommit = true
            }
        }
    }

    fun getTask(taskNumber: Int, limit: Int? = null): List<Int> {
        val sql = buildString {
            append("SELECT task_id FROM tasks WHERE task_number = ?")
            if (limit != null) {
                require(limit >= 0) { "limit must be >= 0" }
                append(" LIMIT ?")
            }
        }

        return connection.prepareStatement(sql).use { ps ->
            ps.setInt(1, taskNumber)
            if (limit != null) {
                ps.setInt(2, limit)
            }
            ps.executeQuery().use { rs ->
                buildList {
                    while (rs.next()) {
                        add(rs.getInt("task_id"))
                    }
                }
            }
        }
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
