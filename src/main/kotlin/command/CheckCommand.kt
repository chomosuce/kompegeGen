package command

class CheckCommand : Command {
    override val name: String = "/check"
    override val help: String = "/check <taskId> or /check <studentId> <taskId> - check solved status"

    override fun execute(args: List<String>, ctx: CommandContext): CommandResult {
        val parsed = parseStudentAndTask(args)
        if (parsed == null) {
            println("Usage: $help")
            return CommandResult.CONTINUE
        }

        val (studentId, taskId) = parsed
        val solved = ctx.dataService.studentService.hasSolvedTask(studentId = studentId, taskId = taskId)
        println(solved)
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
