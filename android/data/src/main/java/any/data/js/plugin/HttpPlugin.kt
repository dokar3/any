package any.data.js.plugin

import com.squareup.moshi.JsonClass

interface HttpPlugin {
    /**
     * Execute an http request.
     *
     * @param requestBody Json string which can be parsed to [HttpRequest].
     */
    fun request(requestBody: String): String?

    @JsonClass(generateAdapter = true)
    data class HttpRequest(
        val url: String,
        val method: String,
        val params: Map<String, Any?>? = null,
        val headers: Map<String, Any?>? = null,
        val timeout: Long = 20000L,
    )

    @JsonClass(generateAdapter = true)
    data class HttpResponse(
        val text: String?,
        val status: Int,
        val headers: Map<String, Any?>?,
    )
}