package command

import ApiService
import data.DataService

enum class CommandResult {
    CONTINUE,
    EXIT
}

data class CommandContext(
    val dataService: DataService,
    val apiService: ApiService
)

data class ParsedCommand(
    val name: String,
    val args: List<String>
)

object CommandParser {
    fun parse(input: String): ParsedCommand? {
        val line = input.trim()
        if (line.isEmpty()) return null

        val parts = line.split(Regex("\\s+"))
        return ParsedCommand(
            name = parts.first(),
            args = parts.drop(1)
        )
    }
}

class CommandRegistry {
    private val byName: LinkedHashMap<String, Command> = linkedMapOf()

    fun register(command: Command) {
        byName[command.name] = command
    }

    fun registerAll(commands: List<Command>) {
        commands.forEach(::register)
    }

    fun find(name: String): Command? = byName[name]

    fun all(): List<Command> = byName.values.sortedBy { it.name }
}
