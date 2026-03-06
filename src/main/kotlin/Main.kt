import data.DataService

fun main() : Unit {
    ApiService().use { apiService ->
        //TODO flexible variant constructor
        //TODO fetch new tasks in the start of program
        //TODO command to mark all tasks from variant(add variant table?) as solved
//        apiService.get(1, 100).forEach{
//                task ->
        //            println("taskId=${task.taskId}, number=${task.number}, key=${task.key}")
//            }
        println(apiService.getDefinedTask(23260).key)
        DataService().use { dataService ->
//            dataService.taskService.saveTaskIds(1, apiService.getTaskIds(1, 100))
            println(dataService.taskService.getTaskIds(1, 10))
            dataService.studentService.addSolvedTask(taskId = 23620)
        }
//        val tasks = apiService.getVariant()
//        tasks.forEach { task ->
//            println("taskId=${task.taskId}, number=${task.number}, key=${task.key}")
//        }
//        PdfgeneratorImpl().getPdf(tasks)
//        println("Generated build/generated/tasks.html and build/generated/tasks.pdf")
    }
}
