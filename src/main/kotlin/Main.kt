import command.*
import data.DataService

fun main() : Unit {
    DataService().use { dataService ->
        ApiService().use { apiService ->
            val registry = CommandRegistry()
            registry.registerAll(
                listOf(
                    HelpCommand { registry.all() },
                    ExitCommand(),
                    SolvedCommand(),
                    CheckCommand()
                )
            )

            println("Type /help to see available commands.")
            ConsoleApp(
                registry = registry,
                context = CommandContext(
                    dataService = dataService,
                    apiService = apiService
                )
            ).run()
        }
    }
}
