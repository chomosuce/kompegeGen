fun main() : Unit {
    ApiService().use { apiService ->
        //TODO fetch new tasks in the start of programm
//        apiService.get(1, 100).forEach{
//                task ->
//            println("taskId=${task.taskId}, number=${task.number}, key=${task.key}")
//            }
        println(apiService.getDefinedTask(23260).key)
        val dataService = DataService()
//        dataService.saveTask(1, apiService.getTaskIds(1, 100))
        println(dataService.getTask(1, 10))
//        val tasks = apiService.getVariant()
//        tasks.forEach { task ->
//            println("taskId=${task.taskId}, number=${task.number}, key=${task.key}")
//        }
//        PdfgeneratorImpl().getPdf(tasks)
//        println("Generated build/generated/tasks.html and build/generated/tasks.pdf")
    }
}
