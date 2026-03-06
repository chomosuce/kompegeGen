package command

class SolvedCommand : Command {
    override val name: String = "/solved"
    override val help: String = "/solved <taskId> or /solved <studentId> <taskId> - mark task as solved"

    override fun execute(args: List<String>, ctx: CommandContext): CommandResult {
        val parsed = parseStudentAndTask(args)
        if (parsed == null) {
            println("Usage: $help")
            return CommandResult.CONTINUE
        }

        val (studentId, taskId) = parsed
        val inserted = ctx.dataService.studentService.addSolvedTask(studentId = studentId, taskId = taskId)
        println(if (inserted) "Saved" else "Already exists")
        return CommandResult.CONTINUE
    }

    private fun parseStudentAndTask(args: List<String>): Pair<String, Int>? {
        return when (args.size) {
            1 -> {
                val taskId = args[0].toIntOrNull() ?: return null
                "default" to taskId
            }
            2 -> {
                val taskId = args[1].toIntOrNull() ?: return null
                args[0] to taskId
            }
            else -> null
        }
    }
}
