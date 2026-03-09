package command

class VariantCommand : Command {
    override val name: String = "/variant"
    override val help: String = "/variant [studentId] - generate and save a new variant"

    override fun execute(args: List<String>, ctx: CommandContext): CommandResult {
        if (args.size > 1) {
            println("Usage: $help")
            return CommandResult.CONTINUE
        }
        try {
            val studentId = args.firstOrNull() ?: "default"
            val tasks = ctx.apiService.generateDefaultVariant(ctx.dataService, studentId)
            if (tasks.isEmpty()) {
                println("No tasks available for variant generation")
                return CommandResult.CONTINUE
            }

            val variantId = ctx.dataService.taskService.saveVariant(tasks)
            tasks.forEach { ctx.dataService.studentService.addSolvedTask(studentId, it.taskId) }
            ctx.pdFgenerator.getPdf(tasks)
            println("variant_id=$variantId")
            println(tasks.map { it.taskId }.joinToString(" "))
        } catch (e: Exception) {
            println("Failed to generate variant: ${e.message ?: e::class.simpleName}")
        }
        return CommandResult.CONTINUE
    }
}
