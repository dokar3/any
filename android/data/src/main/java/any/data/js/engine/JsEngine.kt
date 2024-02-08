package any.data.js.engine

interface JsEngine : AutoCloseable {
    val name: String

    val version: String

    fun <T : Any> set(name: String, type: Class<T>, instance: T)

    suspend fun evaluate(code: String)

    suspend fun <T : Any?> evaluate(code: String, type: Class<T>): T?

    interface Factory {
        fun create(): JsEngine
    }
}