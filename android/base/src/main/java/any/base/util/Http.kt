package any.base.util

import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object Http {
    private const val USER_AGENT_CHROME = "Mozilla/5.0 (iPhone; CPU iPhone OS 13_2_3 like " +
            "Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/13.0.3 " +
            "Mobile/15E148 Safari/604.1"

    const val DEFAULT_TIMEOUT = 20000L

    val DEFAULT_CLIENT_BUILDER by lazy {
        OkHttpClient.Builder()
            .connectTimeout(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS)
            .callTimeout(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS)
            .readTimeout(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS)
            .writeTimeout(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS)
    }

    val DEFAULT_CLIENT by lazy { DEFAULT_CLIENT_BUILDER.build() }

    fun get(
        url: String,
        httpClient: OkHttpClient = DEFAULT_CLIENT,
        catchErrors: Boolean = true,
    ): String? = request(url, null, HttpMethod.Get, httpClient, catchErrors)

    fun post(
        url: String,
        params: Map<String, String>? = null,
        httpClient: OkHttpClient = DEFAULT_CLIENT,
        catchErrors: Boolean = true,
    ): String? = request(url, params, HttpMethod.Post, httpClient, catchErrors)

    private fun request(
        url: String,
        params: Map<String, String>?,
        method: HttpMethod,
        httpClient: OkHttpClient,
        catchErrors: Boolean,
    ): String? {
        val formBodyBuilder = FormBody.Builder()
        params?.forEach { (k, v) ->
            formBodyBuilder.add(k, v)
        }
        val request = Request.Builder()
            .url(url)
            .removeHeader("User-Agent")
            .addHeader("User-Agent", USER_AGENT_CHROME)
            .let {
                when (method) {
                    HttpMethod.Get -> it.get()
                    HttpMethod.Post -> it.post(formBodyBuilder.build())
                }
            }
            .build()

        val call = call@{
            val response = httpClient.newCall(request).execute()
            return@call if (response.isSuccessful) {
                response.body?.string()
            } else {
                null
            }
        }

        return if (catchErrors) {
            try {
                call()
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        } else {
            call()
        }
    }

    private enum class HttpMethod {
        Get,
        Post
    }
}
