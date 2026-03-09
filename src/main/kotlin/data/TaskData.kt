package data

import TaskItem
import java.sql.Connection

class TaskData(private val connection: Connection) {
    init {
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
            stmt.execute(
                """
                CREATE TABLE IF NOT EXISTS variants (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    variant_id INTEGER NOT NULL,
                    task_id INTEGER NOT NULL,
                    UNIQUE(id)
                )
                """.trimIndent()
            )
        }
    }

    fun saveVariant(tasks: List<TaskItem>) {
        if (tasks.isEmpty()) return

        val nextVariantId = connection.createStatement().use { st ->
            st.executeQuery("SELECT COALESCE(MAX(variant_id), 0) + 1 AS next_id FROM variants")
                .use { rs -> rs.next(); rs.getInt("next_id") }
        }

        val sql = "INSERT OR IGNORE INTO variants(variant_id, task_id) VALUES(?, ?)"
        connection.prepareStatement(sql).use { ps ->
            for (task in tasks) {
                ps.setInt(1, nextVariantId)
                ps.setInt(2, task.taskId)
                ps.addBatch()
            }
            ps.executeBatch()
        }
    }

    fun getVariant(variant_id: Long): List<Int> {
        val sql = "SELECT task_id FROM variants WHERE variant_id = ? ORDER BY id"
        return connection.prepareStatement(sql).use { ps ->
            ps.setLong(1, variant_id)
            ps.executeQuery().use { rs ->
                buildList {
                    while (rs.next()) {
                        add(rs.getInt("task_id"))
                    }
                }
            }
        }
    }

    fun saveTaskIds(taskNumber: Int, taskIds: List<Int>) {
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

    fun getTaskIds(taskNumber: Int, limit: Int? = null): List<Int> {
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
}
