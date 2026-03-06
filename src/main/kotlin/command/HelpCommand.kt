package command

class HelpCommand(private val commandsProvider: () -> List<Command>) : Command {
    override val name: String = "/help"
    override val help: String = "/help - show available commands"

    override fun execute(args: List<String>, ctx: CommandContext): CommandResult {
        commandsProvider().forEach { command ->
            println(command.help)
        }
        return CommandResult.CONTINUE
    }
}
