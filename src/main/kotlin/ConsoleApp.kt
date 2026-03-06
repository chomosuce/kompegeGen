import command.CommandContext
import command.CommandParser
import command.CommandRegistry
import command.CommandResult


class ConsoleApp(
    private val registry: CommandRegistry,
    private val context: CommandContext
) {
    fun run() {
        var running = true

        while (running) {
            print("> ")
            val line = readlnOrNull() ?: break
            val parsed = CommandParser.parse(line) ?: continue

            val command = registry.find(parsed.name)
            if (command == null) {
                println("Unknown command: ${parsed.name}. Use /help")
                continue
            }

            val result = command.execute(parsed.args, context)
            running = result == CommandResult.CONTINUE
        }
    }
}
