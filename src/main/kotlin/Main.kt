import command.*
import data.DataService

//TODO draw variant id in pdf/html
//TODO tasks from generated variant adds to solved right after the creation (make it psuhed to solved by the command)
//TODO change init in ConsoleApp to not pull task data every time
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
                    CheckCommand(),
                    VariantCommand()
                )
            )

            println("Type /help to see available commands.")
            ConsoleApp(
                registry = registry,
                context = CommandContext(
                    dataService = dataService,
                    apiService = apiService,
                    pdFgenerator = PdfgeneratorImpl()
                )
            ).run()
        }
    }
}
