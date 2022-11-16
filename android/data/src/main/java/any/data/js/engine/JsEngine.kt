package any.data.js.engine

interface JsEngine : AutoCloseable {
    val name: String

    val version: String

    /**
     * Set object that can be used in javascript.
     */
    fun <T : Any> set(name: String, clz: Class<T>, instance: T)

    /**
     * Evaluate javascript code.
     */
    fun evaluate(jsCode: String)

    /**
     * Evaluate javascript code and get result.
     */
    fun <T : Any?> evaluate(jsCode: String, clz: Class<T>): T?

    interface Factory {
        fun create(): JsEngine
    }
}