import command.*
import data.DataService


//TODO tasks from generated variant adds to solved right after the creation (make it psuhed to solved by the command)
//TODO pull fresh tasks to the task data
//TODO flexible variant generation
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
