package any.data.js.plugin

import android.util.Log
import any.base.util.Http
import any.data.json.Json
import any.data.json.fromJson
import any.data.json.toJson
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class DefaultHttpPlugin(private val json: Json = Json) : HttpPlugin {
    private var currentTimeout: Long = Http.DEFAULT_TIMEOUT
    private var cachedHttpClient: OkHttpClient = Http.DEFAULT_CLIENT

    override fun request(requestBody: String): String? {
        val request = try {
            json.fromJson<HttpPlugin.HttpRequest>(requestBody)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
        if (request == null) {
            Log.e(TAG, "request failed: cannot parse requestBody: \n$requestBody")
            return null
        }

        val formBodyBuilder = FormBody.Builder()
        val params = request.params ?: emptyMap()
        for ((k, v) in params) {
            formBodyBuilder.add(k, v?.toString() ?: "")
        }

        val headersBuilder = Headers.Builder()
        val headers = request.headers ?: emptyMap()
        for ((k, v) in headers) {
            headersBuilder.add(k, v?.toString() ?: "")
        }

        val okHttpRequest = Request.Builder()
            .url(request.url)
            .let {
                if (request.method.equals("GET", ignoreCase = true)) {
                    it.get()
                } else {
                    it.method(request.method, formBodyBuilder.build())
                }
            }
            .headers(headersBuilder.build())
            .build()

        val okResponse = httpClient(request.timeout)
            .newCall(okHttpRequest)
            .execute()

        val response = HttpPlugin.HttpResponse(
            text = okResponse.body?.string(),
            status = okResponse.code,
            headers = okResponse.headers.toMap(),
        )

        return json.toJson(response)
    }

    private fun httpClient(timeout: Long): OkHttpClient {
        val okTimeout = when {
            timeout <= 0 -> Http.DEFAULT_TIMEOUT
            else -> timeout
        }
        return when (okTimeout) {
            currentTimeout -> {
                cachedHttpClient
            }

            Http.DEFAULT_TIMEOUT -> {
                cachedHttpClient = Http.DEFAULT_CLIENT
                cachedHttpClient
            }

            else -> {
                OkHttpClient.Builder()
                    .connectTimeout(okTimeout, TimeUnit.MILLISECONDS)
                    .callTimeout(okTimeout, TimeUnit.MILLISECONDS)
                    .readTimeout(okTimeout, TimeUnit.MILLISECONDS)
                    .writeTimeout(okTimeout, TimeUnit.MILLISECONDS)
                    .build()
                    .also {
                        cachedHttpClient = it
                    }
            }
        }
    }

    companion object {
        private const val TAG = "DefaultHttpPlugin"
    }
}