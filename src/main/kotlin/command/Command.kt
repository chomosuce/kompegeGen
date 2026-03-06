package command

interface Command {
    val name: String
    val help: String
    fun execute(args: List<String>, ctx: CommandContext): CommandResult
}
