import data.DataService
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.DecodeSequenceMode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.decodeToSequence
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.Closeable
import java.util.concurrent.TimeUnit

class ApiService : Closeable {
    companion object {
        private const val TASK_URL = "https://kompege.ru/api/v1/task/number/"
        private const val DEFINED_TASK_URL = "https://kompege.ru/api/v1/task/"
        private val TASKS = (1 .. 27).filter { it != 20 && it != 21 }
        private val json = Json {
            ignoreUnknownKeys = true
        }
    }
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .callTimeout(90, TimeUnit.SECONDS)
        .build()

    @OptIn(ExperimentalSerializationApi::class)
    fun get(number: Int, limit: Int? = null): List<TaskItem> {
        val req = Request.Builder()
            .url(TASK_URL + number.toString())
            .header("Accept", "application/json")
            .header("User-Agent", "Mozilla/5.0")
            .build()
        return client.newCall(req).execute().use { call ->
            if (!call.isSuccessful) {
                error("HTTP ${call.code}: ${call.message}")
            }
            val body = call.body ?: error("Empty response")
            if (limit == null) {
                json.decodeFromStream(body.byteStream())
            } else {
                require(limit >= 0) { "limit must be >= 0" }
                json.decodeToSequence<TaskItem>(
                    stream = body.byteStream(),
                    format = DecodeSequenceMode.ARRAY_WRAPPED
                ).take(limit).toList()
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun getTaskIds(number: Int, limit: Int? = null): List<Int> {
        val req = Request.Builder()
            .url(TASK_URL + number.toString())
            .header("Accept", "application/json")
            .header("User-Agent", "Mozilla/5.0")
            .build()
        return client.newCall(req).execute().use { call ->
            if (!call.isSuccessful) {
                error("HTTP ${call.code}: ${call.message}")
            }
            val body = call.body ?: error("Empty response")
            val ids = json.decodeToSequence<JsonObject>(
                stream = body.byteStream(),
                format = DecodeSequenceMode.ARRAY_WRAPPED
            ).mapNotNull { item ->
                item["taskId"]?.jsonPrimitive?.intOrNull
            }
            if (limit == null) {
                ids.toList()
            } else {
                require(limit >= 0) { "limit must be >= 0" }
                ids.take(limit).toList()
            }
        }
    }


    private fun getDefinedTask(id: Int): TaskItem {
        val req = Request.Builder()
            .url(DEFINED_TASK_URL + id.toString())
            .header("Accept", "application/json")
            .header("User-Agent", "Mozilla/5.0")
            .build()
        return client.newCall(req).execute().use { call ->
            if (!call.isSuccessful) {
                error("HTTP ${call.code}: ${call.message}")
            }
            val body = call.body ?: error("Empty response")
            json.decodeFromString(body.string())
        }
    }

    fun generateDefaultVariant(dataService: DataService, student :String = "default") : List<TaskItem> {
        val ids = mutableListOf<Int>()
        //TODO fix limit
        TASKS.forEach { task_n ->
            ids.add(dataService.taskService.getTaskIds(task_n, 100).
                filter { !dataService.studentService.hasSolvedTask(it, student) }[0])
        }
        return ids.map { id -> getDefinedTask(id) }
    }

    override fun close() {
        client.dispatcher.cancelAll()
        client.dispatcher.executorService.shutdown()
        client.dispatcher.executorService.shutdownNow()
        client.connectionPool.evictAll()
        client.cache?.close()
    }
}
