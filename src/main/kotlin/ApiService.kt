import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.DecodeSequenceMode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.decodeToSequence
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.Closeable

class ApiService : Closeable {
    companion object {
        private const val TASK_URL = "https://kompege.ru/api/v1/task/number/"
        private val json = Json {
            ignoreUnknownKeys = true
        }
    }
    private val client = OkHttpClient()

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

    override fun close() {
        client.dispatcher.cancelAll()
        client.dispatcher.executorService.shutdown()
        client.dispatcher.executorService.shutdownNow()
        client.connectionPool.evictAll()
        client.cache?.close()
    }
}
