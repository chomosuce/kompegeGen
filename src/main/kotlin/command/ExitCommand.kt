package command

class ExitCommand : Command {
    override val name: String = "/exit"
    override val help: String = "/exit - quit program"

    override fun execute(args: List<String>, ctx: CommandContext): CommandResult {
        return CommandResult.EXIT
    }
}
