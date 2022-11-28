package any.data.js.engine

interface JsEngine : AutoCloseable {
    val name: String

    val version: String

    fun <T : Any> set(name: String, clz: Class<T>, instance: T)

    fun evaluate(jsCode: String)

    fun <T : Any?> evaluate(jsCode: String, clz: Class<T>): T?

    interface Factory {
        fun create(): JsEngine
    }
}