import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

class ApiService {
    companion object {
        private const val TASK_URL = "https://kompege.ru/api/v1/task/number/"
        private val client = OkHttpClient()
        private val json = Json {
            ignoreUnknownKeys = true
        }
    }

    fun get(number: Int): List<TaskItem> {
        val req = Request.Builder()
            .url(TASK_URL + number.toString())
            .header("Accept", "application/json")
            .header("User-Agent", "Mozilla/5.0")
            .build()
        val response = client.newCall(req).execute().use { call ->
            if (!call.isSuccessful) {
                error("HTTP ${call.code}: ${call.message}")
            }
            call.body?.string() ?: error("Empty response")
        }
        return json.decodeFromString(response)
    }
}
