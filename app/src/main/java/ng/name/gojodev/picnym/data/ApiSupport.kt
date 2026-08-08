package ng.name.gojodev.picnym.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class NetResponse(val status: Int, val json: JSONObject, val raw: String)

class ApiException(val statusCode: Int, override val message: String) : Exception(message)

object NetClient {
    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(90, TimeUnit.SECONDS)
        .build()

    suspend fun execute(request: Request): NetResponse = withContext(Dispatchers.IO) {
        client.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            val json = runCatching { if (raw.isBlank()) JSONObject() else JSONObject(raw) }
                .getOrElse { JSONObject().put("raw", raw) }
            NetResponse(response.code, json, raw)
        }
    }
}

fun NetResponse.requireSuccess(): JSONObject {
    if (status in 200..299) return json
    val message = json.optString("error").ifBlank { json.optString("msg") }.ifBlank { "Request failed ($status)." }
    throw ApiException(status, message)
}
