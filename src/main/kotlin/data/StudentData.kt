package data

import java.sql.Connection

class StudentData(private val connection: Connection) {
    init {
        connection.createStatement().use { stmt ->
            stmt.execute(
                """
                CREATE TABLE IF NOT EXISTS student_solved_tasks (
                  student_id TEXT NOT NULL,
                  task_id INTEGER NOT NULL,
                  PRIMARY KEY (student_id, task_id)
                )
                """.trimIndent()
            )
        }
    }

    fun addSolvedTask(studentId: String = "default", taskId: Int): Boolean {
        val sql = "INSERT OR IGNORE INTO student_solved_tasks(student_id, task_id) VALUES(?, ?)"
        return connection.prepareStatement(sql).use { ps ->
            ps.setString(1, studentId)
            ps.setInt(2, taskId)
            ps.executeUpdate() > 0
        }
    }

    fun hasSolvedTask(studentId: String = "default", taskId: Int): Boolean {
        val sql = """
            SELECT 1
            FROM student_solved_tasks
            WHERE student_id = ? AND task_id = ?
            LIMIT 1
        """.trimIndent()
        return connection.prepareStatement(sql).use { ps ->
            ps.setString(1, studentId)
            ps.setInt(2, taskId)
            ps.executeQuery().use { rs ->
                rs.next()
            }
        }
    }
}
