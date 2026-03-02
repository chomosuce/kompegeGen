fun main() : Unit {
    ApiService().use { apiService ->
        val tasks = apiService.getVariant()
        tasks.forEach { task ->
            println("taskId=${task.taskId}, number=${task.number}, key=${task.key}")
        }
        PdfgeneratorImpl().getPdf(tasks)
        println("Generated build/generated/tasks.html and build/generated/tasks.pdf")
    }
}
